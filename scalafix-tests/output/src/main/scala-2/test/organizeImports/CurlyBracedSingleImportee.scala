package test.organizeImports

import test.organizeImports.PackageObject.a
import test.organizeImports.PackageObject.{a => A}

import scala.collection.BitSet
import scala.collection.Map
import scala.collection.{Seq => _}
import scala.collection.{Set => ImmutableSet}
import scala.util._

object CurlyBracedSingleImportee
