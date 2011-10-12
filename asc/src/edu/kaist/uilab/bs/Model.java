package edu.kaist.uilab.bs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import com.aliasi.symbol.SymbolTable;
import com.aliasi.util.ObjectToDoubleMap;

import edu.kaist.uilab.asc.Inference;
import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.IntegerMatrix;
import edu.kaist.uilab.asc.util.Utils;
import edu.kaist.uilab.bs.util.BSUtils;

/**
 * Stores all data used in the Gibbs sampling procedure. This model implements
 * {@link Serializable} so that it can be saved and loaded to allow Gibbs
 * sampler to continue from an existing training.
 * 
 * @author trung
 */
public class Model implements Serializable {

  private static final long serialVersionUID = 1L;
  private int numProbWords = 100;

  boolean isExisting = false;
  protected String outputDir = ".";
  protected String extraInfo = ""; // extra info about the model

  protected int numSentiWords; // V'
  protected int numAspectWords; // V
  protected int numTopics; // K
  protected int numSenti; // S
  private int numDocuments; // M
  protected List<Document> documents;
  HashMap<String, Document> annotatedDocuments;
  protected SymbolTable sentiTable;
  SymbolTable aspectTable;
  TwogramsCounter counter;

  double alpha;
  double sumAlpha;
  double[] gammas;
  double sumGamma;
  double betaAspect;
  double sumBetaAspect;
  private double[][] beta; // beta[senti][word]
  private double[] sumBeta; // sumBeta[senti]

  protected HashSet<Integer>[] seedWords;
  protected IntegerMatrix cntWT; // cwt[V][T]
  protected int[] sumWT; // sumWT[T]
  protected IntegerMatrix[] cntSWT; // cstw[S][V'][T]
  protected int[][] sumSTW; // sumSTW[S][T]
  protected IntegerMatrix cntDT; // cdt[M][T]
  protected int[] sumDT; // sumDT[M]
  protected IntegerMatrix cntDS;
  protected int[] sumDS; // sumDS[D]

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
  public Model(int numTopics, int numSenti, SymbolTable sentiTable,
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
    sumAlpha = alpha * numTopics;
    sumGamma = 0;
    for (double gamma : gammas) {
      sumGamma += gamma;
    }
    sumBetaAspect = betaAspect * numAspectWords;
    initBeta(betaSenti);

    this.annotatedDocuments = null;
  }

  /**
   * Sets annotated documents for sentence-level sentiment classification.
   * 
   * @param documents
   */
  public void setAnnotatedDocuments(HashMap<String, Document> documents) {
    annotatedDocuments = documents;
  }

  protected double[][] getBetaSenti(double betaSenti[]) {
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
  protected double getBeta(double[] betaSenti, Integer wordIdx, int s) {
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
  protected void initBeta(double betaSenti[]) {
    this.beta = getBetaSenti(betaSenti);
    sumBeta = new double[numSenti];
    int numSeedWords = seedWords[0].size() + seedWords[1].size();
    double sumBetaOther = betaSenti[2] * (numSentiWords - numSeedWords);
    for (int s = 0; s < numSenti; s++) {
      int numSameSentimentWords = seedWords[s].size();
      sumBeta[s] = sumBetaOther + numSameSentimentWords * betaSenti[0]
          + (numSeedWords - numSameSentimentWords) * betaSenti[1];
    }
  }

  /**
   * Loads an existing model from the specified file.
   * 
   * @param savedModel
   */
  public static Model loadModel(String savedModel) {
    Model model = null;
    ObjectInputStream in = null;
    try {
      System.err.println("Loading model");
      in = new ObjectInputStream(new FileInputStream(savedModel));
      model = (Model) in.readObject();
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

  public List<Document> getDocuments() {
    return documents;
  }

  public void setNumDocuments(int numDocuments) {
    this.numDocuments = numDocuments;
  }

  public int getNumDocuments() {
    return numDocuments;
  }

  public int getNumProbWords() {
    return numProbWords;
  }

  public DoubleMatrix[] getPhiSenti() {
    return Inference.computePhiSenti(cntSWT, sumSTW, beta, sumBeta);
  }

  public ObjectToDoubleMap<String>[][] getPhiSentiIndexedByWord() {
    @SuppressWarnings("unchecked")
    ObjectToDoubleMap<String>[][] newPhi = new ObjectToDoubleMap[numSenti][numTopics];
    DoubleMatrix[] phi = getPhiSenti();
    for (int senti = 0; senti < numSenti; senti++) {
      for (int topic = 0; topic < numTopics; topic++) {
        newPhi[senti][topic] = new ObjectToDoubleMap<String>();
        for (int idx = 0; idx < numSentiWords; idx++) {
          newPhi[senti][topic].increment(sentiTable.idToSymbol(idx),
              phi[senti].getValue(idx, topic));
        }
      }
    }
    return newPhi;
  }

  public DoubleMatrix[] getPhiSentiByTermscore() {
    return buildTermscoreMatrix(getPhiSenti(), numTopics);
  }

  public double[][] getPhiAspect() {
    return Inference.computePhiAspect(cntWT, sumWT, betaAspect, sumBetaAspect);
  }

  public ObjectToDoubleMap<String>[] getPhiAspectIndexedByWord() {
    @SuppressWarnings("unchecked")
    ObjectToDoubleMap<String>[] newPhi = new ObjectToDoubleMap[numTopics];
    double[][] phi = getPhiAspect();
    for (int topic = 0; topic < numTopics; topic++) {
      newPhi[topic] = new ObjectToDoubleMap<String>();
      for (int idx = 0; idx < numAspectWords; idx++) {
        newPhi[topic].increment(aspectTable.idToSymbol(idx), phi[topic][idx]);
      }
    }
    return newPhi;
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
      new File(dir).mkdir();
      BSUtils.saveModel(dir + "/model.gz", this);
      DoubleMatrix[] phiSenti = getPhiSenti();
      double[][] phiAspect = getPhiAspect();
      DoubleMatrix pi = getPi();
      // writePhiSenti(phiSenti, dir + "/phiSenti.csv");
      // writeTheta(getTheta(), dir + "/theta.csv");
      // pi.writeMatrixToCSVFile(dir + "/pi.csv");
      writeTopSentiWords(phiSenti, dir + "/sentiWords.csv");
      String[][][] sentiWords = writeTopSentiWords(
          buildTermscoreMatrix(phiSenti, numTopics), dir
              + "/sentiWordsByTermscore.csv");
      writeTopAspectWords(phiAspect, dir + "/aspectWords.csv");
      String[][] aspectWords = writeTopAspectWords(buildTermscore(phiAspect),
          dir + "/aspectWordsByTermscore.csv");
      writeDocumentClassificationSummary(pi, dir + "/newdocsentiment.txt");
      if (annotatedDocuments != null) {
        writeSentenceClassificationSummary(dir + "/sentenceClassification.txt");
      }
      writeSampleClassifiedDocuments(dir + "/sampleDocs.html");
      writeSampleSummarizedDocuments(dir + "/summarizeDocs.html", sentiWords,
          aspectWords);
      writeCoherence(dir, sentiWords, aspectWords, 25, 50);
      System.err.println("\nModel saved and written to " + dir);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Writes summary of the sentence-level sentiment classification task.
   * 
   * @param file
   * @throws IOException
   */
  public void writeSentenceClassificationSummary(String file)
      throws IOException {
    int[] sentimentClass = countSentimentClasses(annotatedDocuments);
    int numPosCorrect = 0, numPosWrong = 0;
    int numNegCorrect = 0, numNegWrong = 0;
    int numSentencesNotMatched = 0;
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (Document document : getDocuments()) {
      if (annotatedDocuments.containsKey(document.getReviewId())) {
        Document annotatedDoc = annotatedDocuments.get(document.getReviewId());
        for (Sentence sentence : annotatedDoc.getSentences()) {
          // only consider sentence annotated with no sentiment conflict
          int annotated = sentence.getSenti();
          if (annotated != -1) {
            int classified = document.getSentenceSentiment(sentence.getText());
            if (classified != Document.NO_SENTENCE) {
              if (annotated == classified) {
                if (classified == 0) {
                  numPosCorrect++;
                } else {
                  numNegCorrect++;
                }
              } else {
                if (classified == 0) {
                  numPosWrong++;
                } else {
                  numNegWrong++;
                }
              }
            } else {
              numSentencesNotMatched++;
            }
          }
        }
      }
    }
    out.printf("#annotated documents: %d\n", annotatedDocuments.size());
    out.printf("#annotated positive: %d\n", sentimentClass[0]);
    out.printf("#annotated negative: %d\n", sentimentClass[1]);
    out.printf("#annotated neutral or conflict: %d\n", sentimentClass[2]);
    out.printf("#sentences not matched: %d\n", numSentencesNotMatched);
    out.printf(
        "#numPosCorrect = %d, numPosWrong = %d, numNegCorrect=%d, numNegWrong=%d\n",
        numPosCorrect, numPosWrong, numNegCorrect, numNegWrong);
    out.printf("accuracy (positive + negative): %.3f\n", (numPosCorrect
        + numNegCorrect + 0.0)
        / (sentimentClass[0] + sentimentClass[1] - numSentencesNotMatched));
    out.printf("precision (positive): %.3f\n", (numPosCorrect + 0.0)
        / (numPosCorrect + numPosWrong));
    out.printf("recall (positive): %.3f\n", (numPosCorrect + 0.0)
        / sentimentClass[0]);
    out.printf("precision (negative): %.3f\n", (numNegCorrect + 0.0)
        / (numNegCorrect + numNegWrong));
    out.printf("recall (negative): %.3f\n", (numNegCorrect + 0.0)
        / sentimentClass[1]);
    out.close();
  }

  /**
   * Counts the number of sentences for all sentiment classes.
   * 
   * @param documents
   *          annotated documents
   * @return
   */
  private int[] countSentimentClasses(HashMap<String, Document> documents) {
    int[] count = new int[3];
    for (Document document : documents.values()) {
      for (Sentence sentence : document.getSentences()) {
        int senti = sentence.getSenti();
        if (senti == -1) {
          count[2]++;
        } else {
          count[senti]++;
        }
      }
    }

    return count;
  }

  /**
   * Writes some sample classified documents for inspection.
   * 
   * @param file
   * @throws IOException
   */
  public void writeSampleClassifiedDocuments(String file) throws IOException {
    int numDocs = 500;
    String[] sentiColors = { "green", "red" };
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    out.println("<html>");
    out.println("<body>");
    for (int i = 0; i < numDocs; i++) {
      int docIdx = (int) (Math.random() * numDocuments);
      out.println("<p>");
      Document doc = getDocuments().get(docIdx);
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

  /**
   * Writes some sample reviews summarized by sentiment-aspect word pairs.
   * 
   * @param file
   * @param sentiWords
   * @param aspectWords
   * @throws IOException
   */
  public void writeSampleSummarizedDocuments(String file,
      String[][][] sentiWords, String[][] aspectWords) throws IOException {
    int numDocs = 10;
    String[] sentiColors = { "green", "red" };
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    out.println("<html>");
    out.println("<body>");
    for (int docIdx = 0; docIdx < numDocs; docIdx++) {
      out.println("<p>");
      Document doc = getDocuments().get(docIdx);
      for (Sentence sentence : doc.getSentences()) {
        int senti = sentence.getSenti();
        if (senti > -1) {
          int topic = sentence.getTopic();
          Vector<Integer> mySentiWords = sentence.getSentiWords();
          Vector<Integer> myAspectWords = sentence.getAspectWords();
          StringBuilder pairs = new StringBuilder("[");
          for (String sentiWord : sentiWords[senti][topic]) {
            for (String aspectWord : aspectWords[topic]) {
              if (mySentiWords.contains(sentiTable.symbolToID(sentiWord))
                  && myAspectWords.contains(aspectTable.symbolToID(aspectWord))) {
                pairs.append(sentiWord).append(" ").append(aspectWord)
                    .append(", ");
              }
            }
          }
          pairs.append("]\t");
          out.printf(
              "%s, aspect %d, <span style=\"color:%s\">%s.</span><br />",
              pairs.toString(), topic, sentiColors[sentence.getSenti()],
              sentence.getText());
        } else {
          out.printf("%s.<br />", sentence.getText());
        }
      }
      out.println("</p>");
    }
    out.println("</body>");
    out.println("</html>");
    out.close();
  }

  public void writeDocumentClassificationSummary(DoubleMatrix pi, String file)
      throws IOException {
    int numPosCorrect = 0, numNegCorrect = 0;
    int numPosWrong = 0, numNegWrong = 0;
    int numNotRated = 0, numNeutral = 0, numPos = 0, numNeg = 0;
    for (int i = 0; i < numDocuments; i++) {
      Document document = getDocuments().get(i);
      double rating = document.getRating();
//      if (rating != 3.0 && rating != -1.0) {
         if (rating != -1.0) {
        int observedSenti = rating >= 3.0 ? 0 : 1;
        if (observedSenti == 0) {
          numPos++;
        } else {
          numNeg++;
        }
        int inferedSenti = pi.getValue(i, 0) >= pi.getValue(i, 1) ? 0 : 1;
        if (inferedSenti == observedSenti) {
          if (inferedSenti == 0) {
            numPosCorrect++;
          } else {
            numNegCorrect++;
          }
        } else {
          if (inferedSenti == 0) {
            numPosWrong++;
          } else {
            numNegWrong++;
          }
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
    out.printf("#positive = %d, #negative = %d, #total subjective =%d\n",
        numPos, numNeg, numPos + numNeg);
    out.printf("#neutral =%d, #not rated = %d, sum = %d\n", numNeutral,
        numNotRated, numNeutral + numNotRated);
    out.printf(
        "#numPosCorrect = %d, numPosWrong = %d, numNegCorrect=%d, numNegWrong=%d\n",
        numPosCorrect, numPosWrong, numNegCorrect, numNegWrong);
    out.printf("accuracy (positive + negative): %.3f\n", (numPosCorrect
        + numNegCorrect + 0.0)
        / (numPos + numNeg));
    out.printf("precision (positive): %.3f\n", (numPosCorrect + 0.0)
        / (numPosCorrect + numPosWrong));
    out.printf("recall (positive): %.3f\n", (numPosCorrect + 0.0) / numPos);
    out.printf("precision (negative): %.3f\n", (numNegCorrect + 0.0)
        / (numNegCorrect + numNegWrong));
    out.printf("recall (negative): %.3f\n", (numNegCorrect + 0.0) / numNeg);
    out.close();
  }

  @SuppressWarnings("unused")
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

  @SuppressWarnings("unused")
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
  public String[][][] writeTopSentiWords(DoubleMatrix[] matrix, String file)
      throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        out.print("S" + s + "-T" + t + ",");
      }
    }
    out.println();
    int[][][] indice = getIndiceOfTopSentiWords(matrix, getNumProbWords());
    String[][][] topWords = new String[numSenti][numTopics][getNumProbWords()];
    for (int w = 0; w < getNumProbWords(); w++) {
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
  public String[][] writeTopAspectWords(double[][] phi, String file)
      throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (int topic = 0; topic < numTopics; topic++) {
      out.printf("T%d,", topic);
    }
    out.println();
    int[][] topIndice = getIndiceOfTopAspectWords(phi, getNumProbWords());
    // write the inverse of the top word matrix (for easy visualization)
    String[][] topWords = new String[numTopics][getNumProbWords()];
    for (int i = 0; i < getNumProbWords(); i++) {
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
   * @param numWords
   *          number of top words to get
   * @return
   */
  public int[][][] getIndiceOfTopSentiWords(DoubleMatrix[] matrix, int numWords) {
    int[][][] indice = new int[numSenti][numTopics][numWords];
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        Vector<Integer> sortedIndexList = matrix[s].getSortedColIndex(t,
            numWords);
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
  public String[][][] getTopSentiWords(int numWords) {
    int[][][] indice = getIndiceOfTopSentiWords(getPhiSentiByTermscore(),
        numWords);
    String[][][] words = new String[numSenti][numTopics][numWords];
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        for (int w = 0; w < numWords; w++) {
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
   * @param numWords
   * @return
   */
  public int[][] getIndiceOfTopAspectWords(double[][] phi, int numWords) {
    int[][] topIndice = new int[numTopics][];
    for (int topic = 0; topic < numTopics; topic++) {
      topIndice[topic] = Utils.topColumns(phi, topic, numWords);
    }

    return topIndice;
  }

  /**
   * Gets the top aspect words for all topics.
   * 
   * @param numWords
   *          number of top words to get
   * @return
   */
  public String[][] getTopAspectWords(int numWords) {
    int[][] indice = getIndiceOfTopAspectWords(getPhiAspectByTermscore(),
        numWords);
    String[][] words = new String[numTopics][numWords];
    for (int t = 0; t < numTopics; t++) {
      for (int w = 0; w < numWords; w++) {
        words[t][w] = aspectTable.idToSymbol(indice[t][w]);
      }
    }

    return words;
  }

  /**
   * Classifies topic of a segment given its stemmed words <code>words</code>.
   * <p>
   * This classification does not take into account where the segment comes
   * from, i.e., it can classify any arbitrary segment of text. In other words,
   * it returns <code>k_max = argmax_k(p(k|S))</code>.
   * 
   * @param phiAspect
   *          the per-aspect word distributions
   * @param words
   *          the words of the segment
   * @return the topic of this segment or <code>-1</code> if cannot classify
   */
  private int classifySegmentTopic(double[][] phiAspect, String[] words) {
    int maxTopic = -1;
    double max = 0.0;
    for (int topic = 0; topic < numTopics; topic++) {
      double prob = getSegmentProb(phiAspect, words, topic);
      if (max < prob) {
        max = prob;
        maxTopic = topic;
      }
    }

    return maxTopic;
  }

  /**
   * Classifies topic of a segment given its stemmed worsd <code>words</code>.
   * <p>
   * This does a similar job to {
   * {@link #classifySegmentTopic(double[][], String[])} but is used for the
   * segment without an aspect word (i.e., classifySegmentTopic gives -1 for the
   * segment).
   * 
   * @param phiSenti
   * @param words
   * @return
   */
  private int classifySegmentTopic(DoubleMatrix[] phiSenti, String[] words) {
    int maxTopic = -1;
    double max = 0.0;
    for (int topic = 0; topic < numTopics; topic++) {
      double prob = getSentimentProb(phiSenti, words, topic, 0)
          + getSentimentProb(phiSenti, words, topic, 1);
      if (max < prob) {
        max = prob;
        maxTopic = topic;
      }
    }

    return maxTopic;
  }

  /**
   * Classifies topic of a segment given its stemmed words <code>words</code>.
   * <p>
   * This classifier first classifies the segment only using its aspect words.
   * If it fails to classify, it classifies using its sentiment words.
   * 
   * @param phiAspect
   * @param phiSenti
   * @param words
   * @return
   */
  public int classifySegmentTopic(double[][] phiAspect,
      DoubleMatrix[] phiSenti, String[] words) {
    int k = classifySegmentTopic(phiAspect, words);
    if (k < 0) {
      k = classifySegmentTopic(phiSenti, words);
    }

    return k;
  }

  /**
   * Classifies the sentiment of a segment given its aspect.
   * <p>
   * This returns <code>j_max = argmax_j(p(j|k,S)</code>.
   * 
   * @param phiSenti
   * @param words
   * @param topic
   * @return 0 for positive, 1 for negative, -1 for neutral (or cannot classify)
   */
  public int classifySegmentSentiment(DoubleMatrix[] phiSenti, String[] words,
      int topic) {
    double proProb = getSentimentProb(phiSenti, words, topic, 0);
    double negProb = getSentimentProb(phiSenti, words, topic, 1);
    if (proProb > negProb) {
      return 0;
    } else if (proProb < negProb) {
      return 1;
    } else {
      // consider neutral if proProb == negProb (both could equal 0.0)
      return -1;
    }
  }

  /**
   * Returns <code>p(S|j, k)</code> where S is a segment (<code>words</code>), k
   * is a topic, and j is a sentiment.
   * 
   * @param phiSenti
   * @param words
   * @param topic
   * @param sentiment
   * @return
   */
  public double getSentimentProb(DoubleMatrix[] phiSenti, String[] words,
      int topic, int sentiment) {
    double prob = 1.0;
    for (String word : words) {
      double wordProb = getWordProb(phiSenti, word, topic, sentiment);
      // wordProb == 0 means the word is not in the sentiment vocabulary
      // its probability is very low across all topics
      if (wordProb != 0) {
        prob *= wordProb;
      }
    }

    return prob != 1.0 ? prob : 0.0;
  }

  /**
   * Returns p(segment | topic) where segment is <code>words</code>.
   * 
   * @param phiAspect
   *          the inferred (approximated) phi aspect
   * @param phiSenti
   * @param words
   * @param k
   * @return the classified topic; 0 if the segment cannot be classified into
   *         one of the topics (primarily because it does not contain any aspect
   *         word)
   */
  public double getSegmentProb(double[][] phiAspect, String[] words, int topic) {
    double prob = 1.0;
    for (String word : words) {
      double wordProb = getWordProb(phiAspect, word, topic);
      // wordProb == 0 means the word is not in the aspect vocabulary
      // its probability is very low across all topics
      if (wordProb != 0) {
        prob *= wordProb;
      }
    }

    return prob != 1.0 ? prob : 0.0;
  }

  /**
   * Returns true if the given segment (<code>words</code>) contains at least
   * one of the top words (which could be top aspect words or sentiment words).
   * 
   * @param topWords
   * @param words
   * @return
   */
  public boolean segmentContainsTopWords(String[] topWords, String[] words) {
    for (String word : words) {
      if (BSUtils.isInArray(topWords, word)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns <code>p(word|topic)</code>.
   * 
   * @param phiAspect
   * @param word
   * @param topic
   * @return 0 if the word is not in the aspect vocabulary (actually it is very
   *         very small)
   */
  private double getWordProb(double[][] phiAspect, String word, int topic) {
    int id = aspectTable.symbolToID(word);
    if (id != SymbolTable.UNKNOWN_SYMBOL_ID) {
      return phiAspect[topic][id];
    }

    return 0.0;
  }

  /**
   * Returns <code>p(word|topic, sentiment)</code>.
   * 
   * @param phiSenti
   * @param word
   * @param topic
   * @param sentiment
   * @return 0 if the word is not in the sentiment vocabulary (actually it is
   *         very very small)
   */
  private double getWordProb(DoubleMatrix[] phiSenti, String word, int topic,
      int sentiment) {
    int id = sentiTable.symbolToID(word);
    if (id != SymbolTable.UNKNOWN_SYMBOL_ID) {
      return phiSenti[sentiment].getValue(id, topic);
    }

    return 0.0;
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
    for (int k1 = 0; k1 < numTopics; k1++) {
      for (int k2 = 0; k2 < numTopics; k2++) {
        out.print(computeCoherenceScore(sentiWords[0][k1], sentiWords[1][k1],
            aspectWords[k2], nAspect, nSenti) + ",");
      }
      out.println();
    }
    out.close();
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

  /**
   * Returns beta'[s][k][word].
   * 
   * @param s
   *          a sentiment (0 for positive, 1 for negative)
   * @param k
   *          a topic
   * @param sentiWord
   *          a word index
   * @return
   */
  public double getBetaSenti(int s, int k, int sentiWord) {
    return beta[s][sentiWord];
  }

  /**
   * Returns sumBeta'[s][k] = sumOf(beta'[s][k][w]) for all <code>w</code> in
   * sentiment words.
   * 
   * @param s
   * @param k
   * @return
   */
  public double getSumBetaSenti(int s, int k) {
    return sumBeta[s];
  }
}
