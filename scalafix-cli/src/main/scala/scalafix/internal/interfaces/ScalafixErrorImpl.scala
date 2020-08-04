package scalafix.internal.interfaces
import scalafix.cli.ExitStatus
import scalafix.interfaces.ScalafixError

object ScalafixErrorImpl {
  private lazy val statusToError: Map[ExitStatus, ScalafixError] = {
    val ok :: from = ExitStatus.all
    assert(ok.isOk)
    val to = ScalafixError.values().toList
    assert(from.length == to.length, s"$from != $to")
    val map = from.zip(to).toMap
    map.foreach {
      case (key, value) =>
        assert(
          key.name.toLowerCase() == value.toString.toLowerCase,
          s"$key != $value"
        )
    }
    map
  }

  def fromScala(exit: ExitStatus): Array[ScalafixError] = {
    val buf = Array.newBuilder[ScalafixError]
    ExitStatus.all.foreach { code =>
      if (exit.is(code))
        buf += statusToError(code)
    }
    buf.result()
  }
}
