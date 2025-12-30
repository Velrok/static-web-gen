# typed: strict

# Example from Sorbet docs showing T::Enum
class Suit < T::Enum
  # Enum values are declared within an `enums do` block
  enums do
    Spades = new
    Hearts = new
    Clubs = new
    Diamonds = new
  end

  # T::Enums allow us to add methods that dispatch on self
  sig { returns(String) }
  def icon
    case self
    when Spades then "♠️"
    when Hearts then "❤️"
    when Clubs then "♣️"
    when Diamonds then "♦️"
    else
      # this is where sorbet will throw type errors if we where to ever expand on the types of available Suits
      T.absurd(self)
    end
  end
end
