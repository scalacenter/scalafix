/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.targetDialect = Scala3
OrganizeImports.groups = ["*"]
 */
package test.organizeImports

import scala.collection.mutable.ArrayBuffer
import test.organizeImports.GivenImports.Alpha
import test.organizeImports.GivenImports.Beta
import test.organizeImports.GivenImports2.{given ArrayBuffer[Beta], given ArrayBuffer[Alpha]}

object GivenImportsWithTypeParams
