package test.removeUnused

object RemoveUnusedTerms {

  def foo = {
    
    println(5)
    
    println(0)
    println(1)
    
    
    val xy = 42 // scalafix:ok RemoveUnusedTerms
  }

  val dd = 0
  def f(x: Int) = "unused"
  

  locally { println("foo"); 1 }
  locally /* preserved */ {
    println("foo")
    1
  }
  locally /* preserved */
    // preserved
    {
      println("foo")
      1
    }
}
