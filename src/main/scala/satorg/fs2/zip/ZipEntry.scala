package satorg.fs2.zip

import java.util.{zip => JZip}

import fs2._

final case class ZipEntry[F[_]](path: String, body: Stream[F, Byte]) {
  def toJava: JZip.ZipEntry = new JZip.ZipEntry(path)
}

object ZipEntry {
  def fromJavaAndBody[F[_]](jEntry: JZip.ZipEntry, body: ByteStream[F]): ZipEntry[F] =
    ZipEntry(
      path = jEntry.getName,
      body = body)
}
