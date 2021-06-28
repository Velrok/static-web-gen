<header>
# Taking notes with neovim
<time class="article-date" date="2021-06-28">2021-06-28</time>
</header>

I spend a lot of time in my [neovim](https://neovim.io) and as a result it is one
of the fastes ways for me to write down a note, and copy and past some text to
remember later on.
This is specially handy at work, to keep track of a quick Todo list or if I
need to keep hold of some debug log line, a reference link and so on.

## Taking notes

I use [vimwiki](https://github.com/vimwiki/vimwiki), which gives me a global
keybinding to

- edit or create a new file for **today** mapped to `<Leader>w<Leader>w`
- edit or create a new file for **yesterday** mapped to `<Leader>w<Leader>y`
- jump to the wiki index file for more long term note taking mapped to `<Leader>ww`

I've configured vimwiki to create markdown files instead of wiki files:
```vim
let g:vimwiki_list = [{'path': '~/vimwiki/',
      \ 'syntax': 'markdown', 'ext': '.md'}]
```

[vim markdown](https://github.com/plasticboy/vim-markdown) is used to improve
markdown handling in neovim.

## Finding notes

Often times just looking back at yesterdays todolist if enough.

When I'm looking for notes that I might have taken a while ago, I use [telescope](https://github.com/nvim-telescope/telescope.nvim)
to do an interactive full text fuzzy search:

```vim
command! FindNote :Telescope live_grep cwd=~/vimwiki
```

Seting `cwd=~/vimwiki` makes this a global command which will always search my
notes regardless of my current directory.


## Backup

I use Google Drive to sync local file changes to cloud storage.
My `vimwiki` folder in my home directory is actually symlinked to my Google
Drive:
`vimwiki -> Google Drive/vimwiki`

This allows me to use the same setup at work, where the symlink instead points
to the Google File Stream app that allows companies to stream shared files,
keeping my private notes separate from work notes, whilst using the exact same
setup.
