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

import java.util.concurrent.ExecutionException
import java.util.logging.{ Level, Logger }
import java.lang.{ Iterable => jIterable }
import scala.annotation.meta.{ beanGetter, beanSetter, field, getter, setter }
import scala.concurrent.duration.MILLISECONDS
import scala.concurrent.Future._
import scala.collection.mutable
import scala.util.{ Failure, Success }
import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.base.Ticker.asMangoTickerConverter
import org.feijoas.mango.common.cache.LoadingCache.asMangoLoadingCacheConverter
import org.feijoas.mango.common.util.concurrent.Futures.asScalaFutureConverter
import org.junit.Assert.{ assertSame, assertEquals, assertTrue }
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{ times, verify, when }
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import com.google.common.cache.CacheLoader.InvalidCacheLoadException
import com.google.common.cache.{ LoadingCache => GuavaLoadingCache }
import com.google.common.testing.FakeTicker
import com.google.common.util.concurrent.{ ExecutionError }
import scala.concurrent.Future
import scala.util.Failure
import org.scalatest.BeforeAndAfter
import com.google.common.util.concurrent.UncheckedExecutionException
import java.util.concurrent.atomic.AtomicInteger
import scala.ref.WeakReference
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import java.io.IOException
import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * Tests for [[LoadingCacheWrapperTest]]
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
class LoadingCacheWrapperTest extends FlatSpec
    with CacheWrapperBehaviour
    with Matchers
    with MockitoSugar
    with CacheStatsMatcher
    with BeforeAndAfter {

  before {
    // disable Guava log messages
    import java.util.logging._
    val logger = Logger.getLogger("com.google.common.cache")
    logger.setLevel(Level.OFF)
  }

  def wrappedCacheFixture = {
    val wrapped = mock[GuavaLoadingCache[String, Int]]
    val cache: LoadingCache[String, Int] = wrapped.asScala
    (wrapped, cache)
  }

  "LoadingCacheWrapper" should behave like forwardingWrapper(wrappedCacheFixture)

  behavior of "LoadingCacheWrapper"

  it should "forward #get to the underlying LoadingCache" in {
    val (wrapped, cache) = wrappedCacheFixture

    val getExecutionException = new ExecutionException(new Throwable)
    when(wrapped.get("foo")).thenReturn(3).thenThrow(getExecutionException)
    cache.get("foo") should be(Success(3))
    cache.get("foo") should be(Failure(getExecutionException))
    verify(wrapped, times(2)).get("foo")
  }

  it should "forward #getUnchecked to the underlying LoadingCache" in {
    val (wrapped, cache) = wrappedCacheFixture
    when(wrapped.getUnchecked("foo")).thenReturn(4)
    cache.getUnchecked("foo") should be(4)
    verify(wrapped).getUnchecked("foo")
  }

  it should "forward #refresh to the underlying LoadingCache" in {
    val (wrapped, cache) = wrappedCacheFixture
    cache.refresh("foo")
    verify(wrapped).refresh("foo")
  }

  it should "forward #getAll to the underlying LoadingCache" in {
    val (wrapped, cache) = wrappedCacheFixture
    val getAllExecutionException = new ExecutionException(new Throwable)

    when(wrapped.getAll(isA(classOf[jIterable[String]])))
      .thenReturn(jImmutableMap("a" -> 1, "b" -> 2))
      .thenThrow(getAllExecutionException)
    cache.getAll(List("a", "b")) should be(Success(Map("a" -> 1, "b" -> 2)))
    cache.getAll(List("a", "b")) should be(Failure(getAllExecutionException))
    verify(wrapped, times(2)).getAll(anyIterableWith("a", "b"))
  }

  it should "forward #apply(key) to the underlying LoadingCache" in {
    val (wrapped, cache) = wrappedCacheFixture

    when(wrapped.getUnchecked("foo")).thenReturn(4)
    cache("foo") should be(4)
    verify(wrapped).getUnchecked("foo")
  }

  it should "forward #apply(keys) to the underlying LoadingCache" in {
    val (wrapped, cache) = wrappedCacheFixture

    when(wrapped.getAll(isA(classOf[jIterable[String]])))
      .thenReturn(jImmutableMap("a" -> 1, "b" -> 2))
    cache(List("a", "b")) should be(Map("a" -> 1, "b" -> 2))
    verify(wrapped).getAll(anyIterableWith("a", "b"))
  }

  it should "load values if not present" in {
    val cache = CacheBuilder.newBuilder().recordStats().build((a: Any) => a)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key1 = new Object()
    assertSame(key1, cache.get(key1).get)
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    val key2 = new Object()
    assertSame(key2, cache.getUnchecked(key2))
    cache.stats should have(missCount(2), loadSuccessCount(2), loadExceptionCount(0), hitCount(0))

    val key3 = new Object()
    cache.refresh(key3)
    cache.stats should have(missCount(2), loadSuccessCount(3), loadExceptionCount(0), hitCount(0))
    assertSame(key3, cache.get(key3).get)
    cache.stats should have(missCount(2), loadSuccessCount(3), loadExceptionCount(0), hitCount(1))

    // callable is not called
    assertSame(key3, cache.getOrElseUpdate(key3, () => throw new Exception()))
    cache.stats should have(missCount(2), loadSuccessCount(3), loadExceptionCount(0), hitCount(2))

    val key4 = new Object()
    val value4 = new Object()
    assertSame(value4, cache.getOrElseUpdate(key4, () => value4))
    cache.stats should have(missCount(3), loadSuccessCount(4), loadExceptionCount(0), hitCount(2))
  }

  it should "be able to reload values" in {
    val one = new Integer(1)
    val two = new Integer(2)

    val loader = new CacheLoader[Any, Any] {
      override def load(key: Any) = one
      override def reload(key: Any, oldValue: Any) = Future.successful(two)
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object()
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    cache.refresh(key)
    cache.stats should have(missCount(1), loadSuccessCount(2), loadExceptionCount(0), hitCount(0))

    assertSame(two, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(2), loadExceptionCount(0), hitCount(1))
  }

  it should "be able to refresh values" in {
    val one = new Object()
    val two = new Object()
    val ticker = new FakeTicker
    val loader = new CacheLoader[Any, Any] {
      override def load(key: Any) = one
      override def reload(key: Any, oldValue: Any) = Future.successful(two)
    }

    val cache = CacheBuilder.newBuilder()
      .recordStats()
      .ticker(ticker.asScala)
      .refreshAfterWrite(1, MILLISECONDS)
      .build(loader)

    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object()
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    ticker.advance(1, MILLISECONDS)
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(1))

    ticker.advance(1, MILLISECONDS)
    assertSame(two, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(2), loadExceptionCount(0), hitCount(2))

    ticker.advance(1, MILLISECONDS)
    assertSame(two, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(2), loadExceptionCount(0), hitCount(3))
  }

  it should "reload the value if refreshAfterWrite expires" in {
    val one = new Object()
    val two = new Object()
    val ticker = new FakeTicker()
    val loader = new CacheLoader[Any, Any] {
      override def load(key: Any) = one
      override def reload(key: Any, oldValue: Any) = Future.successful(two)
    }

    val cache = CacheBuilder.newBuilder()
      .recordStats()
      .ticker(ticker.asScala)
      .refreshAfterWrite(1, MILLISECONDS)
      .build(loader)

    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object()
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getIfPresent(key).get)
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(1))

    ticker.advance(1, MILLISECONDS);
    assertSame(two, cache.getIfPresent(key).get)
    cache.stats should have(missCount(1), loadSuccessCount(2), loadExceptionCount(0), hitCount(2))

    ticker.advance(1, MILLISECONDS);
    assertSame(two, cache.getIfPresent(key).get)
    cache.stats should have(missCount(1), loadSuccessCount(2), loadExceptionCount(0), hitCount(3))
  }

  it should "be able to bulk load values" in {
    val bulkLoader = new CacheLoader[Int, Int]() {
      override def load(key: Int) = key
      override def loadAll(keys: Traversable[Int]): Map[Int, Int] = {
        keys.map { (key: Int) => (key, load(key)) }.toMap
      }
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(bulkLoader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    cache.getAll(List()) should be(Success(Map()))
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    cache.getAll(List(1)) should be(Success(Map(1 -> 1)))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    cache.getAll(List(1, 2, 3, 4)) should be(Success(Map(1 -> 1, 2 -> 2, 3 -> 3, 4 -> 4)))
    cache.stats should have(missCount(4), loadSuccessCount(2), loadExceptionCount(0), hitCount(1))

    cache.getAll(List(2, 3)) should be(Success(Map(2 -> 2, 3 -> 3)))
    cache.stats should have(missCount(4), loadSuccessCount(2), loadExceptionCount(0), hitCount(3))

    cache.getAll(List(4, 5)) should be(Success(Map(4 -> 4, 5 -> 5)))
    cache.stats should have(missCount(5), loadSuccessCount(3), loadExceptionCount(0), hitCount(4))
  }

  it should "ignore extra values returned from a bulk loader" in {
    val extraValueBulkLoader = new CacheLoader[Any, Any]() {
      override def load(key: Any) = new Object()
      override def loadAll(keys: Traversable[Any]): Map[Any, Any] = {
        val kvs = keys.map { case key => (key, new Object) }
        kvs.toMap ++ // add extra entries
          kvs.map { case (key, value) => (value, key) }.toMap
      }
    }

    val cache = CacheBuilder.newBuilder().build(extraValueBulkLoader)

    val lookupKeys = List(new Object, new Object, new Object)
    val result = cache.getAll(lookupKeys).get
    result.keySet should be(lookupKeys.toSet)

    result.foreach {
      case (key: Any, value: Any) =>
        assertSame(value, result.get(key).get)
        result.get(value) should be(None)
        assertSame(value, cache.asMap().get(key).get)
        assertSame(key, cache.asMap().get(value).get)
    }
  }

  it should "not call CacheLoader#load if bulk loading is required" in {
    val extraKey: Any = new Object
    val extraValue: Any = new Object
    val clobbingBulkLoader = new CacheLoader[Any, Any]() {
      override def load(key: Any) = throw new AssertionError()
      override def loadAll(keys: Traversable[Any]): Map[Any, Any] = {
        val kvs = keys.map { case key => (key, new Object) }.toMap
        kvs.toMap + ((extraKey, extraValue))
      }
    }

    val cache = CacheBuilder.newBuilder().build(clobbingBulkLoader)
    cache.asMap().put(extraKey, extraKey)
    assertSame(extraKey, cache.asMap().get(extraKey).get)

    val lookupKeys = List(new Object, new Object, new Object)
    val result = cache.getAll(lookupKeys).get
    result.keySet should be(lookupKeys.toSet)

    result.foreach {
      case (key: Any, value: Any) =>
        assertSame(value, result.get(key).get)
        assertSame(value, cache.asMap().get(key).get)
    }

    result.get(extraKey) should be(None)
    assertSame(extraValue, cache.asMap().get(extraKey).get)
  }

  it should "#getAll should be Failure if not all keys are in the Map returned by Loader#loadAll" in {
    val extraKey: Any = new Object
    val extraValue: Any = new Object
    val ignoringBulkLoader = new CacheLoader[Any, Any]() {
      override def load(key: Any) = throw new AssertionError()
      override def loadAll(keys: Traversable[Any]): Map[Any, Any] = {
        // ignore request keys
        Map(extraKey -> extraValue)
      }
    }

    val cache = CacheBuilder.newBuilder().build(ignoringBulkLoader)
    val lookupKeys = List(new Object, new Object, new Object)
    cache.getAll(lookupKeys) match {
      case Failure(e: InvalidCacheLoadException) => // expected
      case _                                     => fail
    }

    assertSame(extraValue, cache.asMap().get(extraKey).get)
  }

  it should "fail if the loader throws an error" in {
    val e = new Error()
    val loader = (arg: Any) => throw e
    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    cache.get(new Object) match {
      case Failure(expected: ExecutionError) => assertSame(e, expected.getCause())
      case _                                 => fail
    }
    cache.stats should have(missCount(1), loadSuccessCount(0), loadExceptionCount(1), hitCount(0))

    try {
      cache.getUnchecked(new Object)
      fail()
    } catch {
      case expected: ExecutionError => assertSame(e, expected.getCause())
    }
    cache.stats should have(missCount(2), loadSuccessCount(0), loadExceptionCount(2), hitCount(0))

    cache.refresh(new Object)
    cache.stats should have(missCount(2), loadSuccessCount(0), loadExceptionCount(3), hitCount(0))

    val callableError = new Error()
    try {
      cache.getOrElseUpdate(key, () => throw callableError)
      fail()
    } catch {
      case expected: ExecutionError => assertSame(callableError, expected.getCause())
    }
    cache.stats should have(missCount(3), loadSuccessCount(0), loadExceptionCount(4), hitCount(0))

    cache.getAll(List(new Object)) match {
      case Failure(expected: ExecutionError) => assertSame(e, expected.getCause())
      case _                                 => fail
    }
    cache.stats should have(missCount(4), loadSuccessCount(0), loadExceptionCount(5), hitCount(0))
  }

  it should "handle execptions during reload" in {
    val one = new Object
    val e = new Error
    val loader = new CacheLoader[Any, Any]() {
      override def load(key: Any) = one
      override def reload(key: Any, oldValue: Any) = throw e
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    cache.refresh(key)
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(0))

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(1))
  }

  it should "handle execptions in the Future returned by reload" in {
    val one = new Object
    val e = new Error
    val loader = new CacheLoader[Any, Any]() {
      override def load(key: Any) = one
      override def reload(key: Any, oldValue: Any) = Future.failed(e)
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    cache.refresh(key)
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(0))

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(1))
  }

  it should "fail if the loader returns null" in {
    val cache = CacheBuilder.newBuilder()
      .recordStats()
      .build((any: Any) => null)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    cache.get(new Object) match {
      case Failure(e: InvalidCacheLoadException) => // expected
      case _                                     => fail()
    }

    cache.stats should have(missCount(1), loadSuccessCount(0), loadExceptionCount(1), hitCount(0))

    intercept[InvalidCacheLoadException] {
      cache.getUnchecked(new Object)
    }
    cache.stats should have(missCount(2), loadSuccessCount(0), loadExceptionCount(2), hitCount(0))

    cache.refresh(new Object())
    cache.stats should have(missCount(2), loadSuccessCount(0), loadExceptionCount(3), hitCount(0))

    intercept[InvalidCacheLoadException] {
      cache.getOrElseUpdate(new Object, () => null)
    }
    cache.stats should have(missCount(3), loadSuccessCount(0), loadExceptionCount(4), hitCount(0))

    cache.getAll(List(new Object)) match {
      case Failure(e: InvalidCacheLoadException) => // expected
      case _                                     => fail()
    }

    cache.stats should have(missCount(4), loadSuccessCount(0), loadExceptionCount(5), hitCount(0))
  }

  it should "ignore the reloaded value if reload returns null" in {
    val one = new Object
    val loader = new CacheLoader[Any, Any]() {
      override def load(key: Any) = one
      override def reload(key: Any, oldValue: Any) = null
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    cache.refresh(key)
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(0))

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(1))
  }

  it should "ignore the reloaded value if reload returns a Future with null" in {
    val one = new Object
    val loader = new CacheLoader[Any, Any]() {
      override def load(key: Any) = one
      override def reload(key: Any, oldValue: Any) = Future.successful(null)
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    cache.refresh(key);
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(0))

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(1))
  }

  it should "ignore the reloaded value if reload returns a Future with null (implicity by refreshAfterWrite)" in {
    val one = new Object
    val ticker = new FakeTicker()
    val loader = new CacheLoader[Any, Any]() {
      override def load(key: Any) = one
      override def reload(key: Any, oldValue: Any) = Future.successful(null)
    }

    val cache = CacheBuilder.newBuilder()
      .recordStats()
      .ticker(ticker.asScala)
      .refreshAfterWrite(1, MILLISECONDS)
      .build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    ticker.advance(1, MILLISECONDS)
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(1))

    ticker.advance(1, MILLISECONDS)
    assertSame(one, cache.getUnchecked(key))
    // refreshed
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(2))

    ticker.advance(1, MILLISECONDS)
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(2), hitCount(3))
  }

  it should "fail if CacheLoader#loadAll has a null value in the returned Map" in {
    val loader = new CacheLoader[Any, Any]() {
      override def load(key: Any) = fail
      override def loadAll(keys: Traversable[Any]) = keys.map { case key => (key, null) }.toMap
    }
    val cache = CacheBuilder.newBuilder()
      .recordStats()
      .build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    cache.getAll(List(new Object)) match {
      case Failure(e: InvalidCacheLoadException) => // expected
      case _                                     => fail
    }
    cache.stats should have(missCount(1), loadSuccessCount(0), loadExceptionCount(1), hitCount(0))
  }

  it should "fail if CacheLoader#loadAll returns null" in {
    val loader = new CacheLoader[Any, Any]() {
      override def load(key: Any) = fail
      override def loadAll(keys: Traversable[Any]) = null
    }
    val cache = CacheBuilder.newBuilder()
      .recordStats()
      .build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    cache.getAll(List(new Object)) match {
      case Failure(e: InvalidCacheLoadException) => // expected
      case _                                     => fail
    }
    cache.stats should have(missCount(1), loadSuccessCount(0), loadExceptionCount(1), hitCount(0))
  }

  it should "fail if the loader throws an Error" in {
    val error = new Error()
    val cache = CacheBuilder.newBuilder().recordStats().build((any: Any) => throw error)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    cache.get(new Object) match {
      case Failure(expected: ExecutionError) => assertSame(error, expected.getCause)
      case _                                 => fail
    }
    cache.stats should have(missCount(1), loadSuccessCount(0), loadExceptionCount(1), hitCount(0))

    try {
      cache.getUnchecked(new Object())
      fail
    } catch {
      case expected: ExecutionError => assertSame(error, expected.getCause)
      case _: Throwable             => fail
    }
    cache.stats should have(missCount(2), loadSuccessCount(0), loadExceptionCount(2), hitCount(0))

    cache.refresh(new Object())
    cache.stats should have(missCount(2), loadSuccessCount(0), loadExceptionCount(3), hitCount(0))

    val callableError = new Error
    try {
      cache.getOrElseUpdate(new Object, () => throw callableError)
      fail
    } catch {
      case expected: ExecutionError => assertSame(callableError, expected.getCause)
      case _: Throwable             => fail
    }
    cache.stats should have(missCount(3), loadSuccessCount(0), loadExceptionCount(4), hitCount(0))

    cache.getAll(List(new Object)) match {
      case Failure(expected: ExecutionError) => assertSame(error, expected.getCause)
      case _                                 => fail
    }
    cache.stats should have(missCount(4), loadSuccessCount(0), loadExceptionCount(5), hitCount(0))
  }

  it should "only log the error thrown in #reload" in {
    val one = new Object
    val e = new Error

    val loader = new CacheLoader[Any, Any]() {
      override def load(any: Any) = one
      override def reload(key: Any, oldValue: Any) = throw e
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    cache.refresh(key)
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(0))

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(1))
  }

  it should "only log the error returned as a failed Future by #reload" in {
    val one = new Object
    val e = new Error

    val loader = new CacheLoader[Any, Any]() {
      override def load(any: Any) = one
      override def reload(key: Any, oldValue: Any) = Future.failed(e)
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    cache.refresh(key)
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(0))

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(1))
  }

  it should "not replace value if an execption occures during #reload" in {
    val one = new Object
    val e = new Error
    val ticker = new FakeTicker()
    val loader = new CacheLoader[Any, Any]() {
      override def load(any: Any) = one
      override def reload(key: Any, oldValue: Any) = Future.failed(e)
    }

    val cache = CacheBuilder.newBuilder()
      .recordStats()
      .ticker(ticker.asScala)
      .refreshAfterWrite(1, MILLISECONDS)
      .build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(1))

    ticker.advance(1, MILLISECONDS)
    assertSame(one, cache.getUnchecked(key))
    // refreshed
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(2))

    ticker.advance(1, MILLISECONDS)
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(2), hitCount(3))
  }

  it should "return a failure from #getAll if loader#loadAll throws an error" in {
    val e = new Error()
    val loader = new CacheLoader[Any, Any]() {
      override def load(any: Any) = throw e
      override def loadAll(keys: Traversable[Any]) = throw e
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object
    cache.getAll(List(new Object)) match {
      case Failure(expected: ExecutionError) => assertSame(e, expected.getCause())
      case _                                 => fail
    }
    cache.stats should have(missCount(1), loadSuccessCount(0), loadExceptionCount(1), hitCount(0))
  }

  it should "throw an UncheckedExecutionException in #getUncheched if loader throws an Exception" in {
    val e = new Exception
    val loader = (any: Any) => throw e

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    cache.get(new Object) match {
      case Failure(expected: ExecutionException) => assertSame(e, expected.getCause())
      case _                                     => fail
    }
    cache.stats should have(missCount(1), loadSuccessCount(0), loadExceptionCount(1), hitCount(0))

    try {
      cache.getUnchecked(new Object())
      fail()
    } catch {
      case expected: UncheckedExecutionException => assertSame(e, expected.getCause())
      case _: Throwable                          => fail
    }
    cache.stats should have(missCount(2), loadSuccessCount(0), loadExceptionCount(2), hitCount(0))

    cache.refresh(new Object)
    cache.stats should have(missCount(2), loadSuccessCount(0), loadExceptionCount(3), hitCount(0))

    val callableException = new Exception()
    try {
      cache.getOrElseUpdate(new Object, () => throw callableException)
      fail()
    } catch {
      case expected: ExecutionException => assertSame(callableException, expected.getCause())
    }
    cache.stats should have(missCount(3), loadSuccessCount(0), loadExceptionCount(4), hitCount(0))

    cache.getAll(List(new Object)) match {
      case Failure(expected: ExecutionException) => assertSame(e, expected.getCause())
      case _                                     => fail
    }
    cache.stats should have(missCount(4), loadSuccessCount(0), loadExceptionCount(5), hitCount(0))
  }

  it should "not replace the value in the cache if Loader#reload throws an Exception" in {
    val one = new Object()
    val e = new Exception()
    val loader = new CacheLoader[Any, Any]() {
      override def load(any: Any) = one
      override def reload(key: Any, oldValue: Any) = throw e
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    cache.refresh(key)
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(0))

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(1))
  }

  it should "not replace the value in the cache if Loader#reload returns a failed Future" in {
    val one = new Object
    val e = new Exception
    val loader = new CacheLoader[Any, Any]() {
      override def load(any: Any) = one
      override def reload(key: Any, oldValue: Any) = Future.failed(e)
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    cache.refresh(key)
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(0))

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(1))
  }

  it should "not replace the value in the cache if Loader#reload returns a failed Future on refreshAfterWrite" in {
    val one = new Object
    val e = new Exception
    val ticker = new FakeTicker
    val loader = new CacheLoader[Any, Any]() {
      override def load(any: Any) = one
      override def reload(key: Any, oldValue: Any) = Future.failed(e)
    }

    val cache = CacheBuilder.newBuilder()
      .recordStats().ticker(ticker.asScala)
      .refreshAfterWrite(1, MILLISECONDS).build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    ticker.advance(1, MILLISECONDS)
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(1))

    ticker.advance(1, MILLISECONDS)
    assertSame(one, cache.getUnchecked(key))
    // refreshed
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(2))

    ticker.advance(1, MILLISECONDS)
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(2), hitCount(3))
  }

  it should "return Failure on bulk-load if loadAll throws a checked exception" in {
    val e = new Exception
    val loader = new CacheLoader[Any, Any]() {
      override def load(any: Any) = throw e
      override def loadAll(keys: Traversable[Any]) = throw e
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    cache.getAll(List(new Object)) match {
      case Failure(expected: ExecutionException) => assertSame(e, expected.getCause())
      case _                                     => fail
    }
    cache.stats should have(missCount(1), loadSuccessCount(0), loadExceptionCount(1), hitCount(0))
  }

  it should "return Failure if #load throws an unchecked exception" in {
    val e = new RuntimeException()
    val cache = CacheBuilder.newBuilder().recordStats().build((any: Any) => throw e)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    cache.get(new Object) match {
      case Failure(expected: UncheckedExecutionException) => assertSame(e, expected.getCause())
      case _ => fail
    }
    cache.stats should have(missCount(1), loadSuccessCount(0), loadExceptionCount(1), hitCount(0))

    try {
      cache.getUnchecked(new Object)
      fail()
    } catch {
      case expected: UncheckedExecutionException => assertSame(e, expected.getCause)
    }
    cache.stats should have(missCount(2), loadSuccessCount(0), loadExceptionCount(2), hitCount(0))

    cache.refresh(new Object)
    cache.stats should have(missCount(2), loadSuccessCount(0), loadExceptionCount(3), hitCount(0))

    val callableException = new RuntimeException();
    try {
      cache.getOrElseUpdate(new Object, () => throw callableException)
      fail()
    } catch {
      case expected: UncheckedExecutionException => assertSame(callableException, expected.getCause)
    }
    cache.stats should have(missCount(3), loadSuccessCount(0), loadExceptionCount(4), hitCount(0))

    cache.getAll(List(new Object)) match {
      case Failure(expected: UncheckedExecutionException) => assertSame(e, expected.getCause())
      case _ => fail
    }
    cache.stats should have(missCount(4), loadSuccessCount(0), loadExceptionCount(5), hitCount(0))
  }

  it should "not fail if reload throws an unchecked exception" in {
    val one = new Object
    val e = new RuntimeException
    val loader = new CacheLoader[Any, Any]() {
      override def load(any: Any) = one
      override def reload(key: Any, oldValue: Any) = throw e
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    cache.refresh(key)
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(0))

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(1))
  }

  it should "not fail on refresh if reload returns a Future with an unchecked exception" in {
    val one = new Object
    val e = new RuntimeException
    val loader = new CacheLoader[Any, Any]() {
      override def load(any: Any) = one
      override def reload(key: Any, oldValue: Any) = Future.failed(e)
    }

    val cache = CacheBuilder.newBuilder().recordStats().build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    cache.refresh(key)
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(0))

    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(1))
  }

  it should "not fail on reload if reload returns a Future with an unchecked exception" in {
    val one = new Object
    val e = new RuntimeException
    val loader = new CacheLoader[Any, Any]() {
      override def load(any: Any) = one
      override def reload(key: Any, oldValue: Any) = Future.failed(e)
    }
    val ticker = new FakeTicker

    val cache = CacheBuilder.newBuilder()
      .recordStats().ticker(ticker.asScala)
      .refreshAfterWrite(1, MILLISECONDS).build(loader)
    cache.stats should be(CacheStats(0, 0, 0, 0, 0, 0))

    val key = new Object
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(0))

    ticker.advance(1, MILLISECONDS)
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(0), hitCount(1))

    ticker.advance(1, MILLISECONDS)
    assertSame(one, cache.getUnchecked(key))
    // refreshed
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(1), hitCount(2))

    ticker.advance(1, MILLISECONDS)
    assertSame(one, cache.getUnchecked(key))
    cache.stats should have(missCount(1), loadSuccessCount(1), loadExceptionCount(2), hitCount(3))
  }

  it should "not notify removal listener if reload fails" in {
    val count = new AtomicInteger
    val e = new IllegalStateException("exception to trigger failure on first load()")
    val failOnceFunction = new CacheLoader[Int, String]() {
      override def load(key: Int): String = {
        if (count.getAndIncrement() == 0) {
          throw e
        }
        return key.toString
      }
    }
    val removalListener = new CountingRemovalListener[Int, String]
    val cache = CacheBuilder.newBuilder().removalListener(removalListener).build(failOnceFunction)

    try {
      cache.getUnchecked(1)
      fail()
    } catch {
      case ue: UncheckedExecutionException => assertSame(e, ue.getCause())
    }

    cache.getUnchecked(1) should be("1")
    removalListener.getCount should be(0)

    count.set(0)
    cache.refresh(2)

    cache.getUnchecked(2) should be("2")
    removalListener.getCount should be(0)
  }

  it should "reaload after value reclamation" in {
    val countingLoader = new CountingLoader
    val cache = CacheBuilder.newBuilder()
      .weakValues().build(countingLoader)
    val map = cache.asMap()

    val iterations = 10
    var ref = new WeakReference[AnyRef](null)
    var expectedComputations = 0
    for (i <- 0 until iterations) {
      // The entry should get garbage collected and recomputed.
      var oldValue = ref.get
      if (oldValue == None) {
        expectedComputations = expectedComputations + 1
      }
      ref = new WeakReference[AnyRef](cache.getUnchecked(1).asInstanceOf[AnyRef])
      oldValue = None
      Thread.sleep(i)
      System.gc()
    }
    assertEquals(expectedComputations, countingLoader.getCount())

    for (i <- 0 until iterations) {
      // The entry should get garbage collected and recomputed.
      var oldValue = ref.get
      if (oldValue == None) {
        expectedComputations = expectedComputations + 1
      }
      cache.refresh(1)
      ref = new WeakReference[AnyRef](map.get(1).get.asInstanceOf[AnyRef])
      oldValue = None
      Thread.sleep(i)
      System.gc()
    }
    assertEquals(expectedComputations, countingLoader.getCount())
  }

  it should "be able to load concurrently" in {
    testConcurrentLoading(CacheBuilder.newBuilder())
  }

  it should "be able to load concurrently with expiration" in {
    testConcurrentLoading(CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS))
  }

  private def testConcurrentLoading(builder: CacheBuilder[Any, Any]) = {
    testConcurrentLoadingDefault(builder)
    testConcurrentLoadingNull(builder)
    testConcurrentLoadingUncheckedException(builder)
    testConcurrentLoadingCheckedException(builder)
  }

  /**
   * On a successful concurrent computation, only one thread does the work,
   *  but all the threads get the same result.
   */
  private def testConcurrentLoadingDefault(builder: CacheBuilder[Any, Any]) = {
    val count = 10
    val callCount = new AtomicInteger
    val startSignal = new CountDownLatch(count + 1)
    val result = new Object

    val cache = builder.build(new CacheLoader[String, AnyRef]() {
      override def load(key: String): AnyRef = {
        callCount.incrementAndGet()
        startSignal.await()
        result
      }
    })

    val resultArray = doConcurrentGet(cache, "bar", count, startSignal)
    callCount.get() should be(1)

    for (i <- 0 until count) {
      assertSame("result(" + i + ") didn't match expected", result, resultArray(i))
    }
  }

  /**
   * On a concurrent computation that returns null, all threads should get an
   *  InvalidCacheLoadException, with the loader only called once. The result
   *  should not be cached (a later request should call the loader again).
   */
  private def testConcurrentLoadingNull(builder: CacheBuilder[Any, Any]) = {
    val count = 10
    val callCount = new AtomicInteger
    val startSignal = new CountDownLatch(count + 1)

    val cache = builder.build(new CacheLoader[String, AnyRef]() {
      override def load(key: String): AnyRef = {
        callCount.incrementAndGet()
        startSignal.await()
        null
      }
    })

    val result = doConcurrentGet(cache, "bar", count, startSignal)

    callCount.get() should be(1)

    for (i <- 0 until count) {
      assertTrue(result(i).isInstanceOf[InvalidCacheLoadException])
    }

    // subsequent calls should call the loader again, not get the old
    // exception
    try {
      cache.getUnchecked("bar")
      fail()
    } catch {
      case expected: InvalidCacheLoadException =>
    }
    callCount.get() should be(2)
  }

  /**
   * On a concurrent computation that throws an unchecked exception, all
   *  threads should get the (wrapped) exception, with the loader called only
   *  once. The result should not be cached (a later request should call the
   *  loader again).
   */
  def testConcurrentLoadingUncheckedException(builder: CacheBuilder[Any, Any]) = {
    val count = 10
    val callCount = new AtomicInteger
    val startSignal = new CountDownLatch(count + 1)
    val e = new RuntimeException()

    val cache = builder.build(new CacheLoader[String, AnyRef]() {
      override def load(key: String): AnyRef = {
        callCount.incrementAndGet()
        startSignal.await()
        throw e
      }
    })

    val result = doConcurrentGet(cache, "bar", count, startSignal)

    callCount.get() should be(1)

    for (i <- 0 until count) {
      // doConcurrentGet alternates between calling getUnchecked and
      // calling get, but an unchecked
      // exception thrown by the loader is always wrapped as an
      // UncheckedExecutionException.
      result(i) match {
        case _: UncheckedExecutionException => // expected
        case _                              => fail
      }
      assertSame(e, result(i).asInstanceOf[UncheckedExecutionException].getCause)
    }

    // subsequent calls should call the loader again, not get the old
    // exception
    try {
      cache.getUnchecked("bar")
      fail()
    } catch {
      case expected: UncheckedExecutionException => // expected
      case _: Throwable                          => fail
    }
    callCount.get() should be(2)
  }

  /**
   * On a concurrent computation that throws a checked exception, all threads
   *  should get the (wrapped) exception, with the loader called only once. The
   *  result should not be cached (a later request should call the loader
   *  again).
   */
  private def testConcurrentLoadingCheckedException(builder: CacheBuilder[Any, Any]) = {
    val count = 10
    val callCount = new AtomicInteger
    val startSignal = new CountDownLatch(count + 1)
    val e = new IOException

    val cache = builder.build(new CacheLoader[String, AnyRef]() {
      override def load(key: String): AnyRef = {
        callCount.incrementAndGet()
        startSignal.await()
        throw e
      }
    })

    val result = doConcurrentGet(cache, "bar", count, startSignal)

    callCount.get() should be(1)

    for (i <- 0 until count) {
      // doConcurrentGet alternates between calling getUnchecked and
      // calling get. If we call get(),
      // we should get an ExecutionException; if we call getUnchecked(),
      // we should get an
      // UncheckedExecutionException.
      val mod = i % 3
      if (mod == 0 || mod == 2) {
        result(i) match {
          case _: ExecutionException => // expected
          case _                     => fail
        }
        assertSame(e, result(i).asInstanceOf[ExecutionException].getCause)

      } else {
        result(i) match {
          case _: UncheckedExecutionException => // expected
          case _                              => fail
        }
        assertSame(e, result(i).asInstanceOf[UncheckedExecutionException].getCause)
      }
    }

    // subsequent calls should call the loader again, not get the old
    // exception
    try {
      cache.getUnchecked("bar")
      fail
    } catch {
      case expected: UncheckedExecutionException =>
      case _: Throwable                          => fail
    }
    callCount.get() should be(2)
  }

  /**
   * Test-helper method that performs {@code nThreads} concurrent calls to
   *  {@code cache.get(key)} or {@code cache.getUnchecked(key)}, and returns a
   *  List containing each of the results. The result for any given call to
   *  {@code cache.get} or {@code cache.getUnchecked} is the value returned, or
   *  the exception thrown.
   *
   *  <p>
   *  As we iterate from {@code 0} to {@code nThreads}, threads with an even
   *  index will call {@code getUnchecked}, and threads with an odd index will
   *  call {@code get}. If the cache throws exceptions, this difference may be
   *  visible in the returned List.
   */
  private def doConcurrentGet[K](cache: LoadingCache[K, AnyRef], key: K, nThreads: Int, gettersStartedSignal: CountDownLatch): List[AnyRef] = {
    val result = new AtomicReferenceArray[AnyRef](nThreads)
    val gettersComplete = new CountDownLatch(nThreads)
    for (i <- 0 until nThreads) {
      val index = i
      val thread = new Thread(new Runnable() {
        override def run() = {
          gettersStartedSignal.countDown()
          var value: AnyRef = null
          try {
            val mod = index % 3
            if (mod == 0) {
              value = cache.get(key).get
            } else if (mod == 1) {
              value = cache.getUnchecked(key)
            } else {
              cache.refresh(key)
              value = cache.get(key).get
            }
            result.set(index, value);
          } catch {
            case t: Throwable => result.set(index, t)
          }
          gettersComplete.countDown()
        }
      })
      thread.start()
      // we want to wait until each thread is WAITING - one thread waiting
      // inside CacheLoader.load
      // (in startSignal.await()), and the others waiting for that
      // thread's result.
      while (thread.isAlive() && thread.getState() != Thread.State.WAITING) {
        Thread.`yield`
      }
    }
    gettersStartedSignal.countDown()
    gettersComplete.await()

    var resultList = mutable.MutableList[AnyRef]()
    for (i <- (0 until nThreads)) {
      resultList = resultList ++ mutable.MutableList(result.get(i))
    }
    return List.empty ++ resultList
  }

  it should "be possible to view #asMap during loading" in {
    val getStartedSignal = new CountDownLatch(2)
    val letGetFinishSignal = new CountDownLatch(1)
    val getFinishedSignal = new CountDownLatch(2)
    val getKey = "get"
    val refreshKey = "refresh"
    val suffix = "Suffix"

    val computeFunction = new CacheLoader[String, String]() {
      override def load(key: String): String = {
        getStartedSignal.countDown()
        letGetFinishSignal.await()
        key + suffix
      }
    }

    val cache = CacheBuilder.newBuilder().build(computeFunction)
    val map = cache.asMap()
    map.put(refreshKey, refreshKey)
    map.size should be(1)
    map.keySet.contains(getKey) should be(false)
    assertSame(refreshKey, map.get(refreshKey).get)

    new Thread() {
      override def run {
        cache.getUnchecked(getKey)
        getFinishedSignal.countDown
      }
    }.start

    new Thread() {
      override def run {
        cache.refresh(refreshKey)
        getFinishedSignal.countDown
      }
    }.start

    getStartedSignal.await

    // computation is in progress; asMap shouldn't have changed
    map.size should be(1)
    map.keySet.contains(getKey) should be(false)
    assertSame(refreshKey, map.get(refreshKey).get)

    // let computation complete
    letGetFinishSignal.countDown
    getFinishedSignal.await

    // asMap view should have been updated
    cache.size should be(2)
    map.get(getKey) should be(Some(getKey + suffix))
    map.get(refreshKey) should be(Some(refreshKey + suffix))
  }

  it should "be able to invalidate during loading" in {
    // computation starts; invalidate() is called on the key being computed,
    // computation finishes
    val computationStarted = new CountDownLatch(2)
    val letGetFinishSignal = new CountDownLatch(1)
    val getFinishedSignal = new CountDownLatch(2)
    val getKey = "get"
    val refreshKey = "refresh"
    val suffix = "Suffix"

    val computeFunction = (key: String) => {
      computationStarted.countDown()
      letGetFinishSignal.await()
      key + suffix
    }

    val cache = CacheBuilder.newBuilder().build(computeFunction)
    val map = cache.asMap()
    map.put(refreshKey, refreshKey)

    new Thread() {
      override def run() = {
        cache.getUnchecked(getKey)
        getFinishedSignal.countDown()
      }
    }.start

    new Thread() {
      override def run() = {
        cache.refresh(refreshKey)
        getFinishedSignal.countDown()
      }
    }.start

    computationStarted.await()
    cache.invalidate(getKey)
    cache.invalidate(refreshKey)
    map.keySet.contains(getKey) should be(false)
    map.keySet.contains(refreshKey) should be(false)

    // let computation complete
    letGetFinishSignal.countDown()
    getFinishedSignal.await()

    // results should be visible
    cache.size should be(2)
    map.get(getKey).get should be(getKey + suffix)
    map.get(refreshKey).get should be(refreshKey + suffix)
    cache.size should be(2)
  }

  it should "be able to invalidate and reload during loading" in {
    // computation starts; clear() is called, computation finishes
    val computationStarted = new CountDownLatch(2)
    val letGetFinishSignal = new CountDownLatch(1)
    val getFinishedSignal = new CountDownLatch(4)
    val getKey = "get"
    val refreshKey = "refresh"
    val suffix = "Suffix"

    val computeFunction = (key: String) => {
      computationStarted.countDown()
      letGetFinishSignal.await()
      key + suffix
    }

    val cache = CacheBuilder.newBuilder().build(computeFunction)
    val map = cache.asMap()
    map.put(refreshKey, refreshKey)

    new Thread() {
      override def run() = {
        cache.getUnchecked(getKey)
        getFinishedSignal.countDown()
      }
    }.start

    new Thread() {
      override def run() = {
        cache.refresh(refreshKey)
        getFinishedSignal.countDown()
      }
    }.start

    computationStarted.await()
    cache.invalidate(getKey)
    cache.invalidate(refreshKey)
    map.keySet.contains(getKey) should be(false)
    map.keySet.contains(refreshKey) should be(false)

    // start new computations
    new Thread() {
      override def run() = {
        cache.getUnchecked(getKey)
        getFinishedSignal.countDown()
      }
    }.start

    new Thread() {
      override def run() = {
        cache.refresh(refreshKey)
        getFinishedSignal.countDown()
      }
    }.start

    // let computation complete
    letGetFinishSignal.countDown()
    getFinishedSignal.await()

    // results should be visible
    cache.size should be(2)
    map.get(getKey).get should be(getKey + suffix)
    map.get(refreshKey).get should be(refreshKey + suffix)
  }
}
