package scalafix.internal.util

import scala.meta.Tree
import scala.meta._
import org.typelevel.paiges._

object Inspect {

  def prettyList(tree: List[Tree], showFieldNames: Boolean): Doc = {
    wrapList(tree.map(t => prettyTree(t, showFieldNames)))
  }

  def prettyOption(tree: Option[Tree], showFieldNames: Boolean): Doc = {
    wrapOption(tree.map(t => prettyTree(t, showFieldNames)))
  }

  def prettyTree(tree: Tree, showFieldNames: Boolean): Doc = {
    tree match {
      case _ if tree.tokens.isEmpty =>
        Doc.empty
      case v: Term.Name =>
        Doc.text(v.structure)
      case t: Type.Name =>
        Doc.text(t.structure)
      case _ =>
        val args = tree.productFields.zip(tree.productIterator.toList).map {
          case (fieldName, value) =>
            val rhs = value match {
              case v: Term.Name => Doc.text(v.structure)
              case t: Tree => prettyTree(t, showFieldNames)
              case o: Option[_] =>
                o match {
                  case Some(t: Tree) =>
                    wrap(
                      Doc.text("Some") + Doc.char('('),
                      List(prettyTree(t, showFieldNames)),
                      Doc.char(')')
                    )
                  case None =>
                    Doc.text("None")
                  case _ =>
                    throw new Exception("cannot handle: " + o)
                }
              case vs: List[_] =>
                vs match {
                  case Nil => Doc.text("Nil")
                  case (_: Tree) :: _ =>
                    prettyList(vs.asInstanceOf[List[Tree]], showFieldNames)
                  case (_: List[_]) :: _ =>
                    val vsT = vs.asInstanceOf[List[List[Tree]]]
                    wrapList(vsT.map(v => prettyList(v, showFieldNames)))
                  case _ =>
                    throw new IllegalArgumentException("cannot handle: " + vs)
                }
              case _ => Doc.text(value.toString)
            }

            if (showFieldNames) Doc.text(fieldName) + Doc.text(" = ") + rhs
            else rhs
        }

        wrap(Doc.text(tree.productPrefix) + Doc.char('('), args, Doc.char(')'))
    }
  }

  private def wrapList(args: List[Doc]): Doc = {
    if (args.nonEmpty) {
      wrap(Doc.text("List") + Doc.char('('), args, Doc.char(')'))
    } else {
      Doc.text("Nil")
    }
  }

  private def wrapOption(opt: Option[Doc]): Doc = {
    opt match {
      case Some(doc) => Doc.text("Some") + Doc.char('(') + doc + Doc.char(')')
      case None => Doc.text("None")
    }
  }

  private def wrap(prefix: Doc, args: List[Doc], suffix: Doc): Doc = {
    val body = Doc.intercalate(Doc.char(',') + Doc.line, args)
    body.tightBracketBy(prefix, suffix)
  }
}
