package scalafix.internal.rule

import scala.util.control.NonFatal

import scala.meta._
import scala.meta.internal.pc.ScalafixGlobal

import metaconfig.Configured
import scalafix.internal.compat.CompilerCompat._
import scalafix.internal.v1.LazyValue
import scalafix.patch.Patch
import scalafix.v1._

final class ExplicitResultTypes(
    val config: ExplicitResultTypesConfig,
    global: LazyValue[Option[ScalafixGlobal]]
) extends ExplicitResultTypesBase[Scala2Printer] {

  def this() = this(ExplicitResultTypesConfig.default, LazyValue.now(None))

  override def afterComplete(): Unit = {
    shutdownCompiler()
  }

  private def shutdownCompiler(): Unit = {
    global.foreach(_.foreach(g => {
      try {
        g.askShutdown()
        g.closeCompat()
      } catch {
        case NonFatal(_) =>
      }
    }))
  }

  override def withConfiguration(config: Configuration): Configured[Rule] = {
    val symbolReplacements =
      config.conf.dynamic.ExplicitResultTypes.symbolReplacements
        .as[Map[String, String]]
        .getOrElse(Map.empty)
    val newGlobal: LazyValue[Option[ScalafixGlobal]] =
      if (config.scalacClasspath.isEmpty) {
        LazyValue.now(None)
      } else {
        LazyValue.from { () =>
          ScalafixGlobal.newCompiler(
            config.scalacClasspath,
            config.scalacOptions,
            symbolReplacements
          )
        }
      }
    val inputBinaryScalaVersion =
      stripPatchVersion(config.scalaVersion)
    val runtimeBinaryScalaVersion =
      stripPatchVersion(compilerScalaVersion)
    if (
      config.scalacClasspath.nonEmpty && inputBinaryScalaVersion != runtimeBinaryScalaVersion
    ) {
      Configured.error(
        s"The ExplicitResultTypes rule needs to run with the same Scala binary version as the one used to compile target sources ($inputBinaryScalaVersion). " +
          s"To fix this problem, either remove ExplicitResultTypes from .scalafix.conf or make sure Scalafix is loaded with $inputBinaryScalaVersion."
      )
    } else {
      config.conf // Support deprecated explicitReturnTypes config
        .getOrElse("explicitReturnTypes", "ExplicitResultTypes")(
          ExplicitResultTypesConfig.default
        )
        .map(c => new ExplicitResultTypes(c, newGlobal))
    }
  }

  override def fix(implicit ctx: SemanticDocument): Patch =
    try {
      val typesLazy = global.value.map(new CompilerTypePrinter(_, config))
      implicit val printer = new Scala2Printer(typesLazy)
      unsafeFix()
    } catch {
      case _: CompilerException if !config.fatalWarnings =>
        Patch.empty
    }
}

class Scala2Printer(
    globalPrinter: Option[CompilerTypePrinter]
) extends Printer {
  def defnType(
      defn: Defn,
      replace: Token,
      space: String
  )(implicit
      ctx: SemanticDocument
  ): Option[Patch] =
    for {
      name <- ExplicitResultTypesBase.defnName(defn)
      defnSymbol <- name.symbol.asNonEmpty
      printer <- globalPrinter
      patch <- printer.toPatch(name.pos, defnSymbol, replace, defn, space)
    } yield {
      patch
    }
}
