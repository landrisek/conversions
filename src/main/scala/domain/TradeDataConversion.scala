package domain

// Domain model for trade data conversion
case class TradeDataConversion(
  marketId: Int,
  selectionId: Int,
  odds: Double,
  stake: Double,
  currency: String,
  date: String
)
