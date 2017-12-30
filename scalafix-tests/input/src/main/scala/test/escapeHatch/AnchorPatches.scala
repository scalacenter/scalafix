/*
rule = [RemoveUnusedImports, ExplicitResultTypes]
ExplicitResultTypes.memberKind = [Def]
ExplicitResultTypes.memberVisibility = [Public]
unsafeShortenNames = true
ExplicitResultTypes.unsafeShortenNames = true
 */
package test.escapeHatch

import scala.collection.mutable
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
  def d1s = Duration(1, "s")
  def d2s = Duration(2, "s") // scalafix:ok ExplicitResultTypes
}
