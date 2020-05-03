/*
rules = [OrganizeImports]
OrganizeImports.groupedImports = Keep
OrganizeImports.coalesceToWildcardImportThreshold = 3
 */

package fix

import scala.collection.immutable.{Seq, Map, Vector}
import scala.collection.immutable.{Seq, Map, Vector, Set}
import scala.collection.immutable.{Seq, Map, Vector => Vec, Set, Stream}
import scala.collection.immutable.{Seq, Map, Vector => _, Set, Stream}

object CoalesceImportees
