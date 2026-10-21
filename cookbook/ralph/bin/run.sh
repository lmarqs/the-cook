#!/bin/sh
# Runs ralph-tui in the worktree .worktrees/ralph/<name>, on the branch ralph/<name>.
# Written by io.github.lmarqs.cookbook:ralph#ralph-tui; cook it again to update it.
set -eu

# The file --worktree-setup names, by its path from the repository root; empty without --worktree-setup.
setup=''

usage() {
  echo 'usage: .ralph-tui/bin/run <name> (--epic <id> | --prd <file>) [--base <ref>] [ralph-tui run arguments...]' >&2
  exit 2
}

fail() {
  printf 'run: %s\n' "$1" >&2
  shift
  for line in "$@"; do printf '  %s\n' "$line" >&2; done
  exit 1
}

root=$(CDPATH='' cd -- "$(dirname -- "$0")/../.." && pwd)

[ $# -ge 1 ] || usage
name=$1
shift
case $name in '' | -*) usage ;; esac
branch=ralph/$name
git check-ref-format --branch "$branch" >/dev/null 2>&1 ||
  fail "$name is not a run name" "ralph/$name must be a valid branch name"

epic='' prd='' base=HEAD
count=$#
while [ "$count" -gt 0 ]; do
  argument=$1
  shift
  count=$((count - 1))
  case $argument in
    --epic | --prd | --base)
      [ "$count" -gt 0 ] || fail "$argument needs a value"
      value=$1
      shift
      count=$((count - 1))
      ;;
    --epic=* | --prd=* | --base=*)
      value=${argument#*=}
      argument=${argument%%=*}
      ;;
    *)
      set -- "$@" "$argument"
      continue
      ;;
  esac
  case $argument in
    --epic) epic=$value ;;
    --prd) prd=$value ;;
    --base) base=$value ;;
  esac
done

if [ -n "$epic" ] && [ -n "$prd" ]; then
  fail '--epic and --prd are both given' 'a run takes its tasks from one tracker' 'give one of them'
fi
[ -n "$epic" ] || [ -n "$prd" ] || usage

if [ -n "$prd" ]; then
  [ -f "$prd" ] || fail "$prd is not a file" 'the --prd file is read from where run starts'
  prd=$(CDPATH='' cd -- "$(dirname -- "$prd")" && pwd)/$(basename -- "$prd")
fi

if [ -n "$(git -C "$root" status --porcelain -- .ralph-tui)" ]; then
  fail '.ralph-tui has uncommitted changes' 'a worktree holds only what is committed, so the run would not see them' \
    'commit .ralph-tui, then run again'
fi

worktree=$root/.worktrees/ralph/$name
if [ -e "$worktree/.git" ]; then
  current=$(git -C "$worktree" symbolic-ref --quiet --short HEAD || true)
  [ "$current" = "$branch" ] ||
    fail "$worktree is not on $branch" "it is on ${current:-a detached HEAD}" 'run .ralph-tui/bin/clean first'
  echo "Reusing the worktree $worktree on $branch."
else
  if git -C "$root" show-ref --verify --quiet "refs/heads/$branch"; then
    git -C "$root" worktree add --quiet "$worktree" "$branch"
  else
    git -C "$root" worktree add --quiet -b "$branch" "$worktree" "$base"
  fi
  echo "Created the worktree $worktree on $branch."
  if [ -n "$setup" ]; then
    retry="fix it, then run .ralph-tui/bin/clean $name and run again"
    [ -f "$worktree/$setup" ] ||
      fail "$setup is not in $worktree" 'a worktree holds only what is committed' "$retry"
    [ -x "$worktree/$setup" ] || fail "$setup is not executable" "in $worktree" "$retry"
    (cd "$worktree" && "./$setup") || fail "$setup failed" "in $worktree" "$retry"
  fi
fi
echo "worktree: $worktree"
echo "branch: $branch"

cd "$worktree"
if [ -n "$epic" ]; then
  exec ralph-tui run --epic "$epic" "$@"
fi
exec ralph-tui run --prd "$prd" "$@"
