---
layout: post
title: "Clojure Lesson Learned"
date: 2013-09-27 16:50
comments: true
categories: 
---

## background

I started programming Clojure about 5 month ago. So I am fairly new to the
language. I'm using Clojure to write my master thesis, which is a recommendation
engine for advertisement.
When you start to build a recommendation engine stuff like 
[Mahout](http://mahout.apache.org/) can be a big help.
But it's java :(, which is very unproductive to write.
So I looked into jython and jruby, which enable you to write python and ruby
respectively, that runs on the JVM.
Sadly neither did the trick. Jython is missing a simple way to bundle
everything up in one uber.jar while jruby had some issues finding the correct
constructor for one of the mahout classes :(.
So I seized that opportunity to learn Clojure, and I regret not a second of it.

I'm now using Clojure and [Leiningen](https://github.com/technomancy/leiningen).
I chose [Parallel Colt](http://incanter.org/docs/parallelcolt/api/)
as my matrix library, where I use wrapper functions to provide a nicer
Clojure interface.

I use [incanter](http://incanter.org/) for basic data analysis and 
visualisation.


<!-- more -->

## likes

The best thing about Clojure are its persistent and immutable data structures.
Immutable meand that one can not change a basic data structure in place.
If you need to add a thing to a vector you simple call a function
that returns a new vector. 
This is where the persistent part kicks in.
Persistent means that you are guaranteed the same big O properties.
Which is to say adding a element to a vector will always take O(1) time no
matter the size of the vector.
This is also true for all basic Clojure data structures:

- lists
- vectors
- maps
- sets

Here is a very good video with Rich Hickey, the father of Clojure, and
Brian Beckman, where they talk about the inner workings of Clojure.

I can highly recommend this!

<iframe width="420" height="315" src="//www.youtube.com/embed/wASCH_gPnDw" frameborder="0" allowfullscreen></iframe>

Clojure is the first lisp that I am using and I fell in love with
[s-expressions](http://en.wikipedia.org/wiki/S-expression).
They allow for highly flexible code and with the combination of macros
enable anyone to extend the language itself!
Also there are no syntax exceptions or reserved keywords to remember, because
everything is a list and the first argument is interpreted to be a function.
Done. Everything else is based on that rules.


## dislike


Now its not all fun and games. Clojure, however powerful, is still a young
language. So there are some things missing:

- I'm still looking for a good debugger. As far as I know [ritz](https://github.com/pallet/ritz)
is the best thing you can get right now. However it's not trivial to set up 
if you are not using emacs :(.

- Running on the JVM comes with a startup time :( .
- REPL and [tools.namespace](https://github.com/clojure/tools.namespace) to 
the rescue. Stuart Sierra has an [article about his workflow](https://github.com/clojure/tools.namespace)
which only reloads code from files that have changed and those depending on them.
I haven't applied that to my workflow now, because I hadn't had the need. 
My REPL startup time is about 2 sec. so its OK considering I only have to
restart it about 1-3 times a day.


## lessons learned

One of my main motivations to try clojure was to utilize its 
[STM: Software Transactional Memory](http://en.wikipedia.org/wiki/Transactional_memory)
which enables you to use agents and atoms and a lot of concurrent goodness.
So I assumed having to think in concurrent terms would be the big obsticle.

### Immutable data structures rock!

It turns out that the biggest brain wracker during the first 2 weeks where
immutable data structures!
This things force you to think the other way around. You end up writing 
recepies on how to create a new thing by applying a function to each element of
a input instead of writting recepies on how to change something in place.

Lets assum we want to implement the [game of live](http://en.wikipedia.org/wiki/Conway's_Game_of_Life).

In python a update function could look something like this:

```python a posible update world function in python 
def update(world):
  new_world = [] 
  for y, row in enumerate(world):
    new_row = []
    for x, cell in enumerate(row):
      new_row.append(next_cell_state(world, y, x))
    new_world.append(new_row)
  return new_world
```

This function creates a new world and adds stuff to it while it iterates the
input world.
This is typical state manipulating code.

With immutable data structures and functional programming one has to think
differently:

```clojure a posible update world function in clojure 
(defn update [world]
  (map-indext (fn [y row]
                (map-indext (fn [x cell]
                              (next-cell-state world y x))
                            row))
              world))
```

In the Clojure version we use map to apply a function to each row and then
another map to apply another function to each cell. Each map call returns a
sequence that is implicitly build and returned.
At no point in time can we change the input world nor do we change a already
created object.

You can of course write functional code in python using map. However 
pyhton doesn't ensure that you do not change the datastructure in place by 
accident.


### use require instead of use

Namespaces and including and referencing code from other packages was vey
confusing to me, because to do so you can use `(use)` `(require)` and `(import)`.

When applying `use` all the functions are imported into the current namespace:

```clojure
(ns my.core
  (:use [my-lib.core]))

(hello "world")
```

Where `require` lets you alias packages:

```clojure
(ns my.core
  (:require [my-lib.core :as core]))
(core/hello "world")
```

### inject state into functions

One thing that stuck right in my head from the beginning was that everything a 
function operates on should be past in as an argument instead of
using a gloabel var.
Stuard Sierra gave a talk titled 
[Clojure in the Large](http://www.infoq.com/presentations/Clojure-Large-scale-patterns-techniques)
that points out why this is the right way to do things and gives tips and 
examples on how to achive this goal in the large.


### Don't overuse ->> and ->

It took me a while to understand the 
[->](http://clojure.github.io/clojure/clojure.core-api.html#clojure.core/->) 
and 
[->>](http://clojure.github.io/clojure/clojure.core-api.html#clojure.core/->>) 
operations.
You can use thouse to build a pipeline. Which is a beautyfull construct in
many situations. 


```clojure
; using plain clojure calls:
; one has to start at the last call, making this difficult to follow
(filter #(= 0 (rem % 4)) ; only include the ones that are multiple of 4
        (map #(+ 1 %) ; add one to each
             (map #(* 3 %) ; multiply each by 3
                  (filter odd? ; only include the odd ones
                          (range 20))))) ; numbers from 0 to 19
; => (4 16 28 40 52)

; using the ->> pipe, where the result of the last call
; is used as the last argument in the next function
(->> (range 20) 
     (filter odd?)
     (map #(* 3 %))
     (map #(+ 1 %))
     (filter #(= 0 (rem % 4))))
; => (4 16 28 40 52)
```

However when you find yourself writing huge anonymous functions
that draw from previus defined variables for example in a 
[let](http://clojure.github.io/clojure/clojure.core-api.html#clojure.core/let)
it is probably time to reconsider your code :) and use plain let to
store and name intermediat results.


### Test first!

This is and remains true. You shall test first and implement later!

Beeing new to the language the REPL helped me a lot.
I used it to experiment with code snippets quickly. 
Especially when you are not sure how the output of a function looks exactly,
it's nice to play around with it a little in the REPL.

However I missed going back and fixiate this in test cases.
I got lazy and tried to cut corners by skipping the tests.
Gues what it came back to bite me only 2 weeks later. Write tests!

The default Test framework for Clojure seams to be 
[midje](https://github.com/marick/Midje).
However I liked my classical TDD/BDD DSL so I used [speclj](http://speclj.com/).
Running `lein spec -a` on the command line will autotest all the functions and
rerun tests of files that were changed.
It does keep a JVM running so it's fast.
However, every time you remove definitions you need to restart the call, because
it will keep the old definition around. 
So if you still have code that depends on the allegedly removed function your 
test will not fail until you restart the JVM.


### Making things run on multiple cores is easy.

If you use `map` a lot making things run on multiple cores is easy:
just replace `map` with a `pmap`. This will execute the function in parallel using
multiple cores.
While this is a valid and easy step, it is only beneficial if the function
takes some time to run. Otherwise the overhead of `pmap` will midigate the effect
and you end up beeing slower.
So start at the out calls and see how far you can get.

If you need better prallel performance Clojure gives you the new 
[reducers](http://clojure.com/blog/2012/05/08/reducers-a-library-and-model-for-collection-processing.html).
They sacrifice lazyness to give you a fork join abstraction that uses map
reduce semantics.
I did read the article, but did not have the time to experiment a lot with it.

Also [core.async](https://github.com/clojure/core.async)
gives you Go like lightweight threads and channel semantics.
Drew Olson has a nice 
[article comparing clojure core.async with go](http://blog.drewolson.org/blog/2013/07/04/clojure-core-dot-async-and-go-a-code-comparison/).


## vim and clojure

I started to code Clojure using Sublime.
While it has a nice REPL, which shows the last command send to it, it turned out
to be quiet unstable :( .
So I switched to vim using [vim-fireplace](https://github.com/tpope/vim-fireplace),
which REPL integration is more stable.
It also comes with invaluable :Doc and :Source commands which deliver the 
documentation of the source of a function, respectively.
Sadly ctags has no support for Clojure, but you can use the lisp version for now.
You don't want to miss out on [rainbow parentheses](https://github.com/kien/rainbow_parentheses.vim) 
;).
The vim commands `y%`and `d%` are big time and brain power savers, because they
let you copy or delete a complete s-expression.


## conclusion

There are many things I like about Clojure so far.
It has great java integration and dependency management, it's dynamic and fast.
Learning it is not easy, if you come from a traditional OO background but it is 
worth your time, because you will become a better developer.
Obviously I didn't spent enough time with it to feel the pain every tool brings
with it if you use it intensively.
The development setup is decent while not perfect jet, but it is still quiet a 
young language.

I can recommend you give it a spin :) .

If you live in the cologne area you can find me at the 
[Cologne Clojure User Group](http://www.meetup.com/clojure-cologne/),
or just leave me a comment :) .

Thanks for reading all my ramblings ;).
I hope it was worth your time.
