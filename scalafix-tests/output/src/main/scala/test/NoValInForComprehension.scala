package test

object NoValInForComprehension {
  for {
    n <- List(1, 2, 3)
    inc = n + 1
    inc2 = inc + 1
    inc3 = inc2 + 1
    if inc > 1
  } yield inc2
  for {
    n <- List(1, 2, 3)
    inc = n + 1
    inc2 = inc + 1
    val a = 1 // scalafix:ok NoValInForComprehension
    inc3 = inc2 + 1
    if inc > 1
  } ()
}
