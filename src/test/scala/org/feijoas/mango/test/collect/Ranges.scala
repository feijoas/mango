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
package org.feijoas.mango.test.collect

import scala.math.Ordering.Int

import org.feijoas.mango.common.collect.BoundType.Closed
import org.feijoas.mango.common.collect.BoundType.Open
import org.feijoas.mango.common.collect.Range
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.oneOf

/** Scalacheck generators and utilities
 *
 *  @author Markus Schneider
 *  @since 0.9
 */
object Ranges {

  val minBound = 0
  val maxBound = 10

  /** A list of various ranges
   */
  private lazy val ranges = {
    val builder = List.newBuilder[Range[Int, Int.type]]
    // Add one-ended ranges
    for (
      i <- minBound to maxBound;
      boundType <- List(Open, Closed)
    ) {
      builder += Range.upTo(i, boundType)
      builder += Range.downTo(i, boundType)
    }

    // Add two-ended ranges
    for (
      i <- minBound to maxBound;
      j <- (i + 1) to maxBound;
      lowerType <- List(Open, Closed);
      upperType <- List(Open, Closed) if (!(i == j & lowerType == Open & upperType == Open))
    ) {
      builder += Range.range(i, lowerType, j, upperType)
    }
    builder.result
  }

  /** A list of pairs of ranges that are not overlaping
   */
  private lazy val rangeTuples = for (
    range1 <- ranges;
    range2 <- ranges if (!range1.isConnected(range2) || range1.intersection(range2).isEmpty())
  ) yield (range1, range2)

  implicit lazy val arbRange: Arbitrary[Range[Int, Int.type]] = Arbitrary {
    oneOf(ranges)
  }

  implicit lazy val arbNonOverlappingRangeParis: Arbitrary[(Range[Int, Int.type], Range[Int, Int.type])] = Arbitrary {
    oneOf(rangeTuples)
  }

  implicit lazy val genOpenRange = for {
    lower <- arbitrary[Int]
    upper <- arbitrary[Int]
    if (lower < upper)
  } yield Range.open(lower, upper)

  implicit lazy val genClosedRange = for {
    lower <- arbitrary[Int]
    upper <- arbitrary[Int]
    if (lower <= upper)
  } yield Range.closed(lower, upper)

  implicit lazy val genOpenClosedRange = for {
    lower <- arbitrary[Int]
    upper <- arbitrary[Int]
    if (lower < upper)
  } yield Range.openClosed(lower, upper)

  implicit lazy val genClosedOpenRange = for {
    lower <- arbitrary[Int]
    upper <- arbitrary[Int]
    if (lower < upper)
  } yield Range.closedOpen(lower, upper)

  implicit lazy val genAtLeastRange = for {
    endpoint <- arbitrary[Int]
  } yield Range.atLeast(endpoint)

  implicit lazy val genAtMostRange = for {
    endpoint <- arbitrary[Int]
  } yield Range.atMost(endpoint)

  implicit lazy val genLessThanRange = for {
    endpoint <- arbitrary[Int]
  } yield Range.lessThan(endpoint)

  implicit lazy val genGreaterThanRange = for {
    endpoint <- arbitrary[Int]
  } yield Range.greaterThan(endpoint)
}