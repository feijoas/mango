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

import org.feijoas.mango.common.annotations.Beta
import scala.collection.mutable.Builder
import com.google.common.collect.ImmutableRangeSet
import org.feijoas.mango.common.collect.immutable.ImmutableRangeSetWrapper

/** A base trait for all `RangeSet`, mutable as well as immutable
 *
 *  $rangeSetNote
 *  @author Markus Schneider
 *  @since 0.8
 */
@Beta
trait RangeSet[C] extends RangeSetLike[C, RangeSet[C]] {

}

/** Factory for immutable [[RangeSet]]
 */
object RangeSet extends RangeSetFactory[RangeSet] {

  override def all[C](implicit ord: Ordering[C]) = immutable.RangeSet.all[C]
  override def empty[C](implicit ord: Ordering[C]) = immutable.RangeSet.empty[C]
  override def apply[C](ranges: Range[C]*)(implicit ord: Ordering[C]) = immutable.RangeSet.apply(ranges: _*)
  override def apply[C](rangeSet: RangeSet[C])(implicit ord: Ordering[C]) = immutable.RangeSet.apply(rangeSet)
  override def newBuilder[C](implicit ord: Ordering[C]): Builder[Range[C], RangeSet[C]] = immutable.RangeSet.newBuilder
}