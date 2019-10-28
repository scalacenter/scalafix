package scalafix.internal.rule

import scala.meta.internal.pc.ScalafixGlobal
import scalafix.v1
import scala.{meta => m}
import scala.meta.internal.proxy.GlobalProxy
import scala.collection.mutable
import scala.reflect.internal.{Flags => gf}
import scala.meta.internal.pc.Identifier

class TypeRewrite {
  def toPatch(
      pos: m.Position,
      sym: v1.Symbol,
      replace: m.Token,
      defn: m.Defn,
      space: String
  ): Option[v1.Patch] = None
}
object TypeRewrite {
  def apply(
      global: Option[ScalafixGlobal]
  )(implicit ctx: v1.SemanticDocument): TypeRewrite =
    global match {
      case None => new TypeRewrite
      case Some(value) => new CompilerTypeRewrite(value)
    }
}

case class CompilerException(underlying: Throwable)
    extends Exception("compiler crashed", underlying)

class CompilerTypeRewrite(g: ScalafixGlobal)(implicit ctx: v1.SemanticDocument)
    extends TypeRewrite {
  import g._
  private lazy val unit =
    g.newCompilationUnit(ctx.input.text, ctx.input.syntax)
  private val isRewritten = mutable.Map.empty[Name, ShortName]

  override def toPatch(
      pos: m.Position,
      sym: v1.Symbol,
      replace: m.Token,
      defn: m.Defn,
      space: String
  ): Option[v1.Patch] = {
    try toPatchUnsafe(
      pos,
      sym,
      replace,
      defn,
      space
    )
    catch {
      case e: Throwable =>
        throw CompilerException(e)
    }
  }
  def isDebug = Set[String]("inPlace")
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
    // NOTE(olafurpg) Select the supermethod signature if it exists. In Scala 2,
    // type inference picks the type of the override method expression even if
    // it's more precise than the type of the supermethod. In Scala 3, there's a
    // breaking change in type inference to pick the type of the supermethod
    // even if the expression of the override method is more precise. I believe
    // the Scala 3 behavior aligns more closely with the intuition for how
    // people believe type inference works so we go with the signature of the
    // supermethod, if it exists.
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
    val isDebug = this.isDebug(gsym.name.toString())
    if (gsym == g.NoSymbol) {
      None
    } else if (gsym.info == null || gsym.info.isErroneous) {
      None
    } else {
      val context = g.doLocateContext(gpos)
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
      val fromRewritten = isRewritten.filter {
        case (name, short) =>
          (!context.isNameInScope(name) &&
            !context.isNameInScope(name.otherName)) ||
            history.nameResolvesToSymbol(name, short.symbol)
      }
      history.history ++= fromRewritten
      def loop(tpe: g.Type): g.Type = {
        tpe match {
          case tp @ ThisType(sym)
              if tp.toString() == s"${inverseSemanticdbSymbol.owner.nameString}.this.type" =>
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
            TypeRef(loop(pre), sym, args.map(loop))
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
          case tpe => tpe
        }
      }
      val seenFromType: Type =
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
      var extraPatch = v1.Patch.empty
      val toLoop = seenFromType.finalResultType match {
        case RefinedType(parents, decls) if decls.nonEmpty =>
          val body: Option[m.Term] = defn match {
            case t: m.Defn.Val => Some(t.rhs)
            case t: m.Defn.Var => t.rhs
            case t: m.Defn.Def => Some(t.body)
            case _ => None
          }
          body match {
            case Some(body @ m.Term.NewAnonymous(template))
                if body.tokens.head.syntax == "new" =>
              var suffix = ""
              val nameSyntax = gsym.nameSyntax
              while (context.isNameInScope(TypeName(nameSyntax + suffix))) {
                suffix = suffix match {
                  case "" => "1"
                  case _ => (suffix.toInt + 1).toString()
                }
              }
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
              val name = nameSyntax + suffix
              val indent = " " * defn.pos.startColumn
              extraPatch += v1.Patch.addRight(
                body.tokens.head,
                s" ${name}${tparamCall}${paramCallSuffix}\n${indent}class ${name}${tparamDefn}${paramDefnSuffix} extends"
              )
              new PrettyType(name + tparamCall)
            case _ =>
              seenFromType
          }
        case _ => seenFromType
      }
      val shortType = g.shortType(loop(toLoop).widen, history)
      val short = shortType.toString()

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
      isRewritten ++= history.history
      val addImports = for {
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
}
