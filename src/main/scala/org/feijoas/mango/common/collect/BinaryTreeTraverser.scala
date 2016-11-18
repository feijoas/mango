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
package org.feijoas.mango.common.collect

import scala.collection.convert.decorateAsScala.iterableAsScalaIterableConverter

import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.base.Optional.asGuavaOptionalConverter
import org.feijoas.mango.common.base.Optional.asMangoOptionConverter
import org.feijoas.mango.common.base.Preconditions.checkNotNull
import org.feijoas.mango.common.convert.AsJava
import org.feijoas.mango.common.convert.AsScala

import com.google.common.{ collect => cgcc }

/**
 * A variant of {@link TreeTraverser} for binary trees, providing additional traversals specific to
 * binary trees.
 *
 *  @author Markus Schneider
 *  @since 0.11 (copied from guava-libraries)
 */
@Beta
trait BinaryTreeTraverser[T] extends TreeTraverser[T] {

  /**
   * Returns the left child of the specified node, or `Option#isEmpty` if the specified
   * node has no left child.
   */
  def leftChild: T => Option[T]

  /**
   * Returns the right child of the specified node, or `Option#isEmpty` if the specified
   * node has no right child.
   */
  def rightChild: T => Option[T]

  /**
   * Returns the children of this node, in left-to-right order.
   */
  final override def children: T => Iterable[T] = (root: T) => this.asJava.children(root).asScala

  /**
   * Returns an unmodifiable iterable over the nodes in a tree structure, using in-order
   * traversal.
   */
  final def inOrderTraversal(root: T): Iterable[T] = this.asJava.inOrderTraversal(root).asScala
}

/** Factory for [[BinaryTreeTraverser]] instances. */
object BinaryTreeTraverser {

  /**
   * Creates a new `BinaryTreeTraverser` using a function that returns the left child and one that returns the right child
   */
  final def apply[T](left: T => Option[T], right: T => Option[T]): BinaryTreeTraverser[T] = new BinaryTreeTraverser[T] {
    final override def leftChild = (root: T) => left(root)
    final override def rightChild = (root: T) => right(root)
  }
  
  /**
   * Creates a new `BinaryTreeTraverser` using a function that returns the left child and the right child as a Tuple
   */
  final def apply[T](childs: T => (Option[T],Option[T])): BinaryTreeTraverser[T] = new BinaryTreeTraverser[T] {
    final override def leftChild = (root: T) => childs(root)._1
    final override def rightChild = (root: T) => childs(root)._2
  }  

  /**
   * Adds an `asJava` method that wraps a Scala `BinaryTreeTraverser` in
   *  a Guava `BinaryTreeTraverser`.
   *
   *  The returned Guava `BinaryTreeTraverser` forwards all calls
   *  to the given Scala `BinaryTreeTraverser`.
   *
   *  @param fnc the Scala `BinaryTreeTraverser` to wrap in a Guava `BinaryTreeTraverser`
   *  @return An object with an `asJava` method that returns a Guava `BinaryTreeTraverser`
   *   view of the argument
   */
  implicit final def asGuavaBinaryTreeTraverserConverter[T](traverser: BinaryTreeTraverser[T]): AsJava[cgcc.BinaryTreeTraverser[T]] = {
    def convert(traverser: BinaryTreeTraverser[T]): cgcc.BinaryTreeTraverser[T] = traverser match {
      case t: AsMangoBinaryTreeTraverser[T] => t.delegate
      case _ => AsGuavaBinaryTreeTraverser(traverser)
    }
    new AsJava(convert(traverser))
  }

  /**
   * Adds an `asScala` method that wraps a Guava `BinaryTreeTraverser` in
   *  a Scala `BinaryTreeTraverser`.
   *
   *  The returned Scala `BinaryTreeTraverser` forwards all calls
   *  to the given Guava `BinaryTreeTraverser``.
   *
   *  @param pred the Guava `BinaryTreeTraverser` to wrap in a Scala `BinaryTreeTraverser`
   *  @return An object with an `asScala` method that returns a Scala `BinaryTreeTraverser`
   *   view of the argument
   */
  implicit final def asMangoBinaryTreeTraverserConverter[T](traverser: cgcc.BinaryTreeTraverser[T]): AsScala[BinaryTreeTraverser[T]] = {
    def convert(traverser: cgcc.BinaryTreeTraverser[T]) = traverser match {
      case AsGuavaBinaryTreeTraverser(delegate) => delegate
      case _ => AsMangoBinaryTreeTraverser(traverser)
    }
    new AsScala(convert(traverser))
  }

}

/**
 * Wraps a Scala `BinaryTreeTraverser` in a Guava `BinaryTreeTraverser`
 */
@SerialVersionUID(1L)
private[mango] case class AsGuavaBinaryTreeTraverser[T](delegate: BinaryTreeTraverser[T]) extends cgcc.BinaryTreeTraverser[T] with Serializable {
  checkNotNull(delegate)
  final override def leftChild(root: T) = delegate.leftChild(root).asJava
  final override def rightChild(root: T) = delegate.rightChild(root).asJava
}

/**
 * Wraps a Guava `BinaryTreeTraverser` in a Scala `BinaryTreeTraverser`
 */
@SerialVersionUID(1L)
private[mango] case class AsMangoBinaryTreeTraverser[T](delegate: cgcc.BinaryTreeTraverser[T]) extends BinaryTreeTraverser[T] with Serializable {
  checkNotNull(delegate)
  final override def leftChild = (root: T) => delegate.leftChild(root).asScala
  final override def rightChild = (root: T) => delegate.rightChild(root).asScala
}