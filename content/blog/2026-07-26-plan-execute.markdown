<header>
# plan-execute
<time class="article-date" date="2026-07-26">2026-07-26</time>
</header>

☑️TODO: motivate the problem

# The pattern

I've been using this pattern I call plan / execute with typed ruby (sorbet).

I found it useful for operations that require some validation, user confirmation, followed by execution.

Examples:

- Migrate loans from Funder A to Funder B (user selects loans, and target funder), then we validate that each can be moved, we show the user a summary of go / no-go loans, user confirms or aborts.
- Customer wants to change their payment day, we calculate the impact on payment schedules _without applying it_ and present it to the customer, customer confirms or aborts.

```ruby
class Operation
    sig {returns(Typed::Result[Operation, T::Array[String]])}
    def self.plan
        # run all the pre execution checks (like which loans are valid to move)
        # build impact assessment if needed like (what then new payment schedule would be)
        # You want to keep it side effect free so it can be run many times without impact.
        # if all checks out: return on Operation instance otherwise return errors
    end

    sig {returns(Typed::Result[NilClass, T::Array[String]])}
    def execute
        # This can now only be executed if it was previously build via .plan
        # enforces that all validations still pass.
        # Do the actual side effect here.
        # It may still encounter runtime errors.
    end

    # declare .new to be private, so code has to go through the plan stage
    private class method :new
end
```

It a spin on the "Parse don't validate" principal.

# Benefits

`Operation.plan` provides a common place to put all the pre exec checks and can come with general expectation that code should be free of side effects (No writes or updates).

`Operation#execute` <a href="#footnote-1" id="footnote-1-ref">[1]</a> can't be called unless `Operation.plan` succeeded, which means ruby will make sure the pre-checks have run and we also have a clear method to put all the side effects we want.

With this separation we have an easy and consistent way to delay execution as needed without loosing all the context.

If you don't need typed results and are happy for new to throw Exceptions you can simply use Operation.new.execute.
I prefer the `TypedResult` explicit error handling, and `new` has to return an instance of the class, so I abstract it away with `.plan` which can return a `TypedResult` for explicit error handling.

`plan` and `execute` could be given different names. The point is to split the prep from the side effect.

# What is does _not_ afford

This doesn't provide locking. So an `op = Operation.plan` may be outdated by the time the user triggers the execute.
If data drift is very unlikely then we can just replan and execute optimistically and fail at runtime if data has drifted to the point of failure.

You could serialise or store the plan itself, but then you would need to have a way to validate that the full inputs to the plan are still valid (same params but also same DB or file system state, as needed).

Another option would be to replan and compare that the plan now is still the same as the original plan, if you have a way of reliably compare plans.

# Footnotes

<a id="footnote-1"></a>
**[1]** Ruby documentation convention: `Class.class_method` (dot) refers to a method
called on the class itself, while `Class#instance_method` (hash) refers to a method
called on an instance of that class. So `Operation.plan` is a class method and
`Operation#execute` is an instance method. The `#` is only a documentation notation,
it's never write it in code. <a href="#footnote-1-ref">↩</a>
