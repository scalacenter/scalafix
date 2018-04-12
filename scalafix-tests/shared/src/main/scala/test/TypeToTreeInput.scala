package test

import java.util.Map
import scala.collection.immutable.TreeMap
import scala.language.higherKinds
import scala.language.reflectiveCalls

trait TypeToTreeInput {
  type A
  val a: Int
  val b: String
  val c: TreeMap[String, String]
  val d: Int
  val e: (Int, Int)
  val e2: Int => Int
  val f: Map.Entry[Int, Int]
  val g: TypeToTreeInput.this.A
  val h: TypeToTreeInput
  val i: TypeToTreeInput.this.h.A
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
}

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
}

class TypeToTreeClass private (x: Int)(y: String)
