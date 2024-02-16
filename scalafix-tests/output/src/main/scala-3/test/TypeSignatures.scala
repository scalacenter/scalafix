package test

object TypeSignatures:

  /*[V] =>> Map[K,V]*/ type MapKV[K] = [V] =>> Map[K, V]

  /*X match { String => Char, Int => Float }*/ type Convert[X] = X match
    case String => Char
    case Int => Float
