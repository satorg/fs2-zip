# fs2-zip

Simple streaming wrappers for JDK's `ZipInputStream` and `ZipOutputStream`. The wrappers use Java stream converters
from [fs2-io](https://fs2.io/#/io).

### Read Zip Archive

Reads a zip file and prints MD5 digests for each zip entry to STDOUT:

```scala
import cats.effect._
import fs2._
import satorg.fs2.zip._

def readZipFile[F[_]: ConcurrentEffect : ContextShift](
  filename: String,
  blocker: Blocker
)(implicit config: ZipConfig): F[Unit] =
  io.file.readAll(filename, blocker, config.chunkSize)
    .through(unzipPipe(blocker))
    .flatMap { (entry: ZipEntry[F]) =>
      entry.body
        .through(hash.md5) // simply calculate MD5 digest for each entry
        .foldMap(b => f"$b%02x")
        .map(_ + " " + entry.path)
    }
    .showLinesStdOutAsync(blocker)
    .compile.drain
```

Note that `Stream[F, ZipEntry[F]]` cannot be processed in an order other than how they come from the source.
Furthermore, every `ZipEntry#body` stream must be completed (or drained) before pulling the next `ZipEntry`. This
limitation comes from `java.util.zip.ZipInputStream` which is strictly sequential.

However, it is still possible to process the entries in parallel (e.g., using `.parJoinUnbounded`) – the entries will
arrange each other in the order as they come from the source stream.

### Write Zip Archive

Takes a stream of arbitrary entries, creates string representation for each entry and writes them into a zip file.

```scala
import cats.effect._
import cats.syntax.show._
import fs2._
import satorg.fs2.zip._

def writeZipFile[F[_]: Concurrent : ContextShift, A: Show](
  entries: Stream[F, A],
  filename: String,
  blocker: Blocker
)(implicit config: ZipConfig): F[Unit] =
  entries
    .zipWithIndex
    .map { case (entry, index) =>
      ZipEntry[F](
        path = s"$index.txt",
        body = Stream.emit(entry.show).covary[F].through(text.utf8Encode)
      )
    }
    .through(zipPipe(blocker))
    .through(io.file.writeAll(filename, blocker))
    .compile.drain
```

See `ZipUnzipTest` for more examples.
