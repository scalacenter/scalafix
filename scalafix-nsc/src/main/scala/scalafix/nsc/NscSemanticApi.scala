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

  /** The compiler sets different symbols for `PackageDef`s
		* and term names pointing to that package. Get the
		* underlying symbol of `moduleClass` for them to be equal. */
  @inline
  def getUnderlyingPkgSymbol(pkgSym: g.Symbol) = {
    if (!pkgSym.isModule) pkgSym
    else pkgSym.asModule.moduleClass
  }

  private class ScopeTraverser extends g.Traverser {
    val topLevelPkg: g.Symbol = g.rootMirror.RootPackage
    // Don't introduce fully qualified scopes to cause lookup failure
    val scopes = mutable.Map[g.Symbol, g.Scope](topLevelPkg -> g.newScope)
    val topLevelScope = scopes(topLevelPkg)
    var enclosingScope = topLevelScope

    def getScope(sym: g.Symbol): g.Scope = {
      logger.elem(scopes.keySet.map(_.hashCode))
      scopes.getOrElseUpdate(sym,
                             scopes
                               .get(sym.owner)
                               .map(_.cloneScope)
                               .getOrElse(topLevelScope))
    }

    def addAll(members: g.Scope, scope: g.Scope): g.Scope = {
      members
        .filterNot(s =>
          s.isRoot || s.isPackageObjectOrClass || s.hasPackageFlag)
        .foreach(scope.enter)
      scope
    }

    override def traverse(t: g.Tree): Unit = {
      t match {
        case pkg: g.PackageDef =>
          val sym = getUnderlyingPkgSymbol(pkg.pid.symbol)
          val currentScope = getScope(sym)
          currentScope.enter(sym)

          val previousScope = enclosingScope
          enclosingScope = currentScope
          super.traverse(t)
          enclosingScope = previousScope

        case g.Import(pkg, selectors) =>
          val pkgSym = pkg.symbol

          // Add imported members of FQN
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
        case g.DefDef(_, _, _, _, tpt, _) =>
          offsets += (tpt.pos.point -> tpt)
          treeOwners += (t.pos.point -> t)
        case _ => super.traverse(t)
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

    // Compute scopes for global and local imports
    val st = new ScopeTraverser
    st.traverse(unit.body)
    val rootPkg = st.topLevelPkg
    val rootImported = st.scopes(rootPkg)
    logger.elem(rootImported)
    logger.elem(rootPkg.info.members)

    // Compute offsets for the whole compilation unit
    val traverser = new OffsetTraverser
    traverser.traverse(unit.body)

    def toMetaType(tp: g.Tree) =
      config.dialect(tp.toString).parse[m.Type].get

    def gimmePosition(t: m.Tree): m.Position = {
      t match {
        case m.Defn.Val(_, Seq(pat), _, _) => pat.pos
        case m.Defn.Def(_, name, _, _, _, _) => name.pos
        case _ => t.pos
      }
    }

    def stripThis(ref: m.Tree) = {
      ref.transform {
        case m.Term.Select(m.Term.This(ind: m.Name.Indeterminate), name) =>
          m.Term.Select(m.Term.Name(ind.value), name)
        case m.Type.Select(m.Term.This(ind: m.Name.Indeterminate), name) =>
          m.Type.Select(m.Term.Name(ind.value), name)
      }
    }

    /** Collect all the Meta names from a Meta ref. */
    def collectNames(name: m.Ref): List[m.Name] = {
      @scala.annotation.tailrec
      def loop(ref: m.Ref, acc: List[m.Name]): List[m.Name] = {
        ref match {
          case name: m.Term.Name => name :: acc
          case name: m.Type.Name => name :: acc
          case m.Type.Select(qual, name) => loop(qual, name :: acc)
          case m.Term.Select(qual, name) =>
            loop(qual.asInstanceOf[m.Ref], name :: acc)
          // Preserve indeterminate names so that are converted to type names
          case m.Term.This(name: m.Name.Indeterminate) => name :: acc
          case _ =>
            sys.error(s"Type reference has unexpected ${ref.structure}")
        }
      }
      loop(name, Nil)
    }

    /** Look up a Meta name for both Reflect Term and Type names.
      *
      * Unfortunately, meta selection chains are term refs while they could
      * be type refs (e.g. `A` in `A.this.B` is a term). For this reason,
      * we need to check both names to guarantee whether a name is in scope.
      */
    @inline
    def lookupBothNames(name: String, in: g.Scope): g.Symbol = {
      val typeName = g.TypeName(name)
      val typeNameLookup = in.lookup(typeName)
      val symbol = if (typeNameLookup.exists) typeNameLookup
      else {
        val termName = g.TermName(name)
        val termNameLookup = in.lookup(termName)
        if (termNameLookup.exists) termNameLookup
        else g.NoSymbol
      }
      if (symbol.isOverloaded)
        symbol.alternatives.head
      else symbol
    }

    /** Remove sequential prefixes from a concrete ref. */
    def removePrefixes(ref: m.Ref, prefixes: List[m.Name]): m.Ref = {
      /* Pattern match on `value`s of names b/c `stripThis` creates new names. */
      def loop(ref: m.Term.Ref,
               reversedPrefixes: List[m.Name]): List[m.Term.Ref] = {
        reversedPrefixes match {
          case prefix :: acc =>
            val prefixValue = prefix.value
            ref match {
              case m.Term.Select(qual, name) =>
                val qualAsRef = qual.asInstanceOf[m.Term.Ref]
                if (name.value == prefixValue) loop(qualAsRef, acc)
                else {
                  // Make sure that removes names seq and reconstruct trees
                  val nestedResult = loop(qualAsRef, reversedPrefixes)
                  if (nestedResult.isEmpty) List(name)
                  else List(m.Term.Select(nestedResult.head, name))
                }
              case name: m.Term.Name if name.value == prefixValue => Nil
              case r => List(r)
            }
          case Nil => List(ref)
        }
      }

      val transformedRef = ref.transform {
        case m.Type.Select(qual, typeName) =>
          val removed = loop(qual, prefixes.reverse)
          if (removed.isEmpty) typeName
          else m.Type.Select(removed.head, typeName)
        case r => r
      }

      transformedRef.asInstanceOf[m.Ref]
    }

    def isOnlyVariable(symbol: g.Symbol) =
      symbol.isAccessor

    @inline
    def isModuleOrAccessor(symbol: g.Symbol) =
      symbol.isModule || symbol.isAccessor

    /* Missing import and shortened name */
    type Missing = (m.Ref, m.Ref)

    /* Shortened name */
    type Hit = m.Ref

    /* */
    def getMissingOrHit(
        ref: m.Ref,
        inScope: g.Scope,
        enclosingTerm: g.Symbol): Either[Missing, Hit] = {

      val refNoThis = stripThis(ref).asInstanceOf[m.Ref]
      val names = refNoThis.collect {
        case tn: m.Term.Name => tn
        case tn: m.Type.Name => tn
      }

      // Mix local scope with root scope for FQN and non-FQN lookups
      val wholeScope = mixScopes(inScope, rootPkg.info.members)
      logger.elem(wholeScope)
      val (_, reversedSymbols) = {
        names.iterator.foldLeft(wholeScope -> List.empty[g.Symbol]) {
          case ((scope, symbols), metaName) =>
            val sym = lookupBothNames(metaName.value, scope)
            logger.elem(sym)
            logger.elem(sym.info.members)
            if (!sym.exists) scope -> symbols
            else sym.info.members -> (sym :: symbols)
        }
      }

      val symbols = reversedSymbols.reverse
      val metaToSymbols = names.zip(symbols)
      logger.elem(metaToSymbols)

      if (symbols.nonEmpty) {
        /* Check for path dependent types:
         * 1. Locate the term among the FQN
         * 2. If it exists, get the first value in the chain */
        val maybePathDependentType = metaToSymbols
          .find(ms => isOnlyVariable(ms._2))
          .flatMap(_ => metaToSymbols.dropWhile(ms => !ms._2.isValue).headOption)
        logger.elem(maybePathDependentType)
        val isPathDependent = maybePathDependentType.isDefined

        val (lastName, lastSymbol) = maybePathDependentType
          .getOrElse(metaToSymbols.last)
        val (onlyPaths, shortenedNames) =
          metaToSymbols.span(_._1 != lastName)

        val localSym = inScope.lookup(lastSymbol.name)
        if (lastSymbol.exists &&
            (isPathDependent || localSym.exists)) {
          // Return shortened type for names already in scope
          val onlyNames = onlyPaths.map(_._1)
          Right(removePrefixes(refNoThis, onlyNames))
        } else {
          // Remove unnecessary packages from type name
          val noRedundantPaths = {
            val paths = onlyPaths.dropWhile(path =>
              getUnderlyingPkgSymbol(path._2) != enclosingTerm)
            if (paths.isEmpty) onlyPaths.map(_._1)
            else paths.tail.map(_._1)
          }

          // Shortened names must be just one if no PDT
          assert(shortenedNames.size == 1)
          assert(noRedundantPaths.size >= 1)

          // Get type name to use and build refs out of the names
          val useName = shortenedNames.head._1.asInstanceOf[m.Type.Name]
          val refs = noRedundantPaths.asInstanceOf[List[m.Term.Ref]]
          val pathImportRef = refs.reduceLeft[m.Term.Ref] {
            case (qual: m.Term, path: m.Term.Name) =>
              m.Term.Select(qual, path)
          }

          val importRef = m.Type.Select(pathImportRef, useName)
          Left(importRef -> useName)
        }
      // Received type is not valid/doesn't exist, return what we got
      } else Left(refNoThis -> refNoThis)
    }

    def mixScopes(sc: g.Scope, sc2: g.Scope) = {
      val mixedScope = sc.cloneScope
      sc2.foreach(s => mixedScope.enterIfNew(s))
      mixedScope
    }

    new SemanticApi {
      override def shortenType(tpe: m.Type,
                               owner: m.Tree): (m.Type, Seq[m.Ref]) = {
        logger.elem(tpe)
        val ownerTpePos = gimmePosition(owner).start.offset
        val ownerTree = traverser.treeOwners(ownerTpePos)
        val gtpeTree = traverser.offsets(ownerTpePos)
        val ownerSymbol = ownerTree.symbol
        val contextOwnerChain = ownerSymbol.ownerChain

        // Clean up invalid syntactic imports
        def keepInterestingOwner(s: g.Symbol): Boolean =
          !(s.isRoot || s.isEmptyPackage)

        def keepValidMembers(s: g.Symbol): Boolean =
          !(s.hasPackageFlag || s.isPackageObjectOrClass || s.isPackageObjectClass) &&
            (s.isModule || s.isClass || s.isValue || s.isAccessor) && !s.isMethod

        val interestingOwners =
          contextOwnerChain.filter(keepInterestingOwner)
        val bottomUpScope = interestingOwners.iterator
          .map(_.info.members.filter(keepValidMembers))
          .reduce(mixScopes _)
        interestingOwners.foreach(owner => bottomUpScope.enter(owner))

        val enclosingPkg =
          contextOwnerChain
            .find(s => s.hasPackageFlag)
            .getOrElse(rootPkg)
        logger.elem(enclosingPkg)

        val userImportsScope = {
          enclosingPkg.ownerChain
            .foldLeft(rootImported) { (accScope: g.Scope, owner: g.Symbol) =>
              if (accScope != rootImported) accScope
              else st.scopes.getOrElse(owner, rootImported)
            }
        }

        val globalScope = mixScopes(bottomUpScope, userImportsScope)

        // Get only the type Select chains (that inside have terms)
        val typeRefs = tpe.collect { case ts: m.Type.Select => ts }
        val typeRewrites = typeRefs.map(tr =>
          getMissingOrHit(tr, globalScope, enclosingPkg))
        val typeRefsAndRewrites = typeRefs.zip(typeRewrites)

        val shortenedType = typeRefsAndRewrites.foldLeft(tpe) {
          case (targetTpe, (typeRef, missingOrHit)) =>
            val newTpe = missingOrHit.fold(
              m => targetTpe.transform { case ref if ref == typeRef => m._2 },
              h => targetTpe.transform { case ref if ref == typeRef => h }
            )
            // Cast to type, transform returns tree
            newTpe.asInstanceOf[m.Type]
        }

        val shortenedImports = typeRewrites.collect {
          case Left(missing) => missing._1
        }

        (shortenedType -> shortenedImports)
      }

      override def typeSignature(defn: m.Defn): Option[m.Type] = {
        defn match {
          case m.Defn.Val(_, Seq(pat), _, _) =>
            traverser.offsets.get(pat.pos.start.offset).map(toMetaType)
          case m.Defn.Def(_, name, _, _, _, _) =>
            traverser.offsets.get(name.pos.start.offset).map(toMetaType)
          case _ => None
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
