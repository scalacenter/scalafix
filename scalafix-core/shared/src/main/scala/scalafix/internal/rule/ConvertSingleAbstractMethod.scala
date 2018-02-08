package scalafix.internal.rule

import scala.meta._
import scala.meta.Token._

import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule

import scala.collection.mutable

import scalafix.internal.util.PatchBuilder

/* 
see http://www.scala-lang.org/files/archive/spec/2.12/06-expressions.html#sam-conversion

SAM Conversion

An expression (p1, ..., pN) => body of function type (T1, ..., TN) => T is 
sam-convertible to the expected type S if the following holds:

* [ ] SAM1: the class C of S declares an abstract method m with signature (p1: A1, ..., pN: AN): R;
* [ ] SAM2: besides m, C must not declare or inherit any other deferred value members;
* [ ] SAM3: the method m must have a single argument list;
* [ ] SAM4: there must be a type U that is a subtype of S, so that the expression 
            new U { final def m(p1: A1, ..., pN: AN): R = body } is well-typed 
            (conforming to the expected type S);
* [ ] SAM5: for the purpose of scoping, m should be considered a static member
            (U's members are not in scope in body);
* [ ] SAM6: (A1, ..., AN) => R is a subtype of (T1, ..., TN) => T
            (satisfying this condition drives type inference of unknown type parameters in S);

Note that a function literal that targets a SAM is not necessarily compiled to
the above instance creation expression. This is platform-dependent.

It follows that:

* [ ] SAM7:  if class C defines a constructor, it must be accessible and must define exactly one,
             empty, argument list;
* [ ] SAM8:  class C cannot be final or sealed (for simplicity we ignore the possibility of SAM
             conversion in the same compilation unit as the sealed class);
* [ ] SAM9:  m cannot be polymorphic;
* [ ] SAM10: it must be possible to derive a fully-defined type U from S by inferring any unknown
             type parameters of C.

Finally, we impose some implementation restrictions (these may be lifted in future releases):

* [ ] SAM11: C must not be nested or local (it must not capture its environment, as that results in a zero-argument constructor)
* [ ] SAM12: C's constructor must not have an implicit argument list (this simplifies type inference);
* [ ] SAM13: C must not declare a self type (this simplifies type inference);
* [ ] SAM14: C must not be @specialized.

*/
case class ConvertSingleAbstractMethod(index: SemanticdbIndex)
    extends SemanticRule(index, "ConvertSingleAbstractMethod") {
  override def description: String = ???
  override def fix(ctx: RuleCtx): Patch = {
    val visited = mutable.Set.empty[Tree]
    def isSam(tree: Tree): Boolean =
      tree match {
        case Term.NewAnonymous(
            Template(
              _,
              List(Init(clazz, _, _)),
              _,
              List(Defn.Def(_, method, _, paramss, _, _))
            )
            )
            if !visited.contains(tree) && (paramss.size == 1 || paramss.size == 0) =>

          visited += tree

          val singleAbstractOverride =
            (for {
              definition <- index.denotation(method)
              overrideSymbol <- definition.overrides.headOption
              if definition.overrides.size == 1
              overrideDefinition <- index.denotation(overrideSymbol)
            } yield {
              overrideDefinition.isAbstract &&
              overrideDefinition.signature.startsWith("(") // https://github.com/scala/bug/issues/10555
            }).getOrElse(false)

          val singleMember =
            (for {
              symbol <- index.symbol(clazz)
              denfinition <- index.denotation(symbol)
            } yield denfinition.members.size == 1).getOrElse(false)

          singleAbstractOverride && singleMember

        case _ => false
      }

    def getMods(tree: Tree): List[Mod] = {
      tree match {
        case Term.NewAnonymous(
            Template(
              _,
              _,
              _,
              List(Defn.Def(mods, _, _, _, _, _))
            )
            ) =>
          mods
        case _ => Nil
      }
    }

    def patchSam(
        builder: PatchBuilder,
        mods: List[Mod],
        keepClassName: Boolean): Patch = {
      import builder._

      mods.foreach(_.tokens.foreach(remove))

      // new X(){def m(a: A, b: B, c: C): D = <body> }
      remove[KwNew]
      remove[Space]
      if (!keepClassName) {
        remove[Ident]
      } else {
        addRight[Ident](" =") // class name
      }
      removeOptional[LeftParen]
      removeOptional[RightParen]

      remove[LeftBrace]
      remove[KwDef]
      remove[Space]
      remove[Ident] // method name

      def patchParams(hasParams: Boolean): (Token, Boolean) = {
        find { t =>
            t.is[LeftParen] ||
              t.is[Colon] ||
              t.is[LeftBrace] ||
              t.is[Equals] } match {
          case Some(t) => {
            if (t.is[LeftParen]) {
              // parameter list
              val open = t
              var i = 0
              var cur = next
              while (!cur.is[RightParen]) {
                remove[Colon]
                removeOptional[Space]
                remove[Ident]
                i += 1
                cur = next
              }
              // (a) becomes a
              if (i == 1) {
                remove(open)
                remove(cur)
              }
              patchParams(hasParams = true)
            } else if (t.is[Colon]) {
              remove(t)
              removeOptional[Space]
              remove[Ident]
              removeOptional[Space]
              remove[Equals]
              removeOptional[Space]
              (next, hasParams)
            } else if (t.is[LeftBrace]) {
              (t, hasParams)
            } else if (t.is[Equals]) {
              remove(t)
              (next, hasParams)
            } else {
              throw new Exception("cannot find sam method's body")
            }
          }
          case _ => throw new Exception("cannot find sam method's : or {")
        }
      }
      val (body, hasParams0) = patchParams(hasParams = false)

      val lambdaParams =
        if (hasParams0) ""
        else "()"

      addLeft(body, s"$lambdaParams => ")
      removeLast[RightBrace]
      result()
    }

    def patchSamDefn(
        hasDecltpe: Boolean,
        tokens: Tokens,
        mods: List[Mod]): Patch = {
      if (!hasDecltpe) {
        val builder = new PatchBuilder(tokens, ctx)
        import builder._

        addRight[Ident](":")
        remove[Equals]
        removeOptional[Space]
        patchSam(builder, mods, keepClassName = true)
      } else {
        // trait B { def f(a: Int): Int }
        // trait C extends B
        // val c2: B = new C { def f(a: Int): Int = a }

        // what do we do here ?
        // val c2: C would expose C
        // val c2: B would remove C

        Patch.empty
      }
    }

    ctx.tree.collect {
      case term: Defn.Val if isSam(term.rhs) =>
        patchSamDefn(term.decltpe.nonEmpty, term.tokens, getMods(term.rhs))
      case term: Defn.Var if term.rhs.map(isSam).getOrElse(false) =>
        patchSamDefn(
          term.decltpe.nonEmpty,
          term.tokens,
          term.rhs.map(getMods).getOrElse(Nil))
      case term: Defn.Def if isSam(term.body) =>
        patchSamDefn(term.decltpe.nonEmpty, term.tokens, getMods(term.body))
      case term: Term.NewAnonymous if isSam(term) =>
        patchSam(
          new PatchBuilder(term.tokens, ctx),
          getMods(term),
          keepClassName = false)

    }.asPatch
  }
}
