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
package org.feijoas.mango.common.cache

import scala.math.max

import org.feijoas.mango.common.base.Preconditions.checkArgument
import org.feijoas.mango.common.convert.AsScala

import com.google.common.base.MoreObjects
import com.google.common.cache.{ CacheStats => GuavaCacheStats }

/**
 * Statistics about the performance of a [[Cache]]. Instances of this class are immutable.
 *
 *  <p>Cache statistics are incremented according to the following rules:
 *
 *  <ul>
 *  <li>When a cache lookup encounters an existing cache entry `hitCount` is incremented.
 *  <li>When a cache lookup first encounters a missing cache entry, a new entry is loaded.
 *  <ul>
 *  <li>After successfully loading an entry `missCount` and `loadSuccessCount` are
 *     incremented, and the total loading time, in nanoseconds, is added to
 *     `totalLoadTime`.
 *  <li>When an exception is thrown while loading an entry, `missCount` and `loadExceptionCount`
 *     are incremented, and the total loading time, in nanoseconds, is
 *     added to `totalLoadTime`.
 *  <li>Cache lookups that encounter a missing cache entry that is still loading will wait
 *     for loading to complete (whether successful or not) and then increment `missCount`.
 *  </ul>
 *  <li>When an entry is evicted from the cache, `evictionCount` is incremented.
 *  <li>No stats are modified when a cache entry is invalidated or manually removed.
 *  <li>No stats are modified on a query to `Cache#getIfPresent`.
 *  </ul>
 *
 *  <p>A lookup is specifically defined as an invocation of one of the methods
 *  `LoadingCache#get(key)`,
 *  `Cache#get(key, loader)`, or `LoadingCache#getAll(keys)`.
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from Guava-libraries)
 */
case class CacheStats(
    /**
     * Returns the number of times [[Cache]] lookup methods have returned a cached value.
     */
    hitCount: Long,

    /**
 * Returns the number of times [[Cache]] lookup methods have returned an uncached (newly
 *  loaded) value, or null. Multiple concurrent calls to [[Cache]] lookup methods on an absent
 *  value can result in multiple misses, all returning the results of a single cache load
 *  operation.
 */
    missCount: Long,

    /**
 * Returns the number of times [[Cache]] lookup methods have successfully loaded a new value.
 *  This is always incremented in conjunction with {@link #missCount}, though `missCount`
 *  is also incremented when an exception is encountered during cache loading (see
 *  `#loadExceptionCount`). Multiple concurrent misses for the same key will result in a
 *  single load operation.
 */
    loadSuccessCount: Long,

    /**
 * Returns the number of times [[Cache]] lookup methods threw an exception while loading a
 *  new value. This is always incremented in conjunction with {@code missCount}, though
 *  `missCount` is also incremented when cache loading completes successfully (see
 *  `#loadSuccessCount`). Multiple concurrent misses for the same key will result in a
 *  single load operation.
 */
    loadExceptionCount: Long,

    /**
 * Returns the total number of nanoseconds the cache has spent loading new values. This can be
 *  used to calculate the miss penalty. This value is increased every time
 *  `loadSuccessCount` or `loadExceptionCount` is incremented.
 */
    totalLoadTime: Long,

    /**
 * Returns the number of times an entry has been evicted. This count does not include manual
 *  `Cache#invalidate` invalidations.
 */
    evictionCount: Long) {

  // only non-negative values are allowed
  checkArgument(hitCount >= 0)
  checkArgument(missCount >= 0)
  checkArgument(loadSuccessCount >= 0)
  checkArgument(loadExceptionCount >= 0)
  checkArgument(totalLoadTime >= 0)
  checkArgument(evictionCount >= 0)

  /**
   * Returns the number of times [[Cache]] lookup methods have returned either a cached or
   *  uncached value. This is defined as `hitCount + missCount`.
   */
  def requestCount = hitCount + missCount

  /**
   * Returns the ratio of cache requests which were hits. This is defined as
   *  `hitCount / requestCount`, or `1.0` when `requestCount == 0`.
   *  Note that `hitRate + missRate =~ 1.0`.
   */
  def hitRate(): Double = requestCount match {
    case 0        => 1
    case _@ count => hitCount.toDouble / count
  }

  /**
   * Returns the ratio of cache requests which were misses. This is defined as
   *  `missCount / requestCount`, or `0.0` when `requestCount == 0`.
   *  Note that `hitRate + missRate =~ 1.0`. Cache misses include all requests which
   *  weren't cache hits, including requests which resulted in either successful or failed loading
   *  attempts, and requests which waited for other threads to finish loading. It is thus the case
   *  that `missCount &gt;= loadSuccessCount + loadExceptionCount`. Multiple
   *  concurrent misses for the same key will result in a single load operation.
   */
  def missRate(): Double = requestCount match {
    case 0        => 0
    case _@ count => missCount.toDouble / requestCount
  }

  /**
   * Returns the total number of times that [[Cache]] lookup methods attempted to load new
   *  values. This includes both successful load operations, as well as those that threw
   *  exceptions. This is defined as `loadSuccessCount + loadExceptionCount`.
   */
  def loadCount = loadSuccessCount + loadExceptionCount

  /**
   * Returns the ratio of cache loading attempts which threw exceptions. This is defined as
   *  `loadExceptionCount / (loadSuccessCount + loadExceptionCount)`, or
   *  `0.0` when `loadSuccessCount + loadExceptionCount == 0`.
   */
  def loadExceptionRate(): Double = loadCount match {
    case 0        => 0
    case _@ count => loadExceptionCount.toDouble / count
  }

  /**
   * Returns the average time spent loading new values. This is defined as
   *  `totalLoadTime / (loadSuccessCount + loadExceptionCount)`.
   */
  def averageLoadPenalty(): Double = loadCount match {
    case 0        => 0
    case _@ count => totalLoadTime.toDouble / count
  }

  /**
   * Returns a new [[CacheStats]] representing the difference between this [[CacheStats]]
   *  and `other`. Negative values, which aren't supported by [[CacheStats]] will be
   *  rounded up to zero.
   */
  def -(other: CacheStats): CacheStats = {
    import scala.math._
    CacheStats(
      max(0, hitCount - other.hitCount),
      max(0, missCount - other.missCount),
      max(0, loadSuccessCount - other.loadSuccessCount),
      max(0, loadExceptionCount - other.loadExceptionCount),
      max(0, totalLoadTime - other.totalLoadTime),
      max(0, evictionCount - other.evictionCount))
  }

  /**
   * Returns a new [[CacheStats]] representing the sum of this [[CacheStats]]
   *  and `other`.
   */
  def +(other: CacheStats): CacheStats = {
    CacheStats(
      hitCount + other.hitCount,
      missCount + other.missCount,
      loadSuccessCount + other.loadSuccessCount,
      loadExceptionCount + other.loadExceptionCount,
      totalLoadTime + other.totalLoadTime,
      evictionCount + other.evictionCount)
  }

  override def toString = {
    MoreObjects.toStringHelper(this)
      .add("hitCount", hitCount)
      .add("missCount", missCount)
      .add("loadSuccessCount", loadSuccessCount)
      .add("loadExceptionCount", loadExceptionCount)
      .add("totalLoadTime", totalLoadTime)
      .add("evictionCount", evictionCount)
      .toString();
  }
}

/** [[CacheStats]] helper functions */
final object CacheStats {

  /**
   * Adds an `asScala` method that converts Guava `CacheStats` to
   *  Mango `CacheStats`.
   *
   *  The returned Mango `CacheStats` contains a copy of all values in the
   *  Guava `CacheStats`.
   *
   *  @param stats the Guava `CacheStats` to convert to Mango `CacheStats`
   *  @return An object with an `asScala` method that returns a Mango `CacheStats`
   *   view of the argument
   */
  implicit def asMangoCacheStatsConverter(stats: GuavaCacheStats): AsScala[CacheStats] = new AsScala(
    CacheStats(
      stats.hitCount(),
      stats.missCount(),
      stats.loadSuccessCount(),
      stats.loadExceptionCount(),
      stats.totalLoadTime(),
      stats.evictionCount()))
}
