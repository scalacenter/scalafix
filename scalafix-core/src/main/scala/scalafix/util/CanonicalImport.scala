package scalafix
package util

import scala.collection.immutable.Seq
import scala.meta._, contrib._
import scala.meta.tokens.Token.Comment

object CanonicalImport {
  def fromWildcard(ref: Term.Ref,
                   wildcard: Importee.Wildcard,
                   extraImportees: Seq[Importee])(
      implicit ctx: RewriteCtx,
      ownerImport: Import
  ): CanonicalImport =
    new CanonicalImport(
      ref,
      wildcard,
      extraImportees,
      leadingComments = ctx.comments.leading(ownerImport),
      trailingComments = ctx.comments.trailing(ownerImport) ++
        extraImportees.flatMap(ctx.comments.trailing),
      None
    ) {}
  def fromImportee(ref: Term.Ref, importee: Importee)(
      implicit ctx: RewriteCtx,
      ownerImport: Import
  ): CanonicalImport =
    new CanonicalImport(
      ref,
      importee,
      Nil,
      leadingComments = ctx.comments.leading(ownerImport),
      trailingComments = ctx.comments.trailing(ownerImport) ++
        ctx.comments.trailing(importee),
      None
    ) {}
}

/** A canonical imports is the minimal representation for a single import statement
  *
  * Only construct this class from custom constructors in the companion object.
  * This class should be sealed abstract but the abstract modifier removes the
  * convenient copy method.
  */
sealed case class CanonicalImport(
    ref: Term.Ref,
    importee: Importee,
    extraImportees: Seq[Importee],
    leadingComments: Set[Comment],
    trailingComments: Set[Comment],
    fullyQualifiedRef: Option[Term.Ref]
)(implicit ctx: RewriteCtx)
    extends Ordered[CanonicalImport] {
  lazy val isRootImport: Boolean = ref.collectFirst {
    case q"_root_.$name" => name
  }.isDefined
  private def getRootName(ref: Term.Ref): Term.Name =
    ref.collectFirst {
      case q"_root_.$name" => name
      case q"${name: Term.Name}.$_" => name
    }.get
  lazy val rootName: Term.Name = getRootName(ref)
  private def addRootImport(rootImports: Seq[CanonicalImport])(
      fullyQualifiedRef: Term.Ref): Term.Ref = {
    val fqnRoot = getRootName(fullyQualifiedRef)
    def otherImportIsRoot =
      rootImports.exists(_.rootName.value == fqnRoot.value)
    if (isRootImport || otherImportIsRoot) {
      ("_root_." + fullyQualifiedRef.syntax)
        .parse[Term]
        .get
        .asInstanceOf[Term.Ref]
    } else fullyQualifiedRef
  }
  def withFullyQualifiedRef(
      fqnRef: Option[Term.Ref],
      rootImports: Seq[CanonicalImport]): CanonicalImport =
    copy(fullyQualifiedRef = fqnRef.map(addRootImport(rootImports)))
  def withoutLeading(leading: Set[Comment]): CanonicalImport =
    copy(leadingComments = leadingComments.filterNot(leading))
  def tree: Import = Import(Seq(Importer(ref, extraImportees :+ importee)))
  def syntax: String =
    s"${leading}import $importerSyntax$trailing"
  def leading: String =
    if (leadingComments.isEmpty) ""
    else leadingComments.mkString("", "\n", "\n")
  def trailing: String =
    if (trailingComments.isEmpty) ""
    else trailingComments.mkString(" ", "\n", "")
  def importerSyntax: String =
    s"$refSyntax.$importeeSyntax"
  private def curlySpace =
    if (ctx.config.imports.spaceAroundCurlyBrace) " "
    else ""
  def actualRef: Term.Ref =
    if (ctx.config.imports.expandRelative) fullyQualifiedRef.getOrElse(ref)
    else ref
  def refSyntax: String =
    actualRef.syntax
  def importeeSyntax: String =
    if (extraImportees.nonEmpty)
      s"""{$curlySpace${extraImportees
        .map(_.syntax)
        .mkString(", ")}, $importee$curlySpace}"""
    else
      importee match {
        case i: Importee.Rename => s"{$curlySpace$i$curlySpace}"
        case i => i.syntax
      }
  private def importeeOrder = importee match {
    case i: Importee.Rename => (1, i.name.syntax)
    case i: Importee.Wildcard => (0, i.syntax)
    case i => (1, i.syntax)
  }
  def sortOrder: (String, (Int, String)) = (refSyntax, importeeOrder)
  def structure: String = Importer(ref, Seq(importee)).structure

  override def compare(that: CanonicalImport): Int = {
    import Ordered.orderingToOrdered
    if (ref.syntax == that.ref.syntax) {
      importeeOrder.compare(that.importeeOrder)
    } else syntax.compareTo(that.syntax)
  }
}
