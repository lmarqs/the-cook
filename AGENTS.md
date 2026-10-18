# Agent instructions

This repository builds the-cook, a recipe driven configuration tool. Read
`README.md`, the relevant page under `docs/features/` and the matching
standard under `docs/ux/` before changing a workflow or the command line. Keep cookbook recipes and core, infra or CLI changes in their
existing modules.

Run the focused module check while developing. Before delivery, run
`./gradlew check cliBuild jpackageTest` and `git diff --check`.
Review generated configuration and ownership records after changing recipes.

Write commit subjects as `<type>(<area>): <sentence>`, where `<area>` is the module or
topic, such as `core`, `cli` or `docs`. The type decides the next release:

| Type | Release |
| --- | --- |
| `feat` | minor |
| `fix` | patch |
| `docs`, `refactor`, `test`, `build`, `ci`, `chore` | none |

A `!` after the area, as in `feat(core)!:`, releases a major version. Lint-only changes
use `chore(lint)`.
