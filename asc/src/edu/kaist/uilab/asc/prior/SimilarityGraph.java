package edu.kaist.uilab.asc.prior;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * The similarity graph which represents the prior.
 * 
 * <p> Each word in the vocabulary is an vertex in the graph, and an edge connects
 * two vertices <code>i</code> and <code>i'</code> if they are related.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class SimilarityGraph implements Serializable {
  
  private static final long serialVersionUID = 8042429989832172496L;
  ArrayList<Integer> adj[]; // the adjacency matrix
  int numVertices;
  
  /**
   * Constructor
   * 
   * @param numVertices
   *        the number of vertices (this should equal the size of vocabulary in a dataset)
   * @param edgeFile
   *        the text file which consists the list of vertices for this graph.
   *        Each line should contain two vertices separated by a space.
   */
  public SimilarityGraph(int numVertices, String edgeFile) {
    this(numVertices);
    initGraph(edgeFile);
  }
  
  /**
   * Default constructor
   * 
   * <p> Creates a graph with <code>numVertices</code> vertices and no edge.
   * @param numVertices
   */
  @SuppressWarnings("unchecked")
  public SimilarityGraph(int numVertices) {
    this.numVertices = numVertices;
    adj = new ArrayList[numVertices];
    for (int i = 0; i < numVertices; i++) {
      adj[i] = new ArrayList<Integer>();
    }
  }
  
  /**
   * Inits the graph from the given file.
   * 
   * @param file
   */
  public void initGraph(String file) {
    try {
      StringTokenizer tokenizer;
      String line;
      int u, v;
      BufferedReader in = new BufferedReader(new FileReader(file));
      while ((line = in.readLine()) != null) {
        tokenizer = new StringTokenizer(line, " ");
        u = Integer.parseInt(tokenizer.nextToken());
        v = Integer.parseInt(tokenizer.nextToken());
        adj[u].add(v);
        adj[v].add(u); // undirected graph
      }
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Returns the number of vertices.
   * 
   * @return
   */
  public int getNumVertices() {
    return numVertices;
  }
  
  /**
   * Gets the list of neighborhood vertices for the vertex {@code u}.
   * 
   * @param u
   * @return
   */
  public ArrayList<Integer> getNeighbors(int u) {
    return adj[u];
  }
}
