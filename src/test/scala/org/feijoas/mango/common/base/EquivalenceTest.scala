package org.feijoas.mango.common.base

import org.feijoas.mango.common.base.Equivalence.asGuavaEquiv
import org.feijoas.mango.common.base.Equivalence.asMangoEquiv
import org.scalatest.FreeSpec
import org.scalatest.Matchers.be
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.prop.PropertyChecks

import com.google.common.{base => gcm}

/** Tests for [[Equiv]]
 *
 *  @author Markus Schneider
 *  @since 0.10
 */
class EquivalenceTest extends FreeSpec with PropertyChecks {

  "AsMangoEquiv" - {
    "should forward equiv to Guava" in {
      val mango: Equiv[Int] = SignumEquiv
      val guava: gcm.Equivalence[Int] = mango.asJava
      forAll{ (x: Int, y: Int) =>
        guava.equivalent(x, y) should be (mango.equiv(x, y))
        guava.equivalent(x, x) should be (mango.equiv(x, x))
      }
    }
    "it should not wrap an Equiv twice" in {
      val mango: Equiv[Int] = SignumEquiv
      val guava: gcm.Equivalence[Int] = mango.asJava
      val wrappedAgain: Equiv[Int] = guava.asScala

      mango should be (wrappedAgain)
    }
  }

  "AsGuavaEquiv" - {
    "should forward equiv to Mango" in {
      val guava: gcm.Equivalence[Int] = SignumEquivalence
      val mango: Equiv[Int] = guava.asScala
      forAll{ (x: Int, y: Int) =>
        mango.equiv(x, y) should be (guava.equivalent(x, y))
        mango.equiv(x, x) should be (guava.equivalent(x, x))
      }
    }
    "it should not wrap an Equiv twice" in {
      val guava: gcm.Equivalence[Int] = SignumEquivalence
      val mango: Equiv[Int] = guava.asScala
      val wrappedAgain: gcm.Equivalence[Int] = mango.asJava

      guava should be (wrappedAgain)
    }
  }
}

private[mango] object SignumEquiv extends Equiv[Int] {
  def equiv(x: Int, y: Int) = x.signum == y.signum
}

private[mango] object SignumEquivalence extends gcm.Equivalence[Int] {
  def doEquivalent(x: Int, y: Int) = x.signum == y.signum
  def doHash(x: Int) = 2 * x
}