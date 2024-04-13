package test

import scala.collection.immutable // scalafix:ok RemoveUnusedImports

import scala.collection.mutable.{ // scalafix:ok RemoveUnusedImports
  Map,
  Set
}


import scala.concurrent.duration.Duration

object AnchorPatches {
  
  private def d2s = Duration(2, "s") // scalafix:ok RemoveUnused
}