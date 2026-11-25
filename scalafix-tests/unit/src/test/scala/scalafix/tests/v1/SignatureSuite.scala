package scalafix.tests.v1

import scala.meta.inputs.Input
import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.{semanticdb => s}

import munit.FunSuite
import scalafix.internal.config.ScalaVersion
import scalafix.internal.v0.LegacyCodePrinter
import scalafix.internal.v1.InternalSemanticDoc
import scalafix.v1._

class SignatureSuite extends FunSuite {

  private def ref(symbol: String): s.Type = s.TypeRef(s.NoType, symbol, Nil)

  private val ret = ref("_empty_/R#")

  private def sinfo(
      symbol: String,
      displayName: String,
      kind: s.SymbolInformation.Kind,
      signature: s.Signature,
      properties: Int = 0
  ): s.SymbolInformation =
    s.SymbolInformation(
      symbol = symbol,
      kind = kind,
      properties = properties,
      displayName = displayName,
      signature = signature
    )

  /** A member of `_empty_/A#`, with the infos of its parameters. */
  private class Method(
      name: String,
      val symbol: String,
      kind: s.SymbolInformation.Kind = s.SymbolInformation.Kind.METHOD,
      properties: Int = 0,
      val returnType: s.Type = ret
  ) {
    def apply(signature: s.Signature): s.SymbolInformation =
      sinfo(symbol, name, kind, signature, properties)
    def param(name: String, tpe: String): s.SymbolInformation =
      sinfo(
        Symbols.Global(symbol, Descriptor.Parameter(name)),
        name,
        s.SymbolInformation.Kind.PARAMETER,
        s.ValueSignature(ref(tpe))
      )
    def tparam(name: String): s.SymbolInformation =
      sinfo(
        Symbols.Global(symbol, Descriptor.TypeParameter(name)),
        name,
        s.SymbolInformation.Kind.TYPE_PARAMETER,
        s.TypeSignature(None, ref("scala/Nothing#"), ref("scala/Any#"))
      )
  }

  private def method(name: String): Method =
    new Method(
      name,
      Symbols.Global("_empty_/A#", Descriptor.Method(name, "()"))
    )

  private def v1Signature(
      info: s.SymbolInformation,
      symbols: Seq[s.SymbolInformation]
  ): Signature = {
    implicit val symtab: Symtab = document(symbols)
    new SymbolInformation(info).signature
  }

  private def document(symbols: Seq[s.SymbolInformation]): InternalSemanticDoc =
    new InternalSemanticDoc(
      SyntacticDocument.fromInput(
        Input.VirtualFile("A.scala", ""),
        ScalaVersion.scala2
      ),
      s.TextDocument(symbols = symbols),
      _ => None
    )

  /** Returns the symbols of a method signature, else its class name. */
  private def structure(sig: Signature): String = sig match {
    case sig: MethodSignature =>
      val typeParameters = sig.typeParameters.map(_.symbol.value)
      val parameterLists = sig.parameterLists.map(_.map(_.symbol.value))
      s"MethodSignature($typeParameters, $parameterLists, ${sig.returnType})"
    case _ => sig.productPrefix
  }

  private case class Readers(
      v1: String,
      signature: String,
      info: String,
      v0: String
  )

  /**
   * Returns each reader's output for `info`, with `symbols` in the document.
   */
  private def readers(
      info: s.SymbolInformation,
      symbols: Seq[s.SymbolInformation]
  ): Readers = {
    implicit val sdoc: InternalSemanticDoc = document(symbols)
    val v1 = new SymbolInformation(info)
    val v0 = new LegacyCodePrinter(new SemanticDocument(sdoc))
      .convertDenotation(info.signature, 0, info.displayName)
    Readers(
      structure(v1.signature),
      v1.signature.toString,
      v1.toString,
      v0.signature
    )
  }

  private val ctor = new Method(
    "<init>",
    Symbols.Global("_empty_/A#", Descriptor.Method("<init>", "()")),
    s.SymbolInformation.Kind.CONSTRUCTOR,
    returnType = s.NoType
  )

  test("constructor without parameters") {
    // class A()
    val obtained = readers(
      ctor(s.MethodSignature(Some(s.Scope()), List(s.Scope()), s.NoType)),
      Nil
    )
    assertEquals(obtained.signature, "()")
    assertEquals(obtained.v0, "")
  }

  test("constructor without clauses") {
    // a Java constructor without parameters
    val obtained =
      readers(ctor(s.MethodSignature(Some(s.Scope()), Nil, s.NoType)), Nil)
    assertEquals(obtained.signature, "")
    assertEquals(obtained.v0, "")
  }

  test("constructor with an implicit clause") {
    // class A(x: B)(implicit o: C)
    val x = ctor.param("x", "_empty_/B#")
    val o =
      ctor
        .param("o", "_empty_/C#")
        .copy(properties = s.SymbolInformation.Property.IMPLICIT.value)
    val paramss = List(s.Scope(List(x.symbol)), s.Scope(List(o.symbol)))
    val obtained = readers(
      ctor(s.MethodSignature(Some(s.Scope()), paramss, s.NoType)),
      List(x, o)
    )
    assertEquals(obtained.signature, "(x: B)(implicit o: C)")
    assertEquals(
      obtained.info,
      "_empty_/A#`<init>`(). => ctor <init>(x: B)(implicit o: C)"
    )
    assertEquals(obtained.v0, "(x: B)(implicit o: C)")
  }

  /** `AnyRef { member }`, with `member` as a hardlink. */
  private def refinement(member: s.SymbolInformation): s.Type =
    s.StructuralType(
      ref("scala/AnyRef#"),
      Some(s.Scope(hardlinks = List(member)))
    )

  /**
   * Returns the signature of the refinement member in the return type of `sig`.
   */
  private def member(sig: Signature): Signature = sig match {
    case MethodSignature(_, _, StructuralType(_, List(decl))) => decl.signature
    case _ => fail(s"no refinement: $sig")
  }

  test("implicit and using parameter clauses") {
    // def f(x: B)(implicit o: C): R, and the same with `using`
    val m = method("f")
    val x = m.param("x", "_empty_/B#")
    def signature(property: s.SymbolInformation.Property) = {
      val o = m.param("o", "_empty_/C#").copy(properties = property.value)
      val paramss = List(s.Scope(List(x.symbol)), s.Scope(List(o.symbol)))
      readers(m(s.MethodSignature(Some(s.Scope()), paramss, ret)), List(x, o))
    }
    assertEquals(
      signature(s.SymbolInformation.Property.IMPLICIT).signature,
      "(x: B)(implicit o: C): R"
    )
    assertEquals(
      signature(s.SymbolInformation.Property.GIVEN).signature,
      "(x: B)(using o: C): R"
    )
  }

  test("using parameter clause with dotc's properties") {
    // def f(x: B)(using o: C, p: D): R
    val m = method("f")
    val x = m.param("x", "_empty_/B#")
    val using =
      s.SymbolInformation.Property.GIVEN.value | s.SymbolInformation.Property.IMPLICIT.value
    val o = m.param("o", "_empty_/C#").copy(properties = using)
    val p = m.param("p", "_empty_/D#").copy(properties = using)
    val paramss =
      List(s.Scope(List(x.symbol)), s.Scope(List(o.symbol, p.symbol)))
    // TODO: metap prints `implicit` before each parameter
    assertEquals(
      readers(m(s.MethodSignature(None, paramss, ret)), List(x, o, p)),
      Readers(
        "MethodSignature(List(), List(List(_empty_/A#f().(x)), List(_empty_/A#f().(o), _empty_/A#f().(p))), R)",
        "(x: B)(using o: C,p: D): R",
        "_empty_/A#f(). => method f(x: B)(implicit o: C, implicit p: D): R",
        "(x: B)(using o: C, p: D)R"
      )
    )
  }

  test("implicit parameter clause") {
    // def f(x: B)(implicit o: C): R
    val m = method("f")
    val x = m.param("x", "_empty_/B#")
    val o =
      m.param("o", "_empty_/C#")
        .copy(properties = s.SymbolInformation.Property.IMPLICIT.value)
    val paramss = List(s.Scope(List(x.symbol)), s.Scope(List(o.symbol)))
    val obtained =
      readers(m(s.MethodSignature(Some(s.Scope()), paramss, ret)), List(x, o))
    assertEquals(
      obtained.info,
      "_empty_/A#f(). => method f(x: B)(implicit o: C): R"
    )
    assertEquals(obtained.v0, "(x: B)(implicit o: C)R")
  }

  test("hardlinked parameter of a refinement member") {
    // def g: AnyRef { def h(x: B): R }
    val x =
      sinfo(
        "local1",
        "x",
        s.SymbolInformation.Kind.PARAMETER,
        s.ValueSignature(ref("_empty_/B#"))
      )
    val paramss = List(s.Scope(hardlinks = List(x)))
    val h =
      sinfo(
        "local2",
        "h",
        s.SymbolInformation.Kind.METHOD,
        s.MethodSignature(None, paramss, ret)
      )
    val g = method("g")(s.MethodSignature(Some(s.Scope()), Nil, refinement(h)))
    implicit val symtab: Symtab = document(Nil)
    assertEquals(
      new SymbolInformation(g).toString,
      "_empty_/A#g(). => method g: AnyRef { def h(x: B): R }"
    )
    assertEquals(readers(g, Nil).v0, "AnyRef(x: B)R")
  }

  test("implicit and using clauses of a refinement member") {
    // def g: AnyRef { def h(implicit o: C): R }, and the same with `using`
    def obtained(properties: Int) = {
      val o = sinfo(
        "local1",
        "o",
        s.SymbolInformation.Kind.PARAMETER,
        s.ValueSignature(ref("_empty_/C#")),
        properties
      )
      val paramss = List(s.Scope(hardlinks = List(o)))
      val h =
        sinfo(
          "local2",
          "h",
          s.SymbolInformation.Kind.METHOD,
          s.MethodSignature(None, paramss, ret)
        )
      val sig = s.MethodSignature(Some(s.Scope()), Nil, refinement(h))
      readers(method("g")(sig), Nil).v0
    }
    assertEquals(
      obtained(s.SymbolInformation.Property.IMPLICIT.value),
      "AnyRef(implicit o: C)R"
    )
    assertEquals(
      obtained(
        s.SymbolInformation.Property.GIVEN.value | s.SymbolInformation.Property.IMPLICIT.value
      ),
      "AnyRef(using o: C)R"
    )
  }

  test("hardlinked parameter with the symbol of a parameter in the document") {
    // def g(x: B): AnyRef { def h(x: C): R }, in dotc's layout
    val g = method("g")
    val x = g.param("x", "_empty_/B#")
    val hx = g.param("x", "_empty_/C#")
    val paramss = List(s.Scope(hardlinks = List(hx)))
    val h =
      sinfo(
        "local0",
        "h",
        s.SymbolInformation.Kind.METHOD,
        s.MethodSignature(None, paramss, ret)
      )
    val info = g(
      s.MethodSignature(None, List(s.Scope(List(x.symbol))), refinement(h))
    )
    member(v1Signature(info, List(x))) match {
      case MethodSignature(Nil, List(List(param)), _) =>
        assertEquals(param.signature.toString, "C")
      case sig => fail(s"unexpected member: $sig")
    }
    // TODO: metap reads the parameter from the document
    assertEquals(
      readers(info, List(x)),
      Readers(
        "MethodSignature(List(), List(List(_empty_/A#g().(x))), AnyRef {local0 => method h(x: B): R})",
        "(x: B): AnyRef {local0 => method h(x: B): R}",
        "_empty_/A#g(). => method g(x: B): AnyRef { def h(x: B): R }",
        "(x: B)AnyRef(x: C)R"
      )
    )
  }

  test("hardlinked type parameter of a refinement member") {
    // def g: AnyRef { def h[T]: R }
    val T = sinfo(
      "local1",
      "T",
      s.SymbolInformation.Kind.TYPE_PARAMETER,
      s.TypeSignature(None, ref("scala/Nothing#"), ref("scala/Any#"))
    )
    val tparams = Some(s.Scope(hardlinks = List(T)))
    val h =
      sinfo(
        "local2",
        "h",
        s.SymbolInformation.Kind.METHOD,
        s.MethodSignature(tparams, Nil, ret)
      )
    val g = method("g")(s.MethodSignature(Some(s.Scope()), Nil, refinement(h)))
    assertEquals(
      readers(g, Nil),
      Readers(
        "MethodSignature(List(), List(), AnyRef {local2 => method h[T]: R})",
        ": AnyRef {local2 => method h[T]: R}",
        "_empty_/A#g(). => method g: AnyRef { def h[T]: R }",
        "AnyRef[T] => R"
      )
    )
  }

  test("type parameter of a method") {
    // def f[T](x: B): R
    val m = method("f")
    val T = m.tparam("T")
    val x = m.param("x", "_empty_/B#")
    val tparams = Some(s.Scope(List(T.symbol)))
    val paramss = List(s.Scope(List(x.symbol)))
    val obtained =
      readers(m(s.MethodSignature(tparams, paramss, ret)), List(T, x))
    assertEquals(obtained.v0, "[T] => (x: B)R")
  }

  test("local method") {
    // def h[T](x: B): R, inside a block
    val T = sinfo(
      "local1",
      "T",
      s.SymbolInformation.Kind.TYPE_PARAMETER,
      s.TypeSignature(None, ref("scala/Nothing#"), ref("scala/Any#"))
    )
    val x =
      sinfo(
        "local2",
        "x",
        s.SymbolInformation.Kind.PARAMETER,
        s.ValueSignature(ref("_empty_/B#"))
      )
    val sig = s.MethodSignature(
      Some(s.Scope(List(T.symbol))),
      List(s.Scope(List(x.symbol))),
      ret
    )
    val h = sinfo("local0", "h", s.SymbolInformation.Kind.METHOD, sig)
    assertEquals(
      readers(h, List(h, T, x)),
      Readers(
        "MethodSignature(List(local1), List(List(local2)), R)",
        "[T](x: B): R",
        "local0 => method h[T](x: B): R",
        "[T] => (x: B)R"
      )
    )
  }

}
