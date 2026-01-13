package test.removeUnused

object RemoveUnusedParentheses {
  def main(args: Array[String]): Unit = {
    // Test case from issue - multiline parenthesized expression
    3
        + 4
    
    // Additional test cases
    1 + 2
    
    5 * 6
    
    // Nested parentheses
    (7 + 8)
    
    // Complex expression with parentheses
    (1 + 2) * 3
    
    println("done")
  }
}
