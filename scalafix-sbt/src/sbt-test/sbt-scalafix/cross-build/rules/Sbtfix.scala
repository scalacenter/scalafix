import scalafix.v0._
object Sbtfix extends Rule("Sbtfix") {
  override def fix(ctx: RuleCtx): Patch = {
    if (ctx.tree.pos.input.toString.contains("sbtfix-me"))
      ctx.replaceTree(ctx.tree, "// sbtfixed\n")
    else Patch.empty
  }
}
