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

import com.google.common.primitives.UnsignedBytes

import UByte.join
import UByte.max
import UByte.min

/** Tests for [[UByte]]
 *
 *  @author Markus Schneider
 *  @since 0.10
 */
class UByteTest extends FreeSpec with PropertyChecks {
  val least: Byte = 0
  val greatest: Byte = 255.toByte
  val values: List[Byte] = List(least, 127, 128.toByte, 129.toByte, greatest)

  "An UByte" - {
    "should implement #toInt" in {
      for (value <- 0 to 255) {
        UByte.valueOf(value).toInt should be (value)
      }
    }
    "should implement #toLong" in {
      for (value <- 0 to 255) {
        UByte.valueOf(value).toLong should be (value.toLong)
      }
    }
    "should implement #toFloat" in {
      for (value <- 0 to 255) {
        UByte.valueOf(value).toFloat should be (value.toFloat)
      }
    }
    "should implement #toDouble" in {
      for (value <- 0 to 255) {
        UByte.valueOf(value).toDouble should be (value.toDouble)
      }
    }
    "should implement #toBigInt" in {
      for (value <- 0 to 255) {
        UByte.valueOf(value).toBigInt should be (BigInt(value))
      }
    }
    "should implement #UByte(Long)" in {
      for (value <- 0 to 255) {
        UByte(value).toInt should be (value)
      }
      intercept[IllegalArgumentException]{
        UByte(256)
      }
      intercept[IllegalArgumentException]{
        UByte(-1)
      }
    }
    "should implement #valueOf(Long)" in {
      for (value <- 0 to 255) {
        UByte.valueOf(value).toInt should be (value)
      }
      intercept[IllegalArgumentException]{
        UByte.valueOf(256)
      }
      intercept[IllegalArgumentException]{
        UByte.valueOf(-1)
      }
    }
    "should implement #valueOf(String)" in {
      for (value <- 0 to 255) {
        UByte.valueOf(value.toString).toInt should be (value)
      }
      intercept[IllegalArgumentException]{
        UByte.valueOf("256")
      }
      intercept[IllegalArgumentException]{
        UByte.valueOf("-1")
      }
    }
    "should implement #valueOf(String, radix)" in {
      for (radix <- Character.MIN_RADIX until Character.MAX_RADIX) {
        for (value <- 0 to 255) {
          val str = Integer.toString(value, radix)
          UByte.valueOf(str, radix).value should be (UnsignedBytes.parseUnsignedByte(str, radix))
        }
        intercept[IllegalArgumentException]{
          UByte.valueOf(Integer.toString(1000, radix), radix)
        }
        intercept[IllegalArgumentException]{
          UByte.valueOf(Integer.toString(-1, radix), radix)
        }
        intercept[IllegalArgumentException]{
          UByte.valueOf(Integer.toString(-128, radix), radix)
        }
        intercept[IllegalArgumentException]{
          UByte.valueOf(Integer.toString(256, radix), radix)
        }
      }
    }
    "should implement #saturatedValueOf(Long)" in {
      for (value <- 0 to 255) {
        UByte.saturatedValueOf(value).toInt should be (value)
      }
      UByte.saturatedValueOf(256L).value should be (greatest)
      UByte.saturatedValueOf(-1L).value should be (least)
      UByte.saturatedValueOf(Long.MaxValue).value should be (greatest)
      UByte.saturatedValueOf(Long.MinValue).value should be (least)
    }
    "should implement #compare" in {
      for (x <- 0 to 255; y <- 0 to 255) {
        Math.signum(UByte.compare(UByte(x), UByte(y))) should be (Math.signum(x.compare(y)))
      }
      for (x <- 0 to 255; y <- 0 to 255) {
        Math.signum(UByte(x) compare UByte(y)) should be (Math.signum(x.compare(y)))
      }
    }
    "should implement #max" in {
      import UByte._
      max(UByte(0)) should be (UByte(0))
      max(UByte(255)) should be (UByte(255))
      max(UByte(0), UByte(255), UByte(1), UByte(128)) should be(UByte(255))

      intercept[IllegalArgumentException]{
        max()
      }
    }
    "should implement #min" in {
      import UByte._
      min(UByte(0)) should be (UByte(0))
      min(UByte(255)) should be (UByte(255))
      min(UByte(0), UByte(255), UByte(1), UByte(128)) should be(UByte(0))

      intercept[IllegalArgumentException]{
        min()
      }
    }
    "should implement #toSting" in {
      for (value <- 0 to 255) {
        UByte(value).toString should be (UnsignedBytes.toString(value.toByte))
      }
    }
    "should implement #toSting(radix)" in {
      for (value <- 0 to 255; radix <- Character.MIN_RADIX until Character.MAX_RADIX) {
        UByte(value).toString(radix) should be (UnsignedBytes.toString(value.toByte, radix))
      }
    }
    "should implement #MaxPowerOfTwo" in {
      UByte.MaxPowerOfTwo should be (UnsignedBytes.MAX_POWER_OF_TWO)
    }
    "should implement #MaxValue" in {
      UByte.MaxValue should be (UnsignedBytes.MAX_VALUE)
    }
    "should implement #join" in {
      import UByte._
      join(",") should be("")
      join(",", UByte(1)) should be("1")
      join(",", UByte(1), UByte(2)) should be("1,2")
      join(",", UByte(255), UByte(0)) should be("255,0")
    }
    "should implement #lexicographicalComparator" in {
      val valuesInExpectedOrder = List(
        Array[UByte](),
        Array[UByte](UByte(0)),
        Array[UByte](UByte(0), UByte(0)),
        Array[UByte](UByte(0), UByte(1)),
        Array[UByte](UByte(1)),
        Array[UByte](UByte(1), UByte(0)),
        Array[UByte](UByte(255), UByte(254)),
        Array[UByte](UByte(255), UByte(255)),
        Array[UByte](UByte(255), UByte(255), UByte(255)))

      val comparator = UByte.lexicographicalComparator

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