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
package org.feijoas.mango.common.cache

import org.feijoas.mango.common.cache.RemovalCause._
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import com.google.common.cache.{ RemovalCause => GuavaRemovalCause }

/** Tests for [[RemovalCause]]
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from guava-libraries)
 */
class RemovalCauseTest extends FlatSpec with ShouldMatchers {

  behavior of "RemovalCause"

  it should "convert Guava RemovalCause to Mango RemovalCause" in {
    GuavaRemovalCause.COLLECTED.asScala should be(RemovalCause.Collected)
    GuavaRemovalCause.EXPIRED.asScala should be(RemovalCause.Expired)
    GuavaRemovalCause.EXPLICIT.asScala should be(RemovalCause.Explicit)
    GuavaRemovalCause.REPLACED.asScala should be(RemovalCause.Replaced)
    GuavaRemovalCause.SIZE.asScala should be(RemovalCause.Size)
  }

  it should "convert Mango RemovalCause to Guava RemovalCause" in {
    RemovalCause.Collected.asJava should be(GuavaRemovalCause.COLLECTED)
    RemovalCause.Expired.asJava should be(GuavaRemovalCause.EXPIRED)
    RemovalCause.Explicit.asJava should be(GuavaRemovalCause.EXPLICIT)
    RemovalCause.Replaced.asJava should be(GuavaRemovalCause.REPLACED)
    RemovalCause.Size.asJava should be(GuavaRemovalCause.SIZE)
  }
}