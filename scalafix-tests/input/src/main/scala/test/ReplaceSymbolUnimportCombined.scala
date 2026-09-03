/*
rules = [
  "replace:scala.concurrent.Future/com.geirsson.Future"
  "replace:scala.concurrent.Promise/com.geirsson.Promise"
]
 */
package test

// A single nested wildcard importer that both renames one replaced symbol and
// explicitly names another, alongside the wildcard. Every moved selector must
// be dropped and folded into unimports so the wildcard cannot re-introduce the
// old bindings (scalacenter/scalafix#376), while the surviving wildcard name
// (ExecutionContext) stays available.
object ReplaceSymbolUnimportCombined {
  def f: Any = {
    import scala.concurrent.{Future => AliasF, Promise, _}
    AliasF.successful(1)
    Promise[Int]()
    val ec: ExecutionContext = null
    ec
  }
}
