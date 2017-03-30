package scalafix.cli

case class ExitStatus(code: Int, name: String) {
  override def toString: String = s"$name=$code"
}

object ExitStatus {
  def apply(n: Int)(implicit name: sourcecode.Name) =
    new ExitStatus(n, name.value)
  val Ok: ExitStatus = apply(0)
  val UnexpectedError: ExitStatus = apply(1)
  val ParseError: ExitStatus = apply(2)
  val ScalafixError: ExitStatus = apply(3)

  def all: Seq[ExitStatus] = Seq(
    Ok,
    UnexpectedError,
    ParseError,
    ScalafixError
  )

  def merge(code1: ExitStatus, code2: ExitStatus): ExitStatus =
    if (code1 == Ok) code2 else code1
}
