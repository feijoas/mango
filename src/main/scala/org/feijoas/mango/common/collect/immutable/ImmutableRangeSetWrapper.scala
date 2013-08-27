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

import scala.annotation.meta.beanGetter
import scala.annotation.meta.beanSetter
import scala.annotation.meta.field
import scala.annotation.meta.getter
import scala.annotation.meta.setter
import scala.collection.mutable.Builder

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect.AsOrdered
import org.feijoas.mango.common.collect.Range
import org.feijoas.mango.common.collect.Range.asGuavaRangeConverter
import org.feijoas.mango.common.collect.RangeSetFactory
import org.feijoas.mango.common.collect.RangeSetWrapperLike
import org.feijoas.mango.common.collect

import com.google.common.collect.{ RangeSet => GuavaRangeSet }
import com.google.common.collect.ImmutableRangeSet
import scala.math.Ordering._

/** An immutable implementation of RangeSet that delegates to Guava ImmutableRangeSet
 *
 *  @author Markus Schneider
 *  @since 0.8
 */
@Beta
@SerialVersionUID(1L)
private[mango] class ImmutableRangeSetWrapper[C, O <: Ordering[C]] private (guava: GuavaRangeSet[AsOrdered[C]])(implicit ord: O)
  extends RangeSet[C, O] with RangeSetWrapperLike[C, O, ImmutableRangeSetWrapper[C, O]] with Serializable {

  /** The Guava RangeSet to use internally */
  override protected def delegate = guava

  /** The Ordering[C] used for Ranges is needed */
  override implicit def ordering: O = ord

  /** Creates a new Repr from a Guava RangeSet */
  override def wrap: GuavaRangeSet[AsOrdered[C]] => ImmutableRangeSetWrapper[C, O] = new ImmutableRangeSetWrapper(_)(ordering)

  /** Returns a new builder for a range set.
   */
  override def newBuilder = ImmutableRangeSetWrapper.newBuilder[C, O](ord)
}

/** Factory for ImmutableRangeSetWrapper
 */
private[mango] final object ImmutableRangeSetWrapper extends RangeSetFactory[ImmutableRangeSetWrapper] {

  /** Returns a [[RangeSet]] initialized with the ranges in the specified range set.
   */
  override def apply[C, O <: Ordering[C]](rangeSet: collect.RangeSet[C, O])(implicit ord: O) = rangeSet match {
    case same: ImmutableRangeSetWrapper[C, O] => same
    case _                                    => super.apply(rangeSet)
  }

  /** Returns a new builder for a range set.
   */
  def newBuilder[C, O <: Ordering[C]](implicit ord: O) = new Builder[Range[C, O], ImmutableRangeSetWrapper[C, O]]() {
    var builder = ImmutableRangeSet.builder[AsOrdered[C]]()
    override def +=(range: Range[C, O]): this.type = {
      builder.add(range.asJava)
      this
    }
    override def clear() = builder = ImmutableRangeSet.builder[AsOrdered[C]]()
    override def result() = new ImmutableRangeSetWrapper(builder.build)
  }
}