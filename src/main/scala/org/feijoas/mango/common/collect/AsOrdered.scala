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
package org.feijoas.mango.common.collect

import org.feijoas.mango.common.base.Preconditions._

/** Guavas `Range` requires the parameter `T` to implement `Ordered[_]` (`Comparable[_]` in Java).
 *  This class converts a value `T` to a class `AsOrdered[T]` which implements `Ordered[AsOrdered[T]]`.
 *  Internally we end up with a Guava `Range[AsOrdered[T]]`.
 */
private[mango] case class AsOrdered[T](val value: T)(implicit ord: Ordering[T]) extends Ordered[AsOrdered[T]] {
  override def compare(that: AsOrdered[T]) = ord.compare(value, that.value)
  override def toString = value.toString
}

/** Factory for AsOrdered
 */
private[mango] final object AsOrdered {

  /** Implicit conversion from `Ordering[T]` to `AsOrdered[T]`
   */
  implicit def asOrdered[T](value: T)(implicit ord: Ordering[T]): AsOrdered[T] = AsOrdered(checkNotNull(value))
}