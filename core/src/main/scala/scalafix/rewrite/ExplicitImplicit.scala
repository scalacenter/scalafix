package scalafix.rewrite

import scala.meta._
import scala.{meta => m}
import scalafix.util.Patch
import scalafix.util.Whitespace
import scalafix.util.logger

case object ExplicitImplicit extends Rewrite {
  // Don't explicitly annotate vals when the right-hand body is a single call
  // to `implicitly`. Prevents ambiguous implicit. Not annotating in such cases,
  // this a common trick employed implicit-heavy code to workaround SI-2712.
  // Context: https://gitter.im/typelevel/cats?at=584573151eb3d648695b4a50
  private def isImplicitly(term: m.Term): Boolean = term match {
    case m.Term.ApplyType(m.Term.Name("implicitly"), _) => true
    case _ => false
  }
  @scala.annotation.tailrec
  final def parents(tree: Tree,
                    accum: Seq[Tree] = Seq.empty[Tree]): Seq[Tree] = {
    tree.parent match {
      case Some(parent) => parents(parent, parent +: accum)
      case _ => accum
    }
  }
  /** Get the last sequential top level package from a source file. */
  def getLastTopLevelPkg(potentialPkg: Stat): Stat = {
    potentialPkg match {
      case p: Pkg =>
        val stats = p.stats
        val first = stats.head
        if (stats.size != 1) first
        else getLastTopLevelPkg(first)
      case _ => potentialPkg
    }
  }
  override def rewrite(ast: m.Tree, ctx: RewriteCtx): Seq[Patch] = {
    import scala.meta._
    val semantic = getSemanticApi(ctx)
    def fix(defn: Defn, body: Term): Seq[Patch] = {
      import ctx.tokenList._
      for {
        start <- defn.tokens.headOption
        end <- body.tokens.headOption
        // Left-hand side tokens in definition.
        // Example: `val x = ` from `val x = rhs.banana`
        lhsTokens = slice(start, end)
        replace <- lhsTokens.reverseIterator.find(x =>
          !x.is[Token.Equals] && !x.is[Whitespace])
        typ <- semantic.typeSignature(defn)
        importToken = parents(defn)
          .collectFirst {
            case p: Pkg =>
              getLastTopLevelPkg(p).tokens.head
          }
          .getOrElse(ast.tokens.head)
      } yield {
        val (shortenedTpe, missingImports) = semantic.shortenType(typ, defn)
        Patch(replace, replace, s"$replace: ${shortenedTpe.syntax}") +:
          missingImports.map(missingImport =>
            Patch.AddLeft(importToken, s"import ${missingImport}\n"))
      }
    }.toSeq.flatten
    ast.collect {
      case t @ m.Defn.Val(mods, _, None, body)
          if mods.exists(_.syntax == "implicit") &&
            !isImplicitly(body) =>
        fix(t, body)
      case t @ m.Defn.Def(mods, _, _, _, None, body)
          if mods.exists(_.syntax == "implicit") =>
        fix(t, body)
    }.flatten
  }
}
