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

import scala.collection.generic.Growable
import scala.collection.generic.Shrinkable

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect.Range
import org.feijoas.mango.common.collect

/** Implementation trait for mutable [[RangeSet]]
 *
 *  $rangeSetNote
 *  @author Markus Schneider
 *  @since 0.8
 */
@Beta
trait RangeSetLike[C, O <: Ordering[C], +Repr <: RangeSetLike[C, O, Repr] with RangeSet[C, O]]
  extends collect.RangeSetLike[C, O, Repr]
  with Growable[Range[C, O]]
  with Shrinkable[Range[C, O]] {

  /** Adds the specified range to this {@code RangeSet} (optional operation). That is, for equal
   *  range sets a and b, the result of {@code a.add(range)} is that {@code a} will be the minimal
   *  range set for which both {@code a.enclosesAll(b)} and {@code a.encloses(range)}.
   *
   *  <p>Note that {@code range} will be {@linkplain Range#span(Range) coalesced} with any ranges in
   *  the range set that are {@linkplain Range#isConnected(Range) connected} with it.  Moreover,
   *  if {@code range} is empty, this is a no-op.
   */
  def add(range: Range[C, O])

  /** Alias for `#add(range)` */
  final override def +=(range: Range[C, O]): this.type = {
    add(range)
    this
  }

  /** Removes the specified range from this {@code RangeSet} (optional operation). After this
   *  operation, if {@code range.contains(c)}, {@code this.contains(c)} will return {@code false}.
   *
   *  <p>If {@code range} is empty, this is a no-op.
   */
  def remove(range: Range[C, O])

  /** Alias for `#remove(range)` */
  final override def -=(range: Range[C, O]): this.type = {
    remove(range)
    this
  }

  /** Removes all ranges from this {@code RangeSet} (optional operation).  After this operation,
   *  {@code this.contains(c)} will return false for all {@code c}.
   *
   *  <p>This is equivalent to {@code remove(Range.all())}.
   */
  def clear()

  /** Adds all of the ranges from the specified range set to this range set (optional operation).
   *  After this operation, this range set is the minimal range set that
   *  {@linkplain #enclosesAll(RangeSet) encloses} both the original range set and {@code other}.
   *
   *  <p>This is equivalent to calling {@link #add} on each of the ranges in {@code other} in turn.
   *
   *  @throws UnsupportedOperationException if this range set does not support the {@code addAll}
   *         operation
   */
  def addAll(other: RangeSet[C, O]) = other.asRanges() foreach { range => add(range) }

  /** Removes all of the ranges from the specified range set from this range set (optional
   *  operation). After this operation, if {@code other.contains(c)}, {@code this.contains(c)} will
   *  return {@code false}.
   *
   *  <p>This is equivalent to calling {@link #remove} on each of the ranges in {@code other} in
   *  turn.
   */
  def removeAll(other: RangeSet[C, O]) = other.asRanges() foreach { range => remove(range) }
}
