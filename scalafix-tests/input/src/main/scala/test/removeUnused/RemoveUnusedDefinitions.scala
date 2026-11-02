/*
rule = RemoveUnused
 */
package test.removeUnused

class RemoveUnusedDefinitions {

  private object PrivateObject
  private class PrivateClass
  private trait PrivateTrait
  private type PrivateType = Int
  private def privateDef = 1
  private val privateVal = 1
  private var privateVar = 1
  @deprecated("", "")
  private def annotatedDef = 1
  @deprecated("", "") private val annotatedVal = 1
  @volatile private var annotatedVar = 1
  private final def finalDef = 1
  private[this] val thisVal = 1
  private lazy val lazyVal = 1
  private implicit def implicitDef: Int = 1
  private def typedDef: Int = 1
  private def paramDef(x: Int) = x
  private val typedVal: Int = 1
  private val blockVal = { println(1); 1 }
  private val multiA, multiB = 1
  // the rule removes it only on 2.13.16, and 2.13 has one expected output
//  private val Some(privatePat) = Option(1)

  def locals(): Unit = {
    object LocalObject
    class LocalClass
    trait LocalTrait
    type LocalType = Int
    def localDef = 1
    val localVal = 1
    var localVar = 1
    lazy val localLazyVal = 1
    @deprecated("", "") def localAnnotatedDef = 1
    val localTypedVal: Int = 1
    val (tupleA, tupleB) = (1, 2)
    val Some(someVal) = Option(1)
    val localMultiA, localMultiB = 1
    def localBlockDef = { val inner = 1; 2 }
    println(localBlockDef)
  }

  def params(param: Int)(implicit implicitParam: Int): Unit = ()

  def lambdas(): Unit = {
    val untyped: Int => Int = x => 1
    val typed = (x: Int) => 1
    val pair = (used: Int, unused: Int) => used
    val pairUntyped: (Int, Int) => Int = (used, unused) => used
    def g(f: Int => Int): Unit = f(1)
    g { implicit x => 1 }
    g { (x) => 1 }
    g { (x: Int) => 1 }
    println((untyped, typed, pair, pairUntyped))
  }

  def patterns(x: Option[Int]): Unit = x match {
    case Some(patVar) => ()
    case Some(typedVar: Int) => ()
    case Some(boundVar @ 1) => ()
    case Some(( /* c */ parens /* d */ )) => ()
    case _ => ()
  }
}
