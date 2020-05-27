package scalafix.internal.rule

import scala.meta.internal.pc.ScalafixGlobal
import scalafix.v1
import scala.{meta => m}
import scala.meta.internal.proxy.GlobalProxy
import scala.collection.mutable
import scala.reflect.internal.{Flags => gf}
import scala.meta.internal.pc.Identifier
import scala.collection.compat.immutable.LazyList

class CompilerTypePrinter(g: ScalafixGlobal, config: ExplicitResultTypesConfig)(
    implicit ctx: v1.SemanticDocument
) extends TypePrinter {
  import g._
  private lazy val unit =
    g.newCompilationUnit(ctx.input.text, ctx.input.syntax)
  private val willBeImported = mutable.Map.empty[Name, ShortName]
  private val isInsertedClass = mutable.Set.empty[String]

  override def toPatch(
      pos: m.Position,
      sym: v1.Symbol,
      replace: m.Token,
      defn: m.Defn,
      space: String
  ): Option[v1.Patch] = {
    try toPatchUnsafe(pos, sym, replace, defn, space)
    catch {
      case _: NotImplementedError =>
        None
      case e: Throwable =>
        throw CompilerException(e)
    }
  }

  def toPatchUnsafe(
      pos: m.Position,
      sym: v1.Symbol,
      replace: m.Token,
      defn: m.Defn,
      space: String
  ): Option[v1.Patch] = {
    val gpos = unit.position(pos.start)
    GlobalProxy.typedTreeAt(g, gpos)
    val inverseSemanticdbSymbol = g
      .inverseSemanticdbSymbols(sym.value)
      .find(s => g.semanticdbSymbol(s) == sym.value)
      .getOrElse(g.NoSymbol)
    val hasNothing = inverseSemanticdbSymbol.info.exists {
      case g.definitions.NothingTpe => true
      case _ => false
    }
    val gsym =
      if (hasNothing) {
        inverseSemanticdbSymbol.overrides.lastOption
          .getOrElse(inverseSemanticdbSymbol)
      } else {
        inverseSemanticdbSymbol
      }
    if (gsym == g.NoSymbol) {
      None
    } else if (gsym.info == null || gsym.info.isErroneous) {
      None
    } else {
      val context = g.doLocateContext(gpos)
      printType(replace, defn, space, context, gsym, inverseSemanticdbSymbol)
    }
  }

  private def printType(
      replace: m.Token,
      defn: m.Defn,
      space: String,
      context: Context,
      gsym: Symbol,
      inverseSemanticdbSymbol: Symbol
  ): Option[v1.Patch] = {
    val renames = g.renamedSymbols(context).filterNot {
      case (sym, name) =>
        sym == g.NoSymbol ||
          name.toString() == "_"
    }
    val history = new g.ShortenedNames(
      lookupSymbol = name => {
        context.lookupSymbol(name, _ => true) :: Nil
      },
      renames = renames,
      owners = parentSymbols(context),
      config = g.renameConfig ++ renames
    )
    val fromRewritten = willBeImported.filter {
      case (name, short) =>
        (!context.isNameInScope(name) &&
          !context.isNameInScope(name.otherName)) ||
          history.nameResolvesToSymbol(name, short.symbol)
    }
    history.missingImports ++= fromRewritten
    val seenFromType = asSeenFromType(gsym, inverseSemanticdbSymbol)
    var extraPatch = v1.Patch.empty
    val namedSubclassRewrite =
      rewriteRefinedTypeToNamedSubclass(defn, gsym, context, seenFromType)
    val toLoop = namedSubclassRewrite match {
      case None => seenFromType
      case Some((tpe, patch)) =>
        extraPatch += patch
        tpe
    }
    val preProcessed = preProcess(toLoop, inverseSemanticdbSymbol).widen
    val shortType = g.shortType(preProcessed, history)
    val short = shortType.toString()
    willBeImported ++= history.missingImports
    val addImports = importPatches(history, context)
    val isConstantType = toLoop.finalResultType match {
      case ConstantType(Constant(value)) => !value.isInstanceOf[Symbol]
      case _ => false
    }
    if (isConstantType) None
    else if (seenFromType.isErroneous) None
    else {
      Some(
        v1.Patch.addRight(replace, s"$space: $short") ++
          addImports + extraPatch
      )
    }

  }

  private def asSeenFromType(
      gsym: Symbol,
      inverseSemanticdbSymbol: Symbol
  ): Type = {
    if (gsym == inverseSemanticdbSymbol) gsym.info
    else {
      val from = gsym.typeParams
      val to = inverseSemanticdbSymbol.typeParams
      val substituteSymbols = new SubstSymMap(from, to) {
        val map = from.map(semanticdbSymbol).zip(to).toMap
        override def apply(tp: g.Type): g.Type = {
          tp match {
            // NOTE(olafur): I was unable to obtain the correct reference to
            // the type parameter symbols to get the default `SubstSymMap`
            // working. Using SemanticDB equality makes the substitution
            // work as expected.
            case TypeRef(pre, sym, args)
                if map.contains(semanticdbSymbol(sym)) =>
              super.apply(TypeRef(pre, map(semanticdbSymbol(sym)), args))
            case _ =>
              super.apply(tp)
          }
        }
      }
      substituteSymbols(gsym.info).asSeenFrom(
        ThisType(inverseSemanticdbSymbol.owner),
        gsym.owner
      )
    }
  }

  private def importPatches(
      history: ShortenedNames,
      context: Context
  ): Iterable[v1.Patch] = {
    val toImport = mutable.Map.empty[g.Symbol, List[g.ShortName]]
    for {
      (name, sym) <- history.missingImports.iterator
      owner = sym.owner
      if !isRootSymbol(owner)
      if !context.lookupSymbol(name, _ => true).isSuccess
    } {
      toImport(owner) = sym :: toImport.getOrElse(owner, Nil)
    }
    for {
      (pkg, names) <- toImport
      name <- names
      ref = pkg.owner
    } yield {
      val importee: m.Importee = {
        val ident = m.Name.Indeterminate(name.name.toString())
        if (name.isRename)
          m.Importee.Rename(
            m.Name.Indeterminate(Identifier(name.symbol.name)),
            ident
          )
        else m.Importee.Name(ident)
      }
      val ownerChain = pkg.ownerChain
      if (ownerChain.isEmpty) {
        v1.Patch.empty
      } else {
        val head :: tail = pkg.ownerChain.reverse.tail // Skip root symbol
          .map(sym => m.Term.Name(sym.name.toString()))
        val ref = tail.foldLeft(head: m.Term.Ref) {
          case (owner, name) =>
            if (name.value == "package") owner
            else m.Term.Select(owner, name)
        }
        v1.Patch.addGlobalImport(m.Importer(ref, List(importee)))
      }
    }

  }

  private def parentSymbols(context: Context): collection.Set[Symbol] = {
    val isVisited = mutable.Set.empty[Symbol]
    var cx = context
    def expandParent(parent: Symbol): Unit = {
      if (!isVisited(parent)) {
        isVisited.add(parent)
        parent.parentSymbols.foreach { parent =>
          expandParent(parent)
        }
      }
    }

    while (cx != NoContext && !cx.owner.hasPackageFlag) {
      expandParent(cx.owner)
      cx = cx.outer
    }
    isVisited
  }

  private def isPossibleSyntheticParent(tpe: Type): Boolean = {
    definitions.isPossibleSyntheticParent(tpe.typeSymbol) ||
    definitions.AnyRefTpe == tpe ||
    definitions.ObjectTpe == tpe
  }

  private def rewriteRefinedTypeToNamedSubclass(
      defn: m.Defn,
      gsym: Symbol,
      context: Context,
      tpe: Type
  ): Option[(Type, v1.Patch)] = tpe.finalResultType match {
    case resultType @ RefinedType(parents, decls)
        if config.rewriteStructuralTypesToNamedSubclass &&
          decls.filterNot(_.isOverridingSymbol).nonEmpty =>
      val body: Option[m.Term] = defn match {
        case t: m.Defn.Val => Some(t.rhs)
        case t: m.Defn.Var => t.rhs
        case t: m.Defn.Def => Some(t.body)
        case _ => None
      }
      body match {
        case Some(body @ m.Term.NewAnonymous(template))
            if body.tokens.head.syntax == "new" =>
          val nameSyntax = gsym.nameSyntax
          val suffixes = "" +: LazyList.from(1).map(_.toString())
          val name = suffixes
            .map(suffix => nameSyntax + suffix)
            .find { name =>
              !context.isNameInScope(TypeName(name)) &&
              !isInsertedClass(name)
            }
            .get
          isInsertedClass += name
          val paramDefnSuffix = defn match {
            case d: m.Defn.Def =>
              d.paramss
                .map(_.map(_.syntax).mkString(", "))
                .mkString("(", ")(", ")")
            case _ => ""
          }
          val tparamDefn = defn match {
            case d: m.Defn.Def if d.tparams.nonEmpty =>
              d.tparams.map(_.syntax).mkString("[", ", ", "]")
            case _ => ""
          }
          val tparamCall = defn match {
            case d: m.Defn.Def if d.tparams.nonEmpty =>
              d.tparams.map(_.name.syntax).mkString("[", ", ", "]")
            case _ => ""
          }
          val paramCallSuffix = defn match {
            case d: m.Defn.Def =>
              d.paramss
                .map(_.map(_.name.syntax).mkString(", "))
                .mkString("(", ")(", ")")
            case _ => ""
          }
          val indent = " " * defn.pos.startColumn
          Some(
            (
              new PrettyType(name + tparamCall),
              v1.Patch.addRight(
                body.tokens.head,
                s" ${name}${tparamCall}${paramCallSuffix}\n${indent}class ${name}${tparamDefn}${paramDefnSuffix} extends"
              )
            )
          )
        case _ =>
          None
      }
    case _ =>
      None
  }

  // Clean up the type before passing it to `g.shortType()`.
  private def preProcess(tpe: Type, gsym: Symbol): Type = {
    def loop(tpe: Type): Type = {
      tpe match {
        case TypeRef(pre, sym, args)
            if gsymbolReplacements.contains(semanticdbSymbol(sym)) =>
          loop(TypeRef(pre, gsymbolReplacements(semanticdbSymbol(sym)), args))
        case tp @ ThisType(sym)
            if tp.toString() == s"${gsym.owner.nameString}.this.type" =>
          new PrettyType("this.type")
        case ConstantType(Constant(c: Symbol)) if c.hasFlag(gf.JAVA_ENUM) =>
          // Manually widen Java enums to obtain `x: FileVisitResult`
          // instead of `x: FileVisitResult.Continue.type`
          TypeRef(ThisType(tpe.typeSymbol.owner), tpe.typeSymbol, Nil)
        case t: PolyType => loop(t.resultType)
        case t: MethodType => loop(t.resultType)
        case RefinedType(parents, _) =>
          // Remove redundant `Product with Serializable`, if possible.
          val strippedParents = parents.filterNot { tpe =>
            definitions.isPossibleSyntheticParent(tpe.typeSymbol)
          }
          val newParents =
            if (strippedParents.nonEmpty) strippedParents
            else parents
          RefinedType(newParents.map(loop), EmptyScope)
        case NullaryMethodType(tpe) =>
          NullaryMethodType(loop(tpe))
        case TypeRef(pre, sym, args) =>
          val tpeString = tpe.toString()
          // NOTE(olafur): special case where `Type.toString()` produces
          // unparseable syntax such as `Path#foo.type`. In these cases, we
          // either try to simplify the type into the parents of the singleton
          // type. When we can't safely simplify the type, we give up and
          // don't annotate the type. See "WidenSingleType.scala" for test
          // cases that stress this code path.
          val isSimplifyToParents =
            tpe.toString().endsWith(".type") &&
              pre.prefixString.endsWith("#")
          if (isSimplifyToParents) {
            val hasMeaningfulParent = sym.info.parents.exists { parent =>
              !isPossibleSyntheticParent(parent)
            }
            val hasMeaningfulDeclaration =
              sym.info.decls.exists(decl =>
                !decl.isOverridingSymbol && !decl.isConstructor
              )
            if (hasMeaningfulParent && !hasMeaningfulDeclaration) {
              loop(RefinedType(sym.info.parents, EmptyScope))
            } else {
              throw new NotImplementedError(
                s"don't know how to produce parseable type for '$tpeString'"
              )
            }
          } else {
            TypeRef(loop(pre), sym, args.map(loop))
          }
        case ExistentialType(head :: Nil, underlying) =>
          head.info match {
            case b @ TypeBounds(RefinedType(parents, _), hi)
                if parents.length > 1 =>
              // Remove the lower bound large `Type[_ >: A with B with C <: D
              // with Serializable]` so that it becomes only `Type[_ <: D]`.
              // Large lower bounds `_ >: A with B with C ...` happen in
              // situations like `val x = List(A, B, C)`.
              head.setInfo(TypeBounds(definitions.NothingTpe, loop(hi)))
            case _ =>
          }
          tpe
        case SingleType(ThisType(osym), sym)
            if sym.isKindaTheSameAs(sym) &&
              gsymbolReplacements.contains(semanticdbSymbol(sym)) =>
          loop(
            TypeRef(NoPrefix, gsymbolReplacements(semanticdbSymbol(sym)), Nil)
          )
        case tpe => tpe
      }
    }
    loop(tpe)
  }
  private val isRootSymbol = Set[g.Symbol](
    g.rootMirror.RootClass,
    g.rootMirror.RootPackage
  )
}
