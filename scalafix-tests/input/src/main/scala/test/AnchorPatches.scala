/*
rule = [RemoveUnused]
 */
package test

import scala.collection.AbstractMap
import scala.collection.immutable // scalafix:ok RemoveUnusedImports

import scala.collection.mutable.{ // scalafix:ok RemoveUnusedImports
  Map,
  Set
}

import scala.collection.mutable.{
  Map,
  Set
}

import scala.concurrent.duration.Duration

object AnchorPatches {
  private def d1s = Duration(1, "s")
  private def d2s = Duration(2, "s") // scalafix:ok RemoveUnused
}