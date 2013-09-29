/*
 * Copyright (C) 2013 The Mango Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * The code of this project is a port of (or wrapper around) the Guava-libraries.
 *    See http://code.google.com/p/guava-libraries/
 *
 * @author Markus Schneider
 */
package org.feijoas.mango.common.primitives

import org.scalatest.FreeSpec
import org.scalatest.Matchers.be
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.Matchers.convertToStringShouldWrapper
import org.scalatest.prop.PropertyChecks
import com.google.common.primitives.UnsignedInteger
import com.google.common.primitives.UnsignedInts
import scala.collection.convert.decorateAll._
import java.math.BigInteger
import org.scalacheck.Gen._
import com.google.common.testing.SerializableTester

/** Tests for [[UInt]]
 *
 *  @author Markus Schneider
 *  @since 0.10
 */
class UIntTest extends FreeSpec with PropertyChecks {

  val testInts = (for (i <- -3 to 3) yield List(i, Int.MaxValue + i, Int.MinValue + i)).flatten
  val testLongs = (for (i <- -3 to 3) yield List[Long](i, Int.MaxValue.toLong + i.toLong, Int.MinValue.toLong + i.toLong)).flatten
  val least: Int = 0L.toInt
  val greatest: Int = 0xffffffffL.toInt

  "An UInt " - {
    "should implement +" in {
      forAll { (a: Int, b: Int) =>
        val expected = UnsignedInteger.fromIntBits(a).plus(UnsignedInteger.fromIntBits(b))
        (UInt.fromIntBits(a) + UInt.fromIntBits(b)).toInt should be(expected.intValue)
      }
    }
    "should implement -" in {
      forAll { (a: Int, b: Int) =>
        val expected = UnsignedInteger.fromIntBits(a).minus(UnsignedInteger.fromIntBits(b))
        (UInt.fromIntBits(a) - UInt.fromIntBits(b)).toInt should be(expected.intValue)
      }
    }
    "should implement *" in {
      forAll { (a: Int, b: Int) =>
        val expected = UnsignedInteger.fromIntBits(a).times(UnsignedInteger.fromIntBits(b))
        (UInt.fromIntBits(a) * UInt.fromIntBits(b)).toInt should be(expected.intValue)
      }
    }
    "should implement /" in {
      forAll { (a: Int, b: Int) =>
        whenever(b != 0) {
          val expected = UnsignedInteger.fromIntBits(a).dividedBy(UnsignedInteger.fromIntBits(b))
          (UInt.fromIntBits(a) / UInt.fromIntBits(b)).toInt should be(expected.intValue)
        }
      }
      intercept[ArithmeticException] {
        UInt.fromIntBits(1) / UInt.fromIntBits(0)
      }
    }
    "should implement #mod" in {
      forAll { (a: Int, b: Int) =>
        whenever(b != 0) {
          val expected = UnsignedInteger.fromIntBits(a).mod(UnsignedInteger.fromIntBits(b))
          (UInt.fromIntBits(a) mod UInt.fromIntBits(b)).toInt should be(expected.intValue)
        }
      }
      intercept[ArithmeticException] {
        UInt.fromIntBits(1) mod UInt.fromIntBits(0)
      }
    }
    "should implement #toString" in {
      forAll { (a: Int) =>
        val expected = UnsignedInteger.fromIntBits(a).toString
        UInt.fromIntBits(a).toString should be(expected)
      }
    }
    "should implement #toString(radix)" in {
      forAll { (a: Int) =>
        for (radix <- Character.MIN_RADIX to Character.MAX_RADIX) {
          val expected = UnsignedInteger.fromIntBits(a).toString(radix)
          UInt.fromIntBits(a).toString(radix) should be(expected)
        }
      }
    }
    "should implement #toLong" in {
      forAll { (a: Int) =>
        val expected = UnsignedInteger.fromIntBits(a).longValue()
        UInt.fromIntBits(a).toLong should be(expected)
      }
    }
    "should implement #toInt" in {
      forAll { (a: Int) =>
        val expected = UnsignedInteger.fromIntBits(a).intValue()
        UInt.fromIntBits(a).toInt should be(expected)
      }
    }
    "should implement #toFloat" in {
      forAll { (a: Int) =>
        val expected = UnsignedInteger.fromIntBits(a).floatValue()
        UInt.fromIntBits(a).toFloat should be(expected)
      }
    }
    "should implement #toDouble" in {
      forAll { (a: Int) =>
        val expected = UnsignedInteger.fromIntBits(a).doubleValue()
        UInt.fromIntBits(a).toDouble should be(expected)
      }
    }
    "should implement #toBigInt" in {
      forAll { (a: Int) =>
        val expected = UnsignedInteger.fromIntBits(a).bigIntegerValue()
        UInt.fromIntBits(a).toBigInt.longValue should be(expected.longValue)
      }
    }
    "should implement #asScala" in {
      import UInt._
      forAll { (bits: Int) =>
        val guava: UnsignedInteger = UnsignedInteger.fromIntBits(bits)
        val mango: UInt = guava.asScala
        mango.toLong should be(guava.longValue)
      }
    }
    "should implement #compare" in {
      forAll { (a: Int, b: Int) =>
        val expected = UnsignedInts.compare(a, b)
        (UInt.fromIntBits(a) compare UInt.fromIntBits(b)) should be(expected)
      }
    }
    "should implement #asJava" in {
      import UInt._
      forAll { (bits: Int) =>
        val mango: UInt = UInt.fromIntBits(bits)
        val guava: UnsignedInteger = mango.asJava
        mango.toLong should be(guava.longValue)
      }
    }

    "should be serializeable" in {
      for (bits <- testInts) {
        SerializableTester.reserializeAndAssert(UInt.fromIntBits(bits))
      }
    }
  }

  "An UInt companion object" - {
    import UInt._
    "should implement #compare" in {
      forAll { (a: Int, b: Int) =>
        val expected = UnsignedInts.compare(a, b)
        compare(UInt.fromIntBits(a), UInt.fromIntBits(b)) should be(expected)
      }
    }
    "should implement #valueOf(Long)" in {
      val min: Long = 0
      val max: Long = (1L << 32) - 1
      for (value <- testLongs) {
        val expectSuccess = value >= min && value <= max
        try {
          UInt.valueOf(value).toLong should be(value)
          expectSuccess should be(true)
        } catch {
          case _: IllegalArgumentException => expectSuccess should be(false)
          case _: Throwable                => fail
        }
      }
    }
    "should implement #valueOf(BigInt)" in {
      val min: Long = 0
      val max: Long = (1L << 32) - 1
      for (value <- testLongs) {
        val expectSuccess = value >= min && value <= max
        try {
          UInt.valueOf(BigInt(value)).toLong should be(value)
          expectSuccess should be(true)
        } catch {
          case _: IllegalArgumentException => expectSuccess should be(false)
          case _: Throwable                => fail
        }
      }
    }
    "should implement #valueOf(String)" in {
      forAll(posNum[Long]) { (a: Long) =>
        val str = a.toString
        val expected = UnsignedInteger.valueOf(str).intValue()
        UInt.valueOf(str).toInt should be(expected)
      }
      intercept[NumberFormatException] {
        UInt.valueOf(java.lang.Long.toString(1L << 32))
      }
    }
    "should implement #valueOf(String,Int)" in {
      forAll(posNum[Long]) { (a: Long) =>
        for (radix <- (Character.MIN_RADIX to Character.MAX_RADIX)) {
          val str = java.lang.Long.toString(a, radix)
          val expected = UnsignedInts.parseUnsignedInt(str, radix)
          UInt.valueOf(str, radix).toInt should be(expected)
        }

        // loops through all legal radix values.
        for (radix <- (Character.MIN_RADIX to Character.MAX_RADIX)) {
          // tests can successfully parse a number string with this radix.
          val maxAsString = java.lang.Long.toString((1L << 32) - 1, radix)
          val expected = UnsignedInts.parseUnsignedInt(maxAsString, radix)
          UInt.valueOf(maxAsString, radix).toInt should be(expected)

          intercept[NumberFormatException] {
            // tests that we get exception whre an overflow would occur.
            val overflow: Long = 1L << 32
            val overflowAsString = java.lang.Long.toString(overflow, radix)
            UInt.valueOf(overflowAsString, radix)
          }
        }
      }

      // Valid radix values are Character.MIN_RADIX to Character.MAX_RADIX,
      // inclusive.
      intercept[NumberFormatException] {
        UInt.valueOf("0", Character.MIN_RADIX - 1)
      }

      intercept[NumberFormatException] {
        UInt.valueOf("0", Character.MAX_RADIX + 1)
      }

      intercept[NumberFormatException] {
        UInt.valueOf("0", -1)
      }
    }
    "should implement #decode" in {
      UInt.decode("0xffffffff").toInt should be(0xffffffff)
      UInt.decode("#12345678").toInt should be(0x12345678)
      UInt.decode("76543210").toInt should be(76543210)
      UInt.decode("0x13579135").toInt should be(0x13579135)
      UInt.decode("0X13579135").toInt should be(0x13579135)
      UInt.decode("0").toInt should be(0)

      intercept[NumberFormatException] {
        UInt.decode("0xfffffffff")
      }
      intercept[NumberFormatException] {
        UInt.decode("-5")
      }
      intercept[NumberFormatException] {
        UInt.decode("-0x5")
      }
    }
    "should implement #min" in {
      intercept[IllegalArgumentException] {
        UInt.min()
      }
      UInt.min(UInt.fromIntBits(least)) should be(UInt.fromIntBits(least))
      UInt.min(UInt.fromIntBits(greatest)) should be(UInt.fromIntBits(greatest))
      UInt.min(UInt.fromIntBits(8L.toInt),
        UInt.fromIntBits(6L.toInt),
        UInt.fromIntBits(7L.toInt),
        UInt.fromIntBits(0x12345678L.toInt),
        UInt.fromIntBits(0x5a4316b8L.toInt),
        UInt.fromIntBits(0xff1a618bL.toInt),
        UInt.fromIntBits(0L.toInt)) should be(UInt.fromIntBits(0L.toInt))
    }
    "should implement #max" in {
      intercept[IllegalArgumentException] {
        UInt.max()
      }
      UInt.max(UInt.fromIntBits(least)) should be(UInt.fromIntBits(least))
      UInt.max(UInt.fromIntBits(greatest)) should be(UInt.fromIntBits(greatest))
      UInt.max(UInt.fromIntBits(8L.toInt),
        UInt.fromIntBits(6L.toInt),
        UInt.fromIntBits(7L.toInt),
        UInt.fromIntBits(0x12345678L.toInt),
        UInt.fromIntBits(0x5a4316b8L.toInt),
        UInt.fromIntBits(0xff1a618bL.toInt),
        UInt.fromIntBits(0L.toInt)) should be(UInt.fromIntBits(0xff1a618bLtoInt))
    }
    "should implement #join" in {
      import UInt._
      join(",") should be("")
      join(",", UInt.fromIntBits(1)) should be("1")
      join(",", UInt.fromIntBits(1), UInt.fromIntBits(2)) should be("1,2")
      join(",", UInt.fromIntBits(-1), UInt.fromIntBits(Integer.MIN_VALUE)) should be("4294967295,2147483648")
      join(",", UInt.fromIntBits(1), UInt.fromIntBits(2), UInt.fromIntBits(3)) should be("1,2,3")
    }

    "should implement #lexicographicalComparator" in {
      val valuesInExpectedOrder = List(
        Array[UInt](),
        Array[UInt](UInt.fromIntBits(least)),
        Array[UInt](UInt.fromIntBits(least), UInt.fromIntBits(least)),
        Array[UInt](UInt.fromIntBits(least), UInt.valueOf(1L)),
        Array[UInt](UInt.valueOf(1L)),
        Array[UInt](UInt.valueOf(1L), UInt.fromIntBits(least)),
        Array[UInt](UInt.fromIntBits(greatest), UInt.fromIntBits(greatest - 1L.toInt)),
        Array[UInt](UInt.fromIntBits(greatest), UInt.fromIntBits(greatest)),
        Array[UInt](UInt.fromIntBits(greatest), UInt.fromIntBits(greatest), UInt.fromIntBits(greatest)))

      val comparator = UInt.lexicographicalComparator

      for (i <- 0 until valuesInExpectedOrder.size) {
        val t = valuesInExpectedOrder(i)
        for (j <- 0 until i) {
          val lesser = valuesInExpectedOrder(j)
          comparator.compare(lesser, t) < 0 should be(true)
        }
        comparator.compare(t, t) should be(0)
        for (j <- i + 1 until valuesInExpectedOrder.size) {
          val greater = valuesInExpectedOrder(j)
          comparator.compare(greater, t) > 0 should be(true)
        }
      }
    }
  }
}