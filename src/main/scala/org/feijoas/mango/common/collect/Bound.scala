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

import org.feijoas.mango.common.base.Preconditions.checkNotNull
import org.feijoas.mango.common.collect.BoundType.{Closed, Open}

/** A `Bound` describes the to bounds of a [[Range]]. For example the range
 *  `(4,7]` has the two (finite) bounds "open" 4 and 7 "closed". A bound is
 *  either a `FiniteBound[T]` with a value of type `T` and a [[BoundType]] or
 *  an `InifinteBound`. An `InifinteBound` is used to describe unbounded ranges
 *  as for example `[a..+∞)`
 *
 *  @author Markus Schneider
 *  @since 0.8
 */
sealed trait Bound[+T] extends Serializable {
  private[mango] def describeAsLowerBound(sb: StringBuilder)
  private[mango] def describeAsUpperBound(sb: StringBuilder)
}

/** A `Bound` describes the to bounds of a [[Range]]. For example the range
 *  `(4,7]` has the two (finite) bounds "open" 4 and 7 "closed". A bound is
 *  either a `FiniteBound[T]` with a value of type `T` and a [[BoundType]] or
 *  an `InifinteBound`. An `InifinteBound` is used to describe unbounded ranges
 *  as for example `[a..+∞)`
 *
 *  @author Markus Schneider
 *  @since 0.8
 */
final object Bound {
  /** A `FiniteBound[T]` is a bound with a value of type `T` and a [[BoundType]].
   */
  case class FiniteBound[T](value: T, bt: BoundType) extends Bound[T] {
    checkNotNull(value)
    checkNotNull(bt)

    override def describeAsLowerBound(sb: StringBuilder) = bt match {
      case Closed => sb.append('[').append(value)
      case Open   => sb.append('(').append(value)
    }
    override def describeAsUpperBound(sb: StringBuilder) = bt match {
      case Closed => sb.append(value).append(']')
      case Open   => sb.append(value).append(')')
    }
  }

  /** An `InifinteBound` is used to describe unbounded ranges
   *  as for example `[a..+∞)`
   */
  final object InfiniteBound extends Bound[Nothing] {
    override def describeAsLowerBound(sb: StringBuilder) = sb.append("(-\u221e")
    override def describeAsUpperBound(sb: StringBuilder) = sb.append("+\u221e)")
  }
}