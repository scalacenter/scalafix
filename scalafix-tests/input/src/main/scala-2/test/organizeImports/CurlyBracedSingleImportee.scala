/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.expandRelative = true
 */
package test.organizeImports

import PackageObject.{a}
import PackageObject.{a => A}

import scala.collection.{ BitSet }
import scala.collection.{Map}
import scala.collection.{Seq => _}
import scala.collection.{Set => ImmutableSet}
import scala.util.{_}

object CurlyBracedSingleImportee
