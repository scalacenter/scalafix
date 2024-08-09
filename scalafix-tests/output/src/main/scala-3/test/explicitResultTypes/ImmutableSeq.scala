package test.explicitResultTypes

import scala.collection.immutable.Seq

object ImmutableSeq {
  def seq(): collection.Seq[Int] = Seq.empty[Int]
  def scalaSeq(): scala.Seq[Int] = Seq.empty[Int]
  def foo: scala.collection.Seq[Int] = seq()
  def scalaFoo: Seq[Int] = scalaSeq()

  def foo(a: Int*): List[Int] = identity(a.toList)
}