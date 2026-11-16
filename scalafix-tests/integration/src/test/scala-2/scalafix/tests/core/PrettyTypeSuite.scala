package scalafix.tests.core

import scala.collection.mutable
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import scala.{meta => m}

import scala.meta.cli.Reporter
import scala.meta.internal.classpath.ClasspathIndex
import scala.meta.internal.io.PathIO
import scala.meta.internal.io.PlatformFileIO
import scala.meta.internal.metacp._
import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.symtab._
import scala.meta.internal.{semanticdb => s}
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scala.meta.metacp.Settings

import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.symtab.SymbolTable
import scalafix.internal.util.PrettyType
import scalafix.internal.util.QualifyStrategy

class BasePrettyTypeSuite extends BaseSemanticSuite("TypeToTreeInput") {
  super.beforeAll()
  val classDir: m.AbsolutePath =
    m.AbsolutePath(scalafix.tests.BuildInfo.classDirectory)
  val semanticdbTargetRoots: List[AbsolutePath] =
    scalafix.tests.BuildInfo.semanticClasspath
      .map(m.AbsolutePath.apply)
      .toList

  val classpath: Classpath =
    Classpaths.withDirectories(semanticdbTargetRoots :+ classDir)

  // As of scalameta 4.5.3, this relies on scalap (and not on TASTy), so it
  // cannot work against classes compiled with Scala 3
  val table: SymbolTable =
    ClasspathOps.fromScalameta(GlobalSymbolTable(classpath, includeJdk = true))
}

class PrettyTypeSuite extends BasePrettyTypeSuite {

  val m.Source((pkg: m.Pkg) :: Nil) = source.transform {
    // Remove bodies from methods like `private def foo: Unit` that can't be abstract.
    case t: m.Defn.Def if t.decltpe.isDefined =>
      m.Decl.Def(t.mods, t.name, t.paramClauseGroups, t.decltpe.get)
  }

  // ignoring Functor[C[_]] because of a regression with scalac 2.13.7, see https://github.com/scalacenter/scalafix/pull/1493
  val filteredStats: List[m.Stat with m.Member] = pkg.body.stats.collect {
    case m: m.Member if m.name.value != "Functor" => m
  }

  filteredStats.collect { case expected: m.Member =>
    val name = expected.name.value
    test(s"${expected.productPrefix} - $name") {
      val suffix: String = expected match {
        case _: m.Defn.Object => s"$name."
        case _: m.Pkg.Object => s"$name/package."
        case _ => s"$name#"
      }
      val sym = s"test/$suffix"
      val info =
        table.info(sym).getOrElse(throw new NoSuchElementException(sym))
      val obtained =
        PrettyType
          .toTree(info, table, QualifyStrategy.Readable, fatalErrors = true)
          .tree
      val expectedSyntax = expected.syntax
      assertNoDiff(obtained.syntax, expectedSyntax)
    }
  }

  private def sinfo(
      symbol: String,
      kind: SymbolInformation.Kind,
      signature: s.Signature,
      properties: Int = 0
  ): SymbolInformation =
    SymbolInformation(
      symbol = symbol,
      kind = kind,
      properties = properties,
      displayName = symbol.desc.name.value,
      signature = signature
    )

  private def tableWith(infos: Seq[SymbolInformation]): SymbolTable = sym =>
    infos.find(_.symbol == sym).orElse(table.info(sym))

  /** Returns the syntax of `toTree` for `info`, with `params` in the table. */
  private def toTree(info: SymbolInformation, params: SymbolInformation*) =
    PrettyType
      .toTree(
        info,
        tableWith(info +: params),
        QualifyStrategy.Readable,
        fatalErrors = true
      )
      .tree
      .syntax

  /** Returns the syntax of `toType` for the singleton type of `info`. */
  private def toSingletonType(info: SymbolInformation) =
    PrettyType
      .toType(
        s.SingleType(s.NoType, info.symbol),
        tableWith(List(info)),
        QualifyStrategy.Readable,
        fatalErrors = true
      )
      .tree
      .syntax

  private def ref(symbol: String) = s.TypeRef(s.NoType, symbol, Nil)
  private val unit = ref("scala/Unit#")
  private def scope(infos: SymbolInformation*) = s.Scope(infos.map(_.symbol))

  // def getOrElse(k: Int)[V](d: Long): Unit
  private val getOrElse = "test/DB#getOrElse()."
  private val k =
    sinfo(
      s"$getOrElse(k)",
      SymbolInformation.Kind.PARAMETER,
      s.ValueSignature(ref("scala/Int#"))
    )

  test("method with one group without a type parameter clause") {
    // def getOrElse(k: Int): Unit
    def info(sig: s.Signature) =
      sinfo(getOrElse, SymbolInformation.Kind.METHOD, sig)
    val paramss = List(scope(k))
    assertNoDiff(
      toTree(info(s.MethodSignature(Some(s.Scope()), paramss, unit)), k),
      "def getOrElse(k: Int): Unit"
    )
    // dotc's form; TODO: `PrettyType` fails on it
    intercept[PrettyType#TypeToTreeError](
      toTree(info(s.MethodSignature(None, paramss, unit)), k)
    )
  }

  test("implicit and using parameter clauses") {
    // def f(x: Int)(implicit o: Long): Unit, and the same with `using`
    val f = "test/DB#f()."
    val x =
      sinfo(
        s"$f(x)",
        SymbolInformation.Kind.PARAMETER,
        s.ValueSignature(ref("scala/Int#"))
      )
    def obtained(properties: Int) = {
      val o = sinfo(
        s"$f(o)",
        SymbolInformation.Kind.PARAMETER,
        s.ValueSignature(ref("scala/Long#")),
        properties
      )
      val sig =
        s.MethodSignature(Some(s.Scope()), List(scope(x), scope(o)), unit)
      toTree(sinfo(f, SymbolInformation.Kind.METHOD, sig), x, o)
    }
    val using =
      SymbolInformation.Property.GIVEN.value | SymbolInformation.Property.IMPLICIT.value
    // TODO: `PrettyType` drops `implicit` or `using`
    assertNoDiff(
      obtained(SymbolInformation.Property.IMPLICIT.value),
      "def f(x: Int)(o: Long): Unit"
    )
    assertNoDiff(obtained(using), "def f(x: Int)(o: Long): Unit")
  }

  test("constructor with an implicit clause") {
    // class DB(x: Int)(implicit o: Long)
    val ctor = "test/DB#`<init>`()."
    val x =
      sinfo(
        s"$ctor(x)",
        SymbolInformation.Kind.PARAMETER,
        s.ValueSignature(ref("scala/Int#"))
      )
    val o = sinfo(
      s"$ctor(o)",
      SymbolInformation.Kind.PARAMETER,
      s.ValueSignature(ref("scala/Long#")),
      SymbolInformation.Property.IMPLICIT.value
    )
    val sig =
      s.MethodSignature(Some(s.Scope()), List(scope(x), scope(o)), s.NoType)
    // TODO: `PrettyType` drops `implicit` or `using`
    assertNoDiff(
      toTree(sinfo(ctor, SymbolInformation.Kind.CONSTRUCTOR, sig), x, o),
      "(x: Int)(o: Long)"
    )
  }

  test("dotc's form of a def and a val without clauses") {
    // def f: Unit and val a: Unit
    val f =
      sinfo(
        "test/DB#f().",
        SymbolInformation.Kind.METHOD,
        s.ValueSignature(s.ByNameType(unit))
      )
    val a = sinfo(
      "test/DB#a.",
      SymbolInformation.Kind.METHOD,
      s.ValueSignature(unit),
      SymbolInformation.Property.VAL.value
    )
    // TODO: `PrettyType` fails on dotc's form of a method
    intercept[PrettyType#TypeToTreeError](toTree(f))
    intercept[PrettyType#TypeToTreeError](toSingletonType(f))
    intercept[PrettyType#TypeToTreeError](toTree(a))
  }

  test("dotc's form of a var") {
    // var p: Unit
    def info(sig: s.Signature) =
      sinfo(
        "test/DB#p().",
        SymbolInformation.Kind.METHOD,
        sig,
        SymbolInformation.Property.VAR.value
      )
    assertNoDiff(
      toTree(info(s.MethodSignature(Some(s.Scope()), Nil, unit))),
      "var p: Unit"
    )
    // TODO: `PrettyType` fails on dotc's form of a signature
    intercept[PrettyType#TypeToTreeError](toTree(info(s.ValueSignature(unit))))
  }

  test("dotc's form of an inline def") {
    // inline def i: Unit
    def info(sig: s.Signature) =
      sinfo(
        "test/DB#i().",
        SymbolInformation.Kind.MACRO,
        sig,
        SymbolInformation.Property.INLINE.value
      )
    assertNoDiff(
      toTree(info(s.MethodSignature(Some(s.Scope()), Nil, unit))),
      "def i: Unit"
    )
    // TODO: `PrettyType` fails on dotc's form of a `MACRO`
    intercept[PrettyType#TypeToTreeError](
      toTree(info(s.ValueSignature(s.ByNameType(unit))))
    )
  }

  test("dotc's form of a class without type parameters") {
    // class C
    def info(scope: Option[s.Scope]) = sinfo(
      "test/C#",
      SymbolInformation.Kind.CLASS,
      s.ClassSignature(scope, List(ref("scala/AnyRef#")), s.NoType, scope)
    )
    assertNoDiff(toTree(info(Some(s.Scope()))), "class C")
    // TODO: `PrettyType` fails on dotc's form of a signature
    intercept[PrettyType#TypeToTreeError](toTree(info(None)))
  }

  test("dotc's form of a type alias") {
    // type T = Unit
    def info(tparams: Option[s.Scope]) =
      sinfo(
        "test/DB#T#",
        SymbolInformation.Kind.TYPE,
        s.TypeSignature(tparams, unit, unit)
      )
    assertNoDiff(toTree(info(Some(s.Scope()))), "type T = Unit")
    // TODO: `PrettyType` fails on dotc's form of a signature
    intercept[PrettyType#TypeToTreeError](toTree(info(None)))
  }

  test("dotc's form of a bounded type parameter") {
    // def f[T <: AnyVal]: Unit
    val f = "test/DB#f()."
    def obtained(tparams: Option[s.Scope]) = {
      val T = sinfo(
        s"$f[T]",
        SymbolInformation.Kind.TYPE_PARAMETER,
        s.TypeSignature(tparams, ref("scala/Nothing#"), ref("scala/AnyVal#"))
      )
      toTree(
        sinfo(
          f,
          SymbolInformation.Kind.METHOD,
          s.MethodSignature(Some(scope(T)), Nil, unit)
        ),
        T
      )
    }
    assertNoDiff(obtained(Some(s.Scope())), "def f[T <: AnyVal]: Unit")
    // TODO: `PrettyType` drops the bound
    assertNoDiff(obtained(None), "def f[T]: Unit")
  }

  test("refinement member with a hardlinked parameter") {
    // def g: AnyRef { def h(x: Int): Unit }
    val x = SymbolInformation(
      symbol = "local1",
      kind = SymbolInformation.Kind.PARAMETER,
      displayName = "x",
      signature = s.ValueSignature(ref("scala/Int#"))
    )
    val paramss = List(s.Scope(hardlinks = List(x)))
    val h = SymbolInformation(
      symbol = "local2",
      kind = SymbolInformation.Kind.METHOD,
      displayName = "h",
      signature = s.MethodSignature(Some(s.Scope()), paramss, unit)
    )
    val tpe =
      s.StructuralType(ref("scala/AnyRef#"), Some(s.Scope(hardlinks = List(h))))
    val g = sinfo(
      "test/DB#g().",
      SymbolInformation.Kind.METHOD,
      s.MethodSignature(Some(s.Scope()), Nil, tpe)
    )
    // `PrettyType` drops a member with a local symbol
    assertNoDiff(toTree(g), "def g: AnyRef")
  }

}

class PrettyTypeFuzzSuite extends BasePrettyTypeSuite {

  val classpathIndex: ClasspathIndex =
    ClasspathIndex(classpath, includeJdk = true)

  def checkToplevel(toplevel: SymbolInformation): Unit = {
    try {
      val info = table.info(toplevel.symbol).getOrElse {
        throw new NoSuchElementException(toplevel.symbol)
      }
      PrettyType.toTree(
        info,
        table,
        QualifyStrategy.Readable,
        fatalErrors = false
      )
    } catch {
      case e: NoSuchElementException
          // FIXME
          if e.getMessage.endsWith("_#") ||
            // contains weird macro-generated symbols like MacroTreeBuilder#`_956.type`#
            e.getMessage.contains("MacroTreeBuilder") ||
            e.getMessage.contains("javax/servlet/http/HttpServlet#") ||
            e.getMessage.contains("javax/servlet/ServletContextListener#") ||
            e.getMessage.endsWith("`?0`#") ||
            e.getMessage.endsWith("`1`#") ||
            e.getMessage.endsWith("`2`#") =>
        ()
      case NonFatal(e) =>
        throw new IllegalArgumentException(toplevel.toProtoString, e)
          with NoStackTrace
    }
  }

  def checkPath(file: AbsolutePath): Unit = {
    val settings = Settings()
    val reporter = Reporter().withSilentOut().withSilentErr()
    val node = file.toClassNode
    ClassfileInfos
      .fromClassNode(node, classpathIndex, settings, reporter)
      .foreach { classfile =>
        classfile.infos.foreach { toplevel =>
          if (toplevel.symbol.owner.isPackage) {
            checkToplevel(toplevel)
          }
        }
      }
  }

  def ignore(path: AbsolutePath): Boolean = {
    val uri = path.toURI.toString
    uri.contains("package-info") ||
    uri.contains("scala/tools/nsc/interpreter/jline") ||
    uri.contains("scala/tools/ant/") ||
    uri.contains("org/scalatest/tools/HtmlReporter") ||
    uri.contains("org/scalatest/tools/ScalaTestAntTask") ||
    uri.contains("org/scalatest/testng") ||
    uri.contains("org/scalatest/selenium") ||
    uri.contains("org/scalatest/mockito") ||
    uri.contains("org/scalatest/easymock") ||
    uri.contains("org/scalatest/jmock") ||
    uri.contains("org/scalatest/junit")
  }

  val isUnique: mutable.Set[String] = scala.collection.mutable.Set.empty[String]
  for {
    entry <- classpath.entries
    if entry.isFile
    name = entry.toNIO.getFileName.toString
    if isUnique(name)
  } {
    isUnique += name
    // ignore these test  by default because they are slow and
    // there is no need to run them on every PR.
    ignore(name) {
      PlatformFileIO.withJarFileSystem(entry, create = false) { root =>
        val files = PlatformFileIO.listAllFilesRecursively(root)
        files.foreach { file =>
          if (PathIO.extension(file.toNIO) == "class" && !ignore(file)) {
            try {
              checkPath(file)
            } catch {
              case scala.meta.internal.classpath.MissingSymbolException(_) =>
                println(file)
            }
          }
        }
      }
    }
  }
}
