A team keeps two sets of knowledge, prose for people and context files for agents, and the two drift; a fact stated
on several pages goes stale on some of them, and context loaded up front costs every agent run. Docs gives a
repository one `docs/` tree that people and coding agents both read on demand as the source of truth: each fact has
one owning page, a map at `docs/README.md` says which, and the `docs-layout` skill writes and reviews pages by the
map's criteria.
