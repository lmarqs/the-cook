---
objective:
  problem: >-
    A team composes several recipes and the programs they configure in one repository, and decides how they
    meet. When a recipe connects itself to another recipe's or program's files because it finds them, cooking one recipe
    changes what another writes, and nothing the reviewer reads says why. Each recipe then carries knowledge of the
    others that breaks when they change.
  goal: >-
    What a recipe writes follows from its arguments and the files it models, and every connection between
    recipes, programs and the repository can be read from the arguments a recipe was cooked with.
---
# 0030. Recipes connect to other recipes' files only through parameters the person names

## Context

- `cook.lock` records each recipe's source and normalized supplied arguments, which replicate replays
  ([0007](0007-ownership.md)). Every owned section also opens with a comment line naming the recipe and those
  arguments ([generated content](../ux/generated-content-standards.md)), so a reader sees them beside the output.
  Re-cooking repeats those arguments, and `--check` compares the result with the destination.
- A model can read the current content of any file in the destination, including a file another recipe in the same
  invocation writes.
- Worked example: acme cooks a tracker recipe that adds a session-start script whenever it finds an agents layout's
  hooks directory. A later commit adds the layout. `./cook --check` with the tracker's unchanged arguments now reports
  the tracker as changed, and removing the layout makes the script disappear on the next cook, although nobody asked
  for either.
- Programs ship adapters to other programs themselves: bd 1.3.1's `bd setup` lists integrations for 14 coding agents,
  each with `--check` and `--remove` (`bd setup --help`).
- When two recipes write the same section, key or file, the last one wins and becomes its owner
  ([0007](0007-ownership.md)), and a recipe rewrites what it owns on every cook. A recipe that owns a program's hook
  key therefore replaces the team's own edit to that key, and takes the key from another recipe or loses it to one.
- People and coding agents learn a repository's rules from instruction files the team keeps for them, such as a
  README for people and an agent instruction file for agents. A rule a recipe sets up, such as where worktrees go,
  reaches them only through those files.

## Decision

1. A recipe decides what it writes from its arguments and the current content of the files its models write. It does
   not look for another recipe's or program's files to decide whether to connect to them.
2. Where a recipe's files meet files another recipe or the team provides, the person names the meeting point by a
   parameter: the path of a file or directory, from the repository root, or a name the other side defines, such as a
   role. The recipe may read what the parameter names, and refuses when it is missing.
3. A recipe writes the configuration of the programs it models. Adapting its output to another program, such as a
   coding agent's session-start hook, is the repository's: in files the team owns, or through an extension point of
   another recipe that the person names by point 2.
4. Cook connects automatically only files of one recipe, such as a link from the path a program reads to the file the
   recipe owns.
5. A recipe states the rules it sets up in each instruction file the person names by point 2, as a section
   ([0018](0018-conventions.md)), and in none the person leaves unnamed.

## Reasons

- **1.** The arguments in the lock are what replicate repeats, and the opening line shows them beside a section for
  review and an explicit re-cook or `--check`. A write that depends on another recipe's files makes `--check` report a
  change with no argument changed, and makes the result depend on which recipe was cooked first. The trade is an
  argument to type where detection would have guessed right.
- **2.** A parameter puts the connection in the lock and on the argument line, so it is reviewed, repeated on every
  replicate and removed by cooking without it. The recipe knows the shape of the meeting point, a directory of scripts
  or a settings file, and not which recipe provides it, so either side can change without the other. The trade is that
  the person must know the path; the recipe's help names the meeting point in Connections.
- **3.** A program's adapters to other programs exist outside the recipe and change with that program, so a recipe
  that copies one picks some programs and drifts from the rest. A recipe that owns another program's key also replaces
  the team's use of it on every cook. The trade is a step the team takes by hand, which the recipe's After cooking names.
- **4.** Between files of one recipe the connection has one correct form and no other party to agree with.
- **5.** Otherwise every repository that cooks a recipe writes the same few lines by hand on where its files are and
  how to use them, and those lines drift when the recipe changes. Owned, they change with the recipe's arguments and
  version. Naming the files keeps point 1: the recipe never looks for an instruction file, a repository whose agents
  read another file names that one, and one cook writes to every file named. The trade is a section the team does not
  edit by hand, since the recipe owns it ([0007](0007-ownership.md)).

## Rejected alternatives

### Connect when the other side is found

At its best, a person cooks two recipes and they find each other with no argument, and the setup appears as soon as
both sides exist. It loses because the result changes with files the recipe does not own, which no argument records,
so reviewing, re-cooking and `--check` cannot explain it.

### A convention directory every recipe writes to

At its best, the repository has one agreed place, such as a setup directory, and each tool recipe adds its script
there without a parameter. It loses because the recipe still depends on the convention, now without saying so, and a
place shared by unrelated moments, such as a checkout's setup and an agent session's start, runs each script at the
wrong one.

## Consequences

- A recipe's Connections section says "the person, by a parameter" for every contact with another recipe or the team,
  and "Cook, automatically" only between its own files.
- A recipe that offers a meeting point publishes its shape in API, such as a directory whose scripts run at session
  start, so other recipes' parameters can name it.
