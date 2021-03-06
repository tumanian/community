/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher

import internal.commands.expressions.{Literal, Identifier}
import internal.commands.{GreaterThan, True}
import internal.pipes._
import internal.pipes.matching._
import internal.symbols.IntegerType
import matching.SingleStep
import org.neo4j.graphdb._
import java.util.{Iterator => JIterator}
import java.lang.{Iterable => JIterable}
import org.junit.{Test, Before}
import org.neo4j.graphdb.Traverser.Order
import org.scalatest.Assertions

class LazyTest extends ExecutionEngineHelper with Assertions {

  var a: Node = null
  var b: Node = null
  var c: Node = null


  @Before def my_init() {
    a = createNode()
    b = createNode()
    c = createNode()
    relate(a, b)
    relate(a, c)
    relate(a, createNode())
    relate(a, createNode())
    relate(a, createNode())
    relate(a, createNode())
    relate(a, createNode())
    relate(a, createNode())
    relate(a, createNode())
    relate(a, createNode())
  }

  @Test def get_first_relationship_does_not_iterate_through_all() {
    //Given:
    val limiter = new Limiter(1)
    val monitoredNode = new MonitoredNode(a, limiter.monitor)
    val iter = monitoredNode.getRelationships(Direction.OUTGOING).iterator()

    //When:
    iter.next()

    //Then does not throw exception
  }

  @Test def traversal_matcher_is_lazy() {
    //Given:
    val limiter = new Limiter(2)
    val monitoredNode = new MonitoredNode(a, limiter.monitor)

    val step = SingleStep(0, Seq(), Direction.OUTGOING, None, True(), True())
    val matcher = new MonoDirectionalTraversalMatcher(step, (ctx) => Seq(monitoredNode))
    val ctx = ExecutionContext.empty.newWith("a" -> monitoredNode)

    //When:
    val iter = matcher.findMatchingPaths(QueryState(), ctx)

    //Then:
    assert(limiter.count === 0)

    //Also then, does not throw exception
    iter.next()
  }

  @Test def execution_of_query_is_lazy() {
    //Given:
    val limiter = new Limiter(2)
    val monitoredNode = new MonitoredNode(a, limiter.monitor)

    val engine = new ExecutionEngine(graph)

    //When:
    val iter: ExecutionResult = engine.execute("start n=node({foo}) match n-->x return x", Map("foo" -> monitoredNode))

    //Then:
    assert(limiter.count === 0)

    //Also then does not step over the limit
    iter.next()
  }


  @Test def traversalmatcherpipe_is_lazy() {
    //Given:
    val limiter = new Limiter(2)
    val traversalMatchPipe = createTraversalMatcherPipe(limiter)

    //When:
    val result = traversalMatchPipe.createResults(QueryState())

    //Then:
    assert(limiter.count === 0)

    //Also then:
    result.next()  // throws exception if we iterate over more than expected to fill buffers
  }

  @Test def filterpipe_is_lazy() {
    //Given:
    val limited = new LimitedIterator[Map[String, Any]](4, (x) => Map("val" -> x))
    val input = new FakePipe(limited, "val" -> IntegerType())
    val pipe = new FilterPipe(input, GreaterThan(Identifier("val"), Literal(3)))

    //When:
    val iter = pipe.createResults(QueryState())

    //Then:
    assert(limited.count === 0)

    //Also then:
    iter.next() // throws exception if we iterate over more than expected to
  }


  private def createTraversalMatcherPipe(limiter:Limiter) :TraversalMatchPipe = {
    val monitoredNode = new MonitoredNode(a, limiter.monitor)

    val end = EndPoint("b")
    val trail = SingleStepTrail(end, Direction.OUTGOING, "r", Seq(), "a", None, None, null)
    val parameterPipe = new ParameterPipe()

    val step = trail.toSteps(0).get
    val matcher = new MonoDirectionalTraversalMatcher(step, (ctx) => Seq(monitoredNode))
    new TraversalMatchPipe(parameterPipe, matcher, trail)
  }
}

class LimitedIterator[T](limit: Int, f: Int => T, message: String = "Limit reached!") extends Iterator[T] {
  var count = 0

  def hasNext = true

  def next() = {
    count += 1
    if (count > limit)
      throw new RuntimeException(message)
    f(count)
  }
}

class Limiter(limit: Int) {
  var count: Int = 0

  def monitor() {
    count += 1
    if (count > limit)
      throw new RuntimeException("Limit passed!")
  }
}

class MonitoredNode(inner: Node, monitor: () => Unit) extends Node {
  def getId: Long = inner.getId

  def getRelationships(types: RelationshipType*): JIterable[Relationship] = null

  def delete() {}

  def getRelationships: JIterable[Relationship] = null

  def hasRelationship: Boolean = false

  def getRelationships(direction: Direction, types: RelationshipType*): JIterable[Relationship] = null

  def hasRelationship(types: RelationshipType*): Boolean = false

  def hasRelationship(direction: Direction, types: RelationshipType*): Boolean = false

  def getRelationships(dir: Direction): JIterable[Relationship] = new AIteratable(inner.getRelationships(dir).iterator(), monitor)

  def hasRelationship(dir: Direction): Boolean = false

  def getRelationships(`type`: RelationshipType, dir: Direction): JIterable[Relationship] = null

  def hasRelationship(`type`: RelationshipType, dir: Direction): Boolean = false

  def getSingleRelationship(`type`: RelationshipType, dir: Direction): Relationship = null

  def createRelationshipTo(otherNode: Node, `type`: RelationshipType): Relationship = null

  def traverse(traversalOrder: Order, stopEvaluator: StopEvaluator, returnableEvaluator: ReturnableEvaluator, relationshipType: RelationshipType, direction: Direction): Traverser = null

  def traverse(traversalOrder: Order, stopEvaluator: StopEvaluator, returnableEvaluator: ReturnableEvaluator, firstRelationshipType: RelationshipType, firstDirection: Direction, secondRelationshipType: RelationshipType, secondDirection: Direction): Traverser = null

  def traverse(traversalOrder: Order, stopEvaluator: StopEvaluator, returnableEvaluator: ReturnableEvaluator, relationshipTypesAndDirections: AnyRef*): Traverser = null

  def getGraphDatabase: GraphDatabaseService = null

  def hasProperty(key: String): Boolean = false

  def getProperty(key: String): AnyRef = inner.getProperty(key)

  def getProperty(key: String, defaultValue: AnyRef): AnyRef = null

  def setProperty(key: String, value: AnyRef) {}

  def removeProperty(key: String): AnyRef = null

  def getPropertyKeys: JIterable[String] = null

  def getPropertyValues: JIterable[AnyRef] = null

  override def toString = "°" + inner.toString + "°"
}


class AIteratable(iter: JIterator[Relationship], monitor: () => Unit) extends JIterable[Relationship] {
  def iterator() = new AIterator(iter, monitor)
}


class AIterator(iter: JIterator[Relationship], monitor: () => Unit) extends JIterator[Relationship] {
  def hasNext = iter.hasNext

  def next() = {
    monitor()
    iter.next()
  }

  def remove() {
    iter.remove()
  }
}
