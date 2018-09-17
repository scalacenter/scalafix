package scalafix.docs

import scala.meta.inputs.Input
import scala.meta.interactive._
import scala.meta.internal.semanticdb.Print
import scala.meta.internal.symtab.GlobalSymbolTable
import scala.meta.metap.Format
import scalafix.internal.v1.InternalSemanticDoc
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.util.SymbolOps
import scalafix.patch.Patch
import scalafix.v1._

import scala.meta.internal.semanticdb.Scala.{Descriptor, Symbols}

object PatchDocs {

  implicit class XtensionPatch(p: Patch) {
    def output(implicit doc: SemanticDocument): String = {
      val (obtained, _) =
        Patch.semantic(Map(RuleName("patch") -> p), doc, suppress = false)
      obtained
    }
    def showDiff(context: Int = 0)(implicit doc: SemanticDocument): Unit = {
      printDiff(unifiedDiff(p.output, context))
    }
  }
  implicit class XtensionPatchs(p: Iterable[Patch]) {
    def showLints()(implicit doc: SemanticDocument): Unit = {
      val (_, diagnostics) = Patch.semantic(
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
    val trimmed = diff.lines.drop(3).mkString("\n")
    val message =
      if (trimmed.isEmpty) "<no diff>"
      else trimmed
    println(message)
  }

  def unifiedDiff(obtained: String, context: Int)(
      implicit doc: SemanticDocument): String = {
    val in = Input.VirtualFile("before patch", doc.input.text)
    val out = Input.VirtualFile("after  patch", obtained)
    Patch.unifiedDiff(in, out, context)
  }
  lazy val compiler = InteractiveSemanticdb.newCompiler(List("-Ywarn-unused"))
  lazy val symtab = GlobalSymbolTable(ClasspathOps.thisClasspath)
  lazy val scalafixSymtab = new Symtab { self =>
    override def info(symbol: Symbol): Option[SymbolInformation] = {
      symtab.info(symbol.value).map(new SymbolInformation(_)(self))
    }
  }
  import scalafix.v1._
  lazy val symbolInformationMethods: List[SymbolInformation] = {
    implicit val symtab = PatchDocs.scalafixSymtab
    val info = symtab.info(Symbol("scalafix/v1/SymbolInformation#")).get
    info.signature
      .asInstanceOf[ClassSignature]
      .declarations
      .filter(_.displayName.startsWith("is"))
  }
  def documentSymbolInfo(info: SymbolInformation): Unit = {
    println(s"- `${info.displayName}`")
  }
  def classToSymbol(cls: Class[_]): String = {
    if (cls.getEnclosingClass == null) {
      cls.getName + "."
    } else {
      Symbols.Global(
        classToSymbol(cls.getEnclosingClass),
        Descriptor.Term(cls.getSimpleName))
    }
  }
  def documentSymbolInfoCategory(category: Class[_]): Unit = {
    val normalized = Symbol(classToSymbol(category)).normalized
    symbolInformationMethods.foreach { info =>
      info.annotations.foreach { annotation =>
        annotation.tpe match {
          case TypeRef(_, sym, Nil) =>
            if (sym.normalized == normalized) {
              documentSymbolInfo(info)
            }
          case _ =>
        }
      }
    }
  }
  def fromString(code: String, debug: Boolean = false): SemanticDocument = {
    val filename = "Main.scala"
    println("```scala")
    println("// " + filename)
    println(code.trim)
    println("```")
    val textDocument = InteractiveSemanticdb.toTextDocument(compiler, code)
    if (debug) {
      println("```")
      println(Print.document(Format.Compact, textDocument))
      println("```")
    }
    val input = Input.VirtualFile(filename, code)
    val doc = SyntacticDocument.fromInput(input)
    val internal = new InternalSemanticDoc(
      doc,
      textDocument,
      symtab
    )
    new SemanticDocument(internal)
  }
}
