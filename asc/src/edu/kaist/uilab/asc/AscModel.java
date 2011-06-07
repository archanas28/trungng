package edu.kaist.uilab.asc;

import java.io.Serializable;
import java.util.List;
import java.util.TreeSet;

import edu.kaist.uilab.asc.data.Document;
import edu.kaist.uilab.asc.prior.SimilarityGraph;
import edu.kaist.uilab.asc.util.IntegerMatrix;

/**
 * A base model for all implementations of the {@link AbstractAsc} class.
 * 
 * <p> Thsi class implements serializable so that the model can be saved for later
 * examination or further iteration (in Gibbs sampling).
 * 
 * @author trung nguyen (trung.ngvan@gmail.com0
 */
public class AscModel implements Serializable {
  private static final long serialVersionUID = 1L;
  boolean isExisting = false;
  String outputDir = ".";

  int numUniqueWords; // vocabulary size
  int numTopics; // K
  int numSenti; // S
  int numDocuments;
  int numEnglishDocuments;
  List<Document> documents;
  List<LocaleWord> wordList;
  SimilarityGraph graph;
  String graphFile;

  int numProbWords = 100;
  double alpha;
  double sumAlpha;
  double[] gammas;
  double sumGamma;
  double[][][] beta; // beta[senti][topic][word]
  double[][] sumBeta; // sumBeta[senti][topic]
  double[] vars;

  List<TreeSet<Integer>> sentiWordsList;
  IntegerMatrix[] matrixSWT;
  IntegerMatrix[] matrixSDT;
  IntegerMatrix matrixDS;
  int[][] sumSTW; // sumSTW[S][T]
  int[][] sumDST; // sumDST[D][S]
  int[] sumDS; // sumDS[D]
}
