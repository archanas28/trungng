package edu.kaist.uilab.asc.prior;

import junit.framework.TestCase;

/**
 * Tests for the class {@link SimilarityGraph}.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class TestSimilarityGraph extends TestCase {
  private static final int NUM_VERTICES = 9;
  private static final String GRAPH_FILE = "test/testgraph.txt";
  
  SimilarityGraph graph;
  
  @Override
  public void setUp() {
    graph = new SimilarityGraph(NUM_VERTICES, GRAPH_FILE);
  }
  
  @Override
  public void tearDown() {
    graph = null;
  }
  
  public void testConstructor() {
    assertEquals(NUM_VERTICES, graph.numVertices);
    if (!graph.getNeighbors(1).contains(2)) {
      fail("1 and 2 should be neighbor");
    }
    if (!graph.getNeighbors(2).contains(1)) {
      fail("2 and 1 should be neighbor");
    }
    if (!graph.getNeighbors(3).contains(8)) {
      fail("3 and 8 should be neighbor");
    }
    if (!graph.getNeighbors(8).contains(3)) {
      fail("8 and 3 should be neighbor");
    }
  }
}
