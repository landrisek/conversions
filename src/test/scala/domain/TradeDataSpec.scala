package domain

import java.time.Instant
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TradeDataSpec extends AnyFlatSpec with Matchers {

  val testInstant = Instant.now()

  "A TradeData" should "correctly instantiate with all parameters provided" in {
    val tradeData = TradeData(1, 2, 3.0, 4.0, "USD", testInstant)

    tradeData.marketId shouldBe 1
    tradeData.selectionId shouldBe 2
    tradeData.odds shouldBe 3.0
    tradeData.stake shouldBe 4.0
    tradeData.currency shouldBe "USD"
    tradeData.date shouldBe testInstant
  }
}