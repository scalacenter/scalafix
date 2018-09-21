package scalafix.docs

import scala.meta.inputs.Input
import scala.meta.interactive.InteractiveSemanticdb
import scala.meta.internal.semanticdb.Print
import scala.meta.internal.symtab.GlobalSymbolTable
import scala.meta.metap.Format
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.v1.InternalSemanticDoc
import scalafix.patch.Patch
import scalafix.internal.patch.PatchInternals
import scalafix.v1.RuleName
import scalafix.v1.SemanticDocument
import scalafix.v1.Symbol
import scalafix.v1.SymbolInformation
import scalafix.v1.Symtab
import scalafix.v1.SyntacticDocument

object PatchDocs {
  implicit class XtensionPatch(p: Patch) {
    def output(implicit doc: SemanticDocument): String = {
      val (obtained, _) =
        PatchInternals.semantic(
          Map(RuleName("patch") -> p),
          doc,
          suppress = false)
      obtained
    }
    def showDiff(context: Int = 0)(implicit doc: SemanticDocument): Unit = {
      printDiff(unifiedDiff(p.output, context))
    }
  }
  implicit class XtensionPatchs(p: Iterable[Patch]) {
    def showLints()(implicit doc: SemanticDocument): Unit = {
      val (_, diagnostics) = PatchInternals.semantic(
        Map(RuleName("RuleName") -> p.asPatch),
        doc,
        suppress = false)
      diagnostics.foreach { diag =>
        println(diag.formattedMessage)
      }
    }
    def showDiff(context: Int = 0)(implicit doc: SemanticDocument): Unit = {
      p.asPatch.showDiff(context)
    }
    def showOutput()(implicit doc: SemanticDocument): Unit = {
      println(p.asPatch.output)
    }
  }
  def printDiff(diff: String): Unit = {
    val trimmed = diff.linesIterator.drop(3).mkString("\n")
    val message =
      if (trimmed.isEmpty) "<no diff>"
      else trimmed
    println(message)
  }

  def unifiedDiff(obtained: String, context: Int)(
      implicit doc: SemanticDocument): String = {
    val in = Input.VirtualFile("before patch", doc.input.text)
    val out = Input.VirtualFile("after  patch", obtained)
    PatchInternals.unifiedDiff(in, out, context)
  }
  lazy val scalacOptions = List(
    "-Ywarn-unused",
    "-P:semanticdb:synthetics:on"
  )
  lazy val compiler = InteractiveSemanticdb.newCompiler(scalacOptions)
  lazy val symtab = GlobalSymbolTable(ClasspathOps.thisClasspath)
  lazy val scalafixSymtab = new Symtab { self =>
    override def info(symbol: Symbol): Option[SymbolInformation] = {
      symtab.info(symbol.value).map(new SymbolInformation(_)(self))
    }
  }
  def fromStatement(code: String, debug: Boolean = false): SemanticDocument = {
    fromString(
      "object Main {\n" + code + "\n}",
      debug,
      statement = Some(code)
    )
  }
  def fromString(
      code: String,
      debug: Boolean = false,
      statement: Option[String] = None
  ): SemanticDocument = {
    val filename = "Main.scala"
    println("```scala")
    statement match {
      case Some(stat) =>
        println(stat.trim)
      case None =>
        println("// " + filename)
        println(code.trim)
    }
    println("```")
    val textDocument = InteractiveSemanticdb.toTextDocument(
      compiler,
      code,
      scalacOptions.filter(_.startsWith("-P:semanticdb")))
    if (debug) {
      println("```")
      println(Print.document(Format.Compact, textDocument))
      scala.reflect.classTag
      println("```")
    }

    val input = Input.VirtualFile(filename, code)
    val doc = SyntacticDocument.fromInput(input)
    val internal = new InternalSemanticDoc(
      doc,
      textDocument,
      symtab
    )
    val sdoc = new SemanticDocument(internal)
    if (textDocument.diagnostics.exists(_.severity.isError)) {
      sdoc.diagnostics.foreach(diag => println(diag))
      throw new IllegalArgumentException(sdoc.diagnostics.mkString("\n"))
    }
    sdoc
  }
}
