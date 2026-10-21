#!/bin/sh
# Removes the worktree .worktrees/ralph/<name>, every ralph-parallel/* worktree, their branches and ralph/<name>.
# Written by io.github.lmarqs.cookbook:ralph#ralph-tui; cook it again to update it.
set -eu

usage() {
  echo 'usage: .ralph-tui/bin/clean <name> [--force]' >&2
  exit 2
}

fail() {
  printf 'clean: %s\n' "$1" >&2
  shift
  for line in "$@"; do printf '  %s\n' "$line" >&2; done
  exit 1
}

root=$(CDPATH='' cd -- "$(dirname -- "$0")/../.." && pwd)

name='' force=false
for argument in "$@"; do
  case $argument in
    --force) force=true ;;
    -*) usage ;;
    *) [ -z "$name" ] || usage; name=$argument ;;
  esac
done
[ -n "$name" ] || usage
branch=ralph/$name
worktree=$root/.worktrees/ralph/$name

has_branch=false
if git -C "$root" show-ref --verify --quiet "refs/heads/$branch"; then has_branch=true; fi

if [ "$force" = false ]; then
  if [ "$has_branch" = true ]; then
    ahead=$(git -C "$root" rev-list --count "HEAD..refs/heads/$branch")
    if [ "$ahead" -gt 0 ]; then
      fail "$branch has commits that HEAD does not ($ahead)" \
        'a squash or rebase merge leaves them out of HEAD, so clean cannot tell they were delivered' \
        "merge $branch, or run with --force to delete them"
    fi
  fi
  if [ -e "$worktree/.git" ] && [ -n "$(git -C "$worktree" status --porcelain)" ]; then
    fail "$worktree has uncommitted changes" 'they would be lost' 'commit them, or run with --force to delete them'
  fi
fi

parallel=$(git -C "$root" worktree list --porcelain | awk '
  /^worktree / { path = substr($0, 10) }
  /^branch refs\/heads\/ralph-parallel\// { print substr($0, 19) "\t" path }
')
tab=$(printf '\t')
while IFS=$tab read -r parallel_branch parallel_worktree; do
  [ -n "$parallel_branch" ] || continue
  git -C "$root" worktree remove --force "$parallel_worktree"
  git -C "$root" branch --quiet -D "$parallel_branch"
  echo "Removed the worktree $parallel_worktree and the branch $parallel_branch."
done <<EOF
$parallel
EOF

if [ -e "$worktree/.git" ]; then
  git -C "$root" worktree remove --force "$worktree"
  echo "Removed the worktree $worktree."
fi
git -C "$root" worktree prune
if [ "$has_branch" = true ]; then
  git -C "$root" branch --quiet -D "$branch"
  echo "Removed the branch $branch."
fi
rmdir "$root/.worktrees/ralph" 2>/dev/null || true
