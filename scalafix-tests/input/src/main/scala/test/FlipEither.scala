/*
patches.replacements = [
  {
    from = _root_.scala.util.Right.
    to = Left
  }
  {
    from = _root_.scala.package.Right.
    to = Left
  }
  {
    from = "_root_.scala.Predef.String."
    to = "Int"
  }
  {
    from = _root_.scala.Int.
    to = String
  }
  {
    from = _root_.scala.util.Left.
    to = Right
  }
  {
    from = _root_.scala.package.Left.
    to = Right
  }
]
patches.addGlobalImports = [
  "scala.collection.immutable.Seq"
]
rewrites = []
*/
package test

object FlipEither {
  val x: Either[Int, String] = if (true) Left(1) else Right("msg")
}
