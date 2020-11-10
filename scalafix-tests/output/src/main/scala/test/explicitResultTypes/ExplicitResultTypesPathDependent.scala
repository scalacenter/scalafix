
package test.explicitResultTypes

import test.explicitResultTypes.PackageObject.{ Nested, T1 }
import test.explicitResultTypes.pkg.Obj
// like https://github.com/tpolecat/doobie/blob/c2e044/modules/core/src/main/scala/doobie/free/Aliases.scala#L10
trait Trait {
  type T1 // like https://github.com/tpolecat/doobie/blob/c2e0445/modules/core/src/main/scala/doobie/free/Aliases.scala#L14
  object Nested {
    type T2
  }
}

class Clazz {
  type T3
}

// like https://github.com/tpolecat/doobie/blob/c2e0445/modules/core/src/main/scala/doobie/hi/package.scala#L25
package object PackageObject extends Trait

package pkg {
  abstract class AbstractClazz {
    trait T4
  }
  object Obj extends Clazz {
    object NestedObj extends AbstractClazz
  }
}

object ExplicitResultTypesPathDependent {
  class Path {
    class B { class C }
    implicit val x: B = new B
    implicit val y: x.C = new x.C
    def gimme(yy: x.C): Nothing = identity(???); gimme(y)
  }
  implicit val b: Path#B = new Path().x
  trait Foo[T] {
    type Self
    def bar: Self
  }
  implicit def foo[T]: Foo[T]#Self = null.asInstanceOf[Foo[T]].bar

  // like https://github.com/tpolecat/doobie/blob/c2e0445/modules/core/src/main/scala/doobie/util/query.scala#L163
  def t1: PackageObject.T1 = ???
  val t1Ref: T1 = t1

  def t2: PackageObject.Nested.T2 = ???
  val t2Ref: Nested.T2 = t2

  def t3: pkg.Obj.T3 = ???
  val t3Ref: Obj.T3 = t3

  def t4: pkg.Obj.NestedObj.T4 = ???
  val t4Ref: Obj.NestedObj.T4 = t4
}
