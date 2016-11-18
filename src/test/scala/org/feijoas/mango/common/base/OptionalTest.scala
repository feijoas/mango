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
package org.feijoas.mango.common.base

import org.feijoas.mango.common.base.Optional._
import org.scalatest._
import org.scalatest.prop.PropertyChecks

import com.google.common.base.{ Optional => GuavaOptional }
import com.google.common.testing.SerializableTester

/**
 * Tests for [[Optionals]]
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
class OptionalTest extends FlatSpec with Matchers with PropertyChecks {

  behavior of "implicits"

  it should "map a Guava Present to a Scala Some" in {
    val optional: GuavaOptional[String] = GuavaOptional.of("some")
    val option: Option[String] = optional.asScala

    option should be(Some("some"))
  }

  it should "map a Guava Absent to a Scala None" in {
    val optional: GuavaOptional[Any] = GuavaOptional.absent()
    val option: Option[Any] = optional.asScala

    option should be(None)
  }

  it should "map a Scala Some to a Guava Present" in {
    val option: Option[String] = Some("some")
    val optional: GuavaOptional[String] = option.asJava

    optional should be(GuavaOptional.of("some"))
  }

  it should "map a Scala None to a Guava Absent" in {
    val option: Option[Any] = None
    val optional: GuavaOptional[Any] = option.asJava

    optional should be(GuavaOptional.absent())
  }

  it should "be nullsafe" in {
    intercept[NullPointerException] {
      val option: Option[Any] = null
      val optional: GuavaOptional[Any] = option.asJava
    }

    intercept[NullPointerException] {
      val optional: GuavaOptional[Any] = null
      val option: Option[Any] = optional.asScala
    }
  }

  it should "be serializeable" in {
    SerializableTester.reserialize(GuavaOptional.of("some").asScala)
    SerializableTester.reserialize(GuavaOptional.absent().asScala)
    SerializableTester.reserialize(Some("some").asJava)
    SerializableTester.reserialize(None.asJava)
  }
}
