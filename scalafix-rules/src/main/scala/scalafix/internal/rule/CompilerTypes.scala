package scalafix.internal.rule

import scala.meta.internal.pc.MetalsGlobal
import scalafix.v1._
import scala.meta._
import scala.meta.internal.proxy.GlobalProxy
import scala.collection.mutable
import scala.reflect.internal.{Flags => gf}

class CompilerTypes(global: Option[MetalsGlobal])(
    implicit ctx: SemanticDocument
) {
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
              case ConstantType(Constant(c: Symbol))
                  if c.hasFlag(gf.JAVA_ENUM) =>
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
}
