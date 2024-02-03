/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.expandRelative = true
 */
package test.organizeImports

import GivenImports.{given GivenImports.Alpha}
import GivenImports.{given}
import PackageObject.{a}
import PackageObject.{a as A}

import scala.collection.{ BitSet }
import scala.collection.{Map}
import scala.collection.{Seq as _}
import scala.collection.{Set as ImmutableSet}
import scala.util.{*}

object CurlyBracedSingleImportee
