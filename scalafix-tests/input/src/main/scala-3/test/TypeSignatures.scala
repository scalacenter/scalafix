/*
rules = [
  DecorateTypeSignatures
]
*/
package test

object TypeSignatures:

  type MapKV[K] = [V] =>> Map[K, V]

  type Convert[X] = X match
    case String => Char
    case Int => Float
