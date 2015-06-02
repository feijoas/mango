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
import org.feijoas.mango.common.collect.AsOrdered
import org.feijoas.mango.common.collect.Range
import org.feijoas.mango.common.collect.Range.asGuavaRangeConverter
import org.feijoas.mango.common.collect.RangeMapFactory

import com.google.common.{ collect => gcc }

/** An mutable implementation of RangeMap that delegates to Guava TreeRangeMap
 *
 *  @author Markus Schneider
 *  @since 0.9
 */
@Beta
private[mango] class TreeRangeMapWrapper[K, V, O <: Ordering[K]] private (guava: gcc.RangeMap[AsOrdered[K], V])(override implicit val ordering: O)
  extends RangeMap[K, V, O] with RangeMapWrapperLike[K, V, O, TreeRangeMapWrapper[K, V, O]] {

  override def delegate = guava
  override def factory = TreeRangeMapWrapper(_)(ordering)
  override def newBuilder = TreeRangeMapWrapper.newBuilder(ordering)
}

/** Factory for TreeRangeMapWrapper
 */
private[mango] final object TreeRangeMapWrapper extends RangeMapFactory[TreeRangeMapWrapper] {

  /** Factory method */
  private[mango] def apply[K, V, O <: Ordering[K]](guava: gcc.RangeMap[AsOrdered[K], V])(implicit ord: O) = new TreeRangeMapWrapper(guava)(ord)

  /** Returns a new builder for [[RangeMap]].
   */
  def newBuilder[K, V, O <: Ordering[K]](implicit ord: O) = new Builder[(Range[K], V), TreeRangeMapWrapper[K, V, O]]() {
    val builder = gcc.TreeRangeMap.create[AsOrdered[K], V]()
    override def +=(entry: (Range[K], V)): this.type = {
      builder.put(entry._1.asJava, entry._2)
      this
    }
    override def clear() = builder.clear()
    override def result() = new TreeRangeMapWrapper(builder)
  }
}
