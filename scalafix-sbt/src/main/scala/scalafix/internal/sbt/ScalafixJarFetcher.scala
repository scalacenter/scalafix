package scalafix.internal.sbt

import java.io.File
import java.io.OutputStreamWriter
import coursier.MavenRepository
import scala.concurrent.duration.Duration

private[scalafix] object ScalafixJarFetcher {
  private val SonatypeSnapshots: MavenRepository =
    MavenRepository("https://oss.sonatype.org/content/repositories/snapshots")
  private val MavenCentral: MavenRepository =
    MavenRepository("https://repo1.maven.org/maven2")

  def fetchJars(org: String, artifact: String, version: String): List[File] =
    this.synchronized {
      import coursier._
      val start = Resolution(Set(Dependency(Module(org, artifact), version)))
      val repositories: List[Repository] = List(
        Some(Cache.ivy2Local),
        Some(MavenCentral),
        if (version.endsWith("-SNAPSHOT")) Some(SonatypeSnapshots)
        else None
      ).flatten

      val logger = new TermDisplay(new OutputStreamWriter(System.err), true)
      logger.init()
      val fetch = Fetch.from(
        repositories,
        Cache.fetch(
          logger = Some(logger),
          ttl = Cache.defaultTtl.orElse(Some(Duration.Inf))
        )
      )
      val resolution = start.process.run(fetch).unsafePerformSync
      val errors = resolution.metadataErrors
      if (errors.nonEmpty) {
        sys.error(errors.mkString("\n"))
      }
      val localArtifacts = scalaz.concurrent.Task
        .gatherUnordered(
          resolution.artifacts.map(Cache.file(_).run)
        )
        .unsafePerformSync
        .map(_.toEither)
      val failures = localArtifacts.collect { case Left(e) => e }
      if (failures.nonEmpty) {
        sys.error(failures.mkString("\n"))
      } else {
        val jars = localArtifacts.collect {
          case Right(file) if file.getName.endsWith(".jar") =>
            file
        }
        jars
      }
    }

}
