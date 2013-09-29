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

import scala.collection.convert.decorateAll.asScalaSetConverter

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect.AsOrdered.asOrdered
import org.feijoas.mango.common.collect.Range.asGuavaRangeConverter

import com.google.common.collect.{ RangeSet => GuavaRangeSet }

/** Implementation trait for [[RangeSet]] that delegates to Guava
 *
 *  @author Markus Schneider
 *  @since 0.8
 */
@Beta
private[mango] trait RangeSetWrapperLike[C, O <: Ordering[C], +Repr <: RangeSetWrapperLike[C, O, Repr] with RangeSet[C, O]]
  extends RangeSetLike[C, O, Repr] {
  self =>

  /** The Guava RangeSet to use internally */
  protected def delegate: GuavaRangeSet[AsOrdered[C]]

  /** The Ordering[C] used for Ranges is needed */
  protected implicit def ordering: O

  /** Creates a new Repr from a Guava RangeSet */
  protected[this] def factory: (GuavaRangeSet[AsOrdered[C]]) => Repr

  override def contains(value: C): Boolean = delegate.contains(value)
  override def encloses(otherRange: Range[C, O]): Boolean = delegate.encloses(otherRange.asJava)
  override def isEmpty: Boolean = delegate.isEmpty
  override def complement(): Repr = factory(delegate.complement)
  override def subRangeSet(view: Range[C, O]): Repr = factory(delegate.subRangeSet(view.asJava))

  override def rangeContaining(value: C): Option[Range[C, O]] = delegate.rangeContaining(value) match {
    case null => None
    case some => Some(Range(some))
  }

  override def enclosesAll(other: org.feijoas.mango.common.collect.RangeSet[C, O]): Boolean = other match {
    case wrapper: RangeSetWrapperLike[C, O, _] => delegate.enclosesAll(wrapper.delegate)
    case _                                     => super.enclosesAll(other)
  }

  override def span(): Option[Range[C, O]] = delegate.isEmpty match {
    case true  => None
    case false => Some(Range(delegate.span()))
  }

  override def asRanges(): Set[Range[C, O]] = {
    val set = delegate.asRanges().asScala.view.map(Range[C, O](_))
    // TODO: Change this as soon as we have wrappers for Guavas ImmutableSet
    Set.empty ++ set
  }
}