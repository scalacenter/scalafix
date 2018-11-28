package scalafix.docs

import org.typelevel.paiges.Doc
import scala.meta.inputs.Input
import scala.meta.interactive.InteractiveSemanticdb
import scala.meta.internal.semanticdb.Print
import scala.meta.internal.symtab.GlobalSymbolTable
import scala.meta.metap.Format
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.v1.InternalSemanticDoc
import scalafix.patch.Patch
import scalafix.internal.patch.PatchInternals
import scalafix.internal.util.Pretty
import scalafix.v1._

object PatchDocs {
  object DocPrinter
      extends pprint.PPrinter(
        colorLiteral = fansi.Attrs.Empty,
        colorApplyPrefix = fansi.Attrs.Empty
      ) {
    override def treeify(x: Any): pprint.Tree = {
      def print(doc: Doc): pprint.Tree =
        pprint.Tree.Lazy { ctx =>
          doc
            .nested(ctx.indentCount * ctx.indentStep)
            .renderStream(ctx.leftOffset)
            .toIterator
        }
      x match {
        case t: SemanticTree =>
          print(Pretty.pretty(t))
        case t: SemanticType =>
          print(Pretty.pretty(t))
        case _ =>
          super.treeify(x)
      }
    }
  }
  def println(any: Any): Unit = {
    any match {
      case s: String =>
        Predef.println(s)
      case _ =>
        Predef.println(any.toStringLineWrapped(60))
    }
  }

  implicit class XtensionAnyLineWrapped(any: Any) {
    def toStringLineWrapped(width: Int): String =
      DocPrinter.tokenize(any, width = width).mkString
  }
  implicit class XtensionPatch(p: Patch) {
    def output(implicit doc: SemanticDocument): String = {
      val (obtained, _) =
        PatchInternals.semantic(
          Map(RuleName("patch") -> p),
          doc,
          suppress = false
        )
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
        suppress = false
      )
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
    val trimmed = diff.lines.drop(3).mkString("\n")
    val message =
      if (trimmed.isEmpty) "<no diff>"
      else trimmed
    println(message)
  }

  def unifiedDiff(obtained: String, context: Int)(
      implicit doc: SemanticDocument
  ): String = {
    val in = Input.VirtualFile("before patch", doc.input.text)
    val out = Input.VirtualFile("after  patch", obtained)
    PatchInternals.unifiedDiff(in, out, context)
  }
  lazy val scalacOptions = List(
    "-Ywarn-unused",
    "-P:semanticdb:synthetics:on"
  )
  lazy val compiler = InteractiveSemanticdb.newCompiler(scalacOptions)
  lazy val symtab =
    GlobalSymbolTable(ClasspathOps.thisClasspath, includeJdk = true)
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
      statement: Option[String] = None,
      filename: String = "Main.scala"
  ): SemanticDocument = {
    Predef.println("```scala")
    statement match {
      case Some(stat) =>
        Predef.println(stat.trim)
      case None =>
        Predef.println("// " + filename)
        Predef.println(code.trim)
    }
    Predef.println("```")
    val textDocument = InteractiveSemanticdb.toTextDocument(
      compiler,
      code,
      scalacOptions.filter(_.startsWith("-P:semanticdb"))
    )
    if (debug) {
      Predef.println("```")
      Predef.println(Print.document(Format.Compact, textDocument))
      scala.reflect.classTag
      Predef.println("```")
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
