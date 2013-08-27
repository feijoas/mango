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

import scala.math.BigInt.long2bigInt

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect.AsOrdered.asOrdered

import com.google.common.collect.{ DiscreteDomain => GuavaDiscreteDomain }

/** A descriptor for a <i>discrete</i> domain such as all
 *  `Int` instances. A discrete domain is one that supports the three basic
 *  operations: `#next`, `#previous` and `#distance`, according
 *  to their specifications. The methods `#minValue` and `#maxValue`
 *  should also be overridden for bounded types.
 *
 *  <p>A discrete domain always represents the <i>entire</i> set of values of its
 *  type; it cannot represent partial domains such as "prime integers" or
 *  "strings of length 5."
 *
 *  <p>See the Guava User Guide section on <a href=
 *  "http://code.google.com/p/guava-libraries/wiki/RangesExplained#Discrete_Domains">
 *  {@code DiscreteDomain}</a>.
 *
 *  @author Markus Schneider
 *  @since 0.8 (copied from Guava-libraries)
 */
@Beta
trait DiscreteDomain[C] {

  /** Returns a `Some` wiht the unique least value of type `C` that is greater than
   *  `value`, or `None` if none exists. Inverse operation to
   *  `#previous`.
   *
   *  @param value any value of type `C`
   *  @return `Some` with the least value greater than `value`, or `None` if
   *     `value` is `maxValue()`
   */
  def next(value: C): Option[C]

  /** Returns a `Some` with the unique greatest value of type `C` that is less than
   *  `value`, or `None` if none exists. Inverse operation to
   *  `#next`.
   *
   *  @param value any value of type `C`
   *  @return `Some` with the greatest value less than `value`, or `None` if
   *     `value` is `minValue()`
   */
  def previous(value: C): Option[C]

  /** Returns a signed value indicating how many nested invocations of
   *  `#next` (if positive) or `#previous` (if negative) are needed to reach
   *  {@code end} starting from {@code start}. For example, if {@code end =
   *  next(next(next(start)))}, then {@code distance(start, end) == 3} and {@code
   *  distance(end, start) == -3}. As well, {@code distance(a, a)} is always
   *  zero.
   *
   *  <p>Note that this function is necessarily well-defined for any discrete
   *  type.
   *
   *  @return the distance as described above, or `Long#MIN_VALUE` or
   *     `Long#MAX_VALUE` if the distance is too small or too large,
   *     respectively.
   */
  def distance(start: C, end: C): Long

  /** Returns a `Some` with the minimum value of type {@code C}, if it has one. The minimum
   *  value is the unique value for which `Ordering#compare(this,that)`
   *  never returns a positive value for any input of type {@code C}.
   *
   *  <p>The default implementation returns `None`.
   *
   *  @return a `Some` with the minimum value of type {@code C} or `None` if there is none
   */
  def minValue(): Option[C] = None

  /** Returns a `Some` with the maximum value of type {@code C}, if it has one. The maximum
   *  value is the unique value for which `Ordering#compare(this,that)`
   *  never returns a negative value for any input of type {@code C}.
   *
   *  <p>The default implementation returns `None`.
   *
   *  @return a `Some` with the maximum value of type {@code C} or `None` if there is none
   */
  def maxValue(): Option[C] = None
}

object DiscreteDomain {

  /** Returns the discrete domain for values of type {@code Int}.
   */
  @SerialVersionUID(1L)
  implicit final object IntDomain extends DiscreteDomain[Int] with Serializable {

    override def next(value: Int): Option[Int] = value match {
      case Int.MaxValue => None
      case _            => Some(value + 1)
    }

    override def previous(value: Int): Option[Int] = value match {
      case Int.MinValue => None
      case _            => Some(value - 1)
    }

    override def distance(start: Int, end: Int): Long = end.toLong - start
    override def minValue() = Some(Int.MinValue)
    override def maxValue() = Some(Int.MaxValue)
    override def toString() = "DiscreteDomains.ints()"
  }

  /** Returns the discrete domaipppn for values of type {@code Long}.
   *
   */
  @SerialVersionUID(1L)
  implicit final object LongDomain extends DiscreteDomain[Long] with Serializable {

    override def next(value: Long): Option[Long] = value match {
      case Long.MaxValue => None
      case _             => Some(value + 1)
    }

    override def previous(value: Long): Option[Long] = value match {
      case Long.MinValue => None
      case _             => Some(value - 1)
    }

    override def distance(start: Long, end: Long): Long = {
      val result: Long = end - start
      if (end > start && result < 0) { // overflow
        return Long.MaxValue
      }
      if (end < start && result > 0) { // underflow
        return Long.MinValue
      }
      result
    }

    override def minValue() = Some(Long.MinValue)
    override def maxValue() = Some(Long.MaxValue)
    override def toString() = "DiscreteDomains.longs()"
  }

  /** Returns the discrete domain for values of type {@code BigInteger}.
   */
  // Note: This class is package private in Guava so we don't publish it at the moment
  @SerialVersionUID(1L)
  private[mango] implicit final object BigIntDomain extends DiscreteDomain[BigInt] with Serializable {
    override def next(value: BigInt): Option[BigInt] = Some(value + BigInt(1))
    override def previous(value: BigInt): Option[BigInt] = Some(value - BigInt(1))

    override def distance(start: BigInt, end: BigInt): Long = {
      (end - start).max(Long.MinValue).min(Long.MaxValue).toLong
    }
    override def toString() = "DiscreteDomains.bigInts()"
  }

  /** Conversion from Mango `DiscreteDomain` to Guava `DiscreteDomain`
   */
  private[mango] def asGuavaDiscreteDomain[C: Ordering](dm: DiscreteDomain[C]): GuavaDiscreteDomain[AsOrdered[C]] = {
    new GuavaDiscreteDomain[AsOrdered[C]] {
      // import implicit conversion
      import org.feijoas.mango.common.collect.Range._
      override def next(value: AsOrdered[C]): AsOrdered[C] = dm.next(value.value) match {
        case Some(value) => value
        case None        => null
      }
      override def previous(value: AsOrdered[C]): AsOrdered[C] = dm.previous(value.value) match {
        case Some(value) => value
        case None        => null
      }
      override def distance(start: AsOrdered[C], end: AsOrdered[C]): Long = dm.distance(start.value, end.value)
      override def minValue() = dm.minValue.getOrElse(throw new NoSuchElementException())
      override def maxValue() = dm.maxValue.getOrElse(throw new NoSuchElementException())
      override def toString() = dm.toString()
    }
  }
}