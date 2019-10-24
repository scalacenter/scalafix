/*
rules = ExplicitResultTypes
*/
package test.explicitResultTypes

object SeenFromTypeParams {
  abstract class AbstractStore {
    def get[K1](k1: K1): K1
  }
  class Store extends AbstractStore {
    override def get[K2](k2: K2) = k2
  }
}