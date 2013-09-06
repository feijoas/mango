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
 * The code of this project is a port of (or wrapper around) the guava-libraries.
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
import scala.collection.mutable.Builder
import scala.math.Ordering.Int

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect
import org.feijoas.mango.common.collect.BoundType._
import org.mockito.Mockito._
import org.scalatest.FreeSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.PropertyChecks

import com.google.common.collect.{ RangeSet => GuavaRangeSet }

/** Behavior which all [[RangeSetWrappers]] have in common
 */
private[mango] trait RangeSetWrapperBehaviours extends FreeSpec with PropertyChecks with ShouldMatchers with MockitoSugar {
  this: FreeSpec =>

  def immutableWrapper[Repr <: RangeSetWrapperLike[Int, Int.type, Repr] with RangeSet[Int, Int.type]](constructor: (GuavaRangeSet[AsOrdered[Int]]) => Repr) = {
    val mocked = mock[GuavaRangeSet[AsOrdered[Int]]]
    val withMock = constructor(mocked)
    "it should forward all immutable methods to guava " - {
      "should forward #asRanges" in {
        withMock.asRanges
        verify(mocked).asRanges()
      }
      "should forward #complement" in {
        withMock.complement
        verify(mocked).complement()
      }
      "should forward #contains" in {
        withMock.contains(5)
        verify(mocked).contains(5)
      }
      "should forward #encloses" in {
        withMock.encloses(Range.open(5, 6))
        verify(mocked).encloses(Range.open(5, 6).asJava)
      }
      "should forward #enclosesAll" in {
        withMock.enclosesAll(withMock)
        verify(mocked).enclosesAll(mocked)
      }
      "should forward #isEmpt" in {
        withMock.isEmpty
        verify(mocked).isEmpty()
      }
      "should forward #rangeContaining" in {
        withMock.rangeContaining(5)
        verify(mocked).rangeContaining(5)
      }
      "should forward #span" in {
        withMock.span
        verify(mocked).span()
      }
      "should forward #subRangeSet" in {
        withMock.subRangeSet(Range.open(5, 6))
        verify(mocked).subRangeSet(Range.open(5, 6).asJava)
      }
    }
  }

  def mutableWrapper[Repr <: mutable.RangeSetWrapperLike[Int, Int.type, Repr] with mutable.RangeSet[Int, Int.type]](constructor: (GuavaRangeSet[AsOrdered[Int]]) => Repr) = {
    // forward all methods like immutable RangeSetWrapperLike
    immutableWrapper(constructor)

    def fixture = {
      val mocked = mock[GuavaRangeSet[AsOrdered[Int]]]
      val withMock = constructor(mocked)
      (mocked, withMock)
    }

    // additional mutators
    val range = Range.open(5, 6)
    val rangeColl = List(Range.closed(1, 2), Range.open(5, 6))

    "it should forward all mutable methods to guava " - {
      "should forward #add" in {
        val (mocked, withMock) = fixture
        withMock.add(range)
        verify(mocked).add(range.asJava)
      }
      "should forward #+=" in {
        val (mocked, withMock) = fixture
        withMock += range
        verify(mocked).add(range.asJava)
      }
      "should forward #++=" in {
        val (mocked, withMock) = fixture
        withMock ++= rangeColl
        verify(mocked).add(rangeColl(0).asJava)
        verify(mocked).add(rangeColl(1).asJava)
      }
      "should forward #addAll" in {
        val (mocked, withMock) = fixture
        withMock.addAll(withMock)
        verify(mocked).addAll(mocked)
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
        withMock --= rangeColl
        verify(mocked).remove(rangeColl(0).asJava)
        verify(mocked).remove(rangeColl(1).asJava)
      }
      "should forward #removeAll" in {
        val (mocked, withMock) = fixture
        withMock.removeAll(withMock)
        verify(mocked).removeAll(mocked)
      }
      "should forward #clear" in {
        val (mocked, withMock) = fixture
        withMock.clear
        verify(mocked).clear
      }
    }
  }
}