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
  private def getUnderlyingPkgSymbol(pkgSym: g.Symbol) = {
    if (!pkgSym.isModule) pkgSym
    else pkgSym.asModule.moduleClass
  }

  /** Keep members that are relevant for scoping rules.
    * For example, remove methods (don't appear in types).  */
  @inline
  private def keepRelevantMembers(s: g.Symbol): Boolean =
    (s.isModule || s.isValue || s.isAccessor || s.isType) &&
      !(s.isMethod || s.isSynthetic || s.hasPackageFlag)

  @inline
  private def mixScopes(sc: g.Scope, sc2: g.Scope) = {
    val mixedScope = sc.cloneScope
    sc2.foreach(s => mixedScope.enterIfNew(s))
    mixedScope
  }

  private object RootImports {
    import g.definitions.{ScalaPackage, PredefModule, JavaLangPackage}
    val javaImports = List(JavaLangPackage)
    val scalaAndJavaImports = List(ScalaPackage, JavaLangPackage)
    val allImports = List(PredefModule, ScalaPackage, JavaLangPackage)
  }

  /** Return a scope with the default packages imported by Scala.
    * Follow the same semantics as `rootImports` in the global Context. */
  private def importDefaultPackages(scope: g.Scope, unit: g.CompilationUnit) = {
    // Handle packages to import from user-defined compiler flags
    val packagesToImportFrom = {
      if (g.settings.noimports) Nil
      else if (unit.isJava) RootImports.javaImports
      else if (g.settings.nopredef) RootImports.scalaAndJavaImports
      else RootImports.allImports
    }
    packagesToImportFrom.foldLeft(scope) {
      case (scope, pkg) => mixScopes(scope, pkg.info.members)
    }
  }

  /** Traverse the tree and create the scopes based on packages and imports. */
  private class ScopeTraverser(unit: g.CompilationUnit) extends g.Traverser {
    val topLevelPkg: g.Symbol = g.rootMirror.RootPackage
    // Don't introduce fully qualified scopes to cause lookup failure
    val topLevelScope = importDefaultPackages(g.newScope, unit)
    val scopes = mutable.Map[g.Symbol, g.Scope](topLevelPkg -> topLevelScope)
    val renames = mutable.Map[g.Symbol, g.Symbol]()
    var enclosingScope = topLevelScope

    /** Get the scope for a given symbol. */
    def getScope(sym: g.Symbol): g.Scope = {
      scopes.getOrElseUpdate(sym, {
        scopes
          .get(sym.owner)
          .map(_.cloneScope)
          .getOrElse(topLevelScope)
      })
    }

    /** Add all the `members` to a mutable scope. */
    @inline
    def addAll(members: g.Scope, scope: g.Scope): g.Scope = {
      members
        .filterNot(s => s.isRoot || s.hasPackageFlag)
        .foreach(scope.enterIfNew)
      scope
    }

    /** Define a rename from a symbol to a name. */
    @inline
    def addRename(symbol: g.Symbol, renamedTo: g.Name) = {
      val renamedSymbol = symbol.cloneSymbol.setName(renamedTo)
      renames += symbol -> renamedSymbol
    }

    /** Get the underlying type if symbol represents a type alias. */
    @inline
    def getUnderlyingTypeAlias(symbol: g.Symbol) =
      symbol.info.dealias.typeSymbol

    /** Look up type and term symbols from a given name. */
    def lookupTypeAndTerm(name: g.Name, scope: g.Scope) = {
      val termSymbol = scope.lookup(name.toTermName)
      val typeSymbol = getUnderlyingTypeAlias(scope.lookup(name.toTypeName))
      termSymbol -> typeSymbol
    }

    def traverseScopes(): Unit = traverse(unit.body)

    override def traverse(t: g.Tree): Unit = {
      t match {
        case pkg: g.PackageDef =>
          val sym = getUnderlyingPkgSymbol(pkg.pid.symbol)
          val currentScope = getScope(sym)
          currentScope.enterIfNew(sym)

          // Add members when processing the packages globally
          val members = sym.info.members.filter(keepRelevantMembers)
          members.foreach(currentScope.enterIfNew)

          // Set enclosing package before visiting enclosed trees
          val previousScope = enclosingScope
          enclosingScope = currentScope
          super.traverse(t)
          enclosingScope = previousScope

        case g.Import(pkg, selectors) =>
          val pkgMembers = pkg.symbol.info.members

          // TODO(jvican): Find out Scala rules to process imports
          // We could be processing them in the wrong order...
          selectors.foreach {
            case g.ImportSelector(g.TermName("_"), _, null, _) =>
              // Wildcard import, add everything in the package
              addAll(pkgMembers, enclosingScope)

            case g.ImportSelector(from, _, g.TermName("_"), _) =>
              // Look up symbol and unlink it from the scope
              val symbol = enclosingScope.lookup(from)
              if (symbol.exists)
                enclosingScope.unlink(symbol)

            case g.ImportSelector(name, _, same, _) if name == same =>
              // Ordinary name import, add both term and type to scope
              val (termSymbol, typeSymbol) =
                lookupTypeAndTerm(name, pkgMembers)
              if (termSymbol.exists)
                enclosingScope.enterIfNew(termSymbol)
              if (typeSymbol.exists)
                enclosingScope.enterIfNew(typeSymbol)

            case g.ImportSelector(from, _, to, _) if to != null =>
              // Found a rename, look up symbols and change names
              val (termSymbol, typeSymbol) =
                lookupTypeAndTerm(from, pkgMembers)

              // Add non-renamed symbol because FQN will fully resolve renames
              if (termSymbol.exists) {
                enclosingScope.enterIfNew(termSymbol)
                addRename(termSymbol, to)
              }
              if (typeSymbol.exists) {
                enclosingScope.enterIfNew(typeSymbol)
                addRename(typeSymbol, to)
              }
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
      val instructions =
        "Please re-compile with the scalac option -Yrangepos enabled"
      val explanation =
        "This option is necessary for the semantic API to function"
      sys.error(s"$instructions. $explanation")
    }

    // Compute scopes for global and local imports
    val st = new ScopeTraverser(unit)
    st.traverseScopes()
    val rootPkg = st.topLevelPkg
    val rootImported = st.scopes(rootPkg)
    val realRootScope = rootPkg.info.members

    // Compute offsets for the whole compilation unit
    val traverser = new OffsetTraverser
    traverser.traverse(unit.body)

    def toMetaType(tp: g.Tree) = {
      config.dialect(tp.toString).parse[m.Type].get
    }

    def gimmePosition(t: m.Tree): m.Position = {
      t match {
        case m.Defn.Val(_, Seq(pat), _, _) => pat.pos
        case m.Defn.Def(_, name, _, _, _, _) => name.pos
        case _ => t.pos
      }
    }

    /** Strip `this` references and convert indeterminate names to term names. */
    def stripThis(ref: m.Tree): m.Ref = {
      val transformedTree = ref.transform {
        case m.Term.Select(m.Term.This(ind: m.Name.Indeterminate), name) =>
          m.Term.Select(m.Term.Name(ind.value), name)
        case m.Type.Select(m.Term.This(ind: m.Name.Indeterminate), name) =>
          m.Type.Select(m.Term.Name(ind.value), name)
      }
      transformedTree.asInstanceOf[m.Ref]
    }

    /** Look up a Meta name for both Reflect Term and Type names.
      *
      * Meta type selections are always qualified by terms, so we cannot
      * know whether a term name represents a term or a type. Check both
      * types given a preference to types (since we are shortening them).
      */
    @inline
    def lookupBothNames(
        name: String,
        in: g.Scope,
        disambiguatingOwner: Option[g.Symbol],
        disambiguatingNamespace: String): (g.Symbol, g.Symbol) = {
      def disambiguateOverloadedSymbol(symbol: g.Symbol) = {
        if (symbol.isOverloaded) {
          val alternatives = symbol.alternatives
          disambiguatingOwner
            .flatMap(o => alternatives.find(_.owner == o))
            .getOrElse {
              val substrings = alternatives.iterator
                .filter(s => disambiguatingNamespace.indexOf(s.fullName) == 0)
              // Last effort to disambiguate, pick sym with longest substring
              if (substrings.isEmpty) alternatives.head
              else substrings.maxBy(_.fullName.length)
            }
        } else symbol
      }

      val termName = g.TermName(name)
      val termSymbol = disambiguateOverloadedSymbol(in.lookup(termName))
      val typeName = g.TypeName(name)
      val typeSymbol = disambiguateOverloadedSymbol(in.lookup(typeName))
      termSymbol -> typeSymbol
    }

    /** Look up the symbols attached to meta names in a scope.
      *
      * As we don't know a priori whether a meta name represents a type or
      * a term, we need to explore the whole binary tree of lookups until:
      *   1. All the meta names have a symbol.
      *   2. The last found symbol is a type (we are shortening types).
      *
      * For that, we perform a DFS post-order traversal in the tree of
      * all possible names (first as a type name and then as a term name).
      */
    def lookupSymbols(names: List[m.Name],
                      scope: g.Scope,
                      from: String): List[g.Symbol] = {

      // First compute the size for names
      val namesSize = names.size

      def traverseLookupTree(names: List[m.Name],
                             scope: g.Scope,
                             symbols: List[g.Symbol]): List[g.Symbol] = {
        @inline
        def traverse(symbol: g.Symbol, at: List[m.Name]) = {
          val nextSymbols = symbol :: symbols
          val nextScope = symbol.info.members
          traverseLookupTree(at, nextScope, nextSymbols)
        }

        @inline
        def isSuffixTypeSymbol(symbols: List[g.Symbol]) = {
          symbols match {
            case symbol :: acc => symbol.isType
            case Nil => false
          }
        }

        @inline
        def isCorrectResult(symbols: List[g.Symbol]) =
          namesSize == symbols.size && isSuffixTypeSymbol(symbols)

        names match {
          case name :: acc =>
            val lastSymbol = symbols.headOption
            val (termSymbol, typeSymbol) =
              lookupBothNames(name.value, scope, lastSymbol, from)
            val leftTraversal =
              if (!typeSymbol.exists) symbols
              else traverse(typeSymbol, acc)
            val rightTraversal =
              if (!termSymbol.exists) symbols
              else traverse(termSymbol, acc)

            // Return the first complete lookup that is a type
            if (isCorrectResult(leftTraversal)) leftTraversal
            else if (isCorrectResult(rightTraversal)) rightTraversal
            else symbols
          case Nil => symbols
        }
      }

      traverseLookupTree(names, scope, Nil).reverse
    }

    /** Rename a type based on used import selectors. */
    def renameType[T <: m.Name](toRename: T, renames: Map[m.Name, g.Name]) = {
      val renamed = toRename match {
        case name: m.Name =>
          renames.get(name) match {
            case Some(gname) =>
              val realName = gname.decoded
              if (gname.isTypeName) m.Type.Name(realName)
              else m.Term.Name(realName)
            case None => name
          }
      }
      renamed.asInstanceOf[T]
    }

    def toTermName(name: m.Name): m.Term.Name =
      if (name.is[m.Term.Name]) name.asInstanceOf[m.Term.Name]
      else m.Term.Name(name.value)

    def toTypeName(name: m.Name): m.Type.Name =
      if (name.is[m.Type.Name]) name.asInstanceOf[m.Type.Name]
      else m.Type.Name(name.value)

    /** Get the shortened type `ref` at a concrete spot. */
    def getMissingOrHit(toShorten: m.Ref,
                        inScope: g.Scope,
                        enclosingTerm: g.Symbol): m.Ref = {

      val refNoThis = stripThis(toShorten)
      val names = refNoThis.collect {
        case tn: m.Term.Name => tn
        case tn: m.Type.Name => tn
      }

      // Mix local scope with root scope for FQN and non-FQN lookups
      val wholeScope = mixScopes(inScope, realRootScope)
      val symbols = lookupSymbols(names, wholeScope, toShorten.syntax)
      val metaToSymbols = names.zip(symbols)

      if (symbols.isEmpty) refNoThis
      else {
        val maybePathDependentType =
          metaToSymbols.find(ms => ms._2.isAccessor)
        val isPathDependent = maybePathDependentType.isDefined

        // Get the latest value in path if it's path dependent type
        val firstTermInPathDependent = maybePathDependentType.flatMap(_ =>
          metaToSymbols.dropWhile(ms => !ms._2.isValue).headOption)

        val (lastName, lastSymbol) =
          firstTermInPathDependent.getOrElse(metaToSymbols.last)
        val (onlyPaths, shortenedNames) =
          metaToSymbols.span(_._1 != lastName)

        // Build map of meta names to reflect names
        val renames: Map[m.Name, g.Name] = metaToSymbols.map {
          case (metaName, symbol) =>
            val mappedSym = st.renames.getOrElse(symbol, symbol)
            metaName -> mappedSym.name
        }.toMap

        // Get shortened and renamed type name
        val onlyShortenedNames = shortenedNames.map(_._1)
        val typeName = toTypeName(onlyShortenedNames.last)
        val renamedTypeName = renameType(typeName, renames)

        /* Strategy:
         *   - Has the local scope lookup succeded and is not a PDT?
         *     Return type name since it's already in the scope.
         *   - Otherwise, compute the shortened prefix to use. */
        val localSym = inScope.lookup(lastSymbol.name)
        if (!isPathDependent && localSym.exists) renamedTypeName
        else {
          // Compute the shortest prefix that we append to type name
          val shortenedRefs = {
            if (isPathDependent) onlyShortenedNames.init
            else {
              // Get last symbol that was found in scope
              val lastAccessibleRef =
                onlyPaths.reduceLeft[(m.Name, g.Symbol)] {
                  case (t1 @ (_, s1), t2 @ (_, s2)) =>
                    if (s1.exists && s2.exists) t2 else t1
                }
              val lastName = lastAccessibleRef._1
              renameType(lastName, renames) :: onlyShortenedNames.init
            }
          }
          val termRefs = shortenedRefs.map(toTermName)
          val termSelects = termRefs.reduceLeft[m.Term.Ref] {
            case (qual: m.Term, path: m.Term.Name) =>
              m.Term.Select(qual, renameType(path, renames))
          }
          m.Type.Select(termSelects, renamedTypeName)
        }
      }
    }

    new SemanticApi {
      override def shortenType(tpe: m.Type, owner: m.Tree): m.Type = {
        val ownerTpePos = gimmePosition(owner).start.offset
        val ownerTree = traverser.treeOwners(ownerTpePos)
        val gtpeTree = traverser.offsets(ownerTpePos)
        val ownerSymbol = ownerTree.symbol
        val contextOwnerChain = ownerSymbol.ownerChain

        val enclosingPkg =
          contextOwnerChain
            .find(s => s.hasPackageFlag)
            .getOrElse(rootPkg)

        val userImportsScope = {
          if (enclosingPkg == rootPkg) rootImported
          else st.scopes.getOrElse(enclosingPkg, rootImported)
        }

        // Prune owners and use them to create a local bottom up scope
        val interestingOwners =
          contextOwnerChain.takeWhile(_ != enclosingPkg)
        val bottomUpScope = interestingOwners.iterator
          .map(_.info.members.filter(keepRelevantMembers))
          .reduce(mixScopes _)
        val globalScope = mixScopes(bottomUpScope, userImportsScope)

        // Get only the type Select chains (that inside have terms)
        val typeRefs = tpe.collect { case ts: m.Type.Select => ts }

        // Collect rewrites to be applied on type refs
        val typeRewrites =
          typeRefs.map(tr => getMissingOrHit(tr, globalScope, enclosingPkg))
        val mappedRewrites = typeRefs.zip(typeRewrites).toMap

        // Replace the types in one transform to not change ref equality
        val shortenedType =
          tpe.transform { case ref: m.Type.Select => mappedRewrites(ref) }
        shortenedType.asInstanceOf[m.Type]
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
