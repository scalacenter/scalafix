package com.geirsson

import scala.collection.immutable.TreeMap

object immutable {
  type SortedMap[A, B] = TreeMap[A, B]

  object SortedMap {
    def empty[A : Ordering, B]: SortedMap[A, B] = TreeMap.empty[A, B]
  }
}
