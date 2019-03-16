---
layout: post
title: "moving the counter into the foreach loop"
date: 2013-04-29 01:59
comments: true
categories: python
---

Recently I did a little code review for a friend of mine.
He needed a counter while iterating items of a list.
He solved it the same way I did - many times befor, always a little bit annoyed by the fact that I needed to declare and incredent the counter myself.

``` python 

stuff = ["some thing", "some thing else"]

i = 0
for thing in stuff:
  print "processing item {} of {}".format(i + 1, len(stuff))
  i += 1

```

Python comes with this nice `for i in list` formulation, so taking care of a counter seperatly seamed wrong.

## update


Big thanks to [Rafa Rodríguez](https://plus.google.com/108719046880594833475/posts) for pointing me to `enumerate`

``` python
stuff = ["some thing", "some thing else"]

for i, thing in enumerate(stuff):
  print "processing item {} of {}".format(i + 1, len(stuff))

```

*You can skipp the rest :) .*

<!-- more -->
----

Indeed I came up with the following refactoring:

``` python 

stuff = ["some thing", "some thing else"]

for thing, i in zip(stuff, range(len(stuff))):
  print "processing item {} of {}".format(i + 1, len(stuff))

```

`zip` takes n lists and combines thouse elements into tubles, drawing from all lists, stoping if one of the lists is exeeded. This makes it very usefull in combination with infinit lists.

Ok now this takes care of incrementing `i` for us, nice :) .
However it's still cumbersome to have to specify the range.
What we really want is a lazy sequenze of integers that just counts up starting at a value of our choosing.
Python [itertools](http://docs.python.org/2/library/itertools.html) to the rescure.
The iter tools provide a `count()` function that returns this generator:

```python
def count(start=0, step=1):
    # count(10) --> 10 11 12 13 14 ...
    # count(2.5, 0.5) -> 2.5 3.0 3.5 ...
    n = start
    while True:
        yield n
        n += step
```

So we can rewrite our code to:

``` python 
from itertools import count

stuff = ["some thing", "some thing else"]

for thing, i in zip(stuff, count(1))):
  print "processing item {} of {}".format(i, len(stuff))
```

Note that by calling `count(1)` the sequence starts at 1,
removing the need to call `i + 1` in the print statement all together.

In the end I was very pleased to finally having found - what I belive to be - a beautiefull solution to this problem in python.

Feel free to comment or contact me on twitter.
