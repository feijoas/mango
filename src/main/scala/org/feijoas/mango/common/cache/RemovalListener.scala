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
import org.feijoas.mango.common.cache.RemovalNotification._
import org.feijoas.mango.common.convert.AsJava

import com.google.common.cache.{ RemovalListener => GuavaRemovalListener, RemovalNotification => GuavaRemovalNotification }

/** A function `RemovalNotification[K, V] => Unit` that can receive a notification when an entry is
 *  removed from a cache. The removal resulting in notification could have occured to an entry being
 *  manually removed or replaced, or due to eviction resulting from timed expiration, exceeding a
 *  maximum size, or garbage collection.
 *
 *  <p>An instance may be called concurrently by multiple threads to process different entries.
 *  Implementations of `RemovalListener` should avoid performing blocking calls or synchronizing on
 *  shared resources.
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
final object RemovalListener {

  /** Adds an `asJava` method that wraps a Mango `RemovalNotification[K, V] => Unit` in a
   *  Guava `RemovalListener[K, V]`.
   *
   *  The returned Guava `RemovalCause` forwards all method calls to the provided
   *  Mango `RemovalNotification[K, V] => Unit`.
   *
   *  @param listener the Mango `RemovalNotification[K, V] => Unit` to wrap in a Guava `RemovalListener`
   *  @return An object with an `asJava` method that returns a Guava `RemovalListener`
   *   view of the argument
   */
  implicit def asGuavaRemovalListenerConverter[K, V](listener: RemovalNotification[K, V] => Unit): AsJava[GuavaRemovalListener[K, V]] =
    new AsJava(new GuavaRemovalListener[K, V] {
      override def onRemoval(notification: GuavaRemovalNotification[K, V]): Unit = listener(notification.asScala)
    })
}