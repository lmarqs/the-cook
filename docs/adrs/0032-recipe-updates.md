# 0032. Cook moves a recipe to a newer release on request

Maintainers who reuse shared setup must find newer releases and decide which changes to accept. Requiring them to choose
every exact version by hand makes routine maintenance repetitive, while choosing newer content without their request
changes the setup they trust. Maintainers can request newer shared setup within a known compatibility boundary and
review its effect before accepting it. Their accepted setup remains identifiable independently of later releases.

## Context

- A [recipe](../features/glossary.md) cooked by coordinates has an exact version; cookbook distribution and accepted
  coordinates follow [0029](0029-cookbook-distribution.md). The lock records its source and normalized supplied
  arguments, and replicate repeats those inputs ([0007](0007-ownership.md)).
- Engine upgrades change the engine pin separately from recipe inputs ([0017](0017-engine-version.md)).
- npm's semver 7.7.2 defines caret ranges: `^1.2.3` admits releases below `2.0.0`, `^0.2.3` below `0.3.0`, and
  `^0.0.3` below `0.0.4` ([caret ranges](https://github.com/npm/node-semver/blob/v7.7.2/README.md#caret-ranges-123-025-004)).
- Maven 3.9.11's repository metadata model lists available artifact versions in `versioning/versions`
  ([metadata model](https://maven.apache.org/ref/3.9.11/maven-repository-metadata/repository-metadata.html)).
  Maven-layout repositories publish that listing as `maven-metadata.xml`; Maven local uses
  `maven-metadata-local.xml` for locally published releases.
- A cookbook source directory serves its artifacts at any supplied version from its current files, rather than
  publishing a listing of releases ([0029](0029-cookbook-distribution.md), decision 2).
- `--force` already means replacing an existing template file
  ([CLI standard](../ux/cli-standards.md#prior-art)). Batch approval concerns declared file changes, while trust in
  executable recipe sources precedes evaluation ([0026](0026-cookbook-trust.md)).

## Decision

1. Cook moves a recipe's recorded version only when asked: a caller supplies another exact version when cooking,
   or asks Cook to choose a newer release through `update`. The proposed change goes through cooking's review and
   approval ([0007](0007-ownership.md)); requesting release selection does not itself approve writes.
2. Without `--major`, Cook chooses the newest listed release newer than the recorded version within its compatible
   range: the same major at or above `1.0.0`, or the same `0.minor` below `1.0.0`. This uses npm's caret boundary for
   versions at or above `0.1.0`; Cook also allows newer patches within `0.0`, where npm's caret is narrower.
   `--major` chooses the newest listed release regardless of that boundary. Neither form downgrades a recipe.
3. Available releases come from the listing a Maven-layout repository publishes for the artifact:
   `maven-metadata.xml`, or `maven-metadata-local.xml` in Maven local.
4. A recipe served by a repository without a release listing, such as a cookbook source directory, has no known
   newer release. Cook does not infer one from the ability to serve arbitrary version coordinates.
5. The selected version recorded in the lock remains exact ([0029](0029-cookbook-distribution.md), decision 5).

## Reasons

- **1.** The lock identifies accepted inputs, and the engine pin identifies a different dependency. Requiring a
  request prevents ordinary cooking, replay or an engine upgrade from silently choosing newer recipe code.
  Reusing cooking's review lets maintainers accept the resulting changes, including the new recorded version,
  together. The trade is that requesting an update still requires trusting its sources before evaluation.
- **2.** A bounded default lets maintainers request routine updates without also requesting the next compatibility
  boundary. Below `1.0.0`, holding the minor version keeps the boundary useful before the first major release;
  allowing patch updates within `0.0` keeps that rule uniform, at the cost of differing from npm's strict caret
  behavior there. `--major` makes crossing the boundary explicit. Version numbering is the author's compatibility
  claim, not proof that a recipe preserves output or accepts its previous arguments, so review remains necessary.
- **3.** The metadata model already represents available versions, so using the published listing works with the
  repositories cookbook authors publish to. The trade is reliance on that listing being present and current;
  serving an exact jar alone does not establish what other releases exist.
- **4.** A source directory's ability to serve any version says nothing about a newer release's identity. Treating
  it as having no known newer release avoids inventing versions, at the cost of leaving source edits to ordinary
  cooking or replicate.
- **5.** Exact release identity follows [0029](0029-cookbook-distribution.md), decision 5.

## Rejected alternatives

### Require callers to supply every newer version

At its best, every version choice is explicit and no release discovery is needed. It loses because routine
maintenance requires finding and copying exact versions even when the maintainer already asks for newer setup.
Requested selection retains review of the chosen release and its effect.

### Version ranges in cook.lock

At its best, a range expresses an ongoing compatibility preference without a lock edit for each release. It loses
because replay could choose a later release instead of the one that produced the recorded ownership. Exact
coordinates follow [0029](0029-cookbook-distribution.md), decision 5; the compatibility range governs selection only.

### Cross major versions by default

At its best, each request reaches the latest available setup in one step. It loses because a routine maintenance
request also crosses the author's compatibility boundary. A separate opt-in lets maintainers ask for that broader
change deliberately, including a new minor below `1.0.0`.

### Use --force to cross the compatibility boundary

At its best, the flag is familiar as an override. It loses because `--force` already replaces an existing template
file ([CLI standard](../ux/cli-standards.md#prior-art)); using it for release selection makes one flag express two
independent choices. `--major` names the version-selection choice without changing template behavior.

## Consequences

Costs:

- An update needs release metadata, and an incomplete listing can leave an existing newer release unknown.
- Compatibility boundaries reduce the scope of a request but cannot establish recipe trust or output compatibility.

Must be maintained:

- Release discovery for published Maven metadata and Maven local metadata.
- Selection boundaries, including the same `0.minor` below `1.0.0`, and explicit approval of exact lock changes.
- The separation between engine upgrades, replay of recorded inputs and requested recipe updates.

## Confirmation

- In a worked acme example with recorded `1.2.3` and listed `1.2.4`, `1.9.0` and `2.0.0`, does an update propose
  `1.9.0`, and does `--major` propose `2.0.0`, with the exact version visible in the diff before approval?
- With recorded `0.2.3` and listed `0.2.4`, `0.3.0` and `1.0.0`, is the default `0.2.4` and the opt-in `1.0.0`?
  With recorded `0.0.3` and listed `0.0.4` and `0.1.0`, is the default `0.0.4`?
- Do a dry run and declined approval leave the recorded version and output unchanged, and do ordinary replicate
  and an engine upgrade retain the recorded recipe version?
- Does Maven local discover locally published releases, while a source directory with no release listing reports
  no known newer release?
