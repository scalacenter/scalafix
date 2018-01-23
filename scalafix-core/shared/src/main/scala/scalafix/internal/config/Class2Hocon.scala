package scalafix.internal.config

import metaconfig.HasFields

object String2AnyMap {
  def unapply(arg: Any): Option[Map[String, Any]] = arg match {
    case someMap: Map[_, _] =>
      try {
        Some(someMap.asInstanceOf[Map[String, Any]])
      } catch {
        case _: ClassCastException =>
          None
      }
    case _ => None
  }
}

object Class2Hocon {

  def apply(config: ScalafixConfig = ScalafixConfig()): String =
    toHocon(config).mkString("\n")

  private def toHocon(any: Any): Seq[String] = any match {
    case String2AnyMap(m) =>
      m.flatMap {
        case (k, v) =>
          toHocon(v).map { x =>
            if (x.startsWith(" ")) s"$k$x"
            else s"$k.$x"
          }
      }.toSeq
    case x: FilterMatcher =>
      toHocon(x.includeFilters.regex)
    case x: HasFields => toHocon(x.fields)
    case x: Traversable[_] =>
      if (x.isEmpty) Seq(" = []")
      else
        Seq(
          x.flatMap(toHocon)
            .map(_.stripPrefix(" = "))
            .mkString(" = [\n  ", "\n  ", "\n]"))
    case x: String =>
      Seq(s""""$x"""")
    case x =>
      val str = s"$x"
      val output =
        if (str.headOption.exists(!_.isLetterOrDigit)) s""""$str""""
        else str
      Seq(s" = $output")
  }
}
