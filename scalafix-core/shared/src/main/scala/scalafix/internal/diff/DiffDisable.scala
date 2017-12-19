package scalafix.internal.diff

import scala.meta.inputs.Input
import scala.meta.Position

import scala.collection.mutable.StringBuilder

import scalafix.internal.util.IntervalSet

object DiffDisable {
  def empty: DiffDisable = EmptyDiff
  def apply(diffs: List[GitDiff]): DiffDisable = new FullDiffDisable(diffs)
}

sealed trait DiffDisable {
  def isDisabled(position: Position): Boolean
  def isDisabled(file: Input): Boolean
}

private object EmptyDiff extends DiffDisable {
  def isDisabled(position: Position): Boolean = false
  def isDisabled(file: Input): Boolean = false
}

private class FullDiffDisable(diffs: List[GitDiff]) extends DiffDisable {
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

  def isDisabled(position: Position): Boolean = {
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

    !(isAddition || isModification)
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
