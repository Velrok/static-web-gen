<header>
# Exhaustiveness checks for a saner future
<time class="article-date" date="2025-12-28">2025-12-28</time>
</header>

## Motivation

At work, modelling business processes is the main thing we do.
We do it with Ruby and Sorbet, which is an optional type system for Ruby.
Sorbet is great because it allows us to express business concepts as types, which in turn means the type checker can automatically highlight where interactions between these concepts are ill-defined.

The most powerful tool is [exhaustiveness checks](https://sorbet.org/docs/exhaustiveness), which Sorbet supports for `T::Enum` and `T.any`.

If used correctly, this enables us to lean on the type checker to highlight where a new variant impacts existing code in a complex system maintained and developed by multiple people.

At work we maintain an accounting system which implements various accounting rules in response to a set of financial events (event handlers and services).
Over time business rules change which may mean that some of these handlers and services need to behave differently for different versions. Mortgages transition from one version to the next individually.

I use `T::Enum` for the accounting version property on the mortgage (which defines existing versions 1-3). So when my colleague had to introduce a new version he could rely on the type checker to highlight all the places where he needed to define the behaviour for the new version.

Both `T::Enum` and `T.any` behave like a [tagged union](https://en.wikipedia.org/wiki/Tagged_union), meaning they define a fixed set of different types. At runtime a value keeps track of which of those types it currently holds.

I'll first explain `T::Enum` then `T.any` and finish with a discussion on pros and cons and when to prefer one over the other.

This article assumes a basic familiarity with programming and types. The examples will be in Ruby.

If you are not familiar with types and would like to learn them, I can recommend [Gleam](https://gleam.run) — a very simple language with excellent tooling and a great type system.


## T::Enum

[T::Enum](https://sorbet.org/docs/tenum) is just an enumeration, meaning it acts like a list of known values. However these values can't themselves hold any data.

Example from the official docs:

```ruby
# (1) New enumerations are defined by
# creating a subclass of T::Enum
class Suit < T::Enum
  # (2) Enum values are declared within
  # an `enums do` block
  enums do
    Spades = new
    Hearts = new
    Clubs = new
    Diamonds = new
  end
end
```

The `Suit` values themselves can't hold any extra data. So to represent an Ace of Spades we would need to wrap this `T::Enum` inside another class.

### Different representations

`T::Enum` values can have methods that dispatch on `self`, which lets us give each variant a different representation — but they still can't wrap other data.

```ruby
class Suit < T::Enum
    # ... above implementation
    def icon
        case self
        when Spades: "♠️"
        when Hearts: "❤️"
        when Clubs: "♣️"
        when Diamonds: "♦️"
        else
            # this is where sorbet will throw
            # type errors if we were to ever
            # expand on the types of available
            # Suits
            T.absurd(self)
        end
    end
end
```

This example also shows how `case` can be used with `T.absurd(var)` to enable the exhaustiveness check.


### A practical example

Let's return to the earlier example of versioning the accounting logic for mortgages. To show how exhaustiveness checks help us keep track of all the interactions, we need to flesh it out a bit.

We will model a Mortgage to have an `accounting_version`, which returns an Enum:

```ruby

class AccountingVersion < T::Enum
    enums do
        # use simple int values rather than
        # the default string values to
        # optimize DB storage
        V1 = new(1)
        V2 = new(2)
    end
end

class Mortgage
    sig {returns(AccountingVersion)}
    def accounting_version
        # read serialized value from DB,
        # then deserialize into Enum value
    end
end

```

Let's say we have two financial events we need to model for a mortgage.

1. Payment received
2. Payment due


```ruby

class PaymentReceivedHandler

    sig {params(mortgage: Mortgage, event: Event)}
    def self.call(mortgage:, event)
        # sorbet needs a var to perform
        # exhaustiveness checks, so we can't
        # use the method call directly, but
        # instead need to assign the value to
        # a local var first.
        version = mortgage.accounting_version()
        case version
            when AccountingVersion::V1,
                 AccountingVersion::V2
            # move money into a current account
        else
            # absurd is only defined on vars
            T.absurd(version)
        end
    end
end

class PaymentDueHandler
    sig {params(mortgage: Mortgage, event: Event)}
    def self.call(mortgage:, event)
        # sorbet needs a var to perform
        # exhaustiveness checks, so we can't
        # use the method call directly, but
        # instead need to assign the value to
        # a local var first.
        version = mortgage.accounting_version()
        case version
            when AccountingVersion::V1,
                 AccountingVersion::V2
            # use money in the holding account;
            # no overdraft logic
            # if holding is < then due customers
            # will be in arrears
        else
            # absurd is only defined on vars
            T.absurd(version)
        end
    end
end
```

Now let's say the requirements have changed: in the `PaymentDueHandler` we now need to categorise any potential arrears into a repayment and an interest portion.
Just adding a `V3` to `AccountingVersion` would now make both `case` statements fail with a helpful type error, stating that the match is incomplete and that the `V3` branch is missing.

In the `PaymentReceivedHandler` case we can just add `V3` to the existing list.
For the `PaymentDueHandler` we would need to add a new `when` clause with the new logic.

In this small example it was easy to remember all the places we needed to make changes, but in our actual code base we have around 15 different handlers, each of which may or may not need to behave differently.

Even just adding a new version to the existing `when` clause means we have at least considered that handler and decided no new logic was required.

### Limitation of T::Enum

`T::Enum` values like a `V1` or a `Hearts` can't store data inside them.
This makes them unusable for something like an `Event` type where we would also have a fixed list of possible values, but each event may contain its own data.

In these cases we need to reach for the more complex and generic `T.any`.

## T.any

[T.any](https://sorbet.org/docs/union-types) is Sorbet's union type.
It can only be defined as a list of Ruby classes: `T.any(SomeType, SomeOtherType, ...)` like `T.any(Integer, String)` meaning either a whole number or a string.


### A practical example

Events are a good example of a finite list of values where each variant carries its own data.

```ruby
class PaymentReceivedEvent
  sig { returns(Cents) }
  attr_reader :amount_received
end

class PaymentDueEvent
  sig { returns(Cents) }
  attr_reader :amount_due
end

# Event is a type alias for a union of
# PaymentReceivedEvent and PaymentDueEvent
Event = T.type_alias {
  T.any(
    PaymentReceivedEvent,
    PaymentDueEvent
  )
}
```

Note how both `PaymentReceivedEvent` and `PaymentDueEvent` define different properties.

A simple event processor could dispatch on event type:

```ruby
class EventProcessor
    sig {params(event: Event)}
    def self.call(event:)
        case event
        when PaymentReceivedEvent:
            PaymentReceivedHandler(event:)
        when PaymentDueEvent:
            PaymentDueHandler(event:)
        else
            # if we were to add more events
            # this would fail type checking and
            # we would be reminded to implement
            # a handler for the new event
            T.absurd(event)
        end
    end
end

```

Unlike TypeScript, Sorbet does not allow the use of literals like `'V1'` or `1` in `T.any`.
This is by design.

If you need that, `T::Enum` is the correct construct to use.


## Summary and trade-offs

`T.any` is the most flexible implementation, and the closest to a textbook tagged union.
However it is only defined on Ruby classes, not literals, so if all you need is a list of fixed values like `V1`, `V2` and so on, then `T::Enum` provides a more convenient implementation.

If values need to be data containers, like events that have different properties, then defining separate classes and building a tagged union using `T.any` is the only choice.
If an exhaustive list of possible values is the main objective — like numbered versions — then `T::Enum` is preferable, for its literals support and its ease of serialisation and deserialisation.
