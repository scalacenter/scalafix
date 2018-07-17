package scalafix.internal.v1

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

object FingerprintOps {

  def md5(string: String): String = {
    md5(ByteBuffer.wrap(string.getBytes(StandardCharsets.UTF_8)))
  }

  def md5(buffer: ByteBuffer): String = {
    val md = MessageDigest.getInstance("MD5")
    md.update(buffer)
    DatatypeConverter.printHexBinary(md.digest())
  }

}
