package domain

sealed trait Currency

object Currency {
  case object USD extends Currency
  case object EUR extends Currency
  case object GBP extends Currency

  def withName(name: String): Currency = name match {
    case "USD" => USD
    case "EUR" => EUR
    case "GBP" => GBP
    // Add more currency types as needed
    case _ => throw new IllegalArgumentException(s"Unknown currency: $name")
  }
}
