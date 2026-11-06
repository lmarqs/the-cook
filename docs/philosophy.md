---
description: States why Cook exists, who it is for, what they get, what it is and is not, and why a skill alone does not do it.
audience: Whoever wants to know why Cook exists, or describes it to someone who has not used it
---
# Philosophy

Teams and developers reuse configuration across repositories, and copying it by hand makes improvements hard to share,
changes hard to review and unrelated configuration easy to lose. This page states that problem, who has it, what Cook
gives them and what it is and is not, each claim with the page that backs it, so every description of Cook, the README,
the site or a talk, says the same thing and a reader can check it. [Contributing](contributing.md) gives the tenets a
proposal for Cook is judged against.

## The problems

Teams share repositories, while each person uses their own editors, agents, loop runners and task trackers. These
programs consume native files, often inside the repository. Reusing their setup raises several practical problems:

- A useful configuration change reaches another repository only when someone copies it, and the copies drift.
- Replacing a shared file can erase unrelated settings or changes someone made by hand.
- Hidden changes make it hard to judge what cooking or discarding setup will do.
- Coupling program-specific configuration to the engine makes each program's evolution an engine concern.
- Several agents and people work in one repository at once, each in their own working copy, and each agent harness
  reads its own native files. A setup kept by hand differs between harnesses and between working copies.

## The goals

- Reuse configuration through [recipes](features/glossary.md) and ingredients maintained in cookbook repositories.
- Review the combined effect of the requested changes against current content before approving writes.
- Preserve unrelated content when cooking or discarding recipes.
- Let configured programs consume their native files directly.
- Keep the engine's responsibilities bounded so cookbooks can evolve separately.
- Give every agent harness and every working copy the same reviewed setup from what the repository commits.

## Who it is for

Each organization has its own needs, and recipes exist so each cooks what it needs. Three situations show the range,
in the made-up `acme`:

| Situation | What they cook |
| --- | --- |
| A team sets `acme/api` up so people and coding agents work in it together, and does it again for `acme/web` and `acme/infra` as projects multiply | The published `agents`, `git`, `docs` and `ralph` artifacts ([cookbook](../cookbook/README.md)) |
| One developer keeps many projects and wants the same setup in each, and the improvements made in one to reach the rest | The same artifacts, cooked from a `cook.lock` carried between projects |
| A platform group standardises an organization through conventions no public cookbook holds | Its own recipes, cooked by path first and then from its own cookbook ([write a recipe](write-a-recipe.md)) |

People and agents are one class of worker here. A person works through an editor, an agent through a harness, a
subagent through its parent, and each reads its own native files. The setup reaches all of them, and each keeps the
program they chose ([0028](adrs/0028-place-of-the-tool.md)).

## What they get

| Claim | What backs it |
| --- | --- |
| Onboard on day one: a fresh clone carries the setup, since the links each harness reads are committed, and `replicate` rebuilds what git ignores | [cookbook](../cookbook/README.md#what-runs-today), [replicate](features/replicate.md) |
| Work in parallel: every branch gets a worktree at `.worktrees/<branch>` with the setup that branch commits | [cookbook](../cookbook/README.md#what-runs-today) |
| Make it yours: a recipe owns only the sections, keys and files it writes, and personal settings sit beside the team's | [sections and markers](features/cook.md#sections-and-markers), [0008](adrs/0008-native-configuration.md) |
| Do it again: the setup is a list of recipes at exact versions, recorded in `cook.lock`, and the next repository cooks the same list | [0029](adrs/0029-cookbook-distribution.md), [0007](adrs/0007-ownership.md) |
| Grow it: add a recipe, move it to a newer release within a range you chose, or discard it, and each change is one reviewed diff | [update](features/update.md), [discard](features/discard.md), [review and recovery](features/cook.md#review-and-recovery) |

## In the lab's words

A pitch opens in technical terms and then introduces the lab, the image the name and the site carry. Each term has one
translation:

| Technical term | In the lab |
| --- | --- |
| A recipe, and the cookbook that ships it | The formula |
| Replicate: the same recipes at the same versions give the same files in every clone | The same purity in every batch, in every lab |
| Drift: a hand edit or a copy that no longer follows from the record, caught by `check` | Contamination |
| A worktree per branch, one worker in each | A bench, and the suit that keeps one worker's batch off another's |
| The diff and the one approval before anything is written | The office |
| Cookbooks arriving as versioned jars | The conveyor |

## What Cook is

A configuration manager for repositories, with a reviewed diff. Configuration management converges a machine to the
state its recipes declare; Cook converges a repository's native files to what its recipes declare, and shows the
change for approval instead of converging unattended. The words recipe, cookbook and kitchen are Chef's, applied to a
repository ([glossary](features/glossary.md)).

Its approach is deterministic where it can be and judgment where it must. Recipes carry the mechanical part: files,
links, hooks, scripts and the skills themselves, written as one reviewed diff. The skills carry the part that needs
judgment, such as writing the docs map with the team, adopting the agents layout or reviewing pages, done afterwards
by an agent guided by them. The files Cook writes are not the setup; they produce it
([where new behavior goes](ux/when-cli-when-skill.md)).

## Why not a skill

An agent following a skill sets a repository up once, by judgment, and that serves one repository on one day. The
[division of work](adrs/0002-division-of-work.md) rejected setup by an agent for what it loses at scale, across
repositories, harnesses and upgrades:

| Cook | An agent with a skill | What backs it |
| --- | --- | --- |
| Reproducible: one recipe at one version with the same arguments gives the same files in every repository, and `replicate` rebuilds them in a fresh clone | Ten repositories set up from one skill are ten setups | [0007](adrs/0007-ownership.md), [replicate](features/replicate.md) |
| Provable: `check` fails CI when the committed files no longer follow from the lock, with no agent, model or credentials | No check says an agent did it right | [0031](adrs/0031-drift-check.md), [check](features/cli.md#check) |
| Reversible: Cook marks what it owns, and `discard` removes exactly that | Nobody can later say which lines were the setup and which the team's | [0007](adrs/0007-ownership.md), [discard](features/discard.md) |
| Upgradable: cook the newer release and read one diff per repository, within a compatible range | Run the agent again in every repository and reconcile by eye | [0032](adrs/0032-recipe-updates.md), [update](features/update.md) |
| Reviewed as one change: one diff, one approval, rollback when a write fails | Dozens of tool calls, each its own edit | [0002](adrs/0002-division-of-work.md), [review and recovery](features/cook.md#review-and-recovery) |
| Free of the agent: the output is native files that a teammate without Cook, a session without an agent and CI all read | The next change needs the agent again | [0010](adrs/0010-dependence-on-the-binary.md) |

What the skill keeps, Cook hands to it on purpose: fitting conventions to one team, weighing options and writing
guidance. The impact grows with the number of repositories times harnesses times upgrades.

## What Cook is not

| Not | Why it may look like one | Where it is decided |
| --- | --- | --- |
| A coding agent | It ships skills and configures agents; it runs no model and holds no credentials | [0001](adrs/0001-role-of-the-tool.md) |
| A task runner or CI | A recipe writes hooks, scripts and CI files; the program that owns them runs them, and a batch executes no command | [0019](adrs/0019-action-kinds.md) |
| A lifecycle controller | It configures loop runners and worktrees; it imposes no stages and supervises nobody | [0001](adrs/0001-role-of-the-tool.md), [0028](adrs/0028-place-of-the-tool.md) |
| A task tracker | A recipe may set a tracker up; the tracker keeps the tasks | [0028](adrs/0028-place-of-the-tool.md) |
| A package or dependency manager | Cookbooks are versioned, fetched and locked, like packages; what they manage is setup, not the code's dependencies | [0029](adrs/0029-cookbook-distribution.md) |

Neighbours a reader may reach for instead: a tool manager such as mise or asdf puts tools on `PATH` and is a runtime
dependency of every developer, where Cook's output needs no Cook, and mise is one way to install Cook itself
([install](features/install.md#pinning-a-release-for-a-team)); a project template or generator scaffolds once and
never updates or removes what it made; a dotfile manager configures one person's home, not a repository a team and
its agents share.
