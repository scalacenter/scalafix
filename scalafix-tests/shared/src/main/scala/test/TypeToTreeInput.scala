package test

import java.util.Map
import scala.collection.immutable.TreeMap
import scala.language.higherKinds
// FIXME: https://github.com/scalameta/scalameta/issues/1625
// The import on Either below is needed to workaround that issue.
import scala.util.Either

trait TypeToTreeInput {
  type A
  val a: Int
  val b: String
  val c: TreeMap[String, String]
  val d: Int
  val e: (Int, Int)
  val e2: Int => Int
  val f: Map.Entry[Int, Int]
  val g: TypeToTreeInput#A
  val h: TypeToTreeInput
  val i: h.A
  def j[T]: TypeToTreeInputBox[T]#A
  val k: Either[List[Long], Long]
  val l: TypeToTreeInputBox.Nested
  val m: Either.type
  def n(arg: String): arg.type
  def o(arg: TypeToTreeInputBox.type): arg.Nested
  var p: Int
  def q(byName: => String): String
  def r(vararg: String*): String
  def s[c[x] <: Seq[x]](e: c[String]): c[Int]
  type S = Functor[({ type T[A] = Either[Int, A] })#T]
  private def t1: Unit = ()
  private[test] def t2: Unit
  private[this] def t3: Unit = ()
  protected def t4: Unit
  protected[test] def t5: Unit
  protected[this] def t6: Unit = ()
}

case class Repeated(a: AnyRef*)

trait Functor[C[_]]

trait TypeParams[A, B >: String <: CharSequence]
trait ParentsTrait extends Comparable[Int] with AutoCloseable
class ClassExtendsTrait extends Serializable
class ClassExtendsClass(y: Int) extends ClassExtendsTrait with Serializable
case class CaseClass()

abstract class AbstractClass
final class FinalClass
sealed abstract class SealedAbstractClass

trait TypeToTreeInputBox[T] { type A }
object TypeToTreeInputBox {
  class Nested
  def a(b: Int): Int = b
}
package object pkg {
  def a: Int = 1
}

class TypeToTreeClass private (x: Int)(y: String)
