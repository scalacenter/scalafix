package test.organizeImports.wildcards3

object Apple
object Banana
object Cherry

trait Alpha
given alpha: Alpha = ???
def topLevelDef: Int = 1
val topLevelVal: Int = 2
class Box[T](val v: T)
