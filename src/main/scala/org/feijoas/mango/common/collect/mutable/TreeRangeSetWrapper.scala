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
package org.feijoas.mango.common.collect.mutable

import scala.collection.mutable.Builder

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect.AsOrdered
import org.feijoas.mango.common.collect.Range
import org.feijoas.mango.common.collect.Range.asGuavaRangeConverter
import org.feijoas.mango.common.collect.RangeSetFactory

import com.google.common.collect.{ RangeSet => GuavaRangeSet }
import com.google.common.collect.TreeRangeSet

/** An mutable implementation of RangeSet that delegates to Guava TreeRangeSet
 *
 *  @author Markus Schneider
 *  @since 0.8
 */
@Beta
private[mango] class TreeRangeSetWrapper[C] private (guava: GuavaRangeSet[AsOrdered[C]])(override implicit val ordering: Ordering[C])
  extends RangeSet[C] with RangeSetWrapperLike[C, TreeRangeSetWrapper[C]] {

  override def delegate = guava
  override def factory = TreeRangeSetWrapper(_)(ordering)
  override def newBuilder = TreeRangeSetWrapper.newBuilder[C](ordering)
}

private[mango] object TreeRangeSetWrapper extends RangeSetFactory[TreeRangeSetWrapper] {

  /** Factory method */
  private[mango] def apply[C](guava: GuavaRangeSet[AsOrdered[C]])(implicit ord: Ordering[C]) = new TreeRangeSetWrapper(guava)(ord)

  /** Returns a new builder for a range set.
   */
  def newBuilder[C](implicit ord: Ordering[C]) = new Builder[Range[C], TreeRangeSetWrapper[C]]() {
    var builder = TreeRangeSet.create[AsOrdered[C]]()
    override def +=(range: Range[C]): this.type = {
      builder.add(range.asJava)
      this
    }
    override def clear() = builder = TreeRangeSet.create[AsOrdered[C]]()
    override def result() = new TreeRangeSetWrapper(builder)
  }
}