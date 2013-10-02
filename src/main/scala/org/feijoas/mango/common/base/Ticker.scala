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

import org.feijoas.mango.common.annotations.Beta
import com.google.common.base.{ Ticker => GuavaTicker }
import org.feijoas.mango.common.convert.{ AsJava, AsScala }

/** A time source; returns a time value representing the number of nanoseconds elapsed since some
 *  fixed but arbitrary point in time. Note that most users should use `Stopwatch` instead of
 *  interacting with this class directly.
 *
 *  <p><b>Warning:</b> this interface can only be used to measure elapsed time, not wall time.
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from guava-libraries)
 */
@Beta
trait Ticker {

  /** Returns the number of nanoseconds elapsed since this ticker's fixed
   *  point of reference.
   */
  def read(): Long
}

/** Factory for [[Ticker]] instances. */
object Ticker {

  /** A ticker that reads the current time using `System#nanoTime`.
   */
  def systemTicker() = GuavaTicker.systemTicker().asScala

  /** Adds an `asJava` method that wraps a Mango `Ticker` in
   *  a Guava `Ticker`.
   *
   *  The returned Guava `Ticker` forwards all calls of the `read` method
   *  to the given Scala `ticker`.
   *
   *  @param ticker the Mango `Ticker` to wrap in a Guava `Ticker`
   *  @return An object with an `asJava` method that returns a Guava `Ticker`
   *   view of the argument
   */
  implicit final def asGuavaTickerConverter[T](ticker: Ticker): AsJava[GuavaTicker] = {
      def convert(ticker: Ticker): GuavaTicker = new GuavaTicker {
        override def read() = ticker.read()
      }
    new AsJava(convert(ticker))
  }

  /** Adds an `asScala` method that wraps a Guava `Ticker` in
   *  a Mango `Ticker`.
   *
   *  The returned Mango `Ticker` forwards all calls of the `read` method
   *  to the given Guava `ticker`.
   *
   *  @param ticker the Guava `Ticker` to wrap in a Mango `Ticker`
   *  @return An object with an `asScala` method that returns a Mango `Ticker`
   *   view of the argument
   */
  implicit final def asMangoTickerConverter[T](ticker: GuavaTicker): AsScala[Ticker] = {
      def convert(ticker: GuavaTicker): Ticker = new Ticker {
        override def read() = ticker.read()
      }
    new AsScala(convert(ticker))
  }
}