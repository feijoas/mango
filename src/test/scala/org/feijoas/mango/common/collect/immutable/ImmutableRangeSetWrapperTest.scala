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
package org.feijoas.mango.common.collect.immutable

import scala.math.Ordering.Int

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect.AsOrdered
import org.feijoas.mango.common.collect.Range
import org.feijoas.mango.common.collect.RangeSetBehaviors
import org.feijoas.mango.common.collect.RangeSetWrapperBehaviours
import org.scalatest.FreeSpec

import com.google.common.collect.{RangeSet => GuavaRangeSet}
import com.google.common.testing.SerializableTester.reserializeAndAssert

/** Tests for [[ImmutableRangeSetWrapperTest]]
 *
 *  @author Markus Schneider
 *  @since 0.8
 */
class ImmutableRangeSetWrapperTest extends FreeSpec with RangeSetBehaviors with RangeSetWrapperBehaviours {

  "A ImmutableRangeSetWrapper" - {
    behave like rangeSet(ImmutableRangeSetWrapper.newBuilder[Int, Int.type])
    behave like rangeSetWithBuilder(ImmutableRangeSetWrapper.newBuilder[Int, Int.type])
    "it should be serializeable" - {
      "given the RangeSet contains the Ranges {[5,8],[1,3)}" in {
        val rset = (ImmutableRangeSetWrapper.newBuilder[Int, Int.type] ++= Set(Range.closed(5, 8), Range.closedOpen(1, 3))).result
        reserializeAndAssert(rset)
      }
    }
    "it should not create a copy if RangeSet(same type of immutable range set) is called" in {
      val fst = ImmutableRangeSetWrapper(Range.open(3, 4))
      val snd = ImmutableRangeSetWrapper(fst)
      fst should be theSameInstanceAs (snd)
    }
    behave like immutableWrapper((guava: GuavaRangeSet[AsOrdered[Int]]) => ImmutableRangeSetWrapper[Int, Int.type](guava))
  }
}

