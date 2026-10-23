# Site

The GitHub Pages site at [lmarqs.github.io/the-cook](https://lmarqs.github.io/the-cook/) shows the docs of one release.
Jekyll renders the repository's Markdown from the root with the plugins GitHub Pages allows, so a link between the
README, `docs/` and the cookbook works on GitHub and on the site alike. This directory holds what turns those pages
into the site:

| Path | Holds |
| --- | --- |
| `_config.yml` | The Jekyll configuration: titles, the excluded source and build files, and `release`, which `prepare` sets |
| `_layouts/` | `default`, every Markdown page with the docs sidebar, and `home`, the landing page |
| `_includes/` | The head, top bar, sidebar and footer the layouts share |
| `_includes/icons/` | Inline SVG icons, `{% include icons/<name>.svg %}`, drawn in the colour of the text around them |
| `_data/nav.yml` | The docs sidebar in reading order, and the title each page shows on the site |
| `assets/` | `site.css`, the lab theme in dark and light schemes; `site.js`, heading anchors, the page contents, copy buttons and source links; `hat.svg`, the icon |
| `index.html` | The landing page, served at `/` |
| `prepare` | Readies a checkout for the build: copies `_config.yml` to the root with the release tag, and gives the root `README.md` front matter so Jekyll renders it as a page |
| `preview` | Builds the working tree with the image `actions/jekyll-build-pages` runs and serves it locally |

The theme reads `docs/assets/the-lab.png`: steel and night for surfaces, hazmat yellow for what to act on, the four
tank colours for categories. Text and headings are IBM Plex Sans; the pixel face, Silkscreen, is kept to short uppercase labels, the brand and numbers, where it stays readable.

Brand icons, such as `github.svg`, `linux.svg` and `claude.svg`, are [Simple Icons](https://simpleicons.org) 16.35.0,
under CC0. The rest are [Lucide](https://lucide.dev) 1.61.0, under the licence in `_includes/icons/LICENSE-lucide`. A
new icon takes the path data from the same package, in the markup of the icons already here.

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

A new Markdown page renders on the site with no front matter. To list it in the sidebar, add its path and title to
`_data/nav.yml`; a page under a listed folder, such as one ADR, marks its entry current through `match`. A link to a
file the site does not render, such as a `.groovy` recipe or a directory, opens on GitHub at the release.
