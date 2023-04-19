package plot

import java.io.File
import java.nio.file.{Files, Paths}
import java.time._

import com.twitter.algebird.Operators._
import plotly._
import plotly.element._
import plotly.layout._
import plotly.Plotly._
import upickle.default._
import ujson.{read => _, _}

import com.github.tototoshi.csv._

object Plot {
  def writePlots(): Unit = {

    object relevantVersion {
      val stableVersionRegex = "^(\\d+)\\.(\\d+)\\.(\\d+)$".r

      def unapply(version: String): Option[(Int, Int, Int)] =
        version match {
          case stableVersionRegex(majorStr, minorStr, patchStr) =>
            val major = majorStr.toInt
            val minor = minorStr.toInt
            val patch = patchStr.toInt
            if (major == 2) {
              if (minor >= 11) Some((major, minor, patch))
              else None
            } else Some((major, minor, patch))
          case _ => None
        }
    }

    def csvToBars(dir: File, allowedVersion: String => Boolean, filterOutMonths: Set[YearMonth] = Set()): Seq[Trace] = {

      val data = for {
        year <- 2015 to Year.now(ZoneOffset.UTC).getValue
        month <- 1 to 12
        f = new File(dir, f"$year/$month%02d.csv")
        if f.exists()
        ym = YearMonth.of(year, month)
        elem <- CSVReader.open(f)
          .iterator
          .map(l => (ym, /* version */ l(0), /* downloads */ l(1).toInt))
          .collect { case (date, version @ relevantVersion(major, minor, patch), downloads) if allowedVersion(version) =>
            (date, (major, minor, patch), downloads)
          }
          .toVector
      } yield elem

      data
        .groupBy { case (_, version, _) => version }
        .mapValues { stats =>
          stats
            .map { case (date, _, downloads) => (date, downloads) }
            .filterNot { case (date, _) => filterOutMonths(date) }
            .sortBy { case (date, _) => date }
        }
        .toSeq
        .sortBy { case (version, _) => version }
        .map { case ((major, minor, patch), stats) =>
          val x = stats.map(_._1).map { m =>
            plotly.element.LocalDateTime(m.getYear, m.getMonthValue, 1, 0, 0, 0)
          }
          val y = stats.map(_._2)
          Bar(x, y, name = s"${major}.${minor}.${patch}")
        }
    }

    val dataBase = stats.Params.base

    val htmlSnippets =
      for {
        artifact <- stats.Params.artifacts
        (baseDir, divId, title) <- Seq(
          ("per-version-stats", s"${artifact}-total", s"${artifact} (total downloads)"),
          ("per-version-unique-ips", s"${artifact}-unique", s"${artifact} (unique IPs)")
        )
        bars = csvToBars(dataBase.resolve(baseDir).resolve(artifact).toFile, _ => true /* keep all the versions */)
      } yield
        s"""
           |<h2 id="${divId}-plot">${title} <a href="#${divId}-plot">#</a></h2>
           |<div id="${divId}"></div>
           |<script>${Plotly.jsSnippet(divId, bars, Layout(barmode = BarMode.Stack))}</script>
           |""".stripMargin

    val html =
      s"""<!DOCTYPE html>
         |<html>
         |<head>
         |<title>Scalafix Statistics</title>
         |<script src="https://cdn.plot.ly/plotly-${Plotly.plotlyVersion}.min.js"></script>
         |</head>
         |<body>
         |<h1>Scalafix Statistics</h1>
         |${htmlSnippets.mkString}
         |</body>
         |</html>
         |""".stripMargin

    Files.createDirectories(dataBase)
    Files.write(dataBase.resolve("index.html"), html.getBytes("UTF-8"))

  }
}
