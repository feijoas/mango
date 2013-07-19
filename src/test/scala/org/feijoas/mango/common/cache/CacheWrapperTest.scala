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
package org.feijoas.mango.common.cache

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.cache.Cache._
import org.scalatest.{ FlatSpec, ShouldMatchers }
import org.scalatest.mock.MockitoSugar
import org.junit.Assert._
import com.google.common.cache.{ Cache => GuavaCache }

/** Tests for [[CacheWrapper]]
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
class CacheWrapperTest extends FlatSpec with CacheWrapperBehaviour with ShouldMatchers with MockitoSugar with CacheStatsMatcher {

  def wrappedCacheFixture = {
    val wrapped = mock[GuavaCache[String, Int]]
    val cache: Cache[String, Int] = wrapped.asScala
    (wrapped, cache)
  }

  behavior of "CacheWrapper"

  "LoadingCacheWrapper" should behave like forwardingWrapper(wrappedCacheFixture)

  it should "implement getIfPresent" in {

    val cache = CacheBuilder().recordStats.build[Any, Any]
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val one = new Object()
    val two = new Object()

    cache getIfPresent (one) should be(None)
    cache.stats should have(missCount(1), loadSuccessCount(0), loadExceptionCount(0), hitCount(0))
    cache.asMap.get(one) should be(None)
    cache.asMap.contains(one) should be(false)
    cache.asMap.values.toSet.contains(two) should be(false)

    cache getIfPresent (two) should be(None)
    cache.stats should have(missCount(2), loadSuccessCount(0), loadExceptionCount(0), hitCount(0))
    cache.asMap.get(one) should be(None)
    cache.asMap.contains(one) should be(false)
    cache.asMap.values.toSet.contains(two) should be(false)

    cache.put(one, two)
    assertSame(cache.getIfPresent(one).get, two)
    cache.stats should have(missCount(2), loadSuccessCount(0), loadExceptionCount(0), hitCount(1))
    assertSame(cache.asMap.get(one).get, two)
    cache.asMap.contains(one) should be(true)
    cache.asMap.values.toSet.contains(two) should be(true)

    cache getIfPresent (two) should be(None)
    cache.stats should have(missCount(3), loadSuccessCount(0), loadExceptionCount(0), hitCount(1))
    cache.asMap.get(two) should be(None)
    cache.asMap.contains(two) should be(false)
    cache.asMap.values.toSet.contains(one) should be(false)

    cache.put(two, one)
    assertSame(cache.getIfPresent(two).get, one)
    cache.stats should have(missCount(3), loadSuccessCount(0), loadExceptionCount(0), hitCount(2))
    assertSame(cache.asMap.get(two).get, one)
    cache.asMap.contains(two) should be(true)
    cache.asMap.values.toSet.contains(one) should be(true)
  }

  it should "implement getAllPresent" in {
    val cache = CacheBuilder().recordStats.build[Any, Any]
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    cache.getAllPresent(List()) should be(Map())
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    cache.getAllPresent(List(1, 2, 3)) should be(Map())
    cache.stats should have(missCount(3), loadSuccessCount(0), loadExceptionCount(0), hitCount(0))

    cache.put(2, 22)
    cache.getAllPresent(List(1, 2, 3)) should be(Map(2 -> 22))
    cache.stats should have(missCount(5), loadSuccessCount(0), loadExceptionCount(0), hitCount(1))

    cache.put(3, 33)
    cache.getAllPresent(List(1, 2, 3)) should be(Map(2 -> 22, 3 -> 33))
    cache.stats should have(missCount(6), loadSuccessCount(0), loadExceptionCount(0), hitCount(3))

    cache.put(1, 11)
    cache.getAllPresent(List(1, 2, 3)) should be(Map(1 -> 11, 2 -> 22, 3 -> 33))
    cache.stats should have(missCount(6), loadSuccessCount(0), loadExceptionCount(0), hitCount(6))
  }
}