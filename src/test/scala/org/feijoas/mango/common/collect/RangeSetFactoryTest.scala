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
import org.feijoas.mango.common.collect.immutable.ImmutableRangeSetWrapper
import org.scalatest.FreeSpec
import org.scalatest.Matchers.be
import org.scalatest.Matchers.convertToAnyShouldWrapper

/** Tests for [[RangeSetFactory]]
 *
 *  @author Markus Schneider
 *  @since 0.8
 */
class RangeSetFactoryTest extends FreeSpec {
  "RangeSetFactory" - {
    "should implement #empty" in {
      val rangeSet = DummyRangeSetFactory.empty[Int, Int.type]
      rangeSet.isEmpty should be(true)
      rangeSet.asRanges should be(Set())
    }

    "should implement #apply(range1, ...)" - {
      "given (5,6) it should return a range set with {(5,6)}" in {
        val rangeSet = DummyRangeSetFactory(Range.open(5, 6))
        rangeSet.asRanges should be(Set(Range.open(5, 6)))
      }
      "given (5,6) and [1,2] it should return a range set with {[1,2], (5,6)}" in {
        val rangeSet = DummyRangeSetFactory(Range.open(5, 6), Range.closed(1, 2))
        rangeSet.asRanges should be(Set(Range.open(5, 6), Range.closed(1, 2)))
      }
    }
    "should implement #apply(otherRangeSet)" - {
      "given the range set contains (5,6) and [1,2]" - {
        val rangeSet = DummyRangeSetFactory(Range.open(5, 6), Range.closed(1, 2))
        "#apply(otherRangeSet) should return a RangeSet with {[1,2], (5,6)}" in {
          val copy = DummyRangeSetFactory(rangeSet)
          copy should be(rangeSet)
          copy.asRanges should be(rangeSet.asRanges)
        }
      }
      "given the range set is empty" - {
        val rangeSet = DummyRangeSetFactory.empty[Int, Int.type]
        "#apply(otherRangeSet) should return an empty range set" in {
          val copy = DummyRangeSetFactory(rangeSet)
          copy should be(rangeSet)
          copy.asRanges should be(rangeSet.asRanges)
          copy.isEmpty should be(true)
        }
      }
    }
  }
}

private[mango] object DummyRangeSetFactory extends RangeSetFactory[RangeSet] {
  def newBuilder[C, O <: Ordering[C]](implicit ord: O): Builder[Range[C, O], RangeSet[C, O]] = ImmutableRangeSetWrapper.newBuilder[C, O]
}