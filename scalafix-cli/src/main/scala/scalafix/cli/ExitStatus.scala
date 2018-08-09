package scalafix.cli

import scala.collection.mutable

sealed abstract case class ExitStatus(code: Int, name: String) {
  def isOk: Boolean = code == ExitStatus.Ok.code
  def is(exit: ExitStatus): Boolean = (code & exit.code) != 0
  override def toString: String = s"$name=$code"
}

object ExitStatus {
  // NOTE: ExitCode resembles an Enumeration very much, but has minor differences
  // for example how the name is calculated for merged exit codes.
  private var counter = 0
  private val allInternal = mutable.ListBuffer.empty[ExitStatus]
  private val cache =
    new java.util.concurrent.ConcurrentHashMap[Int, ExitStatus]
  private def generateExitStatus(implicit name: sourcecode.Name) = {
    val code = counter
    counter = if (counter == 0) 1 else counter << 1
    val result = new ExitStatus(code, name.value) {}
    allInternal += result
    result
  }
  // see https://github.com/scalameta/scalafmt/issues/941
  // format: off
  val Ok,
      UnexpectedError,
      ParseError,
      CommandLineError,
      MissingSemanticdbError,
      StaleSemanticdbError,
      TestError,
      LinterError,
      NoFilesError
    : ExitStatus = generateExitStatus
  // format: on
  lazy val all: List[ExitStatus] = allInternal.toList
  private def codeToName(code: Int): String = {
    if (code == 0) Ok.name
    else {
      val names = all.collect {
        case exit if (exit.code & code) != 0 => exit.name
      }
      names.mkString("+")
    }
  }
  def apply(code: Int): ExitStatus = {
    if (cache.contains(code)) cache.get(code)
    else {
      val result = new ExitStatus(code, codeToName(code)) {}
      cache.put(code, result)
      result
    }
  }

  def merge(exit1: ExitStatus, exit2: ExitStatus): ExitStatus =
    apply(exit1.code | exit2.code)
}
