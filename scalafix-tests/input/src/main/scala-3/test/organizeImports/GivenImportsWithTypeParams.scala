/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.targetDialect = Scala3
 */
package test.organizeImports

import scala.collection.mutable.ArrayBuffer
import test.organizeImports.GivenImports.Alpha
import test.organizeImports.GivenImports.Beta
import test.organizeImports.GivenImports.{given ArrayBuffer[Beta], given ArrayBuffer[Alpha]}

object GivenImportsWithTypeParams
