---
layout: post
title: "Versioning work notes with git on Mac"
date: 2014-12-05 14:36
comments: true
categories: Mac OSX notes git
---

I started to keep a loose log of todo's and related useful information at work.
This log is a simple text, file which I use in a append-only fashion.
New information gets added at the top, using markdown notation.

Our company provides we with a Google Account, so I put my notes into
Google Drive to back it up. You can choose Dropbox or a private git repository
for hat matter.

But having just a plain file, makes it hard to keep track of when an entry was
made. So I started to use git to take care of the versioning for me.
I still keep my files in Google Drive and do not push the git repository, 
because I'm only
interested in the versioning, and not in collaboration of backup.

However I would like for commits to happen on a regular basis instead of me
having to remember to also make a commit after changing a file.

Since the usage of cron is discouraged on mac I looked into using apples 
`launchctr deamon`.

Launch Control works by loading job descriptions from `.plist` files, which
are noting more than xml files, which are easy enough to write my hand.

You will find your Launch Control files in `~/Library/LaunchAgents`.

Here is the plist file I use for my notes `velrok.gittify-notes.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>velrok.gittify-notes.plist</string>

    <key>WorkingDirectory</key>
    <string>/Users/velok/Google Drive/notes</string>

    <key>WatchPaths</key>
    <array>
        <string>/Users/velok/Google Drive/notes</string>
    </array>

    <key>ProgramArguments</key>
    <array>
        <string>git</string>
        <string>commit</string>
        <string>-a</string>
        <string>-m</string>
        <string>"snapshot"</string>
    </array>

    <key>ProcessType</key>
    <string>Background</string>
</dict>
</plist>
```

`WorkingDirectory` sets the working directory for all the program calls, which
makes it very convenient to write out the necessary git calls.

`WatchPaths` defines an array of path to watch for changes, which is a
perfect fit for my notes versioning problem, because it facilitates the triggering
of commits every time I save, but not during the long hours I might not change 
anything at all.

`ProgramArguments` is essentially the command line call separated in its components.
I'm simply adding all the changed files and create a commit with a fixed message.

You will obviously have to have your notes directory initialized as a 
git repository `git init`.

If it also contains files that you do not wish to have included I would recommend
adding them to a `.gitignore` file.

To actually register this job and have Launch Control run it you have to load
your file `launchctl load velrok.gittify-notes.plist`.
To stop execution of your script you need to unload it 
`launchctl unload velrok.gittify-notes.plist`.

Now you should have your git versioned directory of plain text notes
running and the launchctrl daemon trigger new commits every time you save
your notes file. There is a slight delay of a couple of seconds between the
save and the actual commit. Just run "git log" in your notes directory to see
a log of commits.
A call to `git blame notes.md` will show you the file contents with
time stamps next to each line, so you know, when they changed last.
