package scalafix.nsc

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.meta.Dialect
import scala.meta.Ref
import scala.meta.Source
import scala.meta.Type
import scala.meta.semantic.v1._
import scala.reflect.internal.util.SourceFile
import scala.tools.nsc.Settings
import scala.util.Try
import scala.{meta => m}
import scalafix.Fixed
import scalafix.Scalafix
import scalafix.config.ScalafixConfig
import scalafix.rewrite.ScalafixMirror
import scalafix.util.logger
case class SemanticContext(enclosingPackage: String, inScope: List[String])

trait NscScalafixMirror extends ReflectToolkit with HijackImportInfos {
  private implicit class XtensionGTree(tree: g.Tree) {
    def structure: String = g.showRaw(tree)
  }
  private implicit class XtensionMInput(input: m.Input) {
    def matches(other: m.Input): Boolean = {
      (input, other) match {
        case (m.inputs.Input.File(f, _), m.inputs.Input.File(f2, _)) =>
          f.getAbsolutePath == f2.getAbsolutePath
        case _ => false
      }
    }
  }
  private implicit class XtensionPosition(
      gpos: scala.reflect.internal.util.Position) {
    def matches(mpos: m.Position): Boolean =
      gpos.isDefined &&
        gpos.start == mpos.start.offset &&
        gpos.end == mpos.end.offset
    def inside(mpos: m.Position): Boolean =
      gpos.isDefined &&
        gpos.start <= mpos.start.offset &&
        gpos.end >= mpos.end.offset
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

      // Removes redudant Foo.this.ActualType prefix from a type
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
        case m.parsers.Parsed.Success(ast) =>
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

  private def find(body: g.Tree, pos: m.Position): Option[g.Tree] = {
    var result = Option.empty[g.Tree]
    new g.Traverser {
      override def traverse(tree: g.Tree): Unit =
        if (tree.pos.matches(pos)) result = Some(tree)
        else if (tree.pos.inside(pos)) super.traverse(tree)
    }.traverse(body)
    result
  }

  private def fullNameToRef(fullName: String): Option[m.Ref] = {
    import scala.meta._
    Try(
      fullName.parse[Term].get
    ).toOption.collect { case t: m.Ref => t }
  }
  private def symbolToRef(sym: g.Symbol): Option[m.Ref] =
    Option(sym).flatMap(sym => fullNameToRef(sym.fullName))

  private def assertSettingsAreValid(config: ScalafixConfig): Unit = {
    val requiredSettings: Seq[(Settings#BooleanSetting, Boolean)] =
      (g.settings.Yrangepos, true) :: {
        if (config.imports.removeUnused)
          g.settings.warnUnusedImport -> true :: Nil
        else Nil
      }
    val missingSettings = requiredSettings.filterNot {
      case (setting, value) => setting.value == value
    }
    if (missingSettings.nonEmpty) {
      val (toEnable, toDisable) = missingSettings.partition(_._2)
      def mkString(key: String,
                   settings: Seq[(Settings#BooleanSetting, Boolean)]) =
        if (settings.isEmpty) ""
        else s"\n$key: ${settings.map(_._1.name).mkString(", ")}"
      val instructions =
        s"Please re-compile with the scalac options:" +
          mkString("Enabled", toEnable) +
          mkString("Disabled", toDisable)
      val explanation =
        "This is necessary for scalafix semantic rewrites to function"
      sys.error(s"$instructions. $explanation")
    }
  }

  private def getUnusedImports(
      unit: g.CompilationUnit
  ): List[g.ImportSelector] = {
    def isMask(s: g.ImportSelector) =
      s.name != g.termNames.WILDCARD && s.rename == g.termNames.WILDCARD
    for {
      imps <- allImportInfos.customRemove(unit).toList
      imp <- imps.reverse.distinct
      used = allUsedSelectors(imp)
      s <- imp.tree.selectors
      if !isMask(s) && !used(s)
      _ = imps.foreach(allUsedSelectors.customRemove)
    } yield s
  }

  private def getSemanticApi(unit: g.CompilationUnit, config: ScalafixConfig)(
      implicit mirror: Mirror): ScalafixMirror = {
    assertSettingsAreValid(config)
    val offsets = offsetToType(unit.body, config.dialect)
    val unused = getUnusedImports(unit)
    new ScalafixMirror {
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
      override def fqn(name: m.Ref): Option[m.Ref] =
        find(unit.body, name.pos)
          .map(_.toString())
          .flatMap(fullNameToRef)

      def isUnusedImport(importee: m.Importee): Boolean =
        unused.exists(_.namePos == importee.pos.start.offset)
      // TODO(olafur) more elegant way to combine two interfaces
      override def dialect: Dialect = mirror.dialect
      override def sources: Seq[Source] = mirror.sources
      override def database: Database = mirror.database
      override def symbol(ref: Ref): Completed[Symbol] = mirror.symbol(ref)
    }
  }

  private def getMetaInput(source: SourceFile): m.Input = {
    if (source.file.file != null && source.file.file.isFile)
      m.Input.File(source.file.file)
    else m.Input.String(new String(source.content))
  }

  def fix(unit: g.CompilationUnit, config: ScalafixConfig)(
      implicit mirror: Mirror): Fixed = {
    val api = getSemanticApi(unit, config)
    val input = getMetaInput(unit.source)
    mirror.sources.find(_.pos.input.matches(input)) match {
      case Some(source) =>
        Scalafix.fix(source, config, Some(api))
      case None =>
        Scalafix.fix(input, config, Some(api))
    }
  }
}
