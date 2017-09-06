package test.explicitReturnTypes

object ExplicitResultTypesSingleton {
  implicit val default: _root_.test.explicitReturnTypes.ExplicitResultTypesSingleton.type = ExplicitResultTypesSingleton
  implicit val singleton: _root_.test.explicitReturnTypes.ExplicitResultTypesSingleton2.Singleton.type = ExplicitResultTypesSingleton2.Singleton
}
object ExplicitResultTypesSingleton2 {
  object Singleton
}
