<header>
# The plan/execute pattern for prepare, review, execute operations
<time class="article-date" date="2026-07-26">2026-07-26</time>
</header>

Recently we needed to support a few _prepare > review > execute_ workflows at work. Often this arrives as a request for a `dry-run` step.

The obvious first move is a flag:

```ruby
def migrate_loans(loan_ids, target_funder, dry_run: false)
    # validate the loans
    # ...
    return summary if dry_run

    # ...and now do the writes
end
```

This mixes the prepare and execute logic in one method. The prepare half is the part you want to reuse — to show the user a summary before they commit — but it's now trapped behind a flag and a body full of writes.

So I started using a pattern of `plan` then `execute`, where a successful `plan` is a prerequisite for `execute`. That way the checks can't be skipped, and their results are a thing you can hold on to and show to someone.

# The pattern

The main idea is a class-level `plan` method that returns an object. That object holds the validated, precomputed inputs, and it is the only thing that exposes `execute`.

This gates the write effect behind a set of validations and data prep stages that have already passed, which makes `execute` far more predictable.

Examples:

- Migrating loans from Funder A to Funder B atomically. The user picks the loans and the target funder, we check each loan can be moved, we show a summary of go / no-go loans, and the user confirms or aborts.
- A customer wants to change their payment day. We calculate the impact on their payment schedule _without applying it_, present it, and they confirm or abort.

```ruby
# Sorbet types are used to illustrate the interface.
# Errors are simplified to String; in prod you'd want differentiated error classes.
# Typed::Result is a gem, not a Sorbet or Ruby built-in.
class Operation
    extend T::Sig

    attr_reader :loans_to_migrate # loan ids to move to the new funder
    attr_reader :target_funder

    sig {params(loan_ids: T::Array[Integer], target_funder: Funder).returns(Typed::Result[Operation, T::Array[String]])}
    def self.plan(loan_ids:, target_funder:)
        # Run all the pre-execution checks, e.g. which loans are valid to move.
        # Keep this free of write effects so it can be run many times without impact.
        # If everything checks out, return an Operation instance; otherwise return the errors.
    end

    # Singular error here: plan collects all validation failures,
    # execute stops at the first runtime one.
    sig {returns(Typed::Result[NilClass, String])}
    def execute
        # This can only be reached if the instance was built via .plan,
        # which enforces that all validations ran.
        # Do the actual side effect here. It may still hit runtime errors.
    end

    # new is private, so callers have to go through the plan stage
    private_class_method :new
end
```

`new` is private because `new` must return an instance of the class — it can't return a `Typed::Result`. Routing construction through `.plan` gives you a spot for explicit error handling. If you don't need typed results and are happy for construction to raise, `Operation.new.execute` works just as well.

It's a spin on Alexis King's ["Parse, don't validate"](https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/).
There, you parse input into a type that proves the checks passed, so downstream code never has to revalidate. Here we do the same, but we hold on to that proof across a user round-trip instead of consuming it immediately — which is where the staleness problem comes from, covered [further down](#what-it-does-not-afford).

It's also similar to the [Command pattern](https://en.wikipedia.org/wiki/Command_pattern), minus the abstract base class and inheritance. Or you can think of it as a [partial function](https://en.wikipedia.org/wiki/Partial_function), as long as the `Operation` attributes are fully owned, immutable values.

# Benefits

`Operation.plan` gives you one place for all the pre-execution checks, with a general expectation that the code there is free of write effects. Reads are fine — they may go stale, which I cover below — and that's usually a practical simplification worth making.

`Operation#execute` <a href="#footnote-1" id="footnote-1-ref">[1]</a> can't be called unless `Operation.plan` succeeded, so Ruby itself enforces that the pre-checks ran. It also gives you one obvious home for every side effect.

With that separation you get a consistent way to delay execution without losing the context you built up.

`plan` and `execute` could be given different names. The point is to split the prep from the side effect.

<a id="what-it-does-not-afford"></a>

# What it _doesn't_ afford

`Operation.plan` essentially caches all the inputs into the instance it returns. There is no built-in protection for these values going stale.
This is by design, because the right mitigation depends on how and where you execute.

If you run it all sync for example you might get away with just wrapping it all in a transaction:

```ruby
Transaction.run do
    case (result = Operation.plan(loan_ids: loan_ids, target_funder: target_funder))
    when Typed::Success then result.payload.execute
    when Typed::Failure then raise result.error
    else T.absurd(result)
    end
end
```

If you're using it in the UI you might use it primarily to facilitate the dry run:

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
        # :warn: stale read risk here — we re-plan, and the world may have
        # moved since the user saw the preview. A checksum or fingerprint on the
        # original Operation.plan result, passed through to the POST, lets us
        # detect it.
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

Even though the data we're about to process has been validated, we can't rule out the write itself failing — the database rejecting it, the disk being full, an API returning an error.

Again, depending on your circumstances, you might be fine to wait and retry, or you might have to abort and ask the user to build and review a fresh plan.

☑️TODO: write a closer — return to the `dry_run` flag from the intro and say what the extra ceremony buys you, and when it isn't worth it.

______________________________________________________________________

<a id="footnote-1"></a>
**[1]** Ruby documentation convention: `Class.class_method` (dot) refers to a method
called on the class itself, while `Class#instance_method` (hash) refers to a method
called on an instance of that class. So `Operation.plan` is a class method and
`Operation#execute` is an instance method. The `#` is only a documentation notation,
it's never written in code. <a href="#footnote-1-ref">↩</a>
