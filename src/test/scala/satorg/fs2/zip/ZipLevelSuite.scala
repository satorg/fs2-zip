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

import munit.ScalaCheckSuite
import org.scalacheck.{Gen, Prop}

class ZipLevelSuite extends ScalaCheckSuite with ZipLevelSuiteData {

  test("ZipLevel.DefaultCompression.toString") {
    assertEquals(
      ZipLevel.DefaultCompression.toString,
      ZipLevel.DefaultCompression.getClass.getSimpleName.stripSuffix("$")
    )
  }

  test("ZipLevel.Value: direct instantiation should not be accessible") {

    assertNoDiff(
      compileErrors("new ZipLevel.Value(0)"),
      """error: class Value is abstract; cannot be instantiated
        |new ZipLevel.Value(0)
        |^""".stripMargin
    )

    assertNoDiff(
      compileErrors("ZipLevel.Value(0)"),
      """error: satorg.fs2.zip.ZipLevel.Value.type does not take parameters
        |ZipLevel.Value(0)
        |              ^
        |""".stripMargin
    )

    assertNoDiff(
      compileErrors("new ZipLevel.Value(0) {}"),
      """error: illegal inheritance from sealed class Value
        |new ZipLevel.Value(0) {}
        |             ^
        |""".stripMargin
    )
  }

  test("ZipLevel.from: should return None if value is out of range") {
    Prop.forAll(incorrectValueGen) {
      ZipLevel.from(_).isEmpty
    }
  }

  test("ZipLevel.from: should return a correct value otherwise") {
    Prop.forAll(correctValueGen) { value =>
      val maybeResult = ZipLevel.from(value)
      assert(maybeResult.isDefined)

      val result = maybeResult.get
      assertEquals(result.value, value)
      assertEquals(result.toString, s"ZipLevel($value)")

      // Check the generated `unapply` method.
      assert(ZipLevel.Value.unapply(result).contains(value))

      // Check that two instances created from the same value are equal.
      val maybeAnotherResult = ZipLevel.from(value)
      assert(maybeAnotherResult.contains(result))
    }
  }
}

sealed trait ZipLevelSuiteData { _: ZipLevelSuite =>

  protected val correctValueGen: Gen[Int] =
    Gen.choose(ZipLevel.MinValue.value, ZipLevel.MaxValue.value)

  protected val incorrectValueGen: Gen[Int] =
    Gen.oneOf(
      Gen.choose(Int.MinValue, ZipLevel.MinValue.value - 1),
      Gen.choose(ZipLevel.MaxValue.value + 1, Int.MaxValue)
    )
}
