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

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.convert.{ AsJava, AsScala }

import com.google.common.cache.{ RemovalCause => GuavaRemovalCause }

/** The reason why a cached entry was removed. See the companion object for
 *  the individual RemovalCause cases.
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from guava-libraries)
 */
@Beta
sealed trait RemovalCause extends Serializable {
  /** Returns {@code true} if there was an automatic removal due to eviction
   *  (the cause is neither [[RemovalCause.Explicit]] nor [[RemovalCause.Replaced]] ).
   */
  def wasEvicted: Boolean
}

/** Available RemovalCauses
 */
final object RemovalCause {

  /** The entry was manually removed by the user. This can result from the user invoking
   *  `Cache#invalidate, `Cache#invalidateAll(Traversable)` or `Cache#invalidateAll()`
   */
  case object Explicit extends RemovalCause {
    def wasEvicted = false
  }

  /** The entry itself was not actually removed, but its value was replaced by the user. This can
   *  result from the user invoking `Cache#put` or `LoadingCache#refresh`
   */
  case object Replaced extends RemovalCause {
    def wasEvicted = false
  }

  /** The entry was removed automatically because its key or value was garbage-collected. This
   *  can occur when using `CacheBuilder#weakKeys`, `CacheBuilder#weakValues`, or
   *  `CacheBuilder#softValues`.
   */
  case object Collected extends RemovalCause {
    def wasEvicted = false
  }

  /** The entry's expiration timestamp has passed. This can occur when using
   *  `CacheBuilder#expireAfterWrite` or `CacheBuilder#expireAfterAccess`.
   */
  case object Expired extends RemovalCause {
    def wasEvicted = false
  }

  /** The entry was evicted due to size constraints. This can occur when using
   *  `CacheBuilder#maximumSize` or `CacheBuilder#maximumWeight`.
   */
  case object Size extends RemovalCause {
    def wasEvicted = false
  }

  /** Adds an `asJava` method that converts a Mango `RemovalCause` to a
   *  Guava `RemovalCause`.
   *
   *  The returned Guava `RemovalCause` contains a copy of all values in the
   *  Mango `RemovalCause`.
   *
   *  @param cause the Mango `RemovalCause` to convert to a Guava `RemovalCause`
   *  @return An object with an `asJava` method that returns a Guava `RemovalCause`
   *   view of the argument
   */
  implicit final def asGuavaRemovalCauseConverter(cause: RemovalCause): AsJava[GuavaRemovalCause] = new AsJava(
    cause match {
      case RemovalCause.Explicit  => GuavaRemovalCause.EXPLICIT
      case RemovalCause.Replaced  => GuavaRemovalCause.REPLACED
      case RemovalCause.Collected => GuavaRemovalCause.COLLECTED
      case RemovalCause.Expired   => GuavaRemovalCause.EXPIRED
      case RemovalCause.Size      => GuavaRemovalCause.SIZE
    })

  /** Adds an `asScala` method that converts a Guava `RemovalCause` to a
   *  Mango `RemovalCause`.
   *
   *  The returned Mango `RemovalCause` contains a copy of all values in the
   *  Guava `RemovalCause`.
   *
   *  @param cause the Guava `RemovalCause` to convert to a Mango `RemovalCause`
   *  @return An object with an `asScala` method that returns a Mango `RemovalCause`
   *   view of the argument
   */
  implicit final def asMangoRemovalCauseConverter(cause: GuavaRemovalCause): AsScala[RemovalCause] = new AsScala(
    cause match {
      case GuavaRemovalCause.EXPLICIT  => Explicit
      case GuavaRemovalCause.REPLACED  => Replaced
      case GuavaRemovalCause.COLLECTED => Collected
      case GuavaRemovalCause.EXPIRED   => Expired
      case GuavaRemovalCause.SIZE      => Size
    })
}