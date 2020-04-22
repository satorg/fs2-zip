package satorg.fs2.zip

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.charset.StandardCharsets

import cats.effect._
import cats.implicits._
import fs2._
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext

class ZipUnzipTest extends Specification {

  import ZipUnzipTest._

  val testBlocker: Blocker = Blocker.liftExecutionContext(ExecutionContext.global)
  implicit val testCS: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "unzipPipe" should {
    "unzip files and directories from a test zip archive" in {
      io.readInputStream(acquireResourceInputStream("/test.zip", testBlocker), 256, testBlocker)
        .through(unzipPipe(testBlocker))
        .flatMap { entry =>
          entry.body
            .through(hash.md5)
            .map(b => f"$b%02x")
            .foldMonoid
            .map(entry.path -> _)
        }
        .compile
        .toList
        .unsafeRunSync() must
        containTheSameElementsAs(Seq(
          "dir1/" -> "d41d8cd98f00b204e9800998ecf8427e",
          "dir1/dir2/" -> "d41d8cd98f00b204e9800998ecf8427e",
          "file1.txt" -> "a88bf0ec0f06e457593a7aea64fd50dd",
          "dir1/file2.txt" -> "7c5ef0612dfc2ddab63b00cc0386aa53",
          "dir1/dir2/file3.txt" -> "0f5fb227947695ca2bfcb3d45b40cb0c",
          "dir1/dir2/file4.txt" -> "db8bfc02dd287a86eb18cdfcab96c6ac",
        ))
    }
  }
  "zipPipe and then unzipPipe" should {
    "process empty archive" in {
      val outputStream = new ByteArrayOutputStream

      Stream.empty.covaryAll[IO, ZipEntry[IO]]
        .through(zipPipe(testBlocker))
        .through(io.writeOutputStream(IO.pure[OutputStream](outputStream), testBlocker))
        .compile
        .drain
        .unsafeRunSync()

      val zippedBytes = outputStream.toByteArray

      zippedBytes must haveLength(be_>(2))
      val initSig = new String(zippedBytes.take(2), StandardCharsets.US_ASCII)
      initSig ==== "PK"

      io.readInputStream(IO.pure[InputStream](new ByteArrayInputStream(zippedBytes)), 1, testBlocker)
        .through(unzipPipe(testBlocker))
        .compile
        .toList
        .unsafeRunSync() must beEmpty
    }
  }
}

object ZipUnzipTest {
  private def acquireResourceInputStream(name: String, blocker: Blocker)(
    implicit cs: ContextShift[IO])
  : IO[InputStream] = {

    blocker.delay[IO, InputStream] { getClass.getResourceAsStream(name) }
      .ensure(throw new RuntimeException(s"cannot load '$name'"))(_ != null)
  }
}
