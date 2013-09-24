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

import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Preconditions.checkNotNull
import com.google.common.primitives.UnsignedBytes

/** A value class for unsigned {@code Byte} values, supporting arithmetic operations.
 *
 *  <p>See the Guava User Guide article on
 *  [[http://code.google.com/p/guava-libraries/wiki/PrimitivesExplained#Unsigned_support unsigned primitive utilities]].
 *
 *  @author Markus Schneider
 *  @since 0.10 (copied from Guava-libraries)
 */
@SerialVersionUID(1L)
final class UByte private (val value: Byte) extends AnyVal with Ordered[UByte] with Serializable {

  /** Returns the value of the given Byte as an integer, when treated as
   *  unsigned. That is, returns {@code value + 256} if {@code value} is
   *  negative; {@code value} itself otherwise.
   */
  def toInt(): Int = UnsignedBytes.toInt(this.value)

  /** Returns the value of this {@code UByte} as a {@code Long}. This is an inverse operation.
   */
  def toLong(): Long = toInt

  /** Returns the value of this {@code UByte} as a {@code Float}, analogous to a widening
   *  primitive conversion from {@code Byte} to {@code Float}, and correctly rounded.
   */
  def toFloat(): Float = toLong

  /** Returns the value of this {@code UByte} as a {@code Double}, analogous to a widening
   *  primitive conversion from {@code Byte} to {@code Double}, and correctly rounded.
   */
  def toDouble(): Double = toLong

  /** Returns the value of this {@code UInt} as a {@code BigInt}.
   */
  def toBigInt() = BigInt(toLong)

  /** Returns a string representation of x, where x is treated as unsigned.
   */
  override def toString() = UnsignedBytes.toString(value)

  /** Returns a string representation of {@code x} for the given radix, where {@code x} is treated
   *  as unsigned.
   *
   *  @param x the value to convert to a string.
   *  @param radix the radix to use while working with {@code x}
   *  @throws IllegalArgumentException if {@code radix} is not between {@code Character#MIN_RADIX}
   *         and {@code Character#MAX_RADIX}.
   */
  def toString(radix: Int) = UnsignedBytes.toString(value, radix)

  /** Compares this unsigned Byte to another unsigned Byte.
   *  Returns {@code 0} if they are equal, a negative number if {@code this < other},
   *  and a positive number if {@code this > other}.
   */
  override def compare(that: UByte): Int = UnsignedBytes.compare(this.value, that.value)
}

/** Static utility methods pertaining to {@code byte} primitives that interpret
 *  values as <i>unsigned</i> (that is, any negative value {@code b} is treated
 *  as the positive value {@code 256 + b}).
 *
 *  <p>See the Guava User Guide article on
 *  [[http://code.google.com/p/guava-libraries/wiki/PrimitivesExplained#Unsigned_support unsigned primitive utilities]].
 *
 *  @author Markus Schneider
 *  @since 0.10 (copied from Guava-libraries)
 */
final object UByte {

  /** The largest power of two that can be represented as an unsigned {@code
   *  Byte}.
   */
  val MaxPowerOfTwo = 0x80.toByte

  /** The largest value that fits into an unsigned Byte.
   */
  val MaxValue = 0xFF.toByte

  /** Returns an {@code UByte} corresponding to a given bit representation.
   *  The argument is interpreted as an unsigned byte value. Specifically, the sign bit
   *  of {@code bits} is interpreted as a normal bit, and all other bits are treated as usual.
   *
   *  <p>If the argument is nonnegative, the returned result will be equal to {@code bits},
   *  otherwise, the result will be equal to {@code bits + 256}.
   */
  def fromByteBits(bits: Byte): UByte = new UByte(bits)

  /** Returns the {@code UByte} value that, when treated as unsigned, is equal to
   *  {@code value}, if possible.
   *
   *  Equivalent to `valueOf(value)`
   *
   *  @param value a value between 0 and 255 inclusive
   *  @return the {@code UByte} value that, when treated as unsigned, equals
   *     {@code value}
   *  @throws IllegalArgumentException if {@code value} is negative or greater
   *     than 255
   */
  def apply(value: Long): UByte = valueOf(value)

  /** Returns the {@code UByte} value that, when treated as unsigned, is equal to
   *  {@code value}, if possible.
   *
   *  @param value a value between 0 and 255 inclusive
   *  @return the {@code UByte} value that, when treated as unsigned, equals
   *     {@code value}
   *  @throws IllegalArgumentException if {@code value} is negative or greater
   *     than 255
   */
  def valueOf(value: Long): UByte = new UByte(UnsignedBytes.checkedCast(value))

  /** Returns the unsigned {@code UByte} value represented by the given decimal string.
   *
   *  @throws NumberFormatException if the string does not contain a valid unsigned {@code UByte}
   *         value
   *  @throws NullPointerException if {@code s} is null
   *         (in contrast to {@code Byte#parseByte(String)})
   */
  def valueOf(string: String): UByte = new UByte(UnsignedBytes.parseUnsignedByte(string))

  /** Returns the unsigned {@code UByte} value represented by a string with the given radix.
   *
   *  @param string the string containing the unsigned {@code UByte} representation to be parsed.
   *  @param radix the radix to use while parsing {@code string}
   *  @throws NumberFormatException if the string does not contain a valid unsigned {@code UByte}
   *         with the given radix, or if {@code radix} is not between {@code Character#MIN_RADIX}
   *         and {@code Character#MAX_RADIX}.
   *  @throws NullPointerException if {@code s} is null
   *         (in contrast to {@code Byte#parseByte(String)})
   */
  def valueOf(string: String, radix: Int): UByte = new UByte(UnsignedBytes.parseUnsignedByte(string, radix))

  /** Returns the {@code UByte} value that, when treated as unsigned, is nearest
   *  in value to {@code value}.
   *
   *  @param value any {@code long} value
   *  @return {@code (Byte) 255} if {@code value >= 255}, {@code (Byte) 0} if
   *     {@code value <= 0}, and {@code value} cast to {@code UByte} otherwise
   */
  def saturatedValueOf(value: Long): UByte = new UByte(UnsignedBytes.saturatedCast(value))

  /** Compares the two specified {@code UByte} values, treating them as unsigned
   *  values between 0 and 255 inclusive. For example, {@code (Byte) -127} is
   *  considered greater than {@code (Byte) 127} because it is seen as having
   *  the value of positive {@code 129}.
   *
   *  @param a the first {@code UByte} to compare
   *  @param b the second {@code UByte} to compare
   *  @return a negative value if {@code a} is less than {@code b}; a positive
   *     value if {@code a} is greater than {@code b}; or zero if they are equal
   */
  def compare(a: UByte, b: UByte): Int = UnsignedBytes.compare(a.value, b.value)

  /** Returns the least value present in {@code array}.
   *
   *  @param array a <i>nonempty</i> array of {@code UByte} values
   *  @return the value present in {@code array} that is less than or equal to
   *     every other value in the array
   *  @throws IllegalArgumentException if {@code array} is empty
   */
  def min(array: UByte*): UByte = {
    checkArgument(array.length > 0)
    val it = array.iterator
    var min = it.next.toInt
    while (it.hasNext) {
      val next = it.next.toInt
      if (next < min)
        min = next
    }
    valueOf(min)
  }

  /** Returns the greatest value present in {@code array}.
   *
   *  @param array a <i>nonempty</i> array of {@code UByte} values
   *  @return the value present in {@code array} that is greater than or equal
   *     to every other value in the array
   *  @throws IllegalArgumentException if {@code array} is empty
   */
  def max(array: UByte*): UByte = {
    checkArgument(array.length > 0)
    val it = array.iterator
    var max = it.next.toInt
    while (it.hasNext) {
      val next = it.next.toInt
      if (next > max)
        max = next
    }
    UByte(max)
  }

  /** Returns a string containing the supplied {@code UByte} values separated by
   *  {@code separator}. For example, {@code join(":", (Byte) 1, (Byte) 2,
   *  (Byte) 255)} returns the string {@code "1:2:255"}.
   *
   *  @param separator the text that should appear between consecutive values in
   *     the resulting string (but not at the start or end)
   *  @param array an array of {@code UByte} values, possibly empty
   */
  def join(separator: String, array: UByte*): String = {
    checkNotNull(separator)
    if (array.length == 0) {
      ""
    } else {
      val it = array.iterator
      val builder = new StringBuilder(array.length * (3 + separator.length()))
      builder.append(it.next.toString)
      while (it.hasNext) {
        builder.append(separator).append(it.next.toString)
      }
      builder.toString
    }
  }

  /** Returns a comparator that compares two {@code UByte} arrays
   *  lexicographically. That is, it compares, using {@code
   *  #compare(Byte, Byte)}), the first pair of values that follow any common
   *  prefix, or when one array is a prefix of the other, treats the shorter
   *  array as the lesser. For example, {@code [] < [0x01] < [0x01, 0x7F] <
   *  [0x01, 0x80] < [0x02]}. Values are treated as unsigned.
   *
   *  <p>The returned comparator is inconsistent with {@code
   *  Object#equals(Object)} (since arrays support only identity equality), but
   *  it is consistent with {@code java.util.Arrays#equals(Byte[], Byte[])}.
   *
   *  @see <a href="http://en.wikipedia.org/wiki/Lexicographical_order">
   *     Lexicographical order article at Wikipedia</a>
   */
  def lexicographicalComparator(): Ordering[Array[UByte]] = UByteArrayOrdering

  private[this] final object UByteArrayOrdering extends Ordering[Array[UByte]] {
    override def compare(left: Array[UByte], right: Array[UByte]): Int = {
      val minLength = Math.min(left.length, right.length)
      var i = 0
      while (i < minLength) {
        val result = UnsignedBytes.compare(left(i).value, right(i).value)
        if (result != 0)
          return result
        i = i + 1
      }
      return left.length - right.length
    }
  }

  /** Ordering that compares the two specified {@code UByte} values.
   */
  implicit final object UByteOrdering extends Ordering[UByte] {
    override def compare(x: UByte, y: UByte) = UnsignedBytes.compare(x.value, y.value)
  }
}