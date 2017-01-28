import scala.collection.immutable.Seq
import scala.meta._
import scalafix.rewrite.RewriteCtx
import scalafix.util.CanonicalImport
import scalafix.util.Patch
import scalafix.util.TreePatch.AddGlobalImport

val code =
  """import scala.language.higherKinds
    |import java.{util => ju}
    |import scala.collection.mutable._
    |
    |object a
  """.stripMargin.parse[Source].get
implicit val ctx: RewriteCtx = RewriteCtx.fromCode(code)
val patch = AddGlobalImport(CanonicalImport.fromImportee(q"cats.data", importee"EitherT"))

val x = Patch.apply(code, Seq(patch))

