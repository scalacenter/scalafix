package test.imports {

  package nested1 {

    import java.io.OutputStream

    object MultiplePackage1 {
      implicit val out: OutputStream = Console.out
    }
  }
  package nested2 {

    import java.io.BufferedReader

    object MultiplePackage2 {
      implicit val in: BufferedReader = Console.in
    }
  }
}

