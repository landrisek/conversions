package domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import org.scalatest.OptionValues

class CurrencySpec extends AnyFlatSpec with Matchers with EitherValues with OptionValues {
  
  "The withName method" should "return the correct Currency for valid inputs" in {
    Currency.withName("USD") shouldBe Currency.USD
    Currency.withName("EUR") shouldBe Currency.EUR
    Currency.withName("GBP") shouldBe Currency.GBP
  }
  
  it should "throw IllegalArgumentException for unknown currencies" in {
    assertThrows[IllegalArgumentException] {
      Currency.withName("unknown")
    }
  }
}
