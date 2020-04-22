package satorg.fs2.zip

import java.nio.charset.{Charset, StandardCharsets}

final case class ZipConfig(chunkSize: Int, charset: Charset)

object ZipConfig {
  implicit lazy val default: ZipConfig =
    ZipConfig(
      chunkSize = 8192,
      charset = StandardCharsets.UTF_8)
}
