package scala.meta.internal.proxy

import scala.reflect.internal.util.Position
import scala.meta.internal.pc.ScalafixGlobal

object GlobalProxy {
  def typedTreeAt(g: ScalafixGlobal, pos: Position): g.Tree = {
    // NOTE(olafur) clearing `unitOfFile` fixes a bug where `typedTreeAt` would
    // produce symbols with erroneous `.info` signatures.
    g.unitOfFile.clear()
    g.typedTreeAt(pos)
  }
}
