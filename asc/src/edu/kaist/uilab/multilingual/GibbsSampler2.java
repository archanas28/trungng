package edu.kaist.uilab.multilingual;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import edu.kaist.uilab.asc.prior.GraphInputProducer;
import edu.kaist.uilab.asc.prior.SimilarityGraph;
import edu.kaist.uilab.asc.util.InvalidArgumentException;
import edu.kaist.uilab.asc.util.Utils;
import edu.kaist.uilab.opt.LBFGS;
import edu.kaist.uilab.opt.MathUtils;
import edu.kaist.uilab.opt.ObjectiveFunction;

/**
 * Implementation of DC using Gibbs sampling.
 * 
 * <p> This implementation samples the mixture of documents from all languages
 * from the beginning instead of sampling for English documents in the first
 * half of iterations. The result of this implementation seems to be worse than
 * that of GibbsSampler3 (no guiding over topic distribution exists).
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class GibbsSampler2 implements ObjectiveFunction {
//  static final String workingDir = "C:/datasets/asc/ldatest/ElectronicsReviews1";
   static final String workingDir = "C:/datasets/asc/ldatest/bigeuroparl";
//  static final String workingDir = "C:/datasets/asc/ldatest/europarl";
  static final String graphFile = "graph.txt";
  static double mLamdaTopic = 1;
  static double mLamdaWord = 1.0;

  private int mNumTopics; // T = numTopics
  private int mNumDocuments; // D = number of documents
  // documents[m][n] = (index of the n_th word in document m) = i
  private int[][] mDocuments;
  private int mVocabularySize; // V = vocabularySize

  // hyper-parameters
  private double mAlpha; // default value
  private int[][] mZ; // topic assignment for each word z[document][word]
  private int mCwt[][]; // word-topic count = cwt[i][k] word i to topic k
  private int mCdt[][]; // document-topic count = cdt[m][k] words in document m
  // to topic k

  private double[][] mBeta; // beta[k][i]
  private double[] mSumBeta;
  double[][] mY; // y[topic][word]
  double[] mYword; // y[word]
  SimilarityGraph mGraph;
  double[] mVars;

  private int mCwtsum[]; // cwtsum[k] = # words assigned to topic k
  private int mCdtsum[]; // cdtsum[m] = # words in document m
  private double mTheta[][]; // D x K
  private double mPhi[][]; // K X V
  ArrayList<String> mSymbol;
  String mOutputDir;

  public static void main(String args[]) throws IOException {
    // getRandomReviews(dir + "/ElectronicsReviews1/docs_other.txt",
    // dir + "/ElectronicsReviews1/docs_fr.txt");
    int numTopics = 100;
    double alpha = 0.1;
    int numIters = 2000;
    int burnIn = 500;
    int optimizationInterval = 100;
    String outputDir = String.format("%s/(v2)T%d-I%d-A%.2f-L%.1f", workingDir,
        numTopics, numIters, alpha, mLamdaWord);
    new File(outputDir).mkdir();

    String dictionaryFile = "en-fr-locale.txt";
    System.out.println("reading word list...");
    ArrayList<String> symbol = readWordList(workingDir + "/WordList.txt");
//    System.out.println("constructing graph...");
//    GraphInputProducer graphProducer = new GraphInputProducer(workingDir
//        + "/WordList.txt", workingDir + "/" + dictionaryFile);
//    graphProducer.write(workingDir + "/" + graphFile, "\t");
    ArrayList<ArrayList<Integer>> documents = new ArrayList<ArrayList<Integer>>();
    readDocuments(workingDir + "/BagOfSentences_en.txt", documents);
    int numEnglishDocuments = documents.size();
    System.out.printf("\n# english reviews: %d", numEnglishDocuments);
    readDocuments(workingDir + "/BagOfSentences_other.txt", documents);
    System.out.printf("\n# french reviews: %d\n", documents.size()
        - numEnglishDocuments);
    // convert to int[][]
    int[][] docs = new int[documents.size()][];
    for (int i = 0; i < docs.length; i++) {
      docs[i] = new int[documents.get(i).size()];
      for (int j = 0; j < docs[i].length; j++) {
        docs[i][j] = documents.get(i).get(j);
      }
    }
    GibbsSampler2 sampler = new GibbsSampler2(numTopics, symbol, docs,
        numEnglishDocuments, alpha, new SimilarityGraph(
            symbol.size()), outputDir);
    sampler.doGibbsSampling(numIters, burnIn, optimizationInterval);
    sampler.writeOutput(numIters);
    System.out.println("\nOutput dir: " + outputDir);
  }

  static void getRandomReviews(String input, String output) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(input), "utf-8"));
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(output), "utf-8"));
    String source, rating, content;
    while ((source = in.readLine()) != null) {
      rating = in.readLine();
      content = in.readLine();
      int rand = (int) (Math.random() * 3);
      if (rand == 0) {
        out.println(source);
        out.println(rating);
        out.println(content);
      }
    }
    in.close();
    out.close();
  }

  /**
   * Reads all words from the specified file.
   * 
   * @param file
   * @return
   * @throws IOException
   */
  private static ArrayList<String> readWordList(String file) throws IOException {
    ArrayList<String> wordList = new ArrayList<String>();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), "utf-8"));
    String line;
    while ((line = in.readLine()) != null) {
      wordList.add(line);
    }
    in.close();
    return wordList;
  }

  /**
   * Reads the bag of words for documents.
   * 
   * @param dir
   * @param docs
   * @return
   * @throws IOException
   */
  private static void readDocuments(String file,
      ArrayList<ArrayList<Integer>> docs) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(file));
    String line;
    ArrayList<Integer> document;
    while ((line = in.readLine()) != null) {
      document = new ArrayList<Integer>();
      int numSentences = Integer.parseInt(line.split(" ")[1]);
      for (int i = 0; i < numSentences; i++) {
        String[] wordIdxs = in.readLine().split(" ");
        for (String wordIdx : wordIdxs) {
          document.add(Integer.parseInt(wordIdx));
        }
      }
      docs.add(document);
    }
    in.close();
  }

  /**
   * Constructs a new GibbsSampler with given model parameters.
   * 
   * @param numTopics
   *          the number of topics
   * @param mVocabularySize
   *          the vocabulary size
   * @param documents
   *          the terms/words matrix
   * @param alpha
   *          the topic prior
   * @param outputDir
   */
  public GibbsSampler2(int numTopics, ArrayList<String> symbol,
      int[][] documents, int numEnglishDocuments, double alpha,
      SimilarityGraph graph, String outputDir) {
    mNumTopics = numTopics;
    mSymbol = symbol;
    mDocuments = documents;
    mAlpha = alpha;
    mGraph = graph;
    mVocabularySize = symbol.size();
    mNumDocuments = documents.length;
    mOutputDir = outputDir;
    initializeBeta();
  }

  void initializeBeta() {
    mBeta = new double[mNumTopics][mVocabularySize];
    mY = new double[mNumTopics][mVocabularySize];
    mSumBeta = new double[mNumTopics];
    for (int k = 0; k < mNumTopics; k++) {
      for (int i = 0; i < mVocabularySize; i++) {
        mBeta[k][i] = 1.0; // because all y = 0
      }
      mSumBeta[k] = mVocabularySize;
    }
    mYword = new double[mVocabularySize];
    mVars = new double[mNumTopics * mVocabularySize + mVocabularySize];
    mGraph.initGraph(workingDir + "/" + graphFile, "\t");
  }

  /**
   * Initializes the model (assign random values to hidden variables -- the
   * topics z).
   */
  private void initialize() {
    // initialize count variables
    mCwt = new int[mVocabularySize][mNumTopics];
    mCwtsum = new int[mNumTopics];
    mCdt = new int[mNumDocuments][mNumTopics];
    mCdtsum = new int[mNumDocuments];
    mTheta = new double[mNumDocuments][mNumTopics];
    mPhi = new double[mNumTopics][mVocabularySize];
    mZ = new int[mNumDocuments][];
    initDocsForGibbs(0, mNumDocuments);
  }
  
  /**
   * Runs the sampler.
   * 
   * @param burnIn
   * @param optimizationInterval
   * @param sampleLags
   * @param numSamples
   */
  public void doGibbsSampling(int numIters, int burnIn, int optimizationInterval) {
    initialize();
    System.err.println("\nGibbs sampling started.");
    long startTime = System.currentTimeMillis();
    for (int iter = 0; iter < numIters; iter++) {
      int realIter = iter + 1;
      System.out.print(realIter + " ");
      if (realIter % 30 == 0) {
        System.out.println();
      }
      if (realIter >= burnIn && realIter % optimizationInterval == 0
          && realIter < numIters) {
        optimizeBeta();
      }
      // sampling each hidden variable z_i
      for (int m = 0; m < mNumDocuments; m++) {
        for (int n = 0; n < mDocuments[m].length; n++) {
          sampleFullConditional(m, n);
        }
      }
      if (realIter > burnIn && realIter % 500 == 0 && realIter != numIters) {
        updateParams();
        writeOutput(realIter);
      }
    }
    System.err.printf("\nGibbs sampling finished (%ds).",
        (System.currentTimeMillis() - startTime) / 1000);
    updateParams();
  }

  /**
   * Samples a topic for the n_th word in document m.
   * 
   * @param m
   *          the document
   * @param n
   *          the index (position) of the word in this document
   * @return the topic
   */
  private void sampleFullConditional(int m, int n) {
    int i, topic = 0;
    i = mDocuments[m][n];
    topic = mZ[m][n];
    // not counting i_th word
    mCwt[i][topic]--;
    mCdt[m][topic]--;
    mCwtsum[topic]--;
    mCdtsum[m]--;

    double[] p = new double[mNumTopics];
    double tAlpha = mNumTopics * mAlpha;
    for (int k = 0; k < mNumTopics; k++) {
      p[k] = (mCwt[i][k] + mBeta[k][i]) / (mCwtsum[k] + mSumBeta[k])
          * (mCdt[m][k] + mAlpha) / (mCdtsum[m] + tAlpha);
    }
    topic = sample(p);

    // assign new topic to the i_th word
    mZ[m][n] = topic;
    mCwt[i][topic]++;
    mCdt[m][topic]++;
    mCwtsum[topic]++;
    mCdtsum[m]++;
  }

  /**
   * Writes output of the model to files.
   * 
   * @param theta
   * @param phi
   * @throws IOException
   */
  public void writeOutput(int iter) {
    try {
      writeTopWords(mOutputDir + "/" + iter + "-TopWords.csv");
      writeBeta(mOutputDir + "/" + iter + "-beta.csv");
    } catch (IOException e) {
      e.printStackTrace();
    }
    // writeDocumentTopic(dir + "/" + iter + "-DocumentTopic.csv", theta);
    // writeTopicWord(dir + "/" + iter + "-TopicWords.csv", phi);
  }

  void writeDocumentTopic(String file, double[][] theta) throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (int m = 0; m < mDocuments.length; m++) {
      for (int k = 0; k < mNumTopics; k++) {
        out.print(String.format("%.4f,", theta[m][k]));
      }
      out.println();
    }
    out.close();
  }

  void writeTopicWord(String file, double[][] phi) throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (int k = 0; k < mNumTopics; k++) {
      for (int i = 0; i < mVocabularySize; i++) {
        out.print(String.format("%.4f,", phi[k][i]));
      }
      out.println();
    }
    out.close();
  }

  void writeBeta(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int t = 0; t < mNumTopics; t++)
      out.print(",T" + t);
    out.println();
    for (int w = 0; w < mVocabularySize; w++) {
      out.printf("%s(%.3f),", mSymbol.get(w), mYword[w]);
      for (int t = 0; t < mNumTopics; t++) {
        out.printf("%.3f(%.3f),", mBeta[t][w], mY[t][w]);
      }
      out.println();
    }
    out.close();
  }

  /**
   * Prints top words assigned to an entity topic to the file.
   * 
   * @param thetad
   * @param file
   * @throws IOException
   */
  void writeTopWords(String file) throws IOException {
    int numTopWords = 100;
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (int topic = 0; topic < mNumTopics; topic++) {
      out.printf("T%d,", topic);
    }
    out.println();
    // ranking according to phi[k][i]
    int[][] topWordMatrix = new int[mNumTopics][];
    for (int topic = 0; topic < mNumTopics; topic++) {
      topWordMatrix[topic] = Utils.topColumns(mPhi, topic, numTopWords);
    }
    // write the inverse of the top word matrix (for easy visualization)
    int wordId;
    for (int i = 0; i < numTopWords; i++) {
      for (int topic = 0; topic < mNumTopics; topic++) {
        wordId = topWordMatrix[topic][i];
        out.printf("%s(%.5f),", mSymbol.get(wordId), mPhi[topic][wordId]);
      }
      out.println();
    }
    out.close();
  }

  // sample values of z_i randomly ([1..numTopics] as the initial state of the
  // Markov chain
  private void initDocsForGibbs(int from, int to) {
    for (int m = from; m < to; m++) {
      int N = mDocuments[m].length;
      int k; // the sample topic
      mZ[m] = new int[N];
      mCdtsum[m] = N; // cdtsum[m] = number of words in document m
      for (int n = 0; n < N; n++) {
        k = (int) (Math.random() * mNumTopics);
        mZ[m][n] = k;
        mCwt[mDocuments[m][n]][k]++; // word i assigned to topic k
        mCdt[m][k]++; // word i in document m assigned to topic k
        mCwtsum[k]++; // total number of words assigned to topic k
      }
    }
  }

  /**
   * Updates the parameters when a new sample is collected.
   */
  private void updateParams() {
    // theta[][] (D x K)
    double tAlpha = mNumTopics * mAlpha;
    for (int m = 0; m < mNumDocuments; m++) {
      for (int k = 0; k < mNumTopics; k++) {
        mTheta[m][k] = (mCdt[m][k] + mAlpha) / (mCdtsum[m] + tAlpha);
      }
    }

    // phi[][] (K X V)
    for (int k = 0; k < mNumTopics; k++) {
      for (int i = 0; i < mVocabularySize; i++) {
        mPhi[k][i] = (mCwt[i][k] + mBeta[k][i]) / (mCwtsum[k] + mSumBeta[k]);
      }
    }
  }

  /**
   * Samples a value from a discrete distribution.
   * <p>
   * The method can modify the parameter {@code p} as it wants because {@code p}
   * is not needed afterward in the calling method.
   * 
   * @param p
   *          the unnormalized distribution
   */
  int sample(double p[]) {
    int T = p.length;
    int topic; // the sample

    // turning p into a cumulative distribution
    for (int i = 1; i < T; i++) {
      p[i] += p[i - 1];
    }

    // scaled sample because of unnormalized p
    double u = Math.random() * p[T - 1];
    // find the interval which contains u
    for (topic = 0; topic < T; topic++) {
      if (u < p[topic]) {
        break;
      }
    }

    return topic;
  }

  /**
   * Optimizes beta over y.
   * <p>
   * In this optimization, we replace b_kv = exp(y_kv + y_v), hence the solution
   * obtained are the values of y. But the function and gradient depend are
   * computed using beta so we have to update beta whenever we change y.
   */
  private void optimizeBeta() {
    System.out.println("\nOptimizing beta over y...\n");
    double startTime = System.currentTimeMillis();
    if (!optimizeWithLbfgs()) {
      System.err.println("Error with optimization");
      System.exit(-1);
    }
    System.out.printf("Optimization done (%.4fs)\n",
        (System.currentTimeMillis() - startTime) / 1000);
  }

  /**
   * Optimizes beta over y.
   */
  boolean optimizeWithLbfgs() {
    // optimize y
    int numCorrections = 4;
    int[] iflag = new int[2];
    double accuracy = 0.2;
    boolean supplyDiag = false;
    double machinePrecision = 1.1920929e-7;
    double[] diag = new double[mVars.length];
    // iprint[0] = output every iprint[0] iterations
    // iprint[1] = 0~3 : least to most detailed output
    int[] iprint = new int[] { 50, 0 };
    do {
      try {
        LBFGS.lbfgs(mVars.length, numCorrections, mVars, computeFunction(null),
            computeGradient(null), supplyDiag, diag, iprint, accuracy,
            machinePrecision, iflag);
        variablesToY();
        updateBeta();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } while (iflag[0] == 1);
    return (iflag[0] == 0);
  }

  /**
   * Updates betas and their sums.
   * <p>
   * This method should be called whenever y_kv and y_word v are changed.
   */
  private void updateBeta() {
    for (int k = 0; k < mNumTopics; k++) {
      mSumBeta[k] = 0;
      for (int i = 0; i < mVocabularySize; i++) {
        mBeta[k][i] = Math.exp(mY[k][i] + mYword[i]);
        mSumBeta[k] += mBeta[k][i];
      }
    }
  }

  /**
   * Converts the variables (used in optimization function) to y.
   */
  private void variablesToY() {
    int idx = 0;
    for (int k = 0; k < mNumTopics; k++) {
      for (int v = 0; v < mVocabularySize; v++) {
        mY[k][v] = mVars[idx++];
      }
    }
    for (int v = 0; v < mVocabularySize; v++) {
      mYword[v] = mVars[idx++];
    }
  }

  @Override
  public double computeFunction(double[] x) throws InvalidArgumentException {
    // compute L_B = - log likelihood = -log p(w,z,s|alpha, beta)
    double negLogLikelihood = 0.0;
    for (int k = 0; k < mNumTopics; k++) {
      negLogLikelihood += MathUtils.logGamma(mCwtsum[k] + mSumBeta[k])
          - MathUtils.logGamma(mSumBeta[k]);
      for (int i = 0; i < mVocabularySize; i++) {
        if (mCwt[i][k] > 0) {
          negLogLikelihood += MathUtils.logGamma(mBeta[k][i])
              - MathUtils.logGamma(mBeta[k][i] + mCwt[i][k]);
        }
      }
    }
    // compute log p(beta)
    double kvTerm = 0.0, term = 0.0;
    for (int i = 0; i < mVocabularySize; i++) {
      ArrayList<Integer> neighbors = mGraph.getNeighbors(i);
      // phi(i, iprime) = 1
      for (int iprime : neighbors) {
        for (int k = 0; k < mNumTopics; k++) {
          term = mY[k][i] - mY[k][iprime];
          kvTerm += term * term;
        }
      }
    }
    kvTerm /= 2; // each edge can be used only once
    kvTerm /= (2 * mLamdaTopic * mLamdaTopic); // divided by 2lamda_kv^{2}
    double vTerm = 0;
    for (int i = 0; i < mVocabularySize; i++) {
      vTerm += mYword[i] * mYword[i];
    }
    vTerm /= (2 * mLamdaWord * mLamdaWord); // divided by 2lamda_v^{2}
    return negLogLikelihood + kvTerm + vTerm;
  }

  public double[] computeGradient(double[] x) throws InvalidArgumentException {
    double[] grads = new double[mVars.length];
    double[][] betaKi = new double[mNumTopics][mVocabularySize];
    // common beta terms for both y_ki and y_word i
    for (int k = 0; k < mNumTopics; k++) {
      double kGamma = MathUtils.digamma(mCwtsum[k] + mSumBeta[k])
          - MathUtils.digamma(mSumBeta[k]);
      for (int i = 0; i < mVocabularySize; i++) {
        betaKi[k][i] = kGamma;
        if (mCwt[i][k] > 0) {
          betaKi[k][i] += MathUtils.digamma(mBeta[k][i])
              - MathUtils.digamma(mBeta[k][i] + mCwt[i][k]);
        }
        betaKi[k][i] *= mBeta[k][i];
      }
    }
    // gradients of y_ki
    int idx = 0;
    ArrayList<Integer> neighbors;
    for (int k = 0; k < mNumTopics; k++) {
      for (int i = 0; i < mVocabularySize; i++) {
        grads[idx] = 0;
        neighbors = mGraph.getNeighbors(i);
        for (int iprime : neighbors) {
          grads[idx] += mY[k][i] - mY[k][iprime];
        }
        grads[idx] /= (mLamdaTopic * mLamdaTopic); // divided by lamda^{2}
        grads[idx] += betaKi[k][i];
        idx++;
      }
    }
    // gradients of y_i
    for (int i = 0; i < mVocabularySize; i++) {
      grads[idx] = mYword[i] / (mLamdaWord * mLamdaWord);
      for (int k = 0; k < mNumTopics; k++) {
        grads[idx] += betaKi[k][i];
      }
      idx++;
    }
    return grads;
  }
}
