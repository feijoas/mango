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

import org.scalatest.matchers._

/** CacheStatsMatcher
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from Guava-libraries)
 */
trait CacheStatsMatcher {

  def hitCount(expectedValue: Long) =
    new HavePropertyMatcher[CacheStats, Long] {
      def apply(stats: CacheStats) = HavePropertyMatchResult(
        stats.hitCount == expectedValue,
        "hitCount",
        expectedValue,
        stats.hitCount)
    }

  def missCount(expectedValue: Long) =
    new HavePropertyMatcher[CacheStats, Long] {
      def apply(stats: CacheStats) = HavePropertyMatchResult(
        stats.missCount == expectedValue,
        "missCount",
        expectedValue,
        stats.missCount)
    }

  def loadSuccessCount(expectedValue: Long) =
    new HavePropertyMatcher[CacheStats, Long] {
      def apply(stats: CacheStats) = HavePropertyMatchResult(
        stats.loadSuccessCount == expectedValue,
        "loadSuccessCount",
        expectedValue,
        stats.loadSuccessCount)
    }

  def loadExceptionCount(expectedValue: Long) =
    new HavePropertyMatcher[CacheStats, Long] {
      def apply(stats: CacheStats) = HavePropertyMatchResult(
        stats.loadExceptionCount == expectedValue,
        "loadExceptionCount",
        expectedValue,
        stats.loadExceptionCount)
    }

  def totalLoadTime(expectedValue: Long) =
    new HavePropertyMatcher[CacheStats, Long] {
      def apply(stats: CacheStats) = HavePropertyMatchResult(
        stats.totalLoadTime == expectedValue,
        "totalLoadTime",
        expectedValue,
        stats.totalLoadTime)
    }

  def evictionCount(expectedValue: Long) =
    new HavePropertyMatcher[CacheStats, Long] {
      def apply(stats: CacheStats) = HavePropertyMatchResult(
        stats.evictionCount == expectedValue,
        "evictionCount",
        expectedValue,
        stats.evictionCount)
    }
}