
package test.explicitResultTypes

object ExplicitResultTypesSingleton {
  implicit val default: ExplicitResultTypesSingleton.type = ExplicitResultTypesSingleton
  implicit val singleton: ExplicitResultTypesSingleton2.Singleton.type = ExplicitResultTypesSingleton2.Singleton
}
object ExplicitResultTypesSingleton2 {
  object Singleton
}
