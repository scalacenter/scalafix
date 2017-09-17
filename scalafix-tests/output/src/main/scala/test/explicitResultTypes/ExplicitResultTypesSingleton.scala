package test.explicitResultTypes

object ExplicitResultTypesSingleton {
  implicit val default: _root_.test.explicitResultTypes.ExplicitResultTypesSingleton.type = ExplicitResultTypesSingleton
  implicit val singleton: _root_.test.explicitResultTypes.ExplicitResultTypesSingleton2.Singleton.type = ExplicitResultTypesSingleton2.Singleton
}
object ExplicitResultTypesSingleton2 {
  object Singleton
}
