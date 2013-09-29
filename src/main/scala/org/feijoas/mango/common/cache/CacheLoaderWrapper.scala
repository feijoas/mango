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

import java.util.{ Map => JMap }
import java.lang.{ Iterable => JIterable }

import scala.collection.convert.decorateAll.{ iterableAsScalaIterableConverter, mapAsJavaMapConverter }

import org.feijoas.mango.common.base.Preconditions.checkNotNull
import org.feijoas.mango.common.util.concurrent.Futures._

import com.google.common.cache.{ CacheLoader => GuavaCacheLoader }
import com.google.common.util.concurrent.ListenableFuture

/** An adapter that wraps a Mango-[[CacheLoader]] in a Guava-`CacheLoader` and forwards all
 *  method calls to the underlying Mango-CacheLoader.
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
private[mango] case class CacheLoaderWrapper[K, V](private val delegate: CacheLoader[K, V])
    extends GuavaCacheLoader[K, V] {

  @throws(classOf[Exception])
  override def load(key: K): V = delegate.load(checkNotNull(key))

  @throws(classOf[Exception])
  override def reload(key: K, oldValue: V): ListenableFuture[V] = {
    delegate.reload(checkNotNull(key), checkNotNull(oldValue)) match {
      case null      => null
      case _@ result => result.asJava
    }
  }

  override def loadAll(keys: JIterable[_ <: K]): JMap[K, V] =
    delegate.loadAll(checkNotNull(keys.asScala)) match {
      case null      => null
      case _@ result => result.asJava
    }
}