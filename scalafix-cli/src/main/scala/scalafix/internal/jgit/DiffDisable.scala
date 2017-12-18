package scalafix.internal.jgit

import metaconfig.Configured

import java.nio.file.Path

import scala.meta.inputs.Input
import scala.meta.Position

import scala.collection.mutable.StringBuilder

import scalafix.internal.util.IntervalSet
import scalafix.LintMessage

object DiffDisable {
  def apply(workingDir: Path, diffBase: String): Configured[DiffDisable] = {
    JGitDiff(workingDir, diffBase).map(diffs => new DiffDisable(diffs))
  }
}

class DiffDisable(diffs: List[GitDiff]) {
  private val newFiles: Set[Input] = diffs.collect {
    case NewFile(path) => Input.File(path)
  }.toSet

  private val modifiedFiles: Map[Input, IntervalSet] = diffs.collect {
    case ModifiedFile(path, changes) => {
      val ranges = changes.map {
        case GitChange(start, end) => (start, end)
      }
      Input.File(path) -> IntervalSet(ranges)
    }
  }.toMap

  def isDisabled(file: Input): Boolean =
    !(newFiles.contains(file) || modifiedFiles.contains(file))

  def isEnabled(position: Position): Boolean = {
    def isAddition: Boolean =
      newFiles.contains(position.input)

    def isModification: Boolean = {
      val startLine = position.startLine
      val endLine = position.endLine
      modifiedFiles
        .get(position.input)
        .fold(false)(
          interval =>
            interval.intersects(
              startLine,
              endLine
          )
        )
    }

    isAddition || isModification
  }

  override def toString: String = {

    val b = new StringBuilder()
    def add(in: String): Unit =
      b ++= in + "\n"

    add("== New Files ==")
    diffs.foreach {
      case NewFile(path) => add(path.toString)
      case _ => ()
    }

    add("== Modified Files ==")
    diffs.foreach {
      case ModifiedFile(path, changes) => {
        add(path.toString)
        changes.foreach {
          case GitChange(start, end) => add(s"  [$start, $end]")
        }
      }
      case _ => ()
    }
    b.result()
  }
}
