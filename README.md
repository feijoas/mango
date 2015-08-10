# The Mango library

The purpose of the Mango library is to provide
[Guava](https://code.google.com/p/guava-libraries/) (Google's core libraries) functionalities to Scala. This is primarily achieved through wrappers around Guava classes and converter between Java and Scala.
Also the package structure is intended to mirror the one from Guava.

However there are differences from the Guava libraries:
 - Whenever `null` is in Guava to indicate the absence of a value we use `Option`
 - Mango is more restrictive when passing `null` as arguments to library functions. The recommendation is to never use `null` at all.
 - In rare cases the method names are changed to conform to the Scala standard library
 - The Mango library uses Type Classes when appropriate

This is a beta version of the library. Many modules are not published yet because they are either not implemented, the test coverage is too low or the documentation is not complete. We publish these modules as soon as they are ready.

**We use Travis CI for continuous integration:**
 - See [https://travis-ci.org/feijoas/mango](https://travis-ci.org/feijoas/mango)
 - [![Build Status](https://travis-ci.org/feijoas/mango.png?branch=master)](https://travis-ci.org/feijoas/mango)

## Downloading 

Mango is programmed against `guava-18.0` (and is compatible to all guava versions >= 15.0) using Scala 2.11. If you want to run the tests you will also need the `guava-testlib-18.0`.

To use the Mango library in [sbt](http://www.scala-sbt.org/) add the following dependency to your project file:
```Scala
resolvers ++= Seq(
    "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases"
)

libraryDependencies += "org.feijoas" %% "mango" % "0.12"
```

## Examples 

### Suppliers
```Scala
import org.feijoas.mango.common.base.Suppliers._

// a supplier is just a function () => T
val supplier = () => 3                      //> supplier  : () => Int 
                                            //= function0
// convert to a Guava supplier
val gSupplier = supplier.asJava 			//> gSupplier  : com.google.common.base.Supplier[Int] 
                                            //= AsGuavaSupplier(function0)

// create s supplier that memoize its return
// value for 10 seconds
val memSupplier = memoizeWithExpiration(supplier, 10, TimeUnit.SECONDS)
                                              //> memSupplier  : () => Int  
                                              //= Suppliers.memoizeWithExpiration(function0, 10, SECONDS)
```
### Caches 
```Scala
import java.util.concurrent.TimeUnit
import org.feijoas.mango.common.cache._

// the function to cache
val expensiveFnc = (str: String) => str.length  //> expensiveFnc  : String => Int 

// create a cache with a maximum size of 100 and 
// exiration time of 10 minutes
val cache = CacheBuilder.newBuilder()
.maximumSize(100)
.expireAfterWrite(10, TimeUnit.MINUTES)
.build(expensiveFnc)              //> cache  : LoadingCache[String,Int]

cache("MyString")                 //> res0: Int = 8
```

### BloomFilter & Funnel 
```Scala
import org.feijoas.mango.common.hash.Funnel._
import org.feijoas.mango.common.hash.BloomFilter._

// A Funnel describes how to decompose a particular object type into primitive field values.
// For example, if we had
case class Person(id: Integer, firstName: String, lastName: String, birthYear: Int)

// our Funnel might look like
implicit val personFunnel = new Funnel[Person] {
 override def funnel(person: Person, into: PrimitiveSink) = {
    into
    .putInt(person.id)
    .putString(person.firstName, Charsets.UTF_8)
    .putString(person.lastName, Charsets.UTF_8)
    .putInt(person.birthYear)
 }
}

val friends: BloomFilter[Person] = BloomFilter.create(500, 0.01)
friendsList.foreach { case p: Person => friends.put(p) }

// much later
if (friends.mightContain(dude)) {
	// the probability that dude reached this place if he isn't a friend is 1%
	// we might, for example, start asynchronously loading things for dude while we do a more expensive exact check
}
```

### Range, RangeSet & RangeMap
```Scala
  import org.feijoas.mango.common.collect.Bound._
  import org.feijoas.mango.common.collect._
  import math.Ordering.Int

  val range = Range.atLeast(6)                                   // Range[Int,math.Ordering.Int.type] = [6..inf)

  // Pattern match using extractor
  range match {
    case Range(FiniteBound(lower, lowerType), InfiniteBound) => ...
  }

  // immutable range set:
  val rangeSet = RangeSet(Range.open(1, 3), Range.closed(4, 9)) // {(1,3), [4,9]}
  val subSet = rangeSet.subRangeSet(Range.closed(2, 6))         // union view {[2,3), [4,6]}
  
  // mutable range set:                                                
  val mutableRangeSet = mutable.RangeSet(Range.closed(1, 10))   // {[1, 10]}
  mutableRangeSet += Range.closedOpen(11, 15)                   // disconnected range: {[1, 10], [11, 15)}
  mutableRangeSet += Range.closedOpen(15, 20)                   // connected range; {[1, 10], [11, 20)}
  mutableRangeSet += Range.openClosed(0, 0)                     // empty range; {[1, 10], [11, 20)}
  mutableRangeSet -= Range.open(5, 10)                          // splits [1, 10]; {[1, 5], [10, 10], [11, 20)}

  // mutable range map
  val rangeMap = mutable.RangeMap(Range.open(3, 7) -> "1") //Map((3..7) -> 1)
  rangeMap += Range.closed(9, 10) -> "2"              // Map((3..7) -> 1, [9..10] -> 2)
  rangeMap += Range.closed(12, 16) -> "3"             // Map((3..7) -> 1, [9..10] -> 2, [12..16] -> 3)
 
  val sub = rangeMap.subRangeMap(Range.closed(5, 11)) // Map([5..7) -> 1, [9..10] -> 2)
  sub.put(Range.closed(7, 9), "4")                    // sub = Map([5..7) -> 1, [7..9] -> 4, (9..10] -> 2)
 
  // rangeMap = Map((3..7) -> 1, [7..9] -> 4, (9..10] -> 2, [12..16] -> 3)  
```

See the individual packages for more examples and documentation.

## Converter 
Conversions to and from the Guava libraries are done with the `.asJava` and `.asScala` methods respectively. These methods are imported together with the utility functions of the class. For example:
```Scala
// import converter for com.google.common.base.Function
import org.feijoas.mango.common.base.Functions._

val fnc = (str: String)=> str.length   //> fnc  : String => Int = function1
fnc.asJava                             //> res0: com.google.common.base.Function[String,Int] 
                                       //= AsGuavaFunction(function1)
```

## Build 

Just clone the git repository and build Mango in the following way:
```Scala
sbt update
sbt compile
```

Don't forget to test
```Scala
sbt test
```

## Help 

Besides the [Scaladoc](http://feijoas.github.io/mango/scaladoc) there is an excellent user guide [Guava Explained](https://code.google.com/p/guava-libraries/wiki/GuavaExplained) which should be sufficient for almost all questions.

 - Mango-Guava conversions: [Functions](http://feijoas.github.io/mango/scaladoc/index.html#org.feijoas.mango.common.base.Functions$), [Optional](http://feijoas.github.io/mango/scaladoc/index.html#org.feijoas.mango.common.base.Optional$), [Predicates] (http://feijoas.github.io/mango/scaladoc/index.html#org.feijoas.mango.common.base.Predicates$), [Suppliers](http://feijoas.github.io/mango/scaladoc/index.html#org.feijoas.mango.common.base.Suppliers$), [Futures](http://feijoas.github.io/mango/scaladoc/index.html#org.feijoas.mango.common.util.concurrent.Futures$)
 - [Preconditions](http://feijoas.github.io/mango/scaladoc/index.html#org.feijoas.mango.common.base.Preconditions$): Test preconditions for your methods more easily.
 - [Caches](http://feijoas.github.io/mango/scaladoc/index.html#org.feijoas.mango.common.cache.CacheBuilder$): Local caching, done right, and supporting a wide variety of expiration behaviors.
 - [Ranges](http://feijoas.github.io/mango/scaladoc/index.html#org.feijoas.mango.common.collect.Range$): RangeSets/RangeMaps query, merge and manipulate ranges
 - [Primitives](http://feijoas.github.io/mango/scaladoc/index.html#org.feijoas.mango.common.primitives$): Unsigned Int, Long and Byte

## License 

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)