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

import scala.collection.convert.decorateAll.asJavaIterableConverter

import org.feijoas.mango.common.collect.AsOrdered.asOrdered
import org.feijoas.mango.common.collect.Bound.FiniteBound
import org.feijoas.mango.common.collect.Bound.InfiniteBound
import org.feijoas.mango.common.collect.BoundType.asGuavaBoundType
import org.feijoas.mango.common.collect.BoundType.asMangoBoundType
import org.feijoas.mango.common.convert.AsJava
import org.feijoas.mango.common.convert.AsScala

import com.google.common.collect.{ Range => GuavaRange }

/** $rangeNote
 *  @author Markus Schneider
 *  @since 0.8 (copied from Guava-libraries)
 *
 *  @define rangeNote
 *
 *  A range (or "interval") defines the <i>boundaries</i> around a contiguous span of values of some
 *  type which an `Ordering` exists; for example, "integers from 1 to 100 inclusive." Note that it is not
 *  possible to <i>iterate</i> over these contained values. To do so, pass this range instance and
 *  an appropriate `DiscreteDomain` to `ContiguousSet#create`.
 *
 *  <h3>Types of ranges</h3>
 *
 *  <p>Each end of the range may be bounded or unbounded. If bounded, there is an associated
 *  <i>endpoint</i> value, and the range is considered to be either <i>open</i> (does not include the
 *  endpoint) or <i>closed</i> (includes the endpoint) on that side. With three possibilities on each
 *  side, this yields nine basic types of ranges, enumerated below. (Notation: a square bracket
 *  `[ ]` indicates that the range is closed on that side; a parenthesis `( )` means
 *  it is either open or unbounded. The construct `{x | statement}` is read "the set of all
 *  <i>x</i> such that <i>statement</i>.")
 *
 *  {{{
 *  Notation      Definition         Factory method
 *  (a..b)        {x | a < x < b}    Range#open
 *  [a..b]        {x | a <= x <= b}  Range#closed
 *  (a..b]        {x | a < x <= b}   Range#openClosed
 *  [a..b)        {x | a <= x < b}   Range#closedOpen
 *  (a..+inf)     {x | x > a}        Range#greaterThan
 *  [a..+inf)     {x | x >= a}       Range#atLeast
 *  (-inf..b)     {x | x < b}        Range#lessThan
 *  (-inf..b]     {x | x <= b}       Range#atMost
 *  (-inf..+inf)  {x}                Range#all
 *  }}}
 *
 *  <p>When both endpoints exist, the upper endpoint may not be less than the lower. The endpoints
 *  may be equal only if at least one of the bounds is closed:
 *
 *  <ul>
 *  <li>{@code [a..a]} : a singleton range
 *  <li>{@code [a..a); (a..a]} : empty ranges; also valid
 *  <li>{@code (a..a)} : <b>invalid</b>; an exception will be thrown
 *  </ul>
 *
 *  <h3>Warnings</h3>
 *
 *  <ul>
 *  <li>Use immutable value types only, if at all possible. If you must use a mutable type, <b>do
 *     not</b> allow the endpoint instances to mutate after the range is created!
 *  <li>Your value type's comparison method should be consistent with equals
 *     if at all possible. Otherwise, be aware that concepts used throughout this documentation such
 *     as "equal", "same", "unique" and so on actually refer to whether `Ordering#compare`
 *     returns zero, not whether equals returns {@code true}.
 *  </ul>
 *
 *  <h3>Other notes</h3>
 *
 *  <ul>
 *  <li>Instances of this type are obtained using the static factory methods in this class.
 *  <li>Ranges are <i>convex</i>: whenever two values are contained, all values in between them must
 *     also be contained. More formally, for any {@code c1 <= c2 <= c3} of type {@code C}, {@code
 *     r.contains(c1) && r.contains(c3)} implies {@code r.contains(c2)}). This means that a {@code
 *     Range[Int,Ordering[Int]} can never be used to represent, say, "all <i>prime</i> numbers from 1 to
 *     100."
 *  <li>When evaluated as a predicate, a range yields the same result as invoking
 *     `#contains`.
 *  <li>Terminology note: a range {@code a} is said to be the <i>maximal</i> range having property
 *     <i>P</i> if, for all ranges {@code b} also having property <i>P</i>, {@code a.encloses(b)}.
 *     Likewise, {@code a} is <i>minimal</i> when {@code b.encloses(a)} for all {@code b} having
 *     property <i>P</i>. See, for example, the definition of {@link #intersection intersection}.
 *  </ul>
 *
 *  <h3>Pattern matching</h3>
 *  Extractors are defined for general ranges with finite and infinite bounds.
 *
 *  Usage example:
 *  {{{
 *    val range = Range.atLeast(6) // Range[Int,math.Ordering.Int.type] = [6..inf)
 *    range match {
 *       case Range(FiniteBound(lower, lowerType),InfiniteBound) =>
 *       // matches lower = 6
 *       //         lowerType = Closed
 *    }
 *  }}}
 *
 *  <h3>Difference between Mango and Guava implementation</h3>
 *  Since most Scala classes don't implement `Ordered` a Range of type `T` needs a "companion" type
 *  class of `Ordering[T]`.
 *
 *  <h3>Further reading</h3>
 *
 *  <p>See the Guava User Guide article on
 *  <a href="http://code.google.com/p/guava-libraries/wiki/RangesExplained">Range</a>.
 */
@SerialVersionUID(1L)
final class Range[T] private (private val range: GuavaRange[AsOrdered[T]])(implicit private val ord: Ordering[T])
  extends (T => Boolean) with Serializable {

  // import implicit conversion
  import org.feijoas.mango.common.collect.Range._

  /** Returns {@code true} if this range is of the form {@code [v..v)} or {@code (v..v]}. (This does
   *  not encompass ranges of the form {@code (v..v)}, because such ranges are <i>invalid</i> and
   *  can't be constructed at all.)
   *
   *  <p>Note that certain discrete ranges such as the integer range {@code (3..4)} are <b>not</b>
   *  considered empty, even though they contain no actual values.  In these cases, it may be
   *  helpful to preprocess ranges with `#canonical(DiscreteDomain)`.
   */
  def isEmpty(): Boolean = range.isEmpty()

  /** Returns {@code true} if {@code value} is within the bounds of this range. For example, on the
   *  range {@code [0..2)}, {@code contains(1)} returns {@code true}, while {@code contains(2)}
   *  returns {@code false}.
   */
  def contains(value: T): Boolean = range.contains(value)

  /** Equivalent to `#contains`; provided only to satisfy the Predicate (`T => Boolean`) interface. When
   *  using a reference of type {@code Range}, always invoke `#contains` directly instead.
   */
  final override def apply(input: T) = contains(input)

  /** Returns {@code true} if every element in {@code values} is contained in
   *  this range.
   */
  def containsAll(values: Iterable[T]): Boolean = {
    val it = values.view.map { v: T => AsOrdered(v) }
    range.containsAll(it.asJava)
  }

  /** Returns {@code true} if the bounds of {@code other} do not extend outside the bounds of this
   *  range. Examples:
   *
   *  <ul>
   *  <li>{@code [3..6]} encloses {@code [4..5]}
   *  <li>{@code (3..6)} encloses {@code (3..6)}
   *  <li>{@code [3..6]} encloses {@code [4..4)} (even though the latter is empty)
   *  <li>{@code (3..6]} does not enclose {@code [3..6]}
   *  <li>{@code [4..5]} does not enclose {@code (3..6)} (even though it contains every value
   *     contained by the latter range)
   *  <li>{@code [3..6]} does not enclose {@code (1..1]} (even though it contains every value
   *     contained by the latter range)
   *  </ul>
   *
   *  Note that if {@code a.encloses(b)}, then {@code b.contains(v)} implies {@code a.contains(v)},
   *  but as the last two examples illustrate, the converse is not always true.
   *
   *  <p>Being reflexive, antisymmetric and transitive, the {@code encloses} relation defines a
   *  <i>partial order</i> over ranges. There exists a unique maximal range
   *  according to this relation, and also numerous minimal ranges. Enclosure
   *  also implies connectedness.
   */
  def encloses(other: Range[T]): Boolean = range.encloses(other.range)

  /** Returns {@code true} if there exists a (possibly empty) range which is
   *  enclosed by both this range and {@code other}.
   *
   *  <p>For example,
   *  <ul>
   *  <li>{@code [2, 4)} and {@code [5, 7)} are not connected
   *  <li>{@code [2, 4)} and {@code [3, 5)} are connected, because both enclose {@code [3, 4)}
   *  <li>{@code [2, 4)} and {@code [4, 6)} are connected, because both enclose the empty range
   *     {@code [4, 4)}
   *  </ul>
   *
   *  <p>Note that this range and {@code other} have a well-defined union and
   *  intersection (as a single, possibly-empty range) if and only if this
   *  method returns {@code true}.
   *
   *  <p>The connectedness relation is both reflexive and symmetric, but does not form an
   *  equivalence relation as it is not transitive.
   *
   *  <p>Note that certain discrete ranges are not considered connected, even though there are no
   *  elements "between them."  For example, {@code [3, 5]} is not considered connected to {@code
   *  [6, 10]}.  In these cases, it may be desirable for both input ranges to be preprocessed with
   *  `#canonical(DiscreteDomain)` before testing for connectedness.
   */
  def isConnected(other: Range[T]): Boolean = range.isConnected(other.range)

  /** Returns the maximal range enclosed by both this range and {@code
   *  connectedRange}, if such a range exists.
   *
   *  <p>For example, the intersection of {@code [1..5]} and {@code (3..7)} is {@code (3..5]}. The
   *  resulting range may be empty; for example, {@code [1..5)} intersected with {@code [5..7)}
   *  yields the empty range {@code [5..5)}.
   *
   *  <p>The intersection exists if and only if the two ranges are connected.
   *
   *  <p>The intersection operation is commutative, associative and idempotent, and its identity
   *  element is `Range#all`.
   *
   *  @throws IllegalArgumentException if {@code isConnected(connectedRange)} is {@code false}
   */
  @throws[IllegalAccessException]
  def intersection(connectedRange: Range[T]): Range[T] = {
    Range(range.intersection(connectedRange.range))
  }

  /** Returns the minimal range that encloses both this range and {@code
   *  other}. For example, the span of {@code [1..3]} and {@code (5..7)} is {@code [1..7)}.
   *
   *  <p><i>If</i> the input ranges are connected, the returned range can
   *  also be called their <i>union</i>. If they are not, note that the span might contain values
   *  that are not contained in either input range.
   *
   *  <p>Like intersection, this operation is commutative, associative
   *  and idempotent. Unlike it, it is always well-defined for any two input ranges.
   */
  def span(other: Range[T]): Range[T] = Range(range.span(other.range))

  /** Returns the canonical form of this range in the given domain. The canonical form has the
   *  following properties:
   *
   *  <ul>
   *  <li>equivalence: {@code a.canonical().contains(v) == a.contains(v)} for all {@code v} (in other
   *     words, {@code ContiguousSet.create(a.canonical(domain), domain).equals(
   *     ContiguousSet.create(a, domain))}
   *  <li>uniqueness: unless {@code a.isEmpty()},
   *     {@code ContiguousSet.create(a, domain).equals(ContiguousSet.create(b, domain))} implies
   *     {@code a.canonical(domain).equals(b.canonical(domain))}
   *  <li>idempotence: {@code a.canonical(domain).canonical(domain).equals(a.canonical(domain))}
   *  </ul>
   *
   *  Furthermore, this method guarantees that the range returned will be one of the following
   *  canonical forms:
   *
   *  <ul>
   *  <li>[start..end)
   *  <li>[start..+∞)
   *  <li>(-∞..end) (only if type {@code C} is unbounded below)
   *  <li>(-∞..+∞) (only if type {@code C} is unbounded below)
   *  </ul>
   */
  def canonical(domain: DiscreteDomain[T]): Range[T] =
    Range(range.canonical(DiscreteDomain.asGuavaDiscreteDomain(domain)))

  /** Returns {@code true} if this range has a lower endpoint.
   */
  def hasLowerBound() = range.hasLowerBound

  /** Returns {@code true} if this range has an upper endpoint.
   */
  def hasUpperBound() = range.hasUpperBound

  /** Returns a string representation of this range, such as {@code "[3..5)"} (other examples are
   *  listed in the class documentation).
   */
  override def toString = range.toString
  override def hashCode = 31 * range.hashCode + ord.hashCode
  override def equals(that: Any) = that match {
    case other: Range[T] => range.equals(other.range) && ord.equals(other.ord)
    case _                  => false
  }
}

/** A range (or "interval") defines the <i>boundaries</i> around a contiguous span of values of some
 *  type which an `Ordering` exists; for example, "integers from 1 to 100 inclusive." Note that it is not
 *  possible to <i>iterate</i> over these contained values. To do so, pass this range instance and
 *  an appropriate `DiscreteDomain` to `ContiguousSet#create`.
 *
 *  <h3>Types of ranges</h3>
 *
 *  <p>Each end of the range may be bounded or unbounded. If bounded, there is an associated
 *  <i>endpoint</i> value, and the range is considered to be either <i>open</i> (does not include the
 *  endpoint) or <i>closed</i> (includes the endpoint) on that side. With three possibilities on each
 *  side, this yields nine basic types of ranges, enumerated below. (Notation: a square bracket
 *  `[ ]` indicates that the range is closed on that side; a parenthesis `( )` means
 *  it is either open or unbounded. The construct `{x | statement}` is read "the set of all
 *  <i>x</i> such that <i>statement</i>.")
 *
 *  {{{
 *  Notation      Definition         Factory method
 *  (a..b)        {x | a < x < b}    Range#open
 *  [a..b]        {x | a <= x <= b}  Range#closed
 *  (a..b]        {x | a < x <= b}   Range#openClosed
 *  [a..b)        {x | a <= x < b}   Range#closedOpen
 *  (a..+inf)     {x | x > a}        Range#greaterThan
 *  [a..+inf)     {x | x >= a}       Range#atLeast
 *  (-inf..b)     {x | x < b}        Range#lessThan
 *  (-inf..b]     {x | x <= b}       Range#atMost
 *  (-inf..+inf)  {x}                Range#all
 *  }}}
 *
 *  <p>When both endpoints exist, the upper endpoint may not be less than the lower. The endpoints
 *  may be equal only if at least one of the bounds is closed:
 *
 *  <ul>
 *  <li>{@code [a..a]} : a singleton range
 *  <li>{@code [a..a); (a..a]} : empty ranges; also valid
 *  <li>{@code (a..a)} : <b>invalid</b>; an exception will be thrown
 *  </ul>
 *
 *  <h3>Warnings</h3>
 *
 *  <ul>
 *  <li>Use immutable value types only, if at all possible. If you must use a mutable type, <b>do
 *     not</b> allow the endpoint instances to mutate after the range is created!
 *  <li>Your value type's comparison method should be consistent with equals
 *     if at all possible. Otherwise, be aware that concepts used throughout this documentation such
 *     as "equal", "same", "unique" and so on actually refer to whether `Ordering#compare`
 *     returns zero, not whether equals returns {@code true}.
 *  </ul>
 *
 *  <h3>Other notes</h3>
 *
 *  <ul>
 *  <li>Instances of this type are obtained using the static factory methods in this class.
 *  <li>Ranges are <i>convex</i>: whenever two values are contained, all values in between them must
 *     also be contained. More formally, for any {@code c1 <= c2 <= c3} of type {@code C}, {@code
 *     r.contains(c1) && r.contains(c3)} implies {@code r.contains(c2)}). This means that a {@code
 *     Range[Int,Ordering[Int]} can never be used to represent, say, "all <i>prime</i> numbers from 1 to
 *     100."
 *  <li>When evaluated as a predicate, a range yields the same result as invoking
 *     `#contains`.
 *  <li>Terminology note: a range {@code a} is said to be the <i>maximal</i> range having property
 *     <i>P</i> if, for all ranges {@code b} also having property <i>P</i>, {@code a.encloses(b)}.
 *     Likewise, {@code a} is <i>minimal</i> when {@code b.encloses(a)} for all {@code b} having
 *     property <i>P</i>. See, for example, the definition of {@link #intersection intersection}.
 *  </ul>
 *
 *  <h3>Pattern matching</h3>
 *  Extractors are defined for general ranges with finite and infinite bounds.
 *  For example:
 *
 *  {{{
 *    val range = Range.atLeast(6) // Range[Int,math.Ordering.Int.type] = [6..inf)
 *    range match {
 *       case Range(FiniteBound(lower, lowerType),InfiniteBound) =>
 *       // matches lower = 6
 *       //         lowerType = Closed
 *    }
 *  }}}
 *
 *  <h3>Difference between Mango and Guava implementation</h3>
 *  Since most Scala classes don't implement `Ordered` a Range of type `T` needs a "companion" type
 *  class of `Ordering[T]`.
 *
 *  <h3>Further reading</h3>
 *
 *  <p>See the Guava User Guide article on
 *  <a href="http://code.google.com/p/guava-libraries/wiki/RangesExplained">{@code Range}</a>.
 *
 *  @author Markus Schneider
 *  @since 0.8 (copied from Guava-libraries)
 */
object Range {

  /** Factory method take the Guava delegate
   */
  private[mango] def apply[T](delegate: GuavaRange[AsOrdered[T]])(implicit ord: Ordering[T]): Range[T] = new Range(delegate)

  /** Returns a range that contains all values strictly greater than {@code
   *  lower} and strictly less than {@code upper}.
   *
   *  @throws IllegalArgumentException if {@code lower} is greater than <i>or
   *     equal to</i> {@code upper}
   */
  @throws[IllegalArgumentException]
  def open[T](lower: T, upper: T)(implicit ord: Ordering[T]): Range[T] = {
    Range(GuavaRange.open(lower, upper))
  }

  /** Returns a range that contains all values greater than or equal to
   *  {@code lower} and less than or equal to {@code upper}.
   *
   *  @throws IllegalArgumentException if {@code lower} is greater than {@code upper}
   */
  @throws[IllegalArgumentException]
  def closed[T](lower: T, upper: T)(implicit ord: Ordering[T]): Range[T] = {
    Range(GuavaRange.closed(lower, upper))
  }

  /** Returns a range that contains all values greater than or equal to
   *  {@code lower} and strictly less than {@code upper}.
   *
   *  @throws IllegalArgumentException if {@code lower} is greater than {@code upper}
   */
  @throws[IllegalArgumentException]
  def closedOpen[T](lower: T, upper: T)(implicit ord: Ordering[T]): Range[T] = {
    Range(GuavaRange.closedOpen(lower, upper))
  }

  /** Returns a range that contains all values strictly greater than {@code
   *  lower} and less than or equal to {@code upper}.
   *
   *  @throws IllegalArgumentException if {@code lower} is greater than {@code upper}
   */
  @throws[IllegalArgumentException]
  def openClosed[T](lower: T, upper: T)(implicit ord: Ordering[T]): Range[T] = {
    Range(GuavaRange.openClosed(lower, upper))
  }

  /** Returns a range that contains any value from {@code lower} to {@code
   *  upper}, where each endpoint may be either inclusive (closed) or exclusive
   *  (open).
   *
   *  @throws IllegalArgumentException if {@code lower} is greater than {@code upper}
   */
  @throws[IllegalArgumentException]
  def range[T](lower: T, lowerType: BoundType, upper: T, upperType: BoundType)(implicit ord: Ordering[T]): Range[T] = {
    Range(GuavaRange.range(lower, lowerType.asJava, upper, upperType.asJava))
  }

  /** Returns a range that contains all values strictly less than {@code
   *  endpoint}.
   */
  def lessThan[T](endpoint: T)(implicit ord: Ordering[T]): Range[T] = {
    Range(GuavaRange.lessThan(endpoint))
  }

  /** Returns a range that contains all values less than or equal to
   *  {@code endpoint}.
   */
  def atMost[T](endpoint: T)(implicit ord: Ordering[T]): Range[T] = {
    Range(GuavaRange.atMost(endpoint))
  }

  /** Returns a range with no lower bound up to the given endpoint, which may be
   *  either inclusive (closed) or exclusive (open).
   */
  def upTo[T](endpoint: T, boundType: BoundType)(implicit ord: Ordering[T]): Range[T] = {
    Range(GuavaRange.upTo(endpoint, boundType.asJava))
  }

  /** Returns a range that contains all values strictly greater than {@code
   *  endpoint}.
   */
  def greaterThan[T](endpoint: T)(implicit ord: Ordering[T]): Range[T] = {
    Range(GuavaRange.greaterThan(endpoint))
  }

  /** Returns a range that contains all values greater than or equal to
   *  {@code endpoint}.
   */
  def atLeast[T](endpoint: T)(implicit ord: Ordering[T]): Range[T] = {
    Range(GuavaRange.atLeast(endpoint))
  }

  /** Returns a range from the given endpoint, which may be either inclusive
   *  (closed) or exclusive (open), with no upper bound.
   */
  def downTo[T](endpoint: T, boundType: BoundType)(implicit ord: Ordering[T]): Range[T] = {
    Range(GuavaRange.downTo(endpoint, boundType.asJava))
  }

  /** Returns a range that contains every value of type {@code T}.
   */
  def all[T]()(implicit ord: Ordering[T]): Range[T] = new Range(GuavaRange.all[AsOrdered[T]]())

  /** Returns a range that contains only the given value. The returned range is closed
   *  on both ends.
   */
  def singleton[T](value: T)(implicit ord: Ordering[T]): Range[T] = new Range(GuavaRange.singleton(value))

  /** Returns the minimal range that contains all of the given values.
   *  The returned range is closed on both ends.
   *
   *  @throws ClassCastException if the parameters are not <i>mutually
   *     comparable</i>
   *  @throws NoSuchElementException if {@code values} is empty
   *  @throws NullPointerException if any of {@code values} is null
   */
  @throws[NoSuchElementException]
  @throws[NullPointerException]
  def encloseAll[T](values: Iterable[T])(implicit ord: Ordering[T]): Range[T] = {
    val it = values.view.map { v: T => AsOrdered(v) }
    Range(GuavaRange.encloseAll(it.asJava))
  }

  /** Extractor to pattern match `Range`
   *
   *  For example:
   *  {{{
   *    val range = Range.atLeast(6) // Range[Int,math.Ordering.Int.type] = [6..inf)
   *    range match {
   *       case Range(FiniteBound(lower, lowerType),InfiniteBound) =>
   *       // matches lower = 6
   *       //         lowerType = Closed
   *    }
   *  }}}
   */
  def unapply[T](range: Range[T]): Option[(Bound[T], Bound[T])] = {
    val delegate = range.range
    val lower = delegate.hasLowerBound match {
      case true  => FiniteBound(delegate.lowerEndpoint.value, delegate.lowerBoundType.asScala)
      case false => InfiniteBound
    }
    val upper = delegate.hasUpperBound match {
      case true  => FiniteBound(delegate.upperEndpoint.value, delegate.upperBoundType.asScala)
      case false => InfiniteBound
    }
    Some((lower, upper))
  }

  /** Adds an `asJava` method that wraps a Scala `Range[T,O]` in
   *  a Guava `Range[AsOrdered[T]]`.
   *
   *  The returned Guava `Range[AsOrdered[T]]` forwards all method calls
   *  to the given Scala `Range[T,O]`.
   *
   *  @param range the Scala `Range[T,O]` to wrap in a Guava `Range[AsOrdered[T]]`
   *  @return An object with an `asJava` method that returns a Guava `Range[AsOrdered[T]]`
   *   view of the argument
   */
  implicit private[mango] def asGuavaRangeConverter[T](range: Range[T]): AsJava[GuavaRange[AsOrdered[T]]] = {
    new AsJava(range.range)
  }

  /** Adds an `asScala` method that wraps a Guava `Range[AsOrdered[T]]` in
   *  a Scala `Range[T,O]`.
   *
   *  The returned Scala `Range[T,O]` forwards all method calls
   *  to the given Guava `Range[AsOrdered[T]]`.
   *
   *  @param range the Guava `Range[AsOrdered[T]]` to wrap in a Scala `Range[T,O]`
   *  @return An object with an `asScala` method that returns a Scala `Range[T,O]`
   *   view of the argument
   */
  implicit private[mango] def asMangoRangeConverter[T](range: GuavaRange[AsOrdered[T]])(implicit ord: Ordering[T]): AsScala[Range[T]] = {
    new AsScala(Range[T](range))
  }
}

