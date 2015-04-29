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
package org.feijoas.mango.common.cache

import org.feijoas.mango.common.cache.CacheStats._
import org.scalacheck.Gen
import org.scalatest.{ FlatSpec, ShouldMatchers }
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import com.google.common.cache.{ CacheStats => GuavaCacheStats }

/** Tests for [[CacheStats]]
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
class CacheStatsTest extends FlatSpec with GeneratorDrivenPropertyChecks with ShouldMatchers {

  behavior of "CacheStats"

  // a non-negative generator
  val nonNegGen: Gen[Long] = Gen.frequency(
    1 -> 0,
    2 -> Gen.choose(0, Long.MaxValue / 2))

  // a CacheStats generator
  val cacheStatsGen: Gen[GuavaCacheStats] = for {
    a <- nonNegGen
    b <- nonNegGen
    c <- nonNegGen
    d <- nonNegGen
    e <- nonNegGen
    f <- nonNegGen
  } yield new GuavaCacheStats(a, b, c, d, e, f)

  it should "have the same members and function values as Guava-CacheStats" in {

    forAll(cacheStatsGen) { (guava: GuavaCacheStats) =>
      val mango: CacheStats = guava.asScala

      // check members
      mango.hitCount should be(guava.hitCount())
      mango.loadCount should be(guava.loadCount())
      mango.missCount should be(guava.missCount())
      mango.loadExceptionCount should be(guava.loadExceptionCount())
      mango.loadSuccessCount should be(guava.loadSuccessCount())
      mango.evictionCount should be(guava.evictionCount())

      // check methods
      mango.hitRate should be(guava.hitRate())
      mango.loadExceptionRate should be(guava.loadExceptionRate())
      mango.missRate should be(guava.missRate())
      mango.totalLoadTime should be(guava.totalLoadTime())
      mango.requestCount should be(guava.requestCount())
      mango.averageLoadPenalty should be(guava.averageLoadPenalty())
    }
  }

  it should "be able to add values" in {
    forAll(cacheStatsGen, cacheStatsGen) {
      (guavaA: GuavaCacheStats, guavaB: GuavaCacheStats) =>
        val mangoA: CacheStats = guavaA.asScala
        val mangoB: CacheStats = guavaB.asScala

        // if the first test passes we can safely convert a Guava-CacheStats
        // to a mango implementation
        (mangoA + mangoB) should be(guavaA.plus(guavaB).asScala)
    }
  }

  it should "be able to subtract values" in {
    forAll(cacheStatsGen, cacheStatsGen) {
      (guavaA: GuavaCacheStats, guavaB: GuavaCacheStats) =>
        val mangoA: CacheStats = guavaA.asScala
        val mangoB: CacheStats = guavaB.asScala

        // if the first test passes we can safely convert a Guava-CacheStats
        // to a mango implementation
        (mangoA - mangoB) should be(guavaA.minus(guavaB).asScala)
    }
  }

  it should "throw an exception if negative values are passed in constructor" in {
    // 0 is allowed
    CacheStats(0, 0, 0, 0, 0, 0)

    // neg. values not
    intercept[IllegalArgumentException] { CacheStats(-1, 0, 0, 0, 0, 0) }
    intercept[IllegalArgumentException] { CacheStats(0, -1, 0, 0, 0, 0) }
    intercept[IllegalArgumentException] { CacheStats(0, 0, -1, 0, 0, 0) }
    intercept[IllegalArgumentException] { CacheStats(0, 0, 0, -1, 0, 0) }
    intercept[IllegalArgumentException] { CacheStats(0, 0, 0, 0, -1, 0) }
    intercept[IllegalArgumentException] { CacheStats(0, 0, 0, 0, 0, -1) }
  }

}