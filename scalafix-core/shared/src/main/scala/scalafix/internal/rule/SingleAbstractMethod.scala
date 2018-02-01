package scalafix.internal.rule

import scala.meta._
import scala.meta.Token._
import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule

import scala.collection.mutable

// SAM conversion: http://www.scala-lang.org/files/archive/spec/2.12/06-expressions.html#sam-conversion
case class SingleAbstractMethod(index: SemanticdbIndex)
    extends SemanticRule(index, "SingleAbstractMethod") {
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
              List(Defn.Def(_, method, _, List(params), _, body))
            )
            ) if !visited.contains(tree) =>
          visited += tree

          val singleAbstractOverride =
            (for {
              definition <- index.denotation(method)
              overrideSymbol <- definition.overrides.headOption
              if definition.overrides.size == 1
              overrideDefinition <- index.denotation(overrideSymbol)
            } yield overrideDefinition.isAbstract).getOrElse(false)

          val singleMember =
            (for {
              symbol <- index.symbol(clazz)
              denfinition <- index.denotation(symbol)
            } yield denfinition.members.size == 1).getOrElse(false)

          singleAbstractOverride && singleMember

        case _ => false
      }

    class PatchBuilder(tokens: Tokens) {
      private val itt = tokens.iterator
      private val patches = List.newBuilder[Patch]
      def skip(f: Token => Boolean, kw: String): Unit =
        doOp(itt.find(f), _ => (), kw)
      def remove(f: Token => Boolean, kw: String): Unit =
        doRemove(itt.find(f), kw)
      def removeLast(f: Token => Boolean, kw: String): Unit = {
        var last: Option[Token] = None
        while (itt.hasNext) {
          last = itt.find(f)
        }
        doRemove(last, kw)
      }
      def addRight(f: Token => Boolean, toAdd: String, kw: String): Unit =
        doOp(itt.find(f), tt => patches += ctx.addRight(tt, toAdd), kw)
      def doRemove(t: Option[Token], kw: String): Unit =
        doOp(t, tt => patches += ctx.removeToken(tt), kw)
      def doOp(t: Option[Token], op: Token => Unit, kw: String): Unit = {
        t match {
          case Some(t) => op(t)
          case _ =>
            throw new Exception(
              s"""|cannot find $kw
                  |Tokens:
                  |$tokens""".stripMargin
            )
        }
      }
      def result(): Patch = Patch.fromIterable(patches.result())
    }

    def patchSam(builder: PatchBuilder, keepClassName: Boolean): Patch = {
      // new X(){def m(a: A, b: B, c: C): D = <body> }
      builder.remove(_.is[KwNew], "new")
      if (!keepClassName) {
        builder.remove(_.is[Ident], "<ident>") // X
      } else {
        builder.addRight(_.is[Ident], " =", "<ident>") // X =
      }
      // BUG: () are optionnals
      // val c1 = new C() { def f(a: Int): Int = a }

      builder.remove(_.is[LeftParen], "(")
      builder.remove(_.is[RightParen], ")")

      builder.remove(_.is[LeftBrace], "{")
      builder.remove(_.is[KwDef], "def")
      builder.remove(_.is[Ident], "<ident>") // methodName
      // TODO: it's possible to remove the types in the parameter list
      // val withParams: WithParams = (a, b) => a + b

      // TODO: it's possible to remove the () when it has only one argument
      // val a: A = x => x

      // BUG: the RightParen is optionnal
      // val runnable1 = new Runnable(){ def run: Unit = println("runnable1!") }

      builder.addRight(_.is[RightParen], " => ", ")") // ) ends the parameter list
      builder.remove(_.is[Colon], ":")
      builder.remove(_.is[Ident], "<ident>") // D
      builder.remove(_.is[Equals], "=")
      builder.removeLast(_.is[RightBrace], "}")
      builder.result()
    }

    def patchSamDefn(hasDecltpe: Boolean, tokens: Tokens): Patch = {
      if (!hasDecltpe) {
        // val v = new X(){def m(a: A, b: B, c: C): D = <body> }
        // val v: X = (a: A, b: B, c: C) => <body>
        val builder = new PatchBuilder(tokens)
        builder.addRight(_.is[Ident], ":", "<ident>")
        builder.remove(_.is[Equals], "=")
        patchSam(builder, keepClassName = true)
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
        patchSamDefn(term.decltpe.nonEmpty, term.tokens)
      case term: Defn.Var if term.rhs.map(isSam).getOrElse(false) =>
        patchSamDefn(term.decltpe.nonEmpty, term.tokens)
      case term: Defn.Def if isSam(term.body) =>
        patchSamDefn(term.decltpe.nonEmpty, term.tokens)
      case term: Term.NewAnonymous if isSam(term) =>
        patchSam(new PatchBuilder(term.tokens), keepClassName = false)

    }.asPatch
  }
}
