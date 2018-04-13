package scalafix.internal.util

object Shorten {

  /** Fully quality up to and including _root_ package */
  case object FullyQualified extends Shorten

  /** Optimize for human-readability
    *
    * In general, tries to quality up to the closest enclosing package but with special handling in
    * a couple of other cases like type aliases inside of objects.
    */
  case object Readable extends Shorten

  /** Discard prefix and use short name only */
  case object NameOnly extends Shorten

}

sealed abstract class Shorten {
  def isReadable: Boolean = this == Shorten.Readable
  def isFullyQualified: Boolean = this == Shorten.FullyQualified
  def isNameOnly: Boolean = this == Shorten.NameOnly
}