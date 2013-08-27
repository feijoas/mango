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
import org.feijoas.mango.common.convert.{ AsJava, AsScala }

import com.google.common.collect.{ BoundType => GuavaBoundType }

/** Indicates whether an endpoint of some range is contained in the range itself ("closed") or not
 *  ("open"). If a range is unbounded on a side, it is neither open nor closed on that side; the
 *  bound simply does not exist.
 *
 *  @author Markus Schneider
 *  @since 0.8 (copied from Guava-libraries)
 */
sealed trait BoundType extends Serializable

/** Indicates whether an endpoint of some range is contained in the range itself ("closed") or not
 *  ("open"). If a range is unbounded on a side, it is neither open nor closed on that side; the
 *  bound simply does not exist.
 *
 *  @author Markus Schneider
 *  @since 0.8 (copied from Guava-libraries)
 */
final object BoundType {

  /** The endpoint value <i>is not</i> considered part of the set ("exclusive").
   */
  final object Open extends BoundType

  /** The endpoint value <i>is</i> considered part of the set ("inclusive").
   */
  final object Closed extends BoundType

  /** Adds an `asScala` method that converts a Guava `BoundType` to a Mango `BoundType`
   *
   *  @param bt the Guava `BoundType` to convert to a Mango `BoundType`
   *  @return An object with an `asScala` method that converts a Guava `BoundType`
   *  to a Mango `BoundType`
   */
  implicit final def asMangoBoundType(bt: GuavaBoundType): AsScala[BoundType] = {
    new AsScala(checkNotNull(bt) match {
      case GuavaBoundType.OPEN   => Open
      case GuavaBoundType.CLOSED => Closed
    })
  }

  /** Adds an `asJava` method that converts a Mango `BoundType` to a Guava `BoundType`
   *
   *  @param bt the Mango `BoundType` to convert to a Guava `BoundType`
   *  @return An object with an `asJava` method that converts a Mango `BoundType`
   *  to a Guava `BoundType`
   */
  implicit final def asGuavaBoundType(bt: BoundType): AsJava[GuavaBoundType] = {
    new AsJava(checkNotNull(bt) match {
      case Open   => GuavaBoundType.OPEN
      case Closed => GuavaBoundType.CLOSED
    })
  }
}