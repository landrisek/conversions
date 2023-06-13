package domain

import java.time.Instant
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TradeDataConversionSpec extends AnyFlatSpec with Matchers {

  val testDate = Instant.now().toString

  "A TradeDataConversion" should "correctly instantiate with all parameters provided" in {
    val tradeDataConversion = TradeDataConversion(1, 2, 3.0, 4.0, "USD", testDate)

    tradeDataConversion.marketId shouldBe 1
    tradeDataConversion.selectionId shouldBe 2
    tradeDataConversion.odds shouldBe 3.0
    tradeDataConversion.stake shouldBe 4.0
    tradeDataConversion.currency shouldBe "USD"
    tradeDataConversion.date shouldBe testDate
  }
}
