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
package org.feijoas.mango.common.primitives

import scala.math.BigInt
import scala.math.Ordering

import org.feijoas.mango.common.base.Preconditions.checkArgument
import org.feijoas.mango.common.base.Preconditions.checkNotNull
import org.feijoas.mango.common.convert.AsJava
import org.feijoas.mango.common.convert.AsScala

import com.google.common.primitives.UnsignedLong
import com.google.common.primitives.UnsignedLongs

/** A value class for unsigned {@code Long} values, supporting arithmetic operations.
 *
 *  <p>See the Guava User Guide article on
 *  [[http://code.google.com/p/guava-libraries/wiki/PrimitivesExplained#Unsigned_support unsigned primitive utilities]].
 *
 *  @author Markus Schneider
 *  @since 0.10 (copied from Guava-libraries)
 */
@SerialVersionUID(1L)
final class ULong private (val value: Long) extends AnyVal with Ordered[ULong] with Serializable {
  import ULong._

  /** Returns the result of adding this and {@code that}. If the result would have more than 64 bits,
   *  returns the low 64 bits of the result.
   *
   */
  def +(that: ULong): ULong = fromLongBits(this.value + that.value)

  /** Returns the result of subtracting this and {@code that}. If the result would have more than 64
   *  bits, returns the low 64 bits of the result.
   */
  def -(that: ULong): ULong = fromLongBits(this.value - that.value)

  /** Returns the result of multiplying this and {@code that}. If the result would have more than 64
   *  bits, returns the low 64 bits of the result.
   */
  def *(that: ULong): ULong = fromLongBits(value * that.value)

  /** Returns the result of dividing this by {@code that}.
   */
  def /(that: ULong): ULong = fromLongBits(UnsignedLongs.divide(value, that.value))

  /** Returns this modulo {@code that}.
   */
  def mod(that: ULong): ULong = fromLongBits(UnsignedLongs.remainder(value, that.value))

  /** Returns the value of this {@code ULong} as an {@code Int}.
   */
  def toInt(): Int = value.toInt

  /** Returns the value of this {@code ULong} as a {@code Long}. This is an inverse operation
   *  to `#fromLongBits`.
   *
   *  <p>Note that if this {@code ULong} holds a value {@code >= 2^63}, the returned value
   *  will be equal to {@code this - 2^64}.
   */
  def toLong(): Long = value

  /** Returns the value of this {@code ULong} as a {@code Float}, analogous to a widening
   *  primitive conversion from {@code Long} to {@code Float}, and correctly rounded.
   */
  def toFloat(): Float = {
    val fValue: Float = (value & unsigned_mask)
    if (value < 0)
      fValue + maxLongAsFloat
    else
      fValue
  }

  /** Returns the value of this {@code ULong} as a {@code Double}, analogous to a widening
   *  primitive conversion from {@code Long} to {@code Double}, and correctly rounded.
   */
  def toDouble(): Double = {
    val dValue: Double = (value & unsigned_mask)
    if (value < 0)
      dValue + maxLongAsFloat
    else
      dValue
  }

  /** Returns the value of this {@code ULong} as a {@code BigInt}.
   */
  def toBigInt(): BigInt = {
    val bigInt = BigInt(value & unsigned_mask)
    if (value < 0)
      bigInt.setBit(java.lang.Long.SIZE - 1)
    else
      bigInt
  }

  /** Compares this unsigned Long to another unsigned Long.
   *  Returns {@code 0} if they are equal, a negative number if {@code this < other},
   *  and a positive number if {@code this > other}.
   */
  override def compare(that: ULong): Int = UnsignedLongs.compare(this.value, that.value)

  /** Returns a string representation of the {@code ULong} value, in base 10.
   */
  override def toString(): String = UnsignedLongs.toString(value)

  /** Returns a string representation of the {@code ULong} value, in base {@code radix}. If
   *  {@code radix < Character.MIN_RADIX} or {@code radix > Character.MAX_RADIX}, the radix
   *  {@code 10} is used.
   */
  def toString(radix: Int): String = UnsignedLongs.toString(value, radix)
}

/** Static utility methods pertaining to {@code Long} primitives that interpret values as
 *  <i>unsigned</i> (that is, any negative value {@code x} is treated as the positive value
 *  {@code 2^64 + x}).
 *
 *  <p>In addition, this class provides several static methods for converting a {@code Long} to a
 *  {@code String} and a {@code String} to a {@code Long} that treat the {@code Long} as an unsigned
 *  number.
 *
 *  <p>See the Guava User Guide article on
 *  [[http://code.google.com/p/guava-libraries/wiki/PrimitivesExplained#Unsigned_support unsigned primitive utilities]].
 *
 *  @author Markus Schneider
 *  @since 0.10 (copied from Guava-libraries)
 */
final object ULong {
  private val maxLongAsFloat = Long.MaxValue.toFloat
  private val unsigned_mask: Long = 0x7fffffffffffffffL

  /** Maximum value an ULong can represent
   */
  val MaxValue = fromLongBits(-1L)

  /** Returns an {@code ULong} representing the same value as the specified {@code Long}.
   *
   *  Equivalent to `valueOf(value)`
   *
   *  @throws IllegalArgumentException if {@code value} is negative
   */
  def apply(value: Long): ULong = valueOf(value)

  /** Returns a {@code ULong} representing the same value as the specified
   *  {@code BigInteger}. This is the inverse operation of `#toBigInt`.
   *
   *  Equivalent to `valueOf(value)`
   *
   *  @throws IllegalArgumentException if {@code value} is negative or {@code value >= 2^64}
   */
  def apply(value: BigInt): ULong = valueOf(value)

  /** Returns an {@code ULong} corresponding to a given bit representation.
   *  The argument is interpreted as an unsigned 64-bit value. Specifically, the sign bit
   *  of {@code bits} is interpreted as a normal bit, and all other bits are treated as usual.
   *
   *  <p>If the argument is nonnegative, the returned result will be equal to {@code bits},
   *  otherwise, the result will be equal to {@code 2^64 + bits}.
   *
   *  <p>To represent decimal constants less than {@code 2^63}, consider {@link #valueOf(long)}
   *  instead.
   *
   */
  def fromLongBits(bits: Long): ULong = new ULong(bits)

  /** Returns an {@code ULong} representing the same value as the specified {@code Long}.
   *
   *  @throws IllegalArgumentException if {@code value} is negative
   */
  def valueOf(value: Long): ULong = {
    checkArgument(value >= 0, "value (%s) is outside the range for an unsigned long value", value)
    fromLongBits(value)
  }

  /** Returns a {@code ULong} representing the same value as the specified
   *  {@code BigInteger}. This is the inverse operation of `#toBigInt`.
   *
   *  @throws IllegalArgumentException if {@code value} is negative or {@code value >= 2^64}
   */
  def valueOf(value: BigInt): ULong = {
    checkNotNull(value)
    checkArgument(value.signum >= 0 && value.bitLength <= java.lang.Long.SIZE,
      "value (%s) is outside the range for an unsigned long value", value)
    fromLongBits(value.longValue())
  }

  /** Returns an {@code ULong} holding the value of the specified {@code String}, parsed as
   *  an {@code ULong} value.
   *
   *  @throws NumberFormatException if the string does not contain a parsable {@code ULong}
   *         value
   */
  def valueOf(string: String): ULong = fromLongBits(UnsignedLongs.parseUnsignedLong(string))

  /** Returns an {@code ULong} holding the value of the specified {@code String}, parsed as
   *  an {@code ULong} value in the specified radix.
   *
   *  @throws NumberFormatException if the string does not contain a parsable {@code ULong}
   *         value, or {@code radix} is not between `Character#MIN_RADIX` and
   *         `Character#MAX_RADIX`
   */
  def valueOf(string: String, radix: Int): ULong = fromLongBits(UnsignedLongs.parseUnsignedLong(string, radix))

  /** Compares the two specified {@code Long} values, treating them as unsigned values between
   *  {@code 0} and {@code 2^64 - 1} inclusive.
   *
   *  @param a the first {@code ULong} to compare
   *  @param b the second {@code ULong} to compare
   *  @return a negative value if {@code a} is less than {@code b}; a positive value if {@code a} is
   *         greater than {@code b}; or zero if they are equal
   */
  def compare(a: ULong, b: ULong): Int = UnsignedLongs.compare(a.value, b.value)

  /** Returns the least value present in {@code array}, treating values as unsigned.
   *
   *  @param array a <i>nonempty</i> array of {@code ULong} values
   *  @return the value present in {@code array} that is less than or equal to every other value in
   *         the array according to `#compare`
   *  @throws IllegalArgumentException if {@code array} is empty
   */
  def min(array: ULong*): ULong = {
    checkArgument(array.length > 0)
    val it = array.iterator
    var min = flip(it.next.value)
    while (it.hasNext) {
      val next = flip(it.next.value)
      if (next < min)
        min = next
    }
    fromLongBits(flip(min))
  }

  /** Returns the greatest value present in {@code array}, treating values as unsigned.
   *
   *  @param array a <i>nonempty</i> array of {@code ULong} values
   *  @return the value present in {@code array} that is greater than or equal to every other value
   *         in the array according to `#compare`
   *  @throws IllegalArgumentException if {@code array} is empty
   */
  def max(array: ULong*): ULong = {
    checkArgument(array.length > 0)
    val it = array.iterator
    var max = flip(it.next.value)
    while (it.hasNext) {
      val next = flip(it.next.value)
      if (next > max) {
        max = next
      }
    }
    fromLongBits(flip(max))
  }

  /** Returns a string containing the supplied {@code ULong} values separated by
   *  {@code separator}. For example, {@code join("-", 1, 2, 3)} returns the string {@code "1-2-3"}.
   *
   *  @param separator the text that should appear between consecutive values in the resulting
   *        string (but not at the start or end)
   *  @param array an array of {@code ULong} values, possibly empty
   */
  def join(separator: String, array: ULong*): String = {
    checkNotNull(separator)
    if (array.length == 0) {
      ""
    } else {
      val it = array.iterator
      val builder = new StringBuilder(array.length * 5)
      builder.append(it.next.toString)
      while (it.hasNext) {
        builder.append(separator).append(it.next.toString)
      }
      builder.toString
    }
  }

  /** Returns a comparator that compares two arrays of {@code ULong} values
   *  lexicographically. That is, it compares, using `#compare(long, long)`, the first pair of
   *  values that follow any common prefix, or when one array is a prefix of the other, treats the
   *  shorter array as the lesser. For example, {@code [] < [1L] < [1L, 2L] < [2L] < [1L << 63]}.
   *
   *  @see <a href="http://en.wikipedia.org/wiki/Lexicographical_order">Lexicographical order
   *      article at Wikipedia</a>
   */
  def lexicographicalComparator(): Ordering[Array[ULong]] = LexicographicalComparator

  private[this] object LexicographicalComparator extends Ordering[Array[ULong]] {
    override def compare(left: Array[ULong], right: Array[ULong]): Int = {
      val minLength = Math.min(left.length, right.length)
      var i = 0
      while (i < minLength) {
        if (left(i) != right(i)) {
          return ULong.compare(left(i), right(i))
        }
        i = i + 1
      }
      return left.length - right.length
    }
  }

  /** Returns the {@code ULong} value represented by the given string.
   *
   *  Accepts a decimal, hexadecimal, or octal number given by specifying the following prefix:
   *
   *  <ul>
   *  <li>{@code 0x}<i>HexDigits</i>
   *  <li>{@code 0X}<i>HexDigits</i>
   *  <li>{@code #}<i>HexDigits</i>
   *  <li>{@code 0}<i>OctalDigits</i>
   *  </ul>
   *
   *  @throws NumberFormatException if the string does not contain a valid {@code ULong}
   *         value
   */
  def decode(stringValue: String): ULong = fromLongBits(UnsignedLongs.decode(stringValue))

  /** Ordering that compares the two specified {@code ULong} values.
   */
  implicit object ULongOrdering extends Ordering[ULong] {
    def compare(x: ULong, y: ULong) = UnsignedLongs.compare(x.value, y.value)
  }

  /** Adds an `asJava` method that converts a Mango `ULong` to a
   *  Guava `UnsignedLong`.
   *
   *  The returned Guava `UnsignedLong` contains a copy of the bits
   *  from Mango `ULong`.
   *
   *  @param ULong the Mango `ULong` to convert to a Guava `UnsignedLong`
   *  @return An object with an `asJava` method that returns a Guava `UnsignedLong`
   *   view of the argument
   */
  implicit def asGuavaULong(ulong: ULong): AsJava[UnsignedLong] = {
    new AsJava(UnsignedLong.fromLongBits(ulong.value))
  }

  /** Adds an `asScala` method that converts a Guava `UnsignedLong` to a
   *  Mango `ULong`.
   *
   *  The returned Mango `ULong` contains a copy of the bits
   *  from Guava `UnsignedLong`.
   *
   *  @param ULong the Guava `UnsignedLong` to convert to a Mango `ULong`
   *  @return An object with an `asScala` method that returns a Mango `ULong`
   *   view of the argument
   */
  implicit def asMangoULong(ulong: UnsignedLong): AsScala[ULong] = {
    new AsScala(fromLongBits(ulong.longValue()))
  }

  /** A (self-inverse) bijection which converts the ordering on unsigned longs to the ordering on
   *  longs, that is, {@code a <= b} as unsigned longs if and only if {@code flip(a) <= flip(b)}
   *  as signed longs.
   */
  private def flip(a: Long): Long = a ^ Long.MinValue
}
