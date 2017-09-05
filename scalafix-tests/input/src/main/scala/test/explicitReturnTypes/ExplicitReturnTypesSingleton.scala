/*
rewrites = ExplicitReturnTypes
explicitReturnTypes.unsafeShortenNames = true
 */
package test.explicitReturnTypes

object ExplicitReturnTypesSingleton {
  implicit val default = ExplicitReturnTypesSingleton
}
