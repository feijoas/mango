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

import org.feijoas.mango.common.base.Preconditions.checkArgument
import org.feijoas.mango.common.base.Preconditions.checkNotNull
import org.feijoas.mango.common.convert.AsJava
import org.feijoas.mango.common.convert.AsScala

import com.google.common.primitives.UnsignedInteger
import com.google.common.primitives.UnsignedInts

/** Static utility methods pertaining to {@code UInt} value classes that interpret values as
 *  <i>unsigned</i> (that is, any negative value {@code x} is treated as the positive value
 *  {@code 2^32 + x}).
 *
 *  <p>See the Guava User Guide article on
 *  [[http://code.google.com/p/guava-libraries/wiki/PrimitivesExplained#Unsigned_support unsigned primitive utilities]].
 *
 *  @author Markus Schneider
 *  @since 0.10 (copied from Guava-libraries)
 */
final object UInt {
  private val intMask = 0xffffffffL

  /** Maximum value an UInt can represent
   */
  val MaxValue = fromIntBits(-1)

  /** Returns an {@code UInt} that is equal to {@code value},
   *  if possible. The inverse operation of {@code #toLong}.
   *
   *  Equivalent to `valueOf(value)`
   */
  def apply(value: Long): UInt = valueOf(value)

  /** Returns a {@code UInt} representing the same value as the specified
   *  {@code BigInteger}. This is the inverse operation of {@code #toBigInt()}.
   *
   *  Equivalent to `valueOf(value)`
   *
   *  @throws IllegalArgumentException if {@code value} is negative or {@code value >= 2^32}
   */
  def apply(value: BigInt): UInt = valueOf(value)

  /** Returns an {@code UInt} corresponding to a given bit representation.
   *  The argument is interpreted as an unsigned 32-bit value. Specifically, the sign bit
   *  of {@code bits} is interpreted as a normal bit, and all other bits are treated as usual.
   *
   *  <p>If the argument is nonnegative, the returned result will be equal to {@code bits},
   *  otherwise, the result will be equal to {@code 2^32 + bits}.
   *
   *  <p>To represent unsigned decimal constants, consider {@code #valueOf(Long)} instead.
   */
  def fromIntBits(bits: Int): UInt = new UInt(bits & 0xffffffff)

  /** Compares the two specified {@code UInt} values.
   *
   *  @param a the first {@code UInt} to compare
   *  @param b the second {@code UInt} to compare
   *  @return a negative value if {@code a} is less than {@code b}; a positive value if {@code a} is
   *         greater than {@code b}; or zero if they are equal
   */
  def compare(a: UInt, b: UInt): Int = UnsignedInts.compare(a.value, b.value)

  /** Returns an {@code UInt} that is equal to {@code value},
   *  if possible. The inverse operation of {@code #toLong}.
   */
  def valueOf(value: Long): UInt = {
    checkArgument((value & intMask) == value, "value (%s) is outside the range for an unsigned integer value", value)
    fromIntBits(value.toInt)
  }

  /** Returns a {@code UInt} representing the same value as the specified
   *  {@code BigInteger}. This is the inverse operation of {@code #toBigInt()}.
   *
   *  @throws IllegalArgumentException if {@code value} is negative or {@code value >= 2^32}
   */
  def valueOf(value: BigInt): UInt = {
    checkNotNull(value)
    checkArgument(value.signum >= 0 && value.bitLength <= Integer.SIZE, "value (%s) is outside the range for an unsigned integer value", value)
    fromIntBits(value.intValue)
  }

  /** Returns the {@code UInt} value represented by the given decimal string.
   *
   *  @throws NumberFormatException if the string does not contain a valid unsigned {@code Int} value
   *  @throws NullPointerException if {@code string} is null
   */
  def valueOf(string: String): UInt = fromIntBits(UnsignedInts.parseUnsignedInt(string))

  /** Returns the {@code UInt} value represented by a string with the given radix.
   *
   *  @param string the string containing the unsigned integer representation to be parsed.
   *  @param radix the radix to use while parsing {@code string}; must be between
   *        {@code Character#MIN_RADIX} and {@code Character#MAX_RADIX}.
   *  @throws NumberFormatException if the string does not contain a valid unsigned {@code Int}, or
   *         if supplied radix is invalid.
   *  @throws NullPointerException if {@code string} is null
   */
  def valueOf(string: String, radix: Int): UInt = fromIntBits(UnsignedInts.parseUnsignedInt(string, radix))

  /** Returns the {@code UInt} value represented by the given string.
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
   *  @throws NumberFormatException if the string does not contain a valid unsigned {@code Int} value
   */
  def decode(stringValue: String): UInt = fromIntBits(UnsignedInts.decode(stringValue))

  /** Returns the least value present in {@code array}, treating values as unsigned.
   *
   *  @param array a <i>nonempty</i> array of unsigned {@code int} values
   *  @return the value present in {@code array} that is less than or equal to every other value in
   *         the array according to {@code #compare}
   *  @throws IllegalArgumentException if {@code array} is empty
   */
  def min(array: UInt*): UInt = {
    checkArgument(array.length > 0)
    val it = array.iterator
    var min = flip(it.next.value)
    while (it.hasNext) {
      val next = flip(it.next.value)
      if (next < min)
        min = next
    }
    fromIntBits(flip(min))
  }

  /** Returns the greatest value present in {@code array}, treating values as unsigned.
   *
   *  @param array a <i>nonempty</i> array of unsigned {@code int} values
   *  @return the value present in {@code array} that is greater than or equal to every other value
   *         in the array according to {@code #compare}
   *  @throws IllegalArgumentException if {@code array} is empty
   */
  def max(array: UInt*): UInt = {
    checkArgument(array.length > 0)
    val it = array.iterator
    var max = flip(it.next.value)
    while (it.hasNext) {
      val next = flip(it.next.value)
      if (next > max) {
        max = next
      }
    }
    fromIntBits(flip(max))
  }

  /** Returns a string containing the supplied {@code UInt} values separated by
   *  {@code separator}. For example, {@code join("-", 1, 2, 3)} returns the string {@code "1-2-3"}.
   *
   *  @param separator the text that should appear between consecutive values in the resulting
   *        string (but not at the start or end)
   *  @param array an array of unsigned {@code UInt} values, possibly empty
   */
  def join(separator: String, array: UInt*): String = {
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

  /** Returns a comparator that compares two arrays of {@code UInt} values lexicographically.
   *  That is, it compares, using {@code #compare(UInt, UInt)}), the first pair of values that follow
   *  any common prefix, or when one array is a prefix of the other, treats the shorter array as the
   *  lesser. For example, {@code [] < [1] < [1, 2] < [2] < [1 << 31]}.
   *
   *  <p>The returned comparator is inconsistent with {@code Any#equals(OAny)} (since arrays
   *  support only identity equality), but it is consistent with {@code Arrays#equals(UInt[], UInt[])}.
   *
   *  @see <a href="http://en.wikipedia.org/wiki/Lexicographical_order"> Lexicographical order
   *      article at Wikipedia</a>
   */
  def lexicographicalComparator(): Ordering[Array[UInt]] = LexicographicalComparator

  private[this] object LexicographicalComparator extends Ordering[Array[UInt]] {
    override def compare(left: Array[UInt], right: Array[UInt]): Int = {
      val minLength = Math.min(left.length, right.length)
      var i = 0
      while (i < minLength) {
        if (left(i) != right(i)) {
          return UInt.compare(left(i), right(i))
        }
        i = i + 1
      }
      return left.length - right.length
    }
  }

  /** Ordering that compares the two specified {@code UInt} values.
   */
  implicit object UIntOrdering extends Ordering[UInt] {
    def compare(x: UInt, y: UInt) = UnsignedInts.compare(x.value, y.value)
  }

  /** Adds an `asJava` method that converts a Mango `UInt` to a
   *  Guava `UnsignedInteger`.
   *
   *  The returned Guava `UnsignedInteger` contains a copy of the bits
   *  from Mango `UInt`.
   *
   *  @param uint the Mango `UInt` to convert to a Guava `UnsignedInteger`
   *  @return An object with an `asJava` method that returns a Guava `UnsignedInteger`
   *   view of the argument
   */
  implicit def asGuavaUInt(uint: UInt): AsJava[UnsignedInteger] = {
    new AsJava(UnsignedInteger.fromIntBits(uint.value))
  }

  /** Adds an `asScala` method that converts a Guava `UnsignedInteger` to a
   *  Mango `UInt`.
   *
   *  The returned Mango `UInt` contains a copy of the bits
   *  from Guava `UnsignedInteger`.
   *
   *  @param uint the Guava `UnsignedInteger` to convert to a Mango `UInt`
   *  @return An object with an `asScala` method that returns a Mango `UInt`
   *   view of the argument
   */
  implicit def asMangoUInt(uint: UnsignedInteger): AsScala[UInt] = {
    new AsScala(fromIntBits(uint.intValue()))
  }

  /** A (self-inverse) bijection which converts the ordering on unsigned ints to the ordering on
   *  longs, that is, {@code a <= b} as unsigned ints if and only if {@code flip(a) <= flip(b)}
   *  as signed ints.
   */
  private[this] def flip(value: Int): Int = value ^ Int.MinValue
}

/** A value class for unsigned {@code Int} values, supporting arithmetic operations.
 *
 *  <p>See the Guava User Guide article on
 *  [[http://code.google.com/p/guava-libraries/wiki/PrimitivesExplained#Unsigned_support unsigned primitive utilities]].
 *
 *  @author Markus Schneider
 *  @since 0.10 (copied from Guava-libraries)
 */
@SerialVersionUID(1L)
final class UInt private (val value: Int) extends AnyVal with Ordered[UInt] with Serializable {

  import UInt._

  /** Returns the result of adding this and {@code that}. If the result would have more than 32 bits,
   *  returns the low 32 bits of the result.
   *
   */
  def +(that: UInt): UInt = fromIntBits(this.value + checkNotNull(that).value)

  /** Returns the result of subtracting this and {@code that}. If the result would be negative,
   *  returns the low 32 bits of the result.
   */
  def -(that: UInt): UInt = fromIntBits(value - checkNotNull(that).value)

  /** Returns the result of multiplying this and {@code that}. If the result would have more than 32
   *  bits, returns the low 32 bits of the result.
   */
  def *(that: UInt): UInt = fromIntBits(value * checkNotNull(that).value)

  /** Returns the result of dividing this by {@code that}.
   *
   *  @throws ArithmeticException if {@code that} is zero
   */
  def /(that: UInt): UInt = fromIntBits(UnsignedInts.divide(value, that.value))

  /** Returns this mod {@code that}.
   *
   *  @throws ArithmeticException if {@code that} is zero
   */
  def mod(that: UInt): UInt = fromIntBits(UnsignedInts.remainder(value, that.value))

  /** Compares this unsigned Int to another unsigned Int.
   *  Returns {@code 0} if they are equal, a negative number if {@code this < other},
   *  and a positive number if {@code this > other}.
   */
  override def compare(that: UInt): Int = UnsignedInts.compare(this.value, that.value)

  /** Returns a string representation of the {@code UInt} value, in base 10.
   */
  override def toString() = UnsignedInts.toString(value)

  /** Returns a string representation of the {@code UInt} value, in base {@code radix}.
   *  If {@code radix < Character.MIN_RADIX} or {@code radix > Character.MAX_RADIX}, the radix
   *  {@code 10} is used.
   */
  def toString(radix: Int) = UnsignedInts.toString(value, radix)

  /** Returns the value of this {@code UInt} as a {@code Long}, when treated as unsigned.
   */
  def toLong(): Long = value & intMask

  /** Returns the value of this {@code UInt} as an {@code Int}. This is an inverse
   *  operation to {@code #fromIntBits}.
   *
   *  <p>Note that if this {@code UInt} holds a value {@code >= 2^31}, the returned value
   *  will be equal to {@code this - 2^32}.
   */
  def toInt(): Int = value

  /** Returns the value of this {@code UInt} as a {@code Float}, analogous to a widening
   *  primitive conversion from {@code Int} to {@code Float}, and correctly rounded.
   */
  def toFloat(): Float = toLong

  /** Returns the value of this {@code UInt} as a {@code Double}, analogous to a widening
   *  primitive conversion from {@code Int} to {@code Double}, and correctly rounded.
   */
  def toDouble(): Double = toLong

  /** Returns the value of this {@code UInt} as a {@code BigInt}.
   */
  def toBigInt() = BigInt(toLong)
}