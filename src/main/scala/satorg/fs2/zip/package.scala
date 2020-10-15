package satorg.fs2

import java.io.{InputStream, OutputStream}
import java.util.{zip => JZip}

import cats.effect._
import cats.effect.concurrent.Deferred
import cats.syntax.all._
import fs2._

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
      mkJZipOutputStreamResource(blocker, config)(_).use { output =>
        val writeOutput = io.writeOutputStream[F](F.pure(output), blocker, closeAfterUse = false)

        entries
          .evalMap { entry =>
            blocker.delay { output.putNextEntry(entry.toJava) } >>
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
      .resourceWeak(
        io.toInputStreamResource[F](zipped).flatMap { mkJZipInputStreamResource[F](blocker, config) }
      )
      .flatMap { input =>
        def emitNextEntry: ZipEntryStream[F] = Stream.force {
          blocker.delay { input.getNextEntry } >>= {
            case null => F.pure(Stream.empty.covaryAll[F, ZipEntry[F]])
            case jEntry =>
              Deferred[F, ExitCase[Throwable]]
                .map { deferred =>
                  val body =
                    io.readInputStream[F](F.pure(input), config.chunkSize, blocker, closeAfterUse = false)
                      .onFinalizeCase { deferred.complete }
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
  )(input: InputStream)(implicit F: Sync[F]) = {

    Resource.fromAutoCloseableBlocking[F, JZip.ZipInputStream](blocker)(
      F.delay { new JZip.ZipInputStream(input, config.charset) }
    )
  }

  private[this] def mkJZipOutputStreamResource[F[_]: ContextShift](
      blocker: Blocker,
      config: ZipConfig
  )(output: OutputStream)(implicit F: Sync[F]) = {

    Resource.fromAutoCloseableBlocking(blocker)(F.delay {
      new JZip.ZipOutputStream(output, config.charset)
    })
  }
}
