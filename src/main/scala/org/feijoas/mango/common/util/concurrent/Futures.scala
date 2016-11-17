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
package org.feijoas.mango.common.util.concurrent

import java.util.concurrent.{ Executor, TimeUnit }
import scala.concurrent.{ Await, CanAwait, ExecutionContext, Future, TimeoutException }
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.util.{ Failure, Success, Try }
import com.google.common.util.concurrent.{ FutureCallback, Futures => GuavaFutures, ListenableFuture, AbstractFuture }
import concurrent.{ ExecutionException, TimeoutException }
import org.feijoas.mango.common.base.Preconditions.checkNotNull
import org.feijoas.mango.common.convert._
import scala.concurrent.Promise
import com.google.common.util.concurrent.MoreExecutors

/**
 * Utility functions for the work with `Future[T]` and Guava `FutureCallback[T]`,
 *  `ListenableFuture[T]`
 *
 *  Usage example for conversion between Guava and Mango:
 *  {{{
 *  // convert a Scala Future[T] to a Guava ListenableFuture[Int]
 *  val scalaFuture: Future[Int] = future { ... }
 *  val guavaFuture: ListenableFuture[Int] = scalaFuture.asJava
 *
 *  // convert a Scala Try[T] => U to a Guava FutureCallback[T]
 *  val callback: Try[T] => U = { ... }
 *  val guavaCallback: FutureCallback[T] = callback.asJava
 *
 *  // convert a Guava ListenableFuture[Int] to a Scala Future[T]
 *  val guavaFuture: ListenableFuture[T] = ...
 *  val scalaFuture: Future[Int] = guavaFuture.asScala
 *  }}}
 *
 *  @author Markus Schneider
 *  @since 0.7
 */
final object Futures {

  /**
   * Adds an `asJava` method that wraps a Scala function `Try[T] => U` in a
   *  Guava `FutureCallback[T]`.
   *
   *  The returned Guava `FutureCallback[T]` forwards all method calls to the
   *  provided Scala function `Try[T] => U`.
   *
   *  @param callback the Scala function `Try[T] => U` to wrap in a Guava `FutureCallback[T]`
   *  @return An object with an `asJava` method that returns a Guava `FutureCallback[T]`
   *   view of the argument
   */
  implicit def asGuavaFutureCallbackConverter[T, U](callback: Try[T] => U): AsJava[FutureCallback[T]] = {
    new AsJava(asGuavaFutureCallback(callback))
  }

  /**
   * converts a function `Try[T] => U` to a `FutureCallback[T]`
   */
  private[mango] def asGuavaFutureCallback[T, U](c: Try[T] => U): FutureCallback[T] = {
    new FutureCallback[T] {
      checkNotNull(c)
      override def onSuccess(result: T) = c(Success(result))
      override def onFailure(t: Throwable) = c(Failure(t))
    }
  }

  /**
   * Adds an `asJava` method that wraps a Scala `Future[T]` in a
   *  Guava `ListenableFuture[T]`.
   *
   *  The returned Guava `ListenableFuture[T]` forwards all method calls to the
   *  provided `Future[T]`.
   *
   *  '''Note:''' A call on the method `cancel` has no effect since it cannot
   *  be delegated to the Scala `Future[T]`
   *
   *  @param future the Scala `Future[T]` to wrap in a Guava `ListenableFuture[T]`
   *  @return An object with an `asJava` method that returns a Guava `ListenableFuture[T]`
   *   view of the argument
   */
  implicit def asGuavaFutureConverter[T](future: Future[T]): AsJava[ListenableFuture[T]] = {
    def convert(future: Future[T]): ListenableFuture[T] = future match {
      case s: AsMangoFuture[T] => s.delegate
      case _                   => AsGuavaFuture[T](future)
    }
    new AsJava(convert(future))
  }

  /**
   * Adds an `asScala` method that wraps Guava `ListenableFuture[T]` in a Scala
   *  `Future[T]`.
   *
   *  The returned Scala `Future[T]` forwards all method calls to the provided
   *  Guava `ListenableFuture[T]`.
   *
   *  @param cache the Guava `ListenableFuture[T]` to wrap in a Scala `Future[T]`
   *  @return An object with an `asScala` method that returns a Scala `Future[T]`
   *   view of the argument
   */
  implicit def asScalaFutureConverter[T](future: ListenableFuture[T]): AsScala[Future[T]] = {
    def convert(future: ListenableFuture[T]): Future[T] = future match {
      case s: AsGuavaFuture[T] => s.delegate
      case _                   => AsMangoFuture[T](future)
    }
    new AsScala(convert(future))
  }
}

/**
 * Wraps a Scala `Future` in a Guava `ListenableFuture`
 */
@SerialVersionUID(1L)
private[mango] case class AsGuavaFuture[T](delegate: Future[T]) extends ListenableFuture[T] {
  override def addListener(listener: Runnable, executor: Executor): Unit = {
    delegate.onComplete(_ => listener.run())(ExecutionContext.fromExecutor(executor))
  }
  override def isCancelled: Boolean = false
  override def get(): T = tryGet(Await.result(delegate, Duration.Inf))
  override def get(timeout: Long, unit: TimeUnit): T = tryGet(Await.result(delegate, Duration.create(timeout, unit)))
  override def cancel(mayInterruptIfRunning: Boolean): Boolean = throw new UnsupportedOperationException("cancel is not supported")
  override def isDone: Boolean = delegate.isCompleted

  private def tryGet(f: => T) = try (f) catch {
    case e: ExecutionException => throw (e)
    case t: Throwable          => throw new ExecutionException(t)
  }
}

/**
 * Wraps a Guava `ListenableFuture` in a Scala `Future`
 */
@SerialVersionUID(1L)
private[mango] case class AsMangoFuture[T](delegate: ListenableFuture[T]) extends Future[T] {
  checkNotNull(delegate)
  import org.feijoas.mango.common.util.concurrent.Futures.asGuavaFutureCallbackConverter

  override def onComplete[U](callback: Try[T] => U)(implicit ec: ExecutionContext): Unit = {
    GuavaFutures.addCallback(delegate, checkNotNull(callback).asJava)
  }

  override def isCompleted: Boolean = delegate.isDone()

  override def value: Option[Try[T]] = isCompleted match {
    case false => None
    case true  => Some(Try(delegate.get()))
  }

  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  override def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
    checkNotNull(atMost)
    import Duration.Undefined
    try {
      atMost match {
        case Duration.Inf      => delegate.get()
        case f: FiniteDuration => delegate.get(f.toNanos, TimeUnit.NANOSECONDS)
        case _                 => throw new IllegalArgumentException("atMost must be finite or Duration.Inf")
      }
    } catch {
      case e: TimeoutException     => throw e
      case e: InterruptedException => throw e
      case _: Throwable            => // other exceptions will be thrown if result is called
    }
    this
  }

  @throws(classOf[Exception])
  override def result(atMost: Duration)(implicit permit: CanAwait): T =
    ready(checkNotNull(atMost)).value.get match {
      case Failure(e) => throw e
      case Success(r) => r
    }
}
