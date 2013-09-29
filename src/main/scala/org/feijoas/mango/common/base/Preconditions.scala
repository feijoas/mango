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
package org.feijoas.mango.common.base

import javax.annotation.Nullable

/** Simple static methods to be called at the start of your own methods to verify
 *  correct arguments and state. This allows constructs such as
 *
 *  {{{
 *    if (count <= 0) {
 *        throw new IllegalArgumentException("must be positive: " + count)
 *    }
 *  }}}
 *
 *  to be replaced with the more compact
 *  {{{
 *  checkArgument(count > 0, "must be positive: %s", count)
 *  }}}
 *
 *  Note that the sense of the expression is inverted with [[Preconditions]]
 *  you declare what you expect to be ''true'', just as you do with an
 *  <a href="http://java.sun.com/j2se/1.5.0/docs/guide/language/assert.html">
 *  {@code assert}</a> or a JUnit {@code assertTrue} call.
 *
 *  <p><b>Warning:</b> only the {@code "%s"} specifier is recognized as a
 *  placeholder in these messages.
 *
 *  <p>Take care not to confuse precondition checking with other similar types
 *  of checks! Precondition exceptions -- including those provided here, but also
 *  {@link IndexOutOfBoundsException}, {@link NoSuchElementException}, {@link
 *  UnsupportedOperationException} and others -- are used to signal that the
 *  <i>calling method</i> has made an error. This tells the caller that it should
 *  not have invoked the method when it did, with the arguments it did, or
 *  perhaps ever. Postcondition or other invariant failures should not throw
 *  these types of exceptions.
 *
 *  See also <a href=
 *  "http://code.google.com/p/guava-libraries/wiki/PreconditionsExplained">
 *  the Guava User Guide
 *  </a> on using [[Preconditions]]
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from guava-libraries)
 */
final object Preconditions {

  /** Ensures the truth of an expression involving one or more parameters to the
   *  calling method.
   *
   *  @param expression an expression: Boolean
   *  @throws IllegalArgumentException if {@code expression} is false
   */
  def checkArgument(expression: Boolean) = {
    if (!expression)
      throw new IllegalArgumentException()
  }

  /** Ensures the truth of an expression involving one or more parameters to the
   *  calling method.
   *
   *  @param expression an expression: Boolean
   *  @param errorMessage the exception message to use if the check fails will
   *     be converted to a string using `String#valueOf(Any)`
   *  @throws IllegalArgumentException if {@code expression} is false
   */
  def checkArgument(expression: Boolean, errorMessage: Any) = {
    if (!expression)
      throw new IllegalArgumentException(String.valueOf(errorMessage))
  }

  /** Ensures the truth of an expression involving one or more parameters to the
   *  calling method.
   *
   *  @param expression an expression: Boolean
   *  @param errorMessageTemplate a template for the exception message should the
   *     check fail. The message is formed by replacing each {@code %s}
   *     placeholder in the template with an argument. These are matched by
   *     position - the first {@code %s} gets {@code errorMessageArgs[0]}, etc.
   *     Unmatched arguments will be appended to the formatted message in square
   *     braces. Unmatched placeholders will be left as-is. Arguments are converted
   *     to strings using `String#valueOf(Any)`.
   *  @param errorMessageArg the first argument to be substituted into the message
   *     template.
   *  @param moreArgs the other arguments to be substituted into the message
   *     template.
   *  @throws IllegalArgumentException if {@code expression} is false
   *  @throws NullPointerException if the check fails and either {@code
   *     errorMessageTemplate} or {@code errorMessageArgs} is null (don't let
   *     this happen)
   */
  def checkArgument(expression: Boolean,
                    errorMessageTemplate: String,
                    errorMessageArg: Any,
                    moreArgs: Any*) = {
    if (!expression)
      throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageArg +: moreArgs: _*))
  }

  /** Ensures the truth of an expression involving the state of the calling
   *  instance, but not involving any parameters to the calling method.
   *
   *  @param expression an expression: Boolean
   *  @throws IllegalStateException if {@code expression} is false
   */
  def checkState(expression: Boolean) = {
    if (!expression)
      throw new IllegalStateException()
  }

  /** Ensures the truth of an expression involving the state of the calling
   *  instance, but not involving any parameters to the calling method.
   *
   *  @param expression an expression: Boolean
   *  @param errorMessage the exception message to use if the check fails will
   *     be converted to a string using `String#valueOf(Any)`
   *  @throws IllegalStateException if {@code expression} is false
   */
  def checkState(expression: Boolean, errorMessage: Any) = {
    if (!expression)
      throw new IllegalStateException(String.valueOf(errorMessage))
  }

  /** Ensures the truth of an expression involving the state of the calling
   *  instance, but not involving any parameters to the calling method.
   *
   *  @param expression a expression: Boolean
   *  @param errorMessageTemplate a template for the exception message should the
   *     check fail. The message is formed by replacing each {@code %s}
   *     placeholder in the template with an argument. These are matched by
   *     position - the first {@code %s} gets {@code errorMessageArgs[0]}, etc.
   *     Unmatched arguments will be appended to the formatted message in square
   *     braces. Unmatched placeholders will be left as-is. Arguments are converted
   *     to strings using `String#valueOf(Any)`.
   *  @param errorMessageArg the first argument to be substituted into the message
   *     template.
   *  @param moreArgs the other arguments to be substituted into the message
   *     template.
   *  @throws IllegalStateException if {@code expression} is false
   *  @throws NullPointerException if the check fails and either {@code
   *     errorMessageTemplate} or {@code errorMessageArgs} is null (don't let
   *     this happen)
   */
  def checkState(expression: Boolean,
                 errorMessageTemplate: String,
                 errorMessageArg: Any,
                 moreArgs: Any*) = {
    if (!expression)
      throw new IllegalStateException(format(errorMessageTemplate, errorMessageArg +: moreArgs: _*))
  }

  /** Ensures that an objecreference: T passed as a parameter to the calling
   *  method is not null.
   *
   *  @param reference an objecreference: T
   *  @return the non-null reference that was validated
   *  @throws NullPointerException if {@code reference} is null
   */
  def checkNotNull[T](reference: T): T = {
    if (reference == null) {
      throw new NullPointerException()
    }
    return reference
  }

  /** Ensures that an objecreference: T passed as a parameter to the calling
   *  method is not null.
   *
   *  @param reference an objecreference: T
   *  @param errorMessage the exception message to use if the check fails will
   *     be converted to a string using `String#valueOf(Any)`
   *  @return the non-null reference that was validated
   *  @throws NullPointerException if {@code reference} is null
   */
  def checkNotNull[T](reference: T, errorMessage: Any): T = {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage))
    }
    return reference
  }

  /** Ensures that an objecreference: T passed as a parameter to the calling
   *  method is not null.
   *
   *  @param reference an objecreference: T
   *  @param errorMessageTemplate a template for the exception message should the
   *     check fail. The message is formed by replacing each {@code %s}
   *     placeholder in the template with an argument. These are matched by
   *     position - the first {@code %s} gets {@code errorMessageArgs[0]}, etc.
   *     Unmatched arguments will be appended to the formatted message in square
   *     braces. Unmatched placeholders will be left as-is. Arguments are converted
   *     to strings using `String#valueOf(Any)`.
   *  @param errorMessageArg the first argument to be substituted into the message
   *     template.
   *  @param moreArgs the other arguments to be substituted into the message
   *     template.
   *  @return the non-null reference that was validated
   *  @throws NullPointerException if {@code reference} is null
   */
  def checkNotNull[T](reference: T,
                      errorMessageTemplate: String,
                      errorMessageArg: Any,
                      moreArgs: Any*): T = {
    if (reference == null) {
      throw new NullPointerException(format(errorMessageTemplate, errorMessageArg +: moreArgs: _*))
    }
    return reference
  }

  /** Ensures that {@code index} specifies a valid <i>element</i> in an array,
   *  list or string of size {@code size}. An element index may range from zero,
   *  inclusive, to {@code size}, exclusive.
   *
   *  @param index a user-supplied index identifying an element of an array, list
   *     or string
   *  @param size the size of that array, list or string
   *  @return the value of {@code index}
   *  @throws IndexOutOfBoundsException if {@code index} is negative or is not
   *     less than {@code size}
   *  @throws IllegalArgumentException if {@code size} is negative
   */
  def checkElementIndex(index: Int, size: Int): Int = {
    return checkElementIndex(index, size, "index")
  }

  /** Ensures that {@code index} specifies a valid <i>element</i> in an array,
   *  list or string of size {@code size}. An element index may range from zero,
   *  inclusive, to {@code size}, exclusive.
   *
   *  @param index a user-supplied index identifying an element of an array, list
   *     or string
   *  @param size the size of that array, list or string
   *  @param desc the text to use to describe this index in an error message
   *  @return the value of {@code index}
   *  @throws IndexOutOfBoundsException if {@code index} is negative or is not
   *     less than {@code size}
   *  @throws IllegalArgumentException if {@code size} is negative
   */
  def checkElementIndex(
    index: Int, size: Int, @Nullable desc: String): Int = {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException(badElementIndex(index, size, desc))
    }
    return index
  }

  private def badElementIndex(index: Int, size: Int, desc: String): String = {
    if (index < 0) {
      return format("%s (%s) must not be negative", desc, index)
    } else if (size < 0) {
      throw new IllegalArgumentException("negative size: " + size)
    } else { // index >= size
      return format("%s (%s) must be less than size (%s)", desc, index, size)
    }
  }

  /** Ensures that {@code index} specifies a valid <i>position</i> in an array,
   *  list or string of size {@code size}. A position index may range from zero
   *  to {@code size}, inclusive.
   *
   *  @param index a user-supplied index identifying a position in an array, list
   *     or string
   *  @param size the size of that array, list or string
   *  @return the value of {@code index}
   *  @throws IndexOutOfBoundsException if {@code index} is negative or is
   *     greater than {@code size}
   *  @throws IllegalArgumentException if {@code size} is negative
   */
  def checkPositionIndex(index: Int, size: Int): Int = {
    checkPositionIndex(index, size, "index")
  }

  /** Ensures that {@code index} specifies a valid <i>position</i> in an array,
   *  list or string of size {@code size}. A position index may range from zero
   *  to {@code size}, inclusive.
   *
   *  @param index a user-supplied index identifying a position in an array, list
   *     or string
   *  @param size the size of that array, list or string
   *  @param desc the text to use to describe this index in an error message
   *  @return the value of {@code index}
   *  @throws IndexOutOfBoundsException if {@code index} is negative or is
   *     greater than {@code size}
   *  @throws IllegalArgumentException if {@code size} is negative
   */
  def checkPositionIndex(index: Int, size: Int, desc: String): Int = {
    if (index < 0 || index > size) {
      throw new IndexOutOfBoundsException(badPositionIndex(index, size, desc))
    }
    return index
  }

  private def badPositionIndex(index: Int, size: Int, desc: String): String = {
    if (index < 0) {
      return format("%s (%s) must not be negative", desc, index)
    } else if (size < 0) {
      throw new IllegalArgumentException("negative size: " + size)
    } else { // index > size
      return format("%s (%s) must not be greater than size (%s)",
        desc, index, size)
    }
  }

  /** Ensures that {@code start} and {@code end} specify a valid <i>positions</i>
   *  in an array, list or string of size {@code size}, and are in order. A
   *  position index may range from zero to {@code size}, inclusive.
   *
   *  @param start a user-supplied index identifying a starting position in an
   *     array, list or string
   *  @param end a user-supplied index identifying a ending position in an array,
   *     list or string
   *  @param size the size of that array, list or string
   *  @throws IndexOutOfBoundsException if either index is negative or is
   *     greater than {@code size}, or if {@code end} is less than {@code start}
   *  @throws IllegalArgumentException if {@code size} is negative
   */
  def checkPositionIndexes(start: Int, end: Int, size: Int) = {
    if (start < 0 || end < start || end > size) {
      throw new IndexOutOfBoundsException(badPositionIndexes(start, end, size))
    }
  }

  private def badPositionIndexes(start: Int, end: Int, size: Int): String = {
    if (start < 0 || start > size) {
      return badPositionIndex(start, size, "start index")
    }
    if (end < 0 || end > size) {
      return badPositionIndex(end, size, "end index")
    }
    // end < start
    return format("end index (%s) must not be less than start index (%s)",
      end, start)
  }

  /** Substitutes each {@code %s} in {@code template} with an argument. These
   *  are matched by position - the first {@code %s} gets {@code args[0]}, etc.
   *  If there are more arguments than placeholders, the unmatched arguments will
   *  be appended to the end of the formatted message in square braces.
   *
   *  @param template a non-null string containing 0 or more {@code %s}
   *     placeholders.
   *  @param args the arguments to be substituted into the message
   *     template. Arguments are converted to strings using
   *     {@link String#valueOf(Object)}. Arguments can be null.
   */
  private def format(templateStr: String, @Nullable args: Any*): String = {
    val template = String.valueOf(templateStr) // null -> "null"

    // start substituting the arguments into the '%s' placeholders
    val builder = new StringBuilder(template.length() + 16 * args.length)
    var templateStart = 0
    var i = 0
    var stop = false
    while (stop == false && i < args.length) {
      var placeholderStart = template.indexOf("%s", templateStart)
      if (placeholderStart == -1) {
        stop = true
      } else {
        builder.append(template.substring(templateStart, placeholderStart))
        builder.append(args(i))
        templateStart = placeholderStart + 2
        i = i + 1
      }
    }
    builder.append(template.substring(templateStart))

    // if we run out of placeholders, append the extra args in square braces
    if (i < args.length) {
      builder.append(" [")
      builder.append(args(i))
      i = i + 1
      while (i < args.length) {
        builder.append(", ")
        builder.append(args(i))
        i = i + 1
      }
      builder.append(']')
    }

    return builder.toString()
  }
}