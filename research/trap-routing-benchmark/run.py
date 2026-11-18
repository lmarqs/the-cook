"""Asks Claude Code and Codex where a learned fact goes, given one version of the agents-layout memory text."""
import argparse
import concurrent.futures
import json
import os
import pathlib
import subprocess
import sys
import tempfile
import threading
import time

HERE = pathlib.Path(__file__).resolve().parent
MEMORY = 'cookbook/src/agents/skills/agents-layout/references/memory.md'
# The text before and after PR #134.
TEXTS = {'old': '6e34b991', 'new': 'c7b3d7c3'}
MODELS = {'claude': 'claude-opus-5-5', 'codex': 'gpt-6-astra'}
DESTINATIONS = [
    ('not_in_repository', 'nowhere in the repository'),
    ('tracker_or_commit', 'the task tracker or a commit message'),
    ('personal_settings', "one person's own harness settings, outside the repository"),
    ('nowhere_already_shown', 'nowhere, because the repository already shows it'),
    ('check_hook_or_policy', 'a check, a hook or the permission policy'),
    ('agents_md', 'AGENTS.md'),
    ('skill', 'a skill in .agents/skills/'),
    ('documentation', 'a documentation page in docs/'),
    ('memory', 'a memory file in .agents/memory/'),
    ('none_of_these', 'none of these; ask the team where they keep it'),
]
SYSTEM = 'You answer questions about how a software team organizes its repository.'


def git(*args):
    return subprocess.run(['git', '-C', str(HERE), *args], check=True, capture_output=True, text=True).stdout


def prompt(repository, fact, text):
    parts = ['You maintain the shared agent setup of a software repository. ' + repository]
    if text is not None:
        parts.append("The team's guidance on where a learned fact goes:\n\n<guidance>\n" + text.strip() + '\n</guidance>')
    parts.append('During a run in this repository, an agent learned this:\n\n<fact>\n' + fact + '\n</fact>')
    parts.append('Where should the team record it? Choose one destination:\n' +
                 '\n'.join(f'- {key}: {meaning}' for key, meaning in DESTINATIONS))
    parts.append('Answer with the destination and a one-sentence reason.')
    return '\n\n'.join(parts) + '\n'


def call_claude(body, schema, workdir):
    command = ['claude', '-p', '--setting-sources', '', '--tools', '', '--strict-mcp-config',
               '--no-session-persistence', '--output-format', 'json', '--json-schema', json.dumps(schema),
               '--model', MODELS['claude'], '--system-prompt', SYSTEM]
    done = subprocess.run(command, input=body, capture_output=True, text=True, cwd=workdir, timeout=600)
    if done.returncode:
        raise RuntimeError(f'claude exited {done.returncode}: {done.stderr[-400:]}')
    out = json.loads(done.stdout)
    answer = out.get('structured_output') or json.loads(out['result'])
    usage = out['modelUsage'][MODELS['claude']]
    tokens = usage['inputTokens'] + usage['cacheReadInputTokens'] + usage['cacheCreationInputTokens']
    return answer, tokens, usage['outputTokens'], out.get('total_cost_usd')


def call_codex(body, schema_path, workdir):
    with tempfile.NamedTemporaryFile('r', suffix='.json') as last:
        command = ['codex', 'exec', '--json', '--ephemeral', '--ignore-user-config', '--ignore-rules',
                   '--skip-git-repo-check', '-s', 'read-only', '-C', workdir, '-m', MODELS['codex'],
                   '-c', 'model_reasoning_effort="medium"', '--output-schema', str(schema_path), '-o', last.name, '-']
        done = subprocess.run(command, input=body, capture_output=True, text=True, cwd=workdir, timeout=600)
        if done.returncode:
            raise RuntimeError(f'codex exited {done.returncode}: {done.stderr[-400:]}')
        answer = json.loads(pathlib.Path(last.name).read_text())
    usage = next(json.loads(line)['usage'] for line in done.stdout.splitlines()
                 if line.startswith('{') and json.loads(line).get('type') == 'turn.completed')
    return answer, usage['input_tokens'], usage['output_tokens'], None


def version(harness):
    return subprocess.run([harness, '--version'], capture_output=True, text=True).stdout.strip()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--harness', nargs='+', default=['claude', 'codex'], choices=['claude', 'codex'])
    parser.add_argument('--arms', nargs='+', default=['old', 'new', 'none'], choices=['old', 'new', 'none'])
    parser.add_argument('--facts', nargs='*', help='fact ids; all when omitted')
    parser.add_argument('--runs', type=int, default=10)
    parser.add_argument('--jobs', type=int, default=4)
    parser.add_argument('--out', default=str(HERE / 'results' / 'raw.jsonl'))
    args = parser.parse_args()

    data = json.loads((HERE / 'facts.json').read_text())
    facts = [f for f in data['facts'] if not args.facts or f['id'] in args.facts]
    schema_path = HERE / 'schema.json'
    schema = json.loads(schema_path.read_text())
    texts = {arm: git('show', f'{commit}:{MEMORY}') for arm, commit in TEXTS.items()}
    texts['none'] = None
    commits = {arm: git('rev-parse', commit).strip() for arm, commit in TEXTS.items()}
    versions = {h: version(h) for h in args.harness}

    out = pathlib.Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    done = set()
    if out.exists():
        for line in out.read_text().splitlines():
            r = json.loads(line)
            done.add((r['fact'], r['arm'], r['harness'], r['run']))
    cells = [(f, arm, h, n) for n in range(1, args.runs + 1) for h in args.harness for arm in args.arms for f in facts
             if (f['id'], arm, h, n) not in done]
    print(f'{len(cells)} runs to make, {len(done)} already in {os.path.relpath(out, HERE)}', file=sys.stderr)

    lock = threading.Lock()
    workdir = tempfile.mkdtemp(prefix='trap-routing-')

    def one(cell):
        fact, arm, harness, n = cell
        body = prompt(data['repository'], fact['text'], texts[arm])
        start = time.monotonic()
        try:
            if harness == 'claude':
                answer, tokens_in, tokens_out, cost = call_claude(body, schema, workdir)
            else:
                answer, tokens_in, tokens_out, cost = call_codex(body, schema_path, workdir)
        except Exception as error:  # a failed run is reported and left for the next invocation to retry
            print(f'FAIL {fact["id"]} {arm} {harness} {n}: {error}', file=sys.stderr)
            return
        record = dict(fact=fact['id'], arm=arm, harness=harness, run=n, model=MODELS[harness],
                      harness_version=versions[harness], text_commit=commits.get(arm),
                      destination=answer['destination'], reason=answer['reason'], input_tokens=tokens_in,
                      output_tokens=tokens_out, cost_usd=cost, seconds=round(time.monotonic() - start, 1))
        with lock:
            with out.open('a') as stream:
                stream.write(json.dumps(record) + '\n')

    with concurrent.futures.ThreadPoolExecutor(args.jobs) as pool:
        list(pool.map(one, cells))


if __name__ == '__main__':
    main()
