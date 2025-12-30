<header>
# Modeling business processes with sorbet in ruby
<time class="article-date" date="2025-12-28">2025-12-28</time>
</header>

## Motivation

At work modelling business processes is the main thing we do.
We do it with ruby and sorbet, which is an optional type system for ruby.
Sorbet is great because it allows us to express business concepts, as types, which in turn means the type checker can automatically highlight where interactions of these concepts are ill defined.

The most powerful tool is [exhaustiveness checks](https://sorbet.org/docs/exhaustiveness), which sorbet supports for `T::Enum` and `T.any`.

If used correctly this enables us to lean on the type checker to highlight where a new variant impacts existing code in complex system maintained and developed by multiple people.

At work we maintain an accounting system which implements various accounting rules in response to a set of financial events (event handlers and services).
Over time business rules change which may mean that some of these handlers and services need to behave different for different versions. Mortgages transition from one version to the next individually.

I use Enums for the accounting version property on the mortgage (which defined existing versions 1-3). So when when my colleague had to introduce a new version he could rely on the type checker to highlight all the places where he needed to define the behaviour for the new version.

Both Enum and T.any behave like a [tagged union](https://en.wikipedia.org/wiki/Tagged_union), meaning they define a fixed set of different types. A value can hold, and keep track of, which actual type is in use in the flow of the program for that variable.

I'll first explain Enums then T.any and finish with a discussion on pros and cons and when to prefer one over the other.

This article assumes a basic familiarity with programming and types. The examples will be in ruby.

If you are not familiar with types and would like to learn them I can recommend [Gleam](https://gleam.run) as a very simple language with an excellent tooling and great type system.


## T::Enum

[T::Enum](https://sorbet.org/docs/tenum) are just enumerations, meaning they act like a list of known values. However these values can't themself hold any data.

Example from the official docs:

```ruby
# (1) New enumerations are defined by creating a subclass of T::Enum
class Suit < T::Enum
  # (2) Enum values are declared within an `enums do` block
  enums do
    Spades = new
    Hearts = new
    Clubs = new
    Diamonds = new
  end
end
```

The `Suit`s themselfs can't hold a value. So if we want to represent an Ace of Spades we would need to combine this Enum within another class.

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
            # this is where sorbet will throw type errors if we where to ever expand on the types of available Suits
            T.absurd(self)
        end
    end
end
```

This example also shows how `case` can be used with `T.absurd(var)` to enable the exhaustiveness check.


### A practical example

Expanding on the initial example of the versioning of the accounting logic for mortgages.
To highlight how exhaustiveness checks help us to keep track of all the interactions we need to expand on the initial examples.

We will model a Mortgage to have a `accounting_version`, which returns an Enum:

```ruby

class AccountingVersion < T::Enum
    enums do
        # use simple int values rather than the default string values to optimize DB storage
        V1 = new(1)
        V2 = new(2)
    end
end

class Mortgage
    sig {returns(AccountingVersion)}
    def accounting_version
        # read serialized value from DB, then deserialize into Enum value
    end
end

```

Lets say we have two financial events we need to model for a mortgage.

1. Payment received
2. Payment due



```ruby

class PaymentReceivedHandler

    sig {params(mortgage: Mortgage, event: Event)}
    def self.call(mortgage:, event)
        # sorbet needs a var to perform exhaustiveness checks, so we can't use the method call directly, but instead need to assign the value to a local var first.
        version = mortgage.accounting_version()
        case version
            when AccountingVersion::V1, AccountingVersion::V2
            # move money into a current account
        else
            # absurd is only defined on vars
            T.absurd(verison)
        end
    end
end

class PaymentDueHandler
    sig {params(mortgage: Mortgage, event: Event)}
    def self.call(mortgage:, event)
        # sorbet needs a var to perform exhaustiveness checks, so we can't use the method call directly, but instead need to assign the value to a local var first.
        version = mortgage.accounting_version()
        case version
            when AccountingVersion::V1, AccountingVersion::V2
            # use money in the holding account; no overdraft logic
            # if holding is < then due customers will be in arrears
        else
            # absurd is only defined on vars
            T.absurd(verison)
        end
    end
end
```

### Limitation of T::Enum

## T.any


