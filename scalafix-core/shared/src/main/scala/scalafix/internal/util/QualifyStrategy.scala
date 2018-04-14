package scalafix.internal.util

sealed abstract class QualifyStrategy {
  def isReadable: Boolean = this == QualifyStrategy.Readable
  def isFull: Boolean = this == QualifyStrategy.Full
  def isName: Boolean = this == QualifyStrategy.Name
}

object QualifyStrategy {

  /** Fully quality up to and including _root_ package */
  case object Full extends QualifyStrategy

  /** Optimize for human-readability
    *
    * In general, tries to quality up to the closest enclosing package but with special handling in
    * a couple of other cases like type aliases inside of objects.
    */
  case object Readable extends QualifyStrategy

  /** Discard prefix and use short name only */
  case object Name extends QualifyStrategy

}
