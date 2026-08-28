<header>
# The plan, execute pattern
<time class="article-date" date="2026-07-26">2026-07-26</time>
</header>

Recently we needed to support a few _prepare > review > execute_ workflows at work. Often the first reaction is to add a `dry-run` parameter to whatever method would run this flow.

```ruby
def migrate_loans(loan_ids, target_funder, dry_run: false)
    # validate the loans
    # ...
    return summary if dry_run

    # ...and now do the writes
end
```

This mixes the prepare and execute logic in one method. The prepare half is the part you want to reuse, to show to the user as a summary before they commit, but it's now trapped behind a flag and a body full of potential writes.

So I started using a pattern of `plan` then `execute`, where a successful `plan` is a prerequisite for `execute`. That way the checks can't be skipped, and their results are a thing you can hold on to and show to someone.

# The pattern

The main idea is a class-level `plan` method that returns an object. That object holds the validated, precomputed inputs, and it is the only thing that exposes `execute`.

This gates the write effect behind a set of data preparations and validations, which makes `execute` far more predictable.

Let's say we need an option to migrate loans from Funder A to Funder B. The user picks the loans and the target funder, we check each loan can be moved, we show a summary of go / no-go loans, and the user confirms or aborts.

```ruby
# Sorbet types are used to illustrate the interface.
# Errors are simplified to String; in prod you'd want differentiated error classes.
# Typed::Result is a gem, not a Sorbet or Ruby built-in.
# We use T::Struct as a base to make it easier to have a typed Data class
class Operation < T::Struct
    extend T::Sig

    const :loans_to_migrate, T::Array[LoanId] # loan ids to move to the new funder
    const :target_funder, FunderId

    sig {params(loan_ids: T::Array[LoanId], target_funder: FunderId).returns(Typed::Result[Operation, T::Array[String]])}
    def self.plan(loan_ids:, target_funder:)
        # Run all the pre-execution checks, e.g. which loans are valid to move.
        # Keep this free of write effects so it can be run many times without impact.
        # If everything checks out, return an Operation instance; otherwise return the errors.
    end

    # Singular error here: plan collects all validation failures,
    # execute stops at the first runtime error.
    sig {returns(Typed::Result[NilClass, String])}
    def execute
        # This can only be reached if the instance was built via .plan,
        # which enforces that all validations ran successfully.
        # Do the actual side effect here. It may still hit runtime errors.
    end

    # new is private, so callers have to go through the plan stage
    private_class_method :new
end
```

`new` is private because `new` must return an instance of the class; it can't return a `Typed::Result`. Routing construction through `.plan` gives you a spot for explicit error handling. If you don't need typed results and are happy for construction to raise, `Operation.new.execute` works just as well.

It's a spin on Alexis King's ["Parse, don't validate"](https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/).
He suggests parsing input into a type that proves the checks passed, so downstream code never has to revalidate. Here we do the same, but we hold on to that proof across a user round-trip instead of consuming it immediately.
This comes with [stale read implications](#what-it-does-not-afford) discussed further down.

It's also similar to the [Command pattern](https://en.wikipedia.org/wiki/Command_pattern), minus the abstract base class and inheritance. Or you can think of it as a [partial function](https://en.wikipedia.org/wiki/Partial_function), as long as the `Operation` attributes are stable, immutable values.

# Benefits

`Operation.plan` gives you one place for all the pre-execution checks, with a general expectation that the code there is free of write effects. Reads are, strictly speaking, a side effect, but allowing them is usually a pragmatic choice. See [stale read implications](#what-it-does-not-afford) for the complications this still brings.

`Operation#execute` <a href="#footnote-1" id="footnote-1-ref">[1]</a> can't be called unless `Operation.plan` succeeded, so Ruby itself enforces that the pre-checks ran. It also gives you one obvious place for every write side effect.

With that separation we get a consistent way to delay execution without losing the context you built up.

`plan` and `execute` could be given different names. The point is to split the prep from the write.

<a id="what-it-does-not-afford"></a>

# What it _doesn't_ afford

`Operation.plan` essentially caches all the inputs into the instance it returns. There is no built-in protection against these values going stale.
This is by design, because the right mitigation depends on how and where you execute.

If you run it all sync, for example, you might get away with just wrapping it all in a transaction:

```ruby
Transaction.run do
    case (result = Operation.plan(loan_ids: loan_ids, target_funder: target_funder_id))
    when Typed::Success then result.payload.execute
    when Typed::Failure then raise result.error
    else T.absurd(result)
    end
end
```

If you're using it in the UI, you might use it primarily to facilitate the dry run:

```ruby
def preview # assuming GET
    case (result = Operation.plan(loan_ids: params[:loan_ids], target_funder: target_funder))
    when Typed::Success then render_review_and_confirm_view(result.payload)
    when Typed::Failure then render_show_validation_errors(result.error)
    else T.absurd(result)
    end
end

def confirm_and_execute # assuming POST
    Transaction.run do
        # :warn: stale read risk here. We re-plan, and the world may have
        # moved since the user saw the preview. A checksum or fingerprint on the
        # original Operation.plan result, passed through to the POST, lets us
        # detect it if needed.
        case (result = Operation.plan(loan_ids: params[:loan_ids], target_funder: target_funder))
        when Typed::Success
            op = result.payload
            next render_stale_confirmation_warning if op.fingerprint != params[:original_fingerprint]

            op.execute
        when Typed::Failure then render_show_validation_errors(result.error)
        else T.absurd(result)
        end
    end
end
```

## Execute runtime errors

Even though the data we're about to process has been validated, we can't rule out the write itself failing.
The database might reject it, the disk might be full, an API might return an error.

Depending on your circumstances, you might be fine to wait and retry, or you might have to abort and ask the user to build and review a fresh plan.

# Summary

If you have one simple command, where you just want the option to skip a few writes, a `dry_run:` flag might be good enough, and in fact short and sweet.
If you're modelling some business flow that needs to find and validate its inputs, show a preview to the user to review and confirm every time, then I think the `plan / execute` pattern presented here gives a separation of pre-processing and execution.
It establishes a place for pre-processing, validation and delayed write effects, without boxing you into a specific execution model. However, holding on to the `Operation` object for too long risks stale reads. How long that is, and how to mitigate it, is context-specific and not answered by this pattern.

______________________________________________________________________

<a id="footnote-1"></a>
**[1]** Ruby documentation convention: `Class.class_method` (dot) refers to a method
called on the class itself, while `Class#instance_method` (hash) refers to a method
called on an instance of that class. So `Operation.plan` is a class method and
`Operation#execute` is an instance method. The `#` is only a documentation notation,
it's never written in code. <a href="#footnote-1-ref">↩</a>
