/*
rule = NoValInForComprehension
 */
package test

object NoValInForComprehension {
  for {
    n <- List(1, 2, 3)
    val inc = n + 1
    inc2 = inc + 1
    val inc3 = inc2 + 1
    if inc > 1
  } yield inc2
  for {
    n <- List(1, 2, 3)
    val inc = n + 1
    inc2 = inc + 1
    val a = 1 // scalafix:ok NoValInForComprehension
    val inc3 = inc2 + 1
    if inc > 1
  } ()
}
