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

import org.feijoas.mango.common.base.Predicates._
import org.scalatest._
import org.scalatest.prop.PropertyChecks
import com.google.common.base.{ Predicates => GuavaPredicates }
import com.google.common.base.{ Predicate => GuavaPredicate }

/**
 * Tests for [[Predicates]]
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from guava-libraries)
 */
class PredicatesTest extends FlatSpec with Matchers with PropertyChecks {

  val isEven = (n: Int) => n % 2 == 0
  val isOdd = (n: Int) => n % 2 != 0
  val neverReach = (n: Any) => { fail("should never reach this code"); false }

  behavior of "Predicates"

  "alwaysTrue" should "should always return true" in {
    forAll { (n: AnyVal) => alwaysTrue(n) should be(true) }
    alwaysTrue(null) should be(true)
  }

  "alwaysFalse" should "should always return false" in {
    forAll { (n: AnyVal) => alwaysFalse(n) should be(false) }
    alwaysFalse(null) should be(false)
  }

  "not" should "return !predicate(arg)" in {
    val notIsEven = Predicates.not(isEven)
    forAll { (n: Int) => notIsEven(n) should be(isOdd(n)) }
  }

  "not alwaysTrue" should "be the same instance as alwaysFalse" in {
    Predicates.not(alwaysTrue) should be theSameInstanceAs (alwaysFalse)
  }

  "not alwaysFalse" should "be the same instance as alwaysTrue" in {
    Predicates.not(alwaysFalse) should be theSameInstanceAs (alwaysTrue)
  }

  "double neg. with not" should "return the orig instance" in {
    Predicates.not(Predicates.not(isEven)) should be theSameInstanceAs (isEven)
  }

  "and" should "with empy arg should return always false" in {
    and(List[Int => Boolean]()) should be theSameInstanceAs (alwaysFalse)
  }

  "and with one arg" should "be the same as the arg itself" in {
    val andIsEven = and(List(isEven))
    forAll { (n: Int) => andIsEven(n) should be(isEven(n)) }
  }

  "and" should "meat the requirements with 2 args" in {
    forAll { (n: Int) => and(isEven, alwaysTrue)(n) should be(isEven(n)) }
    forAll { (n: Int) => and(alwaysTrue, isEven)(n) should be(isEven(n)) }
    forAll { (n: Int) => and(alwaysFalse, neverReach)(n) should be(false) }
  }

  "and" should "meat the requirements with 3 args" in {
    forAll { (n: Int) => and(isEven, alwaysTrue, alwaysTrue)(n) should be(isEven(n)) }
    forAll { (n: Int) => and(alwaysTrue, isEven, alwaysTrue)(n) should be(isEven(n)) }
    forAll { (n: Int) => and(alwaysTrue, alwaysTrue, isEven)(n) should be(isEven(n)) }
    forAll { (n: Int) => and(alwaysTrue, alwaysFalse, neverReach)(n) should be(false) }
  }

  "and" should "meat the requirements with Seq" in {
    forAll { (n: Int) => and(List(isEven))(n) should be(isEven(n)) }
    forAll { (n: Int) => and(List(alwaysTrue, isEven))(n) should be(isEven(n)) }
    forAll { (n: Int) => and(List(alwaysFalse, neverReach))(n) should be(false) }
  }

  "and" should "denfensifly copy mutable Seq" in {
    val array = Array[Any => Boolean](alwaysFalse)
    val predicate = and(array)
    predicate(1) should be(false)
    array(0) = alwaysTrue
    predicate(1) should be(false)
  }

  "and" should "not denfensifly copy immutable Seq" in {
    val list = List[Any => Boolean](alwaysFalse)
    val predicate = and(list)
    predicate match {
      case a: AndPredicate[Any] => a.px should be theSameInstanceAs list
      case _                    => fail("expected an AndPredicate")
    }
  }

  "or" should "with empy arg should return always false" in {
    or(List[Int => Boolean]()) should be theSameInstanceAs (alwaysFalse)
  }

  "or with one arg" should "be the same as the arg itself" in {
    val orIsEven = or(List(isEven))
    forAll { (n: Int) => orIsEven(n) should be(isEven(n)) }
  }

  "or" should "meat the requirements with 2 args" in {
    forAll { (n: Int) => or(isEven, alwaysFalse)(n) should be(isEven(n)) }
    forAll { (n: Int) => or(alwaysFalse, isEven)(n) should be(isEven(n)) }
    forAll { (n: Int) => or(alwaysTrue, neverReach)(n) should be(true) }
  }

  "or" should "meat the requirements with 3 args" in {
    forAll { (n: Int) => or(isEven, alwaysFalse, alwaysFalse)(n) should be(isEven(n)) }
    forAll { (n: Int) => or(alwaysFalse, isEven, alwaysFalse)(n) should be(isEven(n)) }
    forAll { (n: Int) => or(alwaysFalse, alwaysFalse, isEven)(n) should be(isEven(n)) }
    forAll { (n: Int) => or(alwaysFalse, alwaysTrue, neverReach)(n) should be(true) }
  }

  "or" should "meat the requirements with Seq" in {
    forAll { (n: Int) => or(List(isEven))(n) should be(isEven(n)) }
    forAll { (n: Int) => or(List(alwaysFalse, isEven))(n) should be(isEven(n)) }
    forAll { (n: Int) => or(List(alwaysTrue, neverReach))(n) should be(true) }
  }

  "or" should "denfensifly copy mutable Seq" in {
    val array = Array[Any => Boolean](alwaysFalse)
    val predicate = or(array)
    predicate(1) should be(false)
    array(0) = alwaysTrue
    predicate(1) should be(false)
  }

  "or" should "not denfensifly copy immutable Seq" in {
    val list = List[Any => Boolean](alwaysFalse)
    val predicate = or(list)
    predicate match {
      case a: OrPredicate[Any] => a.px should be theSameInstanceAs list
      case _                   => fail("expected an orPredicate")
    }
  }

  "xor" should "with empy arg should return always false" in {
    xor(List[Int => Boolean]()) should be theSameInstanceAs (alwaysFalse)
  }

  "xor with one arg" should "be the same as the arg itself" in {
    val xorIsEven = xor(List(isEven))
    forAll { (n: Int) => xorIsEven(n) should be(isEven(n)) }
  }

  "xor" should "meat the requirements with 2 args" in {
    forAll { (n: Int) => xor(isEven, isEven)(n) should be(false) }
    forAll { (n: Int) => xor(isOdd, isEven)(n) should be(true) }
  }

  "xor" should "meat the requirements with 3 args" in {
    forAll { (n: Int) => xor(isEven, isOdd, alwaysFalse)(n) should be(xor(isEven, xor(isOdd, alwaysFalse))(n)) }
    forAll { (n: Int) => xor(isEven, isOdd, alwaysFalse)(n) should be(xor(xor(isEven, isOdd), alwaysFalse)(n)) }
  }

  "xor" should "meat the requirements with Seq" in {
    forAll { (n: Int) => xor(List(isEven))(n) should be(isEven(n)) }
    forAll { (n: Int) => xor(List(isEven, isEven))(n) should be(false) }
    forAll { (n: Int) => xor(List(isEven, isOdd))(n) should be(true) }
  }

  "xor" should "denfensifly copy mutable Seq" in {
    val array = Array[Any => Boolean](alwaysFalse)
    val predicate = xor(array)
    predicate(1) should be(false)
    array(0) = alwaysTrue
    predicate(1) should be(false)
  }

  "xor" should "not denfensifly copy immutable Seq" in {
    val list = List[Any => Boolean](alwaysFalse)
    val predicate = xor(list)
    predicate match {
      case a: XorPredicate[Any] => a.px should be theSameInstanceAs list
      case _                    => fail("expected an xorPredicate")
    }
  }

  "equalTo(1)" should "be equal to ?==1" in {
    val eq1 = equalTo(1)
    eq1(1) should be(true)
    forAll { (n: Int) => eq1(n) should be(n == 1) }
  }

  "equalTo(null)" should "be equal to ?==null" in {
    val equalToNull = equalTo[Any](null)
    equalToNull(null) should be(true)
    forAll { (n: AnyVal) => equalToNull(n) should be(false) }
  }

  "equalTo" should " be equal/have same hash if content is equal" in {
    forAll { (n: AnyVal) => equalTo(n) should be(equalTo(n)) }
    forAll { (n: AnyVal) => equalTo(n).hashCode should be(equalTo(n).hashCode) }
  }

  "assignableFrom(classOf[Int])" should "be true for Class[Int]" in {
    val isInt = assignableFrom(classOf[Int])
    forAll { (n: Int) => isInt(n.getClass) should be(true) }
    isInt("".getClass) should be(false)
    isInt(null) should be(false)
  }

  "assignableFrom " should "work against traits" in {
    val isAss = assignableFrom(classOf[Traversable[_]])
    isAss(classOf[List[_]]) should be(true)
    isAss(classOf[Map[_, _]]) should be(true)
  }

  "isNull" should "return true only for null" in {
    isNull(null) should be(true)
    forAll { (n: AnyVal) => isNull(n) should be(false) }
  }

  "notNull" should "return never return true for null" in {
    notNull(null) should be(false)
    forAll { (n: AnyVal) => notNull(n) should be(true) }
  }

  "in" should "test weather the values is in the collection" in {
    val isOneOrFive = in[Any](List(1, 5))
    isOneOrFive(1) should be(true)
    isOneOrFive(5) should be(true)
    isOneOrFive(3) should be(false)
    isOneOrFive(null) should be(false)
  }

  "containsPatter" should "be able to match simple patterns" in {
    val isFoobar = containsPattern("^Fo.*o.*bar$")
    isFoobar("Foxyzoabcbar") should be(true)
    isFoobar("Foobarx") should be(false)
  }

  "boolean operators" should "not copute hashCodes per-instance" in {
    val p1 = isNull
    val p2 = isOdd

    Predicates.not(p1).hashCode should be(Predicates.not(p1).hashCode)
    and(p1, p2).hashCode should be(and(p1, p2).hashCode)
    or(p1, p2).hashCode should be(or(p1, p2).hashCode)
    xor(p1, p2).hashCode should be(xor(p1, p2).hashCode)

    // While not a contractual requirement, we'd like the hash codes for ands
    // & ors of the same predicates to not collide.
    and(p1, p2).hashCode should not be (or(p1, p2).hashCode)
    and(p1, p2).hashCode should not be (xor(p1, p2).hashCode)
  }

  "boolean operators" should "be equal if args are equal" in {
    val p1 = isNull
    val p2 = isOdd

    Predicates.not(p1) should be(Predicates.not(p1))
    and(p1, p2) should be(and(p1, p2))
    or(p1, p2) should be(or(p1, p2))
    xor(p1, p2) should be(xor(p1, p2))

    and(p1, p2) should not be (or(p1, p2))
    and(p1, p2) should not be (xor(p1, p2))
  }

  "Predicates" should "convert a Guava Predicate[T] to a Scala function T => Boolean" in {
    val pred: Any => Boolean = GuavaPredicates.alwaysTrue().asScala
  }

  "Predicates" should "convert a Scala function T => Boolean to a Guava Predicate[T]" in {
    val pred: GuavaPredicate[Any] = { (arg: Any) => true }.asJava
  }
}
