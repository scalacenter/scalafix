package scalafix.internal.rule

import scala.util.control.NonFatal

import scala.meta._
import scala.meta.internal.pc.ScalafixGlobal

import metaconfig.Configured
import scalafix.internal.compat.CompilerCompat._
import scalafix.internal.pc.PcExplicitResultTypes
import scalafix.internal.v1.LazyValue
import scalafix.patch.Patch
import scalafix.v1._

final class ExplicitResultTypes(
    val config: ExplicitResultTypesConfig,
    global: LazyValue[Option[ScalafixGlobal]],
    fallback: LazyValue[Option[PcExplicitResultTypes]]
) extends ExplicitResultTypesBase[Scala2Printer] {

  def this() = this(
    ExplicitResultTypesConfig.default,
    LazyValue.now(None),
    LazyValue.now(None)
  )

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
      config.conf // Support deprecated explicitReturnTypes config
        .getOrElse("explicitReturnTypes", "ExplicitResultTypes")(
          ExplicitResultTypesConfig.default
        )
        .map(c =>
          new ExplicitResultTypes(
            c,
            LazyValue.now(None),
            LazyValue.now(Option(PcExplicitResultTypes.dynamic(config)))
          )
        )
    } else {
      config.conf // Support deprecated explicitReturnTypes config
        .getOrElse("explicitReturnTypes", "ExplicitResultTypes")(
          ExplicitResultTypesConfig.default
        )
        .map(c => new ExplicitResultTypes(c, newGlobal, LazyValue.now(None)))
    }
  }

  override def fix(implicit ctx: SemanticDocument): Patch =
    try {
      val typesLazy = global.value.map(new CompilerTypePrinter(_, config))
      implicit val printer = new Scala2Printer(typesLazy, fallback)
      unsafeFix()
    } catch {
      case _: CompilerException if !config.fatalWarnings =>
        Patch.empty
    }
}

class Scala2Printer(
    globalPrinter: Option[CompilerTypePrinter],
    fallback: LazyValue[Option[PcExplicitResultTypes]]
) extends Printer {
  def defnType(
      defn: Defn,
      replace: Token,
      space: String
  )(implicit
      ctx: SemanticDocument
  ): Option[Patch] = {

    globalPrinter match {
      case Some(types) =>
        for {
          name <- ExplicitResultTypesBase.defnName(defn)
          defnSymbol <- name.symbol.asNonEmpty
          patch <- types.toPatch(name.pos, defnSymbol, replace, defn, space)
        } yield {
          patch
        }
      case None =>
        fallback.value.flatMap { fallbackExplicit =>
          fallbackExplicit.defnType(replace)
        }
    }

  }
}
