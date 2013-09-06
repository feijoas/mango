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
package org.feijoas.mango.common.collect

import scala.collection.mutable.Builder

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.base.Preconditions.checkNotNull

/** Factory for RangeMap implementations
 *
 *  @author Markus Schneider
 *  @since 0.9
 */
@Beta
trait RangeMapFactory[Repr[K, V, O <: Ordering[K]] <: RangeMap[K, V, O] with RangeMapLike[K, V, O, Repr[K, V, O]]] {

  /** Returns an empty [[RangeMap]].
   */
  def empty[K, V, O <: Ordering[K]](implicit ord: O): Repr[K, V, O] = newBuilder[K, V, O](ord).result

  /** Returns a [[RangeMap]] that contains the provided ranges
   */
  def apply[K, V, O <: Ordering[K]](entries: (Range[K, O], V)*)(implicit ord: O): Repr[K, V, O] = {
    val builder = newBuilder[K, V, O](ord)
    entries.foreach { builder += checkNotNull(_) }
    builder.result
  }

  /** Returns a [[RangeMap]] initialized with the ranges in the specified range map.
   */
  def apply[K, V, O <: Ordering[K]](rangeMap: RangeMap[K, V, O])(implicit ord: O): Repr[K, V, O] = {
    val builder = newBuilder[K, V, O](ord)
    rangeMap.asMapOfRanges.foreach { builder += checkNotNull(_) }
    builder.result
  }

  /** Returns a new builder for [[RangeMap]].
   */
  def newBuilder[K, V, O <: Ordering[K]](implicit ord: O): Builder[(Range[K, O], V), Repr[K, V, O]]
}