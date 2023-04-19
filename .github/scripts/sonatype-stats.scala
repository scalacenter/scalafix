package stats

import java.nio.file._
import java.time.{YearMonth, ZoneOffset}

import com.softwaremill.sttp.quick._
import upickle.default._
import ujson.{read => _, _}

object Responses {

  case class UniqueIpData(total: Int)
  implicit val uniqueIpDataRW: ReadWriter[UniqueIpData] = macroRW
  case class UniqueIpResp(data: UniqueIpData)
  implicit val uniqueIpRespRW: ReadWriter[UniqueIpResp] = macroRW

  case class Elem(id: String, name: String)
  implicit val elemRW: ReadWriter[Elem] = macroRW

}

import Responses._

object Params {

  // organization one was granted write access to
  val proj = sys.env.getOrElse("SONATYPE_PROJECT", "ch.epfl.scala")
  // actual organization used for publishing (must have proj as prefix)
  val organization = sys.env.getOrElse("SONATYPE_PROJECT", proj)

  val sonatypeUser = sys.env.getOrElse(
    "SONATYPE_USERNAME",
    sys.error("SONATYPE_USERNAME not set")
  )
  val sonatypePassword: String = sys.env.getOrElse(
    "SONATYPE_PASSWORD",
    sys.error("SONATYPE_PASSWORD not set")
  )

  val start = YearMonth.now(ZoneOffset.UTC)

  val cutOff = start.minusMonths(4L)

  // Note: this assumes the current working directory is the repository root directory!
  val base = Paths.get("sonatype-stats")

  val artifacts = Set(
    "scalafix-core_2.12",
    "scalafix-core_2.13"
  )
}

case class Data(
  base: Path,
  ext: String,
  empty: String => Boolean,
  name: String,
  tpe: String,
  projId: String,
  organization: String,
  artifact: Option[String]
) {

  def fileFor(monthYear: YearMonth): Path = {
    val year = monthYear.getYear
    val month = monthYear.getMonth.getValue
    base.resolve(f"$year%04d/$month%02d.$ext")
  }

  def exists(monthYear: YearMonth): Boolean =
    Files.isRegularFile(fileFor(monthYear))

  def write(monthYear: YearMonth, content: String): Unit = {
    System.err.println(s"Writing $monthYear (${content.length} B)")
    val f = fileFor(monthYear)
    Files.createDirectories(f.getParent)
    Files.write(f, content.getBytes("UTF-8"))
  }

  def urlFor(monthYear: YearMonth) = {
    val year = monthYear.getYear
    val month = monthYear.getMonth.getValue

    uri"https://oss.sonatype.org/service/local/stats/$name?p=$projId&g=$organization&a=${artifact.getOrElse("")}&t=$tpe&from=${f"$year%04d$month%02d"}&nom=1"
  }

  def process(monthYears: Iterator[YearMonth]): Iterator[(YearMonth, Boolean)] =
    monthYears
      .filter { monthYear =>
        !exists(monthYear)
      }
      .map { monthYear =>

        val u = urlFor(monthYear)

        System.err.println(s"Getting $monthYear: $u")

        val statResp = sttp
          .auth.basic(Params.sonatypeUser, Params.sonatypePassword)
          .header("Accept", "application/json")
          .get(u)
          .send()

        if (!statResp.isSuccess)
          sys.error("Error getting project stats: " + statResp.statusText)

        val stats = statResp.body.right.get.trim

        val empty0 = empty(stats)
        if (empty0)
          System.err.println(s"Empty response at $monthYear")
        else
          write(monthYear, stats)

        monthYear -> !empty0
      }
}

object SonatypeStats {

  def collect(): Unit = {
    val projId: String = {
      val projectIds: Map[String, String] = {
        val projResp = sttp
          .auth.basic(Params.sonatypeUser, Params.sonatypePassword)
          .header("Accept", "application/json")
          .get(uri"https://oss.sonatype.org/service/local/stats/projects")
          .send()

        if (!projResp.isSuccess)
          sys.error("Error getting project list: " + projResp.statusText)

        val respJson = ujson.read(projResp.body.right.get)

        read[Seq[Elem]](respJson("data"))
          .map(e => e.name -> e.id)
          .toMap
      }

      projectIds(Params.proj)
    }

    val artifactStatsPerVersion = Params.artifacts.flatMap { artifact =>
      Seq(
        Data(
          Params.base.resolve("per-version-unique-ips").resolve(artifact),
          "csv",
          _.isEmpty,
          "slices_csv",
          "ip",
          projId,
          Params.organization,
          artifact = Some(artifact)
        ),
        Data(
          Params.base.resolve("per-version-stats").resolve(artifact),
          "csv",
          _.isEmpty,
          "slices_csv",
          "raw",
          projId,
          Params.organization,
          artifact = Some(artifact)
        )
      )
    }

    for (data <- artifactStatsPerVersion) {
      val it = Iterator.iterate(Params.start)(_.minusMonths(1L))
      val processed = data.process(it)
        .takeWhile {
          case (monthYear, nonEmpty) =>
            nonEmpty || monthYear.compareTo(Params.cutOff) >= 0
        }
        .length

      System.err.println(s"Processed $processed months in ${data.base} for type ${data.tpe}")
    }
  }

}
