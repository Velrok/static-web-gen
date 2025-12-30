# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a static website generator for velrok.github.io built with Clojure. It converts Markdown blog posts to HTML using Hiccup templating, with a live development server that auto-regenerates content on file changes.

## Writing Aid

When asked to review text (blog posts), focus on:
- **Proofreading** - Check spelling and grammar
- **Consistency** - Ensure terminology, style, and voice are consistent throughout
- **Didactic flow** - Evaluate how well concepts are explained and whether ideas progress logically

**Important:** This is a teaching tool for writing skills. Never actually edit text except for:
- Simple spelling and grammar corrections
- Reference corrections
- Small word replacements

For larger structural or content issues, provide feedback and suggestions rather than making direct edits. The goal is to help improve writing skills through guidance, not to rewrite content.

## Development Commands

### Live Development Server
```bash
lein run
```
Starts an HTTP server at http://localhost:4444 (or PORT env variable) with file watching. The server automatically regenerates all static files when content changes in `./content/blog/`.

### Generate Static Files Only
```bash
lein gen
# or
lein run -m static-web-gen.generator
```
Generates all static HTML files without starting the server. Output goes to `./public/` directory.

### Create New Blog Post
```bash
./scripts/new-blog-post.joker <title words>
```
Creates a new markdown file in `./content/blog/` with proper frontmatter. Uses Joker (Clojure interpreter).

### Publish to GitHub Pages
```bash
./scripts/publish.sh
```
Generates static files and publishes to `../velrok.github.io` repository. Requires the sibling repository to exist.

## Architecture

### Component System
Uses Mount for lifecycle management. Components start when `lein run` executes:
1. `static-file-regenerator` (generator.clj) - Watches content and regenerates HTML
2. `http-server` (server.clj) - Serves static files from `./public/`

### Content Processing Pipeline

**Markdown to HTML Flow:**
1. Markdown files in `./content/blog/` are parsed by `parse-blog-post-md`
2. Markdown converted to Hiccup using `markdown-to-hiccup` library
3. Hiccup templates from `./content/layout/*.hiccup.edn` are loaded
4. Content replacement via multimethod `content-replacement` injects blog content into layouts
5. Final Hiccup rendered to HTML via `hiccup.core/html`
6. Output written to `./public/post/<filename>.html`

**Key Transformation Logic (generator.clj):**
- `content-replacement` multimethod handles special DOM transformations:
  - `:div#blog-post` - Injects blog post content
  - `:div#page-index` - Generates blog post index
  - `:a` - Creates link aside annotations for external links
  - `:p` - Manages paragraph extras (asides must be prepended before paragraphs for valid HTML)
  - `:code` - Escapes HTML entities

### File Structure

```
content/
  blog/           - Markdown blog posts (YYYY-MM-DD-title.markdown)
  layout/         - Hiccup EDN templates
    blog-post.hiccup.edn  - Individual post layout
    index.hiccup.edn      - Homepage layout
public/           - Generated static files (git-ignored except .gitkeep)
  css/            - Stylesheets
  post/           - Generated blog post HTML
  index.html      - Generated homepage
src/static_web_gen/
  main.clj        - Entry point, Mount initialization
  generator.clj   - Static file generation logic
  server.clj      - HTTP server (Compojure + http-kit)
  configuration.clj - Config (PORT env var)
```

### Blog Post Format

Markdown files must have this header:
```markdown
<header>
  # Post Title
  <time class="article-date" date="YYYY-MM-DD">YYYY-MM-DD</time>
</header>
```

The generator extracts `:h1` for title and `:time[date]` for sorting.

### Important Implementation Details

- **p-extras atom**: Collects aside elements found within paragraphs during tree walking, then prepends them before the paragraph. This prevents invalid HTML (browsers split `<p>` tags when they contain block elements).
- **File watching**: Uses `clojure-watch` to monitor `./content/blog/` and trigger full regeneration on any change.
- **All-or-nothing regeneration**: The `generate-all!` function rebuilds the entire site (all posts + index) on every change.
- **Link annotations**: External links (except "Mein(un)sin") automatically generate aside elements showing the URL.
