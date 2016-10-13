package scala.meta

object Mirror {
  def apply(artifacts: Artifact*)(implicit resolver: Resolver): Mirror = {
    new Mirror {
      lazy val domain = Domain(artifacts: _*)(resolver)
      override def toString =
        s"""Context(${artifacts.mkString(", ")})($resolver)"""
    }
  }
}
