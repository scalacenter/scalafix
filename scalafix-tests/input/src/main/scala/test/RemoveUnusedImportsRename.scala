/*
rule = RemoveUnused
 */
package test

import scala.util.{Success => UnusedSuccess, _}
import scala.concurrent.{Future => UnusedSuccess, _}

class RemoveUnusedImportsRename {
  Failure(new Exception())
}
