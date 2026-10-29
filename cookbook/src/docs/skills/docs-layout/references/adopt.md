# Adopt the layout

Moves a repository's existing documentation into the layout, page by page, so that each fact ends on the page that
owns it. The person reviews the result as one change.

1. Cook the layout if `docs/README.md` lacks its `kinds` section:
   `./cook io.github.lmarqs.cookbook:docs:<version>#seed --skills=<skills directory> --instructions=AGENTS.md`.
2. List the documentation: every Markdown file under `docs/`, and the files elsewhere that hold the same kinds of fact,
   such as `ARCHITECTURE.md`, `doc/adr/` or a wiki export. Show the list to the person and agree which files are in
   scope.
3. Sort each file by the map's criteria into one kind, or split it when its sections meet different kinds. Ask the
   person where a file meets no kind's criteria; it may belong in the root README, in `AGENTS.md`, in an issue, or
   nowhere.
4. Move each page into its kind with the version control's move command, so history follows it, and rename it by
   the kind's file name rule.
   Existing decision records keep their numbers.
5. Fit each page to its kind's template: the sections the map requires, a lead, the reasoning. Gather missing facts
   from the code and the person, and fill blanks with what they say; leave the person a list of what is still open.
6. Remove what other pages already state: keep the statement on the owning page and link it from the rest. Remove
   status, author, date and version fields; version history records who changed a page and when.
7. Write each kind's guide from its template, listing its pages.
8. Fix the links: the check lists every link that points at a moved file.
9. Run the check until it reports no `FAIL`, and report the moves, the splits, the removed statements and the open
   questions.
