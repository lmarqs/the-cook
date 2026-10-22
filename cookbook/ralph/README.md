ralph-tui works through a tracker's tasks, a Beads epic or a JSON PRD, by running coding agents in a loop. This artifact
sets it up so each run works unattended in a worktree of its own, `.worktrees/ralph/<name>` by default, on the branch
`ralph/<name>`, and connects to worker limits and worktree setup only where the person names them. To set it up, cook
`#ralph-tui`, whose help says what it writes and how to launch a run.

## Gating completion

ralph-tui 0.12 completes and commits a task when the agent's output holds `<promise>COMPLETE</promise>` anywhere, at
any exit code, even in a sentence saying it is not done; it never runs the project's checks itself. Cooking
`#ralph-tui` with `--worker-check=<path>` gates completion on a committed check the team writes: the recipe writes
`.ralph-tui/bin/worker`, a wrapper that follows the pseudo code below, and seeds each agent entry to run it. The check
gets the commit the agent started from as its argument, and exits 0 when the task's work passes.

A team that wants another gate points an agent entry's `command` at a wrapper script of its own. What ralph-tui does
to that command:

- It runs `<command> --version` first, to detect the agent.
- The arguments are the entry's `defaultFlags`, then the plugin's own; Codex's end in `-`, with the prompt on stdin, as
  Claude's prompt is.
- Output to stderr with `rate limit`, `429`, `too many requests`, `quota exceeded` or `overloaded` marks the iteration
  rate-limited, at any exit code; with exit 1, 2 or 429, so do `throttl`, `capacity` and `backoff`.
- A non-zero exit fails the iteration; `[errorHandling] strategy = "retry"`, `maxRetries` and
  `continueOnNonZeroExit = false` retry it, then skip the task.
- The environment passes through, except variables matching `*_API_KEY`, `*_SECRET_KEY` and `*_SECRET`.
- `command` cannot contain `;&|`$()`; put arguments in `defaultFlags`. A relative path is found in each worker's
  worktree.
- ralph-tui stops the agent by signalling the command's own process, which then stops what it started.
- Parallel workers get only `config.toml` copied into their worktree, and no setup runs there.

A wrapper, in the project's own language:

```
if the arguments are just --version: run the agent with --version and exit with its code
if this is the main checkout: print one line to stderr; exit 1
base = git rev-parse HEAD                    # ralph-tui commits each task, so each starts from a new base
on a stop signal: stop the agent's or the checks' processes and their children, then exit
run the agent with the arguments, stdin passed on, stdout held in an ignored file
if the agent exits non-zero: exit with its code
if the held stdout holds <promise>COMPLETE</promise>:
  run the project's checks against base, output to an ignored log file, never stderr
  if the checks fail: print one line to stderr, free of the words above; exit 1
print the held stdout; exit 0
```

Wired in `.ralph-tui/config.toml`, with the wrapper committed so every run worktree has it:

```toml
[[agents]]
name = "claude"
plugin = "claude"
command = "scripts/ralph-worker"

[errorHandling]
strategy = "retry"
maxRetries = 2
continueOnNonZeroExit = false
```
