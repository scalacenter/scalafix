/*
rules = ExplicitResultTypes
*/
package test.imports {

  package nested1 {
    object MultiplePackage1 {
      implicit val out = Console.out
    }
  }
  package nested2 {
    object MultiplePackage2 {
      implicit val in = Console.in
    }
  }
}

