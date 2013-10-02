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

import scala.math.Ordering.Int

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect.Bound.FiniteBound
import org.feijoas.mango.common.collect.Bound.InfiniteBound
import org.feijoas.mango.common.collect.DiscreteDomain.IntDomain
import org.scalatest.FlatSpec
import org.scalatest.Matchers.be
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.prop.PropertyChecks

import com.google.common.testing.SerializableTester.reserializeAndAssert

/** Tests for [[Range]]
 *
 *  @author Markus Schneider
 *  @since 0.8
 */
class RangeTest extends FlatSpec with RangeBehaviors {

  {
    val builder = (start: Int, end: Int) => Range.open(start, end)

    "An open range" should behave like allRanges(builder)
    it should behave like boundedRange(builder)
    it should behave like leftOpenBoundedRange(builder)
    it should behave like rightOpenBoundedRange(builder)

    it should "not acccept the same value as lower and upper bound" in {
      intercept[IllegalArgumentException] {
        builder(3, 3)
      }
    }

    it should "enclose other ranges" in {
      val range = Range.open(2, 5)
      range encloses Range.open(2, 4) should be(true)
      range encloses Range.open(3, 5) should be(true)
      range encloses Range.closed(3, 4) should be(true)

      range encloses Range.openClosed(2, 5) should be(false)
      range encloses Range.closedOpen(2, 5) should be(false)
      range encloses Range.closed(1, 4) should be(false)
      range encloses Range.closed(3, 6) should be(false)
      range encloses Range.greaterThan(3) should be(false)
      range encloses Range.atLeast(3) should be(false)
      range encloses Range.atMost(3) should be(false)
      range encloses Range.all() should be(false)
    }
  }

  {
    val builder = (start: Int, end: Int) => Range.closed(start, end)

    "A closed range" should behave like allRanges(builder)
    it should behave like boundedRange(builder)
    it should behave like leftClosedBoundedRange(builder)
    it should behave like rightClosedBoundedRange(builder)

    it should "acccept the same value as lower and upper bound" in {
      val range = builder(3, 3)
      range.isEmpty should be(false)
      range.contains(2) should be(false)
      range.contains(3) should be(true)
      range.contains(4) should be(false)
    }
    it should "enclose other ranges" in {

      val range = Range.closed(2, 5)
      range encloses Range.open(2, 5) should be(true)
      range encloses Range.openClosed(2, 5) should be(true)
      range encloses Range.closedOpen(2, 5) should be(true)
      range encloses Range.closed(3, 5) should be(true)
      range encloses Range.closed(2, 4) should be(true)

      range encloses Range.open(1, 6) should be(false)
      range encloses Range.greaterThan(3) should be(false)
      range encloses Range.lessThan(3) should be(false)
      range encloses Range.atLeast(3) should be(false)
      range encloses Range.atMost(3) should be(false)
      range encloses Range.all() should be(false)
    }
  }

  {
    val builder = (start: Int, end: Int) => Range.closedOpen(start, end)

    "A closedOpen range" should behave like allRanges(builder)
    it should behave like boundedRange(builder)
    it should behave like leftClosedBoundedRange(builder)
    it should behave like rightOpenBoundedRange(builder)

    it should "acccept the same value as lower and upper bound" in {
      val range = builder(3, 3)
      range.isEmpty should be(true)
      range.contains(2) should be(false)
      range.contains(3) should be(false)
      range.contains(4) should be(false)
    }
  }

  {
    val builder = (start: Int, end: Int) => Range.openClosed(start, end)

    "A openClosed range" should behave like allRanges(builder)
    it should behave like boundedRange(builder)
    it should behave like leftOpenBoundedRange(builder)
    it should behave like rightClosedBoundedRange(builder)

    it should "acccept the same value as lower and upper bound" in {
      val range = builder(3, 3)
      range.isEmpty should be(true)
      range.contains(2) should be(false)
      range.contains(3) should be(false)
      range.contains(4) should be(false)
    }
  }

  {
    "A Range" should "be determine if it is connected to another Rane" in {
      Range.closed(3, 5) isConnected Range.open(5, 6) should be(true)
      Range.closed(3, 5) isConnected Range.openClosed(5, 5) should be(true)
      Range.open(3, 5) isConnected Range.closed(5, 6) should be(true)
      Range.closed(3, 7) isConnected Range.open(6, 8) should be(true)
      Range.open(3, 7) isConnected Range.closed(5, 6) should be(true)
      Range.closed(3, 5) isConnected Range.closed(7, 8) should be(false)
      Range.closed(3, 5) isConnected Range.closedOpen(7, 7) should be(false)
    }
    it should "detemine if it contains all elements in an iterable" in {
      val range = Range.closed(3, 5)
      range containsAll List(3, 3, 4, 5) should be(true)
      range containsAll List(3, 3, 4, 5, 6) should be(false)
      range containsAll List() should be(true)

      Range.openClosed(3, 3) containsAll List() should be(true)
    }
  }

  {
    val builder = (_: Int, value: Int) => Range.lessThan(value)
    "A lessThan range" should behave like allRanges(builder)
    it should behave like rightOpenBoundedRange(builder)
    it should behave like leftUnboundedRange(builder)
  }

  {
    val builder = (_: Int, value: Int) => Range.atMost(value)
    "A atMost range" should behave like allRanges(builder)
    it should behave like rightClosedBoundedRange(builder)
    it should behave like leftUnboundedRange(builder)
  }

  {
    val builder = (value: Int, _: Int) => Range.greaterThan(value)
    "A greaterThan range" should behave like allRanges(builder)
    it should behave like leftOpenBoundedRange(builder)
    it should behave like rightUnboundedRange(builder)
  }

  {
    val builder = (value: Int, _: Int) => Range.atLeast(value)
    "A atLeast range" should behave like allRanges(builder)
    it should behave like leftClosedBoundedRange(builder)
    it should behave like rightUnboundedRange(builder)
  }

  {
    val builder = (_: Int, _: Int) => Range.all[Int, Int.type]()
    "A all range" should behave like allRanges(builder)
    it should behave like leftUnboundedRange(builder)
    it should behave like rightUnboundedRange(builder)
  }

  "Range#intersection" should "throw an exception if the ranges are not connected" in {
    val range = Range.closedOpen(3, 3)
    range.intersection(range) should be(range)

    intercept[IllegalArgumentException] {
      range intersection Range.open(3, 5)
    }

    intercept[IllegalArgumentException] {
      range intersection Range.closed(0, 2)
    }
  }

  it should "intersect if ranges overlap" in {

    val range = Range.closed(4, 8)

    // adjacent below
    range intersection Range.closedOpen(2, 4) should be(Range.closedOpen(4, 4))

    // overlap below
    range intersection Range.closed(2, 6) should be(Range.closed(4, 6))

    // enclosed with same start
    range intersection Range.closed(4, 6) should be(Range.closed(4, 6))

    // enclosed, interior
    range intersection Range.closed(5, 7) should be(Range.closed(5, 7))

    // enclosed with same end
    range intersection Range.closed(6, 8) should be(Range.closed(6, 8))

    // enclosing with same start
    range intersection Range.closed(4, 10) should be(range)

    // enclosing with same end
    range intersection Range.closed(2, 8) should be(range)

    // enclosing, exterior
    range intersection Range.closed(2, 10) should be(range)

    // overlap above
    range intersection Range.closed(6, 10) should be(Range.closed(6, 8))

    // adjacent above
    range intersection Range.openClosed(8, 10) should be(Range.openClosed(8, 8))

    // separate above
    intercept[IllegalArgumentException] {
      range intersection Range.closed(10, 12)
    }

    intercept[IllegalArgumentException] {
      range intersection Range.closed(0, 2)
    }
  }

  "Range#span" should "return the minimal range that encloses both ranges" in {

    val range = Range.closed(4, 8)

    // separate below
    range span Range.closed(0, 2) should be(Range.closed(0, 8))
    range span Range.atMost(2) should be(Range.atMost(8))

    // adjacent below
    range span Range.closedOpen(2, 4) should be(Range.closed(2, 8))
    range span Range.lessThan(4) should be(Range.atMost(8))

    // overlap below
    range span Range.closed(2, 6) should be(Range.closed(2, 8))
    range span Range.atMost(6) should be(Range.atMost(8))

    // enclosed with same start
    range span Range.closed(4, 6) should be(range)

    // enclosed, interior
    range span Range.closed(5, 7) should be(range)

    // enclosed with same end
    range span Range.closed(6, 8) should be(range)

    // equal
    range span range should be(range)

    // enclosing with same start
    range span Range.closed(4, 10) should be(Range.closed(4, 10))
    range span Range.atLeast(4) should be(Range.atLeast(4))

    // enclosing with same end
    range span Range.closed(2, 8) should be(Range.closed(2, 8))
    range span Range.atMost(8) should be(Range.atMost(8))

    // enclosing, exterior
    range span Range.closed(2, 10) should be(Range.closed(2, 10))
    range span Range.all() should be(Range.all[Int, Int.type]())

    // overlap above
    range span Range.closed(6, 10) should be(Range.closed(4, 10))
    range span Range.atLeast(6) should be(Range.atLeast(4))

    // adjacent above
    range span Range.closed(8, 10) should be(Range.closed(4, 10))
    range span Range.atLeast(8) should be(Range.atLeast(4))

    // separate above
    range span Range.closed(10, 12) should be(Range.closed(4, 12))
    range span Range.atLeast(10) should be(Range.atLeast(4))
  }

  "(De-factor empty range)#intersection" should "throw an exception if the ranges are not connected" in {
    val range = Range.open(3, 4)
    range intersection range should be(range)

    range intersection Range.atMost(3) should be(Range.openClosed(3, 3))
    range intersection Range.atLeast(4) should be(Range.closedOpen(4, 4))

    intercept[IllegalArgumentException] {
      range intersection Range.lessThan(3)
    }

    intercept[IllegalArgumentException] {
      range intersection Range.greaterThan(4)
    }

    Range.closed(3, 4) intersection Range.greaterThan(4) should be(Range.openClosed(4, 4))
  }

  "A singleton range" should "be able to intersect with connected ranges" in {
    val range = Range.closed(3, 3)
    range intersection range should be(range)
    range intersection Range.atMost(4) should be(range)
    range intersection Range.atMost(3) should be(range)
    range intersection Range.atLeast(3) should be(range)
    range intersection Range.atLeast(2) should be(range)

    range intersection Range.lessThan(3) should be(Range.closedOpen(3, 3))
    range intersection Range.greaterThan(3) should be(Range.openClosed(3, 3))

    intercept[IllegalArgumentException] {
      range intersection Range.atLeast(4)
    }

    intercept[IllegalArgumentException] {
      range intersection Range.atMost(2)
    }
  }

  "Range#ecloseAll" should "return a range that ecloses all given points" in {
    Range.encloseAll(List(0)) should be(Range.closed(0, 0))
    Range.encloseAll(List(5, -3)) should be(Range.closed(-3, 5))
    Range.encloseAll(List(1, 2, 2, 2, 5, -3, 0, -1)) should be(Range.closed(-3, 5))

    intercept[NoSuchElementException] {
      Range.encloseAll[Int, Int.type](List())
    }

    intercept[NullPointerException] {
      Range.encloseAll(List(null, Integer.valueOf(1)))
    }

    intercept[NullPointerException] {
      Range.encloseAll(List(Integer.valueOf(1), null))
    }
  }

  "Range#canonical" should "return the canonical form of a range" in {
    Range.closed(1, 4).canonical(IntDomain) should be(Range.closedOpen(1, 5))
    Range.open(0, 5).canonical(IntDomain) should be(Range.closedOpen(1, 5))
    Range.closedOpen(1, 5).canonical(IntDomain) should be(Range.closedOpen(1, 5))
    Range.openClosed(0, 4).canonical(IntDomain) should be(Range.closedOpen(1, 5))
    Range.closedOpen(scala.Int.MinValue, 0).canonical(IntDomain) should be(Range.closedOpen(scala.Int.MinValue, 0))
    Range.lessThan(0).canonical(IntDomain) should be(Range.closedOpen(scala.Int.MinValue, 0))
    Range.atMost(0).canonical(IntDomain) should be(Range.closedOpen(scala.Int.MinValue, 1))
    Range.atLeast(0).canonical(IntDomain) should be(Range.atLeast(0))
    Range.greaterThan(0).canonical(IntDomain) should be(Range.atLeast(1))

    Range.lessThan(0).canonical(UnboundedDomain) should be(Range.lessThan(0))
    Range.atMost(0).canonical(UnboundedDomain) should be(Range.lessThan(1))
    Range.atLeast(0).canonical(UnboundedDomain) should be(Range.atLeast(0))
    Range.greaterThan(0).canonical(UnboundedDomain) should be(Range.atLeast(1))

    Range.all[Int, Int.type]().canonical(UnboundedDomain) should be(Range.all[Int, Int.type]())
  }
}

private[mango] trait RangeBehaviors extends PropertyChecks {
  this: FlatSpec =>

  def allRanges(build: (Int, Int) => Range[Int, Int.type]) = {
    val start = -1
    val end = 5
    val range = build(start, end)

    it should "not be empty" in {
      range.isEmpty should be(false)
    }

    it should "pattern match with 'Range(lower, upper)'" in {
      range match {
        case Range(lower, upper) => // expected
        case _                   => fail
      }
    }

    it should "be displayed with '..' in the middle" in {
      range.toString contains ("\u2025") should be(true)
    }

    it should "be serializeable" in {
      reserializeAndAssert(range)
    }

    it should "enclose itself" in {
      range encloses range should be(true)
    }

    it should "be equal to itself" in {
      range equals build(start, end) should be(true)
    }

    it should "have the same hash code as an equal instance" in {
      range.hashCode equals build(start, end).hashCode should be(true)
    }
  }

  def boundedRange(build: (Int, Int) => Range[Int, Int.type]) = {
    val start = -4
    val end = 7
    val range = build(start, end)

    it should "contain only values wihtin the range" in {

      forAll { n: Int =>
        whenever((n != start && n != end) && (n < start - 10 || n > end + 10)) {
          if (n < start || n > end) {
            range.contains(n) should be(false)
            range(n) should be(false)
          } else {
            range.contains(n) should be(true)
            range(n) should be(true)
          }
        }
      }
    }

    it should "have a lower bound" in {
      range.hasLowerBound should be(true)
    }

    it should "have an upper bound" in {
      range.hasUpperBound should be(true)
    }

    it should "pattern match with 'Range(FiniteBound(start, _), FiniteBound(end, _))'" in {
      range match {
        case Range(FiniteBound(start, _), FiniteBound(end, _)) => // expected
        case _ => fail
      }
    }

    it should "be displayed as ?start..end?" in {
      val str = range.toString
      str.substring(1, str.length - 1) should be(start + "\u2025" + end)
    }

    it should "not acccept invalid inputs" in {
      intercept[IllegalArgumentException] {
        build(4, 3)
      }
    }
  }

  def leftOpenBoundedRange(build: (Int, Int) => Range[Int, Int.type]) = {
    val start = 5
    val end = 12
    val range = build(start, end)

    it should "not contain its left boundary value" in {
      range.contains(start) should be(false)
    }

    it should "be displayed as (start..end?" in {
      range.toString.startsWith("(" + start + "\u2025") should be(true)
    }
  }

  def rightOpenBoundedRange(build: (Int, Int) => Range[Int, Int.type]) = {
    val start = 5
    val end = 12
    val range = build(start, end)

    it should "not contain its right boundary value" in {
      range.contains(end) should be(false)
    }

    it should "be displayed as ?start..end)" in {
      val str = range.toString
      str.endsWith("\u2025" + end + ")") should be(true)
    }
  }

  def leftClosedBoundedRange(build: (Int, Int) => Range[Int, Int.type]) = {
    val start = 5
    val end = 12
    val range = build(start, end)

    it should "contain its left boundary value" in {
      range.contains(start) should be(true)
    }

    it should "be displayed as [start..end?" in {
      range.toString.startsWith("[" + start + "\u2025") should be(true)
    }
  }

  def rightClosedBoundedRange(build: (Int, Int) => Range[Int, Int.type]) = {
    val start = 5
    val end = 12
    val range = build(start, end)

    it should "contain its right boundary value" in {
      range.contains(end) should be(true)
    }

    it should "be displayed as ?start..end]" in {
      range.toString.endsWith("\u2025" + end + "]") should be(true)
    }
  }

  def leftUnboundedRange(build: (Int, Int) => Range[Int, Int.type]) = {
    val end = 16
    val range = build(0, end)

    it should "contain the smallest Int" in {
      range.contains(Integer.MIN_VALUE) should be(true)
    }

    it should "contain a value smaller than the right bound" in {
      range.contains(3) should be(true)
    }

    it should "be displayed as (-∞..end]" in {
      val str = range.toString
      str.substring(0, 3) should be("(-\u221e")
    }

    it should "pattern match with 'Range(InfiniteBound, upper)'" in {
      range match {
        case Range(InfiniteBound, upper) => // expected
        case _                           => fail
      }
    }
  }

  def rightUnboundedRange(build: (Int, Int) => Range[Int, Int.type]) = {
    val start = -7
    val range = build(start, 0)

    it should "contain the largest Int" in {
      range.contains(Integer.MAX_VALUE) should be(true)
    }

    it should "contain a value bigger than the left bound" in {
      range.contains(3) should be(true)
    }

    it should "be displayed as ?start..∞)" in {
      val str = range.toString
      str.endsWith("\u221e)") should be(true)
    }

    it should "pattern match with 'Range(FiniteBound(start, _), InfiniteBound)'" in {
      range match {
        case Range(lower, InfiniteBound) => // expected
        case _                           => fail
      }
    }
  }
}

private[mango] final object UnboundedDomain extends DiscreteDomain[Int] {
  override def next(value: Int): Option[Int] = IntDomain.next(value)
  override def previous(value: Int): Option[Int] = IntDomain.previous(value)
  override def distance(start: Int, end: Int): Long = IntDomain.distance(start, end)
}
