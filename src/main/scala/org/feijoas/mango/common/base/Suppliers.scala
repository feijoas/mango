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

import java.util.concurrent.TimeUnit
import org.feijoas.mango.common.base.Preconditions.checkNotNull
import org.feijoas.mango.common.convert.{ AsJava, AsScala }
import com.google.common.base.{ Supplier => GuavaSupplier, Suppliers => GuavaSuppliers }

/** Utility functions for the work with suppliers which are functions of
 *  the type `() => T`
 *
 *  Usage example for conversion between Guava and Mango:
 *  {{{
 *  // convert a Guava Supplier[T] to a Scala function () => T
 *  import com.google.common.base.{ Supplier => GuavaSupplier }
 *  val fnc: () => T = ...
 *  val supplier: GuavaSupplier[T] = fnc.asJava
 *
 *  // convert a Scala function () => T to a Guava Supplier[T]
 *  val guavaSupplier: GuavaSupplier[T] = ...
 *  val supplier: () => T = guavaSupplier.asScala
 *  }}}
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
final object Suppliers {

  /** Returns a function which caches the instance retrieved during the first
   *  evaluation and returns that value on subsequent evaluations. See:
   *  [[http://en.wikipedia.org/wiki/Memoization memoization]]
   *
   *  <p>The returned function is thread-safe. The functions's serialized form
   *  does not contain the cached value, which will be recalculated when
   *  the reserialized instance is evaluated.
   *
   *  <p>If `f` is an instance created by an earlier call to
   *  `memoize, it is returned directly.
   */
  def memoize[T](f: () => T): () => T = f match {
    case _: MemoizingSupplier[T] => f
    case _                       => MemoizingSupplier(checkNotNull(f))
  }

  /** Returns a supplier that caches the instance supplied by the delegate and
   *  removes the cached value after the specified time has passed. Subsequent
   *  evaluations return the cached value if the expiration time has
   *  not passed. After the expiration time, a new value is retrieved, cached,
   *  and returned. See:
   *  [[http://en.wikipedia.org/wiki/Memoization memoization]]
   *
   *  <p>The returned function is thread-safe. The function's serialized form
   *  does not contain the cached value, which will be recalculated when
   *  the reserialized instance is evaluated.
   *
   *  @param duration the length of time after a value is created that it
   *     should stop being returned by subsequent evaluations
   *  @param unit the unit that {@code duration} is expressed in
   *  @throws IllegalArgumentException if {@code duration} is not positive
   */
  def memoizeWithExpiration[T](f: () => T, duration: Long, unit: TimeUnit): () => T = {
    ExpiringMemoizingSupplier[T](checkNotNull(f), duration, unit)
  }

  /** Returns a function whose `apply()` method synchronizes on
   *  `f` before calling it, making it thread-safe.
   */
  def synchronizedSupplier[T](f: () => T): () => T = f match {
    case _: ThreadSafeSupplier[T] => f
    case _                        => ThreadSafeSupplier(checkNotNull(f))
  }

  /** Adds an `asJava` method that wraps a Scala function `() => T` in
   *  a Guava `Supplier[T]`.
   *
   *  The returned Guava `Supplier[T]` forwards all calls of the `apply` method
   *  to the given Scala function `() => T`.
   *
   *  @param fnc the Scala function `() => T` to wrap in a Guava `Supplier[T]`
   *  @return An object with an `asJava` method that returns a Guava `Supplier[T]`
   *   view of the argument
   */
  implicit final def asGuavaSupplierConverter[T](fnc: () => T): AsJava[GuavaSupplier[T]] = {
      def convert(fnc: () => T): GuavaSupplier[T] = fnc match {
        case s: AsMangoSupplier[T] => s.delegate
        case _                     => AsGuavaSupplier(fnc)
      }
    new AsJava(convert(fnc))
  }

  /** Adds an `asScala` method that wraps a Guava `Supplier[T]` in
   *  a Scala function `() => T`.
   *
   *  The returned Scala function `() => T` forwards all calls of the `apply` method
   *  to the given Guava `Supplier[T]`.
   *
   *  @param pred the Guava `Supplier[T]` to wrap in a Scala function `() => T`
   *  @return An object with an `asScala` method that returns a Scala function `() => T`
   *   view of the argument
   */
  implicit final def asMangoSupplierConverter[T](fnc: GuavaSupplier[T]): AsScala[() => T] = {
      def convert(fnc: GuavaSupplier[T]) = fnc match {
        case AsGuavaSupplier(delegate) => delegate
        case _                         => AsMangoSupplier(fnc)
      }
    new AsScala(convert(fnc))
  }
}

// visible for testing
@SerialVersionUID(1L)
private[mango] case class MemoizingSupplier[T](f: () => T) extends (() => T) with Serializable {
  import org.feijoas.mango.common.base.Suppliers._
  private val delegate = GuavaSuppliers.memoize(f.asJava)
  override def apply(): T = delegate.get()
  override def toString = "Suppliers.memoize(" + f + ")"
}

// visible for testing
@SerialVersionUID(1L)
private[mango] case class ExpiringMemoizingSupplier[T](f: () => T, duration: Long, unit: TimeUnit) extends (() => T) with Serializable {
  import org.feijoas.mango.common.base.Suppliers._
  private val delegate = GuavaSuppliers.memoizeWithExpiration(f.asJava, duration, unit)
  override def apply(): T = delegate.get()
  override def toString = "Suppliers.memoizeWithExpiration(" + f + ", " + duration + ", " + unit + ")"
}

// visible for testing
@SerialVersionUID(1L)
private[mango] case class ThreadSafeSupplier[T](f: () => T) extends (() => T) with Serializable {
  override def apply(): T = f.synchronized { f() }
  override def toString = "Suppliers.synchronizedSupplier(" + f + ")"
}

/** Wraps a Scala function in a Guava `Supplier`
 */
@SerialVersionUID(1L)
private[mango] case class AsGuavaSupplier[T](delegate: () => T)
    extends GuavaSupplier[T] with Serializable {
  checkNotNull(delegate)
  override def get() = delegate()
}

/** Wraps a Guava `Supplier` in a Scala function
 */
@SerialVersionUID(1L)
private[mango] case class AsMangoSupplier[T](delegate: GuavaSupplier[T])
    extends (() => T) with Serializable {
  checkNotNull(delegate)
  override def apply() = delegate.get()
}