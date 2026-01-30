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
    
    println("done")
  }
}
