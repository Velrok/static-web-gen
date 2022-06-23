<header>
# The beauty of text UIs
<time class="article-date" date="2022-06-23">2022-06-23</time>
</header>

In a [previous post](https://velrok.github.io/post/2021-06-26-command-line-tool-renaissance.markdown.html) I've listed some of the new command line interface (cli) tools I started to use.
Many of which are mainly faster or provide some extra highlighting.

However a few of the tools were interactive. I since learned that these go under the name
text user interfaces (TUIs). I guess you could argue that vim and nano where some of the first 
in this field :) .


My previous list already included TUIs for git
([lazygit](https://github.com/jesseduffield/lazygit)) and data sheets
([visidata](https://www.visidata.org)).

Since than I discovered a whole lot more as well as some framework to help write them.

## More TUIs

[glow](https://github.com/charmbracelet/glow) will render markdown on the command line for you.
Obviously it cant change the font size, but it introduces text highlights and formatting and special 
characters that make reading markdown on the command line easier.

[slides](https://github.com/maaslalani/slides) is something I'm going to try next, as I'm about to give
a quick presentation at work about TUIs and what better way to do so than in the terminal itself :D.

[termdbms](https://github.com/mathaou/termdbms) seams to be a SQL client TUI.

There are many more I'm still discovering.

List of interesting tools:

- [Kubelive](https://github.com/ameerthehacker/kubelive) kubernetes cluster management
- [share-cli](https://github.com/marionebl/share-cli) quick temporary file sharing
- [emma-cli](https://github.com/maticzav/emma-cli) node package browser

## TUI frameworks

Glow is written with on top of [bubble tea](https://github.com/charmbracelet/bubbletea/#bubble-tea-in-the-wild) and [bubbles](https://github.com/charmbracelet/bubbles) provides a set of typical UI components.
I have not tried them myself, but it looks exciting.
They also keep a [list of apps](https://github.com/charmbracelet/bubbletea/#bubble-tea-in-the-wild) written in it, which is a great way to discover more TUI apps.

[Ink](https://github.com/vadimdemedes/ink) provides the same component-based UI building experience that React offers in the browser, but for command-line apps.
This one I tried and wrote a hack news TUI for, which was fun and worked well.

[garson](https://github.com/goliney/garson) introduces a lot of structure, but if that works for you it looks like much can be achieved in a few lines.

## Quick DIY

Depending on the use case [fzf](https://github.com/junegunn/fzf) can actually provide a TUI.

I wrote a simple bash script leveraging a jira cli app to first list all tickets, pipe that into fzf, which provides quick fuzzy finding, as well as allowing for arbitrary commands to run in order to produce a preview.
Luckily the jira api also provides a cli view of tickets, which I could leverage as the fzf preview.
Finally on select I open the ticket in the browser to handle more complex interactions:

```bash
#!/usr/bin/env bash

set -e

# brew install ankitpokhrel/jira-cli/jira-cli fzf

ticket=$(jira issue list \
  --columns KEY,STATUS,SUMMARY,RESOLUTION,ASSIGNEE \
  --resolution x \
  --type ~Epic \
  --no-headers \
  --plain \
  | tr -s '\t' \
  | fzf --preview-window=up,70% --preview='jira issue view $(echo {} | cut -d "	" -f 1)' \
  | cut -d "	" -f 1)

jira open $ticket
```


## Bonus

[sttr](https://github.com/abhimanyu003/sttr) not a TUI although it has an interactive component to help 
find string transformations, its main function is to transform text.
This looks super useful for scripting in vim.

