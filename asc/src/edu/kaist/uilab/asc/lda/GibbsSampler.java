package edu.kaist.uilab.asc.lda;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.aliasi.util.ObjectToCounterMap;

import edu.kaist.uilab.asc.opt.LBFGS;
import edu.kaist.uilab.asc.opt.MathUtils;
import edu.kaist.uilab.asc.opt.ObjectiveFunction;
import edu.kaist.uilab.asc.prior.GraphInputProducer;
import edu.kaist.uilab.asc.prior.SimilarityGraph;
import edu.kaist.uilab.asc.util.InvalidArgumentException;

/**
 * A naive implementation of Gibbs sampler for LDA.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class GibbsSampler implements ObjectiveFunction {
  private int numTopics; // T = numTopics
  private int vocabularySize; // V = vocabularySize
  private int numDocuments; // D = number of documents
  // documents[m][n] = (index of the n_th word in document m) = i
  private int[][] documents;

  // hyper-parameters
  private double alpha = 0.1; // default value
  private int[][] z; // topic assignment for each word z[document][word]
  private int cwt[][]; // word-topic count = cwt[i][k] word i to topic k
  private int cdt[][]; // document-topic count = cdt[m][k] words in document m
  // to topic k

  private double[][] beta; // beta[k][i]
  private double[] sumBeta;
  double[][] y; // y[topic][word]
  double[] yWord; // y[word]
  double[] vars;
  SimilarityGraph graph;

  private int cwtsum[]; // cwtsum[k] = # words assigned to topic k
  private int cdtsum[]; // cdtsum[m] = # words in document m
  private double thetasum[][]; // D x K : sum of samples
  private double phisum[][]; // K X V : sum of samples
  ArrayList<String> symbol;

  public static void main(String args[]) throws IOException {
    int numTopics = 10;
    double alpha = 0.1;
    int numIters = 1000;
    int burnIn = 500;
    int optimizationInterval = 100;
    int sampleLags = 100;
    int numSamples = 5;

    String dir = "C:/datasets/asc/ldatest";
    String dictionaryFile = "C:/datasets/asc/dict/en-fr.txt";
    String graphFile = "graph.txt";
    System.out.println("reading word list...");
    ArrayList<String> symbol = readWordList(dir + "/WordList.txt");
    System.out.println("constructing graph...");
    GraphInputProducer graphProducer = new GraphInputProducer(dir
        + "/WordList.txt", dictionaryFile);
    graphProducer.write(dir + "/" + graphFile);
    GibbsSampler sampler = new GibbsSampler(numTopics, symbol,
        readDocuments(dir + "/BagOfWords.txt"), alpha, new SimilarityGraph(
            symbol.size(), dir + "/" + graphFile));
    sampler.doGibbsSampling(numIters, burnIn, optimizationInterval, sampleLags,
        numSamples);
    sampler.writeOutput(sampler.getTheta(numSamples),
        sampler.getPhi(numSamples));
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
   * @param file
   * @return
   * @throws IOException
   */
  private static int[][] readDocuments(String file) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(file));
    ArrayList<ArrayList<Integer>> docs = new ArrayList<ArrayList<Integer>>();
    String line;
    StringTokenizer tokenizer;
    int numWords, wordIdx, wordCnt;
    ArrayList<Integer> document;
    while ((line = in.readLine()) != null) {
      document = new ArrayList<Integer>();
      numWords = Integer.parseInt(line.split(" ")[0]);
      tokenizer = new StringTokenizer(in.readLine(), " ");
      for (int i = 0; i < numWords; i++) {
        wordIdx = Integer.parseInt(tokenizer.nextToken());
        wordCnt = Integer.parseInt(tokenizer.nextToken());
        for (int j = 0; j < wordCnt; j++) {
          document.add(wordIdx);
        }
      }
      docs.add(document);
    }
    in.close();
    // convert to int[][]
    int[][] ret = new int[docs.size()][];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = new int[docs.get(i).size()];
      for (int j = 0; j < ret[i].length; j++) {
        ret[i][j] = docs.get(i).get(j);
      }
    }
    return ret;
  }

  /**
   * Constructs a new GibbsSampler with given model parameters.
   * 
   * @param numTopics
   *          the number of topics
   * @param vocabularySize
   *          the vocabulary size
   * @param documents
   *          the terms/words matrix
   * @param alpha
   *          the topic prior
   */
  public GibbsSampler(int numTopics, ArrayList<String> symbol,
      int[][] documents, double alpha, SimilarityGraph graph) {
    this.numTopics = numTopics;
    this.symbol = symbol;
    this.vocabularySize = symbol.size();
    this.documents = documents;
    this.alpha = alpha;
    initializeBeta();
    this.graph = graph;
    this.numDocuments = documents.length;
  }

  void initializeBeta() {
    beta = new double[numTopics][vocabularySize];
    y = new double[numTopics][vocabularySize];
    sumBeta = new double[numTopics];
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        beta[k][i] = 1.0; // because all y = 0
      }
      sumBeta[k] = vocabularySize;
    }
    yWord = new double[vocabularySize];
    int numVariables = numTopics * vocabularySize + vocabularySize;
    vars = new double[numVariables];
  }

  /**
   * Runs the sampler.
   * 
   * @param burnIn
   * @param optimizationInterval
   * @param sampleLags
   * @param numSamples
   */
  public void doGibbsSampling(int numIters, int burnIn,
      int optimizationInterval, int sampleLags, int numSamples) {
    System.out.print("Initializing parameters...");
    initialize();
    System.out.println("done");

    int samplesCollected = 0;
    System.out.println("Burning in period...");
    for (int iter = 0; iter < numIters; iter++) {
      System.out.print(iter + " ");
      if ((iter + 1) % 30 == 0) {
        System.out.println();
      }
      if (iter == burnIn) {
        System.out.println("\nBurning in done");
      }

      // sampling each hidden variable z_i
      for (int m = 0; m < numDocuments; m++) {
        for (int n = 0; n < documents[m].length; n++) {
          z[m][n] = sampleFullConditional(m, n);
        }
      }

      if ((iter + 1) >= burnIn && (iter + 1) % optimizationInterval == 0) {
        optimizeBeta();
      }

      // after burn-in & some sample lags we can collect a sample
      // note that we are not saving z[m][n] for now
      if (iter > burnIn && iter % sampleLags == 0) {
        System.out.println(String.format("Collected a sample at iteration %d",
            iter));
        updateParams();
        samplesCollected++;
        if (samplesCollected > numSamples) {
          return; // enough samples has been collected
        }
      }
    }
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
  private int sampleFullConditional(int m, int n) {
    int i, topic = 0;
    i = documents[m][n];
    topic = z[m][n];
    // not counting i_th word
    cwt[i][topic]--;
    cdt[m][topic]--;
    cwtsum[topic]--;
    cdtsum[m]--;

    double[] p = new double[numTopics];
    double tAlpha = numTopics * alpha;
    for (int k = 0; k < numTopics; k++) {
      p[k] = (cwt[i][k] + beta[k][i]) / (cwtsum[k] + sumBeta[k])
          * (cdt[m][k] + alpha) / (cdtsum[m] + tAlpha);
    }
    topic = sample(p);

    // assign new topic to the i_th word
    z[m][n] = topic;
    cwt[i][topic]++;
    cdt[m][topic]++;
    cwtsum[topic]++;
    cdtsum[m]++;

    return topic;
  }

  /**
   * Returns the estimated theta values of this sampler.
   * <p>
   * If the number of samples was set to be greater than 0, this is the average
   * of the estimated value of each sample collected.
   * 
   * @return
   */
  public double[][] getTheta(int numSamples) {
    double[][] theta = new double[numDocuments][numTopics];
    for (int m = 0; m < numDocuments; m++) {
      for (int k = 0; k < numTopics; k++) {
        theta[m][k] = thetasum[m][k] / numSamples;
      }
    }

    return theta;
  }

  /**
   * Returns the estimated phi values of this sampler.
   * <p>
   * If the number of samples was set to be greater than 0, this is the average
   * of the estimated value of each sample collected.
   * 
   * @return
   */
  public double[][] getPhi(int numSamples) {
    double[][] phi = new double[numTopics][vocabularySize];
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        phi[k][i] = phisum[k][i] / numSamples;
      }
    }

    return phi;
  }

  /**
   * Writes output of the model to files.
   * 
   * @param theta
   * @param phi
   * @throws IOException
   */
  public void writeOutput(double[][] theta, double[][] phi) throws IOException {
    writeTopWords("TopWords.txt");
    writeDocumentTopic("DocumentTopic.csv", theta);
    writeTopicWord("TopicWords.csv", phi);
  }

  void writeDocumentTopic(String file, double[][] theta) throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (int m = 0; m < documents.length; m++) {
      for (int k = 0; k < numTopics; k++) {
        out.print(String.format("%.4f,", theta[m][k]));
      }
      out.println();
    }
    out.close();
  }

  void writeTopicWord(String file, double[][] phi) throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        out.print(String.format("%.4f,", phi[k][i]));
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
    int numTopWords = 30;
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (int topic = 0; topic < numTopics; ++topic) {
      ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
      for (int word = 0; word < vocabularySize; ++word) {
        counter.set(word, cwt[word][topic]);
      }
      List<Integer> top = counter.keysOrderedByCountList();
      writer.printf("\nTOPIC %d (total count=%d)\n", topic, cwtsum[topic]);
      int id;
      for (int rank = 0; rank < numTopWords && rank < top.size(); rank++) {
        id = top.get(rank);
        writer.printf("%15s(%d)\n", symbol.get(id), cwt[id][topic]);
      }
    }
    writer.close();
  }

  /**
   * Initializes the model (assign random values to hidden variables -- the
   * topics z).
   */
  private void initialize() {
    // initialize count variables
    cwt = new int[vocabularySize][numTopics];
    cwtsum = new int[numTopics];
    cdt = new int[numDocuments][numTopics];
    cdtsum = new int[numDocuments];

    // sample values of z_i randomly ([1..numTopics] as the initial state of the
    // Markov chain
    z = new int[numDocuments][];
    for (int m = 0; m < numDocuments; m++) {
      int N = documents[m].length;
      int k; // the sample topic
      z[m] = new int[N];
      cdtsum[m] = N; // cdtsum[m] = number of words in document m
      for (int n = 0; n < N; n++) {
        k = (int) (Math.random() * numTopics);
        z[m][n] = k;
        cwt[documents[m][n]][k]++; // word i assigned to topic k
        cdt[m][k]++; // word i in document m assigned to topic k
        cwtsum[k]++; // total number of words assigned to topic k
      }
    }

    thetasum = new double[numDocuments][numTopics];
    phisum = new double[numTopics][vocabularySize];
  }

  /**
   * Updates the parameters when a new sample is collected.
   */
  private void updateParams() {
    // thetasum[][] (D x K) -- sum of samples (to return the average sample)
    double tAlpha = numTopics * alpha;
    for (int m = 0; m < numDocuments; m++) {
      for (int k = 0; k < numTopics; k++) {
        thetasum[m][k] += (cdt[m][k] + alpha) / (cdtsum[m] + tAlpha);
      }
    }

    // phisum[][] (K X V) -- sum of samples (to return the average sample)
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        phisum[k][i] += (cwt[i][k] + beta[k][i]) / (cwtsum[k] + sumBeta[k]);
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
   * 
   * @param vars
   *          the solution
   */
  boolean optimizeWithLbfgs() {
    // optimize y
    int numCorrections = 4;
    int[] iflag = new int[2];
    // TODO(trung): accuracy can be ~0.1 (gnorm / xnorm < accuracy)
    double accuracy = 0.1;
    boolean supplyDiag = false;
    double machinePrecision = 1e-32;
    // starting point
    double[] diag = new double[vars.length];
    // iprint[0] = output every iprint[0] iterations
    // iprint[1] = 0~3 : least to most detailed output
    int[] iprint = new int[] { 50, 0 };
    do {
      try {
        LBFGS.lbfgs(vars.length, numCorrections, vars,
            computeFunction(vars), computeGradient(vars), supplyDiag,
            diag, iprint, accuracy, machinePrecision, iflag);
        variablesToY(vars);
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
    for (int t = 0; t < numTopics; t++) {
      sumBeta[t] = 0;
      for (int w = 0; w < vocabularySize; w++) {
        beta[t][w] = Math.exp(y[t][w] + yWord[w]);
        sumBeta[t] += beta[t][w];
      }
    }
  }

  /**
   * Converts the variables (used in optimization function) to y.
   */
  private void variablesToY(double[] vars) {
    int idx = 0;
    for (int t = 0; t < numTopics; t++) {
      for (int w = 0; w < vocabularySize; w++) {
        y[t][w] = vars[idx++];
      }
    }
    for (int w = 0; w < vocabularySize; w++) {
      yWord[w] = vars[idx++];
    }
  }

  @Override
  public double computeFunction(double[] vars) throws InvalidArgumentException {
    // compute L_B = - log likelihood = -log p(w,z,s|alpha, beta)
    double negLogLikelihood = 0.0;
    for (int k = 0; k < numTopics; k++) {
      negLogLikelihood += MathUtils.logGamma(cwtsum[k] + sumBeta[k])
          - MathUtils.logGamma(sumBeta[k]);
      for (int i = 0; i < vocabularySize; i++) {
        if (cwt[i][k] > 0) {
          negLogLikelihood += MathUtils.logGamma(beta[k][i])
              - MathUtils.logGamma(beta[k][i] + cwt[i][k]);
        }
      }
    }
    // compute log p(beta)
    double logPrior = 0;
    double term = 0.0;
    for (int i = 0; i < vocabularySize; i++) {
      ArrayList<Integer> neighbors = graph.getNeighbors(i);
      // phi(i, iprime) = 1
      for (int iprime : neighbors) {
        for (int k = 0; k < numTopics; k++) {
          term = y[k][i] - y[k][iprime];
          logPrior += term * term;
        }
      }
    }
    logPrior /= 2; // each edge can be used only once
    for (int i = 0; i < vocabularySize; i++) {
      logPrior += yWord[i] * yWord[i];
    }
    logPrior *= -0.5; // 0.5lamda^2 where lamda = 1
    return negLogLikelihood - logPrior;
  }

  public double[] computeGradient(double[] vars)
      throws InvalidArgumentException {
    double[] grads = new double[vars.length];
    double tmp;
    double[][] betaKi = new double[numTopics][vocabularySize];
    // common beta terms for both y_ki and y_word i
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        tmp = MathUtils.digamma(cwtsum[k] + sumBeta[k])
            - MathUtils.digamma(sumBeta[k]);
        if (cwt[i][k] > 0) {
          tmp += MathUtils.digamma(beta[k][i])
              - MathUtils.digamma(beta[k][i] + cwt[i][k]);
        }
        betaKi[k][i] = beta[k][i] * tmp;
      }
    }

    // gradients of y_ki
    int idx = 0;
    ArrayList<Integer> neighbors;
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < vocabularySize; i++) {
        grads[idx] = betaKi[k][i];
        neighbors = graph.getNeighbors(i);
        for (int iprime : neighbors) {
          grads[idx] += y[k][i] - y[k][iprime];
        }
        idx++;
      }
    }
    // gradients of y_word i
    for (int i = 0; i < vocabularySize; i++) {
      grads[idx] = yWord[i];
      for (int k = 0; k < numTopics; k++) {
        grads[idx] += betaKi[k][i];
      }
      idx++;
    }
    return grads;
  }

}
