import scala.meta.{Import, Pkg, Source, Term, Tree}

package object fix {
  def collectGlobalImports(tree: Tree): Seq[Import] = tree match {
    case s: Source                 => s.children flatMap collectGlobalImports
    case (_: Source) / (p: Pkg)    => p.children flatMap collectGlobalImports
    case (_: Pkg) / (p: Pkg)       => p.children flatMap collectGlobalImports
    case (_: Source) / (i: Import) => Seq(i)
    case (_: Pkg) / (i: Import)    => Seq(i)
    case _                         => Seq.empty[Import]
  }

  private object / {
    def unapply(tree: Tree): Option[(Tree, Tree)] = tree.parent map (_ -> tree)
  }
}
