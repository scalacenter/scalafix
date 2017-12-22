/*
  rules = [DisableVariantTypes]
 */
package test

object DisableVariantTypes {
  class Co[+T](t: T)
  class Contr[-T](t: T)

  class Pro[-A, +B](a: A, b: B)
}
