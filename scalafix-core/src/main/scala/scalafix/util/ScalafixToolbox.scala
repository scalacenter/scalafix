package scalafix.util

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.tools.reflect.ToolBoxError
import scala.util.control.NonFatal
import scalafix.rewrite.Rewrite
import scalafix.util.TreeExtractors._

import metaconfig.ConfError
import metaconfig.Configured

object ScalafixToolbox {
  import scala.reflect.runtime.universe._
  import scala.tools.reflect.ToolBox
  private val tb = runtimeMirror(getClass.getClassLoader).mkToolBox()
  private val rewriteCache: mutable.WeakHashMap[String, Any] =
    mutable.WeakHashMap.empty

  def getRewrite[T](code: String): Configured[Rewrite[T]] =
    try {
      Configured.Ok(
        compile(RewriteInstrumentation.instrument(code))
          .asInstanceOf[Rewrite[T]])
    } catch {
      case e: ToolBoxError => ConfError.msg(e.getMessage).notOk
    }

  private def compile(code: String): Any = {
    rewriteCache.getOrElseUpdate(code, tb.eval(tb.parse(code)))
  }
}
