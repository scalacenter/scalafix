package scalafix.tests.cli

// import bloop.launcher.LauncherMain
// import java.nio.charset.StandardCharsets
// import bloop.launcher.core.Shell
// import scala.concurrent.Promise
// import bloop.launcher.core.Installer
// import ch.epfl.scala.bsp4j.BuildClient
// import ch.epfl.scala.bsp4j.LogMessageParams
// import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
// import ch.epfl.scala.bsp4j.ShowMessageParams
// import ch.epfl.scala.bsp4j.DidChangeBuildTarget
// import ch.epfl.scala.bsp4j.TaskProgressParams
// import ch.epfl.scala.bsp4j.TaskFinishParams
// import ch.epfl.scala.bsp4j.BuildServer
// import ch.epfl.scala.bsp4j.TaskStartParams
// import org.eclipse.lsp4j.jsonrpc.Launcher
// import java.io.PipedOutputStream
// import java.io.PipedInputStream
// import bloop.launcher.LauncherStatus.FailedToConnectToServer
// import bloop.launcher.LauncherStatus.FailedToInstallBloop
// import bloop.launcher.LauncherStatus.FailedToOpenBspConnection
// import bloop.launcher.LauncherStatus.FailedToParseArguments
// import bloop.launcher.LauncherStatus.SuccessfulRun
// import scala.compat.java8.FutureConverters._
// import scala.concurrent.ExecutionContext.Implicits.global
// import ch.epfl.scala.bsp4j.InitializeBuildParams
// import scala.meta.io.AbsolutePath
// import ch.epfl.scala.bsp4j.BuildClientCapabilities
// import scala.jdk.CollectionConverters._
// import scala.concurrent.Future
// import ch.epfl.scala.bsp4j.BuildTarget
// import ch.epfl.scala.bsp4j.ScalaBuildServer
// import ch.epfl.scala.bsp4j.ScalacOptionsParams
// import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
// import ch.epfl.scala.bsp4j.ScalacOptionsResult
// import ch.epfl.scala.bsp4j.ScalacOptionsItem
// import scalafix.interfaces.Scalafix
// import java.nio.file.Paths
// import java.net.URI
// import ch.epfl.scala.bsp4j.SourcesResult
// import java.nio.file.Path
// import java.{util => ju}
// import scalafix.interfaces.ScalafixMainMode
// import ch.epfl.scala.bsp4j.SourceItemKind.FILE
// import ch.epfl.scala.bsp4j.SourceItemKind.DIRECTORY
// import java.nio.file.Files
// import java.util.stream.Collectors
// import ch.epfl.scala.bsp4j.SourcesParams
// import scala.concurrent.Await
// import scala.concurrent.duration.Duration
// import ch.epfl.scala.bsp4j.BuildTargetIdentifier
// import ch.epfl.scala.bsp4j.CompileParams
// import ch.epfl.scala.bsp4j.StatusCode

// trait BloopBuildServer extends BuildServer with ScalaBuildServer
// class BspSuite extends BaseCliSuite {
//   // def scalafix(item: ScalacOptionsItem, sources: ju.List[Path]): Unit = {
//   //   val params = Scalafix.classloadInstance(this.getClass().getClassLoader())
//   //   val classpath =
//   //     item
//   //       .getClasspath()
//   //       .asScala
//   //       .map(uri => Paths.get(URI.create(uri)))
//   //       .asJava
//   //   val args = params
//   //     .newArguments()
//   //     .withClasspath(classpath)
//   //     .withPaths(sources)
//   //     .withRules(List("ExplicitResultTypes").asJava)
//   //   // .withParsedArguments(List("--stdout").asJava)
//   //   // .withMode()
//   //   val exit = args.run()
//   //   pprint.log(exit)
//   // }
//   // def scalafix(
//   //     targets: ju.List[BuildTargetIdentifier],
//   //     options: ScalacOptionsResult,
//   //     sources: SourcesResult
//   // ): Unit = {
//   //   val info = options.getItems().asScala.map(i => i.getTarget() -> i).toMap
//   //   val paths = sources.getItems().asScala.map(i => i.getTarget() -> i).toMap
//   //   for {
//   //     target <- targets.asScala
//   //     scalac <- info.get(target)
//   //     p <- paths.get(target)
//   //   } {
//   //     val ps = p.getSources().asScala.flatMap { item =>
//   //       val path = Paths.get(URI.create(item.getUri()))
//   //       item.getKind() match {
//   //         case FILE => List(path)
//   //         case DIRECTORY =>
//   //           Files
//   //             .walk(path)
//   //             .collect(Collectors.toList())
//   //             .asScala
//   //       }
//   //     }
//   //     val abspaths = ps
//   //       .map(_.toAbsolutePath().normalize())
//   //       .filter(
//   //         p =>
//   //           Files
//   //             .isRegularFile(p) && p.getFileName().toString().endsWith(".scala")
//   //       )
//   //     pprint.log(abspaths)
//   //     if (abspaths.nonEmpty) {
//   //       scalafix(scalac, abspaths.asJava)
//   //     }
//   //   }
//   // }
//   // test("hello") {
//   //   val x = 42
//   //   val client = new BuildClient {
//   //     def onBuildLogMessage(params: LogMessageParams): Unit = ()
//   //     def onBuildPublishDiagnostics(params: PublishDiagnosticsParams): Unit = {
//   //       pprint.log(params)
//   //     }
//   //     def onBuildShowMessage(params: ShowMessageParams): Unit = ()
//   //     def onBuildTargetDidChange(params: DidChangeBuildTarget): Unit = ()
//   //     def onBuildTaskProgress(params: TaskProgressParams): Unit = ()
//   //     def onBuildTaskFinish(params: TaskFinishParams): Unit = ()
//   //     def onBuildTaskStart(params: TaskStartParams): Unit = ()
//   //   }
//   //   val out1 = new PipedOutputStream()
//   //   val out2 = new PipedOutputStream()
//   //   val in1 = new PipedInputStream(out2)
//   //   val in2 = new PipedInputStream(out1)
//   //   val launcher = new LauncherMain(
//   //     in2,
//   //     out2,
//   //     System.err,
//   //     StandardCharsets.UTF_8,
//   //     Shell.default,
//   //     nailgunHost = None,
//   //     nailgunPort = None,
//   //     Promise[Unit]()
//   //   )
//   //   Future {
//   //     val status = launcher.cli(Array("1.3.2"))
//   //     pprint.log(status)
//   //     status match {
//   //       case FailedToConnectToServer =>
//   //       case FailedToInstallBloop =>
//   //       case FailedToOpenBspConnection =>
//   //       case FailedToParseArguments =>
//   //       case SuccessfulRun =>
//   //     }
//   //   }
//   //   val builder = new Launcher.Builder[BloopBuildServer]()
//   //     .setInput(in1)
//   //     .setOutput(out1)
//   //     .setLocalService(client)
//   //     .setRemoteInterface(classOf[BloopBuildServer])
//   //     .create()
//   //   val server = builder.getRemoteProxy()
//   //   val listening = builder.startListening()
//   //   val source = AbsolutePath("/Users/lgeirsson/workspace/source")
//   //   val capabilities = new BuildClientCapabilities(List("scala").asJava)
//   //   val params = new InitializeBuildParams(
//   //     "Scalafix",
//   //     "1.0.0",
//   //     "2.0.0",
//   //     source.toURI.toString,
//   //     capabilities
//   //   )
//   //   pprint.log("starting")
//   //   val fut = for {
//   //     a <- server.buildInitialize(params).toScala
//   //     _ = server.onBuildInitialized()
//   //     _ = Thread.sleep(10)
//   //     // targets <- server.workspaceBuildTargets().toScala
//   //     targetIds = List(
//   //       new BuildTargetIdentifier(
//   //         "sandbox/users/dwagnerhall/scala/src/dwagnerhall:hello"
//   //       )
//   //     ).asJava
//   //     compile <- server.buildTargetCompile(new CompileParams(targetIds)).toScala
//   //     _ = pprint.log(compile)
//   //     if compile.getStatusCode() == StatusCode.OK
//   //     // targets.getTargets().asScala.map(_.getId()).asJava
//   //     scalac <- server
//   //       .buildTargetScalacOptions(new ScalacOptionsParams(targetIds))
//   //       .toScala
//   //     sources <- server.buildTargetSources(new SourcesParams(targetIds)).toScala
//   //     _ = scalafix(targetIds, scalac, sources)
//   //     a <- server.buildShutdown().toScala
//   //     _ = pprint.log("shutdown")
//   //     _ = server.onBuildExit()
//   //   } yield {
//   //     pprint.log("exit")
//   //   }
//   //   Await.result(fut, Duration("30s"))
//   // }
// }
