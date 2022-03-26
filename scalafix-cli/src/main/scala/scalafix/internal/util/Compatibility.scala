package scalafix.internal.util

sealed trait Compatibility

object Compatibility {

  case object Compatible extends Compatibility
  case object Unknown extends Compatibility
  case object Incompatible extends Compatibility
  case class Temptative(compatibleRunWith: String) extends Compatibility

  private val Snapshot = """.*-SNAPSHOT""".r
  private val XYZ = """^([0-9]+)\.([0-9]+)\.([0-9]+)""".r

  def earlySemver(builtAgainst: String, runWith: String): Compatibility = {
    (builtAgainst, runWith) match {
      case (Snapshot(), _) => Unknown
      case (_, Snapshot()) => Unknown
      case (XYZ(bX, _, _), XYZ(rX, _, _)) if bX.toInt > rX.toInt =>
        Incompatible
      case (XYZ("0", bY, bZ), XYZ(rX, _, _)) if 0 < rX.toInt =>
        Temptative(s"0.$bY.x" + (if (bZ.toInt > 0) s" (x>=$bZ)" else ""))
      case (XYZ(bX, bY, _), XYZ(rX, _, _)) if bX.toInt < rX.toInt =>
        Temptative(s"$bX.x" + (if (bY.toInt > 0) s" (x>=$bY)" else ""))
      // --- X match given the cases above
      case (XYZ(_, bY, _), XYZ(_, rY, _)) if bY.toInt > rY.toInt =>
        Incompatible
      case (XYZ("0", bY, bZ), XYZ("0", rY, _)) if bY.toInt < rY.toInt =>
        Temptative(s"0.$bY.x" + (if (bZ.toInt > 0) s" (x>=$bZ)" else ""))
      // --- X.Y match given the cases above
      case (XYZ("0", _, bZ), XYZ("0", _, rZ)) if bZ.toInt > rZ.toInt =>
        Incompatible
      case _ => Compatible
    }
  }
}
