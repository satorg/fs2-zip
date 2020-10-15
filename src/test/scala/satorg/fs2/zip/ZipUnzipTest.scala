package satorg.fs2.zip

import java.io.InputStream

import cats.effect._
import cats.implicits._
import fs2._
import org.scalacheck._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import scodec.bits.ByteVector

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ZipUnzipTest extends Specification with ScalaCheck {

  import ZipUnzipTest._

  val testBlocker: Blocker =
    Blocker.liftExecutionContext(ExecutionContext.global)
  implicit val testCS: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  "unzipPipe" should {
    "unzip files and directories from a test zip archive" in {
      io.readInputStream(
        acquireResourceInputStream("/test.zip", testBlocker),
        256,
        testBlocker
      ).through(unzipPipe(testBlocker))
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
        containTheSameElementsAs(
          Seq(
            "dir1/" -> "d41d8cd98f00b204e9800998ecf8427e",
            "dir1/dir2/" -> "d41d8cd98f00b204e9800998ecf8427e",
            "file1.txt" -> "a88bf0ec0f06e457593a7aea64fd50dd",
            "dir1/file2.txt" -> "7c5ef0612dfc2ddab63b00cc0386aa53",
            "dir1/dir2/file3.txt" -> "0f5fb227947695ca2bfcb3d45b40cb0c",
            "dir1/dir2/file4.txt" -> "db8bfc02dd287a86eb18cdfcab96c6ac"
          )
        )
    }
  }
  "zipPipe and unzipPipe" should {
    "compress and decompress streams correctly" in {
      implicit val arbInputEntries: Arbitrary[Map[String, ByteVector]] =
        Arbitrary {
          Gen.mapOf {
            val entryPathGen =
              Gen
                .listOf(
                  Gen.frequency(9 -> Gen.alphaNumChar, 1 -> Gen.const('/'))
                )
                .map(_.mkString)

            val entryBodyGen =
              Gen.resize(
                1000,
                Gen
                  .containerOf[Array, Byte](Arbitrary.arbByte.arbitrary)
                  .map(ByteVector.view)
              )

            Gen.zip(entryPathGen, entryBodyGen)
          }
        }

      prop { sourceBodiesByPath: Map[String, ByteVector] =>
        val sourceEntries = sourceBodiesByPath.toVector

        Stream
          .emits(sourceEntries)
          .covary[IO]
          // prepare a stream of `ZipEntry`
          .map { case (path, bytes) =>
            ZipEntry(
              path = path,
              body = Stream
                .chunk(Chunk.byteVector(bytes))
                .covary[IO]
                .rechunkRandomly()
            )
          }
          // compress to byte stream
          .through(zipPipe(testBlocker))
          // convert to a single item chunk of bytes
          .chunks
          .fold(Chunk.Queue.empty[Byte])(_ :+ _)
          .map(_.toChunk)
          // create a new stream of bytes from the chunk
          .flatMap(Stream.chunk)
          .rechunkRandomly()
          // decompress from byte stream
          .through(unzipPipe(testBlocker))
          // extract each entry into a separate `ByteVector`
          .evalMap { unzippedEntry =>
            unzippedEntry.body.compile.to(ByteVector).map {
              unzippedEntry.path -> _
            }
          }
          // prepare and run the stream
          .compile
          .toVector
          .unsafeRunTimed(5.seconds) must beSome(===(sourceEntries))
      }
    }
  }
}

object ZipUnzipTest {
  private def acquireResourceInputStream(name: String, blocker: Blocker)(
      implicit cs: ContextShift[IO]
  ): IO[InputStream] = {

    blocker
      .delay[IO, InputStream] { getClass.getResourceAsStream(name) }
      .ensure(throw new RuntimeException(s"cannot load '$name'"))(_ != null)
  }
}
