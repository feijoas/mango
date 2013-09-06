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
package org.feijoas.mango.common.collect.immutable

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
trait RangeMap[K, V, O <: Ordering[K]] extends collect.RangeMap[K, V, O] with RangeMapLike[K, V, O, RangeMap[K, V, O]] {

}

/** Factory for immutable [[RangeMap]]
 */
final object RangeMap extends RangeMapFactory[RangeMap] {
  override def newBuilder[K, V, O <: Ordering[K]](implicit ord: O): Builder[(Range[K, O], V), RangeMap[K, V, O]] = ImmutableRangeMapWrapper.newBuilder
}