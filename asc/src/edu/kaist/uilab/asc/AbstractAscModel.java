package edu.kaist.uilab.asc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.Vector;

import edu.kaist.uilab.asc.data.Document;
import edu.kaist.uilab.asc.prior.SimilarityGraph;
import edu.kaist.uilab.asc.util.IntegerMatrix;
import edu.kaist.uilab.opt.ObjectiveFunction;

/**
 * A base model for all implementations of the {@link AscGibbsSampler} class. *
 * <p>
 * All sub-classes share the same Gibbs sampling implementation (hence the
 * internal data such as hyper-parameters). Each class must provide its own
 * implementation for the optimization of priors (beta and y).
 * 
 * TODO(trung): re-factor the Gibbs sampling dependent data into its own class.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public abstract class AbstractAscModel implements Serializable, ObjectiveFunction {

  private static final long serialVersionUID = 1L;
  boolean isExisting = false;
  String outputDir = ".";
  String extraInfo; // extra info about the model

  int vocabSize; // vocabulary size
  int effectiveVocabSize;
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
  
  public AbstractAscModel(int numTopics, int numSenti,
      Vector<LocaleWord> wordList, List<Document> documents,
      int numEnglishDocuments, List<TreeSet<Integer>> sentiWordsList,
      double alpha, double[] gammas, String graphFile) {
    this.numTopics = numTopics;
    this.numSenti = numSenti;
    this.vocabSize = wordList.size();
    this.effectiveVocabSize = countEnglishWords(wordList);
    this.documents = documents;
    this.numDocuments = documents.size();
    this.numEnglishDocuments = numEnglishDocuments;
    this.wordList = wordList;
    this.sentiWordsList = sentiWordsList;
    this.alpha = alpha;
    this.gammas = gammas;
    this.graphFile = graphFile;
    this.graph = new SimilarityGraph(vocabSize);
    initHyperParameters();
    initVariables();
  }
  
  /**
   * Initializes hyper parameters and related quantities for Gibbs sampling.
   */
  void initHyperParameters() {
    sumAlpha = alpha * numTopics;
    sumGamma = 0;
    for (double gamma : gammas) {
      sumGamma += gamma;
    }
    beta = new double[numSenti][numTopics][vocabSize];
    sumBeta = new double[numSenti][numTopics];
  }
  
  /**
   * Creates an existing model from the specified file, continuing the sampling
   * from the <code>iter</code> iteration.
   * 
   * @param savedModel
   */
  public static AbstractAscModel loadModel(String savedModel, int iter) {
    AbstractAscModel model = null;
    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(new FileInputStream(savedModel));
      model = (AbstractAscModel) in.readObject();
      model.isExisting = true;
      in.close();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return model;
  }
  
  private int countEnglishWords(Vector<LocaleWord> wordList) {
    int cnt = 0;
    for (LocaleWord word : wordList) {
      if (word.mLocale.equals(Locale.ENGLISH)) {
        cnt++;
      }
    }
    System.out.println("# english words: " + cnt);
    return cnt;
  }
  
  abstract void initVariables();
  
  /**
   * Returns the optimization accuracy for this model.
   * 
   * @return
   */
  abstract double getOptimizationAccuracy();

  /**
   * Converts the variables used in optimization to the specific internal
   * representation of the extending class.
   */
  abstract void variablesToY();

  /**
   * Extends the variables when documents from other languages are added to the
   * mixture.
   */
  abstract void extendVars();

  /**
   * Updates betas and their sums.
   * <p>
   * This method must be called whenever the internal variables y (which beta
   * depends on change).
   */
  abstract void updateBeta();

  /**
   * Writes out some values of y.
   * 
   * @param file
   * @throws IOException
   */
  abstract void writeSampleY(String file) throws IOException;
}
