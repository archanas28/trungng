package edu.kaist.uilab.bs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import com.aliasi.symbol.SymbolTable;

import edu.kaist.uilab.asc.Inference;
import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.IntegerMatrix;
import edu.kaist.uilab.asc.util.Utils;

/**
 * Stores all data used in the Gibbs sampling procedure. This model implements
 * {@link Serializable} so that it can be saved and loaded to allow Gibbs
 * sampler to continue from an existing model.
 * 
 * @author trung
 */
public class BSModel implements Serializable {

  private static final long serialVersionUID = 1L;
  int numProbWords = 100;

  boolean isExisting = false;
  String outputDir = ".";
  String extraInfo = ""; // extra info about the model

  int numSentiWords; // V'
  int numAspectWords; // V
  int numTopics; // K
  int numSenti; // S
  int numDocuments; // M
  List<Document> documents;
  SymbolTable sentiTable;
  SymbolTable aspectTable;
  TwogramsCounter counter;

  double alpha;
  double sumAlpha;
  double[] gammas;
  double sumGamma;
  double betaAspect;
  double sumBetaAspect;
  double[][] betaSenti;
  double[] sumBetaSenti; // sumBeta[senti]

  HashSet<Integer>[] seedWords;
  IntegerMatrix cntWT; // cwt[V][T]
  int[] sumWT; // sumWT[T]
  IntegerMatrix[] cntSWT; // cstw[S][V'][T]
  int[][] sumSTW; // sumSTW[S][T]
  IntegerMatrix cntDT; // cdt[M][T]
  int[] sumDT; // sumDT[M]
  IntegerMatrix cntDS;
  int[] sumDS; // sumDS[D]

  /**
   * @param numTopics
   * @param numSenti
   * @param sentiTable
   * @param aspectTable
   * @param counter
   * @param documents
   * @param seedWords
   * @param alpha
   * @param betaAspect
   * @param betaSenti
   * @param gammas
   */
  public BSModel(int numTopics, int numSenti, SymbolTable sentiTable,
      SymbolTable aspectTable, TwogramsCounter counter,
      List<Document> documents, HashSet<Integer>[] seedWords, double alpha,
      double betaAspect, double[] betaSenti, double[] gammas) {
    this.numTopics = numTopics;
    this.numSenti = numSenti;
    this.sentiTable = sentiTable;
    this.aspectTable = aspectTable;
    this.counter = counter;
    this.numSentiWords = sentiTable.numSymbols();
    this.numAspectWords = aspectTable.numSymbols();
    this.documents = documents;
    this.numDocuments = documents.size();
    this.seedWords = seedWords;
    this.alpha = alpha;
    this.betaAspect = betaAspect;
    this.betaSenti = getBetaSenti(betaSenti);
    this.gammas = gammas;
    cntWT = new IntegerMatrix(numAspectWords, numTopics);
    sumWT = new int[numTopics];
    cntSWT = new IntegerMatrix[numSenti];
    for (int i = 0; i < numSenti; i++) {
      cntSWT[i] = new IntegerMatrix(numSentiWords, numTopics);
    }
    sumSTW = new int[numSenti][numTopics];
    cntDT = new IntegerMatrix(numDocuments, numTopics);
    sumDT = new int[numDocuments];
    cntDS = new IntegerMatrix(numDocuments, numSenti);
    sumDS = new int[numDocuments];
    initHyperParameters(betaSenti);
  }

  double[][] getBetaSenti(double betaSenti[]) {
    double[][] ret = new double[numSenti][numSentiWords];
    for (int j = 0; j < numSenti; j++) {
      for (int i = 0; i < numSentiWords; i++) {
        ret[j][i] = getBeta(betaSenti, i, j);
      }
    }

    return ret;
  }

  /**
   * Gets the beta[senti] value for a sentiment word.
   */
  double getBeta(double[] betaSenti, Integer wordIdx, int s) {
    // same senti
    if (seedWords[s].contains(wordIdx)) {
      return betaSenti[0];
    }
    // opposite senti
    if (seedWords[1 - s].contains(wordIdx)) {
      return betaSenti[1];
    }
    return betaSenti[2];
  }

  /**
   * Initializes hyper parameters and related quantities for Gibbs sampling.
   */
  void initHyperParameters(double betaSenti[]) {
    sumAlpha = alpha * numTopics;
    sumGamma = 0;
    for (double gamma : gammas) {
      sumGamma += gamma;
    }
    sumBetaAspect = betaAspect * numAspectWords;
    sumBetaSenti = new double[numSenti];
    int numSeedWords = seedWords[0].size() + seedWords[1].size();
    double sumBetaOther = betaSenti[2] * (numSentiWords - numSeedWords);
    for (int s = 0; s < numSenti; s++) {
      int numSameSentimentWords = seedWords[s].size();
      sumBetaSenti[s] = sumBetaOther + numSameSentimentWords * betaSenti[0]
          + (numSeedWords - numSameSentimentWords) * betaSenti[1];
    }
  }

  /**
   * Loads an existing model from the specified file.
   * 
   * @param savedModel
   */
  public static BSModel loadModel(String savedModel) {
    BSModel model = null;
    ObjectInputStream in = null;
    try {
      System.err.println("Loading model");
      in = new ObjectInputStream(new FileInputStream(savedModel));
      model = (BSModel) in.readObject();
      model.isExisting = true;
      in.close();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return model;
  }

  public int getNumSentiments() {
    return numSenti;
  }
  
  public int getNumTopics() {
    return numTopics;
  }
  
  public DoubleMatrix[] getPhiSenti() {
    return Inference.computePhiSenti(cntSWT, sumSTW, betaSenti, sumBetaSenti);
  }

  public DoubleMatrix[] getPhiSentiByTermscore() {
    return buildTermscoreMatrix(getPhiSenti(), numTopics);
  }

  public double[][] getPhiAspect() {
    return Inference.computePhiAspect(cntWT, sumWT, betaAspect, sumBetaAspect);
  }

  public double[][] getPhiAspectByTermscore() {
    return buildTermscore(getPhiAspect());
  }
  
  public DoubleMatrix getPi() {
    return Inference.calculatePi(cntDS, sumDS, gammas, sumGamma);
  }

  public double[][] getTheta() {
    return Inference.computeTheta(cntDT, sumDT, alpha, sumAlpha);
  }

  public SymbolTable getSentiTable() {
    return sentiTable;
  }
  
  public SymbolTable getAspectTable() {
    return aspectTable;
  }
  
  public TwogramsCounter getTwogramsCounter() {
    return counter;
  }
  
  /**
   * Writes output of the model at the specified iteration.
   * 
   * @param iter
   * @throws Exception
   */
  public void writeModelOutput(int iter) {
    try {
      String dir = outputDir + "/" + iter;
      DoubleMatrix[] phiSenti = getPhiSenti();
      double[][] phiAspect = getPhiAspect();
      DoubleMatrix pi = getPi();
      writePhiSenti(phiSenti, dir + "/phiSenti.csv");
      writeTheta(getTheta(), dir + "/theta.csv");
      pi.writeMatrixToCSVFile(dir + "/pi.csv");
      writeTopSentiWords(phiSenti, dir + "/sentiWords.csv");
      String[][][] sentiWords = writeTopSentiWords(
          buildTermscoreMatrix(phiSenti, numTopics), dir
              + "/sentiWordsByTermscore.csv");
      writeTopAspectWords(phiAspect, dir + "/aspectWords.csv");
      String[][] aspectWords = writeTopAspectWords(buildTermscore(phiAspect),
          dir + "/aspectWordsByTermscore.csv");
      writeClassificationSummary(pi, dir + "/classification.txt");
      writeSampleClassifiedDocuments(dir + "/sampleDocs.html");
      writeCoherence(dir, sentiWords, aspectWords, 25, 25);
      writeCoherence(dir, sentiWords, aspectWords, 25, 50);
      writeCoherence(dir, sentiWords, aspectWords, 50, 50);
      writeCoherence(dir, sentiWords, aspectWords, 50, 100);
      System.err.println("\nModel saved and written to " + dir);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Writes some sample classified documents for inspection.
   * 
   * @param file
   * @throws IOException
   */
  private void writeSampleClassifiedDocuments(String file) throws IOException {
    int numDocs = 500;
    String[] sentiColors = { "green", "red" };
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    out.println("<html>");
    out.println("<body>");
    for (int i = 0; i < numDocs; i++) {
      int docIdx = (int) (Math.random() * numDocuments);
      out.println("<p>");
      Document doc = documents.get(docIdx);
      for (Sentence sentence : doc.getSentences()) {
        if (sentence.getSenti() > -1) {
          out.printf("<span style=\"color:%s\">%s.</span>",
              sentiColors[sentence.getSenti()], sentence.getText());
        } else {
          out.printf("%s.", sentence.getText());
        }
      }
      out.println("</p>");
    }
    out.println("</body>");
    out.println("</html>");
    out.close();
  }

  private void writeClassificationSummary(DoubleMatrix pi, String file)
      throws IOException {
    // get classification accuracy for english documents
    int observedSenti, inferedSenti, numCorrect = 0;
    int numNotRated = 0, numNeutral = 0, numPos = 0, numSubjective = 0;
    for (int i = 0; i < numDocuments; i++) {
      Document document = documents.get(i);
      double rating = document.getRating();
      if (rating != 3.0 && rating != -1.0) {
        numSubjective++;
        observedSenti = rating > 3.0 ? 0 : 1;
        inferedSenti = pi.getValue(i, 0) >= pi.getValue(i, 1) ? 0 : 1;
        if (observedSenti == inferedSenti) {
          numCorrect++;
        }
        if (observedSenti == 0) {
          numPos++;
        }
      } else {
        if (rating == 3.0) {
          numNeutral++;
        } else {
          numNotRated++;
        }
      }
    }
    PrintWriter out = new PrintWriter(file);
    out.println("English reviews:");
    out.printf("\tSubjective:\t%d\tpos = %d(%.2f)\n", numSubjective, numPos,
        ((double) numPos) / numSubjective);
    out.printf("\tNeutral:\t%d\n", numNeutral);
    out.printf("\tNot rated:\t%d\n", numNotRated);
    out.printf("\tAccuracy:\t%.5f\n", ((double) numCorrect) / numSubjective);
    out.close();
  }

  private void writeTheta(double[][] theta, String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int t = 0; t < numTopics; t++)
      out.print("T" + t + ",");
    out.println();
    for (int d = 0; d < numDocuments; d++) {
      for (int t = 0; t < numTopics; t++) {
        out.print(theta[d][t] + ",");
      }
      out.println();
    }
    out.close();
  }

  private void writePhiSenti(DoubleMatrix[] phi, String file)
      throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int s = 0; s < numSenti; s++)
      for (int t = 0; t < numTopics; t++)
        out.print(",S" + s + "-T" + t);
    out.println();
    for (int w = 0; w < numSentiWords; w++) {
      out.print(sentiTable.idToSymbol(w));
      for (int s = 0; s < numSenti; s++) {
        for (int t = 0; t < numTopics; t++) {
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
   * @param matrix
   */
  private String[][][] writeTopSentiWords(DoubleMatrix[] matrix, String file)
      throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        out.print("S" + s + "-T" + t + ",");
      }
    }
    out.println();
    int[][][] indice = getIndiceOfTopSentiWords(matrix);
    String[][][] topWords = new String[numSenti][numTopics][numProbWords];
    for (int w = 0; w < numProbWords; w++) {
      for (int s = 0; s < numSenti; s++) {
        for (int t = 0; t < numTopics; t++) {
          int idx = indice[s][t][w];
          String word = sentiTable.idToSymbol(idx);
          out.printf("%s (%.3f),", word, matrix[s].getValue(idx, t));
          topWords[s][t][w] = word;
        }
      }
      out.println();
    }
    out.close();

    return topWords;
  }

  /**
   * Prints and returns top words for all topics.
   * 
   * @param phi
   * @param file
   * @throws IOException
   */
  private String[][] writeTopAspectWords(double[][] phi, String file)
      throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (int topic = 0; topic < numTopics; topic++) {
      out.printf("T%d,", topic);
    }
    out.println();
    int[][] topIndice = getIndiceOfTopAspectWords(phi);
    // write the inverse of the top word matrix (for easy visualization)
    String[][] topWords = new String[numTopics][numProbWords];
    for (int i = 0; i < numProbWords; i++) {
      for (int topic = 0; topic < numTopics; topic++) {
        int wordId = topIndice[topic][i];
        String word = aspectTable.idToSymbol(wordId);
        out.printf("%s(%.5f),", word, phi[topic][wordId]);
        topWords[topic][i] = word;
      }
      out.println();
    }
    out.close();

    return topWords;
  }

  /**
   * Builds the term-score matrix from the inferred values of Phi.
   * 
   * @return
   */
  private DoubleMatrix[] buildTermscoreMatrix(DoubleMatrix[] phi, int topics) {
    DoubleMatrix[] termScore = new DoubleMatrix[phi.length];
    double sumOfLogs[] = new double[numSentiWords];
    // compute the sum of logs for each word
    for (int w = 0; w < numSentiWords; w++) {
      sumOfLogs[w] = 0.0;
      for (int s = 0; s < numSenti; s++) {
        for (int t = 0; t < numTopics; t++) {
          sumOfLogs[w] += Math.log(phi[s].getValue(w, t));
        }
      }
    }
    double score, prob;
    for (int s = 0; s < numSenti; s++) {
      termScore[s] = new DoubleMatrix(numSentiWords, numTopics);
      for (int t = 0; t < numTopics; t++) {
        for (int w = 0; w < numSentiWords; w++) {
          prob = phi[s].getValue(w, t);
          score = prob * (Math.log(prob) - sumOfLogs[w] / topics);
          termScore[s].setValue(w, t, score);
        }
      }
    }
    return termScore;
  }

  private double[][] buildTermscore(double[][] phi) {
    double[][] termscore = new double[numTopics][numAspectWords];
    double sumOfLogs[] = new double[numAspectWords];
    // compute the sum of logs for each word
    for (int w = 0; w < numAspectWords; w++) {
      sumOfLogs[w] = 0.0;
      for (int t = 0; t < numTopics; t++) {
        sumOfLogs[w] += Math.log(phi[t][w]);
      }
    }
    for (int t = 0; t < numTopics; t++) {
      for (int w = 0; w < numAspectWords; w++) {
        termscore[t][w] = phi[t][w]
            * (Math.log(phi[t][w]) - sumOfLogs[w] / numTopics);
      }
    }

    return termscore;
  }

  /**
   * Gets indice of top sentiment words for all topics.
   * 
   * @return
   */
  public int[][][] getIndiceOfTopSentiWords(DoubleMatrix[] matrix) {
    int[][][] indice = new int[numSenti][numTopics][numProbWords];
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        Vector<Integer> sortedIndexList = matrix[s].getSortedColIndex(t,
            numProbWords);
        for (int w = 0; w < sortedIndexList.size(); w++) {
          indice[s][t][w] = sortedIndexList.get(w);
        }
      }
    }
    
    return indice;
  }

  /**
   * Gets the top sentiment words for all topics.
   * 
   * @return
   */
  public String[][][] getTopSentiWords() {
    int[][][] indice = getIndiceOfTopSentiWords(getPhiSentiByTermscore());
    String[][][] words = new String[numSenti][numTopics][numProbWords];
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        for (int w = 0; w < numProbWords; w++) {
          words[s][t][w] = sentiTable.idToSymbol(indice[s][t][w]);
        }
      }
    }
    
    return words;
  }
  
  /**
   * Gets indice of top aspect words for all topics.
   * 
   * @param phi
   * @return
   */
  public int[][] getIndiceOfTopAspectWords(double[][] phi) {
    int[][] topIndice = new int[numTopics][];
    for (int topic = 0; topic < numTopics; topic++) {
      topIndice[topic] = Utils.topColumns(phi, topic, numProbWords);
    }
    
    return topIndice;
  }
  
  /**
   * Gets the top aspect words for all topics.
   * 
   * @return
   */
  public String[][] getTopAspectWords() {
    int[][] indice = getIndiceOfTopAspectWords(getPhiAspectByTermscore());
    String[][] words = new String[numTopics][numProbWords];
    for (int t = 0; t < numTopics; t++) {
      for (int w = 0; w < numProbWords; w++) {
        words[t][w] = aspectTable.idToSymbol(indice[t][w]);
      }
    }
    
    return words;
  }
  
  /**
   * Reports the coherence scores between topics.
   * 
   * @param file
   * @param aspectWords
   * @param sentiWords
   */
  private void writeCoherence(String dir, String[][][] sentiWords,
      String[][] aspectWords, int nSenti, int nAspect) throws IOException {
    PrintWriter out = new PrintWriter(dir + "/coherence-A" + nAspect + "-S"
        + nSenti + ".csv");
    PrintWriter out2 = new PrintWriter(dir + "/altCoherence-A" + nAspect + "-S"
        + nSenti + ".csv");
    for (int k1 = 0; k1 < numTopics; k1++) {
      for (int k2 = 0; k2 < numTopics; k2++) {
        out.print(computeCoherenceScore(sentiWords[0][k1], sentiWords[1][k1],
            aspectWords[k2], nAspect, nSenti) + ",");
        out2.print(computeAlternativeCoherenceScore(sentiWords[0][k1],
            sentiWords[1][k1], aspectWords[k2], nAspect, nSenti) + ",");
      }
      out.println();
      out2.println();
    }
    out.close();
    out2.close();
  }

  private int computeCoherenceScore(String[] senti0Words, String[] senti1Words,
      String[] aspectWords, int nAspect, int nSenti) {
    int cnt = 0;
    for (int wordIdx = 0; wordIdx < nSenti; wordIdx++) {
      for (int aspectIdx = 0; aspectIdx < nAspect; aspectIdx++) {
        cnt += counter.getCount(senti0Words[wordIdx], aspectWords[aspectIdx]);
      }
    }
    for (int wordIdx = 0; wordIdx < nSenti; wordIdx++) {
      for (int aspectIdx = 0; aspectIdx < nAspect; aspectIdx++) {
        cnt += counter.getCount(senti1Words[wordIdx], aspectWords[aspectIdx]);
      }
    }

    return cnt;
  }

  private int computeAlternativeCoherenceScore(String[] senti0Words,
      String[] senti1Words, String[] aspectWords, int nAspect, int nSenti) {
    int cnt = 0;
    for (int wordIdx = 0; wordIdx < nSenti; wordIdx++) {
      for (int aspectIdx = 0; aspectIdx < nAspect; aspectIdx++) {
        if (counter.getCount(senti0Words[wordIdx], aspectWords[aspectIdx]) > 0) {
          cnt++;
        }
      }
    }
    for (int wordIdx = 0; wordIdx < nSenti; wordIdx++) {
      for (int aspectIdx = 0; aspectIdx < nAspect; aspectIdx++) {
        if (counter.getCount(senti1Words[wordIdx], aspectWords[aspectIdx]) > 0) {
          cnt++;
        }
      }
    }

    return cnt;
  }
}
