/*
 * Copyright (c) 2020 Sergey Torgashov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package satorg.fs2.zip

import cats.effect._
import fs2._
import munit._
import org.scalacheck._
import org.scalacheck.effect.PropF
import scodec.bits.ByteVector

class ZipUnzipSuite extends CatsEffectSuite with ScalaCheckEffectSuite with BlockerFixture {

  private val blockerFixture = ResourceFixture(blockerResource)

  blockerFixture.test(
    "zipPipe and unzipPipe should compress and decompress streams correctly"
  ) { blocker =>
    val gen =
      Gen.mapOf {
        val entryPathGen =
          Gen
            .listOf(Gen.frequency(9 -> Gen.alphaNumChar, 1 -> Gen.const('/')))
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

    PropF.forAllF(gen) { sourceBodiesByPath: Map[String, ByteVector] =>
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
        .through(zipPipe(blocker))
        // convert to a single item chunk of bytes
        .chunks
        .fold(Chunk.Queue.empty[Byte])(_ :+ _)
        .map(_.toChunk)
        // create a new stream of bytes from the chunk
        .flatMap(Stream.chunk)
        .rechunkRandomly()
        // decompress from byte stream
        .through(unzipPipe(blocker))
        // extract each entry into a separate `ByteVector`
        .evalMap { unzippedEntry =>
          unzippedEntry.body.compile.to(ByteVector).map {
            unzippedEntry.path -> _
          }
        }
        // prepare and run the stream
        .compile
        .toVector
        .assertEquals(sourceEntries)
    }
  }
}
