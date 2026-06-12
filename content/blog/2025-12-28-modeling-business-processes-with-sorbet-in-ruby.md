<header>
# Exhaustiveness checks for a saner future
<time class="article-date" date="2025-12-28">2025-12-28</time>
</header>

## Motivation

At work, modelling business processes is the main thing we do.
We do it with Ruby and Sorbet, which is an optional type system for Ruby.
Sorbet is great because it allows us to express business concepts, as types, which in turn means the type checker can automatically highlight where interactions of these concepts are ill-defined.

The most powerful tool is [exhaustiveness checks](https://sorbet.org/docs/exhaustiveness), which Sorbet supports for `T::Enum` and `T.any`.

If used correctly, this enables us to lean on the type checker to highlight where a new variant impacts existing code in a complex system maintained and developed by multiple people.

At work we maintain an accounting system which implements various accounting rules in response to a set of financial events (event handlers and services).
Over time business rules change which may mean that some of these handlers and services need to behave differently for different versions. Mortgages transition from one version to the next individually.

I use T::Enum for the accounting version property on the mortgage (which defines existing versions 1-3). So when my colleague had to introduce a new version he could rely on the type checker to highlight all the places where he needed to define the behaviour for the new version.

Both T::Enum and T.any behave like a [tagged union](https://en.wikipedia.org/wiki/Tagged_union), meaning they define a fixed set of different types. A value can hold, and keep track of, which actual type is in use in the flow of the program for that variable.

I'll first explain T::Enum then T.any and finish with a discussion on pros and cons and when to prefer one over the other.

This article assumes a basic familiarity with programming and types. The examples will be in Ruby.

If you are not familiar with types and would like to learn them, I can recommend [Gleam](https://gleam.run) as a very simple language with an excellent tooling and great type system.


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

The `Suit`s themselves can't hold a value. So if we want to represent an Ace of Spades we would need to combine this T::Enum within another class.

### Different representations

`T::Enums` allow us to add methods to them, which dispatch on self and therefore allow for different representations for the same type, but they fundamentally can't wrap other data.

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

Expanding on the initial example of the versioning of the accounting logic for mortgages.
To highlight how exhaustiveness checks help us to keep track of all the interactions, we need to expand on the initial examples.

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

Now let's say that the requirements have changed and in the PaymentDueHandler case we need to categorise any potential arrears into the repayment and the interest portion.
Just adding a V3 to `AccountingVersion` would now make the two case statements fail with a helpful type error, stating that the case is incomplete and that the V3 case is missing.

In the `PaymentReceivedHandler` case we can just add V3 to the existing list.
For the `PaymentDueHandler` we would need to add a new when clause and add the new logic.

In the example it was easy to remember all the places we needed to make changes, but in our actual code base we have around 15 different handlers that may or may not have to behave differently.

Even just adding a new version to the existing case, means we have at least considered this case and decided that no new logic was required.

### Limitation of T::Enum

T::Enum values like a V1 or a Hearts can't store data inside them.
This makes them unusable for something like an `Event` type where we would also have a fixed list of possible values, but each event may contain its own data.

In these cases we need to reach for the more complex and generic `T.any`.

## T.any

[T.any](https://sorbet.org/docs/union-types) is Sorbet's union type.
It can only be defined as a list of ruby classes: `T.any(SomeType, SomeOtherType, ...)` like `T.any(Integer, String)` meaning either a whole number or a string.


### A practical example

Events are a good example for a finite list of values, that each contain more data.

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

Unlike TypeScript sorbet does not allow the use of literals like `'V1'` or `1` in T.any.
This is by design.

If you need this `T::Enum` is the correct construct to use.


## Summary and trade-offs

`T.any` is the most flexible implementation, and the closest to a textbook tagged union.
However it is only defined on ruby classes, not literals, so if all you need is a list of fixed values like a `V1` `V2` and so on, then `T::Enum` provides a more convenient implementation.

If values need to be data containers, like events that have different properties, then defining separate classes and building a tagged union using `T.any` is the only choice.
If an exhaustive list of possible values is the main objective like numbered versions, then `T::Enum` is preferable, for its literals support and its ease of serialization and deserialization.
