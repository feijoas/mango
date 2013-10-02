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

import com.google.common.cache.{ Weigher => GuavaWeigher }
import org.feijoas.mango.common.convert.AsJava

/** Calculates the weights of cache entries.
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
final object Weigher {

  /** Adds an `asJava` method that wraps a function `(K, V) => Int` in a
   *  Guava `Weigher[K, V]`.
   *
   *  The returned Guava `Weigher[K, V]` forwards all method calls to the provided
   *  function `(K, V) => Int`.
   *
   *  @param weigher the function `(K, V) => Int` to wrap in a Guava `GuavaWeigher[K, V]`
   *  @return An object with an `asJava` method that returns a Guava `GuavaWeigher[K, V]`
   *   view of the argument
   */
  implicit def asGuavaWeigherConverter[K, V](weigher: (K, V) => Int): AsJava[GuavaWeigher[K, V]] = {
    new AsJava(asGuavaWeigher(weigher))
  }

  private def asGuavaWeigher[K, V](weigher: (K, V) => Int): GuavaWeigher[K, V] = new GuavaWeigher[K, V] {
    override final def weigh(key: K, value: V): Int = weigher(key, value)
  }
}