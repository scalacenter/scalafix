package scalafix.internal.pc

import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

import scala.meta.pc.CancelToken
import scala.meta.pc.OffsetParams

case class CompilerOffsetParams(
    uri: URI,
    text: String,
    offset: Int
) extends OffsetParams {

  override def token(): CancelToken = new CancelToken {

    override def checkCanceled(): Unit = ()

    override def onCancel(): CompletionStage[java.lang.Boolean] =
      CompletableFuture.completedFuture(java.lang.Boolean.FALSE)

  }

}
