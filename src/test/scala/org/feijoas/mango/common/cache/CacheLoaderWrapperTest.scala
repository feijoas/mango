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

import java.util.concurrent.TimeUnit
import scala.collection.Traversable
import scala.concurrent.future
import org.feijoas.mango.common.cache.CacheLoader._
import org.scalatest.{ FlatSpec, ShouldMatchers }
import com.google.common.cache.{ CacheLoader => GuavaCacheLoader }
import com.google.common.collect.{ ImmutableMap, Lists }

/** Tests for [[CacheLoaderWrapper]]
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
class CacheLoaderWrapperTest extends FlatSpec with ShouldMatchers {

  behavior of "CacheLoaderWrapper"

  def fixture = new {
    val cacheLoader = new CountingCacheLoader
    val wrapper: GuavaCacheLoader[Int, Int] = cacheLoader.asJava
  }

  it should "forward calls to load" in {
    val f = fixture
    import f._

    wrapper.load(5) should be(25)
    cacheLoader should be(CountingCacheLoader(1, 0, 0))
  }

  it should "forward calls to reload" in {
    val f = fixture
    import f._

    wrapper.reload(5, 25).get(100, TimeUnit.MILLISECONDS) should be(25)
    cacheLoader should be(CountingCacheLoader(0, 1, 0))
  }

  it should "forward calls to loadAll" in {
    val f = fixture
    import f._

    wrapper.loadAll(Lists.newArrayList(1, 2, 3)) should be(ImmutableMap.of(1, 1, 2, 4, 3, 9))
    cacheLoader should be(CountingCacheLoader(0, 0, 1))
  }

}

/** We need this helper until there is mocking support for Scala
 */
private[mango] case class CountingCacheLoader(var loadCnt: Int = 0, var reloadCnt: Int = 0, var loadAllCnt: Int = 0)
    extends CacheLoader[Int, Int] {
  import scala.concurrent.ExecutionContext.Implicits.global

  override def load(key: Int) = synchronized {
    loadCnt = loadCnt + 1
    key * key
  }
  override def reload(key: Int, oldValue: Int) = {
    reloadCnt = reloadCnt + 1
    future { oldValue }
  }
  override def loadAll(keys: Traversable[Int]) = {
    loadAllCnt = loadAllCnt + 1
    keys.map { (key: Int) => (key, key * key) }.toMap
  }
}