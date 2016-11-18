/*
 * Copyright (C) 2013 The Mango Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import scala.annotation.meta.{ beanGetter, beanSetter, field, getter, setter }
import scala.util.{ Failure, Success, Try }

import org.feijoas.mango.common.annotations.Beta
import org.mockito.Mockito.{ spy, times, verify }
import org.scalatest.{ FlatSpec, MustMatchers }
import org.scalatest.mockito.MockitoSugar

/**
 * Tests for [[LoadingCache]]
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from guava-libraries)
 */
class LoadingCacheTest extends FlatSpec with MustMatchers with MockitoSugar {

  def fixture = {
    val loader = (key: String) => 100 / key.length
    val cache = new MapLoadingCache(loader)
    (loader, cache)
  }

  behavior of "the default implementations of Cache"

  "getUnchecked" must "return a value or throw an exception" in {
    val (loader, cache) = fixture
    cache.getUnchecked("a") must be(100)
    intercept[com.google.common.util.concurrent.UncheckedExecutionException] {
      cache.getUnchecked("")
    }
  }

  "getAll" must "return Success with a map of all key/values or Failure" in {
    val (loader, cache) = fixture
    cache.getAll(List("a", "bb", "cc")) must be(Success(Map("a" -> 100, "bb" -> 50, "cc" -> 50)))
    cache.getAll(List("a", "", "cc")) match {
      case Failure(_) => // expected
      case other @ _  => fail("Expected Failure(_) but was " + other)
    }
  }

  "getAll" must "call get only once per key" in {
    val (loader, unspyedcache) = fixture
    val cache = spy(unspyedcache)
    cache.getAll(List("a", "bb", "a")) must be(Success(Map("a" -> 100, "bb" -> 50)))
    verify(cache, times(1)).get("a")
  }
}

/**
 * A cache implemented with a map
 */
protected[mango] class MapLoadingCache[K, V](loader: K => V) extends MapCache[K, V]() with LoadingCache[K, V] {
  def get(key: K): Try[V] = Try(loader(key))
  def refresh(key: K): Unit = loader(key)
}
