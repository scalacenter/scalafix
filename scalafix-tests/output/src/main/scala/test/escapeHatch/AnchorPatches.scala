package test.escapeHatch

import scala.collection.immutable // scalafix:ok RemoveUnusedImports

import scala.collection.mutable.{ // scalafix:ok RemoveUnusedImports
  Map,
  Set
}


import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

object AnchorPatches {
  def d1s: FiniteDuration = Duration(1, "s")
  def d2s = Duration(2, "s") // scalafix:ok ExplicitResultTypes
}
