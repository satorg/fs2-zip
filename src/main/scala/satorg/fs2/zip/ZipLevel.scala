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

import satorg.internal.compat.std.OptionCompat

import java.util.zip.Deflater

/** Defines a compression level for entries with compression method set to [[ZipMethod.Deflated]]
  */
sealed trait ZipLevel extends Any {
  private[zip] def value: Int
}

object ZipLevel extends OptionCompat {
  case object DefaultCompression extends ZipLevel {
    private[zip] override def value = Deflater.DEFAULT_COMPRESSION
  }

  sealed abstract case class Value private[ZipLevel] (override val value: Int) extends ZipLevel {
    override def productPrefix: String = "ZipLevel"
  }

  @inline private[this] final def unsafeFrom(value: Int) = new Value(value) {}

  val NoCompression: Value = unsafeFrom(Deflater.NO_COMPRESSION)
  val BestSpeed: Value = unsafeFrom(Deflater.BEST_SPEED)
  val BestCompression: Value = unsafeFrom(Deflater.BEST_COMPRESSION)

  /** Alias for [[NoCompression]] */
  @inline final def MinValue: Value = NoCompression

  /** Alias for [[BestCompression]] */
  @inline final def MaxValue: Value = BestCompression

  def from(value: Int): Option[Value] =
    Option.when(value >= MinValue.value && value <= MaxValue.value) {
      unsafeFrom(value)
    }
}
