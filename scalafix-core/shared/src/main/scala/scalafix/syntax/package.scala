package scalafix

import java.nio.charset.Charset
import java.nio.file.Paths
import scala.collection.IterableLike
import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.meta._
import scala.meta.semanticdb.Signature
import scala.meta.semanticdb.Symbol
import scala.util.Success
import scala.util.Try
import org.scalameta.logger
import scala.compat.Platform.EOL
import scala.meta.internal.io.PathIO
import scala.meta.internal.scalafix.ScalafixScalametaHacks
import scalafix.internal.util.SymbolOps
import scalafix.util.TreeOps

package object syntax {

  implicit class XtensionIterableLike[A, Repr](xs: IterableLike[A, Repr]) {
    def distinctBy[B, That](f: A => B)(
        implicit cbf: CanBuildFrom[Repr, A, That]): That = {
      val builder = cbf(xs.repr)
      val set = mutable.HashSet.empty[B]
      xs.foreach { o =>
        val b = f(o)
        if (!set(b)) {
          set += b
          builder += o
        }
      }
      builder.result
    }
  }

  implicit class XtensionTryToEither[A](e: Try[A]) {
    def asEither: Either[Throwable, A] = e match {
      case Success(x) => Right(x)
      case scala.util.Failure(x) => Left(x)
    }
  }

  implicit class XtensionEither[A, B](either: Either[A, B]) {
    def map[C](f: B => C): Either[A, C] = either.right.map(f)
    def flatMap[C >: A, D](f: B => Either[C, D]): Either[C, D] =
      either.right.flatMap(f)
  }

  implicit class XtensionEitherThrowable[A](either: Either[Throwable, A]) {
    def leftAsString: Either[String, A] = either.left.map(_.getMessage)
    def get: A = either match {
      case Right(b) => b
      case Left(e) => throw e
    }
  }

  implicit class XtensionEitherString[B](either: Either[String, B]) {
    def get: B = either match {
      case Right(b) => b
      case Left(e) => throw new Exception(e)
    }
  }

  implicit class XtensionRefSymbolOpt(ref: Ref)(implicit mirror: Database) {
    def symbolOpt: Option[Symbol] = mirror.names.collectFirst {
      case ResolvedName(pos, sym, _) if pos == ref.pos => sym
    }
    def symbol: Symbol = symbolOpt.get
  }

  implicit class XtensionParsedOpt[T](parsed: Parsed[T]) {
    def toOption: Option[T] = parsed match {
      case parsers.Parsed.Success(tree) => Some(tree)
      case _ => None
    }
  }

  implicit class XtensionTermRef(ref: Term.Ref) {
    def toTypeRef: Type.Ref = ref match {
      case Term.Name(name) => Type.Name(name)
      case Term.Select(qual: Term.Ref, Term.Name(name)) =>
        Type.Select(qual, Type.Name(name))
      case _ => ref.syntax.parse[Type].get.asInstanceOf[Type.Ref]
    }
  }

  implicit class XtensionSymbolMirror(symbol: Symbol)(implicit mirror: Database) {
    def denotOpt: Option[Denotation] = mirror.symbols.collectFirst {
      case ResolvedSymbol(sym, denot) if sym == symbol => denot
    }
  }
  implicit class XtensionSymbol(symbol: Symbol) {
    def underlyingSymbols: Seq[Symbol] = symbol match {
      case Symbol.Multi(symbols) => symbols
      case _ => List(symbol)
    }
    def isSameNormalized(other: Symbol): Boolean = {
      val syms = symbol.underlyingSymbols.map(_.normalized)
      val otherSyms = other.underlyingSymbols.map(_.normalized)
      syms.exists(otherSyms.contains)
    }

    def normalized: Symbol = SymbolOps.normalize(symbol)

    /** Returns corresponding scala.meta.Tree for symbol
      * Caveat: not thoroughly tested, may crash
      * */
    def to[T <: Tree]: Try[T] = Try {
      val tree = symbol match {
        case Symbol.Global(Symbol.None, Signature.Term(name)) =>
          Term.Name(name)
        case Symbol.Global(sym, Signature.Type(name)) =>
          Type.Select(sym.to[Term.Ref].get, Type.Name(name))
        case Symbol.Global(sym, Signature.Term(name)) =>
          Term.Select(sym.to[Term].get, Term.Name(name))
        case els => sys.error(els.toString)
      }
      tree.asInstanceOf[T] // should crash
    }
  }
  implicit class XtensionString(str: String) {
    def revealWhiteSpace: String = logger.revealWhitespace(str)
    def trimSugar: String = str.trim.replaceAllLiterally(".this", "")
  }
  implicit class XtensionAttributes(attributes: Attributes) {
    def dialect: Dialect = ScalafixScalametaHacks.dialect(attributes.language)
  }
  implicit class XtensionTreeScalafix(tree: Tree) {
    def parents: Stream[Tree] = TreeOps.parents(tree)
    def input: Input = tree.tokens.head.input
    def treeSyntax: String = ScalafixScalametaHacks.resetOrigin(tree).syntax
  }
  implicit class XtensionInputScalafix(input: Input) {
    def charset: Charset = input match {
      case Input.File(_, x) => x
      case _ => Charset.forName("UTF-8")
    }
    def path(sourceroot: AbsolutePath): AbsolutePath = input match {
      case Input.File(path, _) => path
      case Input.VirtualFile(label, _) => sourceroot.resolve(label)
      case els => sys.error(els.toString)
    }
    def isSbtFile: Boolean = label.endsWith(".sbt")
    def label: String = input match {
      case inputs.Input.File(path, _) => path.toString()
      case inputs.Input.VirtualFile(label, _) => label
      case _ =>
        s"Input.${input.productPrefix}('<${input.chars.take(10).mkString}...>')"
          .replaceAllLiterally(EOL, "")
    }
    def asString: String = new String(input.chars)
  }
}
