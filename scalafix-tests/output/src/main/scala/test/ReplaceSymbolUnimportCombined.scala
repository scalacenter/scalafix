package test

import com.geirsson.{Future, Promise}
// A single nested wildcard importer that both renames one replaced symbol and
// explicitly names another, alongside the wildcard. Every moved selector must
// be dropped and folded into unimports so the wildcard cannot re-introduce the
// old bindings (scalacenter/scalafix#376), while the surviving wildcard name
// (ExecutionContext) stays available.
object ReplaceSymbolUnimportCombined {
  def f: Any = {
    import scala.concurrent.{Future => _, Promise => _, _}
    Future.successful(1)
    Promise[Int]()
    val ec: ExecutionContext = null
    ec
  }
}
