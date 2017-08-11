package scalafix.rewrite

import scalafix._
import config._
import metaconfig.ConfError
import metaconfig.Configured

object ConfigRewrite {
  def apply(
      patches: ConfigRewritePatches,
      getMirror: LazyMirror): Configured[Option[Rewrite]] = {
    val configurationPatches = patches.all
    if (configurationPatches.isEmpty) Configured.Ok(None)
    else {
      getMirror(RewriteKind.Semantic) match {
        case None =>
          ConfError
            .msg(".scalafix.conf patches require the Semantic API.")
            .notOk
        case Some(mirror) =>
          val rewrite = Rewrite.constant(
            ".scalafix.conf",
            configurationPatches.asPatch,
            mirror
          )
          Configured.Ok(Some(rewrite))
      }
    }
  }

}
