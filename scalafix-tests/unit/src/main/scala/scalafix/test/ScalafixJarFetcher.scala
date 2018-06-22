package scalafix.internal.sbt

import java.io.File
import java.io.OutputStreamWriter
import coursier.MavenRepository
import coursier.util.Gather
import coursier.util.Task
import scala.concurrent.ExecutionContext.Implicits.global

private[scalafix] object ScalafixJarFetcher {
  private val SonatypeSnapshots: MavenRepository =
    MavenRepository("https://oss.sonatype.org/content/repositories/snapshots")
  private val MavenCentral: MavenRepository =
    MavenRepository("https://repo1.maven.org/maven2")

  def fetchJars(org: String, artifact: String, version: String): List[File] =
    this.synchronized {
      import coursier._
      val res = Resolution(Set(Dependency(Module(org, artifact), version)))
      val repositories: List[Repository] = List(
        Some(Cache.ivy2Local),
        Some(MavenCentral),
        if (version.endsWith("-SNAPSHOT")) Some(SonatypeSnapshots)
        else None
      ).flatten

      val term = new TermDisplay(new OutputStreamWriter(System.err), true)
      term.init()
      val fetch =
        Fetch.from(repositories, Cache.fetch[Task](logger = Some(term)))
      val resolution = res.process.run(fetch).unsafeRun()
      val errors = resolution.errors
      if (errors.nonEmpty) {
        sys.error(errors.mkString("\n"))
      }
      val localArtifacts = Gather[Task]
        .gather(
          resolution.artifacts.map(artifact => Cache.file[Task](artifact).run)
        )
        .unsafeRun()
      val jars = localArtifacts.flatMap {
        case Left(e) =>
          throw new IllegalArgumentException(e.describe)
        case Right(jar) if jar.getName.endsWith(".jar") =>
          jar :: Nil
        case _ =>
          Nil
      }
      term.stop()
      jars.toList
    }

}
