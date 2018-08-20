package scalafix.internal.patch

import scala.meta._
import scala.{meta => m}
import scalafix.v0.{Flags => d}
import scalafix.internal.v1.TreePos
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import scala.meta.internal.ScalametaInternals
import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.semanticdb.SymbolInformation.{Property => p}
import scala.meta.internal.semanticdb.SymbolInformation.{Kind => k}
import scala.meta.internal.{semanticdb => s}
import scala.meta.internal.symtab.SymbolTable
import scalafix.v1.Sym
import scalafix.v1.SemanticDoc
import scalafix.v0
import scalafix.v0._
import DocSemanticdbIndex._
import scalafix.internal.v0.LegacyCodePrinter

class DocSemanticdbIndex(val doc: SemanticDoc)
    extends CrashingSemanticdbIndex
    with SymbolTable {

  override def inputs: Seq[m.Input] =
    doc.input :: Nil

  final override def database: Database =
    Database(documents)

  final override def documents: Seq[Document] =
    Document(
      doc.input,
      doc.sdoc.language.toString(),
      names = this.names.toList,
      messages = this.messages.toList,
      symbols = this.symbols.toList,
      synthetics = this.synthetics.toList
    ) :: Nil

  final override def names: Seq[ResolvedName] =
    doc.sdoc.occurrences.map { o =>
      occurrenceToLegacy(doc, doc.input, o)
    }
  final override def symbols: Seq[ResolvedSymbol] =
    doc.sdoc.symbols.map { s =>
      ResolvedSymbol(
        v0.Symbol(s.symbol),
        infoToDenotation(doc, s)
      )
    }
  final override def synthetics: Seq[Synthetic] =
    doc.sdoc.synthetics.map { s =>
      DocSemanticdbIndex.syntheticToLegacy(doc, s)
    }
  override final def messages: Seq[Message] =
    doc.sdoc.diagnostics.map { diag =>
      val pos = ScalametaInternals.positionFromRange(doc.input, diag.range)
      val severity = diag.severity match {
        case s.Diagnostic.Severity.INFORMATION => Severity.Info
        case s.Diagnostic.Severity.WARNING => Severity.Warning
        case s.Diagnostic.Severity.ERROR => Severity.Error
        case s.Diagnostic.Severity.HINT => Severity.Hint
        case _ => throw new IllegalArgumentException(diag.severity.toString())
      }
      Message(pos, severity, diag.message)
    }

  final override def symbol(position: Position): Option[Symbol] =
    doc.symbols(position).toList.map(s => v0.Symbol(s.value)) match {
      case Nil => None
      case head :: Nil => Some(head)
      case multi => Some(v0.Symbol.Multi(multi))
    }
  final override def symbol(tree: Tree): Option[Symbol] =
    symbol(TreePos.symbol(tree))

  final override def denotation(symbol: Symbol): Option[Denotation] = {
    val info = doc.info(Sym(symbol.syntax))
    if (info.isNone) None
    else Some(DocSemanticdbIndex.infoToDenotation(doc, info.info))
  }
  final override def denotation(tree: Tree): Option[Denotation] =
    symbol(tree).flatMap(denotation)

  override def info(symbol: String): Option[SymbolInformation] = {
    val info = doc.info(Sym(symbol))
    if (info.isNone) None
    else Some(info.info)
  }
}

object DocSemanticdbIndex {

  def infoToDenotation(
      doc: SemanticDoc,
      info: s.SymbolInformation): Denotation = {
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
      info.access match {
        case _: s.PrivateAccess | _: s.PrivateThisAccess |
            _: s.PrivateWithinAccess =>
          dflip(d.PRIVATE)
        case _: s.ProtectedAccess | _: s.ProtectedThisAccess |
            _: s.ProtectedWithinAccess =>
          dflip(d.PROTECTED)
        case _ =>
          ()
      }
      dflags
    }

    new LegacyCodePrinter(doc).convertDenotation(
      info.signature,
      dflags,
      info.name
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
    val pos = ScalametaInternals.positionFromRange(input, occurrence.range)
    ResolvedName(
      pos,
      Symbol(occurrence.symbol),
      isDefinition = occurrence.role.isDefinition
    )
  }

  def syntheticToLegacy(doc: SemanticDoc, synthetic: s.Synthetic): Synthetic = {
    val pos =
      ScalametaInternals.positionFromRange(doc.input, synthetic.range)
    new LegacyCodePrinter(doc).convertSynthetic(synthetic, pos)
  }

}
