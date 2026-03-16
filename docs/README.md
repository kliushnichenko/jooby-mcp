# jooby-mcp Documentation Site

This directory contains the source for the [jooby-mcp](https://github.com/kliushnichenko/jooby-mcp) documentation site, built with [Hugo](https://gohugo.io/) and the [Hextra](https://themes.gohugo.io/themes/hextra/) theme.

## Prerequisites

- [Hugo Extended](https://gohugo.io/installation/) (v0.146.0 or later)
- [Go](https://go.dev/dl/) (required for Hugo Modules)

## Local development

**First time** (or after adding/updating the theme): fetch the Hextra theme via Hugo Modules:

```bash
cd docs
hugo mod get -u
```

Commit `go.mod` and `go.sum` so CI and others use the same theme version.

Then run the site:

```bash
hugo server --baseURL=http://localhost:1313/
```

Open **http://localhost:1313/** in your browser. Using `--baseURL` overrides the config (which targets GitHub Pages at `.../jooby-mcp/`) so the site is served at the root locally.

## Build for production

```bash
cd docs
hugo --minify
```

Output is in `docs/public/`.

## Syntax highlighting

The code block colors follow Hextra’s default Chroma styles (light/dark). To use another style (e.g. `monokai`, `nord`):

1. Generate a stylesheet (from the `docs` directory):
   ```bash
   hugo gen chromastyles --style=monokai > assets/css/chroma-monokai.css
   ```
2. In `assets/css/custom.css`, add:
   ```css
   @import "chroma-monokai.css";
   ```
3. Rebuild. For dark mode you can generate a dark style (e.g. `--style=nord`) and scope it under `.dark` if needed.

[Chroma style gallery](https://xyproto.github.io/splash/docs/all.html) lists all style names.

## Deployment

The site is deployed to [GitHub Pages](https://pages.github.com/) via the [Deploy documentation to GitHub Pages](.github/workflows/hugo.yml) workflow. Pushing changes under `docs/` to the `main` branch triggers a build and deploy.

To enable the site: in the repository **Settings → Pages**, set **Source** to **GitHub Actions**.
