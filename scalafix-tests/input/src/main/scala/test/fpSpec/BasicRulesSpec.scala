/*
rules = [
  Disable
  NoInfer
  DisableSyntax
]

Disable.symbols = [
  "java.lang.Object.equals"
  "java.lang.Object#`==`"
  "java.lang.Object.eq"
  "java.lang.Object.ne"
  "scala.Any.asInstanceOf"
  "scala.util.Either.LeftProjection.get"
  "scala.util.Either.RightProjection.get"
  "scala.Enumeration"
  "scala.collection.JavaConversions"
  "scala.collection.mutable"
  "scala.collection.mutable.ListBuffer"
  "scala.Option.get"
  "scala.Some.get"
  "scala.None.get"
  "scala.util.Try.get"
  "scala.util.Failure.get"
  "scala.util.Success.get"
  "scala.collection.IterableLike.head"
  "scala.collection.TraversableLike.tail"
  "scala.collection.TraversableLike.init"
  "scala.collection.TraversableLike.last"
  "scala.collection.TraversableOnce.reduce"
  "scala.collection.TraversableOnce.reduceLeft"
  "scala.collection.IterableLike.reduceRight"
]

NoInfer.symbols = [
  "scala.Any"
  "scala.AnyVal"
  "java.io.Serializable"
  "scala.Product."
  "scala.collection.convert.WrapAsJava#`deprecated mapAsJavaMap`"
  "scala.Nothing"
  "scala.Option.option2Iterable"
  "scala.Predef.any2stringadd"
]

DisableSyntax.keywords = [
  var
  null
  return
  throw
  while
]

DisableSyntax.noSemicolons = true
*/
// TODO write custom messages for some symbols
package test.fpSpec

import scala.util.Try

object BasicRulesSpec {
  Array(1, 2, 3).equals(Array(4, 5, 6)) // assert: Disable.equals
  Array(1, 2, 3) == Array(4, 5, 6) // assert: Disable.==
  Array(1, 2, 3) eq Array(4, 5, 6) // assert: Disable.eq
  Array(1, 2, 3) ne Array(4, 5, 6) // assert: Disable.ne

  case class A(a: Int, s: String)
  A(1, "1").equals(A(2, "2")) // OK: equals is overrided
  // FIXME == should fail only for specific things like Array
  A(1, "1") == A(2, "2") // assert: Disable.==

  val any = List(1, true, "three") // assert: NoInfer.any
  val xs = List(1, true) // assert: NoInfer.anyval

  val x: Any = "123"
  x.asInstanceOf[String] // assert: Disable.asInstanceOf

  val e = Left("a") // assert: NoInfer.nothing
  e.left.get // assert: Disable.get
  e.right.get // assert: Disable.get

// see https://github.com/scalacenter/scalafix/issues/493
//  object WeekDay extends Enumeration { // assert: Disable.Enumeration
//    type WeekDay = Value
//    val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
//  }

  object O extends Serializable
  val mistake = List("foo", "bar", O /* forgot O.toString */) // assert: NoInfer.serializable

  import scala.collection.JavaConversions._ // assert: Disable.JavaConversions
  val scalaMap: Map[String, String] = Map[String, String]()
  // TODO add regex matching for symbols in Disable or NoInfer or just huge list of blocked implicits
  val javaMap: java.util.Map[String, String] = scalaMap // assert: NoInfer.deprecated mapasjavamap

  // TODO the same list or regex // assert: Disable.mutable
  // val mutList = ListBuffer[Int]() // assert: Disable.ListBuffer

  val nothing = ??? // FIXME NoInfer.nothing
  val nothingList = List.empty // assert: NoInfer.nothing

  var a = 1                 // assert: DisableSyntax.keywords.var
  val n = null              // assert: DisableSyntax.keywords.null
  def foo: Unit = return    // assert: DisableSyntax.keywords.return
  throw new Exception("ok") // assert: DisableSyntax.keywords.throw
  val s = "semicolon";      // assert: DisableSyntax.noSemicolons

  Some(1) zip Some(2) // assert: NoInfer.option2iterable
  Option(1).get // assert: Disable.get
  Some(1).get // assert: Disable.get
  None.get // assert: Disable.get

  val prod = List((1, 2, 3), (1, 2)) // assert: NoInfer.product
  // TODO string + any shoudn't work
  // "foo" + {} // assert: NoInfer.any2stringadd

  {} + "bar" // assert: NoInfer.any2stringadd

  while (true) { println("hi!") } // assert: DisableSyntax.keywords.while

  List(1, 2, 3).head // assert: Disable.head
  Seq(1).head // assert: Disable.head
  List(1, 2, 3).tail // assert: Disable.tail
  Seq(1).tail // assert: Disable.tail
  List(1, 2, 3).init // assert: Disable.init
  Seq(1).init // assert: Disable.init
  //List(1, 2, 3).last // assert: Disable.last
  Seq(1).last // assert: Disable.last
  List(1, 2, 3).reduce(_ + _) // assert: Disable.reduce
  Seq(1).reduce(_ + _) // assert: Disable.reduce
  //List(1, 2, 3).reduceLeft(_ + _) // assert: Disable.reduceLeft
  Seq(1).reduceLeft(_ + _) // assert: Disable.reduceLeft
  //List(1, 2, 3).reduceRight(_ + _) // assert: Disable.reduceRight
  Seq(1).reduceRight(_ + _) // assert: Disable.reduceRight
  // TODO there's some mess with inherited methods for collections
  // possible solutions are to make a huge list again or regexes

  Try { 1 }.get // assert: Disable.get
}
