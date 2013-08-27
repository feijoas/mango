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

import scala.annotation.meta.beanGetter
import scala.annotation.meta.beanSetter
import scala.annotation.meta.field
import scala.annotation.meta.getter
import scala.annotation.meta.setter
import scala.collection.mutable.Builder
import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect.AsOrdered
import org.feijoas.mango.common.collect.Range
import org.feijoas.mango.common.collect.Range.asGuavaRangeConverter
import com.google.common.collect.{ RangeSet => GuavaRangeSet }
import com.google.common.collect.TreeRangeSet
import org.feijoas.mango.common.collect.RangeSetFactory

/** An mutable implementation of RangeSet that delegates to Guava TreeRangeSet
 *
 *  @author Markus Schneider
 *  @since 0.8
 */
@Beta
private[mango] class TreeRangeSetWrapper[C, O <: Ordering[C]] private (guava: GuavaRangeSet[AsOrdered[C]])(implicit ord: O)
  extends RangeSet[C, O] with MutableRangeSetWrapperLike[C, O, TreeRangeSetWrapper[C, O]] {

  /** The Guava RangeSet to use internally */
  override protected def delegate = guava

  /** The Ordering[C] used for Ranges is needed */
  override implicit def ordering: O = ord

  /** Creates a new Repr from a Guava RangeSet */
  override def wrap: GuavaRangeSet[AsOrdered[C]] => TreeRangeSetWrapper[C, O] = new TreeRangeSetWrapper(_)(ordering)

  /** Returns a new builder for a range set.
   */
  override def newBuilder = TreeRangeSetWrapper.newBuilder[C, O](ord)
}

private[mango] final object TreeRangeSetWrapper extends RangeSetFactory[TreeRangeSetWrapper] {

  /** Returns a new builder for a range set.
   */
  def newBuilder[C, O <: Ordering[C]](implicit ord: O) = new Builder[Range[C, O], TreeRangeSetWrapper[C, O]]() {
    var builder = TreeRangeSet.create[AsOrdered[C]]()
    override def +=(range: Range[C, O]): this.type = {
      builder.add(range.asJava)
      this
    }
    override def clear() = builder = TreeRangeSet.create[AsOrdered[C]]()
    override def result() = new TreeRangeSetWrapper(builder)
  }
}