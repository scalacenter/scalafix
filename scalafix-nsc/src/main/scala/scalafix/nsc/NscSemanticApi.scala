package scalafix.nsc

import scala.collection.mutable
import scala.meta.Dialect
import scala.meta.Tree
import scala.meta.Type
import scala.meta.parsers.Parse
import scala.reflect.internal.util.SourceFile
import scala.{meta => m}
import scalafix.Fixed
import scalafix.Scalafix
import scalafix.ScalafixConfig
import scalafix.rewrite.SemanticApi
import scalafix.util.logger

case class SemanticContext(enclosingPackage: String, inScope: List[String])

trait NscSemanticApi extends ReflectToolkit {
  implicit class XtensionPosition(gpos: scala.reflect.internal.util.Position) {
    def matches(mpos: m.Position): Boolean = {
      gpos.isDefined &&
      gpos.start == mpos.start.offset &&
      gpos.end == mpos.end.offset
    }
  }

  /** Returns a map from byte offset to type name at that offset. */
  private def offsetToType(gtree: g.Tree,
                           dialect: Dialect): mutable.Map[Int, m.Type] = {
    // TODO(olafur) Come up with more principled approach, this is hacky as hell.
    // Operating on strings is definitely the wrong way to approach this
    // problem. Ideally, this implementation uses the tree/symbol api. However,
    // while I'm just trying to get something simple working I feel a bit more
    // productive with this hack.
    val builder = mutable.Map.empty[Int, m.Type]
    def add(gtree: g.Tree, ctx: SemanticContext): Unit = {

      /** Removes redudant Foo.this.ActualType prefix from a type */
      val stripRedundantThis: m.Type => m.Type = _.transform {
        case m.Term.Select(m.Term.This(m.Name.Indeterminate(_)), qual) =>
          qual
        case m.Type.Select(m.Term.This(m.Name.Indeterminate(_)), qual) =>
          qual
      }.asInstanceOf[m.Type]

      val stripImportedPrefix: m.Type => m.Type = _.transform {
        case prefix @ m.Type.Select(_, name)
            if ctx.inScope.contains(prefix.syntax) =>
          name
      }.asInstanceOf[m.Type]

      val stripEnclosingPackage: m.Type => m.Type = _.transform {
        case typ: m.Type.Ref =>
          import scala.meta._
          typ.syntax.stripPrefix(ctx.enclosingPackage).parse[m.Type].get
      }.asInstanceOf[m.Type]

      val cleanUp: (Type) => Type =
        stripRedundantThis andThen
          stripImportedPrefix andThen
          stripEnclosingPackage

      val parsed = dialect(gtree.toString()).parse[m.Type]
      parsed match {
        case m.Parsed.Success(ast) =>
          builder(gtree.pos.point) = cleanUp(ast)
        case _ =>
      }
    }

    def members(tpe: g.Type): Iterable[String] = tpe.members.collect {
      case x if !x.fullName.contains("$") =>
        x.fullName
    }

    def evaluate(ctx: SemanticContext, gtree: g.Tree): SemanticContext = {
      gtree match {
        case g.ValDef(_, _, tpt, _) if tpt.nonEmpty => add(tpt, ctx)
        case g.DefDef(_, _, _, _, tpt, _) => add(tpt, ctx)
        case _ =>
      }
      gtree match {
        case g.PackageDef(pid, _) =>
          val newCtx = ctx.copy(enclosingPackage = pid.symbol.fullName + ".",
                                inScope = ctx.inScope ++ members(pid.tpe))
          gtree.children.foldLeft(newCtx)(evaluate)
          ctx // leaving pkg scope
        case t: g.Template =>
          val newCtx =
            ctx.copy(inScope = t.symbol.owner.fullName :: ctx.inScope)
          gtree.children.foldLeft(newCtx)(evaluate)
          ctx
        case g.Import(expr, selectors) =>
          val newNames: Seq[String] = selectors.collect {
            case g.ImportSelector(from, _, to, _) if from == to =>
              Seq(s"${expr.symbol.fullName}.$from")
            case g.ImportSelector(_, _, null, _) =>
              members(expr.tpe)
          }.flatten
          ctx.copy(inScope = ctx.inScope ++ newNames)
        case _ =>
          gtree.children.foldLeft(ctx)(evaluate)
      }
    }
    evaluate(SemanticContext("", Nil), gtree)
    builder
  }

  private def collect[T](gtree: g.Tree)(
      pf: PartialFunction[g.Tree, T]): Seq[T] = {
    val builder = Seq.newBuilder[T]
    val f = pf.lift
    def iter(gtree: g.Tree): Unit = {
      f(gtree).foreach(builder += _)
      gtree match {
        case t @ g.TypeTree() if t.original != null && t.original.nonEmpty =>
          iter(t.original)
        case _ =>
          gtree.children.foreach(iter)
      }
    }
    iter(gtree)
    builder.result()
  }

  private def getSemanticApi(unit: g.CompilationUnit,
                             config: ScalafixConfig): SemanticApi = {
    val offsets = offsetToType(unit.body, config.dialect)
    new SemanticApi {
      override def typeSignature(defn: m.Defn): Option[m.Type] = {
        defn match {
          case m.Defn.Val(_, Seq(pat), _, _) =>
            offsets.get(pat.pos.start.offset)
          case m.Defn.Def(_, name, _, _, _, _) =>
            offsets.get(name.pos.start.offset)
          case _ =>
            None
        }
      }

      override def desugared[T <: Tree](tree: T)(
          implicit parse: Parse[T]): Option[T] = {
        val result = collect[Option[T]](unit.body) {
//          case t if { logger.elem(t.toString(), g.showRaw(t)); false } => None
          case t if t.pos.matches(tree.pos) =>
            import scala.meta._
            parse(m.Input.String(t.toString()), config.dialect) match {
              case m.parsers.Parsed.Success(x) => Some(x)
              case _ => None
            }
        }.flatten
//        logger.elem(result)
        result.headOption
      }
    }
  }

  private def getMetaInput(source: SourceFile): m.Input = {
    if (source.file.file != null && source.file.file.isFile)
      m.Input.File(source.file.file)
    else m.Input.String(new String(source.content))
  }

  def fix(unit: g.CompilationUnit, config: ScalafixConfig): Fixed = {
    val api = getSemanticApi(unit, config)
    val input = getMetaInput(unit.source)
    Scalafix.fix(input, config, Some(api))
  }
}
