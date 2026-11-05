Git keeps a repository's shared work separate from the local files its tools leave behind. When several agents and
people work in one repository at once, each needs a checkout of its own, and everyone needs to find it and prepare it
the same way. This artifact gives the repository one ignored root for its linked worktrees, `.worktrees/<branch>`, with
a setup and a teardown script the team fills in once. To set it up, cook `#worktrees`, whose help says what it writes
and how to state the rule where agents read it.
