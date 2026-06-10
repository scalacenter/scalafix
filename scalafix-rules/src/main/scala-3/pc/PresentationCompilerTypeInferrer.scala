package scalafix.internal.pc

import java.nio.file.Paths

import scala.jdk.CollectionConverters.*
import scala.util.Random
import scala.util.Try

import scala.meta.*
import scala.meta.inputs.Input.File
import scala.meta.inputs.Input.VirtualFile
import scala.meta.pc.PresentationCompiler
import scala.meta.trees.Origin

import scalafix.internal.v1.LazyValue
import scalafix.patch.Patch
import scalafix.patch.Patch.empty
import scalafix.v1.*

final class PresentationCompilerTypeInferrer private (
    pc: LazyValue[PresentationCompiler]
) {

  def shutdownCompiler(): Unit = {
    pc.foreach(_.shutdown())
  }

  def defnName(defn: Defn): Option[Name] = Option(defn).collect {
    case Defn.Val(_, Pat.Var(name) :: Nil, _, _) => name
    case Defn.Var.Initial(_, Pat.Var(name) :: Nil, _, _) => name
    case Defn.Def.Initial(_, name, _, _, _, _) => name
  }

  def defnType(
      replace: Token
  )(implicit
      ctx: SemanticDocument
  ): Patch =
    ctx.tree.origin match {
      case parsed: Origin.Parsed =>
        val text = parsed.source.input.text
        val uri = parsed.source.input match {
          // case Ammonite(input) =>
          case File(path, _) => path.toURI
          case VirtualFile(path, _) => Paths.get(path).toUri()
          case _ => Paths.get(s"./A${Random.nextInt()}.scala").toUri()
        }
        val params = new CompilerOffsetParams(
          uri,
          text,
          replace.pos.end
        )
        val result = pc.value.insertInferredType(params).get()
        result.asScala.toList
          .map { edit =>
            val start = edit.getRange().getStart()
            val last = ctx.tokens.takeWhile { token =>
              val beforeLine = token.pos.endLine < start.getLine()
              val beforeColumn = token.pos.endLine == start
                .getLine() && token.pos.endColumn <= start.getCharacter()
              beforeLine || beforeColumn

            }.last
            Patch.addRight(last, edit.getNewText())
          }
          .asPatch
          .atomic
      case _ => empty
    }
}

/**
 * Prepare the static presentation compiler already in the classpath or download
 * and classload one dynamically.
 */
object PresentationCompilerTypeInferrer {
  private def configure(
      config: Configuration,
      pc: PresentationCompiler
  ): PresentationCompiler = {
    val symbolReplacements =
      config.conf.dynamic.ExplicitResultTypes.symbolReplacements
        .as[Map[String, String]]
        .getOrElse(Map.empty)

    pc.withConfiguration(
      PresentationCompilerConfigImpl(
        symbolPrefixes = symbolReplacements.asJava
      )
    ).newInstance(
      "ExplicitResultTypes",
      config.scalacClasspath.map(_.toNIO).asJava,
      // getting assertion errors if included
      config.scalacOptions.filter(!_.contains("-release")).asJava
    )
  }

  def dynamic(
      config: Configuration
  ): Try[PresentationCompilerTypeInferrer] =
    Try(Embedded.presentationCompiler(config.scalaVersion)).map { pc =>
      static(config, pc)
    }

  def static(
      config: Configuration,
      pc: PresentationCompiler
  ): PresentationCompilerTypeInferrer = {
    val newPc: LazyValue[PresentationCompiler] =
      LazyValue.later(() => configure(config, pc))
    new PresentationCompilerTypeInferrer(newPc)
  }

}
