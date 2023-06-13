package domain

import java.time.Instant

trait Convertable {
  def stake: Double
  def currency: String
  def date: Instant
  // other methods as required
}

// Domain model for trade data
case class TradeData(
  marketId: Int,
  selectionId: Int,
  odds: Double,
  stake: Double,
  currency: String,
  date: Instant
) extends Convertable