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
package org.feijoas.mango.common.hash

import scala.annotation.implicitNotFound

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.hash.Funnel.asGuavaFunnel

import com.google.common.hash.{ BloomFilter => GuavaBloomFilter }

/** A Bloom filter for instances of `T`. A Bloom filter offers an approximate containment test
 *  with one-sided error: if it claims that an element is contained in it, this might be in error,
 *  but if it claims that an element is <i>not</i> contained in it, then this is definitely true.
 *
 *  <p>From the Guava <a href="https://code.google.com/p/guava-libraries/wiki/HashingExplained/">BloomFilter tutorial</a>:
 *  {{{
 *  implicit object PersonFunnel extends Funnel[Person] {
 *     override def funnel(from: Person, into: PrimitiveSink) = ...
 *  }
 *
 *  val friends: BloomFilter[Person] = BloomFilter.create(500, 0.01)
 *  friendsList.foreach { case p: Person => friends.put(p) }
 *
 *  // much later
 *  if (friends.mightContain(dude)) {
 *     // the probability that dude reached this place if he isn't a friend is 1%
 *     // we might, for example, start asynchronously loading things for dude while we do a more expensive exact check
 *  }
 *
 *  }}}
 *
 *  <p>The false positive probability (`FPP`) of a bloom filter is defined as the probability
 *  that `#mightContain(T)` will erroneously return `true` for an object that
 *  has not actually been put in the `BloomFilter`.
 *
 *  @author Markus Schneider
 *  @since 0.6 (copied from Guava-libraries)
 */
@Beta
@SerialVersionUID(1L)
final case class BloomFilter[T] private (private val delegate: GuavaBloomFilter[T]) extends (T => Boolean) with Serializable {

  /** Creates a new {@code BloomFilter} that's a copy of this instance. The new instance is equal to
   *  this instance but shares no mutable state.
   */
  def copy() = new BloomFilter(delegate.copy())

  /** Returns {@code true} if the element <i>might</i> have been put in this Bloom filter,
   *  {@code false} if this is <i>definitely</i> not the case.
   */
  def mightContain(obj: T): Boolean = delegate.mightContain(obj)

  /** Puts an element into this {@code BloomFilter}. Ensures that subsequent invocations of
   *  `#mightContain(T)` with the same element will always return {@code true}.
   *
   *  @return true if the bloom filter's bits changed as a result of this operation. If the bits
   *     changed, this is <i>definitely</i> the first time {@code object} has been added to the
   *     filter. If the bits haven't changed, this <i>might</i> be the first time {@code object}
   *     has been added to the filter. Note that {@code put(t)} always returns the
   *     <i>opposite</i> result to what {@code mightContain(t)} would have returned at the time
   *     it is called."
   */
  def put(obj: T): Boolean = delegate.put(obj)

  /** Returns the probability that `#mightContain(T)` will erroneously return
   *  {@code true} for an object that has not actually been put in the {@code BloomFilter}.
   *
   *  <p>Ideally, this number should be close to the {@code fpp} parameter
   *  passed in `#create(Integer, Double)`, or smaller. If it is
   *  significantly higher, it is usually the case that too many elements (more than
   *  expected) have been put in the {@code BloomFilter}, degenerating it.
   */
  def expectedFpp(): Double = delegate.expectedFpp()

  /** Equivalent to `#mightContain`; provided only to satisfy the {@link Predicate} interface.
   *  When using a reference of type {@code BloomFilter}, always invoke {@link #mightContain}
   *  directly instead.
   */
  override def apply(input: T): Boolean = delegate.apply(input)

  override def toString = "BloomFilter"
}

/** Factory for BloomFilters
 *
 *  In order to be able to create a `BloomFilter[T]` an implementation of a
 *  [[Funnel]] for `T` (called type class) must be in implicit scope (recommended)
 *  or passed explicitly as a parameter.
 */
final object BloomFilter {
  /** Creates a {@code Builder} of a `BloomFilter[T]`, with the expected number
   *  of insertions and expected false positive probability.
   *
   *  <p>Note that overflowing a `BloomFilter` with significantly more elements
   *  than specified, will result in its saturation, and a sharp deterioration of its
   *  false positive probability.
   *
   *  <p>The constructed `BloomFilter[T]` will be serializable if the provided
   *  `Funnel[T]` is.
   *
   *  @param funnel the funnel of T's that the constructed {@code BloomFilter[T]} will use
   *  @param expectedInsertions the number of expected insertions to the constructed
   *     {@code BloomFilter[T]}; must be positive
   *  @param fpp the desired false positive probability (must be positive and less than 1.0)
   *  @return a {@code BloomFilter}
   */
  @implicitNotFound(msg = "No implementation of Funnel[${T}] in implicit scope")
  def create[T](expectedInsertions: Int, fpp: Double)(implicit funnel: Funnel[T]): BloomFilter[T] = {
    new BloomFilter(GuavaBloomFilter.create(funnel.asJava, expectedInsertions, fpp))
  }

  /** Creates a {@code Builder} of a {@link BloomFilter BloomFilter[T]}, with the expected number
   *  of insertions, and a default expected false positive probability of 3%.
   *
   *  <p>Note that overflowing a {@code BloomFilter} with significantly more elements
   *  than specified, will result in its saturation, and a sharp deterioration of its
   *  false positive probability.
   *
   *  <p>The constructed {@code BloomFilter[T]} will be serializable if the provided
   *  {@code Funnel[T]} is.
   *
   *  @param funnel the funnel of T's that the constructed {@code BloomFilter[T]} will use
   *  @param expectedInsertions the number of expected insertions to the constructed
   *     {@code BloomFilter<T>}; must be positive
   *  @return a {@code BloomFilter}
   */
  @implicitNotFound(msg = "No implementation of Funnel[${T}] in implicit scope")
  def create[T](expectedInsertions: Int)(implicit funnel: Funnel[T]): BloomFilter[T] = {
    new BloomFilter(GuavaBloomFilter.create(funnel.asJava, expectedInsertions))
  }
}