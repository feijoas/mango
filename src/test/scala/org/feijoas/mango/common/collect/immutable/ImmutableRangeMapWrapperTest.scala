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
package org.feijoas.mango.common.collect.immutable

import scala.annotation.meta.beanGetter
import scala.annotation.meta.beanSetter
import scala.annotation.meta.field
import scala.annotation.meta.getter
import scala.annotation.meta.setter
import scala.math.Ordering.Int

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect.AsOrdered
import org.feijoas.mango.common.collect.Range
import org.feijoas.mango.common.collect.RangeMapBehaviors
import org.feijoas.mango.common.collect.RangeMapWrapperBehaviours
import org.scalatest.FreeSpec
import org.scalatest.Matchers.be
import org.scalatest.Matchers.convertToAnyShouldWrapper

import com.google.common.{collect => gcc}

/** Tests for [[ImmutableRangeMapWrapperTest]]
 *
 *  @author Markus Schneider
 *  @since 0.9
 */
class ImmutableRangeMapWrapperTest extends FreeSpec with RangeMapBehaviors with RangeMapWrapperBehaviours {

  "A ImmutableRangeMapWrapper" - {
    behave like aRangeMapLike(ImmutableRangeMapWrapper.newBuilder[Int, String, Int.type])
    "it should not create a copy if RangeMap(same type of immutable range map) is called" in {
      val fst = ImmutableRangeMapWrapper(Range.open(3, 4) -> "a")
      val snd = ImmutableRangeMapWrapper(fst)
      fst should be theSameInstanceAs (snd)
    }
    behave like immutableWrapper((guava: gcc.RangeMap[AsOrdered[Int], String]) => ImmutableRangeMapWrapper[Int, String, Int.type](guava))
  }
}

