Beads (`bd`) keeps a project's issues in the repository's clone, with dependencies that tell people and agents which
work is ready. This artifact sets a repository up around what `bd init` writes: `#tracker` writes the ignore rules for
the issues that stay out of commits, the prime text agents read at session start, and, where the person names a path,
a session-start script that prints that text for any runner that adds a script's output to the session. bd writes and
owns its own configuration, hooks and database.
