# typed: strict

require_relative 'mortgage'
require_relative 'event'
require_relative 'accounting_version'

class PaymentDueHandler
  extend T::Sig

  sig { params(mortgage: Mortgage, event: Event).void }
  def self.call(mortgage:, event:)
    # sorbet needs a var to perform exhaustiveness checks, so we can't use the method call directly,
    # but instead need to assign the value to a local var first.
    version = mortgage.accounting_version
    case version
    when AccountingVersion::V1, AccountingVersion::V2
      # use money in the holding account; no overdraft logic
      # if holding is < then due customers will be in arrears
    else
      # absurd is only defined on vars
      # Note: There's a typo in the original - "verison" should be "version"
      T.absurd(version)
    end
  end
end
