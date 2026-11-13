---
description: Records why Cook never replaces an existing template file without the caller's choice, and how the caller makes it.
audience: Whoever writes a recipe that seeds a file, or changes how cooking treats a file that already exists
---
# 0033. Cook never replaces an existing template file without the caller's choice

Some recipes write a starter file from a template the first time they are cooked, and a person may already have
written that file. Replacing it loses their work; keeping it without a word leaves the recipe's setup unapplied and
unseen. The person cooking cannot tell which files collide until something is lost or missing.

No existing file is replaced without the caller's choice. A collision on a file its recipe's author marked as one
that must not be overwritten is visible before anything is written.

## Context

- Worked example: `acme/api` keeps a hand-written `NOTES.md`. The [recipe](../features/glossary.md)
  `acme:notes#starter` writes `NOTES.md` from a template the first time it is cooked. Cooking it in `acme/api` meets
  a file Cook did not write.
- On the first cook of a recipe, `cook.lock` has no record of its files, so a file at the template's path cannot be
  told from one written by hand ([architecture](../architecture.md#ownership-and-the-lock)).
- The template files of a recipe differ in what an existing file means: a starter `NOTES.md` may hold a team's
  prose, and an empty `.gitkeep` placeholder holds nothing.
- Tools that write files a person may already have, and how each treats an existing file, as their documentation or
  source states it:

| Tool | When the file exists | Override | Keep the existing file | Status per file |
| --- | --- | --- | --- | --- |
| Rails generators (Thor) | Asks `Overwrite <file>? [Ynaqdhm]`, with `d` for a diff and `a` for all | `-f`, `--force` | `-s`, `--skip` | `create`, `identical`, `force`, `skip`, `conflict`, also under `--pretend` |
| Angular `ng generate` | Not documented | `--force` | Not documented | Not documented |
| Yeoman | Asks before each write that overwrites content | Not documented | Not documented | Not documented |
| Copier | Asks | `--overwrite`, which overwrites without asking | `skip_if_exists`, declared per file by the template | Not documented |
| cookiecutter | Not documented | `-f`, `--overwrite-if-exists` | `-s`, `--skip-if-file-exists` | Not documented |
| chezmoi | Asks when the file changed since chezmoi last wrote it | `--force`, which applies without prompting | Not documented | Not documented |
| GNU Stow | Records a conflict and refuses to proceed | `--adopt`, which moves the existing file into the package | Not documented | Not documented |
| Kubernetes server-side apply | Rejects a field another field manager owns | `--force-conflicts` | Leave the field out of the applied configuration | Not documented |
| Ansible `copy` | Overwrites; `force` defaults to yes | Not applicable | `force: false`, which writes only when the destination is absent | Not documented |

Sources:

- [Rails generators guide](https://guides.rubyonrails.org/generators.html)
- [Thor `create_file.rb`](https://github.com/rails/thor/blob/main/lib/thor/actions/create_file.rb)
- [Thor `shell/basic.rb`](https://github.com/rails/thor/blob/main/lib/thor/shell/basic.rb)
- [Angular `ng generate`](https://angular.dev/cli/generate)
- [Yeoman file system](https://yeoman.io/authoring/file-system)
- [Copier configuring](https://copier.readthedocs.io/en/stable/configuring/)
- [cookiecutter CLI options](https://cookiecutter.readthedocs.io/en/stable/cli_options.html)
- [chezmoi `apply`](https://www.chezmoi.io/reference/commands/apply/)
- [chezmoi global flags](https://www.chezmoi.io/reference/command-line-flags/global/)

## Decision

1. A recipe declares for each template file whether an existing file is kept or fails the cook, and keeping is the
   default. The points below govern a file declared to fail.
2. Cooking a recipe the first time fails when such a file exists. It writes nothing, and one refusal names every
   colliding file with its recipe.
3. The caller chooses for the whole command: `--force` writes the template over the file, and `--skip` leaves the file
   and records the recipe in `cook.lock`. The two exclude each other and act only the first time the recipe is
   cooked, so later runs leave the file to whoever edits it.
4. Cook reports one status line per file, under `--dry-run` too, so a replacement is visible before it happens.

## Reasons

- **1.** A template file's worth to its author differs by file (Context): replacing a team's prose loses work, while
  keeping a placeholder loses nothing. The author knows which file is which, as Copier's `skip_if_exists` lets a
  template say. The trade is that an author who forgets to declare a file leaves it kept without a word.
- **2.** The lock cannot tell a seeded file from a hand-written one on the first cook (Context), so refusing is the
  only default that cannot lose a person's work. Of the tools above that document a default, all but Ansible `copy`
  ask or refuse. Naming every collision in one refusal, as Stow and server-side apply do, means `--force` replaces
  only files the refusal named. The trade is a second invocation.
- **3.** `--force` and `--skip` are the names people know from Rails and cookiecutter, and one choice for the whole
  command keeps the grammar to one option each. The options contradict each other, so giving both fails. They act
  only the first time because afterwards the file belongs to whoever edits it
  ([architecture](../architecture.md#cooking-and-discard)). The trade
  is that one command cannot replace one file and keep another; the template's declaration (point 1) carries per-file
  differences.
- **4.** A person who chooses `--force` needs to see what it replaces before it happens, as Thor reports under
  `--pretend`. The trade is one more line per file.

## Rejected alternatives

### Overwrite by default

At its best, cooking converges on the recipe's setup with no flag and no second invocation, as Ansible `copy` does. It
loses because a file present on the first cook can be a person's work and nothing in the lock says otherwise.

### Ask for each file

At its best, a person decides each collision with its diff in view, as Thor, Yeoman, Copier and chezmoi do. It loses
because cooking also runs where no terminal can answer, and Cook's review is one approval for the whole batch
([architecture](../architecture.md#evaluation-review-and-writes)); a per-file question adds a second approval a
noninteractive run cannot give.

### Only the template decides

At its best, the author who knows each file settles every collision and the caller has no flags to learn, as with
Copier's `skip_if_exists` alone. It loses because the caller knows their repository and the author does not: a refusal
with no way past it forces a person to delete a file by hand before cooking.

## Consequences

Costs:

- Cooking a recipe over an existing file that fails takes a second invocation with a choice.
- One choice covers the whole command, so replacing one file and keeping another takes two invocations.

Must be maintained:

- The refusal that names every colliding file with its recipe.
- The per-file declaration and its default.
- A status line for each file, including under `--dry-run`.

## Confirmation

- Does cooking `acme:notes#starter` over an existing `NOTES.md` fail, write nothing and name the file and the recipe?
- Does `--force` replace the file and `--skip` keep it and record the recipe in `cook.lock`, and does giving both fail?
- Does a dry run with either option report `Would replace` or `Would keep` and leave the file unchanged?
- Does a second cook of the same recipe leave the file as its editors left it?
