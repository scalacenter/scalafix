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
  def isEmpty: Boolean
}

object EmptyDiff extends DiffDisable {
  override def isEmpty: Boolean = true
  def isDisabled(position: Position): Boolean = false
  def isDisabled(file: Input): Boolean = false
}

class FullDiffDisable(diffs: List[GitDiff]) extends DiffDisable {
  override def isEmpty: Boolean = diffs.isEmpty
  private val newFiles: Set[String] = diffs.collect {
    case NewFile(path) => path.toAbsolutePath.toString
  }.toSet

  private val modifiedFiles: Map[String, IntervalSet] = diffs.collect {
    case ModifiedFile(path, changes) => {
      val ranges = changes.map {
        case GitChange(start, end) => (start, end)
      }
      path.toAbsolutePath.toString -> IntervalSet(ranges)
    }
  }.toMap

  def isDisabled(file: Input): Boolean =
    !(newFiles.contains(file.syntax) || modifiedFiles.contains(file.syntax))

  def isDisabled(position: Position): Boolean = {
    def isAddition: Boolean =
      newFiles.contains(position.input.syntax)

    def isModification: Boolean = {
      val startLine = position.startLine
      val endLine = position.endLine
      modifiedFiles
        .get(position.input.syntax)
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
