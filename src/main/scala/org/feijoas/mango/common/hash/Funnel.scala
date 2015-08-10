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
import org.feijoas.mango.common.base.Preconditions.checkNotNull
import org.feijoas.mango.common.convert.{ AsJava, AsScala }

import com.google.common.hash.{ Funnel => GuavaFunnel, Funnels => GuavaFunnels, PrimitiveSink }

import java.nio.charset.Charset

/** A `Funnel` is a type class for an object which can send data from an object
 *  of type `T` into a `PrimitiveSink`.
 *
 *  <p>Note that serialization of a [[BloomFilter]] requires the proper
 *  serialization of funnels.
 *
 *  <p>From the Guava <a href="https://code.google.com/p/guava-libraries/wiki/HashingExplained/">Funnel tutorial</a>:
 *  {{{
 *  // A Funnel describes how to decompose a particular object type into primitive field values.
 *  // For example, if we had
 *  case class Person(id: Integer, firstName: String, lastName: String, birthYear: Int)
 *
 *  // our Funnel might look like
 *  implicit val personFunnel = new Funnel[Person] {
 *     override def funnel(person: Person, into: PrimitiveSink) = {
 *        into
 *        .putInt(person.id)
 *        .putString(person.firstName, Charsets.UTF_8)
 *        .putString(person.lastName, Charsets.UTF_8)
 *        .putInt(person.birthYear)
 *     }
 *  }
 *  }}}
 *
 *  @author Markus Schneider
 *  @since 0.6 (copied from Guava-libraries)
 */
@Beta
trait Funnel[T] extends Serializable {

  /** Sends a stream of data from the {@code from} object into the sink {@code into}. There
   *  is no requirement that this data be complete enough to fully reconstitute the object
   *  later.
   *
   */
  def funnel(from: T, into: PrimitiveSink)
}

/** Utility functions for the work with [[Funnel]].
 *
 *  Usage example for conversion between Guava and Mango:
 *  {{{
 *  // convert a Guava Funnel[T] to a Mango Funnel[T]
 *  import com.google.common.hash.{ Funnel => GuavaFunnel }
 *  val guavaFunnel: GuavaFunnel[T] = ...
 *  val mangoFunnel: Funnel[T] = guavaFunnel.asScala
 *
 *  import com.google.common.hash.{ Funnel => GuavaFunnel }
 *  val guavaFunnel: GuavaFunnel[CharSequence] = GuavaFunnels.stringFunnel
 *  val mangoFunnel: Funnel[CharSequence] = guavaFunnel.asScala
 *  }}}
 *
 *  @author Markus Schneider
 *  @since 0.6
 */
final object Funnel {

  /**
   * Returns a funnel that processes an `Iterable` by funneling its elements in iteration
   * order with the specified funnel. No separators are added between the elements.
   */
  def sequentialFunnel[E](elementFunnel: Funnel[E]):  Funnel[Iterable[E]] = new SequentialFunnel(elementFunnel)
   
  
 /**
   * Returns a funnel that encodes the characters of a `CharSequence` with the specified
   * `Charset`.
   */
  def stringFunnel(charset: Charset): Funnel[CharSequence] = new StringCharsetFunnel(charset)  
  
  /** Returns a funnel that extracts the characters from a `CharSequence`, a character at a
   * time, without performing any encoding. If you need to use a specific encoding, use
   * `stringFunnel(Charset)` instead.
   */
  implicit val unencodedCharsFunnel: Funnel[CharSequence] = UnencodedCharsFunnel
 
  /** Afunnel that extracts the bytes from a `Byte` array.
   */
  implicit val byteArrayFunnel: Funnel[Array[Byte]] = GuavaFunnels.byteArrayFunnel().asScala

  /** A funnel for Integers.
   */
  implicit val intFunnel: Funnel[Int] = IntFunnel

  /** A funnel for Longs.
   */
  implicit val longFunnel: Funnel[Long] = LongFunnel
  
  /** Adds an `asJava` method that wraps a Mango `Funnel[T]` in
   *  a Guava `Funnel[T]`.
   *
   *  The returned Guava `Funnel[T]` forwards all calls of the `funnel` method
   *  to the given Mango `Funnel[T]`.
   *
   *  @param funnel the Mango function `Funnel[T]` to wrap in a Guava `Funnel[T]`
   *  @return An object with an `asJava` method that returns a Guava `Funnel[T]`
   *   view of the argument
   */
  implicit final def asGuavaFunnel[T](funnel: Funnel[T]): AsJava[GuavaFunnel[T]] = {
      def convert(f: Funnel[T]): GuavaFunnel[T] = f match {
        case AsScalaFunnel(funnel) => funnel
        case _                     => AsGuavaFunnel(funnel)
      }
    new AsJava(convert(funnel))
  }

  /** Adds an `asScala` method that wraps a Guava `Funnel[T]` in
   *  a Mango `Funnel[T]`.
   *
   *  The returned Mango `Funnel[T]` forwards all calls of the `funnel` method
   *  to the given Guava `Funnel[T]`.
   *
   *  @param funnel the Guava `Funnel[T]` to wrap in a Mango `Funnel[T]`
   *  @return An object with an `asScala` method that returns a Mango `Funnel[T]`
   *   view of the argument
   */
  implicit final def asScalaFunnel[T](funnel: GuavaFunnel[T]): AsScala[Funnel[T]] = {
      def convert(f: GuavaFunnel[T]): Funnel[T] = f match {
        case AsGuavaFunnel(funnel) => funnel
        case _                     => AsScalaFunnel(funnel)
      }
    new AsScala(convert(funnel))
  }
}

/** Wraps a `Funnel[T]` in a Guava `Funnel[T]`
 */
@SerialVersionUID(1L)
private[mango] case class AsGuavaFunnel[T](f: Funnel[T]) extends GuavaFunnel[T] with Serializable {
  checkNotNull(f)
  override def funnel(from: T, into: PrimitiveSink) = f.funnel(from, into)
}

/** Wraps a Guava `Funnel[T]` in a `Funnel[T]`
 */
@SerialVersionUID(1L)
private[mango] case class AsScalaFunnel[T](f: GuavaFunnel[T]) extends Funnel[T] with Serializable {
  checkNotNull(f)
  override def funnel(from: T, into: PrimitiveSink) = f.funnel(from, into)
}

@SerialVersionUID(1L)
private[mango] final object LongFunnel extends Funnel[Long] with Serializable {
  val delegate = GuavaFunnels.longFunnel()
  override def funnel(from: Long, into: PrimitiveSink) = delegate.funnel(from, into)
  override def toString = delegate.toString
}

@SerialVersionUID(1L)
private[mango] final object UnencodedCharsFunnel extends Funnel[CharSequence] with Serializable {
  val delegate = GuavaFunnels.unencodedCharsFunnel()
  override def funnel(from: CharSequence, into: PrimitiveSink) = delegate.funnel(from, into)
  override def toString = delegate.toString
}

@SerialVersionUID(1L)
private[mango] final object IntFunnel extends Funnel[Int] with Serializable {
  val delegate = GuavaFunnels.integerFunnel()
  override def funnel(from: Int, into: PrimitiveSink) = delegate.funnel(from, into)
  override def toString = delegate.toString
}

@SerialVersionUID(1L)
private[mango] final class StringCharsetFunnel(charset: Charset) extends Funnel[CharSequence] with Serializable {
  val delegate = GuavaFunnels.stringFunnel(charset)
  override def funnel(from: CharSequence, into: PrimitiveSink) = delegate.funnel(from, into)
  override def toString = delegate.toString
}

@SerialVersionUID(1L)
private[mango] final class SequentialFunnel[E](elementFunnel:Funnel[E]) extends Funnel[Iterable[E]] with Serializable {
  import scala.collection.JavaConversions._
  val guavaElementFunnel:GuavaFunnel[E] = AsGuavaFunnel(elementFunnel)
  val delegate = GuavaFunnels.sequentialFunnel(guavaElementFunnel)
  override def funnel(from: Iterable[E], into: PrimitiveSink) = delegate.funnel(asJavaIterable(from), into)
  override def toString = delegate.toString
}


