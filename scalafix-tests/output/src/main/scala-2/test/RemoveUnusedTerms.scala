package test

object RemoveUnusedTerms {

  def foo {
    
    println(5)
    
    println(0)
    println(1)
    
    
    val xy = 42 // scalafix:ok RemoveUnusedTerms
  }

  val dd = 0
  def f(x: Int) = "unused"
  

  def g(x: String => Unit): Unit = ???
  g{_ => println("g")}
  def pf(x: PartialFunction[Any, Unit]): Unit = ???
  case class A(a: Int)
  pf{
    case _: String => ???
    case (_: Int) => ???
    case (_: Int, b) => println(b)
    case A(_) => ???
    case x :: (_, _) :: Nil => println(x)
    case _ => ???
  }
  try ??? catch {case _: Exception => ???}
}
