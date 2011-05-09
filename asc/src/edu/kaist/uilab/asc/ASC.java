package edu.kaist.uilab.asc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import edu.kaist.uilab.asc.data.OrderedDocument;
import edu.kaist.uilab.asc.data.Sentence;
import edu.kaist.uilab.asc.data.SentiWord;
import edu.kaist.uilab.asc.data.Word;
import edu.kaist.uilab.asc.opt.LBFGS;
import edu.kaist.uilab.asc.opt.MathUtils;
import edu.kaist.uilab.asc.opt.ObjectiveFunction;
import edu.kaist.uilab.asc.prior.SimilarityGraph;
import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.IntegerMatrix;
import edu.kaist.uilab.asc.util.InvalidArgumentException;

/**
 * ASC implementation. TODO(trung): eng-french is not sufficient (lack of words
 * for the current corpus)
 */
public class ASC {
  private Model model;
  private double optimizationAccuracy = 1.7;
  private double[][] probTable;
  ObjectiveFunction func;
  private int startingIteration;

  static class Model implements Serializable {
    private static final long serialVersionUID = 1L;
    boolean isExisting = false;
    String outputDir = ".";

    int numUniqueWords; // vocabulary size
    int numTopics; // K
    int numSenti; // S
    int numDocuments;
    int numEnglishDocuments;
    List<OrderedDocument> documents;
    List<String> wordList;
    SimilarityGraph graph;

    int numProbWords = 100;
    double alpha;
    double sumAlpha;
    double[] gammas;
    double sumGamma;
    double[][][] beta; // beta[senti][topic][word]
    double[][][] y; // y[s][topic][word]
    double[] yWord; // y[word]
    double[] vars;
    double[][] sumBeta; // sumBeta[senti][topic]

    List<TreeSet<Integer>> sentiWordsList;
    IntegerMatrix[] matrixSWT;
    IntegerMatrix[] matrixSDT;
    IntegerMatrix matrixDS;
    int[][] sumSTW; // sumSTW[S][T]
    int[][] sumDST; // sumDST[D][S]
    int[] sumDS; // sumDS[D]
  }

  /**
   * Creates a new ASC model.
   * 
   * @param numTopics
   * @param numSenti
   * @param wordList
   * @param documents
   * @param sentiWordsList
   * @param alpha
   * @param gammas
   * @param graph
   */
  public ASC(int numTopics, int numSenti, List<String> wordList,
      List<OrderedDocument> documents, int numEnglishDocuments,
      List<TreeSet<Integer>> sentiWordsList, double alpha, double[] gammas,
      SimilarityGraph graph) {
    model = new Model();
    probTable = new double[numTopics][numSenti];
    model.numTopics = numTopics;
    model.numSenti = numSenti;
    model.numUniqueWords = wordList.size();
    model.documents = documents;
    model.numDocuments = documents.size();
    model.numEnglishDocuments = numEnglishDocuments;
    model.wordList = wordList;
    model.sentiWordsList = sentiWordsList;
    model.alpha = alpha;
    model.gammas = gammas;
    model.graph = graph;
    initHyperParameters();
    func = new AscObjectiveFunction(model.numSenti * model.numTopics
        * model.numUniqueWords + model.numUniqueWords);
  }

  /**
   * Creates an existing model from the specified file, continuing the sampling
   * from the <code>iter</code> iteration.
   * 
   * @param savedModel
   */
  public ASC(String savedModel, int iter) {
    startingIteration = iter + 1;
    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(new FileInputStream(savedModel));
      model = (Model) in.readObject();
      model.isExisting = true;
      in.close();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    probTable = new double[model.numTopics][model.numSenti];
    func = new AscObjectiveFunction(model.numSenti * model.numTopics
        * model.numUniqueWords + model.numUniqueWords);
  }

  /**
   * Sets the output dir for this model.
   * <p>
   * If an output dir is not set, the model will output to the current
   * directory.
   * 
   * @param dir
   */
  public void setOutputDir(String dir) {
    model.outputDir = dir;
    new File(dir).mkdir();
  }

  /**
   * Initializes hyper parameters and related quantities.
   */
  void initHyperParameters() {
    model.sumAlpha = model.alpha * model.numTopics;
    model.sumGamma = 0;
    for (double gamma : model.gammas) {
      model.sumGamma += gamma;
    }
    model.y = new double[model.numSenti][][];
    model.yWord = new double[model.numUniqueWords];
    for (int s = 0; s < model.numSenti; s++) {
      model.y[s] = new double[model.numTopics][model.numUniqueWords];
    }
    model.beta = new double[model.numSenti][][];
    model.sumBeta = new double[model.numSenti][model.numTopics];
    for (int s = 0; s < model.numSenti; s++) {
      model.beta[s] = new double[model.numTopics][model.numUniqueWords];
      for (int t = 0; t < model.numTopics; t++) {
        model.sumBeta[s][t] = 0;
        for (int w = 0; w < model.numUniqueWords; w++) {
          // asymmetric beta
          if ((s == 0 && model.sentiWordsList.get(1).contains(w))
              || (s == 1 && model.sentiWordsList.get(0).contains(w))) {
            model.beta[s][t][w] = 0.0000001;
          } else {
            model.beta[s][t][w] = 0.001;
          }
          // make beta[s][t][w] = exp(y(stw) + y(w) where y(w) = 0
          model.y[s][t][w] = Math.log(model.beta[s][t][w]);
          model.sumBeta[s][t] += model.beta[s][t][w];
        }
      }
    }
    model.vars = new double[model.numSenti * model.numTopics
        * model.numUniqueWords + model.numUniqueWords];
  }

  /**
   * Initializes for gibbs sampling.
   * 
   * @param randomInit
   */
  void gibbsInit(boolean randomInit) {
    model.sumSTW = new int[model.numSenti][model.numTopics];
    model.sumDST = new int[model.numDocuments][model.numSenti];
    model.sumDS = new int[model.numDocuments];
    model.matrixSWT = new IntegerMatrix[model.numSenti];
    model.matrixSDT = new IntegerMatrix[model.numSenti];
    for (int i = 0; i < model.numSenti; i++) {
      model.matrixSWT[i] = new IntegerMatrix(model.numUniqueWords,
          model.numTopics);
      model.matrixSDT[i] = new IntegerMatrix(model.numDocuments,
          model.numTopics);
    }
    model.matrixDS = new IntegerMatrix(model.numDocuments, model.numSenti);

    for (OrderedDocument currentDoc : model.documents) {
      int docNo = currentDoc.getDocNo();
      for (Sentence sentence : currentDoc.getSentences()) {
        int newSenti = -1;
        int numSentenceSenti = 0;
        for (Word sWord : sentence.getWords()) {
          SentiWord word = (SentiWord) sWord;
          int wordNo = word.getWordNo();
          for (int s = 0; s < model.sentiWordsList.size(); s++) {
            if (model.sentiWordsList.get(s).contains(wordNo)) {
              if (numSentenceSenti == 0 || s != newSenti)
                numSentenceSenti++;
              word.lexicon = s;
              newSenti = s;
            }
          }
        }
        sentence.numSenti = numSentenceSenti;
        // if sentiment of the sentence is not clear, get random sentiment
        if (randomInit || sentence.numSenti != 1) {
          newSenti = (int) (Math.random() * model.numSenti);
        }
        if (numSentenceSenti <= 1) {
          int newTopic = (int) (Math.random() * model.numTopics);
          sentence.setTopic(newTopic);
          sentence.setSenti(newSenti);
          for (Word sWord : sentence.getWords()) {
            ((SentiWord) sWord).setSentiment(newSenti);
            sWord.setTopic(newTopic);
            model.matrixSWT[newSenti].incValue(sWord.wordNo, newTopic);
            model.sumSTW[newSenti][newTopic]++;
          }
          model.matrixSDT[newSenti].incValue(docNo, newTopic);
          model.matrixDS.incValue(docNo, newSenti);
          model.sumDST[docNo][newSenti]++;
          model.sumDS[docNo]++;
        }
      }
    }
  }

  /**
   * Samples using Gibbs sampling.
   * 
   * @param numIters
   *          total iterations to run
   * @param savingInterval
   *          the interval to save the training model (0 if no saving)
   * @param burnIn
   *          burning in period
   * @param optimizationInterval
   *          interval to optimize beta over y
   * @param numThreads
   *          how many threads to run
   * @throws Exception
   */
  public void gibbsSampling(int numIters, int savingInterval, int burnIn,
      int optimizationInterval, int numThreads) throws IOException {
    int iter = 0;
    if (!model.isExisting) {
      gibbsInit(false);
    } else {
      iter = startingIteration;
    }
    System.out.printf("Gibbs sampling started (Iterations: %d)", numIters);
    double startTime = System.currentTimeMillis();
    for (; iter < numIters; iter++) {
      // sampling over english documents in the first half of iterations
      int maxDocNo = iter <= numIters / 2 ? model.numEnglishDocuments
          : model.numDocuments;
      for (int docNo = 0; docNo < maxDocNo; docNo++) {
        sampleForDoc(model.documents.get(docNo));
      }
      if (iter % 50 == 0) {
        System.out.println();
      }
      System.out.printf(" %d ", iter);
      if (savingInterval > 0 && (iter + 1) % savingInterval == 0) {
        saveModel(iter + 1);
        writeModelOutput(iter + 1);
      }
      if (iter + 1 >= burnIn && (iter + 1) % optimizationInterval == 0) {
        optimizeBeta();
      }
    }
    System.out.printf("Gibbs sampling terminated. (%.4fs)\n",
        (System.currentTimeMillis() - startTime) / 1000);
    // save the last model
    saveModel(numIters);
    writeModelOutput(numIters);
  }

  /**
   * Optimizes values of betas over y.
   */
  void optimizeBeta() {
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
   * @return true if an solution was found, false if an error occurs
   */
  boolean optimizeWithLbfgs() {
    // optimize y
    int numCorrections = 4;
    int[] iflag = new int[2];
    boolean supplyDiag = false;
    double machinePrecision = 1e-32;
    // starting point
    double[] diag = new double[func.getNumVariables()];
    // iprint[0] = output every iprint[0] iterations
    // iprint[1] = 0~3 : least to most detailed output
    int[] iprint = new int[] { 50, 0 };
    do {
      try {
        LBFGS.lbfgs(func.getNumVariables(), numCorrections, model.vars,
            func.computeFunction(null), func.computeGradient(null), supplyDiag,
            diag, iprint, optimizationAccuracy, machinePrecision, iflag);
        variablesToY();
        updateBeta();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } while (iflag[0] == 1);
    return (iflag[0] == 0);
  }

  /**
   * Converts the variables (used in optimization function) to y.
   */
  void variablesToY() {
    int idx = 0;
    for (int s = 0; s < model.numSenti; s++) {
      for (int t = 0; t < model.numTopics; t++) {
        for (int w = 0; w < model.numUniqueWords; w++) {
          model.y[s][t][w] = model.vars[idx++];
        }
      }
    }
    for (int w = 0; w < model.numUniqueWords; w++) {
      model.yWord[w] = model.vars[idx++];
    }
  }

  /**
   * Updates betas and their sums.
   * <p>
   * This method should be called whenever y_kv and y_word v are changed.
   */
  void updateBeta() {
    for (int s = 0; s < model.numSenti; s++) {
      for (int t = 0; t < model.numTopics; t++) {
        model.sumBeta[s][t] = 0;
        for (int w = 0; w < model.numUniqueWords; w++) {
          model.beta[s][t][w] = Math.exp(model.y[s][t][w] + model.yWord[w]);
          model.sumBeta[s][t] += model.beta[s][t][w];
        }
      }
    }
  }

  /**
   * Samples a document.
   * 
   * @param currentDoc
   */
  private void sampleForDoc(OrderedDocument currentDoc) {
    int docNo = currentDoc.getDocNo();
    for (Sentence sentence : currentDoc.getSentences()) {
      if (sentence.getSenti() == -1) {
        continue;
      }
      Map<Word, Integer> wordCnt = sentence.getWordCnt();
      double sumProb = 0;
      int oldTopic = sentence.getTopic();
      int oldSenti = sentence.getSenti();
      model.matrixSDT[oldSenti].decValue(docNo, oldTopic);
      model.matrixDS.decValue(docNo, oldSenti);
      model.sumDST[docNo][oldSenti]--;
      model.sumDS[docNo]--;
      for (Word sWord : sentence.getWords()) {
        model.matrixSWT[oldSenti].decValue(sWord.wordNo, oldTopic);
        model.sumSTW[oldSenti][oldTopic]--;
      }

      // Sampling
      for (int si = 0; si < model.numSenti; si++) {
        if (trim(wordCnt, si)) {
          // forced sentiment orientation (by assigning 0 probability to the
          // opposite senti-aspect. example: if a sentences contains the word
          // "excellent", then the probability of being assigned sentiment
          // negative is 0.
          for (int ti = 0; ti < model.numTopics; ti++) {
            probTable[ti][si] = 0;
          }
        } else {
          for (int ti = 0; ti < model.numTopics; ti++) {
            // this quantity is the beta dependent entities in equation for
            // conditional probability in yohan's paper. it is mathematically
            // correct but
            // not straightforward by just looking at the implementation.
            double beta0 = model.sumSTW[si][ti] + model.sumBeta[si][ti];
            int m0 = 0;
            double expectTSW = 1;
            for (Word sWord : wordCnt.keySet()) {
              SentiWord word = (SentiWord) sWord;
              double betaw = model.matrixSWT[si].getValue(word.wordNo, ti)
                  + model.beta[si][ti][word.wordNo];
              int cnt = wordCnt.get(word);
              for (int m = 0; m < cnt; m++) {
                expectTSW *= (betaw + m) / (beta0 + m0);
                m0++;
              }
            }
            probTable[ti][si] = (model.matrixSDT[si].getValue(docNo, ti) + model.alpha)
                / (model.sumDST[docNo][si] + model.sumAlpha)
                * (model.matrixDS.getValue(docNo, si) + model.gammas[si])
                * expectTSW;
            sumProb += probTable[ti][si];
          }
        }
      }

      int newTopic = 0, newSenti = 0;
      double randNo = Math.random() * sumProb;
      double tmpSumProb = 0;
      boolean found = false;
      for (int ti = 0; ti < model.numTopics; ti++) {
        for (int si = 0; si < model.numSenti; si++) {
          tmpSumProb += probTable[ti][si];
          if (randNo <= tmpSumProb) {
            newTopic = ti;
            newSenti = si;
            found = true;
          }
          if (found)
            break;
        }
        if (found)
          break;
      }

      sentence.setTopic(newTopic);
      sentence.setSenti(newSenti);
      for (Word sWord : sentence.getWords()) {
        SentiWord word = (SentiWord) sWord;
        word.setTopic(newTopic);
        word.setSentiment(newSenti);
        model.matrixSWT[newSenti].incValue(word.wordNo, newTopic);
        model.sumSTW[newSenti][newTopic]++;
      }
      model.matrixSDT[newSenti].incValue(docNo, newTopic);
      model.matrixDS.incValue(docNo, newSenti);
      model.sumDST[docNo][newSenti]++;
      model.sumDS[docNo]++;
    }
  }

  // Check to see if the sentence contains one sentiment (seed) word
  private boolean trim(Map<Word, Integer> wordCnt, int si) {
    // TODO(trung): uncomment if see worse behavior
    for (Word sWord : wordCnt.keySet()) {
      SentiWord word = (SentiWord) sWord;
      if (word.lexicon != null && word.lexicon != si) {
        return true;
      }
    }
    return false;
  }

  /**
   * Saves the model at current the iteration <code>iter</code>.
   * 
   * @param iter
   */
  void saveModel(int iter) {
    try {
      new File(model.outputDir + "/" + iter).mkdir();
      ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
          model.outputDir + "/" + iter + "/model.gz"));
      out.writeObject(model);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Writes output of the model at the specified iteration.
   * 
   * @param dir
   * @throws Exception
   */
  void writeModelOutput(int iter) {
    try {
      String dir = model.outputDir + "/" + iter;
      DoubleMatrix[] phi = Inference.calculatePhi(model.matrixSWT,
          model.sumSTW, model.beta, model.sumBeta);
      writePhi(phi, dir + "/Phi.csv");
      printTopWords(phi, dir, true);
      printTopWords(phi, dir, false);
      writeTheta(Inference.calculateTheta(model.matrixSDT, model.sumDST,
          model.alpha, model.sumAlpha), dir + "/Theta.csv");
      Inference.calculatePi(model.matrixDS, model.sumDS, model.gammas,
          model.sumGamma).writeMatrixToCSVFile(dir + "/Pi.csv");
      System.err.println("\nModel saved and written to " + dir);
    } catch (IOException e) {
      System.err.println("Error writing model output");
      e.printStackTrace();
    }
  }

  void writeTheta(double[][][] theta, String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int s = 0; s < model.numSenti; s++)
      for (int t = 0; t < model.numTopics; t++)
        out.print("S" + s + "-T" + t + ",");
    out.println();
    for (int d = 0; d < model.numDocuments; d++) {
      for (int s = 0; s < model.numSenti; s++) {
        for (int t = 0; t < model.numTopics; t++) {
          out.print(theta[s][d][t] + ",");
        }
      }
      out.println();
    }
    out.close();
  }

  void writePhi(DoubleMatrix[] phi, String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int s = 0; s < model.numSenti; s++)
      for (int t = 0; t < model.numTopics; t++)
        out.print(",S" + s + "-T" + t);
    out.println();
    for (int w = 0; w < model.wordList.size(); w++) {
      out.print(model.wordList.get(w));
      for (int s = 0; s < model.numSenti; s++) {
        for (int t = 0; t < model.numTopics; t++) {
          out.print("," + phi[s].getValue(w, t));
        }
      }
      out.println();
    }
    out.close();
  }

  /**
   * Prints top words.
   * 
   * @param dir
   * @param useTermScore
   *          true to print top words ranked by term-score
   */
  void printTopWords(DoubleMatrix[] phi, String dir, boolean useTermScore)
      throws IOException {
    String fileName;
    DoubleMatrix[] matrix;
    if (useTermScore) {
      matrix = buildTermScoreMatrix(phi);
      fileName = "TopWordsByTermScore.csv";
    } else {
      matrix = phi;
      fileName = "TopWords.csv";
    }
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(dir + "/" + fileName), "utf-8"));
    for (int s = 0; s < this.model.numSenti; s++) {
      for (int t = 0; t < this.model.numTopics; t++) {
        out.print("S" + s + "-T" + t + ",");
      }
    }
    int[][][] indices = getWordIndices(matrix);
    int idx;
    out.println();
    for (int w = 0; w < model.numProbWords; w++) {
      for (int s = 0; s < model.numSenti; s++) {
        for (int t = 0; t < model.numTopics; t++) {
          idx = indices[s][t][w];
          out.printf("%s (%.3f),", model.wordList.get(idx),
              matrix[s].getValue(idx, t));
        }
      }
      out.println();
    }
    out.close();
  }

  /**
   * Builds the term-score matrix from the inferred values of Phi.
   * 
   * @return
   */
  private DoubleMatrix[] buildTermScoreMatrix(DoubleMatrix[] phi) {
    DoubleMatrix[] termScore = new DoubleMatrix[phi.length];
    double sumOfLogs[] = new double[model.numUniqueWords];
    // compute the sum of logs for each word
    for (int w = 0; w < model.numUniqueWords; w++) {
      sumOfLogs[w] = 0.0;
      for (int s = 0; s < model.numSenti; s++) {
        for (int t = 0; t < model.numTopics; t++) {
          sumOfLogs[w] += Math.log(phi[s].getValue(w, t));
        }
      }
    }
    double score, prob;
    // int topics = numTopics * numSenti;
    // TODO(trung): this is a different from the term-score formula (with the
    // assumption that a senti-word has only one senti -> only numTopics)
    int topics = model.numTopics;
    for (int s = 0; s < model.numSenti; s++) {
      termScore[s] = new DoubleMatrix(model.numUniqueWords, model.numTopics);
      for (int t = 0; t < model.numTopics; t++) {
        for (int w = 0; w < model.numUniqueWords; w++) {
          prob = phi[s].getValue(w, t);
          score = prob * (Math.log(prob) - sumOfLogs[w] / topics);
          termScore[s].setValue(w, t, score);
        }
      }
    }
    return termScore;
  }

  /**
   * Gets indices of top words for each topic.
   * 
   * @return
   */
  private int[][][] getWordIndices(DoubleMatrix[] matrix) {
    int[][][] indices = new int[model.numSenti][model.numTopics][model.numProbWords];
    for (int s = 0; s < model.numSenti; s++) {
      for (int t = 0; t < model.numTopics; t++) {
        Vector<Integer> sortedIndexList = matrix[s].getSortedColIndex(t,
            model.numProbWords);
        for (int w = 0; w < sortedIndexList.size(); w++) {
          indices[s][t][w] = sortedIndexList.get(w);
        }
      }
    }
    return indices;
  }

  /**
   * The objective function of this model for MAP of betas.
   */
  class AscObjectiveFunction extends ObjectiveFunction {
    public AscObjectiveFunction(int numVariables) {
      super(numVariables);
    }

    @Override
    public double computeFunction(double[] vars)
        throws InvalidArgumentException {
      // compute L_B = - log likelihood = -log p(w,z,s|alpha, beta)
      double negLogLikelihood = 0.0;
      for (int j = 0; j < model.numSenti; j++) {
        for (int k = 0; k < model.numTopics; k++) {
          negLogLikelihood += MathUtils.logGamma(model.sumSTW[j][k]
              + model.sumBeta[j][k])
              - MathUtils.logGamma(model.sumBeta[j][k]);
          for (int i = 0; i < model.numUniqueWords; i++) {
            if (model.matrixSWT[j].getValue(i, k) > 0) {
              negLogLikelihood += MathUtils.logGamma(model.beta[j][k][i])
                  - MathUtils.logGamma(model.beta[j][k][i]
                      + model.matrixSWT[j].getValue(i, k));
            }
          }
        }
      }
      // compute log p(beta)
      double logPrior = 0;
      double term = 0.0;
      for (int i = 0; i < model.numUniqueWords; i++) {
        ArrayList<Integer> neighbors = model.graph.getNeighbors(i);
        // phi(i, iprime) = 1
        for (int iprime : neighbors) {
          for (int j = 0; j < model.numSenti; j++) {
            for (int k = 0; k < model.numTopics; k++) {
              term = model.y[j][k][i] - model.y[j][k][iprime];
              logPrior += term * term;
            }
          }
        }
      }
      // each edge can be used only once
      logPrior /= 2;
      for (int i = 0; i < model.numUniqueWords; i++) {
        logPrior += model.yWord[i] * model.yWord[i];
      }
      logPrior *= -0.5; // 0.5lamda^2 where lamda = 1
      return negLogLikelihood - logPrior;
    }

    @Override
    public double[] computeGradient(double[] vars)
        throws InvalidArgumentException {
      double[] grads = new double[getNumVariables()];
      double tmp;
      double[][][] betaJki = new double[model.numSenti][model.numTopics][model.numUniqueWords];
      // common beta terms for both y_jki and y_word i
      for (int j = 0; j < model.numSenti; j++) {
        for (int k = 0; k < model.numTopics; k++) {
          for (int i = 0; i < model.numUniqueWords; i++) {
            tmp = MathUtils.digamma(model.sumSTW[j][k] + model.sumBeta[j][k])
                - MathUtils.digamma(model.sumBeta[j][k]);
            if (model.matrixSWT[j].getValue(i, k) > 0) {
              tmp += MathUtils.digamma(model.beta[j][k][i])
                  - MathUtils.digamma(model.beta[j][k][i]
                      + model.matrixSWT[j].getValue(i, k));
            }
            betaJki[j][k][i] = model.beta[j][k][i] * tmp;
          }
        }
      }

      // gradients of y_jki
      int idx = 0;
      ArrayList<Integer> neighbors;
      for (int j = 0; j < model.numSenti; j++) {
        for (int k = 0; k < model.numTopics; k++) {
          for (int i = 0; i < model.numUniqueWords; i++) {
            grads[idx] = betaJki[j][k][i];
            neighbors = model.graph.getNeighbors(i);
            for (int iprime : neighbors) {
              grads[idx] += model.y[j][k][i] - model.y[j][k][iprime];
            }
            idx++;
          }
        }
      }
      // gradients of y_word i
      for (int i = 0; i < model.numUniqueWords; i++) {
        grads[idx] = -model.yWord[i];
        for (int j = 0; j < model.numSenti; j++) {
          for (int k = 0; k < model.numTopics; k++) {
            grads[idx] += betaJki[j][k][i];
          }
        }
        idx++;
      }
      return grads;
    }
  }
}
