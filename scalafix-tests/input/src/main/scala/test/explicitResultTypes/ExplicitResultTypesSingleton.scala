/*
rules = ExplicitResultTypes
 */
package test.explicitResultTypes

object ExplicitResultTypesSingleton {
  implicit val default = ExplicitResultTypesSingleton
  implicit val singleton = ExplicitResultTypesSingleton2.Singleton
}
object ExplicitResultTypesSingleton2 {
  object Singleton
}
