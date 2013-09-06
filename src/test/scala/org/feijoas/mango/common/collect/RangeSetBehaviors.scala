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
import scala.math.Ordering.Int
import org.feijoas.mango.common.annotations.Beta
import org.scalatest.FreeSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.prop.PropertyChecks
import org.feijoas.mango.common.collect.BoundType._
import scala.collection.mutable.Builder
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

/** Behavior which all [[RangeSet]] have in common
 */
private[mango] trait RangeSetBehaviors extends FreeSpec with PropertyChecks with ShouldMatchers with MockitoSugar {
  this: FreeSpec =>

  def mutableRangeSet(newBuilder: => Builder[Range[Int, Int.type], mutable.RangeSet[Int, Int.type]]) = {

    val MIN_BOUND = -1
    val MAX_BOUND = 1
    val queryBuilder = List.newBuilder[Range[Int, Int.type]]

    queryBuilder += Range.all()

    for (i <- MIN_BOUND to MAX_BOUND) {
      for (boundType <- List(Open, Closed)) {
        queryBuilder += Range.upTo(i, boundType)
        queryBuilder += Range.downTo(i, boundType)
      }

      queryBuilder += Range.singleton(i)
      queryBuilder += Range.openClosed(i, i)
      queryBuilder += Range.closedOpen(i, i)

      for (lowerBoundType <- List(Open, Closed)) {
        for (j <- i + 1 to MAX_BOUND) {
          for (upperBoundType <- List(Open, Closed)) {
            queryBuilder += Range.range(i, lowerBoundType, j, upperBoundType)
          }
        }
      }
    }
    val QUERY_RANGES = queryBuilder.result

    /*
     * #complement 
     */
    "should implement mutable #complement" - {
      "given the RangeSet contains a single range" - {
        "the #complement should contain Range.all - the containing range" in {
          for (range <- QUERY_RANGES) {
            val rangeSet = newBuilder.result
            rangeSet.add(range)
            val complement = newBuilder.result
            complement.add(Range.all[Int, Int.type])
            complement.remove(range)
            rangeSet.complement should be(complement)
          }
        }
      }
    }
    "connected ranges should be coalesced" - {
      "given the RangeSet is empty" - {
        val rangeSet = newBuilder.result
        "the #coalesced should pass" in {
          testCoalesced(rangeSet)
        }
      }
    }
    "should enclose a range if one of the single ranges encloses the range" - {
      "given the RangeSet is empty" - {
        val rangeSet = newBuilder.result
        "it should not enclose any range" in {
          QUERY_RANGES foreach { range => rangeSet.encloses(range) should be(false) }
        }
      }
      "given the RangeSet contains a single range" - {
        "it should only enclose that range" in {
          for (range <- QUERY_RANGES) {
            val rangeSet = newBuilder.result
            rangeSet.add(range)

            // test range Set
            QUERY_RANGES foreach { queryRange =>
              val shouldEnclose = rangeSet.asRanges.find(_.encloses(queryRange)).isDefined
              rangeSet.encloses(queryRange) should be(shouldEnclose)
            }

            // test complement
            val complement = rangeSet.complement
            QUERY_RANGES foreach { queryRange =>
              val shouldEnclose = complement.asRanges.find(_.encloses(queryRange)).isDefined
              complement.encloses(queryRange) should be(shouldEnclose)
            }
          }
        }
      }
      "given the RangeSet contains a two range" - {
        "it should only enclose both ranges" in {
          for (range1 <- QUERY_RANGES) {
            for (range2 <- QUERY_RANGES) {
              val rangeSet = newBuilder.result
              rangeSet.add(range1)
              rangeSet.add(range2)

              // test range Set
              QUERY_RANGES foreach { queryRange =>
                val shouldEnclose = rangeSet.asRanges.find(_.encloses(queryRange)).isDefined
                rangeSet.encloses(queryRange) should be(shouldEnclose)
              }

              // test complement
              val complement = rangeSet.complement
              QUERY_RANGES foreach { queryRange =>
                val shouldEnclose = complement.asRanges.find(_.encloses(queryRange)).isDefined
                complement.encloses(queryRange) should be(shouldEnclose)
              }
            }
          }
        }
      }
    }
    "should merge overlapping ranges" - {
      "if [1,4] and (2,6) are added" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(1, 4))
        rangeSet.add(Range.open(2, 6))
        "the range set should contain [1,6)" in {
          rangeSet.asRanges should be(Set(Range.closedOpen(1, 6)))
        }
        "and the complement should contain {(-inf,1), [6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(1), Range.atLeast(6)))
        }
      }
      "if [1,4] and [1,6] are added" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(1, 4))
        rangeSet.add(Range.closed(1, 6))
        "the range set should contain [1,6]" in {
          rangeSet.asRanges should be(Set(Range.closed(1, 6)))
        }
        "and the complement should contain {(-inf,1), [6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(1), Range.greaterThan(6)))
        }
      }
      "if [3,6] and [1,6] are added" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(3, 6))
        rangeSet.add(Range.closed(1, 6))
        "the range set should contain [1,6]" in {
          rangeSet.asRanges should be(Set(Range.closed(1, 6)))
        }
        "and the complement should contain {(-inf,1), [6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(1), Range.greaterThan(6)))
        }
      }
      "if [3,4] and [1,6] are added" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(3, 4))
        rangeSet.add(Range.closed(1, 6))
        "the range set should contain [1,6]" in {
          rangeSet.asRanges should be(Set(Range.closed(1, 6)))
        }
        "and the complement should contain {(-inf,1), [6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(1), Range.greaterThan(6)))
        }
      }
      "if [1,3),[4,6) and [3,4) are added" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closedOpen(1, 3))
        rangeSet.add(Range.closedOpen(4, 6))
        rangeSet.add(Range.closedOpen(3, 4))
        "the range set should contain [1,6)" in {
          rangeSet.asRanges should be(Set(Range.closedOpen(1, 6)))
        }
        "and the complement should contain {(-inf,1), [6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(1), Range.atLeast(6)))
        }
      }
      "if [1,3),[4,6) and [2,5) are added" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closedOpen(1, 3))
        rangeSet.add(Range.closedOpen(4, 6))
        rangeSet.add(Range.closedOpen(2, 5))
        "the range set should contain [1,6)" in {
          rangeSet.asRanges should be(Set(Range.closedOpen(1, 6)))
        }
        "and the complement should contain {(-inf,1), [6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(1), Range.atLeast(6)))
        }
      }
    }
    "should merge connected ranges" - {
      "if [1,4] and (4,6) are added" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(1, 4))
        rangeSet.add(Range.open(4, 6))
        "the range set should contain [1,6)" in {
          rangeSet.asRanges should be(Set(Range.closedOpen(1, 6)))
        }
        "and the complement should contain {(-inf,1), [6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(1), Range.atLeast(6)))
        }
      }
    }
    "should ignore a smaller range with no sharing bound" - {
      "if [1,6] and (2,4) are added" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(1, 6))
        rangeSet.add(Range.open(2, 4))
        "the range set should contain [1,6]" in {
          rangeSet.asRanges should be(Set(Range.closed(1, 6)))
        }
        "and the complement should contain {(-inf,1), (6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(1), Range.greaterThan(6)))
        }
      }
    }
    "should ignore a smaller range with a lower sharing bound" - {
      "if [1,6] and [1,4] are added" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(1, 6))
        rangeSet.add(Range.closed(1, 4))
        "the range set should contain [1,6]" in {
          rangeSet.asRanges should be(Set(Range.closed(1, 6)))
        }
        "and the complement should contain {(-inf,1), (6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(1), Range.greaterThan(6)))
        }
      }
    }
    "should ignore a smaller range with a lower sharing bound" - {
      "if [1,6] and [3,6] are added" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(1, 6))
        rangeSet.add(Range.closed(3, 6))
        "the range set should contain [1,6]" in {
          rangeSet.asRanges should be(Set(Range.closed(1, 6)))
        }
        "and the complement should contain {(-inf,1), (6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(1), Range.greaterThan(6)))
        }
      }
    }
    "should ignore a duplicate" - {
      "if [1,6] and [1,6] are added" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(1, 6))
        rangeSet.add(Range.closed(1, 6))
        "the range set should contain [1,6]" in {
          rangeSet.asRanges should be(Set(Range.closed(1, 6)))
        }
        "and the complement should contain {(-inf,1), (6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(1), Range.greaterThan(6)))
        }
      }
    }
    "removing an empty range should have no effect" - {
      "if the set contains [1,6] and [3,3) is removed" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(1, 6))
        rangeSet.remove(Range.closedOpen(3, 3))
        "the range set should contain [1,6]" in {
          rangeSet.asRanges should be(Set(Range.closed(1, 6)))
        }
        "and the complement should contain {(-inf,1), (6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(1), Range.greaterThan(6)))
        }
      }
    }
    "should implement #remove" - {
      "if the set contains [3,5] and [3,5) is removed" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(3, 5))
        rangeSet.remove(Range.closedOpen(3, 5))
        "the range set should contain [5]" in {
          rangeSet.asRanges should be(Set(Range.singleton(5)))
        }
        "and the complement should contain {(-inf,5), (5,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(5), Range.greaterThan(5)))
        }
      }
      "if the set contains [3,5] and (3,5] is removed" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(3, 5))
        rangeSet.remove(Range.openClosed(3, 5))
        "the range set should contain [3]" in {
          rangeSet.asRanges should be(Set(Range.singleton(3)))
        }
        "and the complement should contain {(-inf,3), (3,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(3), Range.greaterThan(3)))
        }
      }
      "if the set contains (-inf,6] and [3,4) is removed" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.atMost(6))
        rangeSet.remove(Range.closedOpen(3, 4))
        "the range set should contain {(-inf, 3), [3,6]}" in {
          rangeSet.asRanges should be(Set(Range.lessThan(3), Range.closed(4, 6)))
        }
        "and the complement should contain {[3,4), (6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.closedOpen(3, 4), Range.greaterThan(6)))
        }
      }
      "if the set contains [3,6] and [1,3) is removed" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(3, 6))
        rangeSet.remove(Range.closedOpen(1, 3))
        "the range set should contain {[3,6]}" in {
          rangeSet.asRanges should be(Set(Range.closed(3, 6)))
        }
        "and the complement should contain {(-inf,3), (6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(3), Range.greaterThan(6)))
        }
      }
      "if the set contains [3,6] and [1,3] is removed" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(3, 6))
        rangeSet.remove(Range.closed(1, 3))
        "the range set should contain {(3,6]}" in {
          rangeSet.asRanges should be(Set(Range.openClosed(3, 6)))
        }
        "and the complement should contain {(-inf,3], (6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.atMost(3), Range.greaterThan(6)))
        }
      }
      "if the set contains [3,6] and [6,9] is removed" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(3, 6))
        rangeSet.remove(Range.closed(6, 9))
        "the range set should contain {[3,6)}" in {
          rangeSet.asRanges should be(Set(Range.closedOpen(3, 6)))
        }
        "and the complement should contain {(-inf,3), [6,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.lessThan(3), Range.atLeast(6)))
        }
      }
      "if the set contains [3,6] and [3,6] is removed" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(3, 6))
        rangeSet.remove(Range.closed(3, 6))
        "the range set should be empty" in {
          rangeSet.isEmpty should be(true)
          rangeSet.asRanges should be(Set())
        }
        "and the complement should be {(-inf,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.all[Int, Int.type]))
        }
      }
      "if the set contains [3,6] and [2,6] is removed" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(3, 6))
        rangeSet.remove(Range.closed(2, 6))
        "the range set should be empty" in {
          rangeSet.isEmpty should be(true)
          rangeSet.asRanges should be(Set())
        }
        "and the complement should be {(-inf,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.all[Int, Int.type]))
        }
      }
      "if the set contains [3,6] and [3,7] is removed" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(3, 6))
        rangeSet.remove(Range.closed(3, 7))
        "the range set should be empty" in {
          rangeSet.isEmpty should be(true)
          rangeSet.asRanges should be(Set())
        }
        "and the complement should be {(-inf,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.all[Int, Int.type]))
        }
      }
      "if the set contains [3,6] and [2,7] is removed" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.closed(3, 6))
        rangeSet.remove(Range.closed(2, 7))
        "the range set should be empty" in {
          rangeSet.isEmpty should be(true)
          rangeSet.asRanges should be(Set())
        }
        "and the complement should be {(-inf,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.all[Int, Int.type]))
        }
      }
    }

    "should not contain empty ranges" - {
      "given the RangeSet is empty" - {
        val rangeSet = newBuilder.result
        "all ranges must be non-empty" in {
          rangeSet.asRanges.foreach { range => range.isEmpty should be(false) }
        }
      }
      "given an empty range is added" - {
        val rangeSet = newBuilder.result
        rangeSet.add(Range.openClosed(3, 3))
        "all ranges must be non-empty" in {
          rangeSet.asRanges.foreach { range => range.isEmpty should be(false) }
        }
        "the range set must be empty" in {
          rangeSet.isEmpty should be(true)
        }
        "the complement must contain (-inf,inf)" in {
          rangeSet.complement.asRanges should be(Set(Range.all[Int, Int.type]))
        }
      }
    }

  }

  private def testCoalesced[T, O <: Ordering[T]](rangeSet: RangeSet[T, O]): Unit = {
    val asRanges = rangeSet.asRanges
    (asRanges.drop(1) zip asRanges.dropRight(1)).foreach {
      case (a: Range[T, O], b: Range[T, O]) => { a.isConnected(b) should be(false) }
    }
  }

  def rangeSet(newBuilder: => Builder[Range[Int, Int.type], RangeSet[Int, Int.type]]) = {
    val build: Iterable[Range[Int, Int.type]] => RangeSet[Int, Int.type] = {
      ranges: Iterable[Range[Int, Int.type]] => (newBuilder ++= ranges).result
    }

    /*
     * #contains
     */
    "should implement #contains" - {
      "given the RangeSet is empty" - {
        val rangeSet = build(Set())
        "when #contains(Int) is called it should be false" in {
          forAll { i: Int =>
            rangeSet contains (i) should be(false)
          }
        }
      }
      "given the RangeSet contains the Range [1,5)" - {
        val rangeSet = build(Set(Range.closedOpen(1, 5)))
        "then #contains(3) should be true" in {
          rangeSet.contains(3) should be(true)
        }
        "then #contains(5) should be false" in {
          rangeSet.contains(5) should be(false)
        }
        "then #contains(0) should be false" in {
          rangeSet.contains(0) should be(false)
        }
      }
      "given the RangeSet contains the Range (2,inf)" - {
        val rangeSet = build(Set(Range.greaterThan(2)))
        "then #contains(3) should be true" in {
          rangeSet.contains(3) should be(true)
        }
        "then #contains(5) should be true" in {
          rangeSet.contains(5) should be(true)
        }
        "then #contains(0) should be false" in {
          rangeSet.contains(0) should be(false)
        }
        "then #contains(2) should be false" in {
          rangeSet.contains(2) should be(false)
        }
      }
      "given the RangeSet contains the Range (-inf,3]" - {
        val rangeSet = build(Set(Range.atMost(3)))
        "then #contains(3) should be true" in {
          rangeSet.contains(3) should be(true)
        }
        "then #contains(0) should be true" in {
          rangeSet.contains(0) should be(true)
        }
        "then #contains(4) should be false" in {
          rangeSet.contains(4) should be(false)
        }
        "then #contains(5) should be false" in {
          rangeSet.contains(5) should be(false)
        }
      }
    }

    /*
     * #isEmpty
     */
    "should implement #isEmpty" - {
      "given the RangeSet is empty" - {
        val rangeSet = build(Set())
        "when #isEmpty is called should return true" in {
          build(Set()).isEmpty should be(true)
        }
        "when #isEmpty is called on the Set returned by #asRanges should be true" in {
          build(Set()).asRanges.isEmpty should be(true)
        }
      }
    }

    /*
     * #encloses
     */
    "should implement #encloses" - {
      "given the RangeSet is empty" - {
        val rangeSet = build(Set())
        "when #encloses is called with a singleton Range it should be false" in {
          forAll { i: Int =>
            rangeSet encloses Range.singleton(i) should be(false)
          }
        }
      }
      "given the RangeSet contains the Range [1,5)" - {
        val rangeSet = build(Set(Range.closedOpen(1, 5)))
        "then #encloses [3,4] should be true" in {
          rangeSet encloses Range.closed(3, 4) should be(true)
        }
        "then #encloses [1,4) should be true" in {
          rangeSet encloses Range.closedOpen(1, 4) should be(true)
        }
        "then #encloses [1,5) should be true" in {
          rangeSet encloses Range.closedOpen(1, 5) should be(true)
        }
        "then #encloses (2,inf) should be false" in {
          rangeSet encloses Range.greaterThan(2) should be(false)
        }
      }
      "given the RangeSet contains the Range (2,inf)" - {
        val rangeSet = build(Set(Range.greaterThan(2)))
        "then #encloses [3,4] should be true" in {
          rangeSet encloses Range.closed(3, 4) should be(true)
        }
        "then #encloses (3,inf] should be true" in {
          rangeSet encloses Range.greaterThan(3) should be(true)
        }
        "then #encloses [1,5) should be false" in {
          rangeSet encloses Range.closedOpen(1, 5) should be(false)
        }
      }
      "given the RangeSet contains the Range (-inf,3]" - {
        val rangeSet = build(Set(Range.atMost(3)))
        "then #encloses [2,3] should be true" in {
          rangeSet encloses Range.closed(2, 3) should be(true)
        }
        "then #encloses (-inf,1) should be true" in {
          rangeSet encloses Range.lessThan(1) should be(true)
        }
        "then #encloses [1,5) should be false" in {
          rangeSet encloses Range.closedOpen(1, 5) should be(false)
        }
      }
      "given the RangeSet contains the Ranges {[5,8],[1,3)}" - {
        val rangeSet = build(Set(Range.closed(5, 8), Range.closedOpen(1, 3)))
        "then #encloses [1,2] should be true" in {
          rangeSet encloses Range.closed(1, 2) should be(true)
        }
        "then #encloses (5,8) should be true" in {
          rangeSet encloses Range.open(5, 8) should be(true)
        }
        "then #encloses [1,8] should be false" in {
          rangeSet encloses Range.closed(1, 8) should be(false)
        }
        "then #encloses (5,inf) should be false" in {
          rangeSet encloses Range.greaterThan(5) should be(false)
        }
      }
      "given the RangeSet contains the Ranges {(6,inf),[1,3)}" - {
        val rangeSet = build(Set(Range.greaterThan(6), Range.closedOpen(1, 3)))
        "then #encloses [1,2] should be true" in {
          rangeSet encloses Range.closed(1, 2) should be(true)
        }
        "then #encloses (6,8) should be true" in {
          rangeSet encloses Range.open(6, 8) should be(true)
        }
        "then #encloses [1,8] should be false" in {
          rangeSet encloses Range.closed(1, 8) should be(false)
        }
        "then #encloses (5,inf) should be false" in {
          rangeSet encloses Range.greaterThan(5) should be(false)
        }
      }
      "given the RangeSet contains the Ranges {(-inf,0],[2,5)}" - {
        val rangeSet = build(Set(Range.atMost(0), Range.closedOpen(2, 5)))
        "then #encloses [2,4] should be true" in {
          rangeSet encloses Range.closed(2, 4) should be(true)
        }
        "then #encloses (-5,-2) should be true" in {
          rangeSet encloses Range.open(-5, -2) should be(true)
        }
        "then #encloses [1,8] should be false" in {
          rangeSet encloses Range.closed(1, 8) should be(false)
        }
        "then #encloses (5,inf) should be false" in {
          rangeSet encloses Range.greaterThan(5) should be(false)
        }
      }
    }

    /*
     * #enclosesAll
     */
    "should implement #enclosesAll" - {
      "given the RangeSet is empty" - {
        val rangeSet = build(Set())
        "when #enclosesAll is called with the empty RangeSet itself it should be true" in {
          rangeSet enclosesAll rangeSet should be(true)
        }
      }
      "given the RangeSet contains the Ranges {[5,8],[1,3)}" - {
        val rangeSet = build(Set(Range.closed(5, 8), Range.closedOpen(1, 3)))
        "#enclosesAll {[5,8],[1,3)} should be true" in {
          rangeSet enclosesAll rangeSet should be(true)
          rangeSet enclosesAll build(Set(Range.closed(5, 8), Range.closedOpen(1, 3))) should be(true)
        }
        "#enclosesAll {[1,3)} should be true" in {
          rangeSet enclosesAll build(Set(Range.closedOpen(1, 3))) should be(true)
        }
        "#enclosesAll {[5,8]} should be true" in {
          rangeSet enclosesAll build(Set(Range.closed(5, 8))) should be(true)
        }
        "#enclosesAll {[4,8]} should be false" in {
          rangeSet enclosesAll build(Set(Range.closed(4, 8))) should be(false)
        }
        "#enclosesAll {[5,5],[1,2)} should be true" in {
          rangeSet enclosesAll build(Set(Range.closed(5, 5), Range.closedOpen(1, 2))) should be(true)
        }
        "#enclosesAll {[5,8],[1,4)} should be false" in {
          rangeSet enclosesAll build(Set(Range.closed(5, 8), Range.closedOpen(1, 4))) should be(false)
        }
      }
    }

    /*
     * #complement 
     */
    "should implement #complement" - {
      "given the RangeSet is empty" - {
        val rangeSet = build(Set())
        "when #complement is called it should return a RangeSet with (-inf, inf)" in {
          val expected = build(Set(Range.all[Int, Int.type]))
          val complement = rangeSet.complement
          complement should be(expected)

          forAll { i: Int =>
            complement.contains(i) should be(true)
          }
        }
      }
      "given the RangeSet contains the Range [1,5)" - {
        val rangeSet = build(Set(Range.closedOpen(1, 5)))
        "when #complement is called it should return a RangeSet with {(-inf, 1), [5,inf)}" in {
          val expected = build(Set(Range.lessThan(1), Range.atLeast(5)))
          val complement = rangeSet.complement
          complement should be(expected)

          forAll { i: Int =>
            rangeSet.contains(i) should be(!complement.contains(i))
          }
        }
      }
      "given the RangeSet contains the Range (2,inf)" - {
        val rangeSet = build(Set(Range.greaterThan(2)))
        "when #complement is called it should return a RangeSet with {(-inf,2]}" in {
          val expected = build(Set(Range.atMost(2)))
          val complement = rangeSet.complement
          complement should be(expected)

          forAll { i: Int =>
            rangeSet.contains(i) should be(!complement.contains(i))
          }
        }
      }
      "given the RangeSet contains the Range (-inf,3]" - {
        val rangeSet = build(Set(Range.atMost(3)))
        "when #complement is called it should return a RangeSet with {(3,inf)}" in {
          val expected = build(Set(Range.greaterThan(3)))
          val complement = rangeSet.complement
          complement should be(expected)

          forAll { i: Int =>
            rangeSet.contains(i) should be(!complement.contains(i))
          }
        }
      }
      "given the RangeSet contains the Ranges {[5,8],[1,3)}" - {
        val rangeSet = build(Set(Range.closed(5, 8), Range.closedOpen(1, 3)))
        "when #complement is called it should return a RangeSet with {(-inf,1),[3,5),(8,inf)}" in {
          val expected = build(Set(Range.lessThan(1), Range.closedOpen(3, 5), Range.greaterThan(8)))
          val complement = rangeSet.complement
          complement should be(expected)

          forAll { i: Int =>
            rangeSet.contains(i) should be(!complement.contains(i))
          }
        }
      }
      "given the RangeSet contains the Ranges {(6,inf),[1,3)}" - {
        val rangeSet = build(Set(Range.greaterThan(6), Range.closedOpen(1, 3)))
        "when #complement is called it should return a RangeSet with {(-inf,1),[3,6]}" in {
          val expected = build(Set(Range.lessThan(1), Range.closed(3, 6)))
          val complement = rangeSet.complement
          complement should be(expected)

          forAll { i: Int =>
            rangeSet.contains(i) should be(!complement.contains(i))
          }
        }
      }
      "given the RangeSet contains the Ranges {(-inf,0],[2,5)}" - {
        val rangeSet = build(Set(Range.atMost(0), Range.closedOpen(2, 5)))
        "when #complement is called it should return a RangeSet with {(0,2),[5,inf)}" in {
          val expected = build(Set(Range.open(0, 2), Range.atLeast(5)))
          val complement = rangeSet.complement
          complement should be(expected)

          forAll { i: Int =>
            rangeSet.contains(i) should be(!complement.contains(i))
          }
        }
      }
    }

    /*
     * #asRanges
     */
    "should implement #asRanges" - {
      "given the RangeSet is empty" - {
        val rangeSet = build(Set())
        "#asRanges should return an empty Set" in {
          rangeSet.asRanges should be(Set())
        }
      }
      "given the RangeSet contains the Range [1,5)" - {
        val rangeSet = build(Set(Range.closedOpen(1, 5)))
        "#asRanges should return a Set with [1,5)" in {
          rangeSet.asRanges should be(Set(Range.closedOpen(1, 5)))
        }
      }
      "given the RangeSet contains the Range (2,inf)" - {
        val rangeSet = build(Set(Range.greaterThan(2)))
        "#asRanges should return a Set with (2,inf)" in {
          rangeSet.asRanges should be(Set(Range.greaterThan(2)))
        }
      }
      "given the RangeSet contains the Range (-inf,3]" - {
        val rangeSet = build(Set(Range.atMost(3)))
        "#asRanges should return a Set with (-inf,3]" in {
          rangeSet.asRanges should be(Set(Range.atMost(3)))
        }
      }
      "given the RangeSet contains the Ranges {[5,8],[1,3)}" - {
        val rangeSet = build(Set(Range.closed(5, 8), Range.closedOpen(1, 3)))
        "#asRanges should return a Set with {[1,3),[5,8]} in order" in {
          rangeSet.asRanges.toList should be(List(Range.closedOpen(1, 3), Range.closed(5, 8)))
        }
      }
      "given the RangeSet contains the Ranges {(6,inf),[1,3)}" - {
        val rangeSet = build(Set(Range.greaterThan(6), Range.closedOpen(1, 3)))
        "#asRanges should return a Set with {(6,inf),[1,3)}" in {
          rangeSet.asRanges should be(Set(Range.greaterThan(6), Range.closedOpen(1, 3)))
        }
      }
      "given the RangeSet contains the Ranges {(-inf,0],[2,5)}" - {
        val rangeSet = build(Set(Range.atMost(0), Range.closedOpen(2, 5)))
        "#asRanges should return a Set with {(-inf,0],[2,5)}" in {
          rangeSet.asRanges should be(Set(Range.atMost(0), Range.closedOpen(2, 5)))
        }
      }
    }

    /*
     * #rangeContaining
     */
    "should implement #rangeContaining" - {
      "given the RangeSet is empty" - {
        val rangeSet = build(Set())
        "#rangeContaining should return no range" in {
          forAll { i: Int =>
            rangeSet.rangeContaining(i) should be(None)
          }
        }
      }
      "given the RangeSet contains the Range [1,5)" - {
        val rangeSet = build(Set(Range.closedOpen(1, 5)))
        "#rangeContaining(1) should return a [1,5)" in {
          rangeSet.rangeContaining(1) should be(Some(Range.closedOpen(1, 5)))
        }
        "#rangeContaining(2) should return a [1,5)" in {
          rangeSet.rangeContaining(2) should be(Some(Range.closedOpen(1, 5)))
        }
        "#rangeContaining(5) should return no range" in {
          rangeSet.rangeContaining(5) should be(None)
        }
      }
      "given the RangeSet contains the Range (2,inf)" - {
        val rangeSet = build(Set(Range.greaterThan(2)))
        "#rangeContaining(1) should return no range" in {
          rangeSet.rangeContaining(1) should be(None)
        }
        "#rangeContaining(2) should return no range" in {
          rangeSet.rangeContaining(2) should be(None)
        }
        "#rangeContaining(100) should return (2,inf)" in {
          rangeSet.rangeContaining(100) should be(Some(Range.greaterThan(2)))
        }
      }
      "given the RangeSet contains the Range (-inf,3]" - {
        val rangeSet = build(Set(Range.atMost(3)))
        "#rangeContaining(1) should return (-inf,3]" in {
          rangeSet.rangeContaining(1) should be(Some(Range.atMost(3)))
        }
        "#rangeContaining(0) should return (-inf,3]" in {
          rangeSet.rangeContaining(0) should be(Some(Range.atMost(3)))
        }
        "#rangeContaining(4) should return no range" in {
          rangeSet.rangeContaining(4) should be(None)
        }
      }
      "given the RangeSet contains the Ranges {[5,8],[1,3)}" - {
        val rangeSet = build(Set(Range.closed(5, 8), Range.closedOpen(1, 3)))
        "#rangeContaining(1) should return [1,3)" in {
          rangeSet.rangeContaining(1) should be(Some(Range.closedOpen(1, 3)))
        }
        "#rangeContaining(6) should return [5,8]" in {
          rangeSet.rangeContaining(6) should be(Some(Range.closed(5, 8)))
        }
        "#rangeContaining(3) should return no range" in {
          rangeSet.rangeContaining(3) should be(None)
        }
      }
      "given the RangeSet contains the Ranges {(6,inf),[1,3)}" - {
        val rangeSet = build(Set(Range.greaterThan(6), Range.closedOpen(1, 3)))
        "#rangeContaining(1) should return [1,3)" in {
          rangeSet.rangeContaining(1) should be(Some(Range.closedOpen(1, 3)))
        }
        "#rangeContaining(7) should return (6,inf)" in {
          rangeSet.rangeContaining(7) should be(Some(Range.greaterThan(6)))
        }
        "#rangeContaining(6) should return no range" in {
          rangeSet.rangeContaining(6) should be(None)
        }
      }
      "given the RangeSet contains the Ranges {(-inf,0],[2,5)}" - {
        val rangeSet = build(Set(Range.atMost(0), Range.closedOpen(2, 5)))
        "#rangeContaining(0) should return (-inf,0)" in {
          rangeSet.rangeContaining(0) should be(Some(Range.atMost(0)))
        }
        "#rangeContaining(1) should return no range" in {
          rangeSet.rangeContaining(1) should be(None)
        }
        "#rangeContaining(2) should return [2,5)" in {
          rangeSet.rangeContaining(2) should be(Some(Range.closedOpen(2, 5)))
        }
      }
    }

    /*
     * #span
     */
    "should implement #span" - {
      "given the RangeSet is empty" - {
        val rangeSet = build(Set())
        "#span should return no range" in {
          rangeSet.span should be(None)
        }
      }
      "given the RangeSet contains the Range [1,5)" - {
        val rangeSet = build(Set(Range.closedOpen(1, 5)))
        "#span should return [1,5)" in {
          rangeSet.span should be(Some(Range.closedOpen(1, 5)))
        }
      }
      "given the RangeSet contains the Range (2,inf)" - {
        val rangeSet = build(Set(Range.greaterThan(2)))
        "#span should return (2,inf)" in {
          rangeSet.span should be(Some(Range.greaterThan(2)))
        }
      }
      "given the RangeSet contains the Range (-inf,3]" - {
        val rangeSet = build(Set(Range.atMost(3)))
        "#span should return (-inf,3]" in {
          rangeSet.span should be(Some(Range.atMost(3)))
        }
      }
      "given the RangeSet contains the Ranges {[5,8],[1,3)}" - {
        val rangeSet = build(Set(Range.closed(5, 8), Range.closedOpen(1, 3)))
        "#span should return [1,8]" in {
          rangeSet.span should be(Some(Range.closed(1, 8)))
        }
      }
      "given the RangeSet contains the Ranges {(6,inf),[1,3)}" - {
        val rangeSet = build(Set(Range.greaterThan(6), Range.closedOpen(1, 3)))
        "#span should return [1,inf)" in {
          rangeSet.span should be(Some(Range.atLeast(1)))
        }
      }
      "given the RangeSet contains the Ranges {(-inf,0],[2,5)}" - {
        val rangeSet = build(Set(Range.atMost(0), Range.closedOpen(2, 5)))
        "#span should return [-inf,5)" in {
          rangeSet.span should be(Some(Range.lessThan(5)))
        }
      }
    }
    /*
     * #subrange
     */
    "should implement #subrange" - {
      "given the RangeSet is empty" - {
        val rangeSet = build(Set())
        "#subRangeSet (1,3) should return an emtpy range set" in {
          rangeSet.subRangeSet(Range.open(1, 3)) should be(rangeSet)
        }
      }
      "given the RangeSet contains the Range [1,5)" - {
        val rangeSet = build(Set(Range.closedOpen(1, 5)))
        "#subRangeSet (0,3) should return {[1,3)}" in {
          rangeSet.subRangeSet(Range.open(0, 3)) should be(build(Set(Range.closedOpen(1, 3))))
        }
        "#subRangeSet (4,5] should return {(4,5)}" in {
          rangeSet.subRangeSet(Range.openClosed(4, 5)) should be(build(Set(Range.open(4, 5))))
        }
        "#subRangeSet (5,6] should return {}" in {
          rangeSet.subRangeSet(Range.openClosed(5, 6)) should be(build(Set()))
        }
        "#subRangeSet [1,inf) should return {[1,5)}" in {
          rangeSet.subRangeSet(Range.atLeast(1)) should be(build(Set(Range.closedOpen(1, 5))))
        }
      }
      "given the RangeSet contains the Range (2,inf)" - {
        val rangeSet = build(Set(Range.greaterThan(2)))
        "#subRangeSet (0,3) should return {(2,3)}" in {
          rangeSet.subRangeSet(Range.open(0, 3)) should be(build(Set(Range.open(2, 3))))
        }
        "#subRangeSet [1,inf) should return {(2,inf)}" in {
          rangeSet.subRangeSet(Range.atLeast(1)) should be(build(Set(Range.greaterThan(2))))
        }
        "#subRangeSet (-inf,5] should return {(2,5]}" in {
          rangeSet.subRangeSet(Range.atMost(5)) should be(build(Set(Range.openClosed(2, 5))))
        }
        "#subRangeSet [1,1] should return {}" in {
          rangeSet.subRangeSet(Range.closed(1, 1)) should be(build(Set()))
        }
      }
      "given the RangeSet contains the Range (-inf,3]" - {
        val rangeSet = build(Set(Range.atMost(3)))
        "#subRangeSet (0,3) should return {(0,3)}" in {
          rangeSet.subRangeSet(Range.open(0, 3)) should be(build(Set(Range.open(0, 3))))
        }
        "#subRangeSet [1,inf) should return {[1,3]}" in {
          rangeSet.subRangeSet(Range.atLeast(1)) should be(build(Set(Range.closed(1, 3))))
        }
        "#subRangeSet (-inf,1] should return {(-inf,1]}" in {
          rangeSet.subRangeSet(Range.atMost(1)) should be(build(Set(Range.atMost(1))))
        }
        "#subRangeSet (3,5] should return {}" in {
          rangeSet.subRangeSet(Range.openClosed(3, 3)) should be(build(Set()))
        }
      }
      "given the RangeSet contains the Ranges {[5,8],[1,3)}" - {
        val rangeSet = build(Set(Range.closed(5, 8), Range.closedOpen(1, 3)))
        "#subRangeSet (2,6) should return {(2,3),[5,6)}" in {
          rangeSet.subRangeSet(Range.open(2, 6)) should be(build(Set(Range.open(2, 3), Range.closedOpen(5, 6))))
        }
        "#subRangeSet [2,inf) should return {[2,3),[5,8]}" in {
          rangeSet.subRangeSet(Range.atLeast(2)) should be(build(Set(Range.closedOpen(2, 3), Range.closed(5, 8))))
        }
        "#subRangeSet (-inf,5] should return {[1,3),[5]}" in {
          rangeSet.subRangeSet(Range.atMost(5)) should be(build(Set(Range.closedOpen(1, 3), Range.singleton(5))))
        }
        "#subRangeSet (3,5) should return {}" in {
          rangeSet.subRangeSet(Range.open(3, 5)) should be(build(Set()))
        }
      }
      "given the RangeSet contains the Ranges {(6,inf),[1,3)}" - {
        val rangeSet = build(Set(Range.greaterThan(6), Range.closedOpen(1, 3)))
        "#subRangeSet (2,7) should return {(2,3),(6,7)}" in {
          rangeSet.subRangeSet(Range.open(2, 7)) should be(build(Set(Range.open(2, 3), Range.open(6, 7))))
        }
        "#subRangeSet [2,inf) should return {[2,3),(6,inf)}" in {
          rangeSet.subRangeSet(Range.atLeast(2)) should be(build(Set(Range.closedOpen(2, 3), Range.greaterThan(6))))
        }
        "#subRangeSet (-inf,6) should return {[1,3)}" in {
          rangeSet.subRangeSet(Range.lessThan(6)) should be(build(Set(Range.closedOpen(1, 3))))
        }
        "#subRangeSet (3,6] should return {}" in {
          rangeSet.subRangeSet(Range.openClosed(3, 6)) should be(build(Set()))
        }
      }
    }

    "should be equal to another RangeSet if the sets returned by #asRanges are equal" - {
      "given the RangeSet is empty" - {
        val rangeSet = build(Set())
        "if the other RangeSet is empty it should be equal" in {
          val mocked = mock[RangeSet[Int, Int.type]]
          when(mocked.asRanges).thenReturn(Set[Range[Int, Int.type]]())
          rangeSet should be(mocked)
        }
        "if the other RangeSet contains the ranges {[1,8],[1,3]} it should not be equal" in {
          val mocked = mock[RangeSet[Int, Int.type]]
          when(mocked.asRanges).thenReturn(Set(Range.closed(1, 8), Range.closed(1, 3)))
          rangeSet should not be (mocked)
        }
      }
      "given the RangeSet contains the ranges {[5,8],[1,3)}" - {
        val rangeSet = build(Set(Range.closed(5, 8), Range.closedOpen(1, 3)))
        "if the other RangeSet contains the ranges {[5,8],[1,3)} it should be equal" in {
          val mocked = mock[RangeSet[Int, Int.type]]
          when(mocked.asRanges).thenReturn(Set(Range.closed(5, 8), Range.closedOpen(1, 3)))
          rangeSet should be(mocked)
        }
        "if the other RangeSet contains the ranges {[1,8],[1,3]} it should not be equal" in {
          val mocked = mock[RangeSet[Int, Int.type]]
          when(mocked.asRanges).thenReturn(Set(Range.closed(1, 8), Range.closed(1, 3)))
          rangeSet should not be (mocked)
        }
      }
    }

    "should calculate the hashCode as #asRanges.hashCode" - {
      "given the RangeSet is empty" - {
        val rangeSet = build(Set())
        "it's hashCode should be Set().hashCode" in {
          rangeSet.hashCode should be(Set().hashCode)
        }
      }
      "given the RangeSet contains the ranges {[5,8],[1,3)}" - {
        val rangeSet = build(Set(Range.closed(5, 8), Range.closedOpen(1, 3)))
        "it's hashCode should be {[5,8],[1,3)}.hashCode" in {
          rangeSet.hashCode should be(Set(Range.closed(5, 8), Range.closedOpen(1, 3)).hashCode)
        }
      }
    }
  }

  def rangeSetWithBuilder(newBuilder: => Builder[Range[Int, Int.type], RangeSet[Int, Int.type]]) = {
    "should implement newBuilder" - {
      "given the builder is empty" - {
        val builder = newBuilder
        "#result should return an empty RangeSet" in {
          builder.result.isEmpty should be(true)
        }
      }
      "when values are aded to the builder" - {
        val builder = newBuilder
        builder += Range.open(4, 7)
        builder += Range.open(8, 9)
        "#result should return a RangeSet with these values" in {
          builder.result.asRanges should be(Set(Range.open(4, 7), Range.open(8, 9)))
        }
      }
    }
  }
}