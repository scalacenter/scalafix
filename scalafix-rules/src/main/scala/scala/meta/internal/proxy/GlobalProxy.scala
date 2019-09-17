package scala.meta.internal.proxy

import scala.meta.internal.pc.MetalsGlobal
import scala.reflect.internal.util.Position

object GlobalProxy {
  def typedTreeAt(g: MetalsGlobal, pos: Position): g.Tree = {
    g.typedTreeAt(pos)
  }
}
