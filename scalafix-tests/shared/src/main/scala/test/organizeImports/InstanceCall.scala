package test.organizeImports

object InstanceCall {
  trait Base {
    def inherited: Int = 1
  }
  object Sub extends Base {
    def own: Int = 2
  }
  val other: Base = Sub
}
