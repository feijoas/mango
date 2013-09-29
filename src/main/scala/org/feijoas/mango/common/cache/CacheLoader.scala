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

import scala.collection.immutable
import scala.concurrent.{ Future, Promise }
import org.feijoas.mango.common.base.Preconditions.checkNotNull
import org.feijoas.mango.common.convert.AsJava
import com.google.common.cache.{ CacheLoader => GuavaCacheLoader }

/** Computes or retrieves values, based on a key, for use in populating a [[LoadingCache]].
 *
 *  This class should be when bulk retrieval is significantly more efficient than
 *  many individual lookups. Otherwise the `CacheLoader` companion object can be used
 *  to create a `CacheLoader[K,V]` from a function `K => V`. For example
 *
 *  {{{
 *    val createExpensiveGraph: Key => Graph = ...
 *    val loader: CacheLoader[Key,Graph] = CacheLoader.from(createExpensiveGraph)
 *  }}}
 *
 *  Alternatively the function `K => V` can be passed to [[CacheBuilder]] directly.
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from Guava-libraries)
 */
trait CacheLoader[K, V] extends (K => V) {

  /** Computes or retrieves the value corresponding to `key`. Simply forwards
   *  to `load(K)`.
   */
  final def apply(key: K): V = load(key)

  /** Computes or retrieves the value corresponding to `key`.
   *
   *  @param key the non-null key whose value should be loaded
   *  @return the value associated with `key`; <b>must not be null</b>
   *  @throws Exception if unable to load the result
   *  @throws InterruptedException if this method is interrupted. InterruptedException is
   *     treated like any other Exception in all respects except that, when it is caught,
   *     the thread's interrupt status is set
   */
  @throws(classOf[Exception])
  def load(key: K): V

  /** Computes or retrieves a replacement value corresponding to an already-cached `key`. This
   *  method is called when an existing cache entry is refreshed by
   *  `CacheBuilder#refreshAfterWrite`, or through a call to `LoadingCache#refresh`.
   *
   *  <p>This implementation synchronously delegates to `#load`. It is recommended that it be
   *  overridden with an asynchronous implementation when using
   *  `CacheBuilder#refreshAfterWrite`.
   *
   *  <p><b>Note:</b> <i>all exceptions thrown by this method will be logged and then swallowed</i>.
   *
   *  @param key the non-null key whose value should be loaded
   *  @param oldValue the non-null old value corresponding to `key`
   *  @return the future new value associated with `key`;
   *     <b>must not be null, must not return null</b>
   *  @throws Exception if unable to reload the result
   *  @throws InterruptedException if this method is interrupted. `InterruptedException` is
   *     treated like any other `Exception` in all respects except that, when it is caught,
   *     the thread's interrupt status is set
   */
  @throws(classOf[Exception])
  def reload(key: K, oldValue: V): Future[V] = {
    checkNotNull(key)
    checkNotNull(oldValue)
    Promise successful (load(key)) future
  }

  /** Computes or retrieves the values corresponding to `keys`. This method is called by
   *  `LoadingCache#getAll`.
   *
   *  <p>If the returned map doesn't contain all requested `keys` then the entries it does
   *  contain will be cached, but `getAll` will throw an exception. If the returned map
   *  contains extra keys not present in `keys` then all returned entries will be cached,
   *  but only the entries for `keys` will be returned from `getAll`.
   *
   *  <p>This method should be overridden when bulk retrieval is significantly more efficient than
   *  many individual lookups. Note that `LoadingCache#getAll` will defer to individual calls
   *  to `LoadingCache#get` if this method is not overridden.
   *
   *  @param keys the unique, non-null keys whose values should be loaded
   *  @return an immutable map from each key in `keys` to the value associated with that key;
   *     <b>may not contain null values</b>
   *  @throws Exception if unable to load the result
   *  @throws InterruptedException if this method is interrupted. InterruptedException is
   *     treated like any other Exception in all respects except that, when it is caught,
   *     the thread's interrupt status is set
   */
  def loadAll(keys: Traversable[K]): immutable.Map[K, V] = {
    checkNotNull(keys)
    keys.map { (key: K) => (key, load(key)) }.toMap
  }
}

/** Factory for [[CacheLoader]] instances. */
object CacheLoader {

  /** Returns a cache loader based on an <i>existing</i> function.
   *
   *  @param function the function to be used for loading values; must never return {@code null}
   *  @return a cache loader that loads values by passing each key to {@code function}
   */
  def from[K, V](f: K => V) = new CacheLoader[K, V] {
    checkNotNull(f)
    override def load(key: K) = f(checkNotNull(key))
  }

  /** Returns a cache loader based on an <i>existing</i> supplier instance.
   *
   *  @param supplier the supplier to be used for loading values; must never return {@code null}
   *  @return a cache loader that loads values irrespective of the key
   */
  def from[K, V](f: () => V) = new CacheLoader[K, V] {
    checkNotNull(f)
    override def load(key: K) = { checkNotNull(key); f() }
  }

  /** Adds an `asJava` method that wraps Mango `CacheLoader[K,V]` in a Guava `CacheLoader[K, V]`
   *  using a `CacheLoaderWrapper[K, V]`.
   *
   *  The returned Guava `CacheLoader[K, V]` forwards all method calls to the provided
   *  Mango `CacheLoader[K, V]`.
   *
   *  @param loader the Mango `CacheLoader[K, V]` to wrap in a Guava `CacheLoader[K, V]`
   *  @return An object with an `asJava` method that returns a Guava `CacheLoader[K, V]`
   *   view of the argument
   */
  implicit def asGuavaCacheLoaderConverter[K, V](loader: CacheLoader[K, V]): AsJava[GuavaCacheLoader[K, V]] =
    new AsJava(CacheLoaderWrapper(checkNotNull(loader)))
}