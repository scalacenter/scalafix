package scalafix.cli

import scala.collection.mutable
import scala.util.control.NonFatal

import scala.meta.parsers.ParseException

import scalafix.internal.v1.MainOps.StaleSemanticDB
import scalafix.v1.SemanticDocument

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
  private def generateExitStatus(name: String) = {
    val code = counter
    counter = if (counter == 0) 1 else counter << 1
    val result = new ExitStatus(code, name) {}
    allInternal += result
    result
  }

  val Ok: ExitStatus =
    generateExitStatus("Ok")
  val UnexpectedError: ExitStatus =
    generateExitStatus("UnexpectedError")
  val ParseError: ExitStatus =
    generateExitStatus("ParseError")
  val CommandLineError: ExitStatus =
    generateExitStatus("CommandLineError")
  val MissingSemanticdbError: ExitStatus =
    generateExitStatus("MissingSemanticdbError")
  val StaleSemanticdbError: ExitStatus =
    generateExitStatus("StaleSemanticdbError")
  val TestError: ExitStatus =
    generateExitStatus("TestError")
  val LinterError: ExitStatus =
    generateExitStatus("LinterError")
  val NoFilesError: ExitStatus =
    generateExitStatus("NoFilesError")
  val NoRulesError: ExitStatus =
    generateExitStatus("NoRulesError")

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

  def merge(exitStatus: Seq[ExitStatus]): ExitStatus =
    exitStatus.foldLeft(ExitStatus.Ok) { (status, next) =>
      apply(status.code | next.code)
    }

  def from(throwable: Throwable): (ExitStatus) =
    throwable match {
      case _: ParseException =>
        ExitStatus.ParseError
      case _: SemanticDocument.Error.MissingSemanticdb =>
        ExitStatus.MissingSemanticdbError
      case _: StaleSemanticDB =>
        ExitStatus.StaleSemanticdbError
      case NonFatal(_) =>
        ExitStatus.UnexpectedError
    }
}
