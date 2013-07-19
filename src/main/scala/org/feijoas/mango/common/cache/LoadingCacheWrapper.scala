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
import scala.collection.convert.decorateAll.{ asJavaIterableConverter, mapAsScalaMapConverter }
import scala.collection.immutable
import scala.util.Try

import org.feijoas.mango.common.annotations.Beta

import com.google.common.cache.{ LoadingCache => GuavaLoadingCache }

/** An adapter that wraps a Guava-`LoadingCache` in a [[LoadingCache]] and forwards all
 *  method calls to the underlying Guava-`LoadingCache`.
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
protected[mango] trait LoadingCacheWrapper[K, V] extends CacheWrapper[K, V] with LoadingCache[K, V] {

  /** Returns the backing Guava `LoadingCache` delegate instance that methods are forwarded to.
   *  Concrete subclasses override this method to supply the instance being decorated.
   */
  protected def cache: GuavaLoadingCache[K, V]

  override def get(key: K): Try[V] = Try(cache.get(key))
  override def getUnchecked(key: K): V = cache.getUnchecked(key)
  override def refresh(key: K): Unit = cache.refresh(key)
  override def getAll(keys: Traversable[K]): Try[immutable.Map[K, V]] = Try {
    val map = cache.getAll(keys.toIterable.asJava).asScala
    // TODO: Change this as soon as we have wrappers for Guavas ImmutableMap
    immutable.Map.empty ++ map
  }
  override def getIfPresent(key: K) = Option(cache.getIfPresent(key))
}

private[mango] final object LoadingCacheWrapper {

  /** Factory method to create a `LoadingCache[K, V]` from a Guava `LoadingCache[K, V]`
   */
  def apply[K, V](guavaCache: GuavaLoadingCache[K, V]): LoadingCache[K, V] = new LoadingCacheWrapper[K, V] {
    override def cache = guavaCache
  }
}