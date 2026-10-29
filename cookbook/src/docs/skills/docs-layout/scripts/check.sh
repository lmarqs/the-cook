# Checks the repository's docs/ tree against the map's criteria and reports each finding with the step that fixes it.
# Run from the repository root: sh <skills>/docs-layout/scripts/check.sh
# Writes nothing. Prints "ok <what>" for each check that passes, and one line per finding:
# "FAIL <path>[:<line>] <what> -- <next>" or "WARN <path>[:<line>] <what> -- <next>".
# A FAIL is a broken link or a page outside its kind's shape; a WARN is wording to review. Exits 1 on any FAIL.
# Prose means lines outside fenced code and HTML comments, with inline code and link targets removed.
# An anchor with non-ASCII letters passes unverified: awk lowercases them only in some versions and locales.

templates=$(cd "$(dirname "$0")/../templates" 2>/dev/null && pwd)

root=$(git rev-parse --show-toplevel 2>/dev/null) || {
  printf 'FAIL %s -- %s\n' 'the check runs inside a git repository' 'run it from the repository root'
  exit 1
}
cd "$root" || exit 1

if [ ! -f docs/README.md ]; then
  printf 'FAIL %s -- %s\n' 'docs/README.md, the map, is missing' 'cook io.github.lmarqs.cookbook:docs#seed'
  exit 1
fi

# The kinds the map offers: the --kinds on the argument line the recipe wrote, or all five.
kinds=$(sed -n 's/^<!-- io\.github\.lmarqs\.cookbook:docs#seed \(.*\) -->$/\1/p' docs/README.md | head -n 1 |
  tr ' ' '\n' | sed -n 's/^--kinds=//p' | tr '\n' ' ')
[ -n "$kinds" ] || kinds='adr api features runbooks ux'

files=$(find docs -type f -name '*.md' | LC_ALL=C sort)

scan() {
  printf '%s\n' "$files" | while IFS= read -r file; do printf 'F\t%s\n' "$file"; done
  # shellcheck disable=SC2086
  printf '%s\n' "$files" | tr '\n' '\0' | xargs -0 awk '
    BEGIN { punct = "!\"#$%&\047()*+,./:;<=>?@[\\]^`{|}~" }
    function slug(text,   s, i, c, out) {
      s = tolower(text)
      out = ""
      for (i = 1; i <= length(s); i++) {
        c = substr(s, i, 1)
        if (index(punct, c)) continue
        out = out (c == " " ? "-" : c)
      }
      return out
    }
    function target(t) {
      sub(/^[ \t]+/, "", t)
      if (t ~ /^</) { sub(/^</, "", t); sub(/>.*$/, "", t) } else sub(/[ \t].*$/, "", t)
      return t
    }
    FNR == 1 { fence = ""; comment = 0 }
    {
      line = $0
      if (fence != "") {
        if (line ~ ("^ ? ? ?" fence)) fence = ""
        next
      }
      if (line ~ /^ ? ? ?```/) { fence = "```"; next }
      if (line ~ /^ ? ? ?~~~/) { fence = "~~~"; next }
      if (comment) { if (line ~ /-->/) comment = 0; next }
      if (line ~ /^[ \t]*<!--/) { if (line !~ /-->/) comment = 1; next }

      if (line ~ /^##?#?#?#?#?[ \t]/) {
        level = index(line, " ") - 1
        if (index(line, "\t") && index(line, "\t") - 1 < level) level = index(line, "\t") - 1
        text = substr(line, level + 2)
        sub(/[ \t]+#+[ \t]*$/, "", text)
        sub(/^[ \t]+/, "", text)
        sub(/[ \t]+$/, "", text)
        gsub(/`/, "", text)
        s = slug(text)
        count[FILENAME, s]++
        if (count[FILENAME, s] > 1) s = s "-" (count[FILENAME, s] - 1)
        printf "S\t%s\t%s\n", FILENAME, s
        printf "H\t%s\t%d\t%d\t%s\n", FILENAME, FNR, level, text
      }

      gsub(/`[^`]*`/, "", line)
      rest = line
      while (match(rest, /\]\([^)]*\)/)) {
        printf "L\t%s\t%d\t%s\n", FILENAME, FNR, target(substr(rest, RSTART + 2, RLENGTH - 3))
        rest = substr(rest, RSTART + RLENGTH)
      }
      if (line ~ /^ ? ? ?\[[^]]+\]:[ \t]/) {
        t = line
        sub(/^ ? ? ?\[[^]]+\]:/, "", t)
        printf "L\t%s\t%d\t%s\n", FILENAME, FNR, target(t)
      }
      prose = line
      gsub(/\]\([^)]*\)/, "]", prose)
      printf "P\t%s\t%d\t%s\n", FILENAME, FNR, prose
    }
  '
}

scan | awk -F '\t' -v kinds="$kinds" -v templates="$templates" '
  function fail(where, what, next_) { printf "FAIL %s %s -- %s\n", where, what, next_; failed = 1; found = 1 }
  function warn(where, what, next_) { printf "WARN %s %s -- %s\n", where, what, next_; found = 1 }
  function ok(what) { if (!found) printf "ok %s\n", what; found = 0 }
  function dir(p) { if (p !~ /\//) return "."; sub(/\/[^\/]*$/, "", p); return p }
  function normal(p,   n, parts, i, out, k, stack) {
    n = split(p, parts, "/")
    k = 0
    for (i = 1; i <= n; i++) {
      if (parts[i] == "" || parts[i] == ".") continue
      if (parts[i] == "..") { if (k > 0) k--; continue }
      stack[++k] = parts[i]
    }
    out = ""
    for (i = 1; i <= k; i++) out = out (i > 1 ? "/" : "") stack[i]
    return out
  }
  function quoted(p) { gsub(/\047/, "\047\\\047\047", p); return "\047" p "\047" }
  function exists(p) {
    if (!(p in present)) present[p] = (system("test -e " quoted(p)) == 0)
    return present[p]
  }
  function word(text, w, fold,   t, pos, at, before, after) {
    t = fold ? tolower(text) : text
    w = fold ? tolower(w) : w
    pos = 0
    while ((at = index(substr(t, pos + 1), w)) > 0) {
      pos += at
      before = pos > 1 ? substr(t, pos - 1, 1) : " "
      after = substr(t, pos + length(w), 1)
      if (before !~ /[A-Za-z0-9_]/ && after !~ /[A-Za-z0-9_]/) return 1
    }
    return 0
  }
  function kind(p,   k) {
    if (p !~ /^docs\/[^\/]+\//) return ""
    k = p
    sub(/^docs\//, "", k)
    sub(/\/.*$/, "", k)
    return (k in offered) ? k : ""
  }
  function sections(file, wanted, label,   n, want, i, have, j, list, seen) {
    n = split(wanted, want, "|")
    list = ""
    for (j = 1; j <= nh[file]; j++) {
      for (i = 1; i <= n; i++) {
        if (heading[file, j] != want[i] || (want[i] in seen)) continue
        seen[want[i]] = 1
        list = list (list == "" ? "" : ", ") want[i]
      }
    }
    have = ""
    for (i = 1; i <= n; i++) have = have (i > 1 ? ", " : "") want[i]
    if (list != have) fail(file, label " sections are [" list "]", "write the sections " have ", in that order")
  }
  BEGIN { nk = split(kinds, kindlist, " "); for (i = 1; i <= nk; i++) offered[kindlist[i]] = 1 }
  $1 == "F" { file[++nf] = $2; scanned[$2] = 1 }
  $1 == "S" { slugs[$2, $3] = 1 }
  $1 == "H" { if ($4 == 2) heading[$2, ++nh[$2]] = $5 }
  $1 == "L" { lfile[++nl] = $2; lline[nl] = $3; ltarget[nl] = $4 }
  $1 == "P" { pfile[++np] = $2; pline[np] = $3; ptext[np] = $4 }
  END {
    for (i = 1; i <= nl; i++) {
      t = ltarget[i]
      if (t == "" || t ~ /^[A-Za-z][A-Za-z0-9+.-]*:/) continue
      anchor = ""
      if (index(t, "#")) { anchor = substr(t, index(t, "#") + 1); t = substr(t, 1, index(t, "#") - 1) }
      if (t == "") p = lfile[i]
      else if (t ~ /^\//) p = normal(substr(t, 2))
      else p = normal(dir(lfile[i]) "/" t)
      where = lfile[i] ":" lline[i]
      if (t != "" && !exists(p)) { fail(where, "link " ltarget[i] " points at a missing file", "point it at an existing file, or write the page it names"); continue }
      linked[lfile[i], p] = 1
      ascii = anchor
      gsub(/[ -~]/, "", ascii)
      if (anchor != "" && ascii == "" && (p in scanned) && !((p, anchor) in slugs))
        fail(where, "link " ltarget[i] " points at a missing heading", "use the heading slug GitHub shows for the target page")
    }
    ok("every link and anchor under docs/ resolves")

    for (i = 1; i <= nf; i++) {
      p = file[i]
      if (p == "docs/README.md" || p == "docs/glossary.md" || kind(p) != "") continue
      fail(p, "sits outside the map, the glossary and the kind directories the map offers", "move it into the kind whose criteria it meets, or offer that kind with --kinds")
    }
    ok("every Markdown file under docs/ sits in the map, the glossary or a kind the map offers")

    for (i = 1; i <= nf; i++) {
      p = file[i]
      k = kind(p)
      if (k == "" || p == "docs/" k "/README.md") continue
      guide = "docs/" k "/README.md"
      if (!(guide in scanned)) { if (!(k in guideless)) fail("docs/" k, "has pages and lacks its guide README.md", "write the guide with the docs-layout skill"); guideless[k] = 1; continue }
      if (!((guide, p) in linked)) fail(p, "is missing from " guide, "add a list item linking it, with one sentence on it")
    }
    ok("every kind directory has its guide, and every page is listed in it")

    for (i = 1; i <= nf; i++) {
      p = file[i]
      if (kind(p) != "adr" || p == "docs/adr/README.md" || p !~ /^docs\/adr\/[^\/]+$/) continue
      name = substr(p, 10)
      if (name !~ /^[0-9][0-9][0-9][0-9]-[a-z0-9]+(-[a-z0-9]+)*\.md$/) { fail(p, "is named outside the pattern NNNN-<slug>.md", "rename it with the next number and a lowercase slug"); continue }
      number = substr(name, 1, 4)
      if (number in numbered) fail(p, "reuses the number " number " of " numbered[number], "give it the next number")
      else numbered[number] = p
      sections(p, "Context|Options|Decision|Consequences", "decision record")
    }
    ok("every decision record is named NNNN-<slug>.md, numbered once, with its sections in order")

    for (i = 1; i <= nf; i++) {
      p = file[i]
      if (kind(p) == "runbooks" && p != "docs/runbooks/README.md") sections(p, "Before you begin|Steps|If it fails", "runbook")
      if (kind(p) == "api" && p != "docs/api/README.md") sections(p, "Contract", "interface page")
    }
    ok("every runbook and interface page has its sections")

    for (i = 1; i <= np; i++) {
      if (pfile[i] != "docs/glossary.md") continue
      t = ptext[i]
      if (match(t, /\*\*[^*]+\*\*:/)) preferred = substr(t, RSTART + 2, RLENGTH - 5)
      if (index(t, "_Avoid_:")) {
        t = substr(t, index(t, "_Avoid_:") + 8)
        n = split(t, terms, ",")
        for (j = 1; j <= n; j++) {
          term = terms[j]
          gsub(/^[ \t]+|[ \t.]+$/, "", term)
          if (term != "" && !(term in avoided)) { avoided[term] = preferred; avoid[++na] = term }
        }
      }
    }
    for (i = 1; i <= np; i++) {
      where = pfile[i] ":" pline[i]
      t = ptext[i]
      if (word(t, "currently", 1)) warn(where, "says \"currently\"", "state what is true now, plainly")
      if (word(t, "legacy", 1)) warn(where, "says \"legacy\"", "name the thing precisely")
      if (word(t, "TODO", 0)) warn(where, "says \"TODO\"", "write it now, or track it in an issue")
      if (word(t, "TBD", 0)) warn(where, "says \"TBD\"", "write it now, or track it in an issue")
      if (pfile[i] == "docs/glossary.md") continue
      for (j = 1; j <= na; j++) if (word(t, term = avoid[j], 1)) warn(where, "uses \"" term "\", a synonym the glossary rejects", "write \"" avoided[term] "\"")
    }
    ok("the prose under docs/ uses the glossary terms and states what is true now")

    for (i = 1; i <= np; i++) if (index(ptext[i], "[[fill:")) warn(pfile[i] ":" pline[i], "has an unfilled blank [[fill:", "write the text the blank asks for")
    ok("every template blank is filled")

    for (i = 1; i <= nk; i++) {
      k = kindlist[i]
      guide = "docs/" k "/README.md"
      template = templates "/guide-" k ".template.md"
      if (!(guide in scanned)) continue
      expected = ""
      while ((getline line < template) > 0) expected = expected line "\n"
      close(template)
      actual = ""
      while ((getline line < guide) > 0) { if (line ~ /^[-*+] /) break; actual = actual line "\n" }
      close(guide)
      sub(/\n+$/, "", expected)
      sub(/\n+$/, "", actual)
      if (actual != expected) warn(guide, "text above the list differs from the release template", "compare it with " template " and take its wording")
    }
    ok("every guide opens with the release template text")

    exit failed
  }
'
status=$?

if [ -f README.md ]; then
  if grep -q 'docs/README\.md' README.md; then
    printf 'ok %s\n' 'the root README links docs/README.md'
  else
    printf 'WARN %s -- %s\n' 'README.md lacks a link to docs/README.md' 'link the map from the root README'
  fi
fi

exit "$status"
