package scalafix
package rewrite
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.contrib._

import org.scalameta.logger

object ScalaJsRewrites {
  // Symbols copy pasted from mirror.database
  val nativeSymbol = Symbol("_root_.scala.scalajs.js.package.native#")
  val globalScopeSymbol =
    Symbol("_root_.scala.scalajs.js.annotation.JSGlobalScope#`<init>`()V.")
  val globalScopeParentSymbol =
    Symbol("_root_.scala.scalajs.js.GlobalScope#")
  val jsImportSymbol = Symbol(
    "_root_.scala.scalajs.js.annotation.JSImport#`<init>`(Ljava/lang/String;Ljava/lang/String;)V.")

  private object ClassOrObject {
    def unapply(arg: Tree): Option[(Seq[Mod], Seq[Ctor.Call])] = arg match {
      case Defn.Class(mods, _, _, _, Template(_, parents, _, _)) =>
        Option(mods -> parents)
      case Defn.Object(mods, _, Template(_, parents, _, _)) =>
        Option(mods -> parents)
      case _ => None
    }
  }

  val DemandJSGlobal = Rewrite.semantic { implicit mirror => ctx =>
    import ctx._
    object JsNative {
      def unapply(mods: Seq[Mod]): Option[(Mod, Option[Name])] =
        mods.collectFirst {
          case native @ Mod.Annot(ref: Ref) if ref.symbol == nativeSymbol =>
            native -> mods.collectFirst {
              case Mod.Annot(
                  Term.Apply(jsName @ Ctor.Ref.Name("JSName"), _)) =>
                jsName
            }
        }
    }
    // https://github.com/scala-js/scala-js/pull/2794#issuecomment-284982025
    def skipRewrite(mods: Seq[Mod], parents: Seq[Ctor.Call]): Boolean = {
      def hasInvalidAnnot: Boolean =
        mods.exists(_.exists {
          case ref: Ref =>
            ref.symbol == jsImportSymbol ||
              ref.symbol == globalScopeSymbol
          case _ => false
        })
      def hasGlobalScopeParent: Boolean =
        parents.exists(_.exists {
          case ref: Ref => ref.symbol == globalScopeParentSymbol
          case _ => false
        })
      hasInvalidAnnot || hasGlobalScopeParent
    }
    val patchB = Seq.newBuilder[Patch]
    new Traverser {
      override def apply(tree: Tree): Unit = tree match {
        case ClassOrObject(mods, parents) =>
          if (skipRewrite(mods, parents)) Unit // do nothing
          else {
            mods match {
              case JsNative(_, Some(jsName)) =>
                patchB += ctx.rename(jsName, q"JSGlobal")
              case JsNative(native, None) =>
                patchB += ctx.addRight(native.tokens.last, " @JSGlobal")
              case _ =>
            }
          }
        case _ => super.apply(tree)
      }
    }.apply(tree)
    val patches = patchB.result()

    if (patches.nonEmpty && config.imports.organize) {
      ctx.addGlobalImport(importer"scala.scalajs.js.annotation.JSGlobal") ++ patches
    } else {
      patches.asPatch
    }
  }
}
