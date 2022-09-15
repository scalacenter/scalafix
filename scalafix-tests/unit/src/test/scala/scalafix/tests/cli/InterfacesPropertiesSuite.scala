package scalafix.tests.cli
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import scalafix.Versions

class InterfacesPropertiesSuite extends FunSuite with BeforeAndAfterAll {
  val props = new java.util.Properties()
  override def beforeAll(): Unit = {
    val path = "scalafix-interfaces.properties"
    val in = this.getClass.getClassLoader.getResourceAsStream(path)
    try props.load(in)
    finally in.close()
    super.beforeAll()
  }

  def check(key: String, expected: String): Unit = {
    test(key) {
      val obtained = props.get(key)
      assert(obtained == expected)
    }
  }

  check("scalafixVersion", Versions.version)
  check("scalafixStableVersion", Versions.stableVersion)
  check("scalametaVersion", Versions.scalameta)
  check("scala211", Versions.scala211)
  check("scala212", Versions.scala212)

}
