package scalafix.tests.core

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import scala.meta._
import scala.meta.internal.io.PathIO
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.v0.LegacyInMemorySemanticdbIndex
import scalafix.syntax._
import scalafix.testkit.DiffAssertions
import scalafix.tests.BuildInfo
import scalafix.v1.Doc
import scalafix.v1.SemanticDoc

object BaseSemanticSuite {
  def loadDoc(filename: String): SemanticDoc = {
    val root = AbsolutePath(BuildInfo.sharedSourceroot).resolve("scala/test")
    val abspath = root.resolve(filename)
    val relpath = abspath.toRelative(AbsolutePath(BuildInfo.baseDirectory))
    val input = Input.File(abspath)
    val doc = Doc.fromInput(input)
    val classpath =
      Classpaths.withDirectory(AbsolutePath(BuildInfo.sharedClasspath))
    val symtab = ClasspathOps.newSymbolTable(classpath).get
    SemanticDoc.fromPath(doc, relpath, ClasspathOps.thisClassLoader, symtab)
  }
}

abstract class BaseSemanticSuite(filename: String)
    extends FunSuite
    with BeforeAndAfterAll
    with DiffAssertions {
  var _db: LegacyInMemorySemanticdbIndex = _
  var _input: Input = _
  implicit def index: LegacyInMemorySemanticdbIndex = _db
  def input: Input = _input
  def source: Source = {
    input.parse[Source].get
  }

  override def beforeAll(): Unit = {
    val dir = AbsolutePath(scalafix.tests.BuildInfo.sharedClasspath)
    _db = LegacyInMemorySemanticdbIndex.load(
      Classpaths.withDirectory(dir),
      PathIO.workingDirectory
    )
    _input = _db.inputs
      .collectFirst {
        case i if i.label.contains(filename) =>
          i
      }
      .getOrElse {
        throw new IllegalArgumentException(
          s"No $filename.semanticdb file found!"
        )
      }
  }
}
