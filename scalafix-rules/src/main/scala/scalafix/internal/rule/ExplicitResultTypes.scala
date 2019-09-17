package scalafix.internal.rule

import scala.meta._
import scala.meta.contrib._
import scala.meta.internal.scalafix.ScalafixScalametaHacks
import scala.meta.internal.proxy.GlobalProxy
import scalafix.patch.Patch
import scalafix.v1._
import scalafix.util.TokenOps
import metaconfig.Configured
import scalafix.internal.util.PrettyResult
import scalafix.internal.util.QualifyStrategy
import scalafix.internal.util.PrettyType
import scalafix.v1.MissingSymbolException
import scala.meta.internal.pc.MetalsGlobal
import scala.meta.internal.pc.ScalaPresentationCompiler
import scala.collection.mutable

final class ExplicitResultTypes(
    config: ExplicitResultTypesConfig,
    global: Option[MetalsGlobal]
) extends SemanticRule("ExplicitResultTypes") {

  def this() = this(ExplicitResultTypesConfig.default, None)

  override def description: String =
    "Inserts explicit annotations for inferred types of def/val/var"
  override def isRewrite: Boolean = true
  override def isExperimental: Boolean = true

  override def afterComplete(): Unit = {
    global.foreach(_.askShutdown())
  }

  override def withConfiguration(config: Configuration): Configured[Rule] = {
    val newGlobal =
      if (config.scalacClasspath.isEmpty) None
      else {
        Some(
          ScalaPresentationCompiler(
            classpath = config.scalacClasspath.map(_.toNIO)
          ).newCompiler()
        )
      }
    config.conf // Support deprecated explicitReturnTypes config
      .getOrElse("explicitReturnTypes", "ExplicitResultTypes")(
        ExplicitResultTypesConfig.default
      )
      .map(c => new ExplicitResultTypes(c, newGlobal))
  }

  // Don't explicitly annotate vals when the right-hand body is a single call
  // to `implicitly`. Prevents ambiguous implicit. Not annotating in such cases,
  // this a common trick employed implicit-heavy code to workaround SI-2712.
  // Context: https://gitter.im/typelevel/cats?at=584573151eb3d648695b4a50
  private def isImplicitly(term: Term): Boolean = term match {
    case Term.ApplyType(Term.Name("implicitly"), _) => true
    case _ => false
  }

  def defnName(defn: Defn): Option[Name] = Option(defn).collect {
    case Defn.Val(_, Pat.Var(name) :: Nil, _, _) => name
    case Defn.Var(_, Pat.Var(name) :: Nil, _, _) => name
    case Defn.Def(_, name, _, _, _, _) => name
  }

  def visibility(mods: Traversable[Mod]): MemberVisibility =
    mods
      .collectFirst {
        case _: Mod.Private => MemberVisibility.Private
        case _: Mod.Protected => MemberVisibility.Protected
      }
      .getOrElse(MemberVisibility.Public)

  def kind(defn: Defn): Option[MemberKind] = Option(defn).collect {
    case _: Defn.Val => MemberKind.Val
    case _: Defn.Def => MemberKind.Def
    case _: Defn.Var => MemberKind.Var
  }
  import scala.meta.internal.{semanticdb => s}
  def unsafeToType(
      ctx: SemanticDocument,
      pos: Position,
      symbol: Symbol
  ): PrettyResult[Type] = {
    val info = ctx.internal.symtab
      .info(symbol.value)
      .getOrElse(throw new NoSuchElementException(symbol.value))
    val tpe = info.signature match {
      case method: s.MethodSignature =>
        method.returnType
      case value: s.ValueSignature =>
        value.tpe
      case els =>
        throw new IllegalArgumentException(s"Unsupported signature $els")
    }
    PrettyType.toType(
      tpe,
      ctx.internal.symtab,
      if (config.unsafeShortenNames) QualifyStrategy.Readable
      else QualifyStrategy.Full,
      fatalErrors = config.fatalWarnings
    )
  }

  def toType(
      pos: Position,
      symbol: Symbol
  )(implicit ctx: SemanticDocument): Option[PrettyResult[Type]] = {
    try {
      Some(unsafeToType(ctx, pos, symbol))
    } catch {
      case e: MissingSymbolException =>
        if (config.fatalWarnings) {
          ctx.internal.config.reporter.error(e.getMessage, pos)
        } else {
          // Silently discard failures from producing a new type.
          // Errors are most likely caused by known upstream issue that have been reported in Scalameta.
        }
        None
    }
  }

  override def fix(implicit ctx: SemanticDocument): Patch = {
    lazy val unit =
      global.map(g => g.newCompilationUnit(ctx.input.text, ctx.input.syntax))
    def toCompilerType(
        pos: Position,
        sym: Symbol,
        replace: Token,
        space: String
    ): Option[Patch] = {
      global match {
        case None => None
        case Some(g) =>
          val gpos = unit.get.position(pos.start)
          GlobalProxy.typedTreeAt(g, gpos)
          val gsym = g.inverseSemanticdbSymbol(sym.value)
          if (gsym == g.NoSymbol) None
          else {
            val context = g.doLocateContext(gpos)
            val history = new g.ShortenedNames(
              lookupSymbol = name => {
                val other =
                  if (name.isTermName) name.toTypeName else name.toTermName
                context.lookupSymbol(name, _ => true) ::
                  context.lookupSymbol(other, _ => true) ::
                  Nil
              },
              config = g.renamedSymbols(context)
            )
            val long = gsym.info.toString()
            def loop(tpe: g.Type): g.Type = {
              import g._
              tpe match {
                case t: g.PolyType => loop(t.resultType)
                case t: g.MethodType => loop(t.resultType)
                case g.RefinedType(parents, _) =>
                  g.RefinedType(parents, g.EmptyScope)
                case g.NullaryMethodType(tpe) =>
                  g.NullaryMethodType(loop(tpe))
                case TypeRef(pre, sym, args) =>
                  TypeRef(loop(pre), sym, args.map(loop))
                case tpe => tpe
              }
            }
            val shortT = g.shortType(loop(gsym.info).widen, history)
            val short = shortT.toString()
            val toImport = mutable.Map.empty[g.Symbol, List[g.ShortName]]
            val isRootSymbol = Set[g.Symbol](
              g.rootMirror.RootClass,
              g.rootMirror.RootPackage
            )
            for {
              (name, sym) <- history.history.iterator
              owner = sym.owner
              if !isRootSymbol(owner)
              if !context.lookupSymbol(name, _ => true).isSuccess
            } {
              toImport(owner) = sym :: toImport.getOrElse(owner, Nil)
            }
            val addImports = for {
              (pkg, names) <- toImport
              name <- names
              ref = pkg.owner
            } yield {
              val head :: tail = pkg.ownerChain.reverse.tail // Skip root symbol
                .map(sym => Term.Name(sym.name.toString()))
              val ref = tail.foldLeft(head: Term.Ref) {
                case (owner, name) =>
                  Term.Select(owner, name)
              }
              Patch.addGlobalImport(
                Importer(
                  ref,
                  List(Importee.Name(Name.Indeterminate(name.name.toString())))
                )
              )
            }
            Some(Patch.addRight(replace, s"$space: $short") ++ addImports)
          }
      }
    }
    def toMetaType(
        pos: Position,
        sym: Symbol,
        replace: Token,
        space: String
    ): Option[Patch] = {
      for {
        result <- toType(pos, sym)
      } yield {
        val addGlobalImports = result.imports.map { s =>
          val symbol = Symbol(s)
          Patch.addGlobalImport(symbol)
        }
        Patch.addRight(replace, s"$space: ${treeSyntax(result.tree)}") +
          addGlobalImports.asPatch
      }
    }
    def defnType(defn: Defn, replace: Token, space: String): Option[Patch] =
      for {
        name <- defnName(defn)
        defnSymbol <- name.symbol.asNonEmpty
        patch <- toCompilerType(name.pos, defnSymbol, replace, space)
      } yield patch
    import scala.meta._

    def fix(defn: Defn, body: Term): Patch = {
      val lst = ctx.tokenList
      import lst._
      for {
        start <- defn.tokens.headOption
        end <- body.tokens.headOption
        // Left-hand side tokens in definition.
        // Example: `val x = ` from `val x = rhs.banana`
        lhsTokens = slice(start, end)
        replace <- lhsTokens.reverseIterator.find(
          x => !x.is[Token.Equals] && !x.is[Trivia]
        )
        space = {
          if (TokenOps.needsLeadingSpaceBeforeColon(replace)) " "
          else ""
        }
        patch <- defnType(defn, replace, space)
      } yield patch
    }.asPatch.atomic

    def treeSyntax(tree: Tree): String =
      ScalafixScalametaHacks.resetOrigin(tree).syntax

    def isRuleCandidate[D <: Defn](
        defn: D,
        nm: Name,
        mods: Traversable[Mod],
        body: Term
    )(implicit ev: Extract[D, Mod]): Boolean = {
      import config._

      def matchesMemberVisibility(): Boolean =
        memberVisibility.contains(visibility(mods))

      def matchesMemberKind(): Boolean =
        kind(defn).exists(memberKind.contains)

      def isFinalLiteralVal: Boolean =
        defn.is[Defn.Val] &&
          mods.exists(_.is[Mod.Final]) &&
          body.is[Lit]

      def matchesSimpleDefinition(): Boolean =
        body.is[Lit] && skipSimpleDefinitions

      def isImplicit: Boolean =
        defn.hasMod(mod"implicit") && !isImplicitly(body)

      def hasParentWihTemplate: Boolean =
        defn.parent.exists(_.is[Template])

      def isLocal: Boolean =
        if (config.skipLocalImplicits) nm.symbol.isLocal
        else false

      isImplicit && !isFinalLiteralVal && !isLocal || {
        hasParentWihTemplate &&
        !defn.hasMod(mod"implicit") &&
        !matchesSimpleDefinition() &&
        matchesMemberKind() &&
        matchesMemberVisibility()
      }
    }

    val result = ctx.tree.collect {
      case t @ Defn.Val(mods, Pat.Var(name) :: Nil, None, body)
          if isRuleCandidate(t, name, mods, body) =>
        fix(t, body)

      case t @ Defn.Var(mods, Pat.Var(name) :: Nil, None, Some(body))
          if isRuleCandidate(t, name, mods, body) =>
        fix(t, body)

      case t @ Defn.Def(mods, name, _, _, None, body)
          if isRuleCandidate(t, name, mods, body) =>
        fix(t, body)
    }.asPatch
    result
  }
}
