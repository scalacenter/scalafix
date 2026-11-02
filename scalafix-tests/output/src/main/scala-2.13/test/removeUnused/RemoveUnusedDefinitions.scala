package test.removeUnused

class RemoveUnusedDefinitions {

  private object PrivateObject
  private class PrivateClass
  private trait PrivateTrait
  private type PrivateType = Int
  
  
  
  
  
  
  
  
  
  
  
  
  
  locally { println(1); 1 }
  private val multiA, multiB = 1
  // the rule removes it only on 2.13.16, and 2.13 has one expected output
//  private val Some(privatePat) = Option(1)

  def locals(): Unit = {
    object LocalObject
    class LocalClass
    trait LocalTrait
    type LocalType = Int
    
    
    
    
    
    
    val (tupleA, tupleB) = (1, 2)
    val Some(someVal) = Option(1)
    val localMultiA, localMultiB = 1
    def localBlockDef = { ; 2 }
    println(localBlockDef)
  }

  def params(param: Int)(implicit implicitParam: Int): Unit = ()

  def lambdas(): Unit = {
    val untyped: Int => Int = _ => 1
    val typed = (_: Int) => 1
    val pair = (used: Int, _: Int) => used
    val pairUntyped: (Int, Int) => Int = (used, _) => used
    def g(f: Int => Int): Unit = f(1)
    g { _ => 1 }
    g { (_) => 1 }
    g { (_: Int) => 1 }
    println((untyped, typed, pair, pairUntyped))
  }

  def patterns(x: Option[Int]): Unit = x match {
    case Some(_) => ()
    case Some(_: Int) => ()
    case Some(1) => ()
    case Some(( /* c */ parens /* d */ )) => ()
    case _ => ()
  }
}
