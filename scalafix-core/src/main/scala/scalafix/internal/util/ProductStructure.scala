package scalafix.internal.util

import org.typelevel.paiges._
import scala.meta.Tree
import scala.meta._
import scalafix.v1.Symbol
import scalafix.v1.SymbolInfo
import DocConstants._

object ProductLabeledStructure
    extends ProductStructure(
      showFieldNames = true,
      isUnhelpfulFieldName = Set("value", "")
    )
object ProductStructure
    extends ProductStructure(
      showFieldNames = false,
      isUnhelpfulFieldName = Set()
    )
class ProductStructure(
    showFieldNames: Boolean,
    isUnhelpfulFieldName: Set[String]
) {

  def structure(product: Product): Doc =
    prettyAny(product)

  private def prettyAny(any: Any): Doc = any match {
    case iterable: Iterable[_] =>
      val prefix = iterable match {
        case _: List[_] => "List"
        case _: Map[_, _] => "Map"
        case _: Set[_] => "Set"
        case product: Product => product.productPrefix
        case _ => ""
      }
      if (iterable.isEmpty) {
        Doc.text(prefix) + `(` +
          Doc.intercalate(Doc.comma + Doc.space, iterable.map(prettyAny)) +
          `)`
      } else {
        Doc
          .intercalate(Doc.comma + Doc.line, iterable.map(prettyAny))
          .tightBracketBy(Doc.text(prefix) + `(`, `)`)
      }
    case value: String =>
      Doc.text(Lit.String(value).syntax)
    case value: Product =>
      prettyProduct(value)
    case value: Symbol =>
      prettySymbol(value)
    case value: SymbolInfo =>
      prettySymbolInfo(value)
    case value: Int =>
      Doc.text(Lit.Int(value).syntax)
    case value: Long =>
      Doc.text(Lit.Long(value).syntax)
    case value: Double =>
      Doc.text(Lit.Double(value).syntax)
    case value: Float =>
      Doc.text(Lit.Float(value).syntax)
    case _ =>
      Doc.str(any)
  }

  private def prettyFields(product: Product): List[String] = {
    product match {
      case value: Tree =>
        value.productFields
      case _ =>
        List.fill(product.productArity)("")
    }
  }

  def prettyProduct(product: Product): Doc = {
    val prefix = Doc.text(product.productPrefix)
    val values = product.productIterator.map(prettyAny).toList
    if (product.productArity == 0) {
      prefix
    } else if (product.productArity == 1) {
      prefix + `(` + values.head + `)`
    } else {
      val fieldNames = prettyFields(product)
      val allNamesAreUnhelpful = fieldNames.forall(isUnhelpfulFieldName)
      val args =
        if (showFieldNames && !allNamesAreUnhelpful) {
          val table = fieldNames.zip(values).map {
            case (fieldName, fieldValue) =>
              Doc.text(fieldName) + Doc.text(" = ") + fieldValue
          }
          Doc.intercalate(Doc.comma + Doc.line, table)
        } else {
          Doc.intercalate(Doc.comma + Doc.line, values)
        }
      args.tightBracketBy(prefix + `(`, `)`)
    }
  }

  private def prettySymbolInfo(info: SymbolInfo): Doc = {
    Doc.text(s"SymbolInfo(${info.toString})")
  }

  private def prettySymbol(symbol: Symbol): Doc = {
    Doc.text(s"""Symbol("${symbol.value}")""")
  }

}
