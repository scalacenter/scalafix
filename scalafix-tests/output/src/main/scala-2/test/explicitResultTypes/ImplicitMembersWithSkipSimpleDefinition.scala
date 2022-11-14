package test.explicitResultTypes

object ImplicitMembersWithSkipSimpleDefinition {

    val x = 42

    implicit var y = 43

    val map_x: Map[Int,String] = null.asInstanceOf[Map[Int,String]]

    implicit val map_y: Map[Int,String] = null.asInstanceOf[Map[Int,String]]

}
