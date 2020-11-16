<header>
# Writing Clojure cli tools with joker
<time class="article-date" date="2020-11-16">2020-11-16</time>
</header>

Recently I've been using [joker](https://joker-lang.org) more and more for writing little cli (command line interface) scripts.

On a Mac it can be quickly installed via Homebrew: `brew install candid82/brew/joker`.

Bash is still my first choice for strait forward sequences of calls, but if it gets more complex, I found an alternative to bash and ruby in joker.

## About joker

Joker is a go based Clojure implementation that started out as a linter, but has added more and more useful functions to its [standard lib](https://candid82.github.io/joker/), which makes it an excellent choice for small command line tools and scripts.
It's standard lib includes support for regular os interaction, as well as parsers and writer for json, yaml and html. Other highlights include libs for uuid and http calls.
This set of functions covers a lot of my day to day automation needs.

With it being implemented in go it's startup time is negligible:

```bash
time joker -e '(+ 2 3)'
5

real  0m0.033s
user  0m0.029s
sys	  0m0.007s
```

## Example

As an example I'd like to submit my script to generate a basic markdown file for a new blog post. It takes a title and creates a file with basic content based on the title and current date.


```clojure
#!/usr/bin/env joker

;; checkout the joker API for a full list of functions: https://candid82.github.io/joker/
(require '[joker.time :as t])
(require '[joker.os :as os])
(require '[joker.string :as string])

;; define a leiningen style -main which takes a variable amount of cli string args
(defn -main
  [& [title]]
  (when-not title
    (println "Need a title as first arg.")
    (os/exit 1))

  (let [t0 (t/now)
        go-format-notation  "2006-01-02" ;; go date format notation is just odd https://programming.guide/go/format-parse-string-time-date-example.html
        date-str (t/format t0 go-format-notation)
        title-str (-> title
                      string/lower-case
                      (string/replace #" " "-"))
        filename (str "./content/blog/" date-str "-" title-str ".markdown")
        content (str
                  "<header>" "\n"
                  "# " title  "\n"
                  "<time class=\"article-date\" date=\"" date-str "\">" date-str "</time>"  "\n"
                  "</header>\n")]
    (println content)
    (println "written to "  filename)
    (spit filename content)))


;; *command-line-args* is a vector of command line args
;; using apply here call it in a way that is similar to the
;; -main used by leiningen
(apply -main *command-line-args*)
```

I hope this was helpful.

Thank you for reading.
