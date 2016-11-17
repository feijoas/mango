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

import scala.collection.generic.HasNewBuilder

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.base.Preconditions.checkNotNull

/**
 * Implementation trait for [[RangeSet]]
 *
 *  $rangeSetNote
 *  @author Markus Schneider
 *  @since 0.8
 *
 *  @define rangeSetNote
 *
 *  A range set is a set comprising zero or more nonempty, disconnected ranges of type `C`
 *  for which an `Ordering[C]` is defined.
 *
 *  <p>Note that the behavior of `Range#isEmpty()` and `Range#isConnected(Range)` may
 *  not be as expected on discrete ranges.  See the Scaladoc of those methods for details.
 *
 *  <p>For a `Set` whose contents are specified by a [[Range]], see [[ContiguousSet]].
 *
 *  Usage example:
 *
 *  {{{
 *  import org.feijoas.mango.common.collect.Bound._
 *  import org.feijoas.mango.common.collect._
 *  import math.Ordering.Int
 *
 *  // immutable range set:
 *  val rangeSet = RangeSet(Range.open(1, 3), Range.closed(4, 9)) // {(1,3), [4,9]}
 *  val subSet = rangeSet.subRangeSet(Range.closed(2, 6))         // union view {[2,3), [4,6]}
 *
 *  // mutable range set:
 *  val mutableRangeSet = mutable.RangeSet(Range.closed(1, 10))   // {[1, 10]}
 *  mutableRangeSet += Range.closedOpen(11, 15)                   // disconnected range: {[1, 10], [11, 15)}
 *  mutableRangeSet += Range.closedOpen(15, 20)                   // connected range; {[1, 10], [11, 20)}
 *  mutableRangeSet += Range.openClosed(0, 0)                     // empty range; {[1, 10], [11, 20)}
 *  mutableRangeSet -= Range.open(5, 10)                          // splits [1, 10]; {[1, 5], [10, 10], [11, 20)}
 *  }}}
 *
 *  @tparam C    the type of the elements of the set
 *  @tparam O    the type of Ordering used to order the elements
 *  @tparam Repr the type of the set itself.
 */
@Beta
trait RangeSetLike[C, O <: Ordering[C], +Repr <: RangeSetLike[C, O, Repr] with RangeSet[C, O]]
    extends HasNewBuilder[Range[C, O], Repr] {
  self =>

  /**
   * Determines whether any of this range set's member ranges contains `value`.
   */
  def contains(value: C): Boolean = rangeContaining(value) != None

  /**
   * Returns the unique range from this range set that contains
   *  `value` as `Some(value)`, or `None` if this range set does not contain `value`.
   */
  def rangeContaining(value: C): Option[Range[C, O]] = {
    checkNotNull(value)
    asRanges.find { _.contains(value) }
  }

  /**
   * Returns `true` if there exists a member range in this range set which
   *  encloses the specified range.
   */
  def encloses(otherRange: Range[C, O]): Boolean = {
    checkNotNull(otherRange)
    asRanges.find { _.encloses(otherRange) }.isDefined
  }

  /**
   * Returns `true` if for each member range in `other` there exists a member range in
   *  this range set which encloses it. It follows that `this.contains(value)` whenever `other.contains(value)`.
   *  Returns `true` if `other` is empty.
   *
   *  <p>This is equivalent to checking if this range set `#encloses` each of the ranges in
   *  `other`.
   */
  def enclosesAll(other: RangeSet[C, O]): Boolean = {
    checkNotNull(other)
    other.asRanges.find { !this.encloses(_) }.isEmpty
  }

  /**
   * Returns `true` if this range set contains no ranges.
   */
  def isEmpty: Boolean = asRanges().isEmpty

  /**
   * Returns a `Some` with the minimal range which encloses all ranges in this range set
   *  or `None` if this range set is empty
   */
  def span(): Option[Range[C, O]]

  /**
   * Returns a view of the disconnected ranges that make up this
   *  range set.  The returned set may be empty. The iterators returned by its
   *  `Iterable#iterator` method return the ranges in increasing order of lower bound
   *  (equivalently, of upper bound).
   */
  def asRanges(): Set[Range[C, O]]

  /**
   * Returns a view of the complement of this `RangeSet`.
   */
  def complement(): Repr

  /**
   * Returns a view of the intersection of this `RangeSet` with the specified range.
   */
  def subRangeSet(view: Range[C, O]): Repr

  /**
   * Returns `true` if `obj` is another `RangeSet` that contains the same ranges
   *  according to `Range#equals(Any)`.
   */
  override def equals(obj: Any): Boolean = obj match {
    case other: RangeSet[_, _] => asRanges == other.asRanges
    case _                     => false
  }

  /**
   * Returns `asRanges().hashCode()`.
   */
  override def hashCode(): Int = asRanges.hashCode

  /**
   * Returns a readable string representation of this range set. For example, if this
   *  `RangeSet` consisted of `Ranges.closed(1, 3)` and `Ranges.greaterThan(4)`,
   *  this might return `"{[1‥3](4‥+∞)}"`.
   */
  override def toString(): String = {
    val builder = new StringBuilder()
    builder.append('{')
    asRanges foreach { builder.append(_) }
    builder.append('}')
    builder.toString()
  }
}
