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
 * The code of this project is a port of (or wrapper around) the guava-libraries.
 *    See http://code.google.com/p/guava-libraries/
 * 
 * @author Markus Schneider
 */
package org.feijoas.mango.common.base

import org.feijoas.mango.common.base.Preconditions.checkNotNull
import org.feijoas.mango.common.convert.AsJava
import org.feijoas.mango.common.convert.AsScala

import com.google.common.{ base => gcm }

/** Utility functions to convert from Scala `Equiv[T]` to Guava `Equivalence[T]` and vice versa.
 *
 *  Usage example for conversion between Guava and Mango:
 *  {{{
 *  import org.feijoas.mango.common.base.Equivalence._
 *
 *  // convert a Guava Equivalence[T] to a Scala Equiv[T]
 *  val guava: gcm.Equivalence[T] = ...
 *  val mango: Equiv[T] = guava.asScala
 *
 *  // convert a Scala Equiv[T] to a Guava Equivalence[T]
 *  val mango: Equiv[T] = ...
 *  val guava: gcm.Equivalence[T] = mango.asJava
 *  }}}
 *
 *  @author Markus Schneider
 *  @since 0.10
 */
final object Equivalence {

  /** Adds an `asScala` method that wraps a Guava `Equivalence[T]` in
   *  a Scala `Equiv[T]`.
   *
   *  The returned Scala `Equiv[T]` forwards all calls
   *  to the given Guava `Equivalence[T]`.
   *
   *  @param equiv the Guava `Equivalence[T]` to wrap in a Scala `Equiv[T]`
   *  @return An object with an `asScala` method that returns a Scala `Equiv[T]`
   *   view of the argument
   */
  implicit def asMangoEquiv[T](equiv: gcm.Equivalence[T]): AsScala[Equiv[T]] = new AsScala(
    equiv match {
      case AsGuavaEquiv(delegate) => delegate
      case _                      => AsMangoEquiv(equiv)
    })

  /** Adds an `asJava` method that wraps a Scala `Equiv[T]` in
   *  a Guava `Equivalence[T]`.
   *
   *  The returned Guava `Equivalence[T]` forwards all calls
   *  to the given Scala `Equiv[T]`.
   *
   *  @param equiv the Scala `Equiv[T]` to wrap in a Guava `Equivalence[T]`
   *  @return An object with an `asJava` method that returns a Guava `Equivalence[T]`
   *   view of the argument
   */
  implicit def asGuavaEquiv[T](equiv: Equiv[T]): AsJava[gcm.Equivalence[T]] = new AsJava(
    equiv match {
      case AsMangoEquiv(delegate) => delegate
      case _                      => AsGuavaEquiv(equiv)
    })
}

/** Wraps a Guava `Equivalence` in a Scala `Equiv`
 */
@SerialVersionUID(1L)
private[mango] case class AsMangoEquiv[T](equiv: gcm.Equivalence[T]) extends Equiv[T] with Serializable {
  checkNotNull(equiv)
  override def equiv(x: T, y: T): Boolean = equiv.equivalent(x, y)
}

/** Wraps a Scala `Equiv` in a Guava `Equivalence`
 */
@SerialVersionUID(1L)
private[mango] case class AsGuavaEquiv[T](equiv: Equiv[T]) extends gcm.Equivalence[T] with Serializable {
  checkNotNull(equiv)
  override def doEquivalent(x: T, y: T): Boolean = equiv.equiv(x, y)
  override def doHash(t: T): Int = t.hashCode
}