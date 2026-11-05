---
description: Records why each engine package ships with a sha256 that installers check, and why that does not prove who published it.
audience: Whoever changes release verification or relies on it
---
# 0021. Published checksums verify package integrity without proving publisher identity

Installing Cook downloads executable engine content that runs with the person's credentials. A replaced or corrupted
download must not silently run as the release the person chose. Detecting a mismatch does not establish that the
release came from its claimed publisher or is free of malicious code. What runs matches the package the release
published, and maintainers understand what that check proves and what trust remains their responsibility.

## Context

- FR-26 in the [architecture requirements](../architecture.md#requirements) selects JVM distribution using `jpackage`:
  complete platform packages containing Java, installed on `PATH` with Homebrew, mise or the release package
  ([0017](0017-engine-version.md)).
- A release publishes each package with its sha256, computed by the build that produced it. The release workflow
  commits the same checksums to the Homebrew formula in this repository, so a formula change is a reviewable commit.
- A checksum detects whether downloaded bytes match the published ones. If a package was already compromised when its
  checksum was computed, that checksum will still match. Whoever can replace both a package and its checksum at their
  source defeats a comparison with that source.
- Download transport and publisher identity are different concerns. A download host's identity alone does not prove
  who authored the executable content.
- Recipes are trusted executable Groovy code. [Ingredients](../features/glossary.md) model native program files and
  configuration and are held and shipped with recipes through cookbook repositories, whose builds can be separate from
  Cook's. Engine package verification does not authenticate those sources or sandbox their evaluation.

## Decision

1. Each release publishes a sha256 beside every platform package.
2. Homebrew checks a download against the checksum in [`Formula/cook.rb`](../../Formula/cook.rb) and refuses a
   mismatch. A person who installs the release package checks it against its `.sha256` before unpacking it
   ([install](../features/install.md)).
3. A matching checksum establishes agreement with the published package, not publisher identity, code safety, or
   protection from an already compromised package. Reviewers remain responsible for the source and release they
   choose.
4. Engine integrity is separate from recipe trust and file-operation approval ([0026](0026-cookbook-trust.md)).

## Reasons

- **1, 2.** A checksum recorded by the build and carried into the formula's commit history catches a replaced or
  corrupted download before it runs. The check covers the complete package: verifying only a launcher would omit the
  Java runtime and other content it needs.
- **3.** Checksums answer an integrity question. Treating them as publisher authentication hides the remaining risk.
- **4.** Engine packages and cookbook sources have separate distribution responsibilities. A trusted engine does not
  make every recipe trustworthy, and approval cannot undo effects outside the declared batch.

## Rejected alternatives

### Pin checksums in every repository

At its best, a repository commits the checksums of the release it reviewed, so a later download is held to that
review rather than to whatever the release currently serves. It loses because it needs a script of Cook's own in every
repository to download, verify and cache the package ([0017](0017-engine-version.md)); a team that wants a reviewed
version pins it in its tool manager.

### Verify publisher signatures

At its best, signed checksum files with embedded public keys, or a signing system with external trust roots,
authenticate the publisher, including on a first download, beyond checksum integrity. It loses because each needs an
initial trust root, key custody, rotation and recovery decisions, none of which the agreed architecture chooses.
Promising their guarantees without those decisions would overstate what verification proves.

## Consequences

Costs:

- A compromised package published with its matching checksum still passes verification. Review and checksum
  verification have distinct roles.
- Verification at install does not protect an installed package from later local modification.

Must be maintained:

- Installed executable packages are protected from unintended changes.
- Maintainers publish corresponding checksums for the complete platform packages they distribute, and the formula
  carries the same ones.
- Cookbook authors retain responsibility for their program models and recipes; the engine release does not manage
  recipe versions.

## Confirmation

- Does every release package have a `.sha256`, and does `Formula/cook.rb` after the release carry the same values?
- Does `brew install cook` refuse a package whose sha256 differs from the formula's?
- Does the [install](../features/install.md) page describe a matching checksum as integrity against the published
  package, with publisher identity and prior compromise left as trust limits, without promising signatures?
