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

import scala.annotation.meta.{ beanGetter, beanSetter, field, getter, setter }
import scala.collection.immutable
import scala.util.{ Failure, Success, Try }

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.convert.AsScala

import com.google.common.cache.{ LoadingCache => GuavaLoadingCache }
import com.google.common.util.concurrent.UncheckedExecutionException

/** A semi-persistent mapping from keys to values. Values are automatically loaded by the cache,
 *  and are stored in the cache until either evicted or manually invalidated.
 *
 *  <p>Implementations of this interface are expected to be thread-safe, and can be safely accessed
 *  by multiple concurrent threads.
 *
 *  <p>When evaluated as a [[http://www.scala-lang.org/api/current/index.html#scala.Function1 scala.Function]],
 *  a cache yields the same result as invoking `#getUnchecked`.
 *
 *  <p>Note that while this class is still annotated as `Beta`, the API is frozen from a
 *  consumer's standpoint. In other words existing methods are all considered `non-Beta` and
 *  won't be changed without going through an 18 month deprecation cycle; however new methods may be
 *  added at any time.
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from Guava-libraries)
 */
@Beta
trait LoadingCache[K, V] extends Cache[K, V] with (K => V) {

  /** Returns the value associated with `key` in this cache, first loading that value if
   *  necessary. No observable state associated with this cache is modified until loading completes.
   *
   *  <p>If another call to `#get` or `#getIfPresent` is currently loading the value for
   *  `key`, simply waits for that thread to finish and returns its loaded value. Note that
   *  multiple threads can concurrently load values for distinct keys.
   *
   *  <p>Caches loaded by a [[CacheLoader]] will call `CacheLoader#load` to load new values
   *  into the cache. If another value was associated
   *  with `key` while the new value was loading then a removal notification will be sent for
   *  the new value.
   *
   *  <p> The [[CacheLoader]] should not throw exceptions. If the calculation from a [[CacheLoader]]
   *  can fail the use of `CacheLoader[K,Option[V]]` or `CacheLoader[K,Try[V]]` is preferred.
   *  If the cache loader associated with this cache is known not to throw checked
   *  exceptions, then prefer `#getUnchecked` over this method.
   *
   *
   *  @throws ExecutionException if a checked exception was thrown while loading the value
   *  @throws UncheckedExecutionException if an unchecked exception was thrown while loading the
   *     value
   *  @throws ExecutionError if an error was thrown while loading the value
   */
  def get(key: K): Try[V]

  /** Returns the value associated with `key` in this cache, first loading that value if
   *  necessary. No observable state associated with this cache is modified until loading
   *  completes. Unlike `#get`, this method does not handle checked exceptions, and thus should
   *  only be used in situations where checked exceptions are not thrown by the cache loader.
   *
   *  <p>If another call to `#get` or `#getUnchecked` is currently loading the value for
   *  `key`, simply waits for that thread to finish and returns its loaded value. Note that
   *  multiple threads can concurrently load values for distinct keys.
   *
   *  <p>Caches loaded by a [[CacheLoader]] will call `CacheLoader#load` to load new values
   *  into the cache. Newly loaded values are added to the cache using
   *  `Cache.asMap().putIfAbsent` after loading has completed; if another value was associated
   *  with `key` while the new value was loading then a removal notification will be sent for
   *  the new value.
   *
   *  <p><b>Warning:</b> this method silently converts checked exceptions to unchecked exceptions,
   *  and should not be used with cache loaders which throw checked exceptions. In such cases use
   *  `#get` instead.
   *
   *  @throws UncheckedExecutionException if an exception was thrown while loading the value,
   *     regardless of whether the exception was checked or unchecked
   *  @throws ExecutionError if an error was thrown while loading the value
   */
  def getUnchecked(key: K): V = get(key) match {
    case Success(value) => value
    case Failure(e)     => throw new UncheckedExecutionException(e.getCause())
  }

  /** Loads a new value for key `key`, possibly asynchronously. While the new value is loading
   *  the previous value (if any) will continue to be returned by `get(key)` unless it is
   *  evicted. If the new value is loaded successfully it will replace the previous value in the
   *  cache; if an exception is thrown while refreshing the previous value will remain, <i>and the
   *  exception will be logged (using `java.util.logging.Logger`) and swallowed</i>.
   *
   *  <p>Caches loaded by a [[CacheLoader]] will call `CacheLoader#reload` if the
   *  cache currently contains a value for `key`, and `CacheLoader#load` otherwise.
   *  Loading is asynchronous only if `CacheLoader#reload` was overridden with an
   *  asynchronous implementation.
   *
   *  <p>Returns without doing anything if another thread is currently loading the value for
   *  `key`. If the cache loader associated with this cache performs refresh asynchronously
   *  then this method may return before refresh completes.
   */
  def refresh(key: K): Unit

  /** Returns a map of the values associated with `keys`, creating or retrieving those values
   *  if necessary. The returned map contains entries that were already cached, combined with newly
   *  loaded entries; it will never contain null keys or values.
   *
   *  <p>Caches loaded by a [[CacheLoader]] will issue a single request to
   *  `CacheLoader#loadAll` for all keys which are not already present in the cache. All
   *  entries returned by `CacheLoader#loadAll` will be stored in the cache, over-writing
   *  any previously cached values. This method will throw an exception if
   *  `CacheLoader#loadAll` returns `null`, returns a map containing null keys or values,
   *  or fails to return an entry for each requested key.
   *
   *  <p>Note that duplicate elements in `keys`, as determined by `==`, will
   *  be ignored.
   *
   *  @throws ExecutionException if a checked exception was thrown while loading the values
   *  @throws UncheckedExecutionException if an unchecked exception was thrown while loading the
   *     values
   *  @throws ExecutionError if an error was thrown while loading the values
   */
  def getAll(keys: Traversable[K]): Try[immutable.Map[K, V]] = {
    /*keys.foldLeft(Try(Map.empty[K, V])) {
      case (tmap, key) => for {
        map <- tmap
        value <- map.get(key) match {
          case Some(value) => Success(value)
          case None        => get(key)
        }
      } yield map.updated(key, value)
    }*/
    Try { keys.toSet.map((k: K) => (k, get(k).get)).toMap }
  }

  /** Returns the value associated with `key` in this cache, first loading that value if
   *  necessary. No observable state associated with this cache is modified until loading completes.
   *  The method simply forwards to `get(key)`.
   */
  final def apply(key: K): V = getUnchecked(key)

  /** Returns a map of the values associated with `keys`, creating or retrieving those values
   *  if necessary. The returned map contains entries that were already cached, combined with newly
   *  loaded entries; it will never contain null keys or values.
   *  The method simply forwards to `getAll(keys)`.
   */
  final def apply(keys: Traversable[K]): immutable.Map[K, V] = getAll(keys).get
}

/** Utility functions to convert between Guava `LoadingCache[K, V]` and `LoadingCache[K, V]`
 *  and vice versa.
 */
final object LoadingCache {

  /** Adds an `asScala` method that wraps Guava `LoadingCache[K, V]` in a Mango `LoadingCache[K, V]`
   *  using a `LoadingCacheWrapper[K, V]`.
   *
   *  The returned Mango `LoadingCache[K, V]` forwards all method calls to the provided
   *  Guava `LoadingCache[K, V]`.
   *
   *  @param cache the Guava `LoadingCache[K, V]` to wrap in a Mango `LoadingCache[K, V]`
   *  @return An object with an `asScala` method that returns a Mango `LoadingCache[K, V]`
   *   view of the argument
   */
  implicit def asMangoLoadingCacheConverter[K, V](cache: GuavaLoadingCache[K, V]): AsScala[LoadingCache[K, V]] = {
    new AsScala(LoadingCacheWrapper(cache))
  }
}