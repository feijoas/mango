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

import scala.Option.option2Iterable
import scala.annotation.meta.{ beanGetter, beanSetter, field, getter, setter }
import scala.collection.{ concurrent, immutable }

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.convert.AsScala

import com.google.common.cache.{ Cache => GuavaCache }

/** A semi-persistent mapping from keys to values. Cache entries are manually added using
 *  `getOrElseUpdate(key, loader)` or `put(key, value)`, and are stored in the cache until
 *  either evicted or manually invalidated.
 *
 *  Implementations of this interface are expected to be thread-safe, and can be safely accessed
 *  by multiple concurrent threads.
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from Guava-libraries)
 */
@Beta
trait Cache[K, V] {

  /** Returns an [[http://www.scala-lang.org/api/current/index.html#scala.Option Option]] which is `Some(value)` with the value associated with
   *  `key` in this cache, or `None` if there is no cached value for `key`.
   */
  def getIfPresent(key: K): Option[V]

  /** Returns the value associated with `key` in this cache, obtaining that value from
   *  `loader` if necessary. No observable state associated with this cache is modified
   *  until loading completes. This method provides a simple substitute for the conventional
   *  `if cached, return; otherwise create, cache and return` pattern.
   *
   *  '''Warning:''' as with `[[CacheLoader]]#load`, `loader` '''must not''' return
   *  `null`; it may either return a non-null value or throw an exception.
   *  The recommend way would be to use a `Cache[K, Option[V]]` with `loader`
   *  return `None` instead of throwing an exception.
   *
   *  This method is equivalent to `Cache.get(K, Callable<? extends V>)` from
   *  the Guava-Libraries.
   *
   *  @throws ExecutionException if a checked exception was thrown while loading the value
   *  @throws UncheckedExecutionException if an unchecked exception was thrown while loading the
   *     value
   *  @throws ExecutionError if an error was thrown while loading the value
   */
  @throws[ExecutionException]
  def getOrElseUpdate(key: K, loader: () => V): V

  /** Returns a map of the values associated with `keys` in this cache.
   *  The returned map will only contain entries which are already present in the cache.
   */
  def getAllPresent(keys: Traversable[K]): immutable.Map[K, V] = {
      def getOptional(key: K): Option[(K, V)] = getIfPresent(key) match {
        case Some(value) => Some((key, value))
        case _           => None
      }

    immutable.Map() ++ keys.flatMap(getOptional)
  }

  /** Adds a new key/value pair to this cache. If the cache previously contained a
   *  value associated with the `key`, the old value is replaced by the `value`.
   *
   *  Prefer `getOrElseUpdate(key, loader)` when using the conventional
   *  `if cached, return; otherwise create, cache and return` pattern.
   *
   */
  def put(key: K, value: V): Unit

  /** Puts all key/value pairs from the specified `Traversable` to the cache.
   *  The effect of this call is equivalent to that of calling `put(key, value)` for
   *  each `(key, value)` in the `Traversable`. The behavior of this operation is undefined
   *  if the specified map is modified while the operation is in progress.
   */
  def putAll(kvs: Traversable[(K, V)]): Unit = {
    kvs.seq foreach { kv => put(kv._1, kv._2) }
  }

  /** Discards any cached value for key `key`.
   */
  def invalidate(key: K): Unit

  /** Discards any cached values for keys `keys`.
   */
  def invalidateAll(keys: Traversable[K]): Unit = { keys foreach invalidate }

  /** Discards all entries in the cache.
   */
  def invalidateAll(): Unit

  /** Returns the approximate number of entries in this cache.
   */
  def size(): Long

  /** Returns a current snapshot of this cache's cumulative statistics. All stats are initialized
   *  to zero, and are monotonically increasing over the lifetime of the cache.
   */
  def stats(): CacheStats

  /** Performs any pending maintenance operations needed by the cache. Exactly which activities are
   *  performed -- if any -- is implementation-dependent.
   */
  def cleanUp(): Unit = {}

  /** Returns a view of the entries stored in this cache as a thread-safe map. Modifications made to
   *  the map directly affect the cache.
   */
  def asMap(): concurrent.Map[K, V]
}

/** Utility functions to convert between Guava `Cache[K, V]` and `Cache[K, V]`
 *  and vice versa.
 */
object Cache {

  /** Adds an `asScala` method that wraps Guava `Cache[K, V]` in a Mango `Cache[K, V]`
   *  using a `CacheWrapper[K, V]`.
   *
   *  The returned Mango `Cache[K, V]` forwards all method calls to the provided
   *  Guava `Cache[K, V]`.
   *
   *  @param cache the Guava `Cache[K, V]` to wrap in a Mango `Cache[K, V]`
   *  @return An object with an `asScala` method that returns a Mango `Cache[K, V]`
   *   view of the argument
   */
  implicit def asCacheConverter[K, V](cache: GuavaCache[K, V]): AsScala[Cache[K, V]] = {
    new AsScala(CacheWrapper(cache))
  }
}