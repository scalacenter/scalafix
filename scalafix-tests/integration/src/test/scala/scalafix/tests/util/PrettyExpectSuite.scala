package scalafix.tests.util

import scala.meta.internal.ScalametaInternals
import scala.meta.internal.semanticdb.Scala._

import org.typelevel.paiges.Doc
import scalafix.internal.util.PositionSyntax._
import scalafix.internal.util.Pretty
import scalafix.internal.v1.DocumentFromProtobuf
import scalafix.v1

// Run `sbt save-expect` to acknowledge changes
class PrettyExpectSuite extends ExpectSuite {
  def filename: String = "PrettyTest.scala"
  def obtained(): String = {
    val synthetics = sdoc.internal.textDocument.synthetics.map { synth =>
      val pos = ScalametaInternals.positionFromRange(sdoc.input, synth.range)
      val tree = DocumentFromProtobuf.convert(synth, sdoc.internal)
      pos -> Pretty.pretty(tree)
    }
    val types = sdoc.internal.textDocument.occurrences.collect {
      case occurence
          if occurence.role.isDefinition &&
            !occurence.symbol.isPackage &&
            occurence.symbol.isGlobal =>
        val pos =
          ScalametaInternals.positionFromRange(sdoc.input, occurence.range)
        val info = sdoc.info(v1.Symbol(occurence.symbol)).get
        pos -> Pretty.pretty(info)
    }
    val rows = (synthetics ++ types)
      .sortBy { case (pos, _) =>
        pos.start
      }
      .map { case (pos, doc) =>
        s"[${pos.rangeNumber}]: " -> doc

      }
    Doc.tabulate(rows.toList).render(80)
  }
}
