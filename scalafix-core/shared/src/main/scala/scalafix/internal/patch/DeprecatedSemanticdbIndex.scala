package scalafix.internal.patch

import scala.meta._
import scala.{meta => m}
import scala.{meta => d}
import scalafix.SemanticdbIndex
import scalafix.internal.v1.TreePos
import DeprecatedSemanticdbIndex.DeprecationMessage
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import scala.meta.internal.ScalafixLangmetaHacks
import scala.meta.internal.semanticdb3.SymbolInformation
import scala.meta.internal.semanticdb3.Accessibility.{Tag => a}
import scala.meta.internal.semanticdb3.SymbolInformation.{Property => p}
import scala.meta.internal.semanticdb3.SymbolInformation.{Kind => k}
import scala.meta.internal.{semanticdb3 => s}
import scalafix.internal.util.SymbolTable
import scalafix.v1.Sym
import scalafix.v1.SemanticDoc

class DeprecatedSemanticdbIndex(val doc: SemanticDoc)
    extends SemanticdbIndex
    with SymbolTable {

  @deprecated(DeprecationMessage, "0.6.0")
  final override def classpath: Classpath =
    throw new UnsupportedOperationException
  @deprecated(DeprecationMessage, "0.6.0")
  final override def database: Database =
    throw new UnsupportedOperationException
  @deprecated(DeprecationMessage, "0.6.0")
  final override def names: Seq[ResolvedName] =
    throw new UnsupportedOperationException
  @deprecated(DeprecationMessage, "0.6.0")
  final override def synthetics: Seq[Synthetic] =
    doc.sdoc.synthetics.map(s =>
      DeprecatedSemanticdbIndex.syntheticToLegacy(doc, s))
  @deprecated(DeprecationMessage, "0.6.0")
  final override def documents: Seq[Document] =
    throw new UnsupportedOperationException
  @deprecated(DeprecationMessage, "0.6.0")
  override final def messages: Seq[Message] = doc.sdoc.diagnostics.map { diag =>
    val pos = ScalafixLangmetaHacks.positionFromRange(doc.input, diag.range)
    val severity = diag.severity match {
      case s.Diagnostic.Severity.INFORMATION => Severity.Info
      case s.Diagnostic.Severity.WARNING => Severity.Warning
      case s.Diagnostic.Severity.ERROR => Severity.Error
      case s.Diagnostic.Severity.HINT => Severity.Hint
      case _ => throw new IllegalArgumentException(diag.severity.toString())
    }
    Message(pos, severity, diag.message)
  }
  @deprecated(DeprecationMessage, "0.6.0")
  final override def withDocuments(documents: Seq[Document]): SemanticdbIndex =
    throw new UnsupportedOperationException

  @deprecated(DeprecationMessage, "0.6.0")
  final override def symbol(position: Position): Option[Symbol] =
    doc.symbols(position).toList.map(s => m.Symbol(s.value)) match {
      case Nil => None
      case head :: Nil => Some(head)
      case multi => Some(m.Symbol.Multi(multi))
    }
  @deprecated(DeprecationMessage, "0.6.0")
  final override def symbol(tree: Tree): Option[Symbol] =
    symbol(TreePos.symbol(tree))

  @deprecated(DeprecationMessage, "0.6.0")
  final override def denotation(symbol: Symbol): Option[Denotation] = {
    val info = doc.info(Sym(symbol.syntax))
    if (info.isNone) None
    else Some(DeprecatedSemanticdbIndex.infoToDenotation(info.info))
  }
  @deprecated(DeprecationMessage, "0.6.0")
  final override def denotation(tree: Tree): Option[Denotation] =
    symbol(tree).flatMap(denotation)

  override def info(symbol: String): Option[SymbolInformation] = {
    val info = doc.info(Sym(symbol))
    if (info.isNone) None
    else Some(info.info)
  }
}

object DeprecatedSemanticdbIndex {
  final val DeprecationMessage = "Use SemanticDoc instead"

  def infoToDenotation(info: s.SymbolInformation): Denotation = {
    val dflags = {
      var dflags = 0L
      def dflip(dbit: Long) = dflags ^= dbit
      if (info.language.isJava) dflip(d.JAVADEFINED)
      info.kind match {
        case k.LOCAL => dflip(d.LOCAL)
        case k.FIELD => dflip(d.FIELD)
        case k.METHOD => dflip(d.METHOD)
        case k.CONSTRUCTOR => dflip(d.CTOR)
        case k.MACRO => dflip(d.MACRO)
        case k.TYPE => dflip(d.TYPE)
        case k.PARAMETER => dflip(d.PARAM)
        case k.SELF_PARAMETER => dflip(d.SELFPARAM)
        case k.TYPE_PARAMETER => dflip(d.TYPEPARAM)
        case k.OBJECT => dflip(d.OBJECT)
        case k.PACKAGE => dflip(d.PACKAGE)
        case k.PACKAGE_OBJECT => dflip(d.PACKAGEOBJECT)
        case k.CLASS => dflip(d.CLASS)
        case k.TRAIT => dflip(d.TRAIT)
        case k.INTERFACE => dflip(d.INTERFACE)
        case _ => ()
      }
      def stest(p: s.SymbolInformation.Property) =
        (info.properties & p.value) != 0
      if (stest(p.ABSTRACT)) dflip(d.ABSTRACT)
      if (stest(p.FINAL)) dflip(d.FINAL)
      if (stest(p.SEALED)) dflip(d.SEALED)
      if (stest(p.IMPLICIT)) dflip(d.IMPLICIT)
      if (stest(p.LAZY)) dflip(d.LAZY)
      if (stest(p.CASE)) dflip(d.CASE)
      if (stest(p.COVARIANT)) dflip(d.COVARIANT)
      if (stest(p.CONTRAVARIANT)) dflip(d.CONTRAVARIANT)
      if (stest(p.VAL)) dflip(d.VAL)
      if (stest(p.VAR)) dflip(d.VAR)
      if (stest(p.STATIC)) dflip(d.STATIC)
      if (stest(p.PRIMARY)) dflip(d.PRIMARY)
      if (stest(p.ENUM)) dflip(d.ENUM)
      info.accessibility.map(_.tag) match {
        case Some(a.PRIVATE | a.PRIVATE_THIS | a.PRIVATE_WITHIN) =>
          dflip(d.PRIVATE)
        case Some(a.PROTECTED | a.PROTECTED_THIS | a.PROTECTED_WITHIN) =>
          dflip(d.PROTECTED)
        case _ =>
          ()
      }
      dflags
    }
    Denotation(
      dflags,
      info.name,
      "",
      Nil
    )
  }

  // Input.Synthetic is gone so we hack it here by extending java.io.InputStream and
  // piggy backing on Input.Stream.
  final case class InputSynthetic(
      value: String,
      input: Input,
      start: Int,
      end: Int
  ) extends InputStream {
    val in = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))
    override def read(): Int = in.read()
  }

  def occurrenceToLegacy(
      doc: SemanticDoc,
      input: Input,
      occurrence: s.SymbolOccurrence): ResolvedName = {
    val pos = ScalafixLangmetaHacks.positionFromRange(input, occurrence.range)
    ResolvedName(
      pos,
      Symbol(occurrence.symbol),
      isDefinition = occurrence.role.isDefinition
    )
  }

  def syntheticToLegacy(doc: SemanticDoc, synthetic: s.Synthetic): Synthetic = {
    val pos =
      ScalafixLangmetaHacks.positionFromRange(doc.input, synthetic.range)
    val names: List[ResolvedName] = synthetic.text match {
      case Some(td) =>
        val input = Input.Stream(
          InputSynthetic(td.text, doc.input, pos.start, pos.end),
          StandardCharsets.UTF_8)
        td.occurrences.iterator
          .map(o => occurrenceToLegacy(doc, input, o))
          .toList
      case _ =>
        Nil
    }
    Synthetic(
      pos,
      synthetic.text.fold("")(_.text),
      names
    )
  }
}
