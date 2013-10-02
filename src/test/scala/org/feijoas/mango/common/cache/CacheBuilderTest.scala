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

import java.util.concurrent.TimeUnit.NANOSECONDS
import java.util.concurrent.TimeUnit.SECONDS

import scala.actors.threadpool.AtomicInteger
import scala.annotation.meta.beanGetter
import scala.annotation.meta.beanSetter
import scala.annotation.meta.field
import scala.annotation.meta.getter
import scala.annotation.meta.setter
import scala.reflect.runtime.universe

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.base.Ticker
import org.scalatest.FlatSpec
import org.scalatest.Matchers.be
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.Matchers.not
import org.scalatest.PrivateMethodTester

import com.google.common.cache.{CacheBuilder => GuavaCacheBuilder}

/** Tests for [[CacheBuilder]]
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from guava-libraries)
 */
class CacheBuilderTest extends FlatSpec with PrivateMethodTester {

  behavior of "CacheBuilder"

  it should "create a new LoadingCache if build is called with a loader" in {
    val loader = (any: String) => 1
    val cache: LoadingCache[String, Int] = CacheBuilder.newBuilder()
      .removalListener(CountingRemovalListener())
      .build(loader)

    cache.getUnchecked("one") should be(1)
    cache.size should be(1)
  }

  it should "be contravariant in both types" in {
    // compiler must not complain    
    val superBuilder: CacheBuilder[Any, Any] = CacheBuilder.newBuilder
    val subBuilder: CacheBuilder[String, Int] = superBuilder
    val guavaBuilder: GuavaCacheBuilder[String, Int] = CacheBuilder.createGuavaBuilder(subBuilder)
  }

  it should "be able to create a 'Null'-cache" in {
    val listener = CountingRemovalListener[Any, Any]()
    val cache = CacheBuilder.newBuilder()
      .maximumSize(0)
      .removalListener(listener)
      .build((any: Any) => any)

    cache.size should be(0)

    val key = new Object()
    cache.getUnchecked(key) should be(key)
    listener.count.get() should be(1)
    cache.size should be(0)
    cache.asMap should be(Map())
  }

  /*
    CountingRemovalListener<Object, Object> listener = countingRemovalListener();
    LoadingCache<Object, Object> nullCache = new CacheBuilder<Object, Object>()
        .maximumSize(0)
        .removalListener(listener)
        .build(identityLoader());
    assertEquals(0, nullCache.size());
    Object key = new Object();
    assertSame(key, nullCache.getUnchecked(key));
    assertEquals(1, listener.getCount());
    assertEquals(0, nullCache.size());
    CacheTesting.checkEmpty(nullCache.asMap());
  }  */

  behavior of "CacheBuilder#initialCapacity"

  it should "throw a IllegalArgumentException if negative" in {
    val builder = CacheBuilder()
    intercept[IllegalArgumentException] {
      builder.initialCapacity(-1)
    }
  }

  it should "throw a IllegalStateException if called twice" in {
    val builder = CacheBuilder().initialCapacity(16)
    intercept[IllegalStateException] {
      builder.initialCapacity(16)
    }
  }

  it should "accept the smallest possible value" in {
    CacheBuilder().initialCapacity(0).build((any: String) => 0)
  }

  it should "accept the largest possible value" in {
    val builder = CacheBuilder().initialCapacity(Integer.MAX_VALUE)
    CacheBuilder.createGuavaBuilder(builder)
    // must not blow up
  }

  behavior of "CacheBuilder#concurrencyLevel"

  it should "throw a IllegalArgumentException if zero" in {
    val builder = CacheBuilder()
    intercept[IllegalArgumentException] {
      builder.concurrencyLevel(0)
    }
  }

  it should "throw a IllegalStateException if called twice" in {
    val builder = CacheBuilder().concurrencyLevel(16)
    intercept[IllegalStateException] {
      builder.concurrencyLevel(16)
    }
  }

  it should "accept the smallest possible value" in {
    CacheBuilder().concurrencyLevel(1).build((any: String) => 0)
  }

  it should "accept the largest possible value" in {
    val builder = CacheBuilder().concurrencyLevel(Integer.MAX_VALUE)
    CacheBuilder.createGuavaBuilder(builder)
    // must not blow up
  }

  behavior of "CacheBuilder#maximumSize"

  it should "throw a IllegalArgumentException if negative" in {
    val builder = CacheBuilder()
    intercept[IllegalArgumentException] {
      builder.maximumSize(-1)
    }
  }

  it should "throw a IllegalStateException if called twice" in {
    val builder = CacheBuilder().maximumSize(16)
    intercept[IllegalStateException] {
      builder.maximumSize(16)
    }
  }

  it should "throw a IllegalStateException if called together with #maximumWeight" in {
    val builder = CacheBuilder().maximumSize(16)
    intercept[IllegalStateException] {
      builder.maximumWeight(16)
    }
  }

  behavior of "CacheBuilder#maximumWeight"

  it should "throw a IllegalArgumentException if negative" in {
    val builder = CacheBuilder()
    intercept[IllegalArgumentException] {
      builder.maximumWeight(-1)
    }
  }

  it should "throw a IllegalStateException if called twice" in {
    val builder = CacheBuilder().maximumWeight(16)
    intercept[IllegalStateException] {
      builder.maximumWeight(16)
    }
  }

  it should "throw a IllegalStateException if called without #weigher" in {
    val builder = CacheBuilder().maximumWeight(1)
    intercept[IllegalStateException] {
      builder.build((any: String) => 0)
    }
  }

  behavior of "CacheBuilder#weigher"

  it should "throw a IllegalStateException if called without #maximumWeight" in {
    val builder = CacheBuilder().weigher((a: Any, b: Any) => 42)
    intercept[IllegalStateException] {
      builder.build((any: String) => 0)
    }
  }

  it should "throw a IllegalStateException if called without #maximumSize" in {
    intercept[IllegalStateException] {
      CacheBuilder().weigher((a: Any, b: Any) => 42).maximumSize(1)
    }
    intercept[IllegalStateException] {
      CacheBuilder().maximumSize(1).weigher((a: Any, b: Any) => 42)
    }
  }

  behavior of "CacheBuilder#weakKeys"

  it should "throw a IllegalStateException if called twice" in {
    val builder = CacheBuilder().weakKeys
    intercept[IllegalStateException] {
      builder.weakKeys
    }
  }

  behavior of "CacheBuilder#weakValues"

  it should "throw a IllegalStateException if called twice" in {
    val builder1 = CacheBuilder().weakValues
    intercept[IllegalStateException] {
      builder1.weakValues
    }
    intercept[IllegalStateException] {
      builder1.softValues
    }
    val builder2 = CacheBuilder().softValues
    intercept[IllegalStateException] {
      builder1.weakValues
    }
    intercept[IllegalStateException] {
      builder1.softValues
    }
  }

  behavior of "CacheBuilder#expireAfterWrite"

  it should "throw a IllegalArgumentException if negative" in {
    val builder = CacheBuilder()
    intercept[IllegalArgumentException] {
      builder.expireAfterWrite(-1, SECONDS)
    }
  }

  it should "throw a IllegalStateException if called twice" in {
    val builder = CacheBuilder().expireAfterWrite(100, SECONDS)
    intercept[IllegalStateException] {
      builder.expireAfterWrite(100, SECONDS)
    }
  }

  it should "accept the smallest possible value" in {
    CacheBuilder().expireAfterWrite(1, NANOSECONDS).build((any: String) => 0)
    // must not blow up
  }

  behavior of "CacheBuilder#expireAfterAccess"

  it should "throw a IllegalArgumentException if negative" in {
    val builder = CacheBuilder()
    intercept[IllegalArgumentException] {
      builder.expireAfterAccess(-1, SECONDS)
    }
  }

  it should "throw a IllegalStateException if called twice" in {
    val builder = CacheBuilder().expireAfterAccess(100, SECONDS)
    intercept[IllegalStateException] {
      builder.expireAfterAccess(100, SECONDS)
    }
  }

  it should "accept the smallest possible value" in {
    CacheBuilder().expireAfterAccess(1, NANOSECONDS).build((any: String) => 0)
    // must not blow up
  }

  it should "be possible to call it with #expireAfterWrite" in {
    CacheBuilder().expireAfterWrite(1, NANOSECONDS)
      .expireAfterAccess(1, NANOSECONDS)
      .build((any: String) => 0)
    // must not blow up
  }

  behavior of "CacheBuilder#refreshAfterWrite"

  it should "throw a IllegalArgumentException if zero" in {
    val builder = CacheBuilder()
    intercept[IllegalArgumentException] {
      builder.refreshAfterWrite(0, SECONDS)
    }
  }

  it should "throw a IllegalStateException if called twice" in {
    val builder = CacheBuilder().refreshAfterWrite(100, SECONDS)
    intercept[IllegalStateException] {
      builder.refreshAfterWrite(100, SECONDS)
    }
  }

  behavior of "CacheBuilder#ticker"

  it should "throw a IllegalStateException if called twice" in {
    val ticker = Ticker.systemTicker()
    val builder = CacheBuilder().ticker(ticker)
    intercept[IllegalStateException] {
      builder.ticker(ticker)
    }
  }

  behavior of "CacheBuilder#removalListener"

  it should "throw a IllegalStateException if called twice" in {
    val listener = (any: RemovalNotification[Any, Any]) => {}
    val builder = CacheBuilder().removalListener(listener)
    intercept[IllegalStateException] {
      builder.removalListener(listener)
    }
  }

  // check that all method calls are properly forwarded
  // unfortunately we cannot mock Guava CacheBuilder
  import scala.reflect.runtime.{ universe => ru }

  behavior of "CacheBuilder#createGuavaBuilder"

  def getMember[T](builder: GuavaCacheBuilder[_, _], name: String): T = {
    val m = ru.runtimeMirror(builder.getClass.getClassLoader)
    val im = m.reflect(builder)
    val symb = ru.typeOf[GuavaCacheBuilder[_, _]].declaration(ru.newTermName(name)).asTerm
    im.reflectField(symb).get.asInstanceOf[T]
  }

  def testHasValue[T](method: String, value: T, cacheBuilder: CacheBuilder[Any, Any]) {
    val initBuilder = CacheBuilder.createGuavaBuilder(CacheBuilder())
    getMember[T](initBuilder, method) should not be value
    val builder = CacheBuilder.createGuavaBuilder(cacheBuilder)
    getMember[T](builder, method) should be(value)
  }

  def testNotNull[T](method: String, cacheBuilder: CacheBuilder[Any, Any]) {
    val initBuilder = CacheBuilder.createGuavaBuilder(CacheBuilder())
    assert(getMember[T](initBuilder, method) == null)
    val builder = CacheBuilder.createGuavaBuilder(cacheBuilder)
    assert(getMember[T](builder, method) != null)
  }

  it should "forward the call on maximumSize" in {
    testHasValue("maximumSize", 7, CacheBuilder().maximumSize(7))
  }

  it should "forward the call on concurrencyLevel" in {
    testHasValue("concurrencyLevel", 7, CacheBuilder().concurrencyLevel(7))
  }

  it should "forward the call on expireAfterAccess" in {
    testHasValue("expireAfterAccessNanos", 7, CacheBuilder().expireAfterAccess(7, NANOSECONDS))
  }

  it should "forward the call on expireAfterWrite" in {
    testHasValue("expireAfterWriteNanos", 7, CacheBuilder().expireAfterWrite(7, NANOSECONDS))
  }

  it should "forward the call on initialCapacity" in {
    testHasValue("initialCapacity", 7, CacheBuilder().initialCapacity(7))
  }

  it should "forward the call on maximumWeight" in {
    testHasValue("maximumWeight", 7, CacheBuilder().maximumWeight(7))
  }

  it should "forward the call on recordStats" in {
    val cache = CacheBuilder().recordStats().build((any: Any) => any)
    cache.get("foo")
    cache.stats match {
      case CacheStats(0, 1, 1, 0, _, 0) => // OK
      case actual @ _                   => fail(actual.toString)
    }
  }

  it should "forward the call on refreshAfterWrite" in {
    testHasValue("refreshNanos", 7, CacheBuilder().refreshAfterWrite(7, NANOSECONDS))
  }

  it should "forward the call on removalListener" in {
    val listener = (any: RemovalNotification[Any, Any]) => {}
    testNotNull("removalListener", CacheBuilder().removalListener(listener))
  }

  it should "forward the call on softValues" in {
    testNotNull("valueStrength", CacheBuilder().softValues)
  }

  it should "forward the call on ticker" in {
    val ticker = Ticker.systemTicker()
    testNotNull("ticker", CacheBuilder().ticker(ticker))
  }

  it should "forward the call on weakKeys" in {
    testNotNull("keyStrength", CacheBuilder().weakKeys)
  }

  it should "forward the call on weakValues" in {
    testNotNull("valueStrength", CacheBuilder().weakValues)
  }

  it should "forward the call on weigher" in {
    val weigher = (a: Any, b: Any) => 0
    testNotNull("weigher", CacheBuilder().weigher(weigher))
  }
}

/** {@link RemovalListener} that counts each {@link RemovalNotification} it receives, and provides
 *  access to the most-recently received one.
 */
private case class CountingRemovalListener[K, V](val count: AtomicInteger = new AtomicInteger)
  extends (RemovalNotification[K, V] => Unit) {
  @volatile var lastNotification: RemovalNotification[K, V] = null

  override def apply(notification: RemovalNotification[K, V]): Unit = onRemoval(notification)

  def onRemoval(notification: RemovalNotification[K, V]): Unit = {
    count.incrementAndGet()
    lastNotification = notification
  }

  def lastEvictedKey = lastNotification.key
  def lastEvictedValue = lastNotification.value
  def getCount = count.get
}

/** Returns a {@code new Object()} for every request, and increments a counter for every request.
 *  The count is accessible via {@link #getCount}.
 */
private case class CountingLoader() extends CacheLoader[Any, Any] {
  val count = new AtomicInteger
  override def load(from: Any) = {
    count.incrementAndGet
    new Object
  }

  def getCount() = count.get()
}