package test.removeUnused

object RemoveUnusedParentheses {
  def main(args: Array[String]): Unit = {
    // Test case from issue - multiline parenthesized expression
    val a: Int =
      (3
        + 4)
    
    // Additional test cases
    val b = (1 + 2)
    
    val c =
      (5 * 6)
    
    // Nested parentheses
    val d = ((7 + 8))
    
    // Complex expression with parentheses
    (1 + 2) * 3
    
    // Function call wrapped in parentheses - critical test case
    val f = (Some(42))
    
    // Expression ending with function call wrapped in parentheses
    val g = (List(1, 2, 3).head)
    
    // Nested function calls with parentheses
    val h = (Option(Some(1)))
    
    println("done")
  }
}
