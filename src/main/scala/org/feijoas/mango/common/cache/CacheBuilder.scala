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

import scala.concurrent.duration._
import com.google.common.cache.{ CacheBuilder => GuavaCacheBuilder }
import org.feijoas.mango.common.base.Ticker
import org.feijoas.mango.common.base.Preconditions._
import org.feijoas.mango.common.cache._
import org.feijoas.mango.common.cache.CacheWrapper._
import org.feijoas.mango.common.cache.CacheLoaderWrapper._
import org.feijoas.mango.common.cache.LoadingCacheWrapper._
import org.feijoas.mango.common.cache.Weigher._
import org.feijoas.mango.common.cache.RemovalListener._

/** A builder of [[LoadingCache]] and [[Cache]] instances having any combination of the
 *  following features:
 *
 *  <ul>
 *  <li>automatic loading of entries into the cache
 *  <li>least-recently-used eviction when a maximum size is exceeded
 *  <li>time-based expiration of entries, measured since last access or last write
 *  <li>keys automatically wrapped in {@linkplain WeakReference weak} references
 *  <li>values automatically wrapped in {@linkplain WeakReference weak} or
 *     {@linkplain SoftReference soft} references
 *  <li>notification of evicted (or otherwise removed) entries
 *  <li>accumulation of cache access statistics
 *  </ul>
 *
 *  These features are all optional; caches can be created using all or none of them. By default
 *  cache instances created by {@code CacheBuilder} will not perform any type of eviction.
 *
 *  Usage example:
 *  {{{
 *
 *    val createExpensiveGraph: Key => Graph = ...
 *
 *    val graphs: LoadingCache[Key, Graph] = CacheBuilder.newBuilder()
 *        .maximumSize(10000)
 *        .expireAfterWrite(10, TimeUnit.MINUTES)
 *        .removalListener(MY_LISTENER)
 *        .build(createExpensiveGraph)
 *
 *  }}}
 *
 *  This class just delegates to the [[http://code.google.com/p/guava-libraries/ CacheBuilder implementation in Guava]]
 *
 *
 *  '''Note:''' by default, the returned cache uses equality comparisons to determine
 *  equality for keys or values. However, if `weakKeys` was specified, the cache uses identity (`eq`)
 *  comparisons instead for keys. Likewise, if `weakValues` or `softValues` was
 *  specified, the cache uses identity comparisons for values.
 *
 *
 *  Entries are automatically evicted from the cache when any of
 *  <ul>
 *  <li>`maximumSize(Long)`
 *  <li>`maximumWeight(Long)`
 *  <li>`expireAfterWrite`
 *  <li>`expireAfterAccess`
 *  <li>`weakKeys`
 *  <li>`weakValues`
 *  <li>`softValues`
 *  </ul>
 *  are requested.
 *
 *
 *  If `maximumSize` or `maximumWeight` is requested entries may be evicted on each cache
 *  modification.
 *
 *  If `expireAfterWrite` or `#expireAfterAccess` is requested entries may be evicted on each
 *  cache modification, on occasional cache accesses, or on calls to `Cache.cleanUp()`. Expired
 *  entries may be counted in `Cache.size()`}, but will never be visible to read or write
 *  operations.
 *
 *  If `weakKeys`, `weakValues`, or `softValues` are requested, it is possible for a key or value present in
 *  the cache to be reclaimed by the garbage collector. Entries with reclaimed keys or values may be
 *  removed from the cache on each cache modification, on occasional cache accesses, or on calls to
 *  `Cache.cleanUp()`; such entries may be counted in `Cache.size()`, but will never be
 *  visible to read or write operations.
 *
 *  Certain cache configurations will result in the accrual of periodic maintenance tasks which
 *  will be performed during write operations, or during occasional read operations in the absense of
 *  writes. The `Cache.cleanUp()` method of the returned cache will also perform maintenance, but
 *  calling it should not be necessary with a high throughput cache. Only caches built with
 *  `removalListener`, `expireAfterWrite`, `expireAfterAccess`, `weakKeys`, `weakValues`, or
 *  `softValues` perform periodic maintenance.
 *
 *  The caches produced by [[CacheBuilder]] are serializable, and the deserialized caches
 *  retain all the configuration properties of the original cache. Note that the serialized form does
 *  <i>not</i> include cache contents, but only configuration.
 *
 *  <p>'''Warning:''' This implementation differs from Guava. It is truly immutable and returns a new
 *  instance on each method call. Use the standard method-chaining idiom,
 *  as illustrated in the documentation above. This has the advantage that it is
 *  not possible to create a cache whose key or value type is incompatible with e.g. the weigher. This
 *  `CacheBuilder` is also contravariant in both type `K` and `V`.
 *
 *
 *  See the [[http://code.google.com/p/guava-libraries/ Guava User Guide]] for
 *  more information.
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from Guava-libraries)
 *
 *  @tparam K the key of the cache
 *  @tparam V the value of the cache
 */
final case class CacheBuilder[-K, -V] private (
    private val withWeigher: Option[(K, V) => Int],
    private val withRemovalListener: Option[RemovalNotification[K, V] => Unit],
    private val withInitialCapacity: Option[Int],
    private val withConcurrencyLevel: Option[Int],
    private val withMaximumSize: Option[Long],
    private val withMaximumWeight: Option[Long],
    private val withWeakKeys: Option[Boolean],
    private val withWeakValues: Option[Boolean],
    private val withSoftValues: Option[Boolean],
    private val withExpireAfterWrite: Option[(Long, TimeUnit)],
    private val withExpireAfterAccess: Option[(Long, TimeUnit)],
    private val withRefreshAfterWrite: Option[(Long, TimeUnit)],
    private val withTicker: Option[Ticker],
    private val withRecordStats: Option[Boolean]) {

  /** Builds a cache which does not automatically load values when keys are requested.
   *
   *  <p>Consider `build(CacheLoader)` instead, if it is feasible to implement a
   *  [[CacheLoader]].
   *
   *  <p>This method does not alter the state of this [[CacheBuilder]] instance, so it can be
   *  invoked again to create multiple independent caches.
   *
   *  @return a cache having the requested features
   */
  def build[K1 <: K, V1 <: V](): Cache[K1, V1] = {
    val guavaBuilder: GuavaCacheBuilder[K1, V1] = CacheBuilder.createGuavaBuilder(this)
    CacheWrapper(guavaBuilder.build())
  }

  /** Builds a cache, which either returns an already-loaded value for a given key or atomically
   *  computes or retrieves it using the supplied `loader` (the function used to compute corresponding values).
   *  If another thread is currently
   *  loading the value for this key, simply waits for that thread to finish and returns its
   *  loaded value. Note that multiple threads can concurrently load values for distinct keys.
   *
   *  <p>This method does not alter the state of this [[CacheBuilder]] instance, so it can be
   *  invoked again to create multiple independent caches.
   *
   *  @param loader the cache loader used to obtain new values
   *  @return a cache having the requested features
   */
  def build[K1 <: K, V1 <: V](loader: K1 => V1): LoadingCache[K1, V1] = {
    build(CacheLoader.from(loader))
  }

  /** Builds a cache, which either returns an already-loaded value for a given key or atomically
   *  computes or retrieves it using the supplied [[CacheLoader]]. If another thread is currently
   *  loading the value for this key, simply waits for that thread to finish and returns its
   *  loaded value. Note that multiple threads can concurrently load values for distinct keys.
   *
   *  <p>This method does not alter the state of this [[CacheBuilder]] instance, so it can be
   *  invoked again to create multiple independent caches.
   *
   *  @param loader the cache loader used to obtain new values
   *  @return a cache having the requested features
   */
  def build[K1 <: K, V1 <: V](loader: CacheLoader[K1, V1]): LoadingCache[K1, V1] = {
    checkNotNull(loader)
    val guavaBuilder: GuavaCacheBuilder[K1, V1] = CacheBuilder.createGuavaBuilder(this)
    LoadingCacheWrapper(guavaBuilder.build(loader.asJava))
  }

  /** Specifies the weigher to use in determining the weight of entries. Entry weight is taken
   *  into consideration by `maximumWeight(Long)` when determining which entries to evict, and
   *  use of this method requires a corresponding call to `maximumWeight(Long)` prior to
   *  calling `build`. Weights are measured and recorded when entries are inserted into the
   *  cache, and are thus effectively static during the lifetime of a cache entry.
   *
   *  <p>When the weight of an entry is zero it will not be considered for size-based eviction
   *  (though it still may be evicted by other means).
   *
   *  @param weigher the weigher to use in calculating the weight of cache entries
   */
  def weigher[K1 <: K, V1 <: V](weigher: (K1, V1) => Int): CacheBuilder[K1, V1] = {
    checkNotNull(weigher)
    checkState(withWeigher == None, "weigher was already set")
    checkState(withMaximumSize == None, "weigher can not be combined with maximum size")
    copy(withWeigher = Some(weigher))
  }

  /** Specifies a listener instance that caches should notify each time an entry is removed for any
   *  [[RemovalCause]] reason. Each cache created by this builder will invoke this listener
   *  as part of the routine maintenance described in the class documentation above.
   *
   *  <p>'''Warning:''' any exception thrown by `listener` will <i>not</i> be propagated to
   *  the [[Cache]] user, only logged via a `Logger`.
   *
   *  @return the cache builder reference that should be used instead of {@code this} for any
   *     remaining configuration and cache building
   */
  def removalListener[K1 <: K, V1 <: V](listener: (RemovalNotification[K1, V1]) => Unit): CacheBuilder[K1, V1] = {
    checkNotNull(listener)
    checkState(withRemovalListener == None, "removal listener was already set")
    copy(withRemovalListener = Some(listener))
  }

  /** Sets the minimum total size for the internal hash tables. For example, if the initial capacity
   *  is `60`, and the concurrency level is `8`, then eight segments are created, each
   *  having a hash table of size eight. Providing a large enough estimate at construction time
   *  avoids the need for expensive resizing operations later, but setting this value unnecessarily
   *  high wastes memory.
   *
   *  @throws IllegalArgumentException if {@code initialCapacity} is negative
   */
  def initialCapacity(initialCapacity: Int): CacheBuilder[K, V] = {
    checkNotNull(initialCapacity)
    checkState(withInitialCapacity == None, "initial capacity was already set to %s", withInitialCapacity)
    checkArgument(initialCapacity >= 0)
    copy(withInitialCapacity = Some(initialCapacity))
  }

  /** Guides the allowed concurrency among update operations. Used as a hint for internal sizing. The
   *  table is internally partitioned to try to permit the indicated number of concurrent updates
   *  without contention. Because assignment of entries to these partitions is not necessarily
   *  uniform, the actual concurrency observed may vary. Ideally, you should choose a value to
   *  accommodate as many threads as will ever concurrently modify the table. Using a significantly
   *  higher value than you need can waste space and time, and a significantly lower value can lead
   *  to thread contention. But overestimates and underestimates within an order of magnitude do not
   *  usually have much noticeable impact. A value of one permits only one thread to modify the cache
   *  at a time, but since read operations and cache loading computations can proceed concurrently,
   *  this still yields higher concurrency than full synchronization.
   *
   *  <p> Defaults to `4`. '''Note:''' The default may change in the future. If you care about this
   *  value, you should always choose it explicitly.
   *
   *  <p>The current implementation uses the concurrency level to create a fixed number of hashtable
   *  segments, each governed by its own write lock. The segment lock is taken once for each explicit
   *  write, and twice for each cache loading computation (once prior to loading the new value,
   *  and once after loading completes). Much internal cache management is performed at the segment
   *  granularity. For example, access queues and write queues are kept per segment when they are
   *  required by the selected eviction algorithm. As such, when writing unit tests it is not
   *  uncommon to specify `concurrencyLevel(1)` in order to achieve more deterministic eviction
   *  behavior.
   *
   *  <p>Note that future implementations may abandon segment locking in favor of more advanced
   *  concurrency controls.
   *
   *  @throws IllegalArgumentException if {@code concurrencyLevel} is nonpositive
   */
  def concurrencyLevel(concurrencyLevel: Int): CacheBuilder[K, V] = {
    checkNotNull(concurrencyLevel)
    checkState(withConcurrencyLevel == None, "concurrency level was already set to %s", withConcurrencyLevel)
    checkArgument(concurrencyLevel > 0)
    copy(withConcurrencyLevel = Some(concurrencyLevel))
  }

  /** Specifies the maximum number of entries the cache may contain. Note that the cache <b>may evict
   *  an entry before this limit is exceeded</b>. As the cache size grows close to the maximum, the
   *  cache evicts entries that are less likely to be used again. For example, the cache may evict an
   *  entry because it hasn't been used recently or very often.
   *
   *  <p>When `size` is zero, elements will be evicted immediately after being loaded into the
   *  cache. This can be useful in testing, or to disable caching temporarily without a code change.
   *
   *  <p>This feature cannot be used in conjunction with `maximumWeight`.
   *
   *  @param size the maximum size of the cache
   *  @throws IllegalArgumentException if `size` is negative
   */
  def maximumSize(size: Long): CacheBuilder[K, V] = {
    checkNotNull(size)
    checkState(withMaximumSize == None, "maximum size was already set to %s", withMaximumSize)
    checkState(withMaximumWeight == None, "maximum weight was already set to %s", withMaximumWeight)
    checkState(withWeigher == None, "maximum size can not be combined with weigher")
    checkArgument(size >= 0, "maximum size must not be negative")
    copy(withMaximumSize = Some(size))
  }

  /** Specifies the maximum weight of entries the cache may contain. Weight is determined using the
   *  `weigher` specified with `weigher((K1, V1) => Int)`, and use of this method requires a
   *  corresponding call to `weigher` prior to calling `build`.
   *
   *  <p>Note that the cache <b>may evict an entry before this limit is exceeded</b>. As the cache
   *  size grows close to the maximum, the cache evicts entries that are less likely to be used
   *  again. For example, the cache may evict an entry because it hasn't been used recently or very
   *  often.
   *
   *  <p>When {@code weight} is zero, elements will be evicted immediately after being loaded into
   *  cache. This can be useful in testing, or to disable caching temporarily without a code
   *  change.
   *
   *  <p>Note that weight is only used to determine whether the cache is over capacity; it has no
   *  effect on selecting which entry should be evicted next.
   *
   *  <p>This feature cannot be used in conjunction with `maximumSize`.
   *
   *  @param weight the maximum total weight of entries the cache may contain
   *  @throws IllegalArgumentException if {@code weight} is negative
   */
  def maximumWeight(weight: Long): CacheBuilder[K, V] = {
    checkNotNull(weight)
    checkState(withMaximumWeight == None, "maximum weight was already set to %s", withMaximumWeight)
    checkState(withMaximumSize == None, "maximum size was already set to %s", withMaximumSize)
    checkArgument(weight >= 0, "maximum weight must not be negative")
    copy(withMaximumWeight = Some(weight))
  }

  /** Specifies that each key (not value) stored in the cache should be wrapped in a
   *  `WeakReference` (by default, strong references are used).
   *
   *  <p><b>Warning:</b> when this method is used, the resulting cache will use identity (`eq`)
   *  comparison to determine equality of keys.
   *
   *  <p>Entries with keys that have been garbage collected may be counted in `Cache.size()`,
   *  but will never be visible to read or write operations; such entries are cleaned up as part of
   *  the routine maintenance described in the class doc.
   *
   *  @throws IllegalStateException if the key strength was already set
   */
  def weakKeys(): CacheBuilder[K, V] = {
    checkState(withWeakKeys == None, "Key strength was already set")
    copy(withWeakKeys = Some(true))
  }

  /** Specifies that each value (not key) stored in the cache should be wrapped in a
   *  `WeakReference` (by default, strong references are used).
   *
   *  <p>Weak values will be garbage collected once they are weakly reachable. This makes them a poor
   *  candidate for caching; consider {@link #softValues} instead.
   *
   *  <p><b>Note:</b> when this method is used, the resulting cache will use identity (`eq`)
   *  comparison to determine equality of values.
   *
   *  <p>Entries with values that have been garbage collected may be counted in {@link Cache#size},
   *  but will never be visible to read or write operations; such entries are cleaned up as part of
   *  the routine maintenance described in the class doc.
   */
  def weakValues(): CacheBuilder[K, V] = {
    checkState(withWeakValues == None, "Value strength was already set")
    checkState(withSoftValues == None, "Value strength was already set")
    copy(withWeakValues = Some(true))
  }

  /** Specifies that each value (not key) stored in the cache should be wrapped in a
   *  `SoftReference` (by default, strong references are used). Softly-referenced objects will
   *  be garbage-collected in a <i>globally</i> least-recently-used manner, in response to memory
   *  demand.
   *
   *  <p><b>Warning:</b> in most circumstances it is better to set a per-cache {@linkplain
   *  #maximumSize(long) maximum size} instead of using soft references. You should only use this
   *  method if you are well familiar with the practical consequences of soft references.
   *
   *  <p><b>Note:</b> when this method is used, the resulting cache will use identity (`eq`)
   *  comparison to determine equality of values.
   *
   *  <p>Entries with values that have been garbage collected may be counted in `Cache.size()`,
   *  but will never be visible to read or write operations; such entries are cleaned up as part of
   *  the routine maintenance described in the class doc.
   */
  def softValues(): CacheBuilder[K, V] = {
    checkState(withWeakValues == None, "Value strength was already set")
    checkState(withSoftValues == None, "Value strength was already set")
    copy(withSoftValues = Some(true))
  }

  /** Specifies that each entry should be automatically removed from the cache once a fixed duration
   *  has elapsed after the entry's creation, or the most recent replacement of its value.
   *
   *  <p>When `duration` is zero, this method hands off to
   *  `maximumSize(Long)` `(0)`, ignoring any otherwise-specificed maximum
   *  size or weight. This can be useful in testing, or to disable caching temporarily without a code
   *  change.
   *
   *  <p>Expired entries may be counted in {@link Cache#size}, but will never be visible to read or
   *  write operations. Expired entries are cleaned up as part of the routine maintenance described
   *  in the class doc.
   *
   *  @param duration the length of time after an entry is created that it should be automatically
   *     removed
   *  @param unit the unit that {@code duration} is expressed in
   *  @throws IllegalArgumentException if {@code duration} is negative
   */
  def expireAfterWrite(duration: Long, unit: TimeUnit): CacheBuilder[K, V] = {
    checkNotNull(duration)
    checkNotNull(unit)
    checkState(withExpireAfterWrite == None, "expireAfterWrite was already set to %s", withExpireAfterWrite)
    checkArgument(duration >= 0, "duration cannot be negative: %s %s", duration, unit)
    copy(withExpireAfterWrite = Some((duration, unit)))
  }

  /** Specifies that each entry should be automatically removed from the cache once a fixed duration
   *  has elapsed after the entry's creation, the most recent replacement of its value, or its last
   *  access. Access time is reset by all cache read and write operations.
   *
   *  <p>When `duration` is zero, this method hands off to
   *  `maximumSize(Long)` `(0)`, ignoring any otherwise-specificed maximum
   *  size or weight. This can be useful in testing, or to disable caching temporarily without a code
   *  change.
   *
   *  <p>Expired entries may be counted in `Cache.size()`, but will never be visible to read or
   *  write operations. Expired entries are cleaned up as part of the routine maintenance described
   *  in the class doc.
   *
   *  @param duration the length of time after an entry is last accessed that it should be
   *     automatically removed
   *  @param unit the unit that {@code duration} is expressed in
   *  @throws IllegalArgumentException if {@code duration} is negative
   */
  def expireAfterAccess(duration: Long, unit: TimeUnit): CacheBuilder[K, V] = {
    checkNotNull(duration)
    checkNotNull(unit)
    checkState(withExpireAfterAccess == None, "expireAfterAccess was already set to %s ns", withExpireAfterAccess)
    checkArgument(duration >= 0, "duration cannot be negative: %s %s", duration, unit)
    copy(withExpireAfterAccess = Some((duration, unit)))
  }

  /** Specifies that active entries are eligible for automatic refresh once a fixed duration has
   *  elapsed after the entry's creation, or the most recent replacement of its value. The semantics
   *  of refreshes are specified in `LoadingCache.refresh()`, and are performed by calling
   *  `CacheLoader.reload()`.
   *
   *  <p>As the default implementation of `CacheLoader.reload()` is synchronous, it is
   *  recommended that users of this method override `CacheLoader.reload()` with an asynchronous
   *  implementation; otherwise refreshes will be performed during unrelated cache read and write
   *  operations.
   *
   *  <p>Currently automatic refreshes are performed when the first stale request for an entry
   *  occurs. The request triggering refresh will make a blocking call to `CacheLoader.reload()`
   *  and immediately return the new value if the returned future is complete, and the old value
   *  otherwise.
   *
   *  <p><b>Note:</b> <i>all exceptions thrown during refresh will be logged and then swallowed</i>.
   *
   *  @param duration the length of time after an entry is created that it should be considered
   *     stale, and thus eligible for refresh
   *  @param unit the unit that `duration` is expressed in
   *  @throws IllegalArgumentException if `duration` is negative
   */
  def refreshAfterWrite(duration: Long, unit: TimeUnit): CacheBuilder[K, V] = {
    checkNotNull(duration)
    checkNotNull(unit)
    checkState(withRefreshAfterWrite == None, "refresh was already set to %s", withRefreshAfterWrite)
    checkArgument(duration > 0, "duration must be positive: %s %s", duration, unit)
    copy(withRefreshAfterWrite = Some((duration, unit)))
  }

  /** Specifies a nanosecond-precision time source for use in determining when entries should be
   *  expired. By default, `System#nanoTime` is used.
   *
   *  <p>The primary intent of this method is to facilitate testing of caches which have been
   *  configured with `expireAfterWrite` or `expireAfterAccess`.
   *
   *  @throws IllegalStateException if a ticker was already set
   */
  def ticker(ticker: Ticker): CacheBuilder[K, V] = {
    checkNotNull(ticker)
    checkState(withTicker == None)
    copy(withTicker = Some(ticker))
  }

  /** Enable the accumulation of [[CacheStats]] during the operation of the cache. Without this
   *  `Cache.stats()` will return zero for all statistics. Note that recording stats requires
   *  bookkeeping to be performed with each operation, and thus imposes a performance penalty on
   *  cache operation.
   */
  def recordStats(): CacheBuilder[K, V] = copy(withRecordStats = Some(true))
}

/** Factory for [[CacheBuilder]] instances. */
final object CacheBuilder {

  /** Creates a new [[LoadingCache]] which caches the values from the function `f`
   *  with default settings, including strong keys, strong values,
   *  and no automatic eviction of any kind.
   */
  def cache[K, V](f: K => V) = newBuilder().build(f)

  /** Constructs a new [[CacheBuilder]] instance with default settings, including strong keys,
   *  strong values, and no automatic eviction of any kind.
   */
  def apply() = newBuilder()

  /** Constructs a new [[CacheBuilder]] instance with default settings, including strong keys,
   *  strong values, and no automatic eviction of any kind.
   */
  def newBuilder() = new CacheBuilder[Any, Any](None, None, None, None, None, None, None, None, None, None, None, None, None, None)

  /** Creates a Guava CacheBuilder and sets the all properties from this
   *  CacheBuilder that are not `None`
   *
   *  @return a Guava CacheBuilder with the properties mapped from this CachBuilder
   */
  private[mango] def createGuavaBuilder[K, V](builder: CacheBuilder[K, V]): GuavaCacheBuilder[K, V] = {
    var gb = GuavaCacheBuilder.newBuilder().asInstanceOf[GuavaCacheBuilder[K, V]]
    builder.withWeigher foreach { weigher => gb = gb.weigher(weigher.asJava) }
    builder.withRemovalListener foreach { listener => gb = gb.removalListener(listener.asJava) }
    builder.withInitialCapacity foreach { cap => gb = gb.initialCapacity(cap) }
    builder.withConcurrencyLevel foreach { level => gb = gb.concurrencyLevel(level) }
    builder.withMaximumSize foreach { size => gb = gb.maximumSize(size) }
    builder.withMaximumWeight foreach { weight => gb = gb.maximumWeight(weight) }
    builder.withWeakKeys foreach { _ => gb = gb.weakKeys() }
    builder.withWeakValues foreach { _ => gb = gb.weakValues() }
    builder.withSoftValues foreach { _ => gb = gb.softValues() }
    builder.withExpireAfterWrite foreach { case (duration, unit) => gb = gb.expireAfterWrite(duration, unit) }
    builder.withExpireAfterAccess foreach { case (duration, unit) => gb = gb.expireAfterAccess(duration, unit) }
    builder.withRefreshAfterWrite foreach { case (duration, unit) => gb = gb.refreshAfterWrite(duration, unit) }
    builder.withTicker foreach { ticker => gb = gb.ticker(ticker.asJava) }
    builder.withRecordStats foreach { _ => gb = gb.recordStats() }
    return gb
  }
}