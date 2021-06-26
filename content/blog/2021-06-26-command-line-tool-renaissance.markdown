<header>
# Command line tool renaissance
<time class="article-date" date="2021-06-26">2021-06-26</time>
</header>

Over the last few months I've started to modernize many of the typical command
line interface (cli) tools I use. In some cases I replaced graphical user
interface (GUI) apps with simple interactive cli tools.

## List of cli tools

- cat -> [bat](https://github.com/sharkdp/bat) 
- ls -> [exa](https://github.com/ogham/exa) 
- find -> [fd](https://github.com/sharkdp/fd) 
- [fzf](https://github.com/junegunn/fzf) 
- curl -> [httpie](https://httpie.io) 
- Github Desktop -> [lazygit](https://github.com/jesseduffield/lazygit) 
- vim -> [neovim](https://neovim.io) 
- grep -> [rg](https://github.com/BurntSushi/ripgrep) 
- [starship](https://starship.rs/guide/) 
- Excel -> [visidata](https://www.visidata.org)

In addition I found more comprehensive lists from
[Jakub](https://zaiste.net/posts/shell-commands-rust/)
and [Sebastian Witowski](https://switowski.com/blog/favorite-cli-tools).

## Defaults and fullback's

In the case of `ls` and `cat` you might not want to change your mustle memory,
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
