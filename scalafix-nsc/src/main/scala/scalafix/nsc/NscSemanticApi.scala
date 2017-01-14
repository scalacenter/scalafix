package scalafix.nsc

import scala.collection.mutable
import scala.meta.Dialect
import scala.meta.Type
import scala.reflect.internal.util.SourceFile
import scala.{meta => m}
import scalafix.Fixed
import scalafix.Scalafix
import scalafix.ScalafixConfig
import scalafix.rewrite.SemanticApi
import scalafix.util.logger

trait NscSemanticApi extends ReflectToolkit {
  private class ScopeTraverser extends g.Traverser {
    // TODO(jvican): PackageRoot seems to get also first pkg's members
    val topLevelPkg: g.Symbol = g.rootMirror.RootPackage
    val scopes = mutable.Map[g.Symbol, g.Scope](topLevelPkg -> g.newScope)
    var enclosingScope = scopes(topLevelPkg)

    /** The compiler sets different symbols for `PackageDef`s
      * and term names pointing to that package. Get the
      * underlying symbol of `moduleClass` for them to be equal. */
    @inline
    def getUnderlyingPkgSymbol(pkgSym: g.Symbol) =
      pkgSym.asModule.moduleClass

    def getScope(sym: g.Symbol): g.Scope = {
      logger.elem(scopes.keySet.map(_.hashCode))
      scopes.getOrElseUpdate(sym,
                             scopes
                               .get(sym.owner)
                               .map(_.cloneScope)
                               .getOrElse(scopes(topLevelPkg)))
    }

    def addAll(members: g.Scope, scope: g.Scope): g.Scope = {
      members
        .filterNot(s =>
          s.isRoot || s.isPackageObjectOrClass || s.hasPackageFlag)
        .foreach(scope.enter)
      scope
    }

    def addAllMembers(sym: g.Symbol): g.Scope = {
      val currentScope = getScope(sym)
      addAll(sym.info.members, currentScope)
    }

    override def traverse(t: g.Tree): Unit = {
      t match {
        case pkg: g.PackageDef =>
          val sym = getUnderlyingPkgSymbol(pkg.pid.symbol)
          val currentScope = addAllMembers(sym)
          val previousScope = enclosingScope

          enclosingScope = currentScope
          super.traverse(t)
          enclosingScope = previousScope

        case g.Import(pkg, selectors) =>
          val pkgSym = pkg.symbol
          val importedNames = selectors.map(_.name.decode).toSet
          val imported = pkgSym.info.members.filter(m =>
            importedNames.contains(m.name.decode))
          addAll(imported, enclosingScope)

          selectors.foreach {
            case isel @ g.ImportSelector(g.TermName("_"), _, null, _) =>
              addAll(pkgSym.info.members, enclosingScope)
            // TODO(jvican): Handle renames
            case _ =>
          }
          super.traverse(t)
        case _ => super.traverse(t)
      }
    }
  }

  private class OffsetTraverser extends g.Traverser {
    val offsets = mutable.Map[Int, g.Tree]()
    val treeOwners = mutable.Map[Int, g.Tree]()
    override def traverse(t: g.Tree): Unit = {
      t match {
        case g.ValDef(_, name, tpt, _) if tpt.nonEmpty =>
          offsets += (tpt.pos.point -> tpt)
          treeOwners += (t.pos.point -> t)
          logger.elem(t, t.pos.point)
        case g.DefDef(_, _, _, _, tpt, _) =>
          offsets += (tpt.pos.point -> tpt)
          treeOwners += (t.pos.point -> t)
        case _ => super.traverse(t)
      }
    }
  }

  private class FQNMetaConverter extends g.Traverser {
    override def traverse(t: g.Tree): Unit = {
      t match {
        case t => t
      }
    }
  }

  private def getSemanticApi(unit: g.CompilationUnit,
                             config: ScalafixConfig): SemanticApi = {
    if (!g.settings.Yrangepos.value) {
      val instructions = "Please re-compile with the scalac option -Yrangepos enabled"
      val explanation  = "This option is necessary for the semantic API to function"
      sys.error(s"$instructions. $explanation")
    }

    def toMetaType(tp: g.Tree) =
      config.dialect(tp.toString).parse[m.Type].get

    def parseAsType(tp: String) =
      config.dialect(tp).parse[m.Type].get

    def gimmePosition(t: m.Tree): m.Position = {
      t match {
        case m.Defn.Val(_, Seq(pat), _, _) => pat.pos
        case m.Defn.Def(_, name, _, _, _, _) => name.pos
        case _ => t.pos
      }
    }

    val st = new ScopeTraverser
    st.traverse(unit.body)
    val globalPkg = st.topLevelPkg
    val globallyImported = st.scopes(globalPkg)

    val traverser = new OffsetTraverser
    traverser.traverse(unit.body)

    def typeRefsInTpe(tpe: m.Type): Seq[m.Ref] = {
      val b = Seq.newBuilder[m.Ref]
      def loop(t: m.Tree): Unit = {
        t match {
          case s: m.Type.Select => b += s
          case s: m.Term.Select => b += s
          case _ => t.children.foreach(loop)
        }
      }
      loop(tpe)
      b.result()
    }

    def stripRedundantPkg(ref: m.Ref, enclosingPkg: Option[String]): m.Ref = {
      ref
        .transform {
          case ref: m.Term.Select if enclosingPkg.contains(ref.qual.syntax) =>
            ref.name
        }
        .asInstanceOf[m.Ref]
    }

    def stripThis(ref: m.Tree) = {
      ref.transform {
        case m.Term.Select(m.Term.This(m.Name.Indeterminate(_)), qual) =>
          qual
        case m.Type.Select(m.Term.This(m.Name.Indeterminate(_)), qual) =>
          qual
      }
    }

    def toFQN(ref: m.Ref, inScope: g.Scope): m.Ref = {
      logger.elem(ref.structure)
      (ref
        .transform {
          case ts: m.Type.Select =>
            val sym = inScope.lookup(g.TypeName(ts.name.value))
            if (sym.exists) config.dialect(sym.fullName).parse[m.Type].get
            else ts
          case ts: m.Term.Select =>
            val sym = inScope.lookup(g.TermName(ts.name.value))
            if (sym.exists)
            config.dialect(sym.fullName).parse[m.Type].get
            else ts
          case tn: m.Type.Name =>
            //val sym = inScope.lookup(ts.qual)
            logger.elem(tn)

            tn
        })
        .asInstanceOf[m.Ref]
    }

    new SemanticApi {
      override def shortenType(tpe: m.Type,
                               owner: m.Tree): (m.Type, Seq[m.Ref]) = {
        val ownerTpePos = gimmePosition(owner).start.offset
        val ownerTree = traverser.treeOwners(ownerTpePos)
        val gtpeTree = traverser.offsets(ownerTpePos)
        val ownerSymbol = ownerTree.symbol
        val contextOwnerChain = ownerSymbol.ownerChain

        // Clean up invalid syntactic imports
        def keepOwner(s: g.Symbol): Boolean =
          !(s.isRoot ||
            s.isEmptyPackage ||
            s.isPackageObjectOrClass ||
            g.definitions.ScalaPackageClass == s)

        def keepValidReference(s: g.Symbol): Boolean =
          ((s.isStaticModule && !s.isEmptyPackage) || s.isClass ||
            (s.isValue && !s.isMethod && !s.isLabel))

        def mixScopes(sc: g.Scope, sc2: g.Scope) = {
          val newScope = sc.cloneScope
          sc2.foreach(s => newScope.enter(s))
          newScope
        }

        val bottomUpScope = contextOwnerChain
          .filter(keepOwner)
          .filter(s => s.isType)
          .map(_.info.members.filter(keepValidReference))
          .reduce(mixScopes _)
        logger.elem(bottomUpScope)
        val enclosingPkg =
          contextOwnerChain
            .find(s => s.hasPackageFlag)
            .getOrElse(globalPkg)
        val closestScope =
          enclosingPkg.ownerChain
            .foldLeft(globallyImported) {
              (accScope: g.Scope, owner: g.Symbol) =>
                if (accScope != globallyImported) accScope
                else st.scopes.getOrElse(owner, globallyImported)
            }
        logger.elem(closestScope)

        val accessible = bottomUpScope.map(_.fullName).toSet ++ closestScope
            .map(s => parseAsType(s.fullName).syntax)

        val missingRefs =
          typeRefsInTpe(tpe)
          //.map(ref => toFQN(ref, enclosingPkg))
            .filterNot(ref => accessible.contains(ref.syntax))
            .map(stripThis(_).asInstanceOf[m.Ref])
            .filterNot(ref => ref.is[m.Name])

        val allInScope = accessible ++ missingRefs.map(_.syntax)
        val shortenedTpe = stripThis(tpe.transform {
          case ref: m.Type.Select if allInScope.contains(ref.syntax) =>
            ref.name
          case ref: m.Term.Select if allInScope.contains(ref.syntax) =>
            ref.name
        })

        logger.elem(shortenedTpe, missingRefs, allInScope)
        val pkg =
          if (enclosingPkg == globalPkg) None else Some(enclosingPkg.fullName)
        val finalMissingRefs = missingRefs
          .map(ref => stripRedundantPkg(ref, pkg))
        (shortenedTpe.asInstanceOf[m.Type] -> finalMissingRefs)
      }
      override def typeSignature(defn: m.Defn): Option[m.Type] = {
        defn match {
          case m.Defn.Val(_, Seq(pat), _, _) =>
            traverser.offsets.get(pat.pos.start.offset).map(toMetaType)
          case m.Defn.Def(_, name, _, _, _, _) =>
            traverser.offsets.get(name.pos.start.offset).map(toMetaType)
          case _ =>
            None
        }
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
