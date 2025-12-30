# typed: strict

class AccountingVersion < T::Enum
  enums do
    # use simple int values rather than the default string values to optimize DB storage
    V1 = new(1)
    V2 = new(2)
  end
end
