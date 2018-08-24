package scalafix.internal.v0
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import scala.meta.Input

// Input.Synthetic is gone so we hack it here by extending java.io.InputStream and
// piggy backing on Input.Stream.
final case class InputSynthetic(
    value: String,
    input: Input,
    start: Int,
    end: Int
) extends InputStream {
  val in = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))
  override def read(): Int = in.read()
}
