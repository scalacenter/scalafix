package test

object DottyVarArgPattern {
  List(1, 2, 3, 4) match {
    case List(1, 2, xs : _*) =>
    case _ =>
  }

  val List(1, 2, x : _*) = List(1, 2, 3, 4)
}
