ralph-tui works through a tracker's tasks, a Beads epic or a JSON PRD, by running coding agents in a loop. This artifact
sets it up so each run works unattended in a worktree of its own, `.worktrees/ralph/<name>` on the branch
`ralph/<name>`, and connects to worker limits and worktree setup only where the team names them. To set it up, cook
`#ralph-tui`, whose help says what it writes and how to launch a run.
