// Small worksheet to demonstrate usage of import patches
import scala.collection.immutable.Seq
import scala.meta._
import scalafix.rewrite.ScalafixCtx
import scalafix.util.Patch
import scalafix.util.TreePatch.AddGlobalImport
import scalafix.util.TreePatch.RemoveGlobalImport
import scala.meta._
import scalafix.util.logger
val code =
  """import scala.language.higherKinds
    |import java.{util => ju}
    |import scala.collection.mutable._
    |
    |object a
  """.stripMargin.parse[Source].get
implicit val ctx: ScalafixCtx = RewriteCtx.fromCode(code)
val add = AddGlobalImport(importer"cats.data.EitherT")
val addRedundant = AddGlobalImport(importer"scala.collection.mutable._")
val remove = RemoveGlobalImport(importer"scala.language.higherKinds")

val x = Patch.apply(code, Seq(add, addRedundant, remove))
logger.elem(x)
