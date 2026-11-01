---
description: Introduces Cook to a newcomer, and says which feature page to open first.
audience: Whoever is new to Cook or about to use or change one of its commands
---
# the-cook

## Overview

Cook evaluates trusted executable Groovy [recipes](glossary.md) into an ordered batch of file operations, shows one
combined terminal diff, obtains approval for the whole batch and performs
the writes. Ingredients model programs' native files, cookbook repositories hold and distribute ingredients and
recipes separately from the engine, and programs consume the resulting files directly. The CLI is the public cooking
interface.

These pages state what each command and operation does, within [Cook architecture](../architecture.md). The
[map of the docs](../README.md) says which kind of page owns which fact; this directory also holds two pages of other
kinds, the [glossary](glossary.md) and the [pitfalls](pitfalls.md). Read the [CLI reference](cli.md) first, then the
page of the operation at hand.

## Pages

| Page | Subject |
| --- | --- |
| [Glossary](glossary.md) | The single list of Cook's terms |
| [CLI reference](cli.md) | Cooking, check, discard, replicate, update and help grammar, recipe operands, recipe arguments and batch approval |
| [Cooking](cook.md) | Ordered file, key and marker contributions, ownership, review and source paths |
| [Discard](discard.md) | Removing what a recipe owns, without evaluating it |
| [Replicate](replicate.md) | Cooking named recipes again from `cook.lock`, including in a fresh clone |
| [Update](update.md) | Choosing newer compatible releases, major updates, release discovery and approval |
| [Wrapper](wrapper.md) | Version and checksum pinning, first-use download and cache, explicit upgrades and the current launcher |
| [Pitfalls, gaps and traps](pitfalls.md) | Symptoms, causes, Cook's responsibility and what to review |
