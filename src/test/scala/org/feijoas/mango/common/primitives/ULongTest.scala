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
import com.google.common.primitives.UnsignedLong
import com.google.common.primitives.UnsignedInts
import scala.collection.convert.decorateAll._
import java.math.BigInteger
import org.scalacheck.Gen._
import com.google.common.testing.SerializableTester
import com.google.common.primitives.UnsignedLongs

/** Tests for [[ULong]]
 *
 *  @author Markus Schneider
 *  @since 0.10
 */
class ULongTest extends FreeSpec with PropertyChecks {
  val least: Long = 0L
  val greatest: Long = 0xffffffffffffffffL
  val testLongs = (for (i <- -3 to 3) yield List[Long](i,
    Int.MaxValue.toLong + i,
    Int.MinValue.toLong + i,
    Long.MaxValue.toLong + i,
    Long.MinValue.toLong + i)).flatten
  val testBigInts = (for (i <- -3 to 3) yield List[BigInt](BigInt(i),
    BigInt(Long.MaxValue) + i,
    BigInt(Long.MinValue) + i,
    BigInt(Int.MaxValue) + i,
    BigInt(Int.MinValue) + i,
    (BigInt(1) << 63) + i,
    (BigInt(1) << 64) + i)).flatten

  "An ULong " - {
    "should implement +" in {
      forAll { (a: Long, b: Long) =>
        val expected = UnsignedLong.fromLongBits(a).plus(UnsignedLong.fromLongBits(b))
        (ULong.fromLongBits(a) + ULong.fromLongBits(b)).toLong should be(expected.longValue)
      }
    }
    "should implement -" in {
      forAll { (a: Long, b: Long) =>
        val expected = UnsignedLong.fromLongBits(a).minus(UnsignedLong.fromLongBits(b))
        (ULong.fromLongBits(a) - ULong.fromLongBits(b)).toLong should be(expected.longValue)
      }
    }
    "should implement *" in {
      forAll { (a: Long, b: Long) =>
        val expected = UnsignedLong.fromLongBits(a).times(UnsignedLong.fromLongBits(b))
        (ULong.fromLongBits(a) * ULong.fromLongBits(b)).toLong should be(expected.longValue)
      }
    }
    "should implement /" in {
      forAll { (a: Long, b: Long) =>
        whenever(b != 0) {
          val expected = UnsignedLong.fromLongBits(a).dividedBy(UnsignedLong.fromLongBits(b))
          (ULong.fromLongBits(a) / ULong.fromLongBits(b)).toLong should be(expected.longValue)
        }
      }
      intercept[ArithmeticException] {
        ULong.fromLongBits(1) / ULong.fromLongBits(0)
      }
    }
    "should implement #mod" in {
      forAll { (a: Long, b: Long) =>
        whenever(b != 0) {
          val expected = UnsignedLong.fromLongBits(a).mod(UnsignedLong.fromLongBits(b))
          (ULong.fromLongBits(a) mod ULong.fromLongBits(b)).toLong should be(expected.longValue)
        }
      }
      intercept[ArithmeticException] {
        ULong.fromLongBits(1) mod ULong.fromLongBits(0)
      }
    }
    "should implement #toString" in {
      forAll { (a: Long) =>
        val expected = UnsignedLong.fromLongBits(a).toString
        ULong.fromLongBits(a).toString should be(expected)
      }
    }
    "should implement #toString(radix)" in {
      forAll { (a: Long) =>
        for (radix <- Character.MIN_RADIX to Character.MAX_RADIX) {
          val expected = UnsignedLong.fromLongBits(a).toString(radix)
          ULong.fromLongBits(a).toString(radix) should be(expected)
        }
      }
    }
    "should implement #toInt" in {
      forAll { (a: Long) =>
        val expected = UnsignedLong.fromLongBits(a).intValue()
        ULong.fromLongBits(a).toInt should be(expected)
      }
    }
    "should implement #toLong" in {
      forAll { (a: Long) =>
        val expected = UnsignedLong.fromLongBits(a).longValue()
        ULong.fromLongBits(a).toLong should be(expected)
      }
    }
    "should implement #toFloat" in {
      forAll { (a: Long) =>
        val expected = UnsignedLong.fromLongBits(a).floatValue()
        ULong.fromLongBits(a).toFloat should be(expected)
      }
    }
    "should implement #toDouble" in {
      forAll { (a: Long) =>
        val expected = UnsignedLong.fromLongBits(a).doubleValue()
        ULong.fromLongBits(a).toDouble should be(expected)
      }
    }
    "should implement #toBigInt" in {
      forAll { (a: Long) =>
        val expected = UnsignedLong.fromLongBits(a).bigIntegerValue()
        ULong.fromLongBits(a).toBigInt.longValue should be(expected.longValue)
      }
    }
    "should implement #compare" in {
      forAll { (a: Long, b: Long) =>
        val expected = UnsignedLongs.compare(a, b)
        (ULong.fromLongBits(a) compare ULong.fromLongBits(b)) should be(expected)
      }
    }
    "should implement #asScala" in {
      import ULong._
      forAll { (bits: Long) =>
        val guava: UnsignedLong = UnsignedLong.fromLongBits(bits)
        val mango: ULong = guava.asScala
        mango.toLong should be(guava.longValue)
      }
    }
    "should implement #asJava" in {
      import ULong._
      forAll { (bits: Long) =>
        val mango: ULong = ULong.fromLongBits(bits)
        val guava: UnsignedLong = mango.asJava
        mango.toLong should be(guava.longValue)
      }
    }

    "should be serializeable" in {
      forAll { (bits: Long) =>
        val ulong = ULong.fromLongBits(bits)
        SerializableTester.reserializeAndAssert(ulong)
        ulong should be(ULong.fromLongBits(bits))
      }
    }
  }

  "An ULong companion object" - {
    import ULong._
    "should implement #compare" in {
      forAll { (a: Long, b: Long) =>
        val expected = UnsignedLongs.compare(a, b)
        compare(ULong.fromLongBits(a), ULong.fromLongBits(b)) should be(expected)
      }
    }
    "should implement #valueOf(Long)" in {
      for (value <- testLongs) {
        val expectSuccess = value >= 0
        try {
          ULong.valueOf(value).toLong should be(value)
          expectSuccess should be(true)
        } catch {
          case _: IllegalArgumentException => expectSuccess should be(false)
          case _: Throwable                => fail
        }
      }
    }

    "should implement #valueOf(BigInt)" in {
      val min = BigInt(0)
      val max = UnsignedLong.MAX_VALUE.bigIntegerValue()
      for (big <- testBigInts) {
        val expectSuccess = big.compare(min) >= 0 && big.compare(max) <= 0
        try {
          ULong.apply(big).toBigInt should be(big)
          expectSuccess should be(true)
        } catch {
          case _: IllegalArgumentException => expectSuccess should be(false)
          case _: Throwable                => fail
        }
      }
    }
    "should implement #valueOf(String)" in {
      ULong.valueOf("18446744073709551615").toLong should be(0xffffffffffffffffL)
      ULong.valueOf("9223372036854775807").toLong should be(0x7fffffffffffffffL)
      ULong.valueOf("18382112080831834642").toLong should be(0xff1a618b7f65ea12L)
      ULong.valueOf("6504067269626408013").toLong should be(0x5a4316b8c153ac4dL)
      ULong.valueOf("7851896530399809066").toLong should be(0x6cf78a4b139a4e2aL)

      intercept[NumberFormatException] {
        ULong.valueOf("18446744073709551616")
      }
    }
    "should implement #valueOf(String,Int)" in {
      ULong.valueOf("ffffffffffffffff", 16).toLong should be(0xffffffffffffffffL)
      ULong.valueOf("1234567890abcdef", 16).toLong should be(0x1234567890abcdefL)

      val max = BigInteger.ZERO.setBit(64).subtract(BigInteger.ONE)
      // loops through all legal radix values.
      for (radix <- Character.MIN_RADIX to Character.MAX_RADIX) {
        // tests can successfully parse a number string with this radix.
        val maxAsString = max.toString(radix)
        ULong.valueOf(maxAsString, radix).toLong should be(max.longValue())

        intercept[NumberFormatException] {
          // tests that we get exception whre an overflow would occur.
          val overflow = max.add(BigInteger.ONE)
          val overflowAsString = overflow.toString(radix)
          ULong.valueOf(overflowAsString, radix)
        }
      }

      // Valid radix values are Character.MIN_RADIX to Character.MAX_RADIX,
      // inclusive.
      intercept[NumberFormatException] {
        ULong.valueOf("0", Character.MIN_RADIX - 1)
      }

      intercept[NumberFormatException] {
        ULong.valueOf("0", Character.MAX_RADIX + 1)
      }

      intercept[NumberFormatException] {
        ULong.valueOf("0", -1)
      }
    }
    "should implement #decode" in {

      ULong.decode("0xffffffffffffffff").toLong should be(0xffffffffffffffffL)
      ULong.decode("#1234567890abcdef").toLong should be(0x1234567890abcdefL)
      ULong.decode("987654321012345678").toLong should be(987654321012345678L)
      ULong.decode("0X135791357913579").toLong should be(0x135791357913579L)
      ULong.decode("0").toLong should be(0)

      intercept[NumberFormatException] {
        ULong.decode("0xfffffffffffffffff")
      }
      intercept[NumberFormatException] {
        ULong.decode("-5")
      }
      intercept[NumberFormatException] {
        ULong.decode("-0x5")
      }
    }
    "should implement #min" in {
      intercept[IllegalArgumentException] {
        ULong.min()
      }
      ULong.min(ULong.fromLongBits(least)) should be(ULong.fromLongBits(least))
      ULong.min(ULong.fromLongBits(greatest)) should be(ULong.fromLongBits(greatest))
      ULong.min(ULong.fromLongBits(0L),
        ULong.fromLongBits(0x5a4316b8c153ac4dL),
        ULong.fromLongBits(8L),
        ULong.fromLongBits(100L),
        ULong.fromLongBits(0x6cf78a4b139a4e2aL),
        ULong.fromLongBits(0L),
        ULong.fromLongBits(0xff1a618b7f65ea12L)) should be(ULong.fromLongBits(0L))
    }
    "should implement #max" in {
      intercept[IllegalArgumentException] {
        ULong.max()
      }
      ULong.max(ULong.fromLongBits(least)) should be(ULong.fromLongBits(least))
      ULong.max(ULong.fromLongBits(greatest)) should be(ULong.fromLongBits(greatest))
      ULong.max(ULong.fromLongBits(0L),
        ULong.fromLongBits(0x5a4316b8c153ac4dL),
        ULong.fromLongBits(8L),
        ULong.fromLongBits(100L),
        ULong.fromLongBits(0x6cf78a4b139a4e2aL),
        ULong.fromLongBits(0L),
        ULong.fromLongBits(0xff1a618b7f65ea12L)) should be(ULong.fromLongBits(0xff1a618b7f65ea12L))
    }
    "should implement #join" in {
      import ULong._
      join(",") should be("")
      join(",", ULong.fromLongBits(1)) should be("1")
      join(",", ULong.fromLongBits(1), ULong.fromLongBits(2)) should be("1,2")
      join(",", ULong.fromLongBits(-1), ULong.fromLongBits(Long.MinValue)) should be("18446744073709551615,9223372036854775808")
      join(",", ULong.fromLongBits(1), ULong.fromLongBits(2), ULong.fromLongBits(3)) should be("1,2,3")
    }

    "should implement #lexicographicalComparator" in {
      val valuesInExpectedOrder = List(
        Array[ULong](),
        Array[ULong](ULong.fromLongBits(least)),
        Array[ULong](ULong.fromLongBits(least), ULong.fromLongBits(least)),
        Array[ULong](ULong.fromLongBits(least), ULong.valueOf(1L)),
        Array[ULong](ULong.valueOf(1L)),
        Array[ULong](ULong.valueOf(1L), ULong.fromLongBits(least)),
        Array[ULong](ULong.fromLongBits(greatest), ULong.fromLongBits(greatest - 1L.toLong)),
        Array[ULong](ULong.fromLongBits(greatest), ULong.fromLongBits(greatest)),
        Array[ULong](ULong.fromLongBits(greatest), ULong.fromLongBits(greatest), ULong.fromLongBits(greatest)))

      val comparator = ULong.lexicographicalComparator

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