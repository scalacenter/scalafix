package foo.bar

import scalafix.v0._
import scala.meta._

case object MyRule extends Rule {
  def rule(ctx: RuleCtx): Patch = {
    val custom =
      ctx.config.x.dynamic.custom.asConf
        .map(_.asInstanceOf[Conf.Bool].value)
        .get
    assert(custom, "Expected `x.custom = true` in the configuration!")
    ctx.tree.collect {
      case n: scala.meta.Name => ctx.replaceTree(n, n.syntax + "1")
    }.asPatch
  }
}

case class MyRule2(index: SemanticdbIndex) extends SemanticRule(index) {
  def rule(ctx: RuleCtx): Patch =
    ctx.addGlobalImport(importer"scala.collection.immutable.Seq")
}

