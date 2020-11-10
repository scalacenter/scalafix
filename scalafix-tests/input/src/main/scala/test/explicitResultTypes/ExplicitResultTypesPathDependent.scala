/*
rules = ExplicitResultTypes
ExplicitResultTypes.skipSimpleDefinitions = ["Lit"]
 */
package test.explicitResultTypes

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
    implicit val x = new B
    implicit val y = new x.C
    def gimme(yy: x.C) = identity(???); gimme(y)
  }
  implicit val b = new Path().x
  trait Foo[T] {
    type Self
    def bar: Self
  }
  implicit def foo[T] = null.asInstanceOf[Foo[T]].bar

  // like https://github.com/tpolecat/doobie/blob/c2e0445/modules/core/src/main/scala/doobie/util/query.scala#L163
  def t1: PackageObject.T1 = ???
  val t1Ref = t1

  def t2: PackageObject.Nested.T2 = ???
  val t2Ref = t2

  def t3: pkg.Obj.T3 = ???
  val t3Ref = t3

  def t4: pkg.Obj.NestedObj.T4 = ???
  val t4Ref = t4
}
