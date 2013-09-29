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

import java.util.concurrent.Callable

import org.feijoas.mango.common.base.Preconditions.checkNotNull
import org.feijoas.mango.common.convert.{ AsJava, AsScala }

import com.google.common.base.{ Function => GuavaFunction }

/** Utility functions for the work with Guava `Function[T, R]`
 *
 *  Usage example for conversion between Guava and Mango:
 *  {{{
 *  // convert a Guava function to a Scala function
 *  val fnc: Function[T, R] = { (arg: T) => ... }.asJava
 *
 *  // convert a Scala function to a Guava function
 *  val fnc: (T => R) = SomeGuavaFunction.asScala
 *
 *  // convert a Scala function to a Callable
 *  val callable: Callable[R] = { () => n }.asJava
 *
 *  // convert a Scala functio to a Runnable
 *  val runnable: Runnable = { () => ... }.asJava
 *  }}}
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
final object Functions {

  /** Adds an `asScala` method that wraps a Guava `Function[T, R]` in
   *  a Scala function `T => R`.
   *
   *  All calls to the Scala function are forwarded to the provided Guava function.
   *  @param fnc the Guava `Function[T, R]` to wrap in a Scala funcion `T => R`
   *  @return An object with an `asScala` method that returns a Scala
   *          `T => R` view of the argument.
   */
  implicit def asMangoFunctionConverter[T, R](fnc: GuavaFunction[T, R]): AsScala[T => R] = {
      def convert(fnc: GuavaFunction[T, R]): (T => R) = fnc match {
        case gf: AsGuavaFunction[T, R] => gf.delegate
        case _                         => AsMangoFunction(fnc)
      }
    new AsScala(convert(fnc))
  }

  /** Adds an `asJava` method that wraps a Scala function `T => R`  in
   *  a Guava `Function[T, R]`.
   *
   *  All calls to the Guava function are forwarded to the provided Scala function.
   *  @param fnc the Scala function `T => R` to wrap in a Guava `Function[T, R]`
   *  @return An object with an `asJava` method that returns a Guava `Function[T, R]`
   *   view of the argument.
   */
  implicit def asGuavaFunctionConverter[T, R](fnc: T => R): AsJava[GuavaFunction[T, R]] = {
      def convert(fnc: T => R) = fnc match {
        case cf: AsMangoFunction[T, R] => cf.delegate
        case _                         => AsGuavaFunction(fnc)
      }
    new AsJava(convert(fnc))
  }

  /** Adds an `asJava` method that wraps a Scala function `() => Unit`  in
   *  a Java `Runnable`.
   *
   *  All calls to the `Runnable` are forwarded to the provided Scala function.
   *  @param fnc the Scala function `() => Unit` to wrap in a Java `Runnable`
   *  @return An object with an `asJava` method that returns a Java `Runnable`
   *   view of the argument.
   */
  implicit def asRunnableConverter(fnc: () => Unit): AsJava[Runnable] = new AsJava(
    new Runnable() {
      override def run() = fnc()
    })

  /** Adds an `asJava` method that wraps a Scala function `() => R`  in
   *  a Java `Callable`.
   *
   *  All calls to the `Callable` are forwarded to the provided Scala function.
   *  @param fnc the Scala function `() => R` to wrap in a Java `Callable`
   *  @return An object with an `asJava` method that returns a Java `Callable`
   *   view of the argument.
   */
  implicit def asCallableConverter[R](fnc: () => R): AsJava[Callable[R]] = new AsJava(
    new Callable[R]() {
      def call() = fnc()
    })
}

/** Wraps a Guava `Function` in a Scala function */
@SerialVersionUID(1L)
private[mango] case class AsMangoFunction[R, T](delegate: GuavaFunction[R, T]) extends Function1[R, T] with Serializable {
  checkNotNull(delegate)
  override def apply(input: R) = delegate.apply(input)
}

/** Wraps a Scala function in a Guava `Function` */
@SerialVersionUID(1L)
private[mango] case class AsGuavaFunction[T, R](delegate: R => T) extends GuavaFunction[R, T] with Serializable {
  checkNotNull(delegate)
  override def apply(input: R) = delegate(input)
}