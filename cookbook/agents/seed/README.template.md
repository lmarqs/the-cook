# Shared agent layout

Files every coding agent in this repository reads. Each harness's own directory, such as `.claude/` or `.codex/`,
holds only links into this one, so it can be deleted and recreated.

| Path | Holds | Read by |
| --- | --- | --- |
| `policy.yaml` | The commands agents may run, must ask for or must not run | Each harness, through its rendered settings |
| `adapters/<harness>/` | Each harness's settings, rendered from the policy | `.claude/settings.json`, `.codex/config.toml` |
| `skills/` | One directory per skill, holding `SKILL.md` and its resources | Claude through `.claude/skills`; Codex natively |
| `commands/` | One Markdown file per slash command, or a link to a skill's `SKILL.md` | Claude through `.claude/commands` |
| `hooks/` | Scripts that hook settings run, by their path from the repository root | Linked as `.claude/hooks`, `.codex/hooks` |
| `memory/` | Durable, repository-wide facts not already in `README.md` or `AGENTS.md`; no secrets or personal data | Linked as `.claude/memory`; read when `AGENTS.md` points to it |
