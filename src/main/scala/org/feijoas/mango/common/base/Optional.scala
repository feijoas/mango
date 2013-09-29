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

import org.feijoas.mango.common.base.Preconditions.checkNotNull
import org.feijoas.mango.common.convert.{ AsJava, AsScala }

import com.google.common.base.{ Optional => GuavaOptional }

/** Utility functions for the work with `Option[T]` and `Optional[T]`
 *
 *  Usage example for conversion between Guava and Mango:
 *  {{{
 *  // convert a Guava Optional[T] to a Scala Option[T]
 *  Optional.of("some").asScala
 *  Optional.absent().asScala
 *
 *  // convert a Scala Option[T] to a Guava Optional[T]
 *  Some("some").asJava
 *  None.asJava
 *  }}}
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
final object Optional {

  /** Adds an `asJava` method that converts a Scala `Option[T]` to
   *  a Guava `Optional[T]`.
   *
   *  The returned Guava `Optional[T]` contains the same reference as in the
   *  Scala `Option[T]` or `GuavaOptional.absent()` if the `Option[T]` is None.
   *
   *  @param option the Scala `Option[T]` to convert to a Guava `Optional[T]`
   *  @return An object with an `asJava` method that returns a Guava `Optional[T]`
   *   view of the argument
   */
  implicit def asGuavaOptionalConverter[T](option: Option[T]): AsJava[GuavaOptional[T]] = {
      def convert(option: Option[T]): GuavaOptional[T] = checkNotNull(option) match {
        case Some(value) => GuavaOptional.of(value)
        case None        => GuavaOptional.absent()
      }
    new AsJava(convert(option))
  }

  /** Adds an `asScala` method that converts a Guava `Optional[T]` to
   *  a Scala `Option[T]`.
   *
   *  The returned Scala `Option[T]` contains the same reference as in the
   *  Guava `Optional[T]` or `None` if the `Optional[T]` is absent.
   *
   *  @param option the Guava `Optional[T]` to convert to a Scala `Option[T]`
   *  @return An object with an `asScala` method that returns a Scala `Option[T]`
   *   view of the argument
   */
  implicit def asMangoOptionConverter[T](option: GuavaOptional[T]): AsScala[Option[T]] = {
      def convert(option: GuavaOptional[T]) = option.isPresent() match {
        case true  => Some(option.get())
        case false => None
      }
    new AsScala(convert(option))
  }
}