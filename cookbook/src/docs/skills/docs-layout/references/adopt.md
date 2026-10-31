# Adopt the layout

Brings a repository's existing documentation under a map, so each fact ends on the page that owns it. The person
reviews the result as one change.

1. Write the map with the person, by the skill's `map` procedure, unless one exists.
2. List the documentation: `docs/` and the files elsewhere that hold the same kinds of fact, such as
   `ARCHITECTURE.md`, `doc/adr/` or a wiki export. Agree the scope with the person.
3. Place each file by the map's questions, or split it when its parts fit different kinds. Ask where none fits; the
   answer may be outside `docs/`, a kind the team declares, or nowhere.
4. Move each page with the version control's move command, so history follows it. Records keep their numbers.
5. Edit for facts and owners: keep a statement on its owner and link it from the rest; drop status, author and date
   fields. Leave a page's shape alone unless it fails a reader.
6. Write each kind's guide, fix the links to moved files, and report the moves, splits and open questions.
