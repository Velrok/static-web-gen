<header>
# The plan-execute pattern for prep, review, execute operations 
<time class="article-date" date="2026-07-26">2026-07-26</time>
</header>

Recently we had the need at work to present a few _prepare > review > execute_ workflows. Sometimes this comes through as a workflow that requires a `dry-run` step. For this I started to use a pattern of `plan` then `execute` requiring the plan as a prerequisite to ensure that the initial checks have been performed and have passed.
Not having a pattern for this can often result in methods that take an `is_dry_run` which ends up with one method mixing the prepare and execute logic, which makes it hard to reuse the prepare logic separately from presenting its results to the user for review.

# The pattern

The main idea is to have a static / class level `plan` method, which returns an object which carries the matching execute method.
This gates the write effect behind a passed set of validations and data prep stages, to make the execute more predictable.

I found it useful for operations that require some validation, user confirmation, followed by execution.

Examples:

- Migrate loans from Funder A to Funder B atomically (user selects loans, and target funder), then we validate that each loan can be moved, we show the user a summary of go / no-go loans, user confirms or aborts.
- Customer wants to change their payment day, we calculate the impact on payment schedules _without applying it_ and present it to the customer, customer confirms or aborts.

```ruby
# This uses Sorbet types to illustrate the interface.
# Errors are simplified to just String, in prod you would want to use differentiated Error classes.
# Typed::Result is a gem not a sorbet / ruby built-in.
class Operation
    attr loans_to_migrate # list of loan ids to move to new funder
    attr target_funder

    # This would likely take extra input params like source_funder, target_funder
    # or customer new_preferred_payment_day
    sig {returns(Typed::Result[Operation, String])}
    def self.plan
        # run all the pre execution checks (like which loans are valid to move)
        # You want to keep it write effect free so it can be run many times without impact.
        # if all checks out: return an Operation instance otherwise return errors
    end

    sig {returns(Typed::Result[NilClass, String])}
    def execute
        # This can now only be executed if it was previously built via .plan,
        # which enforces that all validations were run.
        # Do the actual side effect here.
        # It may still encounter runtime errors.
    end

    # declare .new to be private, so code has to go through the plan stage
    private class method :new
end
```

It's a spin on the ["Parse don't validate" principle](https://github.com/wyattgill9/knowledge-base/blob/main/Wiki/Pages/parse-dont-validate.md).
Rather than parsing input data into a valid shape, which allows skipping further validations, we parse inputs into a set of precomputed stable inputs, but delay their execution.
There are downsides around staleness with this which I cover [further down](#what-it-does-not-afford).

Also it is somewhat similar to the [Command pattern](https://en.wikipedia.org/wiki/Command_pattern) although without the abstract class and inheritance, or one could think of it as a [partial function](https://en.wikipedia.org/wiki/Partial_function) as long as the `Operation` attributes are fully owned and immutable values.

# Benefits

`Operation.plan` provides a common place to put all the pre exec checks and can come with general expectation that code should be free of write effects (No writes or updates). Note that I'd happily put reads in here, which may go stale see below for downsides. This is usually a practical simplification.

`Operation#execute` <a href="#footnote-1" id="footnote-1-ref">[1]</a> can't be called unless `Operation.plan` succeeded, which means ruby will make sure the pre-checks have run and we also have a clear method to put all the side effects we want.

With this separation we have an easy and consistent way to delay execution as needed without losing all the context.

If you don't need typed results and are happy for new to throw Exceptions you can simply use Operation.new.execute.
I prefer the `TypedResult` explicit error handling, and `new` has to return an instance of the class, so I abstract it away with `.plan` which can return a `TypedResult` for explicit error handling.

`plan` and `execute` could be given different names. The point is to split the prep from the side effect.

<a id="what-it-does-not-afford"></a>

# What it _doesn't_ afford

`Operation.plan` essentially caches all the inputs into the instance it returns. There is no built-in protection for these values going stale.
This is by design, because the mitigation against this will likely depend on your execution situation.

If you run it all sync for example you might get away with just wrapping it all in a transaction:

```ruby
Transaction.run do
    case (op = Operation.plan)
        when Typed::Success then op.execute
        when Typed::Failure then raise op.error
        else T.absurd(op)
    end
end
```

If you're using it in the UI you might use it primarily to facilitate the dry run:

```ruby
    def preview # assuming GET
        case (op = Operation.plan)
            when Typed::Success then render_review_and_confirm_view(op)
            when Typed::Failure then render_show_validation_errors(op.error)
            else T.absurd(op)
        end
    end

    def confirm_and_execute # assuming POST
        Transaction.run do
            # :warn: stale read risk here
            # If this is a real risk consider adding some kind of checksum or fingerprint to the original
            # Operation.plan result pass it through to the POST to compare against
            op = Operation.plan

            return render_stale_confirmation_warning unless op.fingerprint == params[:original_fingerprint]

            case op
            when Typed::Success then render_review_and_confirm_view(op)
            when Typed::Failure then render_show_validation_errors(op.error)
            else T.absurd(op)
            end
        end
    end
```

## Execute runtime errors

Although we have made sure that the data we are about to process is valid, we can't rule out the write failing.
Issues like the DB refusing the write, the File refusing the write, or some API failing to execute as requested.

Again depending on your circumstance you might be OK to just wait a bit and retry or you might have to abort and ask the user to create and review another plan.

______________________________________________________________________

<a id="footnote-1"></a>
**[1]** Ruby documentation convention: `Class.class_method` (dot) refers to a method
called on the class itself, while `Class#instance_method` (hash) refers to a method
called on an instance of that class. So `Operation.plan` is a class method and
`Operation#execute` is an instance method. The `#` is only a documentation notation,
it's never written in code. <a href="#footnote-1-ref">↩</a>
