package test.organizeImports

trait ExpandUnmodeledBase {
  def inherited: Int = 1
}

object ExpandUnmodeledM extends ExpandUnmodeledBase {
  val direct: Int = 2
  // The structural result type cannot be modeled as a nominal symbol, so a
  // member selected on it is classified as an unmodelable use of its owner.
  def makeBase(): ExpandUnmodeledBase { def extra: Int } =
    new ExpandUnmodeledBase { def extra: Int = 3 }
}
