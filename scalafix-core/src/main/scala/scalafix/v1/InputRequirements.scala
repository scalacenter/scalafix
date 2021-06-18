package scalafix.v1

trait InputRequirements {
  def scalaVersionsRequired: Seq[InputRequirements.ScalaVersion]
  def scalacOptionsRequired: Seq[InputRequirements.ScalacOption]
  def meetRequirements(sv: String, scalacOptions: Seq[String]): Boolean = {
    (scalacOptionsRequired.isEmpty || scalaVersionsRequired
      .map(_.value)
      .contains(sv)) && (scalacOptionsRequired.isEmpty || scalacOptionsRequired
      .map(_.value)
      .iterator
      .forall(scalacOptions.contains))
  }

  def messageRequirementNotMet: String = ""
}

object InputRequirements {
  case class ScalaVersion(value: String)

  case class ScalacOption(value: String)
}
