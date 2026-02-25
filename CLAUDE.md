# ChromaDMX — Claude Instructions

Project context, architecture, code style, and testing: see [`AGENTS.md`](AGENTS.md)

## Claude-Specific Workflow

- Use git worktrees for feature work — isolate changes from main
- Use subagents for parallel independent tasks
- Track work via GitHub Issues — create issues before starting features, reference them in commits/PRs
- Never commit directly to main — always use feature branches + PRs
