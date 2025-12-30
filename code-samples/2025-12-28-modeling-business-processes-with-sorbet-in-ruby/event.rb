# typed: strict

# Type alias for representing monetary amounts in cents
Cents = T.type_alias { Integer }

class PaymentReceivedEvent
  extend T::Sig

  sig { returns(Cents) }
  attr_reader :amount_received

  sig { params(amount_received: Cents).void }
  def initialize(amount_received:)
    @amount_received = amount_received
  end
end

class PaymentDueEvent
  extend T::Sig

  sig { returns(Cents) }
  attr_reader :amount_due

  sig { params(amount_due: Cents).void }
  def initialize(amount_due:)
    @amount_due = amount_due
  end
end

# Event is a type alias for a union of PaymentReceivedEvent and PaymentDueEvent
Event = T.type_alias { T.any(PaymentReceivedEvent, PaymentDueEvent) }
