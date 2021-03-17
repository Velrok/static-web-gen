<header>
# Clojure: making a case for case
<time class="article-date" date="2021-03-17">2021-03-17</time>
</header>

I recently came across a usage pattern in our services where we would overuse
clojures dynamic function dispatch. I would like to submit an
opinion on when to use a simple case statement instead.

## The use case

The fundamental need here is to choose a different implementation based on some
attribute of the input.

I observed two instances: a) when receiving a message we need to choose
the service that should handle it b) during start-up we have different modes we
need to start the app in (persists incoming data / provide an API on top of the
DB).


## About multi methods and case statements

Clojure has `defmulti` ([defmulti docs](https://clojuredocs.org/clojure.core/defmulti))
which declares a function with dynamic runtime dispatch, paired with `defmethod`
([defmethod docs](https://clojuredocs.org/clojure.core/defmethod)) which defines
an implementation.
This can be very powerful specially in libraries, where you might want to leave
the implementation open for further specialisation later on, or where knowing
all cases would be impossible ahead of time.

[Example from clojure.org](https://clojure.org/about/runtime_polymorphism):

```clojure
(defmulti encounter
  (fn [x y] [(:Species x) (:Species y)]))
(defmethod encounter [:Bunny :Lion] [b l] :run-away)
(defmethod encounter [:Lion :Bunny] [l b] :eat)
(defmethod encounter [:Lion :Lion] [l1 l2] :fight)
(defmethod encounter [:Bunny :Bunny] [b1 b2] :mate)
(def b1 {:Species :Bunny :other :stuff})
(def b2 {:Species :Bunny :other :stuff})
(def l1 {:Species :Lion :other :stuff})
(def l2 {:Species :Lion :other :stuff})
(encounter b1 b2)
-> :mate
(encounter b1 l1)
-> :run-away
(encounter l1 b1)
-> :eat
(encounter l1 l2)
-> :fight
```

The humble [`case` macro](https://clojuredocs.org/clojure.core/case) in constrast only allows a fixed set of cases to be
handled. All need to be known during definition and it can't be extended
dynamically by code outside that expression.

```
(case meal-time
  :breakfast "Jam on toast."
  :lunch     "Ham sandwitch."
  :dinner    "Roast dinner.")
```

I think it's the extra power of `defmulti` that makes it appealing.

## Making the case

Lets revisit our initial case again, in the context of our app:

a) we receive a message and need to dynamically choose the service that should handle it
b) during start-up we have different modes we need to start the app in

The main insight here is that in both cases (a and b) we actually know the full list of
supported services or modes, so a simple case statement will suffice. 

Let's concider an example for a)

**PS: The code is indended for illustration purposes only and not is not complete.**

Base file
```clojure
(ns supplier-change
  (:require
    [super-power] ;; only here for side effects
    [mega-power]  ;; only here for side effects
    ))

(defmulti handle-supply-change
  (fn [contract] (:supplier-id contract)))

;; called if no other implementation is found
(defmethod handle-supply-change :default
  [contract]
  (panic! "We don't work with " (:supplier-id contract))) 
```

Supplier super-power
```clojure
(ns super-power
  (:require [super-power-api]))

(defmethod handle-supply-change :super-power
  [contract]
  (super-power-api/take-over contract))
```

Supplier mega-power
```clojure
(ns mega-power
  (:require [mega-power-api]))

(defmethod handle-supply-change :mega-power
  [contract]
  (mega-power-api/take-over contract))
```

The main benefit of multi methods, which is to allow for an open implementation,
is of little use here since we will need to implement any additional suppliers in any
case.
The downsides are that 1) we have to make sure that all supplier namespaces
are required, and therefor loaded during startup, or risk running into the panic
case.
2) the panic default implementation is a hint here that we actually have a fixed
list of supported suppliers in mind, but the `defmethods` are in other files,
and nothing explicitly mentions them. We are instead implicitly loading them as
a side effect of requiring their namespaces. This has poor discover-ability.

Let's replace this with a simple case statement:

```
(ns supplier-change
  (:require
    ;; require apis directly
    [super-power-api]
    [mega-power-api]))

(defn handle-supply-change
  [contract]
  (case (:supplier-id contract)
    :super-power (super-power-api/take-over contract)
    :mega-power  (mega-power-api/take-over contract)
    ;; default case
    (panic! "We don't work with " (:supplier-id contract))))
```

## In summary

Multi-methods are a powerful tool, which allows for open implementations in
Clojure.
However when writing application level code consider if the implementation is
actually truly open or if you are actually looking to dispatch to a fixed and
known set of functions.

If the set of functions is known and exhaustive at least for now, than `case`
provides better readability.

Seeing a default implementation for `defmethod`, which just throws some sort of
error is an indicator that a case might be more appropriate here.
