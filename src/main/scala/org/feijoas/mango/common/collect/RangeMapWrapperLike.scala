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

import scala.collection.convert.decorateAll.mapAsScalaMapConverter

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect.AsOrdered.asOrdered
import org.feijoas.mango.common.collect.Range.asGuavaRangeConverter

import com.google.common.{ collect => gcc }

/** Implementation trait for [[RangeMap]] that delegates to Guava
 *
 *  @author Markus Schneider
 *  @since 0.8
 */
@Beta
private[mango] trait RangeMapWrapperLike[K, V, +Repr <: RangeMapWrapperLike[K, V, Repr] with RangeMap[K, V]]
  extends RangeMapLike[K, V, Repr] {
  self =>

  /** The Guava RangeMap to use internally */
  protected def delegate: gcc.RangeMap[AsOrdered[K], V]

  /** The `Ordering[K]` used for Ranges is needed */
  protected[this] implicit def ordering: Ordering[K]

  /** Creates a new Repr from a Guava RangeMap */
  protected[this] def factory: gcc.RangeMap[AsOrdered[K], V] => Repr

  override def get(key: K) = Option(delegate.get(key))
  override def isEmpty = delegate.asMapOfRanges().isEmpty
  override def subRangeMap(range: Range[K]) = factory(delegate.subRangeMap(range.asJava))

  override def getEntry(key: K) = {
    val entry = delegate.getEntry(key)
    if (entry == null)
      None
    else
      Some((Range(entry.getKey()), entry.getValue()))
  }

  override def span() = {
    if (isEmpty)
      None
    else
      Some(Range(delegate.span()))
  }

  override def asMapOfRanges() = {
    // TODO: Change this as soon as we have wrappers for immutable collectios
    val gmap = delegate.asMapOfRanges.asScala
    val builder = Map.newBuilder[Range[K], V]
    gmap.foreach(kv => builder += ((Range[K](kv._1), kv._2)))
    builder.result
  }
}