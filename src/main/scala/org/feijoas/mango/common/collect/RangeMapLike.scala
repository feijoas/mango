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

/** Implementation trait for [[RangeMap]]
 *
 *  $rangeMapNote
 *  @author Markus Schneider
 *  @since 0.9
 *
 *  @define rangeMapNote
 *
 *  A mapping from disjoint nonempty ranges to non-null values. Queries look up the value
 *  associated with the range (if any) that contains a specified key.
 *
 *  <p>In contrast to [[RangeSet]], no "coalescing" is done of connected ranges, even
 *  if they are mapped to the same value.
 *
 *  Usage example:
 *
 *  {{{
 *   import org.feijoas.mango.common.collect.mutable
 *   import org.feijoas.mango.common.collect.Range
 *   import math.Ordering.Int
 *
 *   // mutable range map
 *   val rangeMap = mutable.RangeMap(Range.open(3, 7) -> "1") //Map((3..7) -> 1)
 *   rangeMap += Range.closed(9, 10) -> "2"              // Map((3..7) -> 1, [9..10] -> 2)
 *   rangeMap += Range.closed(12, 16) -> "3"             // Map((3..7) -> 1, [9..10] -> 2, [12..16] -> 3)
 *
 *   val sub = rangeMap.subRangeMap(Range.closed(5, 11)) // Map([5..7) -> 1, [9..10] -> 2)
 *   sub.put(Range.closed(7, 9), "4")                    // sub = Map([5..7) -> 1, [7..9] -> 4, (9..10] -> 2)
 *
 *   // rangeMap = Map((3..7) -> 1, [7..9] -> 4, (9..10] -> 2, [12..16] -> 3)
 *  }}}
 *
 *  @tparam K    the type of the keys of the map
 *  @tparam V    the type of the elements of the map
 *  @tparam O    the type of Ordering used to order the elements
 *  @tparam Repr the type of the map itself.
 */
@Beta
trait RangeMapLike[K, V, O <: Ordering[K], +Repr <: RangeMapLike[K, V, O, Repr] with RangeMap[K, V, O]]
  extends HasNewBuilder[(Range[K, O], V), Repr] {
  self =>

  /** Returns the value associated with the specified key in a `Some`, or `None` if there is no
   *  such value.
   *
   *  <p>Specifically, if any range in this range map contains the specified key, the value
   *  associated with that range is returned.
   */
  def get(key: K): Option[V]

  /** Returns the range containing this key and its associated value in a `Some`, if such a range is present
   *  in the range map, or `None` otherwise.
   */
  def getEntry(key: K): Option[(Range[K, O], V)]

  /** Returns the minimal range enclosing the ranges in this `RangeMap` in a `Some`
   *  or `None` if this range map is empty
   */
  def span(): Option[Range[K, O]]

  /** Returns this RangeMap as a map of ranges.
   *
   *  <b>Warning:</b> This differs from Guava which returns an unmodifiable view
   *  of this RangeMap which modifications to its range map are guaranteed
   *  to read through to the returned map.
   *
   *  <p>It is guaranteed that no empty ranges will be in the returned `Map`.
   */
  def asMapOfRanges(): Map[Range[K, O], V]

  /** Returns a view of the part of this range map that intersects with `range`.
   *
   *  <p>For example, if `rangeMap` had the entries
   *  {@code [1, 5] => "foo", (6, 8) => "bar", (10, \u2025) => "baz"}
   *  then `rangeMap.subRangeMap(Range.open(3, 12))` would return a range map
   *  with the entries {@code (3, 5) => "foo", (6, 8) => "bar", (10, 12) => "baz"}.
   *
   *  <p>The returned range map supports all optional operations that this range map supports.
   */
  def subRangeMap(range: Range[K, O]): Repr

  /** Returns `true` if this range map contains no ranges.
   */
  def isEmpty: Boolean = asMapOfRanges.isEmpty

  /** Returns `true` if `obj` is another [[RangeMap]] that has an equivalent
   *  `#asMapOfRanges()`.
   */
  override def equals(obj: Any): Boolean = obj match {
    case other: RangeMap[_, _, _] => asMapOfRanges == other.asMapOfRanges
    case _                        => false
  }

  /** Returns `asMapOfRanges().hashCode()`.
   */
  override def hashCode(): Int = asMapOfRanges().hashCode

  /** Returns a readable string representation of this range map.
   */
  override def toString(): String = asMapOfRanges().toString
}