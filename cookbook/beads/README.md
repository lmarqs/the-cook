Beads (`bd`) keeps a project's issues in the repository's clone, with dependencies that tell people and agents which
work is ready. This artifact sets a repository up around what `bd init` writes: `#tracker` writes the ignore rules for
the issues that stay out of commits and the prime text agents read at session start, and connects that text to Claude
Code and Codex directly or through the agents layout's session-start hook. bd writes and owns its own configuration,
hooks and database.
