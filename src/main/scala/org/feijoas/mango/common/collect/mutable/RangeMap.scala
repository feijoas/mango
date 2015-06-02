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
package org.feijoas.mango.common.collect.mutable

import scala.collection.mutable.Builder

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect
import org.feijoas.mango.common.collect.Range
import org.feijoas.mango.common.collect.RangeMapFactory

/** $rangeMapNote
 *  @author Markus Schneider
 *  @since 0.9
 */
@Beta
trait RangeMap[K, V] extends collect.RangeMap[K, V] with RangeMapLike[K, V, RangeMap[K, V]] {

}

/** Factory for immutable [[RangeMap]]
 */
object RangeMap extends RangeMapFactory[RangeMap] {
  override def newBuilder[K, V](implicit ord: Ordering[K]): Builder[(Range[K], V), RangeMap[K, V]] = TreeRangeMapWrapper.newBuilder
}