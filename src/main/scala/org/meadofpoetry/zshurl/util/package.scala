package org.meadofpoetry.zshurl

package object util {

  def base64decode(s: String): String =
    new String(java.util.Base64.getDecoder.decode(s))

  def base64encode(s: String): Array[Byte] =
    java.util.Base64.getEncoder.encode(s.getBytes())

  def genURLSlug(id: Long): String = {
    val sb = new StringBuilder(8)

    def longToChar(i: Long): Char = {
      if (i < 26) ('a' + i).toChar
      else if (i < 52) ('A' + i - 26).toChar
      else ('0' + i - 52).toChar
    }

    def loop(i: Long, nth: Int): Unit =
      if (nth == 0) ()
      else if (i <= 0) loop(Long.MaxValue-id-4, nth)
      else {
        val next = Math.floorDiv(i, 62)
        val mod  = Math.floorMod(i, 62)
        sb.append(longToChar(mod))
        loop(next, nth-1)
      }

    loop(id, 8)
    sb.result()
  }

}
