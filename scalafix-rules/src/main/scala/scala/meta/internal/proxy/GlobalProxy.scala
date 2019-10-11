package scala.meta.internal.proxy

import scala.reflect.internal.util.Position
import scala.meta.internal.pc.ScalafixGlobal

object GlobalProxy {
  def typedTreeAt(g: ScalafixGlobal, pos: Position): g.Tree = {
    g.typedTreeAt(pos)
  }
}
