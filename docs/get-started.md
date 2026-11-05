---
description: Walks a newcomer from installing Cook to a cooked recipe, a policy change and a failing check.
audience: Whoever tries Cook for the first time, with a terminal and ten minutes
---
# Get started

In ten minutes you give an empty repository one permission policy that Claude Code and Codex both obey, change it, and
watch `cook check` catch a change that was not cooked. The output shown is from a real run, shortened. You need Linux or macOS on
arm64, or Linux on x64, with Git. Cook brings its own Java runtime.

## 1. Install Cook

Install it with Homebrew, or with mise as `mise use -g github:lmarqs/the-cook`; [Install](features/install.md) also
covers the release package:

```sh
brew tap lmarqs/the-cook https://github.com/lmarqs/the-cook
brew install cook
cook --version
```

`cook --version` prints the release, such as `cook 11.0.0`. Make a repository to try it in, and keep the release
at hand, since recipes are named by the same version:

```sh
mkdir lab && cd lab && git init
agents=io.github.lmarqs.cookbook:agents:$(cook --version | cut -d' ' -f2)
```

## 2. Cook two recipes

`#seed` writes `AGENTS.md` and a starter policy that grants nothing. `#connect` renders that policy into each
harness's own settings and links `.claude/` and `.codex/` to them:

```sh
cook $agents#seed $agents#connect --harness=claude --harness=codex
```

Cook prints the diff of every file it would write, `cook.lock` included, then asks once:

```text
Write these changes? [y/N]
```

Answer `y`. Cook prints one line per path, then what each recipe suggests next:

```text
Created .agents/README.md
Created .agents/adapters/claude/settings.json
Created .agents/adapters/codex/config.toml
...
Created AGENTS.md
Created cook.lock

After cooking io.github.lmarqs.cookbook:agents:<version>#connect
  Review the rendered permissions against each harness's permission model. Edit `.agents/permission/policy.yaml` and
  cook again to update them; after deleting `.claude/` or `.codex/`, cook again to recreate the links.
```

Run the same command again. Nothing changed, so Cook asks nothing:

```text
Already cooked.
```

Commit it all, `cook.lock` included. A teammate who clones the repository now has the same setup
without running Cook.

## 3. Change the policy

Open `.agents/permission/policy.yaml` and let agents read the repository's state, but never throw work away:

```yaml
commands:
  allow:
    - [git, [status, diff]]
  ask: []
  deny:
    - [git, reset, --hard]
```

## 4. Let check catch it

Before cooking, ask Cook whether the files match what `cook.lock` records:

```sh
cook check
```

```text
Would update .agents/adapters/claude/settings.json
Would create .agents/adapters/codex/rules/cook.rules
Would create .codex/rules
Would update cook.lock
cook: check: 4 files differ from what cook.lock records; nothing was written
```

It exits 1. In CI, this is the step that fails when someone edits the policy and forgets to cook.

## 5. Cook again

```sh
cook $agents#connect --harness=claude --harness=codex
```

The diff shows one decision landing in each harness's own words. Claude Code gets permission rules:

```diff
-    "allow": [],
+    "allow": [
+      "Bash(git status *)",
+      "Bash(git diff *)"
+    ],
     "ask": [],
-    "deny": [],
+    "deny": [
+      "Bash(git reset --hard *)"
+    ],
```

Codex gets rules, inside markers that name the recipe that owns them:

```diff
+# cook:begin io.github.lmarqs.cookbook:agents#connect/commands
+prefix_rule(pattern = ["git", ["status", "diff"]], decision = "allow")
+prefix_rule(pattern = ["git", "reset", "--hard"], decision = "forbidden")
+# cook:end io.github.lmarqs.cookbook:agents#connect/commands
```

Answer `y`, and `cook check` passes again. Anything you write in those files outside what the recipe owns stays as
you wrote it.

## Where to go next

- [Write a recipe](write-a-recipe.md) turns the setup your team is missing into a recipe of your own.
- [Cooking](features/cook.md) explains what a recipe owns and what it leaves alone.
- [The agents cookbook](../cookbook/src/agents/README.md) lists what else `#connect` sets up, such as session hooks and
  worktrees.
- [Discard](features/discard.md) takes a recipe's output back out.
- [Pitfalls](features/pitfalls.md) lists the setups that look right and are not.
