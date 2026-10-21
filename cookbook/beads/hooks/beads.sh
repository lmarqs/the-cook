#!/bin/sh
# .agents/hooks/session-start runs this with the harness name as $1, which bd prime does not need, and hands what it
# prints to the session. A clone without bd or its database starts the session without it, so this never fails.
command -v bd >/dev/null 2>&1 || exit 0
bd prime 2>/dev/null
exit 0
