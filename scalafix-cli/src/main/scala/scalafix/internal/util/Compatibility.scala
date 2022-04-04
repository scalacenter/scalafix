package scalafix.internal.util

import scala.util.matching.Regex
sealed trait Compatibility

object Compatibility {

  case object Compatible extends Compatibility
  case object Unknown extends Compatibility
  case class TemptativeUp(compatibleRunWith: String) extends Compatibility
  case class TemptativeDown(compatibleRunWith: String) extends Compatibility

  private val Snapshot = """.*-SNAPSHOT""".r
  val XYZ: Regex = """^([0-9]+)\.([0-9]+)\.([0-9]+).*""".r

  def earlySemver(builtAgainst: String, runWith: String): Compatibility = {
    (builtAgainst, runWith) match {
      case (Snapshot(), _) => Unknown
      case (_, Snapshot()) => Unknown
      case (XYZ(bX, bY, _), XYZ(rX, _, _)) if bX.toInt > rX.toInt =>
        TemptativeUp(s"$bX.x" + (if (bY.toInt > 0) s" (x>=$bY)" else ""))
      case (XYZ("0", bY, bZ), XYZ(rX, _, _)) if 0 < rX.toInt =>
        TemptativeDown(s"0.$bY.x" + (if (bZ.toInt > 0) s" (x>=$bZ)" else ""))
      case (XYZ(bX, bY, _), XYZ(rX, _, _)) if bX.toInt < rX.toInt =>
        TemptativeDown(s"$bX.x" + (if (bY.toInt > 0) s" (x>=$bY)" else ""))
      // --- X match given the cases above
      case (XYZ(bX, bY, bZ), XYZ(_, rY, _)) if bY.toInt > rY.toInt =>
        TemptativeUp(s"$bX.$bY.x" + (if (bZ.toInt > 0) s" (x>=$bZ)" else ""))
      case (XYZ("0", bY, bZ), XYZ("0", rY, _)) if bY.toInt < rY.toInt =>
        TemptativeDown(s"0.$bY.x" + (if (bZ.toInt > 0) s" (x>=$bZ)" else ""))
      // --- X.Y match given the cases above
      case (XYZ("0", bY, bZ), XYZ("0", _, rZ)) if bZ.toInt > rZ.toInt =>
        TemptativeUp(s"0.$bY.x (x>=$bZ)")
      case _ => Compatible
    }
  }
}
