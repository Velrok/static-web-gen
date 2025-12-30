# typed: strict

require_relative 'accounting_version'

class Mortgage
  extend T::Sig

  sig { returns(AccountingVersion) }
  def accounting_version
    # read serialized value from DB, then deserialize into Enum value
    # This is a placeholder implementation
    AccountingVersion::V1
  end
end
