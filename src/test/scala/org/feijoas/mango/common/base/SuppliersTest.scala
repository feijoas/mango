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

import java.lang.Thread.State.{ BLOCKED, TIMED_WAITING, WAITING }
import java.util.concurrent.{ TimeUnit, TimeoutException }
import java.util.concurrent.atomic.{ AtomicInteger, AtomicReference }

import scala.util.control.Breaks.{ break, breakable }

import org.feijoas.mango.common.base.Suppliers._
import org.scalatest.{ FlatSpec, ShouldMatchers }
import org.scalatest.prop.PropertyChecks

import com.google.common.base.{ Supplier => GuavaSupplier }
import com.google.common.testing.SerializableTester

/** Tests for [[Suppliers]]
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from guava-libraries)
 */
class SuppliersTest extends FlatSpec with ShouldMatchers with PropertyChecks {
  behavior of "memoize"

  it should "memoize" in {
    val count = new CountingSupplier()
    checkMemoize(count, memoize(count))
  }

  it should "return the same instance if already memoized" in {
    val count = new CountingSupplier()
    val mem = memoize(count)
    mem should be theSameInstanceAs memoize(mem)
  }

  it should "be serializeable" in {
    val count = new CountingSupplier()
    val mem = memoize(count)

    checkMemoize(count, mem)
    mem()

    val memCopy = SerializableTester.reserialize(mem)
    mem()

    val countCopy = memCopy.asInstanceOf[MemoizingSupplier[Int]].f.asInstanceOf[CountingSupplier]
    checkMemoize(countCopy, memCopy)
  }

  it should "propagate exceptions" in {

    val bad: () => Int = () => throw new NullPointerException
    val mem = memoize(bad)

    intercept[NullPointerException] {
      mem()
    }

    intercept[NullPointerException] {
      mem()
    }
  }

  it should "be thread-safe" in {
    val memoizer = (supplier: () => Boolean) => memoize(supplier)
    testSupplierThreadSafe(memoizer)
  }

  behavior of "memoize with expiration"

  it should "memoize and expire" in {
    val count = new CountingSupplier()
    val mem = memoizeWithExpiration(count, 75, TimeUnit.MILLISECONDS)
    checkMemoize(count, mem)
  }

  it should "be serializeable" in {
    val count = new CountingSupplier()
    val mem = memoizeWithExpiration(count, 75, TimeUnit.MILLISECONDS)
    checkMemoize(count, mem)
    mem()

    val memCopy = SerializableTester.reserialize(mem)
    mem()

    val countCopy = memCopy.asInstanceOf[ExpiringMemoizingSupplier[Int]].f.asInstanceOf[CountingSupplier]
    checkMemoize(countCopy, memCopy)
  }

  it should "be thread-safe" in {
    val memoizer = (supplier: () => Boolean) =>
      memoizeWithExpiration(supplier, java.lang.Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    testSupplierThreadSafe(memoizer)
  }

  behavior of "implicits"

  it should "be serializeable" in {
    import org.feijoas.mango.common.base.Suppliers._

    val gs: GuavaSupplier[Int] = { () => 1 } asJava
    val cs: () => Int = {
      new GuavaSupplier[Int] with Serializable {
        override def get() = 1
      }
    }.asScala

    SerializableTester.reserialize(gs)
    SerializableTester.reserialize(cs)
  }

  behavior of "synchronized supplier"

  it should "synchonize on non-thread-safe supplier" in {

    val nonThreadSafe = new Function0[Int] {
      var counter = 0

      override def apply(): Int = {
        val nextValue = counter + 1
        Thread.`yield`
        counter = nextValue
        counter
      }
    }

    val numThreads = 10
    val iterations = 1000
    val threads = Array.ofDim[Thread](numThreads)
    for (i <- 0 until numThreads) {
      threads(i) = new Thread() {
        override def run() {
          for (j <- 0 until iterations) {
            Suppliers.synchronizedSupplier(nonThreadSafe)()
          }
        }
      }
    }
    for (i <- 0 until numThreads) threads(i).start()
    for (i <- 0 until numThreads) threads(i).join()

    nonThreadSafe() should be(numThreads * iterations + 1)
  }

  private def checkMemoize(count: CountingSupplier, mem: () => Int) = {
    count.calls should be(0) // check that in initial state
    mem() should be(10) // first call to mem should delegate to count
    count.calls should be(1) // was called once
    mem() should be(10) // mem should memoize
    count.calls should be(1) // still one call to count

  }

  private def checkExpiration(count: CountingSupplier, mem: () => Int) {
    checkMemoize(count, mem)

    Thread.sleep(150)

    // should be expired now
    mem() should be(20)
    count.calls should be(2)

    // it still should only have executed twice due to memoization
    mem() should be(20)
    count.calls should be(2)
  }

  def testSupplierThreadSafe(memoizer: (() => Boolean) => (() => Boolean)) = {
    val count = new AtomicInteger(0)
    val thrown = new AtomicReference[Throwable](null)
    val numThreads = 3
    val threads = Array.ofDim[Thread](numThreads)
    val timeout = TimeUnit.SECONDS.toNanos(60)

    val supplier = new Function0[Boolean]() {
      import java.lang.Thread.State._
      def isWaiting(thread: Thread): Boolean = thread.getState() match {
        case BLOCKED       => true
        case WAITING       => true
        case TIMED_WAITING => true
        case _             => false
      }

      def waitingThreads(): Int = {
        var waitingThreads = 0
        for (i <- 0 until numThreads) {
          if (isWaiting(threads(i))) {
            waitingThreads = waitingThreads + 1
          }
        }
        return waitingThreads
      }

      override def apply(): Boolean = {
        import scala.util.control.Breaks._

        // Check that this method is called exactly once, by the first
        // thread to synchronize.
        val t0 = System.nanoTime()
        breakable {
          while (waitingThreads() != numThreads - 1) {
            if (System.nanoTime() - t0 > timeout) {
              thrown.set(new TimeoutException(
                "timed out waiting for other threads to block" +
                  " synchronizing on supplier"));
              break
            }
            Thread.`yield`
          }
        }
        count.getAndIncrement()
        return true
      }
    }

    val memoizedSupplier = memoizer.apply(supplier)

    for (i <- 0 until numThreads) {
      threads(i) = new Thread() {
        override def run() = {
          memoizedSupplier() should be(true)
        }
      }
    }

    for (i <- 0 until numThreads) threads(i).start()
    for (i <- 0 until numThreads) threads(i).join()

    if (thrown.get() != null) {
      throw thrown.get();
    }

    count.get() should be(1)
  }
}

private class CountingSupplier() extends (() => Int) with Serializable {
  @transient var calls = 0
  override def apply() = {
    calls = calls + 1
    calls * 10
  }
}
