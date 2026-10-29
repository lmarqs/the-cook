# Checks the repository's docs/ tree against the map's criteria and reports each finding with the step that fixes it.
# Run from the repository root, the directory that holds docs/README.md: sh <skills>/docs-layout/scripts/check.sh
# Writes nothing. Prints "ok <what>" for each check that passes, and one line per finding:
# "FAIL <path>[:<line>] <what> -- <next>" or "WARN <path>[:<line>] <what> -- <next>".
# A FAIL is a broken link or a page outside its kind's shape; a WARN is wording to review. Exits 1 on any FAIL.
# Prose means lines outside fenced code and HTML comments, with inline code and link targets removed.
# An anchor with non-ASCII letters passes unverified: awk lowercases them only in some versions and locales.

if [ ! -f docs/README.md ]; then
  printf 'FAIL %s -- %s\n' 'docs/README.md, the map, is missing' \
    'run from the repository root, or cook io.github.lmarqs.cookbook:docs#seed'
  exit 1
fi

# The kinds the map offers: the --kinds on the argument line the recipe wrote, or all five.
kinds=$(tr -d '\r' <docs/README.md | sed -n 's/^<!-- io\.github\.lmarqs\.cookbook:docs#seed \(.*\) -->$/\1/p' |
  head -n 1 | tr ' ' '\n' | sed -n 's/^--kinds=//p' | tr '\n' ' ')
[ -n "$kinds" ] || kinds='adrs api features runbooks ux'

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
    function unlinked(text,   out, link) {
      out = ""
      while (match(text, /!?\[[^]]*\]\([^)]*\)/)) {
        link = substr(text, RSTART, RLENGTH)
        sub(/^!?\[/, "", link)
        sub(/\]\(.*$/, "", link)
        out = out substr(text, 1, RSTART - 1) link
        text = substr(text, RSTART + RLENGTH)
      }
      return out text
    }
    function heading(text, level, at,   s) {
      sub(/^[ \t]+/, "", text)
      sub(/[ \t]+$/, "", text)
      text = unlinked(text)
      gsub(/`/, "", text)
      s = slug(text)
      count[FILENAME, s]++
      if (count[FILENAME, s] > 1) s = s "-" (count[FILENAME, s] - 1)
      printf "S\t%s\t%s\n", FILENAME, s
      printf "H\t%s\t%d\t%d\t%s\n", FILENAME, at, level, text
    }
    { sub(/\r$/, "") }
    FNR == 1 { fence = ""; comment = 0; para = ""; front = ($0 == "---"); if (front) next }
    front { if ($0 == "---" || $0 == "...") front = 0; next }
    {
      line = $0
      if (fence != "") {
        if (line ~ ("^ ? ? ?" fence "[" substr(fence, 1, 1) "]*[ \t]*$")) fence = ""
        next
      }
      if (line ~ /^ ? ? ?(```|~~~)/) {
        fence = line
        sub(/^ */, "", fence)
        match(fence, substr(fence, 1, 1) == "`" ? "^`+" : "^~+")
        fence = substr(fence, 1, RLENGTH)
        para = ""
        next
      }
      if (comment) { if (line ~ /-->/) comment = 0; next }
      if (line ~ /^[ \t]*<!--/) { if (line !~ /-->/) comment = 1; para = ""; next }

      if (para != "" && line ~ /^ ? ? ?(=+|-+)[ \t]*$/) {
        heading(para, line ~ /=/ ? 1 : 2, FNR - 1)
        para = ""
        next
      }
      para = ""
      if (line ~ /^ ? ? ?##?#?#?#?#?[ \t]/) {
        text = line
        sub(/^ */, "", text)
        match(text, /^#+/)
        level = RLENGTH
        text = substr(text, level + 1)
        sub(/[ \t]+#+[ \t]*$/, "", text)
        heading(text, level, FNR)
      } else if (line ~ /^[^ \t]/ && line !~ /^([-*+] |[0-9]+[.)] |>|\||<)/) para = line

      gsub(/`[^`]*`/, "", line)
      if (line ~ /^ ? ? ?\[[^]^][^]]*\]:[ \t]/) {
        sub(/^ ? ? ?\[[^]]+\]:/, "", line)
        printf "L\t%s\t%d\t%s\n", FILENAME, FNR, target(line)
        next
      }
      rest = line
      while (match(rest, /\]\([^)]*\)/)) {
        printf "L\t%s\t%d\t%s\n", FILENAME, FNR, target(substr(rest, RSTART + 2, RLENGTH - 3))
        rest = substr(rest, RSTART + RLENGTH)
      }
      prose = line
      gsub(/\]\([^)]*\)/, "]", prose)
      printf "P\t%s\t%d\t%s\n", FILENAME, FNR, prose
    }
  '
}

{ scan || printf 'E\n'; } | awk -F '\t' -v kinds="$kinds" '
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
    if (p in scanned) return 1
    if (!(p in present)) present[p] = (system("test -e " quoted(p)) == 0)
    return present[p]
  }
  function word(text, w,   pos, at, before, after) {
    pos = 0
    while ((at = index(substr(text, pos + 1), w)) > 0) {
      pos += at
      before = pos > 1 ? substr(text, pos - 1, 1) : " "
      after = substr(text, pos + length(w), 1)
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
  $1 == "E" { unread = 1 }
  END {
    if (unread) fail("docs/", "holds a page the check could not read", "make each page readable, then run the check again")
    for (i = 1; i <= nl; i++) {
      t = ltarget[i]
      if (t == "" || t ~ /^[A-Za-z][A-Za-z0-9+.-]*:/) continue
      anchor = ""
      if (index(t, "#")) { anchor = substr(t, index(t, "#") + 1); t = substr(t, 1, index(t, "#") - 1) }
      if (t == "") p = lfile[i]
      else if (t ~ /^\//) p = normal(substr(t, 2))
      else p = normal(dir(lfile[i]) "/" t)
      if (p == "") p = "."
      where = lfile[i] ":" lline[i]
      if (t != "" && !exists(p)) { fail(where, "link " ltarget[i] " points at a missing file", "point it at an existing file, or write the page it names"); continue }
      linked[lfile[i], p] = 1
      ascii = anchor
      gsub(/[ -~]/, "", ascii)
      if (anchor != "" && ascii == "" && (p in scanned) && !((p, anchor) in slugs))
        fail(where, "link " ltarget[i] " points at a missing heading", "use the heading slug: lowercase, punctuation removed, spaces as hyphens")
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
      if (kind(p) != "adrs" || p == "docs/adrs/README.md" || p !~ /^docs\/adrs\/[^\/]+$/) continue
      name = substr(p, 11)
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
          if (term != "" && !(term in avoided)) { avoided[term] = preferred; avoid[++na] = term; folded[na] = tolower(term) }
        }
      }
    }
    for (i = 1; i <= np; i++) {
      where = pfile[i] ":" pline[i]
      t = ptext[i]
      lower = tolower(t)
      if (word(lower, "currently")) warn(where, "says \"currently\"", "state what is true now, plainly")
      if (word(lower, "legacy")) warn(where, "says \"legacy\"", "name the thing precisely")
      if (word(t, "TODO")) warn(where, "says \"TODO\"", "write it now, or track it in an issue")
      if (word(t, "TBD")) warn(where, "says \"TBD\"", "write it now, or track it in an issue")
      if (pfile[i] == "docs/glossary.md") continue
      for (j = 1; j <= na; j++) if (word(lower, folded[j])) warn(where, "uses \"" avoid[j] "\", a synonym the glossary rejects", "write \"" avoided[avoid[j]] "\"")
    }
    ok("the prose under docs/ uses the glossary terms and states what is true now")

    for (i = 1; i <= np; i++) if (index(ptext[i], "[[fill:")) warn(pfile[i] ":" pline[i], "has an unfilled blank [[fill:", "write the text the blank asks for")
    ok("every template blank is filled")

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
