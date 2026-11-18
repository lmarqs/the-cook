"""Scores results/raw.jsonl: accuracy, consistency and memory precision and recall, per harness and arm."""
import argparse
import collections
import json
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
GROUPS = ['control', 'example', 'recognition', 'contested', 'slow', 'no_row']


def pct(part, whole):
    return f'{100 * part / whole:.0f}%' if whole else '-'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--raw', default=str(HERE / 'results' / 'raw.jsonl'))
    parser.add_argument('--runs', type=int, default=10)
    parser.add_argument('--partial', action='store_true', help='score an incomplete file')
    args = parser.parse_args()

    facts = {f['id']: f for f in json.loads((HERE / 'facts.json').read_text())['facts']}
    rows = [json.loads(line) for line in pathlib.Path(args.raw).read_text().splitlines() if line.strip()]
    cells = collections.Counter((r['fact'], r['arm'], r['harness'], r['run']) for r in rows)
    duplicates = [c for c, n in cells.items() if n > 1]
    if duplicates:
        sys.exit(f'duplicate cells: {duplicates[:5]}')
    harnesses = sorted({r['harness'] for r in rows})
    arms = [a for a in ('old', 'new', 'none') if any(r['arm'] == a for r in rows)]
    missing = [(f, a, h, n) for f in facts for a in arms for h in harnesses for n in range(1, args.runs + 1)
               if (f, a, h, n) not in cells]
    if missing and not args.partial:
        sys.exit(f'{len(missing)} cells missing, such as {missing[:3]}; rerun run.py or pass --partial')

    answers = collections.defaultdict(list)
    for r in rows:
        answers[(r['fact'], r['arm'], r['harness'])].append(r['destination'])

    def correct(fact, answer, lenient):
        f = facts[fact]
        return answer == f['gold'] or (lenient and answer == f.get('alt'))

    def modal(values):
        top, count = collections.Counter(values).most_common(1)[0]
        return top, count / len(values)

    columns = [(h, a) for h in harnesses for a in arms]
    head = '| ' + ' | '.join(f'{h} {a}' for h, a in columns) + ' |'
    rule = '|' + ' --- |' * len(columns)

    print('## Accuracy by group, strict (lenient where a second label exists)\n')
    print('| Group | Facts ' + head + '\n| --- | --- ' + rule)
    for group in GROUPS + ['all']:
        ids = [i for i, f in facts.items() if group in ('all', f['group'])]
        line = [group, str(len(ids))]
        for h, a in columns:
            vals = [(i, x) for i in ids for x in answers[(i, a, h)]]
            strict = sum(correct(i, x, False) for i, x in vals)
            lenient = sum(correct(i, x, True) for i, x in vals)
            line.append(pct(strict, len(vals)) + (f' ({pct(lenient, len(vals))})' if lenient != strict else ''))
        print('| ' + ' | '.join(line) + ' |')

    print('\n## Consistency by group: mean share of runs giving a fact\'s most common answer\n')
    print('| Group ' + head + '\n| --- ' + rule)
    for group in GROUPS + ['all']:
        ids = [i for i, f in facts.items() if group in ('all', f['group'])]
        line = [group]
        for h, a in columns:
            shares = [modal(answers[(i, a, h)])[1] for i in ids if answers[(i, a, h)]]
            line.append(pct(sum(shares), len(shares)))
        print('| ' + ' | '.join(line) + ' |')

    print('\n## Memory or not: precision and recall of answering memory, against the strict label\n')
    print('| Measure ' + head + '\n| --- ' + rule)
    precision, recall = ['precision'], ['recall']
    for h, a in columns:
        tp = sum(x == 'memory' and facts[i]['gold'] == 'memory' for i in facts for x in answers[(i, a, h)])
        said = sum(x == 'memory' for i in facts for x in answers[(i, a, h)])
        actual = sum(facts[i]['gold'] == 'memory' for i in facts for _ in answers[(i, a, h)])
        precision.append(pct(tp, said))
        recall.append(pct(tp, actual))
    print('| ' + ' | '.join(precision) + ' |\n| ' + ' | '.join(recall) + ' |')

    print('\n## Per fact: most common answer, its share of runs, and a mark when it matches the label\n')
    print('| Fact | Group | Label ' + head + '\n| --- | --- | --- ' + rule)
    for i, f in facts.items():
        line = [i, f['group'], f['gold'] + (f' / {f["alt"]}' if f.get('alt') else '')]
        for h, a in columns:
            values = answers[(i, a, h)]
            if not values:
                line.append('-')
                continue
            top, share = modal(values)
            mark = ' ✓' if correct(i, top, True) else ''
            line.append(f'{top} {share:.0%}{mark}')
        print('| ' + ' | '.join(line) + ' |')

    print(f'\nRuns scored: {len(rows)}. Missing cells: {len(missing)}.')


if __name__ == '__main__':
    main()
