/*
rewrites = ExplicitReturnTypes
 */
package test.explicitReturnTypes

object ExplicitReturnTypesSingleton {
  implicit val default = ExplicitReturnTypesSingleton
  implicit val singleton = ExplicitReturnTypesSingleton2.Singleton
}
object ExplicitReturnTypesSingleton2 {
  object Singleton
}
