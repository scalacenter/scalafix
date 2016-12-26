package scalafix.util

import scala.collection.immutable.Seq
import scala.{meta => m}
import scalafix.rewrite._

//Provide a little structure to the replacements we will be performing
case class ReplaceType(original: m.Type,
                       replacement: m.Type,
                       newString: String) {
  def toPatch(t: m.Type): Patch = Patch.replace(t, newString)
}

class ChangeType(ast: m.Tree)(implicit sApi: SemanticApi) {
  import sApi._

  def partialTypeMatch(
      rt: ReplaceType): PartialFunction[m.Tree, (m.Type, ReplaceType)] = {
    case t @ DType(desugared)
        if StructurallyEqual(desugared, rt.original).isRight =>
      t -> rt
  }

  def partialTypeMatches(replacementTypes: Seq[ReplaceType])
    : PartialFunction[m.Tree, (m.Type, ReplaceType)] =
    replacementTypes.map(partialTypeMatch).reduce(_ orElse _)

  def tpes(ptm: PartialFunction[m.Tree, (m.Type, ReplaceType)])
    : Seq[(m.Type, ReplaceType)] = ast.collect { ptm }

  //This is unsafe, come up with something better
  def typeReplacements(tpes: Seq[(m.Type, ReplaceType)]): Seq[Patch] =
    tpes.map {
      case (tree, rt) => rt.toPatch(tree)
    }

  def gatherPatches(tr: Seq[ReplaceType]): Seq[Patch] =
    typeReplacements(tpes(partialTypeMatches(tr)))
}
