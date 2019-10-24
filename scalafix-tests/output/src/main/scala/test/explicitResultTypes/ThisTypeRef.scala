package test.explicitResultTypes

object ThisTypeRef {
  trait Base {
    class T
  }
  class Sub extends Base {
    val ref: T = new T()
  }

  trait ThisType  {
    def cp(): this.type
  }
  class ThisTypeImpl extends ThisType {
    def cp(): this.type = this
  }

}