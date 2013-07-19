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
import org.feijoas.mango.common.convert.AsScala
import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.cache.RemovalCause._

import com.google.common.cache.{ RemovalNotification => GuavaRemovalNotification }
import com.google.common.cache.{ RemovalCause => GuavaRemovalCause }
import org.feijoas.mango.common.cache.RemovalCause._

/** A notification of the removal of a single entry. The key and/or value may be
 *  `None` if the corresponding key/value was already garbage collected.
 *
 *  <p>A key/value pair associated with [[CacheBuilder]], this class holds
 *  strong references to the key and value, regardless of the type of references the cache may be
 *  using.
 *
 *  @constructor create a new RemovalNotification with key,value and cause of removal
 *  @param key the key of the cache entry
 *  @param value the value of the cache entry
 *  @param cause the cause for which the entry was removed
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from Guava-libraries)
 */
@Beta
case class RemovalNotification[+K, +V](
    val key: Option[K],
    val value: Option[V],
    val cause: RemovalCause) {
}

final object RemovalNotification {

  /** Adds an `asScala` method that converts a Guava `RemovalNotification[K, V]`
   *  to a Mango `RemovalNotification[K, V]`.
   *
   *  The returned Mango `RemovalNotification[K, V]` contains a reference of the
   *  `key` and `value` from the Guava `RemovalNotification[K, V]` wrapped in
   *  a `Some[K]` or `None` if `key` or `value` is `null`.
   *
   *  @param notification the Guava `RemovalNotification[K, V]` to convert to a Mango `RemovalNotification[K, V]`
   *  @return An object with an `asScala` method that returns a Mango `RemovalNotification[K, V]`
   *   view of the argument
   */
  implicit final def asMangoRemovalNotificationConverter[K, V](notification: GuavaRemovalNotification[K, V]): AsScala[RemovalNotification[K, V]] = {
    val (key, value, cause) = (notification.getKey(), notification.getValue(), notification.getCause())
    new AsScala(RemovalNotification(Option(key), Option(value), cause.asScala))
  }
}