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
package org.feijoas.mango.common.hash

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.hash.Funnel.{ asGuavaFunnel, asScalaFunnel, byteArrayFunnel, intFunnel, longFunnel, stringFunnel }
import org.mockito.Mockito.verify
import org.scalatest.{ FlatSpec, PrivateMethodTester }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.Matchers._

import com.google.common.hash.{ Funnel => GuavaFunnel, Funnels => GuavaFunnels, PrimitiveSink }

/** Tests for [[Funnel]]
 *
 *  @author Markus Schneider
 *  @since 0.6 (copied from guava-libraries)
 */
class FunnelTest extends FlatSpec with PrivateMethodTester with MockitoSugar {

  it should "convert from Guava to Mango" in {
    val guava: GuavaFunnel[Integer] = GuavaFunnels.integerFunnel
    val mango: Funnel[Integer] = guava.asScala
    // enough if the compiler does not complain
  }

  it should "convert from Mango to Guava" in {
    val mango: Funnel[Int] = implicitly[Funnel[Int]]
    val guava: GuavaFunnel[Int] = mango.asJava
    // enough if the compiler does not complain    
  }

  it should "not wrap a Guava Funnel twice" in {
    val guava: GuavaFunnel[Integer] = GuavaFunnels.integerFunnel
    val mango: Funnel[Integer] = guava.asScala
    mango.asJava should be theSameInstanceAs (guava)
  }

  it should "not wrap a Mango Funnel twice" in {
    val mango: Funnel[Int] = implicitly[Funnel[Int]]
    val guava: GuavaFunnel[Int] = mango.asJava
    guava.asScala should be theSameInstanceAs (mango)
  }

  it should "implement Funnel[Array[Byte]]" in {
    val primitiveSink = mock[PrimitiveSink]
    val funnel = implicitly[Funnel[Array[Byte]]]
    funnel.funnel(Array[Byte](4, 3, 2, 1), primitiveSink)
    verify(primitiveSink).putBytes(Array[Byte](4, 3, 2, 1))
  }

  it should "implement Funnel[CharSequence]" in {
    val primitiveSink = mock[PrimitiveSink]
    val funnel = implicitly[Funnel[CharSequence]]
    funnel.funnel("test", primitiveSink)
    verify(primitiveSink).putUnencodedChars("test")
  }

  it should "implement Funnel[Int]" in {
    val primitiveSink = mock[PrimitiveSink]
    val funnel = implicitly[Funnel[Int]]
    funnel.funnel(1234, primitiveSink)
    verify(primitiveSink).putInt(1234)
  }

  it should "implement Funnel[Long]" in {
    val primitiveSink = mock[PrimitiveSink]
    val funnel = implicitly[Funnel[Long]]
    funnel.funnel(1234, primitiveSink)
    verify(primitiveSink).putLong(1234)
  }
}