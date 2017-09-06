package test.explicitReturnTypes

object ExplicitReturnTypesSingleton {
  implicit val default: _root_.test.explicitReturnTypes.ExplicitReturnTypesSingleton.type = ExplicitReturnTypesSingleton
  implicit val singleton: _root_.test.explicitReturnTypes.ExplicitReturnTypesSingleton2.Singleton.type = ExplicitReturnTypesSingleton2.Singleton
}
object ExplicitReturnTypesSingleton2 {
  object Singleton
}
