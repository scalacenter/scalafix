package test.organizeImports

import test.organizeImports.ExpandWildcardEnclosingScope.M.direct

object ExpandWildcardEnclosingScope {
  trait Base {
    def inherited: Int = 1
  }
  object M extends Base {
    val direct: Int = 2
    val own: Int = 3
    def useOwn: Int = own
  }
  class Sub extends Base {
    def viaInheritance: Int = inherited
  }
  val anon = new Base { def viaAnonymous: Int = inherited }
  val used: Int = direct
}
