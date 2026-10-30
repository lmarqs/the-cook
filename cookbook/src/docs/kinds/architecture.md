## Architecture, `architecture.md`

Holds how the parts of the system fit together: the shape across its directories that no single file shows. Diátaxis
type: explanation.

- One page. The lead says what the system is and where its boundary is.
- Sections: `Parts`, each part with its responsibility and where it lives in the repository; `How they connect`, the
  dependencies and main flows, with diagrams where they help.
- Each structural choice links its record in `adr/`; the page states no reasons for them. Interface rules, behavior and
  requirements live in their own kinds.
