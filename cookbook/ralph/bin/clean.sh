#!/bin/sh
# Removes the worktree <worktrees>/ralph/<name>, its parallel workers' worktrees, their branches and ralph/<name>.
# Written by io.github.lmarqs.cookbook:ralph#ralph-tui; cook it again to update it.
set -eu

# The directory --worktree-root names, by its path from the repository root.
worktrees='.worktrees'
# The file --worktree-teardown names, by its path from the repository root; empty without --worktree-teardown.
teardown=''

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
git check-ref-format --branch "$branch" >/dev/null 2>&1 ||
  fail "$name is not a run name" "ralph/$name must be a valid branch name"
worktree=$root/$worktrees/ralph/$name
# ralph-tui puts a run's parallel workers beside its worktree, in .ralph-worktrees/<name>/.
parallel=$(git -C "$root" worktree list --porcelain | PREFIX="$root/$worktrees/ralph/.ralph-worktrees/$name/" awk '
  /^worktree / { path = substr($0, 10) }
  /^branch refs\/heads\// && index(path, ENVIRON["PREFIX"]) == 1 { print path "\t" substr($0, 19) }
')
tab=$(printf '\t')

# The commits of $branch whose patches $1 lacks, one per line; git cherry matches merged, rebased and cherry-picked
# commits by patch. A git error stops clean, never reads as delivered.
undelivered() {
  cherry=$(git -C "$root" cherry -v --abbrev "$1" "refs/heads/$branch") || exit 1
  printf '%s\n' "$cherry" | sed -n 's/^+ //p' | sed '2,$s/^/  /'
}

has_branch=false
if git -C "$root" show-ref --verify --quiet "refs/heads/$branch"; then has_branch=true; fi

if [ "$force" = false ]; then
  if [ "$has_branch" = true ]; then
    missing=$(undelivered HEAD)
    if [ -n "$missing" ] && git -C "$root" rev-parse --verify --quiet 'refs/remotes/origin/HEAD^{commit}' >/dev/null; then
      missing=$(undelivered refs/remotes/origin/HEAD)
    fi
    [ -z "$missing" ] || fail "$branch has commits that neither HEAD nor origin/HEAD has" "$missing" \
      'a squash merge, or a commit changed while delivering it, such as by resolving conflicts, changes its patch' \
      "deliver $branch, or run with --force to delete them"
  fi
  while IFS=$tab read -r path _; do
    if [ -e "$path/.git" ] && [ -n "$(git -C "$path" status --porcelain)" ]; then
      fail "$path has uncommitted changes" 'they would be lost' 'commit them, or run with --force to delete them'
    fi
  done <<EOF
$worktree
$parallel
EOF
fi

if [ -n "$teardown" ] && [ -e "$worktree/.git" ]; then
  retry="fix it, then run .ralph-tui/bin/clean $name again"
  [ -f "$worktree/$teardown" ] ||
    fail "$teardown is not in $worktree" 'a worktree holds only what is committed' "$retry"
  [ -x "$worktree/$teardown" ] || fail "$teardown is not executable" "in $worktree" "$retry"
  (cd "$worktree" && "./$teardown") || fail "$teardown failed" "in $worktree" 'nothing was removed' "$retry"
fi

while IFS=$tab read -r parallel_worktree parallel_branch; do
  [ -n "$parallel_worktree" ] || continue
  git -C "$root" worktree remove --force "$parallel_worktree"
  git -C "$root" branch --quiet -D "$parallel_branch"
  echo "Removed the worktree $parallel_worktree and the branch $parallel_branch."
done <<EOF
$parallel
EOF
rmdir "$root/$worktrees/ralph/.ralph-worktrees/$name" "$root/$worktrees/ralph/.ralph-worktrees" 2>/dev/null || true

if [ -e "$worktree/.git" ]; then
  git -C "$root" worktree remove --force "$worktree"
  echo "Removed the worktree $worktree."
fi
git -C "$root" worktree prune
if [ "$has_branch" = true ]; then
  git -C "$root" branch --quiet -D "$branch"
  echo "Removed the branch $branch."
fi
rmdir "$root/$worktrees/ralph" 2>/dev/null || true
