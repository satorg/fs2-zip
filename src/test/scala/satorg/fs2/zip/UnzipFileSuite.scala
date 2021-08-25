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
import cats.syntax.all._
import fs2._
import munit.CatsEffectSuite

class UnzipFileSuite extends CatsEffectSuite {
  import UnzipFileSuite._

  private val blockerSuiteFixture = ResourceSuiteLocalFixture("blocker", Blocker[IO])
  override def munitFixtures: Seq[Fixture[_]] = blockerSuiteFixture +: super.munitFixtures

  private val inputFixture =
    ResourceFixture(Resource.eval(IO { blockerSuiteFixture() })
      .flatMap(InputFixture.resource))

  private val expectedResults =
    Vector(
      "dir1/" -> "d41d8cd98f00b204e9800998ecf8427e",
      "dir1/dir2/" -> "d41d8cd98f00b204e9800998ecf8427e",
      "file1.txt" -> "a88bf0ec0f06e457593a7aea64fd50dd",
      "dir1/file2.txt" -> "7c5ef0612dfc2ddab63b00cc0386aa53",
      "dir1/dir2/file3.txt" -> "0f5fb227947695ca2bfcb3d45b40cb0c",
      "dir1/dir2/file4.txt" -> "db8bfc02dd287a86eb18cdfcab96c6ac"
    ).sorted

  private def decodeEntry(entry: ZipEntry[IO]) =
    entry.body
      .through(hash.md5)
      .foldMap(b => f"$b%02x")
      .map(entry.path -> _)

  inputFixture.test("unzipPipe: should unzip files and directories from a test zip archive") { fixture =>
    fixture.input
      .through(unzipPipe(fixture.blocker))
      .flatMap(decodeEntry)
      .compile
      .toVector
      .map { _.sorted }
      .assertEquals(expectedResults)
  }
  inputFixture.test("unzipPipe: should allow processing unzipped entries in parallel") { fixture =>
    fixture.input
      .through(unzipPipe(fixture.blocker))
      .map(decodeEntry)
      .parJoinUnbounded
      .compile
      .toVector
      .map { _.sorted }
      .assertEquals(expectedResults)
  }
}

object UnzipFileSuite {
  class InputFixture private (val blocker: Blocker, val input: Stream[IO, Byte])

  object InputFixture {
    private final val testZip = "/test.zip"
    private final val chunkSize = 256

    def resource(blocker: Blocker)(implicit cs: ContextShift[IO]): Resource[IO, InputFixture] =
      Resource
        .fromAutoCloseableBlocking(blocker)(IO { getClass.getResourceAsStream(testZip) })
        .ensure(throw new RuntimeException(s"cannot load '$testZip'")) { _ != null }
        .map(IO.pure)
        .map { io.readInputStream(_, chunkSize, blocker) }
        .map { new InputFixture(blocker, _) }
  }
}
