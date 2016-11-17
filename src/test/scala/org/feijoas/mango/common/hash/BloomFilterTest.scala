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
package org.feijoas.mango.common.hash

import scala.annotation.implicitNotFound
import scala.annotation.meta.beanGetter
import scala.annotation.meta.beanSetter
import scala.annotation.meta.field
import scala.annotation.meta.getter
import scala.annotation.meta.setter

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.hash.Funnel.byteArrayFunnel
import org.feijoas.mango.common.hash.Funnel.intFunnel
import org.feijoas.mango.common.hash.Funnel.longFunnel
import org.feijoas.mango.common.hash.Funnel.stringFunnel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.scalatest.FlatSpec
import org.scalatest.Matchers.be
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.Matchers.not
import org.scalatest.mockito.MockitoSugar

import com.google.common.hash.PrimitiveSink
import com.google.common.primitives.Ints
import com.google.common.testing.SerializableTester

/**
 * Tests for [[BloomFilter]]
 *
 *  @author Markus Schneider
 *  @since 0.6 (copied from guava-libraries)
 */
class BloomFilterTest extends FlatSpec with MockitoSugar {

  it should "throw an exception if the arguments are out of range" in {
    intercept[IllegalArgumentException] {
      BloomFilter.create[CharSequence](-1)
    }
    intercept[IllegalArgumentException] {
      BloomFilter.create[CharSequence](-1, 0.03)
    }
    intercept[IllegalArgumentException] {
      BloomFilter.create[CharSequence](1, 0.0)
    }
    intercept[IllegalArgumentException] {
      BloomFilter.create[CharSequence](1, 1.0)
    }
    intercept[NullPointerException] {
      BloomFilter.create[CharSequence](1)(null)
    }
  }

  it should "fail when more than 255 HashFunctions are needed" in {
    val n = 1000
    val p = 0.00000000000000000000000000000000000000000000000000000000000000000000000000000001
    intercept[IllegalArgumentException] {
      BloomFilter.create[CharSequence](n, p)
    }
  }

  it should "be able to create a copy" in {
    val original = BloomFilter.create[CharSequence](100)
    val copy = original.copy
    assertNotSame(original, copy)
    assertEquals(original, copy)
  }

  it should "be equal to another instance with the same elements" in {

    val bf1 = BloomFilter.create[CharSequence](100)
    bf1.put("1")
    bf1.put("2")

    val bf2 = BloomFilter.create[CharSequence](100)
    bf2.put("1")
    bf2.put("2")

    bf1 should be(bf2)
    bf2.put("3")
    bf1 should not be (bf2)
  }

  it should "be equal to another instance even if the Funnels are different instances" in {
    case class CustomFunnel() extends Funnel[Any] {
      override def funnel(any: Any, bytePrimitiveSink: PrimitiveSink) {
        bytePrimitiveSink.putInt(any.hashCode())
      }
    }
    BloomFilter.create(100)(new CustomFunnel) should be(BloomFilter.create(100)(new CustomFunnel))
  }

  it should "be serializeable" in {
    SerializableTester.reserializeAndAssert(BloomFilter.create[CharSequence](100))
    SerializableTester.reserializeAndAssert(BloomFilter.create[Int](100))
    SerializableTester.reserializeAndAssert(BloomFilter.create[Long](100))
    SerializableTester.reserializeAndAssert(BloomFilter.create[Array[Byte]](100))
    SerializableTester.reserializeAndAssert(BloomFilter.create[CharSequence](100))
  }

  it should "perform basic operations" in {
    object BAD_FUNNEL extends Funnel[Any] {
      override def funnel(any: Any, bytePrimitiveSink: PrimitiveSink) {
        bytePrimitiveSink.putInt(any.hashCode())
      }
    }

    var fpr = 0.0000001
    while (fpr < 0.1) {
      var expectedInsertions = 1
      while (expectedInsertions <= 10000) {
        checkSanity(BloomFilter.create(expectedInsertions, fpr)(BAD_FUNNEL))
        expectedInsertions = expectedInsertions * 10

      }
      fpr = fpr * 10
    }
  }

  it should "true if the bloom filter's bits changed" in {
    for (i <- 0 until 10) {
      val bf = BloomFilter.create[CharSequence](100)
      for (j <- 0 until 10) {
        val value = new Object().toString()
        val mightContain = bf.mightContain(value)
        val put = bf.put(value)
        put should be(!mightContain)
      }
    }
  }

  it should "testJavaSerialization()" in {
    val bf = BloomFilter.create[Array[Byte]](100)
    for (i <- 0 until 10) {
      bf.put(Ints.toByteArray(i))
    }

    val copy = SerializableTester.reserialize(bf)
    for (i <- 0 until 10) {
      copy.mightContain(Ints.toByteArray(i)) should be(true)
    }
    bf.expectedFpp() should be(copy.expectedFpp())

    SerializableTester.reserializeAndAssert(bf)
  }

  private def checkSanity(bf: BloomFilter[Any]) = {
    bf.mightContain(new Object()) should be(false)
    bf.apply(new Object()) should be(false)
    for (i <- 0 until 100) {
      val o = new Object
      bf.put(o)
      bf.mightContain(o) should be(true)
      bf.apply(o) should be(true)
    }
  }
}
