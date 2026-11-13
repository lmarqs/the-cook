# Site

The GitHub Pages site at [lmarqs.github.io/the-cook](https://lmarqs.github.io/the-cook/) shows the docs of one release.
Jekyll renders the repository's Markdown from the root with the plugins GitHub Pages allows, so a link between the
README, `docs/` and the cookbook works on GitHub and on the site alike. This directory holds what turns those pages
into the site:

| Path | Holds |
| --- | --- |
| `_config.yml` | The Jekyll configuration: titles, the `jekyll-sitemap` plugin, the excluded source and build files, `google_analytics`, the Google Analytics measurement ID that `head.html` loads on every page, `release`, which `prepare` sets, and `defaults`, the descriptions of pages outside `docs/` and the pages kept out of search |
| `_layouts/` | `default`, every Markdown page with the docs sidebar, and `home`, the landing page |
| `_includes/` | The head, top bar, sidebar and footer the layouts share; the head carries the title, description, canonical link, Open Graph and Twitter tags, and the JSON-LD of the page |
| `_includes/icons/` | Inline SVG icons, `{% include icons/<name>.svg %}`, drawn in the colour of the text around them |
| `_data/nav.yml` | The docs sidebar in reading order, and the title each page shows on the site |
| `assets/` | `site.css`, the lab theme in dark and light schemes; `site.js`, heading anchors, the page contents, copy buttons, source links and Mermaid diagrams; `demo.js`, the landing page's demo, which only the `home` layout loads; `hat.svg`, the favicon and the brand mark in the top bar; `the-lab-768.webp` and `the-lab-1536.webp`, the lab at two widths, which the landing page shows in its hero and its problem scenes, and `the-lab-share.jpg`, its 1200x800 link-preview image, all cut from `docs/assets/the-lab.png`, which stays the README's image on GitHub |
| `index.html` | The landing page, served at `/` |
| `prepare` | Readies a checkout for the build: copies `_config.yml` to the root with the release tag, and gives the root `README.md` front matter so Jekyll renders it as a page |
| `preview` | Builds the working tree with the image `actions/jekyll-build-pages` runs and serves it locally |

The theme reads `docs/assets/the-lab.png`: steel and night for surfaces, hazmat yellow for what to act on, the four
tank colours for categories. Text and headings are IBM Plex Sans; the pixel face, Silkscreen, is kept to short uppercase labels, the brand and numbers, where it stays readable.

Brand icons, such as `github.svg`, `linux.svg` and `claude.svg`, are [Simple Icons](https://simpleicons.org) 16.35.0,
under CC0. The rest are [Lucide](https://lucide.dev) 1.61.0, under the licence in `_includes/icons/LICENSE-lucide`. A
new icon takes the path data from the same package, in the markup of the icons already here.

## The landing page's demo

The section under the hero of `index.html` holds a sample project that a visitor changes from a menu of three recipes,
and shows only when JavaScript runs. Cooking a recipe writes its files, updating it brings the lines a newer version
added, and discarding it takes out what it wrote; the project's own lines stay through all three. The editor window shows the result as a developer sees it day to day: the
explorer with Git's status letters, the open file with the recipe that wrote each line, and a terminal with the command
and its diff. A fourth tab opens when every recipe is at its newest version, and links to the start section of the page.

Nothing in the demo runs Cook. `demo.js` holds the sample: each recipe's two versions and the files they write. The
menu's markup, its texts and every icon are in `index.html`, where each card keeps one height in every state. The
commands write without the `Write these changes? [y/N]` prompt the CLI shows, and recipe names are shortened, which
the caption under the demo says.

## Publishing

`.github/workflows/pages.yml` checks out a release tag, runs `site/prepare <tag>` and builds. The `release` workflow
calls it with each new tag; a manual run publishes the tag it names, or the latest release. A change here reaches the
site with the next release.

## Preview

With Docker and Python 3, from the repository root:

```sh
site/preview v6.1.0
```

It builds into `build/site/_site` and serves `http://localhost:4000/the-cook/`. The tag only labels the pages; the
content is the working tree.

## Pages and the sidebar

A new Markdown page renders on the site as it is. The `description` in its front matter, which every page under
`docs/` carries, is the page's description for search engines and link previews. A page outside `docs/` gets its
description from `defaults` in `_config.yml`, and a page with neither gets the site's. The agent-facing skill pages
under `cookbook/src/*/skills` carry `robots: noindex, follow` and `sitemap: false` there, because their titles repeat
across skills. `jekyll-sitemap` writes `sitemap.xml`, which the head links; a project site's `robots.txt` is not read
by crawlers, so submit the sitemap in the search consoles. To list a page in the sidebar, add its path and title to
`_data/nav.yml`; a page under a listed folder, such as one ADR, marks its entry current through `match`.
A ```` ```mermaid ```` code block renders as a diagram in the page's colours, with
Mermaid 11.4.1 loaded from jsDelivr on pages that have one; `site.js` pins its SHA-384, so a new version takes a new
hash. A link to a
file the site does not render, such as a `.groovy` recipe or a directory, opens on GitHub at the release.
