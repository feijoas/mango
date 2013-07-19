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
package org.feijoas.mango.common.base

import java.util.concurrent.Callable

import org.feijoas.mango.common.base.Functions._
import org.scalatest.{ FlatSpec, ShouldMatchers }
import org.scalatest.prop.PropertyChecks

import com.google.common.base.{ Function => GuavaFunction }
import com.google.common.testing.SerializableTester

/** Tests for [[Functions]]
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
class FunctionsTest extends FlatSpec with ShouldMatchers with PropertyChecks {

  behavior of "implicits"

  it should "be variant" in {
    val cf: (C => A) = new GuavaFunction[B, B]() {
      override def apply(i: B) = null
    }.asScala
  }

  it should "map a Guava Function to a Scala Function" in {
    val cf: (Int => String) = SomeFunction.asScala
    forAll { (n: Int) => cf(n) should be(n.toString) }
  }

  it should "map a Scala Function to a Guava Function" in {
    val gf: GuavaFunction[Int, String] = { (i: Int) => i.toString }.asJava
    forAll { (n: Int) => gf(n) should be(n.toString) }
  }

  it should "not wrap Scala function twice" in {
    val cf = (i: Int) => i.toString
    val gf: GuavaFunction[Int, String] = cf.asJava

    val wrappedTwice: Int => String = gf.asScala
    wrappedTwice should be theSameInstanceAs cf
  }

  it should "not wrap Guava function twice" in {
    val gf = SomeFunction
    val cf: Int => String = gf.asScala

    val wrappedTwice: GuavaFunction[Int, String] = cf.asJava
    wrappedTwice should be theSameInstanceAs gf
  }

  it should "be serializeable" in {
    val gf1 = SomeFunction
    val cf1: Int => String = gf1.asScala
    SerializableTester.reserialize(cf1)

    val cf2 = (i: Int) => i.toString
    val gf2: GuavaFunction[Int, String] = cf2.asJava
    SerializableTester.reserialize(gf2)
  }

  behavior of "Callable[T] conversion"

  it should "wrap a function in a Callable[T]" in {
    import java.util.concurrent.Callable
    import org.feijoas.mango.common.base.Functions._

    forAll { (n: Int) =>
      val callable: Callable[Int] = { () => n }.asJava
      callable.call() should be(n)
    }
  }

  behavior of "Runnable conversion"

  it should "wrap a function in a Runnable" in {
    import org.feijoas.mango.common.base.Functions._

    forAll { (n: Int) =>
      var result = 0 // the runnable will write this variable

      val runnable: Runnable = { () => { result = n; } }.asJava

      // run and check result
      runnable.run()
      result should be(n)
    }
  }

}

private case object SomeFunction extends GuavaFunction[Int, String] {
  override def apply(i: Int) = i.toString
}

private trait A
private trait B extends A
private trait C extends B