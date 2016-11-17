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
import scala.collection.convert.decorateAll.mapAsScalaMapConverter
import scala.collection.mutable.Builder
import scala.math.Ordering.Int

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect.Range.asGuavaRangeConverter
import org.feijoas.mango.test.collect.Ranges.arbNonOverlappingRangeParis
import org.feijoas.mango.test.collect.Ranges.arbRange
import org.feijoas.mango.test.collect.Ranges.maxBound
import org.feijoas.mango.test.collect.Ranges.minBound
import org.mockito.Mockito.when
import org.scalatest.FreeSpec
import org.scalatest.Matchers.be
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.Matchers.not
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks

import com.google.common.{ collect => gcc }

/**
 * Behavior which all [[RangeMap]] have in common
 *
 *  @author Markus Schneider
 *  @since 0.9
 */
object RangeMapBehaviors {
  type TIntRange = Range[Int, Int.type]
  type TIntRangeMap = RangeMap[Int, String, Int.type]
  type TIntMutableRangeMap = mutable.RangeMap[Int, String, Int.type]
  type TIntRangeMapLike = RangeMapLike[Int, String, Int.type, TIntRangeMap]
  type TIntMutableRangeMapLike = mutable.RangeMapLike[Int, String, Int.type, TIntMutableRangeMap]
  type TIntRangeMapWrapperLike = RangeMapWrapperLike[Int, String, Int.type, _]
  type TBuilder = Builder[(TIntRange, String), TIntRangeMap]
}

private[mango] trait RangeMapBehaviors extends FreeSpec with PropertyChecks with MockitoSugar {
  this: FreeSpec =>

  import RangeMapBehaviors._

  def aMutableRangeMapLike(newBuilder: => Builder[(TIntRange, String), TIntMutableRangeMapLike]) = {
    // behave like a "normal" RangeMap ...
    aRangeMapLike(newBuilder)

    // ... and
    "it should implement #put" - {
      "for any single range" in {
        forAll { range: Range[Int, Int.type] =>
          val rangeMap = newBuilder.result
          rangeMap.put(range, "1")
          rangeMap.asMapOfRanges should be(Map(range -> "1"))
        }

      }
      "for any two ranges" in {
        forAll { t: (Range[Int, Int.type], Range[Int, Int.type]) =>
          val (range1, range2) = t
          val rangeMap = newBuilder.result
          rangeMap.put(range1, "1")
          rangeMap.put(range2, "2")
          rangeMap.asMapOfRanges should be(Map(range1 -> "1", range2 -> "2"))
        }
      }
    }
    "it should implement #+=" - {
      "for any single range" in {
        forAll { range: Range[Int, Int.type] =>
          val rangeMap = newBuilder.result
          rangeMap += range -> "1"
          rangeMap.asMapOfRanges should be(Map(range -> "1"))
        }
      }
      "for any two ranges" in {
        forAll { t: (Range[Int, Int.type], Range[Int, Int.type]) =>
          val (range1, range2) = t
          val rangeMap = newBuilder.result
          rangeMap += range1 -> "1" += range2 -> "2"
          rangeMap.asMapOfRanges should be(Map(range1 -> "1", range2 -> "2"))
        }
      }
    }
    "it should implement #putAll" - {
      "for any two ranges" in {
        forAll { t: (Range[Int, Int.type], Range[Int, Int.type]) =>
          val (range1, range2) = t
          val model = mock[TIntRangeMap]
          when(model.asMapOfRanges).thenReturn(Map(range1 -> "1", range2 -> "2"))

          val rangeMap = newBuilder.result
          rangeMap.asMapOfRanges.isEmpty
          rangeMap.putAll(model)
          rangeMap.asMapOfRanges should be(Map(range1 -> "1", range2 -> "2"))
        }
      }
    }
    "it should implement #clear" - {
      "for any single range" in {
        forAll { range: Range[Int, Int.type] =>
          val rangeMap = (newBuilder += range -> "1").result
          rangeMap.asMapOfRanges.isEmpty should be(false)
          rangeMap.isEmpty should be(false)
          rangeMap.clear
          rangeMap.asMapOfRanges.isEmpty should be(true)
          rangeMap.isEmpty should be(true)
        }
      }
    }
    "it should implement #remove" - {
      "removing from an empty map should have no effect" in {
        val rangeMap = newBuilder.result
        forAll { range: Range[Int, Int.type] =>
          rangeMap.remove(range)
          rangeMap.asMapOfRanges should be(Map())
        }
      }
      "for any two not overlapping ranges" - {
        "removing the first should retain the other" in {
          forAll { t: (Range[Int, Int.type], Range[Int, Int.type]) =>
            val (range1, range2) = t
            val rangeMap = (newBuilder += range1 -> "1" += range2 -> "2").result
            rangeMap.asMapOfRanges should be(Map(range1 -> "1", range2 -> "2"))
            rangeMap.remove(range1)
            rangeMap.asMapOfRanges should be(Map(range2 -> "2"))
            rangeMap.remove(range2)
            rangeMap.asMapOfRanges should be(Map())
            rangeMap.remove(range2)
            rangeMap.asMapOfRanges should be(Map())
          }
        }
        "removing the second should retain the other" in {
          forAll { t: (Range[Int, Int.type], Range[Int, Int.type]) =>
            val (range1, range2) = t
            val rangeMap = (newBuilder += range1 -> "1" += range2 -> "2").result
            rangeMap.asMapOfRanges should be(Map(range1 -> "1", range2 -> "2"))
            rangeMap.remove(range2)
            rangeMap.asMapOfRanges should be(Map(range1 -> "1"))
            rangeMap.remove(range1)
            rangeMap.asMapOfRanges should be(Map())
            rangeMap.remove(range2)
            rangeMap.asMapOfRanges should be(Map())
          }
        }
      }
      "removing an overlapping range should retain parts of the other" in {
        forAll { (rangeToPut: Range[Int, Int.type], rangeToRemove: Range[Int, Int.type]) =>
          val model = gcc.TreeRangeMap.create[AsOrdered[Int], String]
          whenever(rangeToPut isConnected rangeToRemove) {
            // create the model
            model.put(rangeToPut.asJava, "1")
            model.remove(rangeToRemove.asJava)

            // put & remove
            val rangeMap = (newBuilder += rangeToPut -> "1").result
            rangeMap.remove(rangeToRemove)

            // compare
            verify(rangeMap, model)
          }
        }
      }
      "put two and remove one should retain parts" in {
        forAll { (rangeToPut1: Range[Int, Int.type], rangeToPut2: Range[Int, Int.type], rangeToRemove: Range[Int, Int.type]) =>
          val model = gcc.TreeRangeMap.create[AsOrdered[Int], String]
          // create the model
          model.put(rangeToPut1.asJava, "1")
          model.put(rangeToPut2.asJava, "2")
          model.remove(rangeToRemove.asJava)

          // put & remove
          val rangeMap = (newBuilder += rangeToPut1 -> "1").result
          rangeMap.put(rangeToPut2, "2")
          rangeMap.remove(rangeToRemove)

          // compare
          verify(rangeMap, model)
        }
      }
    }
    "should implement #subRangeMap" - {
      "allow #put into a #subRangeMap" in {
        val model = gcc.TreeRangeMap.create[AsOrdered[Int], String]
        model.put(Range.open(3, 7).asJava, "1")
        model.put(Range.closed(9, 10).asJava, "2")
        model.put(Range.closed(12, 16).asJava, "3")

        val rangeMap = newBuilder.result
        rangeMap += Range.open(3, 7) -> "1"
        rangeMap += Range.closed(9, 10) -> "2"
        rangeMap += Range.closed(12, 16) -> "3"
        verify(rangeMap, model)

        val modelSub = model.subRangeMap(Range.closed(5, 11).asJava)
        val sub = rangeMap.subRangeMap(Range.closed(5, 11))
        verify(sub, modelSub)

        modelSub.put(Range.closed(7, 9).asJava, "4")
        sub.put(Range.closed(7, 9), "4")
        verify(sub, modelSub)

        intercept[IllegalArgumentException] {
          sub.put(Range.open(9, 12), "5")
        }

        val modelSubSub = modelSub.subRangeMap(Range.closedOpen(5, 5).asJava)
        val subsub = sub.subRangeMap(Range.closedOpen(5, 5))
        verify(subsub, modelSubSub)

        modelSubSub.put(Range.closedOpen(5, 5).asJava, "6")
        subsub += Range.closedOpen(5, 5) -> "6"
        verify(subsub, modelSubSub)
      }
    }
    "allow #remove from a #subRangeMap" in {
      val model = gcc.TreeRangeMap.create[AsOrdered[Int], String]
      model.put(Range.open(3, 7).asJava, "1")
      model.put(Range.closed(9, 10).asJava, "2")
      model.put(Range.closed(12, 16).asJava, "3")

      val rangeMap = newBuilder.result
      rangeMap += Range.open(3, 7) -> "1"
      rangeMap += Range.closed(9, 10) -> "2"
      rangeMap += Range.closed(12, 16) -> "3"
      verify(rangeMap, model)

      val modelSub = model.subRangeMap(Range.closed(5, 11).asJava)
      val sub = rangeMap.subRangeMap(Range.closed(5, 11))
      verify(sub, modelSub)

      modelSub.remove(Range.closed(7, 9).asJava)
      sub.remove(Range.closed(7, 9))
      verify(sub, modelSub)

      modelSub.remove(Range.closed(3, 9).asJava)
      sub -= Range.closed(3, 9)
      verify(sub, modelSub)

      val modelSubSub = modelSub.subRangeMap(Range.closedOpen(5, 5).asJava)
      val subsub = sub.subRangeMap(Range.closedOpen(5, 5))
      verify(subsub, modelSubSub)
    }

    "allow #clear from a #subRangeMap" in {
      val rangeMap = newBuilder.result
      rangeMap += Range.open(3, 7) -> "1"
      rangeMap += Range.closed(9, 10) -> "2"
      rangeMap += Range.closed(12, 16) -> "3"
      val sub = rangeMap.subRangeMap(Range.closed(5, 11))
      sub.clear
      rangeMap.asMapOfRanges should be(Map(Range.open(3, 5) -> "1", Range.closed(12, 16) -> "3"))
    }
  }

  private def verify(test: TIntRangeMapLike, model: gcc.RangeMap[AsOrdered[Int], String]) = {
    test.asMapOfRanges.map { case (range, value) => (range.asJava, value) } should be(model.asMapOfRanges.asScala)
  }

  def aRangeMapLike(newBuilder: => Builder[(TIntRange, String), TIntRangeMapLike]) = {
    "it should implement #isEmpty" - {
      "given the RangeMap is empty" - {
        val rangeMap = newBuilder.result
        "#asMapOfRanges should be empty" in {
          rangeMap.asMapOfRanges.isEmpty should be(true)
        }
        "#isEmpty should return true" in {
          rangeMap.isEmpty should be(true)
        }
      }
      "given the RangeMap contains {(1,2)->'a'}" - {
        val rangeMap = (newBuilder += (Range.open(1, 2) -> "a")).result
        "#asMapOfRanges should not be empty" in {
          rangeMap.asMapOfRanges.isEmpty should be(false)
        }
        "#isEmpty should return false" in {
          rangeMap.isEmpty should be(false)
        }
      }
    }

    "should calculate the hashCode as #asMapOfRanges.hashCode" - {
      "given the RangeMap is empty" - {
        val rangeMap = newBuilder.result
        "it's hashCode should be Map().hashCode" in {
          rangeMap.hashCode should be(rangeMap.asMapOfRanges.hashCode)
          rangeMap.hashCode should be(Map().hashCode)
        }
      }
      "given the RangeMap contains {(1,2)->'a'}" - {
        val rangeMap = (newBuilder += (Range.open(1, 2) -> "a")).result
        "it's hashCode should be Map().hashCode" in {
          rangeMap.hashCode should be(rangeMap.asMapOfRanges.hashCode)
          rangeMap.hashCode should be(Map(Range.open(1, 2) -> "a").hashCode)
        }
      }
    }

    "should be equal to another RangeMap if the maps returned by #asMapOfRanges are equal" - {
      "given the RangeMap is empty" - {
        val rangeMap = newBuilder.result
        "if the other RangeMap is empty it should be equal" in {
          val mocked = mock[TIntRangeMap]
          when(mocked.asMapOfRanges).thenReturn(Map[TIntRange, String]())
          rangeMap should be(mocked)
        }
        "if the other RangeMap contains {[1,8]->'a',[1,3]->'b'} it should not be equal" in {
          val mocked = mock[TIntRangeMap]
          when(mocked.asMapOfRanges).thenReturn(Map(Range.closed(1, 8) -> "a", Range.closed(1, 3) -> "b"))
          rangeMap should not be (mocked)
        }
      }
      "given the RangeMap contains {[5,8]->'a',[1,3)->'b'}" - {
        val rangeMap = (newBuilder += Range.closed(5, 8) -> "a" += Range.closedOpen(1, 3) -> "b").result
        "if the other RangeMap contains {[5,8]->'a',[1,3)->'b'} it should be equal" in {
          val mocked = mock[TIntRangeMap]
          when(mocked.asMapOfRanges).thenReturn(Map(Range.closed(5, 8) -> "a", Range.closedOpen(1, 3) -> "b"))
          rangeMap should be(mocked)
        }
        "if the other RangeMap contains {[1,8]->'a',[1,3]->'b'} it should not be equal" in {
          val mocked = mock[TIntRangeMap]
          when(mocked.asMapOfRanges).thenReturn(Map(Range.closed(1, 8) -> "a", Range.closed(1, 3) -> "b"))
          rangeMap should not be (mocked)
        }
      }
    }

    "should implement #get" in {
      forAll { t: (Range[Int, Int.type], Range[Int, Int.type]) =>
        val (range1, range2) = t
        val rangeMap = (newBuilder += range1 -> "a" += range2 -> "b").result
        for (i <- minBound to maxBound) {
          val expected = {
            if (range1.contains(i)) Some("a")
            else if (range2.contains(i)) Some("b")
            else None
          }
          rangeMap.get(i) should be(expected)
        }
      }
    }

    "should implement #asMapOfRanges" - {
      "given the RangeMap is empty" - {
        val rangeMap = newBuilder.result
        "it should return an empty Map" in {
          rangeMap.asMapOfRanges should be(Map())
        }
      }
      "given the RangeMap contains any two ranges" - {
        "it should return a map with these ranges" in {
          forAll { t: (Range[Int, Int.type], Range[Int, Int.type]) =>
            val (range1, range2) = t
            val rangeMap = (newBuilder += range1 -> "a" += range2 -> "b").result
            rangeMap.asMapOfRanges should be(Map(range1 -> "a", range2 -> "b"))
          }
        }
      }
    }

    "should implement #span" - {
      "given the RangeMap is empty" - {
        val rangeMap = newBuilder.result
        "it should return None" in {
          rangeMap.span should be(None)
        }
      }
      "given the RangeMap contains a single range" - {
        "it should return Some(thatRanage)" in {
          forAll { range: Range[Int, Int.type] =>
            val rangeMap = (newBuilder += range -> "dummy").result
            rangeMap.span should be(Some(range))
          }
        }
      }
      "given the RangeMap contains any two ranges" - {
        "it should return the span of both" in {
          forAll { t: (Range[Int, Int.type], Range[Int, Int.type]) =>
            val (range1, range2) = t
            val rangeMap = (newBuilder += range1 -> "a" += range2 -> "b").result
            rangeMap.span should be(Some(range1 span range2))
          }
        }
      }
    }

    "should implement #subRangeMap" - {
      "given the RangeMap is empty" - {
        val rangeMap = newBuilder.result
        "it should return an empty RangeMap for all args" in {
          forAll { range: Range[Int, Int.type] =>
            rangeMap.subRangeMap(range).isEmpty should be(true)
            rangeMap.subRangeMap(range).asMapOfRanges should be(Map())
          }
        }
      }
      "given the RangeMap contains any two ranges" - {
        "it should return a subrange view" in {
          forAll { (t: (Range[Int, Int.type], Range[Int, Int.type]), subRange: Range[Int, Int.type]) =>
            val (range1, range2) = t
            val rangeMap = (newBuilder += range1 -> "a" += range2 -> "b").result
            val expected = {
              val builder = newBuilder
              rangeMap.asMapOfRanges foreach ({
                case (key, value) => {
                  if (key.isConnected(subRange) && !key.intersection(subRange).isEmpty)
                    builder += key.intersection(subRange) -> value
                }
              })
              builder.result
            }
            rangeMap.subRangeMap(subRange) should be(expected)
          }
        }
      }
      "given a subrange map a call to #subRangeMap should return a sub-sub rangeMap" in {
        val model = gcc.TreeRangeMap.create[AsOrdered[Int], String]
        model.put(Range.open(3, 7).asJava, "1")
        model.put(Range.closed(9, 10).asJava, "2")
        model.put(Range.closed(12, 16).asJava, "3")
        val modelSub1 = model.subRangeMap(Range.closed(5, 11).asJava)
        val modelSub2 = modelSub1.subRangeMap(Range.open(6, 15).asJava)

        val rangeMap = (newBuilder += Range.open(3, 7) -> "1" += Range.closed(9, 10) -> "2" += Range.closed(12, 16) -> "3").result
        val sub1 = rangeMap.subRangeMap(Range.closed(5, 11))
        val sub2 = sub1.subRangeMap(Range.open(6, 15))
        verify(rangeMap, model)
        verify(sub1, modelSub1)
        verify(sub2, modelSub2)
      }
    }
  }
}
