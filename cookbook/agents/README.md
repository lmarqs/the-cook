Shared setup for the coding agents in one repository, so project guidance, permissions and skills stay the same
across every agent program the team uses. The artifact also models the programs it drives, Claude Code and Codex,
and ships the `agents-layout` skill, which explains the layout to the people and agents working in it.
Its `references/design.md` gives the reason behind each decision of the layout, such as committed links, deny-only
roles and no Codex roles.

To set the layout up in a repository, cook `#seed` first: it writes the skill into `.agents/skills/agents-layout/`,
and add `--skip` before the recipe when the repository already has an `AGENTS.md`. Then start the skill's `adopt`
procedure, which connects, fills in, reviews and commits the layout in one commit: `$agents-layout adopt` in Codex,
which reads `.agents/skills/` itself, and `/agents-layout adopt` in Claude Code once `.claude/skills` links to
`.agents/skills`. Until a `#connect --harness=claude` makes that link, ask Claude Code to follow
`.agents/skills/agents-layout/SKILL.md` with the argument `adopt`.
