/*
rules = DisableVariantTypes
 */
package test

object DisableVariantTypes {
  class Co[+T](t: T) // assert: DisableVariantTypes.Covariant
  class Contr[-T](t: T) // assert: DisableVariantTypes.Contravariant

  class Pro[-A,             // assert: DisableVariantTypes.Contravariant
            +B](a: A, b: B) // assert: DisableVariantTypes.Covariant
}
