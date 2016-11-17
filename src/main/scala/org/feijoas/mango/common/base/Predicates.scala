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

import java.util.regex.Pattern
import javax.annotation.Nullable

import scala.collection.immutable

import org.feijoas.mango.common.base.Preconditions.checkNotNull
import org.feijoas.mango.common.convert.{ AsJava, AsScala }

import com.google.common.base.{ Predicate => GuavaPredicate }

/**
 * Utility functions for the work with Guava `Predicate[T]`
 *
 *  Usage example for conversion between Guava and Mango:
 *  {{{
 *  // convert a Guava Predicate[T] to a Scala function T => Boolean
 *  import com.google.common.base.{ Predicate => GuavaPredicate }
 *  val pred: GuavaPredicate[Any] = { (arg: Any) => true }.asJava
 *
 *  // convert a Scala function T => Boolean to a Guava Predicate[T]
 *  import com.google.common.base.{ Predicates => GuavaPredicates }
 *  val pred: Any => Boolean = GuavaPredicates.alwaysTrue().asScala
 *  }}}
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
final object Predicates {

  /**
   * Returns a predicate that always evaluates to `false`.
   */
  case object alwaysFalse extends (Any => Boolean) {
    override def apply(ref: Any) = false
    override def toString = "alwaysFalse"
  }

  /**
   * Returns a predicate that always evaluates to `true`.
   */
  case object alwaysTrue extends (Any => Boolean) {
    override def apply(ref: Any) = true
    override def toString = "alwaysTrue"
  }

  /**
   * Returns a predicate that evaluates to `true` if the object reference
   *  being tested is not `null`.
   */
  case object notNull extends (Any => Boolean) {
    override def apply(ref: Any) = ref != null
    override def toString = "notNull"
  }

  /**
   * Returns a predicate that evaluates to `true` if the object reference
   *  being tested is `null`.
   */
  case object isNull extends (Any => Boolean) {
    override def apply(ref: Any) = ref == null
    override def toString = "isNull"
  }

  /**
   * Returns a predicate that evaluates to `true` if the object reference
   *  being tested is `null`.
   */
  private[mango] case class NotPredicate[T](val predicate: T => Boolean) extends (T => Boolean) {
    checkNotNull(predicate)
    override def apply(arg: T) = !predicate(arg)
    override def toString = "Not(" + predicate.toString + ")"
  }

  /**
   * Returns a predicate that evaluates to {@code true} if each of its
   *  components evaluates to {@code true}. The components are evaluated in
   *  order, and evaluation will be "short-circuited" as soon as a false
   *  predicate is found. It defensively copies the array passed in, so future
   *  changes to it won't alter the behavior of this predicate. If {@code
   *  components} is empty, the returned predicate will always evaluate to {@code
   *  true}.
   */
  private[mango] case class AndPredicate[T](px: Seq[(T => Boolean)]) extends (T => Boolean) {
    checkNotNull(px)
    override def apply(arg: T) = px.indexWhere(f => !f(arg)) == -1
    override def toString = px.mkString("And(", ",", ")")
    override def hashCode = px.hashCode() + 0x12472c2c
  }

  /**
   * Returns a predicate that evaluates to {@code true} if any one of its
   *  components evaluates to {@code true}. The components are evaluated in
   *  order, and evaluation will be "short-circuited" as soon as a
   *  true predicate is found. It defensively copies the iterable passed in, so
   *  future changes to it won't alter the behavior of this predicate. If {@code
   *  components} is empty, the returned predicate will always evaluate to {@code
   *  false}.
   */
  private[mango] case class OrPredicate[T](px: immutable.Seq[(T => Boolean)]) extends (T => Boolean) {
    checkNotNull(px)
    override def apply(arg: T) = px.indexWhere(f => f(arg)) != -1
    override def toString = px.mkString("Or(", ",", ")")
    override def hashCode = px.hashCode() + 0x053c91cf
  }

  /**
   * Returns a predicate that evaluates to (((x1 xor x2) xor x3) xor x4)...
   *  for all elements `xi` in the sequence. It defensively copies the iterable
   *  passed in, so future changes to it won't alter the behavior of this predicate.
   *  If {@code components} is empty, the returned predicate will always evaluate
   *  to {@code false}.
   */
  private[mango] case class XorPredicate[T](px: Seq[(T => Boolean)]) extends (T => Boolean) {
    checkNotNull(px)
    private val xor = px.reduceLeft[T => Boolean]((acc, fnc) => ((arg: T) => fnc(arg) != acc(arg)))
    override def apply(arg: T) = xor(arg)
    override def toString = px.mkString("Xor(", ",", ")")
    override def hashCode = px.hashCode + 0x75bcf4d
  }

  private[mango] case class InstanceOfPredicate(clazz: Class[_]) extends (Any => Boolean) {
    checkNotNull(clazz)
    override def apply(arg: Any) = arg != null && arg.getClass == clazz
    override def toString = "InstanceOf(" + clazz + ")"
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the object being
   *  tested {@code equals()} the given target or both are null.
   */
  private[mango] case class EqualToPredicate[T](@Nullable target: T) extends (T => Boolean) {
    override def apply(arg: T) = target == arg
    override def toString = "IsEqualTo(" + target + ")"
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the
   *  {@code CharSequence} being tested contains any match for the given
   *  regular expression pattern. The test used is equivalent to
   *  {@code pattern.matcher(arg).find()}
   */
  private[mango] case class ContainsPatternPredicate(pattern: Pattern) extends (String => Boolean) {
    checkNotNull(pattern)
    override def apply(arg: String) = pattern.matcher(arg).find()
    override def toString = "ContainsPattern(" + pattern + ")"
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the given predicate
   *  evaluates to {@code false}.
   */
  def not[T](predicate: T => Boolean): T => Boolean = checkNotNull(predicate) match {
    case `alwaysFalse`       => alwaysTrue
    case `alwaysTrue`        => alwaysFalse
    case NotPredicate(inner) => inner
    case _                   => NotPredicate(predicate)
  }

  /**
   * Returns a predicate that evaluates to {@code true} if each of its
   *  components evaluates to {@code true}. The components are evaluated in
   *  order, and evaluation will be "short-circuited" as soon as a false
   *  predicate is found. It defensively copies the array passed in, so future
   *  changes to it won't alter the behavior of this predicate. If {@code
   *  components} is empty, the returned predicate will always evaluate to {@code
   *  true}.
   */
  def and[T](predicates: Seq[(T => Boolean)]): T => Boolean = {
    checkNotNull(predicates)
    if (predicates.isEmpty)
      return alwaysFalse

    predicates match {
      case s: immutable.Seq[_] => AndPredicate(s)
      case _                   => AndPredicate(immutable.Seq() ++ predicates)
    }
  }

  /**
   * Returns a predicate that evaluates to {@code true} if each of its
   *  components evaluates to {@code true}. The components are evaluated in
   *  order, and evaluation will be "short-circuited" as soon as a false
   *  predicate is found. It defensively copies the array passed in, so future
   *  changes to it won't alter the behavior of this predicate. If {@code
   *  components} is empty, the returned predicate will always evaluate to {@code
   *  true}.
   */
  def and[T](fst: T => Boolean, rest: (T => Boolean)*): T => Boolean = {
    checkNotNull(fst)
    checkNotNull(rest)
    val seq: Seq[(T => Boolean)] = fst +: rest
    and(seq)
  }

  /**
   * Returns a predicate that evaluates to {@code true} if any one of its
   *  components evaluates to {@code true}. The components are evaluated in
   *  order, and evaluation will be "short-circuited" as soon as a
   *  true predicate is found. It defensively copies the iterable passed in, so
   *  future changes to it won't alter the behavior of this predicate. If {@code
   *  components} is empty, the returned predicate will always evaluate to {@code
   *  false}.
   */
  def or[T](predicates: Seq[T => Boolean]): T => Boolean = {
    checkNotNull(predicates)
    if (predicates.isEmpty)
      return alwaysFalse

    predicates match {
      case s: immutable.Seq[_] => OrPredicate(s)
      case _                   => OrPredicate(immutable.Seq() ++ predicates)
    }
  }

  /**
   * Returns a predicate that evaluates to {@code true} if any one of its
   *  components evaluates to {@code true}. The components are evaluated in
   *  order, and evaluation will be "short-circuited" as soon as a
   *  true predicate is found. It defensively copies the iterable passed in, so
   *  future changes to it won't alter the behavior of this predicate. If {@code
   *  components} is empty, the returned predicate will always evaluate to {@code
   *  false}.
   */
  def or[T](fst: T => Boolean, rest: (T => Boolean)*): T => Boolean = {
    checkNotNull(fst)
    checkNotNull(rest)
    val seq: Seq[(T => Boolean)] = fst +: rest
    or(seq)
  }

  /**
   * Returns a predicate that evaluates to (((x1 xor x2) xor x3) xor x4)...
   *  for all elements `xi` in the sequence. It defensively copies the iterable
   *  passed in, so future changes to it won't alter the behavior of this predicate.
   *  If {@code components} is empty, the returned predicate will always evaluate
   *  to {@code false}.
   */
  def xor[T](predicates: Seq[T => Boolean]): T => Boolean = {
    checkNotNull(predicates)
    if (predicates.isEmpty)
      return alwaysFalse

    predicates match {
      case s: immutable.Seq[_] => XorPredicate(s)
      case _                   => XorPredicate(immutable.Seq() ++ predicates)
    }
  }

  /**
   * Returns a predicate that evaluates to (((x1 xor x2) xor x3) xor x4)...
   *  for all elements `xi` in the sequence. It defensively copies the iterable
   *  passed in, so future changes to it won't alter the behavior of this predicate.
   *  If {@code components} is empty, the returned predicate will always evaluate
   *  to {@code false}.
   */
  def xor[T](fst: T => Boolean, rest: (T => Boolean)*): T => Boolean = {
    checkNotNull(fst)
    checkNotNull(rest)
    val seq: Seq[(T => Boolean)] = fst +: rest
    xor(seq)
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the object being
   *  tested {@code equals()} the given target or both are null.
   */
  def equalTo[T](target: T): (T => Boolean) = EqualToPredicate(target)

  /**
   * Returns a predicate that evaluates to {@code true} if the class being
   *  tested is assignable from the given class.  The returned predicate
   *  does not allow null inputs.
   *
   * @deprecated Use the correctly-named method {@link #subtypeOf} instead.
   * @since 10.0
   */
  @Deprecated
  def assignableFrom(clazz: Class[_]): Class[_] => Boolean = subtypeOf(clazz)

  /**
   * Returns a predicate that evaluates to {@code true} if the class being
   *  tested is assignable from the given class.  The returned predicate
   *  does not allow null inputs.
   *
   * @since 20.0 (since 10.0 under the incorrect name {@code assignableFrom})
   */
  def subtypeOf(clazz: Class[_]): Class[_] => Boolean = {
    checkNotNull(clazz)
    (arg: Class[_]) => arg != null && clazz.isAssignableFrom(arg)
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the object reference
   *  being tested is a member of the given collection. It does not defensively
   *  copy the collection passed in, so future changes to it will alter the
   *  behavior of the predicate.
   *
   *  The test used is equivalent to `coll.contains(arg)`
   *
   *  @param target the collection that may contain the function input
   */
  def in[T](coll: Seq[T]): T => Boolean = {
    checkNotNull(coll)
    (arg: T) => coll.contains(arg)
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the
   *  {@code String} being tested contains any match for the given
   *  regular expression pattern. The test used is equivalent to
   *  {@code Pattern.compile(pattern).matcher(arg).find()}
   *
   *  @throws java.util.regex.PatternSyntaxException if the pattern is invalid
   */
  def containsPattern(pattern: String): String => Boolean = {
    checkNotNull(pattern)
    containsPattern(Pattern.compile(pattern))
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the
   *  {@code CharSequence} being tested contains any match for the given
   *  regular expression pattern. The test used is equivalent to
   *  {@code pattern.matcher(arg).find()}
   */
  def containsPattern(pattern: Pattern): (String => Boolean) = ContainsPatternPredicate(pattern)

  /**
   * Adds an `asJava` method that wraps a Scala function `T => Boolean` in
   *  a Guava `Predicate[T]`.
   *
   *  The returned Guava `Predicate[T]` forwards all calls of the `apply` method
   *  to the given Scala function `T => Boolean`.
   *
   *  @param pred the Scala function `T => Boolean` to wrap in a Guava `Predicate[T]`
   *  @return An object with an `asJava` method that returns a Guava `Predicate[T]`
   *   view of the argument
   */
  implicit def asGuavaPredicateConverter[T](pred: T => Boolean): AsJava[GuavaPredicate[T]] = {
    checkNotNull(pred)
    def wrap(pred: T => Boolean): GuavaPredicate[T] = new GuavaPredicate[T] {
      override def apply(arg: T): Boolean = pred(arg)
    }
    new AsJava(wrap(pred))
  }

  /**
   * Adds an `asScala` method that wraps a Guava `Predicate[T]` in
   *  a Scala function `T => Boolean`.
   *
   *  The returned Scala function `T => Boolean` forwards all calls of the `apply` method
   *  to the given Guava `Predicate[T]`.
   *
   *  @param pred the Guava `Predicate[T]` to wrap in a Scala function `T => Boolean`
   *  @return An object with an `asScala` method that returns a Scala function `T => Boolean`
   *   view of the argument
   */
  implicit def asMangoPredicateConverter[T](pred: GuavaPredicate[T]): AsScala[T => Boolean] = {
    checkNotNull(pred)
    new AsScala((arg: T) => pred.apply(arg))
  }
}

