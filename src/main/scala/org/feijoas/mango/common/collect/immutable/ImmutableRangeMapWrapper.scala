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
import org.feijoas.mango.common.collect.AsOrdered
import org.feijoas.mango.common.collect.Range
import org.feijoas.mango.common.collect.Range.asGuavaRangeConverter
import org.feijoas.mango.common.collect.RangeMapFactory
import org.feijoas.mango.common.collect.RangeMapWrapperLike

import com.google.common.{ collect => gcc }

/** An immutable implementation of RangeMap that delegates to Guava ImmutableRangeMap
 *
 *  @author Markus Schneider
 *  @since 0.9
 */
@Beta
private[mango] class ImmutableRangeMapWrapper[K, V] private (guava: gcc.RangeMap[AsOrdered[K], V])(override implicit val ordering: Ordering[K])
  extends RangeMap[K, V] with RangeMapWrapperLike[K, V, ImmutableRangeMapWrapper[K, V]] {

  override def delegate = guava
  override def factory = ImmutableRangeMapWrapper(_)(ordering)
  override def newBuilder = ImmutableRangeMapWrapper.newBuilder(ordering)
}

/** Factory for ImmutableRangeMapWrapper
 */
private[mango] object ImmutableRangeMapWrapper extends RangeMapFactory[ImmutableRangeMapWrapper] {

  /** Factory method */
  private[mango] def apply[K, V](guava: gcc.RangeMap[AsOrdered[K], V])(implicit ord: Ordering[K]) = new ImmutableRangeMapWrapper(guava)(ord)

  /** Returns a [[RangeMap]] initialized with the ranges in the specified range set.
   */
  override def apply[K, V](rangeMap: collect.RangeMap[K, V])(implicit ord: Ordering[K]) = rangeMap match {
    case same: ImmutableRangeMapWrapper[K, V] => same
    case _                                       => super.apply(rangeMap)
  }

  /** Returns a new builder for [[RangeMap]].
   */
  def newBuilder[K, V](implicit ord: Ordering[K]) = new Builder[(Range[K], V), ImmutableRangeMapWrapper[K, V]]() {
    var builder = gcc.ImmutableRangeMap.builder[AsOrdered[K], V]()
    override def +=(entry: (Range[K], V)): this.type = {
      builder.put(entry._1.asJava, entry._2)
      this
    }
    override def clear() = builder = gcc.ImmutableRangeMap.builder[AsOrdered[K], V]()
    override def result() = new ImmutableRangeMapWrapper(builder.build)
  }
}
