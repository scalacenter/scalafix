package test.organizeImports

object ExpandInheritance {
  trait Base {
    def inherited: Int = 1
  }
  object Sub extends Base {
    def own: Int = 2
  }
}
