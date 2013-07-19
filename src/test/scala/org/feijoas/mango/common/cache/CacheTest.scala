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

import scala.annotation.meta.{ beanGetter, beanSetter, field, getter, setter }
import scala.collection.concurrent
import scala.collection.concurrent.TrieMap

import org.feijoas.mango.common.annotations.Beta
import org.scalatest.{ FlatSpec, GivenWhenThen, MustMatchers }
import org.scalatest.mock.MockitoSugar

/** Tests for [[Cache]]
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
class CacheTest extends FlatSpec with MustMatchers with GivenWhenThen with MockitoSugar {

  def fixture = {
    val cache = new MapCache[String, Int]()
    (cache.asMap, cache)
  }

  behavior of "the default implementations of Cache"

  "getAllPresent(keys)" must "return all (key,value)-pairs in the cache" in {
    val (map, cache) = fixture
    map.put("a", 1)
    map.put("c", 3)
    cache.getAllPresent(List("a", "b", "c")) must be(Map("a" -> 1, "c" -> 3))
  }

  "putAll(keys)" must "put all key in the map" in {
    val (map, cache) = fixture
    cache.putAll(Map("a" -> 1, "c" -> 3))
    map must be(Map("a" -> 1, "c" -> 3))
  }

  "invalidateAll(keys)" must "call invalidate for each key" in {
    val (map, cache) = fixture
    map.put("a", 1)
    map.put("b", 2)
    map.put("c", 3)
    map must be(Map("a" -> 1, "b" -> 2, "c" -> 3))
    cache.invalidateAll(List("a", "b", "d"))
    map must be(Map("c" -> 3))
  }
}

/** A cache implemented with a map
 */
protected[mango] class MapCache[K, V]() extends Cache[K, V] {
  val map = TrieMap[K, V]()
  def getIfPresent(key: K): Option[V] = map.get(key)
  def getOrElseUpdate(key: K, loader: () => V): V = map.getOrElseUpdate(key, loader())
  def put(key: K, value: V): Unit = map.put(key, value)
  def invalidate(key: K): Unit = map.remove(key)
  def invalidateAll(): Unit = map.clear
  def size(): Long = map.size
  def stats(): CacheStats = throw new NotImplementedError()
  def asMap(): concurrent.Map[K, V] = map
}