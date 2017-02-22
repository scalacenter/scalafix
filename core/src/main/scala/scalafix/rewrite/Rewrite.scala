package scalafix.rewrite

import scala.collection.immutable.Seq
import scala.meta._
import scalafix.util.Patch
import scala.collection.immutable.Seq
import scalafix.config.ReaderUtil

/** A rewrite is a named RewriteCtx[A] => Seq[Patch] function.
  * @tparam A Required api in [[ScalafixMirror]]. Example values:
  *           [[ScalafixMirror]] for scalafix-nsc,
  *           [[scala.meta.Mirror]] when using scalahost or
  *           [[Any]] for syntactic rewrites.
  */
abstract class Rewrite[-A](implicit sourceName: sourcecode.Name) {
  def name: String = sourceName.value
  override def toString: String = name
  def rewrite[B <: A](ctx: RewriteCtx[B]): Seq[Patch]
}

object Rewrite {
  def withName[A](name: String)(f: RewriteCtx[A] => Seq[Patch]): Rewrite[A] =
    apply(f)(sourcecode.Name(name))
  def apply[T](f: RewriteCtx[T] => Seq[Patch])(
      implicit name: sourcecode.Name): Rewrite[T] = new Rewrite[T]() {
    override def rewrite[B <: T](ctx: RewriteCtx[B]): Seq[Patch] = f(ctx)
  }
}
