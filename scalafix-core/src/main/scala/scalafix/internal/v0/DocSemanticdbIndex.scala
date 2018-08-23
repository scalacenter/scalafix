package scalafix.internal.v0

import scala.meta._
import scala.meta.internal.ScalametaInternals
import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.semanticdb.SymbolInformation.{Kind => k}
import scala.meta.internal.semanticdb.SymbolInformation.{Property => p}
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => s}
import scala.{meta => m}
import scalafix.internal.patch.CrashingSemanticdbIndex
import scalafix.internal.v0.DocSemanticdbIndex._
import scalafix.internal.v1.TreePos
import scalafix.v0
import scalafix.v0.{Flags => d}
import scalafix.v1

class DocSemanticdbIndex(val doc: v1.SemanticDoc)
    extends CrashingSemanticdbIndex
    with SymbolTable {

  override def inputs: Seq[m.Input] =
    doc.input :: Nil

  final override def database: v0.Database =
    v0.Database(documents)

  final override def documents: Seq[v0.Document] =
    v0.Document(
      doc.input,
      doc.internal.textDocument.language.toString(),
      names = this.names.toList,
      messages = this.messages.toList,
      symbols = this.symbols.toList,
      synthetics = this.synthetics.toList
    ) :: Nil

  final override def names: Seq[v0.ResolvedName] =
    doc.internal.textDocument.occurrences.map { o =>
      occurrenceToLegacy(doc, doc.input, o)
    }
  final override def symbols: Seq[v0.ResolvedSymbol] =
    doc.internal.textDocument.symbols.map { s =>
      v0.ResolvedSymbol(
        v0.Symbol(s.symbol),
        infoToDenotation(doc, s)
      )
    }
  final override def synthetics: Seq[v0.Synthetic] = {
    doc.internal.textDocument.synthetics.map { s =>
      DocSemanticdbIndex.syntheticToLegacy(doc, s)
    }
  }
  override final def messages: Seq[v0.Message] =
    doc.internal.textDocument.diagnostics.map { diag =>
      val pos = ScalametaInternals.positionFromRange(doc.input, diag.range)
      val severity = diag.severity match {
        case s.Diagnostic.Severity.INFORMATION => v0.Severity.Info
        case s.Diagnostic.Severity.WARNING => v0.Severity.Warning
        case s.Diagnostic.Severity.ERROR => v0.Severity.Error
        case s.Diagnostic.Severity.HINT => v0.Severity.Hint
        case _ => throw new IllegalArgumentException(diag.severity.toString())
      }
      v0.Message(pos, severity, diag.message)
    }

  final override def symbol(position: Position): Option[v0.Symbol] =
    doc.internal.symbols(position).toList.map(s => v0.Symbol(s.value)) match {
      case Nil => None
      case head :: Nil => Some(head)
      case multi => Some(v0.Symbol.Multi(multi))
    }
  final override def symbol(tree: Tree): Option[v0.Symbol] =
    symbol(TreePos.symbol(tree))

  final override def denotation(symbol: v0.Symbol): Option[v0.Denotation] = {
    doc.internal.info(v1.Symbol(symbol.syntax)).map { info =>
      DocSemanticdbIndex.infoToDenotation(doc, info.info)
    }
  }
  final override def denotation(tree: Tree): Option[v0.Denotation] =
    symbol(tree).flatMap(denotation)

  override def info(symbol: String): Option[SymbolInformation] = {
    doc.internal.info(v1.Symbol(symbol)).map(_.info)
  }
}

object DocSemanticdbIndex {

  def infoToDenotation(
      doc: v1.SemanticDoc,
      info: s.SymbolInformation): v0.Denotation = {
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
      info.displayName
    )
  }

  def occurrenceToLegacy(
      doc: v1.SemanticDoc,
      input: Input,
      occurrence: s.SymbolOccurrence): v0.ResolvedName = {
    val pos = ScalametaInternals.positionFromRange(input, occurrence.range)
    v0.ResolvedName(
      pos,
      v0.Symbol(occurrence.symbol),
      isDefinition = occurrence.role.isDefinition
    )
  }

  def syntheticToLegacy(
      doc: v1.SemanticDoc,
      synthetic: s.Synthetic): v0.Synthetic = {
    new LegacyCodePrinter(doc).convertSynthetic(synthetic)
  }
}
