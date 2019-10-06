package scalafix.internal.rule

import scala.meta.internal.pc.MetalsGlobal
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
      space: String
  ): Option[v1.Patch] = None
}
object TypeRewrite {
  def apply(
      global: Option[MetalsGlobal]
  )(implicit ctx: v1.SemanticDocument): TypeRewrite =
    global match {
      case None => new TypeRewrite
      case Some(value) => new CompilerTypeRewrite(value)
    }
}

class CompilerTypeRewrite(g: MetalsGlobal)(implicit ctx: v1.SemanticDocument)
    extends TypeRewrite {
  import g._
  private lazy val unit =
    g.newCompilationUnit(ctx.input.text, ctx.input.syntax)

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

  override def toPatch(
      pos: m.Position,
      sym: v1.Symbol,
      replace: m.Token,
      space: String
  ): Option[v1.Patch] = {
    val gpos = unit.position(pos.start)
    GlobalProxy.typedTreeAt(g, gpos)
    val gsym = g
      .inverseSemanticdbSymbols(sym.value)
      .find(s => g.semanticdbSymbol(s) == sym.value)
      .getOrElse(g.NoSymbol)
    if (gsym == g.NoSymbol) {
      None
    } else if (gsym.alternatives.length > 1) {
      pprint.log(sym.value)
      pprint.log(gsym)
      pprint.log(gsym.info)
      None
    } else {
      val context = g.doLocateContext(gpos)
      val renames = g.renamedSymbols(context)
      val history = new g.ShortenedNames(
        lookupSymbol = name => {
          context.lookupSymbol(name, _ => true) :: Nil
        },
        renames = renames,
        owners = parentSymbols(context),
        config = g.renameConfig ++ renames
      )
      def loop(tpe: g.Type): g.Type = {
        tpe match {
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
              if (strippedParents.nonEmpty) strippedParents else parents
            RefinedType(newParents.map(loop), EmptyScope)
          case NullaryMethodType(tpe) =>
            NullaryMethodType(loop(tpe))
          case TypeRef(pre, sym, args) =>
            TypeRef(loop(pre), sym, args.map(loop))
          case ExistentialType(head :: Nil, underlying) =>
            head.info match {
              case TypeBounds(RefinedType(parents, _), _)
                  if parents.length > 1 =>
                // Avoid large `Type[_ <: A with B with C]` that get
                // frequently inferred from long lists of ADT subtypes.
                head.setInfo(TypeBounds.empty)
              case _ =>
            }
            tpe
          case tpe => tpe
        }
      }

      if (gsym.name.toString() == "True") {
        // pprint.log/inverse
      }
      val shortT = g.shortType(loop(gsym.info).widen, history)
      val short = shortT.toString()
      if (short == "IndexedParserInput") {
        pprint.log(sym.value)
        pprint.log(gsym)
        pprint.log(gsym.alternatives)
        pprint.log(gsym.info)
      }
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
        val importee: m.Importee = {
          val ident = m.Name.Indeterminate(Identifier(name.name))
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
              m.Term.Select(owner, name)
          }
          v1.Patch.addGlobalImport(m.Importer(ref, List(importee)))
        }
      }
      Some(v1.Patch.addRight(replace, s"$space: $short") ++ addImports)
    }
  }
}
