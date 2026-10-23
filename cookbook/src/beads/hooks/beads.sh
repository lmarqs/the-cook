#!/bin/sh
# Prints what bd prime prints, for a session-start runner that runs executables and adds their standard output to the
# session. It ignores its arguments. Without bd or its database it prints nothing and exits 0, so the session starts
# without the text rather than failing.
command -v bd >/dev/null 2>&1 || exit 0
bd prime 2>/dev/null
exit 0
