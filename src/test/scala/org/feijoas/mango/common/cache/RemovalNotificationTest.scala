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
package org.feijoas.mango.common.cache

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import org.feijoas.mango.common.cache.RemovalNotification._
import org.feijoas.mango.common.cache.RemovalCause._
import com.google.common.cache.{ RemovalNotification => GuavaRemovalNotification }
import org.scalatest.Matchers._

/** Tests for [[RemovalNotification]]
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from guava-libraries)
 */
class RemovalNotificationTest extends FlatSpec {
  behavior of "RemovalNotification"

  it should "convert Guava RemovalNotification to Mango RemovalNotification" in {
    // check for all causes, keys, values
    val causes = List(Collected, Expired, Explicit, Replaced, Size)
    val keys = List(null, "key")
    val values = List(null, "values")

    for {
      key <- keys
      value <- values
      cause <- causes
    } {
      val gn = guavaNotification(key, value, cause)
      val cn: RemovalNotification[String, String] = gn.asScala

      Option(key) should be(cn.key)
      Option(value) should be(cn.value)
      cause should be(cn.cause)
    }
  }

  /** create a guava GuavaRemovalNotification via reflection
   */
  def guavaNotification[K, V](key: K, value: V, cause: RemovalCause) = {
    val ctors = classOf[GuavaRemovalNotification[K, V]].getDeclaredConstructors()
    ctors.length should be(1)
    val ctor = ctors(0)
    ctor.setAccessible(true)

    val gkey = key.asInstanceOf[Object]
    val gvalue = value.asInstanceOf[Object]
    val gcause = cause.asJava
    ctor.newInstance(gkey, gvalue, gcause).asInstanceOf[GuavaRemovalNotification[K, V]]
  }
}