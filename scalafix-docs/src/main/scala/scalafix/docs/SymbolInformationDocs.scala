package scalafix.docs

import scala.meta.internal.semanticdb.Scala.Descriptor
import scala.meta.internal.semanticdb.Scala.Symbols

object SymbolInformationDocs {
  import scalafix.v1._
  lazy val symbolInformationMethods: List[SymbolInformation] = {
    val info =
      PatchDocs.scalafixSymtab
        .info(Symbol("scalafix/v1/SymbolInformation#"))
        .get
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

}
