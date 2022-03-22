package scalafix.internal.util

sealed trait Compatibility

object Compatibility {

  case object Compatible extends Compatibility
  case object Unknown extends Compatibility
  case object Incompatible extends Compatibility

  private val Snapshot = """.*-SNAPSHOT""".r
  private val XYZ = """^([0-9]+)\.([0-9]+)\.([0-9]+)""".r

  def earlySemver(builtAgainst: String, runWith: String): Compatibility = {
    (builtAgainst, runWith) match {
      case (Snapshot(), _) => Unknown
      case (_, Snapshot()) => Unknown
      case (XYZ(bX, _, _), XYZ(rX, _, _)) if bX.toInt > rX.toInt =>
        Incompatible
      case (XYZ(bX, _, _), XYZ(rX, _, _)) if bX.toInt < rX.toInt =>
        Unknown
      // --- X match given the cases above
      case (XYZ(_, bY, _), XYZ(_, rY, _)) if bY.toInt > rY.toInt =>
        Incompatible
      case (XYZ("0", bY, _), XYZ("0", rY, _)) if bY.toInt < rY.toInt =>
        Unknown
      case (XYZ(_, bY, _), XYZ(_, rY, _)) if bY.toInt < rY.toInt =>
        Compatible
      // --- X.Y match given the cases above
      case (XYZ("0", _, bZ), XYZ("0", _, rZ)) if bZ.toInt > rZ.toInt =>
        Incompatible
      case _ => Compatible
    }
  }
}
