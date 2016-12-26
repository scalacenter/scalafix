package scalafix.util

import scala.collection.immutable.Seq
import scala.{meta => m}
import scala.meta._
import scalafix.rewrite._

class AddImport(ast: m.Tree)(implicit sApi: SemanticApi) {
  val allImports = ast.collect {
    case t @ q"import ..$importersnel" => t -> importersnel
  }

  val firstImport = allImports.headOption
  val firstImportFirstToken = firstImport.flatMap {
    case (importStatement, _) => importStatement.tokens.headOption
  }
  val tokenBeforeFirstImport = firstImportFirstToken.flatMap { stopAt =>
    ast.tokens.takeWhile(_ != stopAt).lastOption
  }

  //This is currently a very dumb implementation.
  //It does no checking for existing imports and makes
  //no attempt to consolidate imports
  def addedImports(importString: String): Seq[Patch] =
    tokenBeforeFirstImport
      .map(
        beginImportsLocation =>
          Patch
            .insertAfter(beginImportsLocation, importString)
      )
      .toList

  def gatherPatches(imports: Seq[String]): Seq[Patch] = {
    val importStrings = imports.map("import " + _).mkString("\n", "\n", "\n")
    addedImports(importStrings)
  }
}
