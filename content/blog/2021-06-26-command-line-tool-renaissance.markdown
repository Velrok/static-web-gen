<header>
# Command line tool renaissance
<time class="article-date" date="2021-06-26">2021-06-26</time>
</header>

Over the last few months I've started to modernize many of the typical command
line interface (cli) tools I use. In some cases I replaced graphical user
interface (GUI) apps with simple interactive cli tools.

## List of cli tools

**[bat](https://github.com/sharkdp/bat)**
Replaces _cat_ . Has out of the box syntax highlighting (if unless output is
piped), as well as automated paging.

**[exa](https://github.com/ogham/exa)**
Replaces _ls_. Provides nicer output colouring.

**[fd](https://github.com/sharkdp/fd)**
Replaces _find_. Is a lot faster due to parallel folder traversal.

**[fzf](https://github.com/junegunn/fzf)**
Very fast fuzzy finder. Pipe any line based output to it to have interactive
fuzzy find.

**[httpie](https://httpie.io)**
Replaces _curl_. Provides many conveniences when it comes to specifying prams or
addressing hosts.

**[lazygit](https://github.com/jesseduffield/lazygit)**
Replaces _Github Desktop_. Interactive git management on the command line. Can
be used without a mouse.

**[neovim](https://neovim.io)**
Replaces _vim_. Ships with lua scripting support, integrated LSP interface (from
0.5.0+), lot of fast and async plugins.

**[rg](https://github.com/BurntSushi/ripgrep)**
Replaces _grep_. Fast. Very very fast.

**[starship](https://starship.rs/guide/)**
Fancy prompt.

**[visidata](https://www.visidata.org)**



In addition I found more comprehensive lists from
[Jakub](https://zaiste.net/posts/shell-commands-rust/)
and [Sebastian Witowski](https://switowski.com/blog/favorite-cli-tools).

## Defaults and fullback's

In the case of `ls` and `cat` you might not want to change your muscle memory,
specially since many systems may not have exa and bat installed by default.

So im aliasing them as follows:

```bash
# if bat is available use it instead of cat
if hash bat 2>/dev/null; then
  alias cat="bat"
fi

# if exa is available use it instead of ls
if hash exa 2>/dev/null; then
  alias ls="exa"
fi
```

## Something extra

`fd` and `fzf` are so fast that they allow for new work follows.
I've combined them into a bash function I call `cdcd` which allows quick
directory change via fuzzy find (up to 4 levels deep).


```bash
if hash fd 2>/dev/null; then
  if hash fzf 2>/dev/null; then
    function cdcd {
      # pick a directory via fuzzy find
      dir=$(fd -t directory -d 4 | fzf)
      # cd into it
      cd "$dir"
    }
  fi
fi
```
