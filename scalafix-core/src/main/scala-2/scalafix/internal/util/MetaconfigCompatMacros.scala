
package scalafix.internal.util

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

import metaconfig.generic

object MetaconfigCompatMacros {
  // metaconfig.generic.deriveSurface cannot be used within scalafix-core as macro implementations cannot
  // be used in the same compilation run that defines them
  def deriveSurfaceOrig[T]: metaconfig.generic.Surface[T] =
    macro metaconfig.internal.Macros.deriveSurfaceImpl[T]
}

class MetaconfigCompatMacros(override val c: blackbox.Context)
    extends metaconfig.internal.Macros(c) {

  import c.universe._

  // Provide an actionnable error in case of linking mismatch, see https://github.com/scalacenter/scalafix/pull/1530#issuecomment-1061174301
  override def deriveSurfaceImpl[T: c.WeakTypeTag]: Tree = {
    val unsafeTree = super.deriveSurfaceImpl[T]

    val buildVersion = scalafix.Versions.nightly
    q"""
      val runtimeVersion = _root_.scalafix.Versions.nightly
      try {
        new java.util.concurrent.Callable[${weakTypeOf[generic.Surface[T]]}] {
          // Since the unsafeTree gets lifted into a static block, we wrap its execution
          // inside a nested class to be able to intercept the NoClassDefFoundError
          override def call = $unsafeTree
        }.call
      } catch {
        case e: NoClassDefFoundError =>
          throw new RuntimeException(
            "Scalafix version " + runtimeVersion + " detected - please upgrade to " + $buildVersion + " or later",
            e
          )
      }
    """
  }
}