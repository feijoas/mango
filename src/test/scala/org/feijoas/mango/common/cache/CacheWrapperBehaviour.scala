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

import java.util.concurrent.{ Callable => Callable, ConcurrentHashMap, ConcurrentMap, ExecutionException }

import scala.annotation.meta.{ beanGetter, beanSetter, field, getter, setter }
import scala.collection.convert.WrapAsJava.mapAsJavaMap
import scala.collection.convert.WrapAsScala.asScalaIterator

import org.feijoas.mango.common.annotations.Beta
import org.junit.Assert.assertSame
import org.mockito.{ ArgumentMatcher, Matchers }
import org.mockito.Matchers.{ argThat, isA }
import org.mockito.Mockito.{ verify, when }
import org.scalatest.{ FlatSpec, ShouldMatchers }

import com.google.common.cache.{ Cache => GuavaCache, CacheStats => GuavaCacheStats }
import com.google.common.collect.ImmutableMap

/** Shared tests for all CacheWrapper
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
trait CacheWrapperBehaviour extends ShouldMatchers { this: FlatSpec =>

  def forwardingWrapper(mockedFixture: => (GuavaCache[String, Int], Cache[String, Int])) = {

    behavior of "CacheWrapper"

    it should "forward #get to the underlying LoadingCache" in {
      val (wrapped, cache) = mockedFixture
      when(wrapped.getIfPresent("foo")).thenReturn(3)
      cache.getIfPresent("foo") should be(Some(3))
      verify(wrapped).getIfPresent("foo")
    }

    it should "forward #getOrElseUpdate to the underlying LoadingCache" in {
      val (wrapped, cache) = mockedFixture
      when(wrapped.get(Matchers.eq("foo"), isA(classOf[Callable[Int]]))).thenReturn(4)
      cache.getOrElseUpdate("foo", () => 4) should be(4)
      verify(wrapped).get(Matchers.eq("foo"), isA(classOf[Callable[Int]]))
    }

    it should "forward #getAllPresent to the underlying LoadingCache" in {
      val (wrapped, cache) = mockedFixture
      when(wrapped.getAllPresent(isA(classOf[java.lang.Iterable[String]]))).thenReturn(jImmutableMap("a" -> 1, "b" -> 2))
      cache.getAllPresent(List("a", "b")) should be(Map("a" -> 1, "b" -> 2))
      verify(wrapped).getAllPresent(anyIterableWith("a", "b"))
    }

    it should "forward #put to the underlying LoadingCache" in {
      val (wrapped, cache) = mockedFixture
      cache.put("foo", 3)
      verify(wrapped).put("foo", 3)
    }

    it should "forward #putAll to the underlying LoadingCache" in {
      val (wrapped, cache) = mockedFixture
      cache.putAll(Map("e" -> 11, "f" -> 13))
      verify(wrapped).putAll(ImmutableMap.of("e", 11, "f", 13))
    }

    it should "forward #invalidate to the underlying LoadingCache" in {
      val (wrapped, cache) = mockedFixture
      cache.invalidate("g")
      verify(wrapped).invalidate("g")
    }

    it should "forward #invalidateAll(keys) to the underlying LoadingCache" in {
      val (wrapped, cache) = mockedFixture
      cache.invalidateAll(List("h", "i"))
      verify(wrapped).invalidateAll(anyIterableWith("h", "i"))
    }

    it should "forward #invalidateAll to the underlying LoadingCache" in {
      val (wrapped, cache) = mockedFixture
      cache.invalidateAll
      verify(wrapped).invalidateAll()
    }

    it should "forward #size to the underlying LoadingCache" in {
      val (wrapped, cache) = mockedFixture
      when(wrapped.size()).thenReturn(7)
      cache.size should be(7)
      verify(wrapped).size()
    }

    it should "forward #stats to the underlying LoadingCache" in {
      val (wrapped, cache) = mockedFixture
      when(wrapped.stats()).thenReturn(new GuavaCacheStats(1, 2, 4, 6, 8, 16))
      cache.stats should be(CacheStats(1, 2, 4, 6, 8, 16))
      verify(wrapped).stats()
    }

    it should "forward #asMap to the underlying LoadingCache" in {
      val (wrapped, cache) = mockedFixture
      when(wrapped.asMap()).thenReturn(jConcurrentMap("c" -> 3, "d" -> 7))
      cache.asMap should be(Map("c" -> 3, "d" -> 7))
      verify(wrapped).asMap()
    }

    it should "forward #cleanUp to the underlying LoadingCache" in {
      val (wrapped, cache) = mockedFixture
      cache.cleanUp
      verify(wrapped).cleanUp()
    }
  }

  /** creates a Mockito `ArgumentMatcher` which checks that the `Iterable` to match
   *  has the provided elements
   */
  def anyIterableWith[T](elements: Any*) = argThat(new ArgumentMatcher[java.lang.Iterable[T]] {
    val expected = List(elements.seq: _*)
    override def matches(arg: Any) = arg match {
      case it: java.lang.Iterable[_] => expected sameElements it.iterator().toList
      case _                         => false
    }
  })

  /** creates a Guava ImmutableMap with elems */
  def jImmutableMap[K, V](elems: (K, V)*): ImmutableMap[K, V] = {
    val map: java.util.Map[K, V] = Map(elems: _*)
    ImmutableMap.copyOf(map)
  }

  /** creates a Java ConcurrentMap with elems */
  def jConcurrentMap[K, V](elems: (K, V)*): ConcurrentMap[K, V] = {
    val map = new ConcurrentHashMap[K, V]()
    map.putAll(jImmutableMap(elems: _*))
    map
  }
}