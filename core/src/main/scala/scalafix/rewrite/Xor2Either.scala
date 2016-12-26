package scalafix.rewrite

import scala.collection.immutable.Seq
import scala.{meta => m}
import scalafix.util._
import scala.meta._

case object Xor2Either extends Rewrite {
  override def rewrite(ast: m.Tree, ctx: RewriteCtx): Seq[Patch] = {
    implicit val semanticApi: SemanticApi = getSemanticApi(ctx)

    //Create a sequence of type replacements
    val replacementTypes = List(
      ReplaceType(t"cats.data.XorT", t"cats.data.EitherT", "EitherT"),
      ReplaceType(t"cats.data.Xor", t"scala.util.Either", "Either"),
      ReplaceType(t"cats.data.Xor.Left", t"scala.util.Left", "Left"),
      ReplaceType(t"cats.data.Xor.Right", t"scala.util.Either.Right", "Right")
    )

    //Add in some method replacements
    val replacementTerms = List(
      ReplaceTerm(q"cats.data.Xor.Right.apply",
                  q"scala.util.Right.apply",
                  q"scala.util"),
      ReplaceTerm(q"cats.data.Xor.Left.apply",
                  q"scala.util.Left.apply",
                  q"scala.util")
    )

    //Then add needed imports.
    //todo - derive this from patches created, types
    //and terms replaced
    //Only add if they are not already imported
    val additionalImports = List(
      "cats.data.EitherT",
      "cats.implicits._",
      "scala.util.Either"
    )

    val typeReplacements =
      new ChangeType(ast).gatherPatches(replacementTypes)

    val termReplacements =
      new ChangeMethod(ast).gatherPatches(replacementTerms)

    //Make this additional imports, beyond what can be derived from the types
    val addedImports =
      if (typeReplacements.isEmpty && termReplacements.isEmpty) Seq[Patch]()
      else new AddImport(ast).gatherPatches(additionalImports)

    addedImports ++ typeReplacements ++ termReplacements
  }
}
