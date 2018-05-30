package scalafix.v1

import scala.util.control.NoStackTrace
import scalafix.cli.ExitStatus

final class NonZeroExitCode(code: ExitStatus)
    extends Exception(code.toString)
    with NoStackTrace
