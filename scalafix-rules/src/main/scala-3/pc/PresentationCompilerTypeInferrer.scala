package scalafix.internal.pc

import java.nio.file.Paths

import scala.jdk.CollectionConverters.*
import scala.util.Random
import scala.util.Try

import scala.meta.*
import scala.meta.inputs.Input.File
import scala.meta.inputs.Input.VirtualFile
import scala.meta.pc.PresentationCompiler
import scala.meta.trees.Origin.DialectOnly
import scala.meta.trees.Origin.Parsed

import scalafix.internal.v1.LazyValue
import scalafix.patch.Patch
import scalafix.patch.Patch.empty
import scalafix.v1.*

final class PresentationCompilerTypeInferrer private (
    pc: LazyValue[Option[PresentationCompiler]]
) {

  def shutdownCompiler(): Unit = {
    pc.value.foreach {
      _.shutdown()
    }
  }

  def defnName(defn: Defn): Option[Name] = Option(defn).collect {
    case Defn.Val(_, Pat.Var(name) :: Nil, _, _) => name
    case Defn.Var(_, Pat.Var(name) :: Nil, _, _) => name
    case Defn.Def(_, name, _, _, _, _) => name
  }

  def defnType(
      replace: Token
  )(implicit
      ctx: SemanticDocument
  ): Option[Patch] =
    for {
      pc <- pc.value
    } yield {
      ctx.tree.origin match {
        case _: DialectOnly => empty
        case scala.meta.trees.Origin.None => empty
        case parsed: Parsed =>
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
          val result = pc.insertInferredType(params).get()
          // TODO we need to actually insert after each change
          val allPatches: List[Patch] = result.asScala.toList
            .map { edit =>
              val start = edit.getRange().getStart()
              val last = ctx.tokens.tokens.takeWhile { token =>
                val beforeLine = token.pos.endLine < start.getLine()
                val beforeColumn = token.pos.endLine == start
                  .getLine() && token.pos.endColumn <= start.getCharacter()
                beforeLine || beforeColumn

              }.last
              Patch.addRight(last, edit.getNewText())
            }
          allPatches.reduce[Patch] { case (p1, p2) =>
            p1 + p2
          }
      }
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

  def dynamic(config: Configuration): PresentationCompilerTypeInferrer = {
    val newPc: LazyValue[Option[PresentationCompiler]] =
      LazyValue.from { () =>
        Try(
          configure(
            config,
            Embedded.presentationCompiler(config.scalaVersion)
          )
        )
      }
    new PresentationCompilerTypeInferrer(newPc)
  }

  def static(
      config: Configuration,
      pc: PresentationCompiler
  ): PresentationCompilerTypeInferrer = {
    val newPc: LazyValue[Option[PresentationCompiler]] =
      LazyValue.from { () =>
        Try(configure(config, pc))
      }
    new PresentationCompilerTypeInferrer(newPc)
  }

}
