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

package satorg.fs2

import cats.effect._
import cats.effect.concurrent.Deferred
import cats.syntax.all._
import fs2._

import java.io.{InputStream, OutputStream}
import java.util.{zip => JZip}

package object zip {

  type ByteStream[F[_]] = Stream[F, Byte]
  type ZipEntryStream[F[_]] = Stream[F, ZipEntry[F]]
  type ZipPipe[F[_]] = Pipe[F, ZipEntry[F], Byte]
  type UnzipPipe[F[_]] = Pipe[F, Byte, ZipEntry[F]]

  def zipPipe[F[_]: ContextShift](blocker: Blocker)(
      implicit
      F: Concurrent[F],
      config: ZipConfig
  ): ZipPipe[F] = { entries =>
    io.readOutputStream[F](blocker, config.chunkSize) {
      mkJZipOutputStreamResource(blocker, config)(_).use { zipOut =>
        val writeOutput = io.writeOutputStream[F](F.pure(zipOut), blocker, closeAfterUse = false)

        entries
          .evalMap { entry =>
            blocker.delay { zipOut.putNextEntry(entry.toJava) } >>
              entry.body.through(writeOutput).compile.drain
          }
          .compile
          .drain
      }
    }
  }

  def unzipPipe[F[_]: ContextShift](blocker: Blocker)(
      implicit
      F: ConcurrentEffect[F],
      config: ZipConfig
  ): UnzipPipe[F] = { zipped =>
    Stream
      .resource(
        io.toInputStreamResource[F](zipped)
          .flatMap { mkJZipInputStreamResource[F](blocker, config) }
      )
      .flatMap { zipIn =>
        def emitNextEntry: ZipEntryStream[F] = Stream.force {
          blocker
            .delay { zipIn.getNextEntry }
            .flatMap {
              case null =>
                Stream.empty.covaryAll[F, ZipEntry[F]].pure[F]
              case jEntry =>
                Deferred[F, Unit].map { deferred =>
                  val body =
                    io.readInputStream[F](
                      F.pure(zipIn),
                      config.chunkSize,
                      blocker,
                      closeAfterUse = false
                    ).onFinalize(deferred.complete(()))

                  val entry = ZipEntry.fromJavaAndBody(jEntry, body)

                  Stream.emit(entry) ++ Stream.eval_(deferred.get) ++ emitNextEntry
                }
            }
        }

        emitNextEntry
      }
  }

  private[this] def mkJZipInputStreamResource[F[_]: ContextShift](
      blocker: Blocker,
      config: ZipConfig
  )(input: InputStream)(implicit F: Sync[F]) =
    Resource.fromAutoCloseableBlocking[F, JZip.ZipInputStream](blocker)(F.delay {
      new JZip.ZipInputStream(input, config.charset)
    })

  private[this] def mkJZipOutputStreamResource[F[_]: ContextShift](
      blocker: Blocker,
      config: ZipConfig
  )(output: OutputStream)(implicit F: Sync[F]) =
    Resource.fromAutoCloseableBlocking(blocker)(F.delay {
      new JZip.ZipOutputStream(output, config.charset)
    })
}
