package scalafix.tests.rule

import scala.meta._
import scala.meta.internal.{semanticdb => s}

import munit.FunSuite
import munit.Location
import scalafix.internal.config.ScalaVersion
import scalafix.internal.patch.PatchInternals
import scalafix.internal.rule.NoAutoTupling
import scalafix.internal.rule.OrganizeImports
import scalafix.internal.rule.OrganizeImportsConfig
import scalafix.internal.rule.RemoveUnused
import scalafix.internal.rule.RemoveUnusedConfig
import scalafix.internal.v1.InternalSemanticDoc
import scalafix.v1.SemanticDocument
import scalafix.v1.SemanticRule
import scalafix.v1.SyntacticDocument

/*
 * Scalameta may set the range of a scalac diagnostic to the innermost tree
 * that starts at its caret. Each `¦` marks a caret. The suite builds a
 * diagnostic at each caret.
 */
class DiagnosticCaretSuite extends FunSuite {

  private val privateMember = "unused private member"
  private val localDefinition = "unused local definition"
  private val explicitParameter = "unused explicit parameter"
  private val patternVariable = "unused pattern variable"
  private val unusedImport = "unused import"
  private val tupling = "adapted the argument list to the expected 2-tuple"
  private val unitInsertion =
    "adaptation of an empty argument list by inserting ()"
  private val organizer3 =
    new OrganizeImports(OrganizeImportsConfig.default, dialects.Scala3)

  // as in scalameta, index each tree at its first and its last character
  private def treesAt(source: Tree): Array[Tree] = {
    val trees = new Array[Tree](source.pos.input.chars.length + 1)
    def index(t: Tree): Unit = {
      val pos = t.pos
      if (pos.start < pos.end) { trees(pos.start) = t; trees(pos.end - 1) = t }
    }
    def walk(t: Tree): Unit = {
      index(t)
      t match {
        case t: Importee.Rename => index(t.name)
        case _ => t.children.foreach(walk)
      }
    }
    walk(source)
    trees
  }

  private def document(
      message: String,
      code: String,
      ranged: Seq[String],
      version: ScalaVersion = ScalaVersion.scala2
  ): SemanticDocument = {
    val carets = code.split('¦').init.scanLeft(0)(_ + _.length).drop(1)
    val input = Input.VirtualFile("Caret.scala", code.replace("¦", ""))
    val doc = SyntacticDocument.fromInput(input, version)
    val explicit = ranged.map { text =>
      val start = input.text.indexOf(text)
      Position.Range(input, start, start + text.length)
    }
    val trees = treesAt(doc.tree)
    val atCarets = carets.map { caret =>
      trees(caret) match {
        case t: Tree if t.pos.start == caret => t.pos
        case _ => fail(s"no tree starts at the caret at $caret")
      }
    }
    val positions = atCarets ++ explicit
    val diagnostics = positions.toList.map { pos =>
      val range =
        s.Range(pos.startLine, pos.startColumn, pos.endLine, pos.endColumn)
      s.Diagnostic(Some(range), s.Diagnostic.Severity.WARNING, message)
    }
    val sdoc = s.TextDocument(diagnostics = diagnostics)
    new SemanticDocument(new InternalSemanticDoc(doc, sdoc, _ => None))
  }

  // add a diagnostic with the range of each text in `ranged`
  private def check(rule: SemanticRule, message: String, ranged: String*)(
      code: String,
      expected: String
  )(implicit loc: Location): Unit = {
    implicit val doc: SemanticDocument = document(message, code, ranged)
    val patches = Map(rule.name -> rule.fix)
    val obtained = PatchInternals.semantic(patches, doc, false).fixed
    assertEquals(obtained, expected)
  }

  // as `check`, on a Scala 3 source
  private def check3(rule: SemanticRule, message: String, ranged: String*)(
      code: String,
      expected: String
  )(implicit loc: Location): Unit = {
    implicit val doc: SemanticDocument =
      document(message, code, ranged, ScalaVersion.scala3)
    val patches = Map(rule.name -> rule.fix)
    val obtained = PatchInternals.semantic(patches, doc, false).fixed
    assertEquals(obtained, expected)
  }

  private def checkUnused(message: String, code: String, expected: String)(
      implicit loc: Location
  ): Unit =
    check(new RemoveUnused(), message)(code, expected)

  test("private def") {
    checkUnused(
      privateMember,
      "object A { private def ¦a = 1 }",
      "object A {  }"
    )
  }

  test("private val") {
    checkUnused(
      privateMember,
      "object A { private val ¦a = 1 }",
      "object A {  }"
    )
  }

  test("private var") {
    checkUnused(
      privateMember,
      "object A { private var ¦a = 1 }",
      "object A {  }"
    )
  }

  test("private lazy val with a type") {
    checkUnused(
      privateMember,
      "object A { private lazy val ¦a: Int = 1 }",
      "object A {  }"
    )
  }

  test("private def with the range of the definition") {
    check(new RemoveUnused(), privateMember, "private def a = 1")(
      "object A { private def a = 1 }",
      "object A {  }"
    )
  }

  test("private lazy val with the range of the definition") {
    check(new RemoveUnused(), privateMember, "private lazy val a = 1")(
      "object A { private lazy val a = 1 }",
      "object A {  }"
    )
  }

  test("annotated private def") {
    checkUnused(
      privateMember,
      "object A { @deprecated private def ¦a = 1 }",
      "object A {  }"
    )
  }

  test("annotated private val with a block") {
    checkUnused(
      privateMember,
      "object A { @deprecated private val ¦a = { 1 } }",
      "object A { locally { 1 } }"
    )
  }

  test("annotated private def with the range from the modifier") {
    check(new RemoveUnused(), privateMember, "private def a = 1")(
      "object A { @deprecated private def a = 1 }",
      "object A { @deprecated private def a = 1 }"
    )
  }

  test("annotated local def with the range from the keyword") {
    check(new RemoveUnused(), localDefinition, "def a = 1")(
      "object A { def f = { @deprecated def a = 1; 2 } }",
      "object A { def f = { @deprecated def a = 1; 2 } }"
    )
  }

  test("annotated local val with the range from the keyword") {
    check(new RemoveUnused(), localDefinition, "val a = { 1")(
      "object A { def f = { @deprecated val a = { 1 }; 2 } }",
      "object A { def f = { @deprecated val a = { 1 }; 2 } }"
    )
  }

  test("private def with a local def in its annotation") {
    check(new RemoveUnused(), localDefinition, "def b = 1")(
      "object A { @ann({ def b = 1; 2 }) private def a = 3 }",
      "object A { @ann({ ; 2 }) private def a = 3 }"
    )
  }

  test("private object, class, trait and type") {
    checkUnused(
      privateMember,
      "object A { private object ¦B; private class ¦C; private trait ¦D; private type ¦E = Int }",
      "object A { private object B; private class C; private trait D; private type E = Int }"
    )
  }

  test("private object with the range of the definition") {
    check(new RemoveUnused(), privateMember, "private object B")(
      "object A { private object B }",
      "object A { private object B }"
    )
  }

  test("local def, val and var") {
    checkUnused(
      localDefinition,
      "object A { def f = { def ¦a = 1; val ¦b = 2; var ¦c = 3; 4 } }",
      "object A { def f = { ; ; ; 4 } }"
    )
  }

  test("local val with a block, with the range of the definition") {
    check(new RemoveUnused(), localDefinition, "val a = { println(); 1 }")(
      "object A { def f = { val a = { println(); 1 }; 2 } }",
      "object A { def f = { locally { println(); 1 }; 2 } }"
    )
  }

  test("local object, class, trait and type") {
    checkUnused(
      localDefinition,
      "object A { def f = { object ¦B; class ¦C; trait ¦D; type ¦E = Int; 4 } }",
      "object A { def f = { object B; class C; trait D; type E = Int; 4 } }"
    )
  }

  test("private def with a local val") {
    checkUnused(
      privateMember,
      "object A { private def ¦a = { val b = 1; b } }",
      "object A {  }"
    )
  }

  test("local val in a private def") {
    checkUnused(
      localDefinition,
      "object A { private def a = { val ¦b = 1; 2 } }",
      "object A { private def a = { ; 2 } }"
    )
  }

  test("val with two names") {
    checkUnused(
      localDefinition,
      "object A { def f = { val ¦a, b = 1; b } }",
      "object A { def f = { val a, b = 1; b } }"
    )
  }

  test("val with two names, with the range from the second name") {
    check(new RemoveUnused(), localDefinition, "b = 1")(
      "object A { def f = { val a, b = 1; a } }",
      "object A { def f = { val a, b = 1; a } }"
    )
  }

  test("val with a pattern") {
    checkUnused(
      localDefinition,
      "object A { def f = { val Some(¦a) = Option(1); 2 } }",
      "object A { def f = { val Some(a) = Option(1); 2 } }"
    )
  }

  test("val with a pattern, at the pattern") {
    checkUnused(
      localDefinition,
      "object A { def f = { val ¦Some(a) = Option(1); 2 } }",
      "object A { def f = { Option(1); 2 } }"
    )
  }

  test("private val with a pattern, at its arguments") {
    checkUnused(
      privateMember,
      "object A { private val Some¦(a) = Option(1) }",
      "object A { private val Some(a) = Option(1) }"
    )
  }

  test("private val with a pattern, with the range of the definition") {
    check(new RemoveUnused(), privateMember, "private val Some(a) = Option(1)")(
      "object A { private val Some(a) = Option(1) }",
      "object A { Option(1) }"
    )
  }

  test("val with a pattern variable") {
    checkUnused(
      patternVariable,
      "object A { def f = { val Some(¦a) = Option(1); 2 } }",
      "object A { def f = { val Some(a) = Option(1); 2 } }"
    )
  }

  test("val with a pattern variable, with the range of the definition") {
    check(new RemoveUnused(), patternVariable, "val Some(a) = Option(1)")(
      "object A { def f = { val Some(a) = Option(1); 2 } }",
      "object A { def f = { val Some(a) = Option(1); 2 } }"
    )
  }

  test("untyped lambda parameter") {
    checkUnused(
      explicitParameter,
      "object A { f { ¦x => 1 } }",
      "object A { f { _ => 1 } }"
    )
  }

  test("untyped lambda parameter in parens") {
    checkUnused(
      explicitParameter,
      "object A { f { (¦x) => 1 } }",
      "object A { f { (_) => 1 } }"
    )
  }

  test("typed lambda parameter") {
    checkUnused(
      explicitParameter,
      "object A { f { (¦x: Int) => 1 } }",
      "object A { f { (_: Int) => 1 } }"
    )
  }

  test("typed lambda parameter with the range of the parameter") {
    check(new RemoveUnused(), explicitParameter, "x: Int")(
      "object A { f { (x: Int) => 1 } }",
      "object A { f { (_: Int) => 1 } }"
    )
  }

  test("typed lambda parameter without parens") {
    check(new RemoveUnused(), explicitParameter, "x: Int")(
      "object A { f { x: Int => 1 } }",
      "object A { f { _: Int => 1 } }"
    )
  }

  test("implicit lambda parameter") {
    checkUnused(
      explicitParameter,
      "object A { f { implicit ¦x => 1 } }",
      "object A { f { _ => 1 } }"
    )
  }

  test("typed implicit lambda parameter") {
    check(new RemoveUnused(), explicitParameter, "x: Int")(
      "object A { f { implicit x: Int => 1 } }",
      "object A { f { (_: Int) => 1 } }"
    )
  }

  test("typed implicit lambda parameter at the name") {
    checkUnused(
      explicitParameter,
      "object A { f { implicit ¦x: Int => 1 } }",
      "object A { f { (_: Int) => 1 } }"
    )
  }

  test("typed implicit lambda parameter with comments at the name") {
    checkUnused(
      explicitParameter,
      "object A { f { implicit /* a */ ¦x /* b */ : Int => 1 } }",
      "object A { f { ( /* a */ _ /* b */ : Int) => 1 } }"
    )
  }

  test("second of two typed lambda parameters") {
    checkUnused(
      explicitParameter,
      "object A { (x: Int, ¦y: Int) => x }",
      "object A { (x: Int, _: Int) => x }"
    )
  }

  test("second of two untyped lambda parameters") {
    checkUnused(
      explicitParameter,
      "object A { (x, ¦y) => x }",
      "object A { (x, _) => x }"
    )
  }

  test("two typed lambda parameters with the ranges of the parameters") {
    check(new RemoveUnused(), explicitParameter, "x: Int", "y: Int")(
      "object A { (x: Int, y: Int) => 1 }",
      "object A { (_: Int, _: Int) => 1 }"
    )
  }

  test("method parameter") {
    checkUnused(
      explicitParameter,
      "object A { def f(¦x: Int) = 1 }",
      "object A { def f(x: Int) = 1 }"
    )
  }

  test("pattern variable") {
    checkUnused(
      patternVariable,
      "object A { 1 match { case ¦a => } }",
      "object A { 1 match { case _ => } }"
    )
  }

  test("typed pattern variable") {
    checkUnused(
      patternVariable,
      "object A { 1 match { case ¦a: Int => } }",
      "object A { 1 match { case _: Int => } }"
    )
  }

  test("typed pattern variable with the range of the pattern") {
    check(new RemoveUnused(), patternVariable, "a: Int")(
      "object A { 1 match { case a: Int => } }",
      "object A { 1 match { case _: Int => } }"
    )
  }

  test("typed pattern variable in parens with the range of the pattern") {
    check(new RemoveUnused(), patternVariable, "a: Int")(
      "object A { 1 match { case (a: Int) => } }",
      "object A { 1 match { case (_: Int) => } }"
    )
  }

  test("typed pattern variable in a catch with the range of the pattern") {
    check(new RemoveUnused(), patternVariable, "e: Exception")(
      "object A { try 1 catch { case e: Exception => 2 } }",
      "object A { try 1 catch { case _: Exception => 2 } }"
    )
  }

  test("typed pattern variable in a catch") {
    checkUnused(
      patternVariable,
      "object A { try 1 catch { case ¦e: Exception => 2 } }",
      "object A { try 1 catch { case _: Exception => 2 } }"
    )
  }

  test("bound pattern variable") {
    checkUnused(
      patternVariable,
      "object A { 1 match { case ¦a @ 1 => } }",
      "object A { 1 match { case 1 => } }"
    )
  }

  test("bound pattern variable with the range of the pattern") {
    check(new RemoveUnused(), patternVariable, "a @ 1")(
      "object A { 1 match { case a @ 1 => } }",
      "object A { 1 match { case 1 => } }"
    )
  }

  test("pattern variable bound to a pattern variable") {
    checkUnused(
      patternVariable,
      "object A { 1 match { case a @ ¦b => a } }",
      "object A { 1 match { case a @ _ => a } }"
    )
  }

  test("two bound pattern variables") {
    checkUnused(
      patternVariable,
      "object A { 1 match { case ¦a @ ¦b => } }",
      "object A { 1 match { case _ => } }"
    )
  }

  test("bound sequence pattern variable") {
    checkUnused(
      patternVariable,
      "object A { Nil match { case List(¦a @ _*) => } }",
      "object A { Nil match { case List(_*) => } }"
    )
  }

  test("bound sequence pattern variable with the range of the pattern") {
    check(new RemoveUnused(), patternVariable, "a @ _*")(
      "object A { Nil match { case List(a @ _*) => } }",
      "object A { Nil match { case List(_*) => } }"
    )
  }

  test("nested bound pattern variables") {
    checkUnused(
      patternVariable,
      "object A { None match { case ¦a @ Some(¦b @ Some(_)) => } }",
      "object A { None match { case Some(Some(_)) => } }"
    )
  }

  test("nested bound pattern variables with the ranges of the patterns") {
    check(
      new RemoveUnused(),
      patternVariable,
      "a @ Some(b @ Some(_))",
      "b @ Some(_)"
    )(
      "object A { None match { case a @ Some(b @ Some(_)) => } }",
      "object A { None match { case Some(Some(_)) => } }"
    )
  }

  test("bound typed pattern in parens") {
    checkUnused(
      patternVariable,
      "object A { 1 match { case ¦a @ (_: Int) => } }",
      "object A { 1 match { case (_: Int) => } }"
    )
  }

  test("bound typed pattern in parens with the range of the pattern") {
    check(new RemoveUnused(), patternVariable, "a @ (_: Int")(
      "object A { 1 match { case a @ (_: Int) => } }",
      "object A { 1 match { case (_: Int) => } }"
    )
  }

  test("pattern variable in parens") {
    checkUnused(
      patternVariable,
      "object A { Option(1) match { case Some((¦a)) => } }",
      "object A { Option(1) match { case Some((_)) => } }"
    )
  }

  test("pattern variable in parens with comments") {
    checkUnused(
      patternVariable,
      "object A { Option(1) match { case Some(( /* c */ ¦a /* d */ )) => } }",
      "object A { Option(1) match { case Some(( /* c */ _ /* d */ )) => } }"
    )
  }

  test("Scala 3 pattern variable in parens with comments") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { Option(1) match { case Some(( /* c */ ¦a /* d */ )) => } }",
      "object A { Option(1) match { case Some(( /* c */ _ /* d */ )) => } }"
    )
  }

  test("pattern variable in a partial function") {
    checkUnused(
      patternVariable,
      "object A { f { case Some(¦a) => } }",
      "object A { f { case Some(_) => } }"
    )
  }

  test("pattern variable in parens in a tuple pattern") {
    check(new RemoveUnused(), patternVariable)(
      "object A { (1, 2) match { case ((¦a), b) => b } }",
      "object A { (1, 2) match { case ((_), b) => b } }"
    )
  }

  test("pattern variable in a tuple pattern") {
    check(new RemoveUnused(), patternVariable)(
      "object A { (1, 2) match { case (¦a, b) => b } }",
      "object A { (1, 2) match { case (_, b) => b } }"
    )
  }

  test("pattern variable in parens at the top") {
    check(new RemoveUnused(), patternVariable)(
      "object A { 1 match { case (¦a) => } }",
      "object A { 1 match { case (_) => } }"
    )
  }

  test("pattern variable in double parens at the top") {
    check(new RemoveUnused(), patternVariable)(
      "object A { 1 match { case ((¦a)) => } }",
      "object A { 1 match { case ((_)) => } }"
    )
  }

  test("pattern variable in double parens in an extractor") {
    check(new RemoveUnused(), patternVariable)(
      "object A { Option(1) match { case Some(((¦a))) => } }",
      "object A { Option(1) match { case Some(((_))) => } }"
    )
  }

  test("pattern variable in parens with comments") {
    check(new RemoveUnused(), patternVariable)(
      "object A { Option(1) match { case Some(( /* c */ ¦a /* d */ )) => } }",
      "object A { Option(1) match { case Some(( /* c */ _ /* d */ )) => } }"
    )
  }

  test("pattern variable in parens with a comment before the parens") {
    check(new RemoveUnused(), patternVariable)(
      "object A { Option(1) match { case Some(/* c */ (¦a)) => } }",
      "object A { Option(1) match { case Some(/* c */ (_)) => } }"
    )
  }

  test("typed pattern variable in parens") {
    check(new RemoveUnused(), patternVariable)(
      "object A { 1 match { case (¦a: Int) => } }",
      "object A { 1 match { case (_: Int) => } }"
    )
  }

  test("typed pattern variable in parens in an extractor") {
    check(new RemoveUnused(), patternVariable)(
      "object A { Option(1) match { case Some((¦a: Int)) => } }",
      "object A { Option(1) match { case Some((_: Int)) => } }"
    )
  }

  test("pattern variable in parens in an extractor of two") {
    check(new RemoveUnused(), patternVariable)(
      "object A { (1, 2) match { case Tuple2((¦a), b) => b } }",
      "object A { (1, 2) match { case Tuple2((_), b) => b } }"
    )
  }

  test("pattern variable in parens in an infix extractor") {
    check(new RemoveUnused(), patternVariable)(
      "object A { List(1) match { case (¦a) :: t => t } }",
      "object A { List(1) match { case (_) :: t => t } }"
    )
  }

  test("pattern variable in parens bound to a pattern variable") {
    check(new RemoveUnused(), patternVariable)(
      "object A { 1 match { case a @ (¦b) => a } }",
      "object A { 1 match { case a @ (_) => a } }"
    )
  }

  test("bound pattern variable in parens") {
    check(new RemoveUnused(), patternVariable)(
      "object A { 1 match { case (¦a @ 1) => } }",
      "object A { 1 match { case 1 => } }"
    )
  }

  test("bound tuple patterns") {
    checkUnused(
      patternVariable,
      "object A { (1, (2, 3)) match { case ¦t @ (i, ¦v @ (_, _)) => i } }",
      "object A { (1, (2, 3)) match { case (i, (_, _)) => i } }"
    )
  }

  test("bound unit pattern") {
    checkUnused(
      patternVariable,
      "object A { () match { case ¦u @ () => } }",
      "object A { () match { case () => } }"
    )
  }

  test("bound typed pattern in parens in an infix extractor") {
    check(new RemoveUnused(), patternVariable)(
      "object A { List(1) match { case (¦a @ (_: Int)) :: t => t } }",
      "object A { List(1) match { case (_: Int) :: t => t } }"
    )
  }

  test("bound alternative pattern in an infix extractor") {
    check(new RemoveUnused(), patternVariable)(
      "object A { List(1) match { case ¦a @ (1 | 2) :: Nil => } }",
      "object A { List(1) match { case (1 | 2) :: Nil => } }"
    )
  }

  test("bound typed pattern in an infix extractor") {
    check(new RemoveUnused(), patternVariable)(
      "object A { List(1) match { case ¦a @ (_: Int) :: t => t } }",
      "object A { List(1) match { case (_: Int) :: t => t } }"
    )
  }

  test("bound alternative pattern in an extractor") {
    check(new RemoveUnused(), patternVariable)(
      "object A { Option(1) match { case Some(¦a @ (1 | 2)) => } }",
      "object A { Option(1) match { case Some((1 | 2)) => } }"
    )
  }

  test("pattern variable in parens in a partial function") {
    check(new RemoveUnused(), patternVariable)(
      "object A { f { case Some((¦a)) => } }",
      "object A { f { case Some((_)) => } }"
    )
  }

  test("pattern variable in parens in a catch") {
    check(new RemoveUnused(), patternVariable)(
      "object A { try 1 catch { case (¦e) => 2 } }",
      "object A { try 1 catch { case (_) => 2 } }"
    )
  }

  test("val with a pattern variable in parens") {
    check(new RemoveUnused(), patternVariable)(
      "object A { def f = { val Some((¦a)) = Option(1); 2 } }",
      "object A { def f = { val Some((a)) = Option(1); 2 } }"
    )
  }

  test("generator with a pattern variable in parens") {
    check(new RemoveUnused(), patternVariable)(
      "object A { for { (¦a) <- List(1) } yield 1 }",
      "object A { for { (a) <- List(1) } yield 1 }"
    )
  }

  test("generator with a pattern variable in parens in an extractor") {
    check(new RemoveUnused(), patternVariable)(
      "object A { for { Some((¦a)) <- List(Option(1)) } yield 1 }",
      "object A { for { Some((a)) <- List(Option(1)) } yield 1 }"
    )
  }

  test("pattern variable in parens with the range of the variable") {
    check(new RemoveUnused(), patternVariable, "xx")(
      "object A { Option(1) match { case Some((xx)) => } }",
      "object A { Option(1) match { case Some((_)) => } }"
    )
  }

  test("pattern variable in parens with the range of the parens") {
    check(new RemoveUnused(), patternVariable, "(xx)")(
      "object A { Option(1) match { case Some((xx)) => } }",
      "object A { Option(1) match { case Some((_)) => } }"
    )
  }

  test("pattern variable in parens at the top with the range of the variable") {
    check(new RemoveUnused(), patternVariable, "xx")(
      "object A { 1 match { case (xx) => } }",
      "object A { 1 match { case (_) => } }"
    )
  }

  test("pattern variable in parens at the top with the range of the parens") {
    check(new RemoveUnused(), patternVariable, "(xx)")(
      "object A { 1 match { case (xx) => } }",
      "object A { 1 match { case (_) => } }"
    )
  }

  test("bound typed pattern in parens with the range through the parens") {
    check(new RemoveUnused(), patternVariable, "xx @ (_: Int)")(
      "object A { 1 match { case xx @ (_: Int) => } }",
      "object A { 1 match { case (_: Int) => } }"
    )
  }

  test(
    "bound typed pattern in parens in an infix extractor with the range of the pattern"
  ) {
    check(new RemoveUnused(), patternVariable, "xx @ (_: Int)")(
      "object A { List(1) match { case (xx @ (_: Int)) :: t => t } }",
      "object A { List(1) match { case (_: Int) :: t => t } }"
    )
  }

  test(
    "bound alternative pattern in an extractor with the range of the pattern"
  ) {
    check(new RemoveUnused(), patternVariable, "xx @ (1 | 2)")(
      "object A { Option(1) match { case Some(xx @ (1 | 2)) => } }",
      "object A { Option(1) match { case Some((1 | 2)) => } }"
    )
  }

  test("Scala 3 pattern variable in parens in a tuple pattern") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { (1, 2) match { case ((¦a), b) => b } }",
      "object A { (1, 2) match { case ((_), b) => b } }"
    )
  }

  test("Scala 3 pattern variable in a tuple pattern") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { (1, 2) match { case (¦a, b) => b } }",
      "object A { (1, 2) match { case (_, b) => b } }"
    )
  }

  test("Scala 3 pattern variable in parens at the top") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { 1 match { case (¦a) => } }",
      "object A { 1 match { case (_) => } }"
    )
  }

  test("Scala 3 pattern variable in double parens at the top") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { 1 match { case ((¦a)) => } }",
      "object A { 1 match { case ((_)) => } }"
    )
  }

  test("Scala 3 pattern variable in double parens in an extractor") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { Option(1) match { case Some(((¦a))) => } }",
      "object A { Option(1) match { case Some(((_))) => } }"
    )
  }

  test("Scala 3 pattern variable in parens with comments") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { Option(1) match { case Some(( /* c */ ¦a /* d */ )) => } }",
      "object A { Option(1) match { case Some(( /* c */ _ /* d */ )) => } }"
    )
  }

  test("Scala 3 pattern variable in parens with a comment before the parens") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { Option(1) match { case Some(/* c */ (¦a)) => } }",
      "object A { Option(1) match { case Some(/* c */ (_)) => } }"
    )
  }

  test("Scala 3 typed pattern variable in parens") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { 1 match { case (¦a: Int) => } }",
      "object A { 1 match { case (_: Int) => } }"
    )
  }

  test("Scala 3 typed pattern variable in parens in an extractor") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { Option(1) match { case Some((¦a: Int)) => } }",
      "object A { Option(1) match { case Some((_: Int)) => } }"
    )
  }

  test("Scala 3 pattern variable in parens in an extractor of two") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { (1, 2) match { case Tuple2((¦a), b) => b } }",
      "object A { (1, 2) match { case Tuple2((_), b) => b } }"
    )
  }

  test("Scala 3 pattern variable in parens in an infix extractor") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { List(1) match { case (¦a) :: t => t } }",
      "object A { List(1) match { case (_) :: t => t } }"
    )
  }

  test("Scala 3 pattern variable in parens bound to a pattern variable") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { 1 match { case a @ (¦b) => a } }",
      "object A { 1 match { case a @ (_) => a } }"
    )
  }

  test("Scala 3 bound pattern variable in parens") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { 1 match { case (¦a @ 1) => } }",
      "object A { 1 match { case 1 => } }"
    )
  }

  test("Scala 3 bound tuple patterns") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { (1, (2, 3)) match { case ¦t @ (i, ¦v @ (_, _)) => i } }",
      "object A { (1, (2, 3)) match { case (i, (_, _)) => i } }"
    )
  }

  test("Scala 3 bound unit pattern") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { () match { case ¦u @ () => } }",
      "object A { () match { case () => } }"
    )
  }

  test("Scala 3 bound typed pattern in parens in an infix extractor") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { List(1) match { case (¦a @ (_: Int)) :: t => t } }",
      "object A { List(1) match { case (_: Int) :: t => t } }"
    )
  }

  test("Scala 3 bound alternative pattern in an infix extractor") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { List(1) match { case ¦a @ (1 | 2) :: Nil => } }",
      "object A { List(1) match { case (1 | 2) :: Nil => } }"
    )
  }

  test("Scala 3 bound typed pattern in an infix extractor") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { List(1) match { case ¦a @ (_: Int) :: t => t } }",
      "object A { List(1) match { case (_: Int) :: t => t } }"
    )
  }

  test("Scala 3 bound alternative pattern in an extractor") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { Option(1) match { case Some(¦a @ (1 | 2)) => } }",
      "object A { Option(1) match { case Some((1 | 2)) => } }"
    )
  }

  test("Scala 3 pattern variable in parens in a partial function") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { f { case Some((¦a)) => } }",
      "object A { f { case Some((_)) => } }"
    )
  }

  test("Scala 3 pattern variable in parens in a catch") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { try 1 catch { case (¦e) => 2 } }",
      "object A { try 1 catch { case (_) => 2 } }"
    )
  }

  test("Scala 3 val with a pattern variable in parens") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { def f = { val Some((¦a)) = Option(1); 2 } }",
      "object A { def f = { val Some((a)) = Option(1); 2 } }"
    )
  }

  test("Scala 3 generator with a pattern variable in parens") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { for { (¦a) <- List(1) } yield 1 }",
      "object A { for { (a) <- List(1) } yield 1 }"
    )
  }

  test("Scala 3 generator with a pattern variable in parens in an extractor") {
    check3(new RemoveUnused(), patternVariable)(
      "object A { for { Some((¦a)) <- List(Option(1)) } yield 1 }",
      "object A { for { Some((a)) <- List(Option(1)) } yield 1 }"
    )
  }

  test("Scala 3 pattern variable in parens with the range of the variable") {
    check3(new RemoveUnused(), patternVariable, "xx")(
      "object A { Option(1) match { case Some((xx)) => } }",
      "object A { Option(1) match { case Some((_)) => } }"
    )
  }

  test("Scala 3 pattern variable in parens with the range of the parens") {
    check3(new RemoveUnused(), patternVariable, "(xx)")(
      "object A { Option(1) match { case Some((xx)) => } }",
      "object A { Option(1) match { case Some((_)) => } }"
    )
  }

  test(
    "Scala 3 pattern variable in parens at the top with the range of the variable"
  ) {
    check3(new RemoveUnused(), patternVariable, "xx")(
      "object A { 1 match { case (xx) => } }",
      "object A { 1 match { case (_) => } }"
    )
  }

  test(
    "Scala 3 pattern variable in parens at the top with the range of the parens"
  ) {
    check3(new RemoveUnused(), patternVariable, "(xx)")(
      "object A { 1 match { case (xx) => } }",
      "object A { 1 match { case (_) => } }"
    )
  }

  test(
    "Scala 3 bound typed pattern in parens with the range through the parens"
  ) {
    check3(new RemoveUnused(), patternVariable, "xx @ (_: Int)")(
      "object A { 1 match { case xx @ (_: Int) => } }",
      "object A { 1 match { case (_: Int) => } }"
    )
  }

  test(
    "Scala 3 bound typed pattern in parens in an infix extractor with the range of the pattern"
  ) {
    check3(new RemoveUnused(), patternVariable, "xx @ (_: Int)")(
      "object A { List(1) match { case (xx @ (_: Int)) :: t => t } }",
      "object A { List(1) match { case (_: Int) :: t => t } }"
    )
  }

  test(
    "Scala 3 bound alternative pattern in an extractor with the range of the pattern"
  ) {
    check3(new RemoveUnused(), patternVariable, "xx @ (1 | 2)")(
      "object A { Option(1) match { case Some(xx @ (1 | 2)) => } }",
      "object A { Option(1) match { case Some((1 | 2)) => } }"
    )
  }

  test("imports") {
    checkUnused(
      unusedImport,
      "import a.{¦b, c}, d.{¦e => f, g}, h.¦_",
      "import a.c, d.g"
    )
  }

  test("renamed import beside a wildcard") {
    checkUnused(unusedImport, "import a.{¦b => c, _}", "import a.{b => _, _}")
  }

  test("renamed import beside a wildcard, organized") {
    check(new OrganizeImports(), unusedImport)(
      """|import a.{¦b => c, _}
        |object A""".stripMargin,
      """|import a.{b => _, _}
        |object A""".stripMargin
    )
  }

  test("renamed import beside a name, organized") {
    check(new OrganizeImports(), unusedImport)(
      """|import a.{¦b => c, d}
        |object A""".stripMargin,
      """|import a.d
        |object A""".stripMargin
    )
  }

  test("renamed import beside an unused wildcard, organized") {
    check(new OrganizeImports(), unusedImport)(
      """|import a.{¦b => c, ¦_}
        |object A""".stripMargin,
      """|
        |object A""".stripMargin
    )
  }

  test("used renamed import beside an unused wildcard, organized") {
    check(new OrganizeImports(), unusedImport)(
      """|import a.{b => c, ¦_}
        |object A""".stripMargin,
      """|import a.{b => c}
        |object A""".stripMargin
    )
  }

  test("renamed import with the range of the importee") {
    check(new RemoveUnused(), unusedImport, "b => c")(
      "import a.{b => c, d}",
      "import a.d"
    )
  }

  test("unimport beside an unused name") {
    checkUnused(unusedImport, "import a.{b => _, ¦c}", "import a.{b => _}")
  }

  test("unimport beside a used and an unused name") {
    checkUnused(
      unusedImport,
      "import a.{b => _, c, ¦d}",
      "import a.{b => _, c}"
    )
  }

  test("unimport alone, organized") {
    check(new OrganizeImports(), unusedImport)(
      """|import a.{b => _}
        |object A""".stripMargin,
      """|import a.{b => _}
        |object A""".stripMargin
    )
  }

  test("unimport beside an unused name, organized") {
    check(new OrganizeImports(), unusedImport)(
      """|import a.{b => _, ¦c}
        |object A""".stripMargin,
      """|import a.{b => _}
        |object A""".stripMargin
    )
  }

  test("local unimport alone, organized") {
    check(new OrganizeImports(), unusedImport)(
      "object A { import a.{b => _} }",
      "object A { import a.{b => _} }"
    )
  }

  test("local unimport beside an unused name, organized") {
    check(new OrganizeImports(), unusedImport)(
      "object A { import a.{b => _, ¦c} }",
      "object A { import a.{b => _} }"
    )
  }

  test("wildcard import with the range of the import") {
    check(new RemoveUnused(), unusedImport, "import a._")(
      "import a._",
      ""
    )
  }

  test("wildcard with the range of the import, and the name at a caret") {
    check(new RemoveUnused(), unusedImport, "import a.{b, _}")(
      "import a.{¦b, _}",
      ""
    )
  }

  test("wildcard with the range of the import, beside a used name") {
    check(new RemoveUnused(), unusedImport, "import a.{b, _}")(
      "import a.{b, _}",
      ""
    )
  }

  test("wildcard in the first clause with the range from the import") {
    check(new RemoveUnused(), unusedImport, "import a.{b, _}")(
      "import a.{b, _}, c.d",
      "import c.d"
    )
  }

  test("wildcard in the second clause with the range of the clause") {
    check(new RemoveUnused(), unusedImport, "c.{d, _}")(
      "import a.b, c.{d, _}",
      "import a.b"
    )
  }

  test("unimport beside a wildcard") {
    checkUnused(unusedImport, "import a.{b => _, ¦_}", "import a.{b => _}")
  }

  test("unimport beside a wildcard with the range of the import") {
    check(new RemoveUnused(), unusedImport, "import a.{b => _, _}")(
      "import a.{b => _, _}",
      ""
    )
  }

  test("unimport beside a wildcard, organized") {
    check(new OrganizeImports(), unusedImport)(
      """|import a.{b => _, ¦_}
        |object A""".stripMargin,
      """|import a.{b => _}
        |object A""".stripMargin
    )
  }

  test("unimport beside a wildcard with the range of the import, organized") {
    check(new OrganizeImports(), unusedImport, "import a.{b => _, _}")(
      """|import a.{b => _, _}
        |object A""".stripMargin,
      """|
        |object A""".stripMargin
    )
  }

  test("Predef unimport beside a wildcard") {
    checkUnused(
      unusedImport,
      "import scala.Predef.{any2stringadd => _, ¦_}",
      "import scala.Predef.{any2stringadd => _}"
    )
  }

  test("Predef unimport beside a wildcard, organized") {
    check(new OrganizeImports(), unusedImport)(
      """|import scala.Predef.{any2stringadd => _, ¦_}
        |object A""".stripMargin,
      """|import scala.Predef.{any2stringadd => _}
        |object A""".stripMargin
    )
  }

  test("Predef unimport beside an unused name") {
    checkUnused(
      unusedImport,
      "import scala.Predef.{any2stringadd => _, ¦println}",
      "import scala.Predef.{any2stringadd => _}"
    )
  }

  test("Predef unimport beside an unused name, organized") {
    check(new OrganizeImports(), unusedImport)(
      """|import scala.Predef.{any2stringadd => _, ¦println}
        |object A""".stripMargin,
      """|import scala.Predef.{any2stringadd => _}
        |object A""".stripMargin
    )
  }

  test("unqualified Predef unimport beside a wildcard") {
    checkUnused(
      unusedImport,
      "import Predef.{any2stringadd => _, ¦_}",
      "import Predef.{any2stringadd => _}"
    )
  }

  test("rooted Predef unimport beside a wildcard") {
    checkUnused(
      unusedImport,
      "import _root_.scala.Predef.{any2stringadd => _, ¦_}",
      "import _root_.scala.Predef.{any2stringadd => _}"
    )
  }

  test("unimport from another Predef beside a wildcard") {
    checkUnused(
      unusedImport,
      "import a.Predef.{b => _, ¦_}",
      "import a.Predef.{b => _}"
    )
  }

  test("unused Predef unimport beside a wildcard") {
    check(new RemoveUnused(), unusedImport, "any2stringadd => _")(
      "import scala.Predef.{any2stringadd => _, ¦_}",
      ""
    )
  }

  test("unused Predef unimport beside a wildcard, organized") {
    check(new OrganizeImports(), unusedImport, "any2stringadd => _")(
      """|import scala.Predef.{any2stringadd => _, ¦_}
        |object A""".stripMargin,
      """|
        |object A""".stripMargin
    )
  }

  test("Predef unimport beside a wildcard with the range of the import") {
    val imp = "import scala.Predef.{any2stringadd => _, _}"
    check(new RemoveUnused(), unusedImport, imp)(imp, "")
  }

  test("local Predef unimport beside a wildcard") {
    checkUnused(
      unusedImport,
      "object A { import scala.Predef.{any2stringadd => _, ¦_} }",
      "object A { import scala.Predef.{any2stringadd => _} }"
    )
  }

  test("local Predef unimport beside a wildcard, organized") {
    check(new OrganizeImports(), unusedImport)(
      "object A { import scala.Predef.{any2stringadd => _, ¦_} }",
      "object A { import scala.Predef.{any2stringadd => _} }"
    )
  }

  test("Predef unimport beside another unused import") {
    checkUnused(
      unusedImport,
      """|import scala.Predef.{any2stringadd => _}
        |import a.¦b""".stripMargin,
      "import scala.Predef.{any2stringadd => _}"
    )
  }

  test("Predef unimport alone") {
    checkUnused(
      unusedImport,
      "import scala.Predef.{any2stringadd => _}",
      "import scala.Predef.{any2stringadd => _}"
    )
  }

  test("Predef unimports beside another unused import") {
    checkUnused(
      unusedImport,
      """|import scala.Predef.{any2stringadd => _, augmentString => _}
        |import a.¦b""".stripMargin,
      "import scala.Predef.{any2stringadd => _, augmentString => _}"
    )
  }

  test("unqualified Predef unimport beside another unused import") {
    checkUnused(
      unusedImport,
      """|import Predef.{any2stringadd => _}
        |import a.¦b""".stripMargin,
      "import Predef.{any2stringadd => _}"
    )
  }

  test("local Predef unimport beside another unused import") {
    checkUnused(
      unusedImport,
      "object A { import scala.Predef.{any2stringadd => _}; import a.¦b }",
      "object A { import scala.Predef.{any2stringadd => _}; }"
    )
  }

  test("unused Predef unimport beside another unused import") {
    checkUnused(
      unusedImport,
      """|import scala.Predef.{¦any2stringadd => _}
        |import a.¦b""".stripMargin,
      ""
    )
  }

  test("unimport alone beside another unused import") {
    checkUnused(
      unusedImport,
      """|import a.{b => _}
        |import c.¦d""".stripMargin,
      "import a.{b => _}"
    )
  }

  test("Scala 3 unimport alone beside another unused import") {
    check3(new RemoveUnused(), unusedImport)(
      """|import a.{b as _}
        |import c.¦d""".stripMargin,
      "import a.{b as _}"
    )
  }

  test("Predef unimport beside an unused private member") {
    checkUnused(
      privateMember,
      """|import scala.Predef.{any2stringadd => _}
        |object A { private def ¦a = 1 }""".stripMargin,
      """|import scala.Predef.{any2stringadd => _}
        |object A {  }""".stripMargin
    )
  }

  test("unimport beside an unused private member, with imports off") {
    val rule = new RemoveUnused(RemoveUnusedConfig(imports = false))
    check(rule, privateMember)(
      """|import a.{b => _}
        |object A { private def ¦a = 1 }""".stripMargin,
      """|import a.{b => _}
        |object A {  }""".stripMargin
    )
  }

  test("unused import, with imports off") {
    val rule = new RemoveUnused(RemoveUnusedConfig(imports = false))
    check(rule, unusedImport)("import a.¦b", "import a.b")
  }

  test("imports beside an unused private member") {
    checkUnused(
      privateMember,
      """|import a.{b, _}
        |object A { private def ¦c = 1 }""".stripMargin,
      """|import a.{b, _}
        |object A {  }""".stripMargin
    )
  }

  test("imports beside an unused private member, organized") {
    check(new OrganizeImports(), privateMember)(
      """|import a.{b, _}
        |object A { private def ¦c = 1 }""".stripMargin,
      """|import a._
        |import a.b
        |object A { private def c = 1 }""".stripMargin
    )
  }

  test("unimport clause beside an unused clause") {
    checkUnused(unusedImport, "import a.{b => _}, c.¦d", "import a.{b => _}")
  }

  test("unused unimport beside another unimport") {
    checkUnused(unusedImport, "import a.{¦b => _, c => _}", "import a.{c => _}")
  }

  test("local unimport beside a wildcard, organized") {
    check(new OrganizeImports(), unusedImport)(
      "object A { import a.{b => _, ¦_} }",
      "object A { import a.{b => _} }"
    )
  }

  test(
    "local unimport beside a wildcard with the range of the import, organized"
  ) {
    check(new OrganizeImports(), unusedImport, "import a.{b => _, _}")(
      "object A { import a.{b => _, _} }",
      "object A { }"
    )
  }

  test("Scala 3 unimport alone with the range of the importee") {
    check3(new RemoveUnused(), unusedImport, "b as _")(
      "import a.{b as _}",
      ""
    )
  }

  test("Scala 3 unimport alone with the range of the importee, organized") {
    check3(organizer3, unusedImport, "b as _")(
      """|import a.{b as _}
        |object A""".stripMargin,
      """|
        |object A""".stripMargin
    )
  }

  test("Scala 3 unimports with the ranges of the importees") {
    check3(new RemoveUnused(), unusedImport, "b as _", "c as _")(
      "import a.{b as _, c as _}",
      ""
    )
  }

  test("Scala 3 unimports with the ranges of the importees, organized") {
    check3(
      organizer3,
      unusedImport,
      "b as _",
      "c as _"
    )(
      """|import a.{b as _, c as _}
        |object A""".stripMargin,
      """|
        |object A""".stripMargin
    )
  }

  test("Scala 3 unimport beside a wildcard with the ranges of the importees") {
    check3(new RemoveUnused(), unusedImport, "b as _", "*")(
      "import a.{b as _, *}",
      ""
    )
  }

  test(
    "Scala 3 unimport beside a wildcard with the ranges of the importees, organized"
  ) {
    check3(
      organizer3,
      unusedImport,
      "b as _",
      "*"
    )(
      """|import a.{b as _, *}
        |object A""".stripMargin,
      """|
        |object A""".stripMargin
    )
  }

  test(
    "Scala 3 unimport beside a used wildcard with the range of the importee"
  ) {
    check3(new RemoveUnused(), unusedImport, "b as _")(
      "import a.{b as _, *}",
      "import a.*"
    )
  }

  test(
    "Scala 3 unimport beside a used wildcard with the range of the importee, organized"
  ) {
    check3(organizer3, unusedImport, "b as _")(
      """|import a.{b as _, *}
        |object A""".stripMargin,
      """|import a.*
        |object A""".stripMargin
    )
  }

  test("Scala 3 unimport beside a used name with the range of the importee") {
    check3(new RemoveUnused(), unusedImport, "b as _")(
      "import a.{b as _, c}",
      "import a.c"
    )
  }

  test(
    "Scala 3 unimport beside a used name with the range of the importee, organized"
  ) {
    check3(organizer3, unusedImport, "b as _")(
      """|import a.{b as _, c}
        |object A""".stripMargin,
      """|import a.c
        |object A""".stripMargin
    )
  }

  test("Scala 3 given wildcard with the range of the importee") {
    check3(new RemoveUnused(), unusedImport, "given")(
      "import a.given",
      ""
    )
  }

  test("Scala 3 given wildcard with the range of the importee, organized") {
    check3(organizer3, unusedImport, "given")(
      """|import a.given
        |object A""".stripMargin,
      """|
        |object A""".stripMargin
    )
  }

  test("Scala 3 given import with the range of the importee") {
    check3(new RemoveUnused(), unusedImport, "given B")(
      "import a.{given B}",
      ""
    )
  }

  test("Scala 3 given import with the range of the importee, organized") {
    check3(organizer3, unusedImport, "given B")(
      """|import a.{given B}
        |object A""".stripMargin,
      """|
        |object A""".stripMargin
    )
  }

  test("Scala 3 given import beside a used given import") {
    check3(new RemoveUnused(), unusedImport, "given B")(
      "import a.{given B, given C}",
      "import a.{given C}"
    )
  }

  test("Scala 3 given import beside a used given import, organized") {
    check3(organizer3, unusedImport, "given B")(
      """|import a.{given B, given C}
        |object A""".stripMargin,
      """|import a.given C
        |object A""".stripMargin
    )
  }

  test(
    "Scala 3 given wildcard beside a name with the ranges of the importees"
  ) {
    check3(new RemoveUnused(), unusedImport, "b", "given")(
      "import a.{b, given}",
      ""
    )
  }

  test(
    "Scala 3 given wildcard beside a name with the ranges of the importees, organized"
  ) {
    check3(
      organizer3,
      unusedImport,
      "b",
      "given"
    )(
      """|import a.{b, given}
        |object A""".stripMargin,
      """|
        |object A""".stripMargin
    )
  }

  test("Scala 3 given import beside a used name") {
    check3(new RemoveUnused(), unusedImport, "given C")(
      "import a.{b, given C}",
      "import a.b"
    )
  }

  test("Scala 3 given import beside a used name, organized") {
    check3(organizer3, unusedImport, "given C")(
      """|import a.{b, given C}
        |object A""".stripMargin,
      """|import a.b
        |object A""".stripMargin
    )
  }

  test("Scala 3 given wildcard beside a used wildcard") {
    check3(new RemoveUnused(), unusedImport, "given")(
      "import a.{*, given}",
      "import a.*"
    )
  }

  test("Scala 3 given wildcard beside a used wildcard, organized") {
    check3(organizer3, unusedImport, "given")(
      """|import a.{*, given}
        |object A""".stripMargin,
      """|import a.*
        |object A""".stripMargin
    )
  }

  test("imports, organized") {
    implicit val doc: SemanticDocument =
      document(unusedImport, "import a.{¦b, c}, d.{¦e => f, g}, h.¦_", Nil)
    val unused = new OrganizeImports.UnusedImporteePositions
    val importees = doc.tree.collect { case i: Importee => i }
    assertEquals(
      importees.filter(unused(_)).map(_.syntax),
      List("b", "e => f", "_")
    )
  }

  test("imports with the ranges of the import and the importee, organized") {
    implicit val doc: SemanticDocument = document(
      unusedImport,
      "import a.{b => c, d}; import e.{f, _}",
      Seq("b => c", "import e.{f, _}")
    )
    val unused = new OrganizeImports.UnusedImporteePositions
    val importees = doc.tree.collect { case i: Importee => i }
    assertEquals(
      importees.filter(unused(_)).map(_.syntax),
      List("b => c", "f", "_")
    )
  }

  test("imports with the ranges of each clause, organized") {
    implicit val doc: SemanticDocument = document(
      unusedImport,
      "import a.{b, _}, c.d; import e.f, g.{h, _}",
      Seq("import a.{b, _}", "g.{h, _}")
    )
    val unused = new OrganizeImports.UnusedImporteePositions
    val importees = doc.tree.collect { case i: Importee => i }
    assertEquals(
      importees.filter(unused(_)).map(_.syntax),
      List("b", "_", "h", "_")
    )
  }

  test("imports without an unused import, organized") {
    implicit val doc: SemanticDocument =
      document(
        privateMember,
        "import a.{b, _}; object A { private def ¦c = 1 }",
        Nil
      )
    val unused = new OrganizeImports.UnusedImporteePositions
    val importees = doc.tree.collect { case i: Importee => i }
    assertEquals(importees.filter(unused(_)).map(_.syntax), Nil)
  }

  test("tupled arguments") {
    check(new NoAutoTupling(), tupling)(
      "object A { a¦(1, 2) }",
      "object A { a((1, 2)) }"
    )
  }

  test("tupled arguments in two clauses") {
    check(new NoAutoTupling(), tupling)(
      "object A { a¦(1, 2)¦(3, 4) }",
      "object A { a((1, 2))((3, 4)) }"
    )
  }

  test("tupled inner call with the range of the application") {
    check(new NoAutoTupling(), tupling, "a(1, 2)")(
      "object A { a(1, 2)(3, 4) }",
      "object A { a((1, 2))(3, 4) }"
    )
  }

  test("tupled outer call with the range of the application") {
    check(new NoAutoTupling(), tupling, "a(1, 2)(3, 4)")(
      "object A { a(1, 2)(3, 4) }",
      "object A { a(1, 2)((3, 4)) }"
    )
  }

  test("tupled two calls with the ranges of the applications") {
    check(new NoAutoTupling(), tupling, "a(1, 2)", "a(1, 2)(3, 4)")(
      "object A { a(1, 2)(3, 4) }",
      "object A { a((1, 2))((3, 4)) }"
    )
  }

  test("tupled arguments of a selected method") {
    check(new NoAutoTupling(), tupling)(
      "object A { a.b¦(1, 2) }",
      "object A { a.b((1, 2)) }"
    )
  }

  test(
    "tupled arguments of a selected method with the range of the application"
  ) {
    check(new NoAutoTupling(), tupling, "a.b(1, 2)")(
      "object A { a.b(1, 2) }",
      "object A { a.b((1, 2)) }"
    )
  }

  test("tupled arguments with a type argument") {
    check(new NoAutoTupling(), tupling)(
      "object A { a[Int]¦(1, 2) }",
      "object A { a[Int]((1, 2)) }"
    )
  }

  test(
    "tupled arguments with a type argument and the range of the application"
  ) {
    check(new NoAutoTupling(), tupling, "a[Int](1, 2)")(
      "object A { a[Int](1, 2) }",
      "object A { a[Int]((1, 2)) }"
    )
  }

  test("tupled arguments with a block") {
    check(new NoAutoTupling(), tupling)(
      "object A { a¦(1, { 2 }) }",
      "object A { a((1, { 2 })) }"
    )
  }

  test("tupled arguments with a block and the range of the application") {
    check(new NoAutoTupling(), tupling, "a(1, { 2 })")(
      "object A { a(1, { 2 }) }",
      "object A { a((1, { 2 })) }"
    )
  }

  test("tupled infix arguments") {
    check(new NoAutoTupling(), tupling)(
      "object A { a ¦b (1, 2) }",
      "object A { a b (1, 2) }"
    )
  }

  test("tupled infix arguments with the range of the application") {
    check(new NoAutoTupling(), tupling, "a b (1, 2)")(
      "object A { a b (1, 2) }",
      "object A { a b (1, 2) }"
    )
  }

  test("inserted unit") {
    check(new NoAutoTupling(), unitInsertion)(
      "object A { a¦() }",
      "object A { a(()) }"
    )
  }

  test("inserted unit with the range of the application") {
    check(new NoAutoTupling(), unitInsertion, "a()")(
      "object A { a() }",
      "object A { a(()) }"
    )
  }

  test("inserted unit of a selected method") {
    check(new NoAutoTupling(), unitInsertion)(
      "object A { a.b¦() }",
      "object A { a.b(()) }"
    )
  }

  test("inserted unit of a selected method with the range of the application") {
    check(new NoAutoTupling(), unitInsertion, "a.b()")(
      "object A { a.b() }",
      "object A { a.b(()) }"
    )
  }
}
