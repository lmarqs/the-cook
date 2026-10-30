# Languages

For whoever adds text to the app or answers a member who cannot read a page. Larder is available in Portuguese,
English, Ukrainian and Arabic, and volunteers write the translations in one shared sheet. Written when a new payment
screen went live with English text only and Marta's phone filled with calls from Ukrainian-speaking families.

## Choosing a language

A member picks a language on the first page and can change it from the footer; larder remembers the choice for that
member. Visitors who are not logged in get the language their browser asks for when it is one of the four, and
Portuguese otherwise. Arabic pages are laid out right to left: the page direction is set from the language, so no page
sets it by hand.

## Translating

Every sentence in the app has a key in `larder/messages/`, and the shared sheet has one row per key and one column per
language. Leo and Priya keep the Ukrainian and Arabic columns; Marta checks the Portuguese, and Ana writes the English
first. A developer who adds a sentence adds its row with the English text and a note on where it appears, and the
sheet is exported into the repository before each release.

## Missing translations

A key with no text in a language shows the English text, never the key itself. The release check lists keys that are
empty in any language so that Leo or Priya can fill them in; an empty cell does not stop a release, because a family
reading English is better served than a family waiting for a translation.
