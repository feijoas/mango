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
package org.feijoas.mango.common.collect

import scala.annotation.meta.beanGetter
import scala.annotation.meta.beanSetter
import scala.annotation.meta.field
import scala.annotation.meta.getter
import scala.annotation.meta.setter
import scala.math.Ordering.Int

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect.AsOrdered.asOrdered
import org.feijoas.mango.common.collect.Range.asGuavaRangeConverter
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.FreeSpec
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks

import com.google.common.{ collect => gcc }

/**
 * Behavior which all [[RangeMapWrappers]] have in common
 */
private[mango] trait RangeMapWrapperBehaviours extends FreeSpec with PropertyChecks with MockitoSugar {
  this: FreeSpec =>

  def immutableWrapper[Repr <: RangeMapWrapperLike[Int, String, Int.type, Repr] with RangeMap[Int, String, Int.type]](constructor: (gcc.RangeMap[AsOrdered[Int], String]) => Repr) = {
    val mocked = mock[gcc.RangeMap[AsOrdered[Int], String]]
    val withMock = constructor(mocked)
    "it should forward all immutable methods to guava " - {
      "should forward #asMapOfRanges" in {
        withMock.asMapOfRanges
        verify(mocked).asMapOfRanges()
      }
      "should forward #get" in {
        withMock.get(5)
        verify(mocked).get(5)
      }
      "should forward #getEntry" in {
        withMock.getEntry(5)
        verify(mocked).getEntry(5)
      }
      "should forward #span" in {
        val nonEmpty = gcc.Maps.newHashMap[gcc.Range[AsOrdered[Int]], String]
        nonEmpty.put(Range.open(3, 4).asJava, "a")
        when(mocked.asMapOfRanges).thenReturn(nonEmpty)
        withMock.span
        verify(mocked, times(1)).span()
      }
      "should forward #subRangeMap" in {
        withMock.subRangeMap(Range.open(5, 6))
        verify(mocked).subRangeMap(Range.open(5, 6).asJava)
      }
    }
  }

  def mutableWrapper[Repr <: mutable.RangeMapWrapperLike[Int, String, Int.type, Repr] with mutable.RangeMap[Int, String, Int.type]](constructor: (gcc.RangeMap[AsOrdered[Int], String]) => Repr) = {
    // forward all methods like immutable RangeMapWrapperLike
    immutableWrapper(constructor)

    def fixture = {
      val mocked = mock[gcc.RangeMap[AsOrdered[Int], String]]
      val withMock = constructor(mocked)
      (mocked, withMock)
    }

    // additional mutators
    val range = Range.open(5, 6)
    val rangeColl = Map(Range.closed(1, 2) -> "1", Range.open(5, 6) -> "2")

    "it should forward all mutable methods to guava " - {
      "should forward #put" in {
        val (mocked, withMock) = fixture
        withMock.put(range, "1")
        verify(mocked).put(range.asJava, "1")
      }
      "should forward #+=" in {
        val (mocked, withMock) = fixture
        withMock += range -> "1"
        verify(mocked).put(range.asJava, "1")
      }
      "should forward #++=" in {
        val (mocked, withMock) = fixture
        withMock ++= rangeColl
        verify(mocked).put(Range.closed(1, 2).asJava, "1")
        verify(mocked).put(Range.open(5, 6).asJava, "2")
      }
      "should forward #putAll" in {
        val (mocked, withMock) = fixture
        withMock.putAll(withMock)
        verify(mocked).putAll(mocked)
      }
      "should forward #remove" in {
        val (mocked, withMock) = fixture
        withMock.remove(range)
        verify(mocked).remove(range.asJava)
      }
      "should forward #-=" in {
        val (mocked, withMock) = fixture
        withMock -= range
        verify(mocked).remove(range.asJava)
      }
      "should forward #--=" in {
        val (mocked, withMock) = fixture
        withMock --= rangeColl.keySet
        verify(mocked).remove(Range.closed(1, 2).asJava)
        verify(mocked).remove(Range.open(5, 6).asJava)
      }
      "should forward #clear" in {
        val (mocked, withMock) = fixture
        withMock.clear
        verify(mocked).clear
      }
    }
  }
}
