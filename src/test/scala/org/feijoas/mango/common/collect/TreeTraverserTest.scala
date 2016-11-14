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

import scala.math.Ordering.Int
import org.feijoas.mango.common.annotations.Beta
import org.feijoas.mango.common.collect.Bound.FiniteBound
import org.feijoas.mango.common.collect.Bound.InfiniteBound
import org.feijoas.mango.common.collect.DiscreteDomain.IntDomain
import org.scalatest.FlatSpec
import org.scalatest.Matchers.be
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.prop.PropertyChecks
import com.google.common.testing.SerializableTester.reserializeAndAssert
import org.scalatest._
import org.scalatest.FunSpec

/**
 * Tests for [[TreeTraverser]]
 *
 *  @author Markus Schneider
 *  @since 0.11 (copied from guava-libraries)
 */
class TreeTraverserTest extends FunSpec with Matchers {
  case class Tree(value: Char, children: Tree*)
  case class BinaryTree(value: Char, left: BinaryTree, right: BinaryTree)

  val traverser = TreeTraverser((node: Tree) => node.children)
  val binTraverser = BinaryTreeTraverser((node: BinaryTree) => (Option(node.left), Option(node.right)))

  //        h
  //      / | \
  //     /  e  \
  //    d       g
  //   /|\      |
  //  / | \     f
  // a  b  c
  val a_ = Tree('a')
  val b_ = Tree('b')
  val c_ = Tree('c')
  val d_ = Tree('d', a_, b_, c_)
  val e_ = Tree('e')
  val f_ = Tree('f')
  val g_ = Tree('g', f_)
  val h_ = Tree('h', d_, e_, g_)

  //      d
  //     / \
  //    b   e
  //   / \   \
  //  a   c   f
  //         /
  //        g
  val ba_ = BinaryTree('a', null, null)
  val bc_ = BinaryTree('c', null, null)
  val bb_ = BinaryTree('b', ba_, bc_)
  val bg_ = BinaryTree('g', null, null)
  val bf_ = BinaryTree('f', bg_, null)
  val be_ = BinaryTree('e', null, bf_)
  val bd_ = BinaryTree('d', bb_, be_)

  def treeAsString(tree: Iterable[Tree]): String = tree.foldLeft(""){ case (str, tree) => str + tree.value }
  def bTreeAsString(tree: Iterable[BinaryTree]): String = tree.foldLeft(""){ case (str, tree) => str + tree.value }

  describe("A TreeTraverser") {
    it("should be able traverse the tree in preOrder") {
      treeAsString(traverser.preOrderTraversal(h_)) should be("hdabcegf")
    }
    it("should be able traverse the tree in postOrder") {
      treeAsString(traverser.postOrderTraversal(h_)) should be("abcdefgh")
    }
    it("should be able traverse the tree in breadthFirstOrder") {
      treeAsString(traverser.breadthFirstTraversal(h_)) should be("hdegabcf")
    }
  }

  describe("A BinaryTreeTraverser") {
    it("should be able traverse the tree in preOrder") {
      bTreeAsString(binTraverser.preOrderTraversal(bd_)) should be("dbacefg")
    }
    it("should be able traverse the tree in postOrder") {
      bTreeAsString(binTraverser.postOrderTraversal(bd_)) should be("acbgfed")
    }
    it("should be able traverse the tree in breadthFirstOrder") {
      bTreeAsString(binTraverser.breadthFirstTraversal(bd_)) should be("dbeacfg")
    }
    it("should be able traverse the tree in order") {
      bTreeAsString(binTraverser.inOrderTraversal(bd_)) should be("abcdegf")
    }
  }
}
