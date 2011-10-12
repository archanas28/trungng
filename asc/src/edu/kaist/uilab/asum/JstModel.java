/*
 * Implementation of Sentence Topic/Opinion
 *   - Different THETAs for different sentiments: THETA[S]
 *   - Positive/Negative
 * Author: Yohan Jo
 */
package edu.kaist.uilab.asum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import com.aliasi.util.ObjectToDoubleMap;

import edu.kaist.uilab.asc.Application;
import edu.kaist.uilab.asc.LocaleWord;
import edu.kaist.uilab.asc.data.Document;
import edu.kaist.uilab.asc.data.SamplingWord;
import edu.kaist.uilab.asc.data.Sentence;
import edu.kaist.uilab.asc.data.SentiWord;
import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.IntegerMatrix;
import edu.kaist.uilab.asc.util.TextFiles;
import edu.kaist.uilab.bs.CorpusParser;
import edu.kaist.uilab.bs.TwogramsCounter;
import edu.kaist.uilab.bs.util.BSUtils;
import edu.kaist.uilab.bs.util.DocumentUtils;

/**
 * Implementation of the JST model.
 * 
 * @author trung
 */
public class JstModel {

  public static class JstModelData implements Serializable {
    private static final long serialVersionUID = 3339673245278669017L;

    int numProbWords = 200;
    int maxSentenceLength = 40;
    Integer outputInterval = 500;

    String inputDir;
    String outputDir;

    int numUniqueWords;
    int numTopics;
    int numSenti;
    int numRealIterations;
    int numDocuments;
    List<LocaleWord> wordList;
    double alpha;
    double sumAlpha;
    double[] betas;
    double[] sumBeta;
    double[] gammas;
    double sumGamma;
    DoubleMatrix[] Phi;
    DoubleMatrix[] Theta;
    DoubleMatrix Pi;
    List<TreeSet<Integer>> sentiWordsList;
    IntegerMatrix[] matrixSWT;
    IntegerMatrix[] matrixSDT;
    IntegerMatrix matrixDS;
    int[][] sumSTW;
    int[][] sumDST;
    int[] sumDS;

    TwogramsCounter counter;

    public DoubleMatrix[] phi() {
      return Phi;
    }

    /**
     * Returns the per-topic word distributions (i.e., <code>phi</code>) indexed
     * by words instead of integers.
     * 
     * @return
     */
    public ObjectToDoubleMap<String>[][] phiIndexedByWord() {
      @SuppressWarnings("unchecked")
      ObjectToDoubleMap<String>[][] newPhi = new ObjectToDoubleMap[numSenti][numTopics];
      for (int senti = 0; senti < numSenti; senti++) {
        for (int topic = 0; topic < numTopics; topic++) {
          newPhi[senti][topic] = new ObjectToDoubleMap<String>();
          for (int idx = 0; idx < numUniqueWords; idx++) {
            newPhi[senti][topic].put(wordList.get(idx).getValue(),
                Phi[senti].getValue(idx, topic));
          }
        }
      }

      return newPhi;
    }

    /**
     * Returns top <code>numWords</code> words for each senti-aspect.
     * 
     * @param phi
     * @param numWords
     * @return
     */
    public String[][][] topWords(ObjectToDoubleMap<String>[][] phi, int numWords) {
      String[][][] topWords = new String[numSenti][numTopics][numWords];
      for (int senti = 0; senti < numSenti; senti++) {
        for (int topic = 0; topic < numTopics; topic++) {
          List<String> rankedList = phi[senti][topic].keysOrderedByValueList();
          for (int idx = 0; idx < numWords; idx++) {
            topWords[senti][topic][idx] = rankedList.get(idx);
          }
        }
      }

      return topWords;
    }

    /**
     * Classifies sentiment and topic of a segment.
     * 
     * @param segment
     *          a sequence of words
     * @return a 2-element array, the first is sentiment and the second is topic
     */
    public int[] classifySegment(ObjectToDoubleMap<String>[][] phi,
        String[] segment) {
      double maxProb = 0.0;
      int senti = -1, topic = -1;
      for (int j = 0; j < numSenti; j++) {
        for (int k = 0; k < numTopics; k++) {
          double prob = 1.0;
          for (String word : segment) {
            if (phi[j][k].containsKey(word)) {
              prob *= phi[j][k].getValue(word);
            }
          }
          if (prob != 1.0 && maxProb < prob) {
            maxProb = prob;
            senti = j;
            topic = k;
          }
        }
      }

      return new int[] { senti, topic };
    }

    /**
     * Returns the if <code>segment</code> contains at least one of the word in
     * <code>topWords</code>.
     * 
     * @param topWords
     * @param segment
     * @return
     */
    public boolean segmentContainsTopWords(String[] topWords, String[] segment) {
      for (String word : segment) {
        if (BSUtils.isInArray(topWords, word)) {
          return true;
        }
      }

      return false;
    }

    public int getNumUniqueWords() {
      return numUniqueWords;
    }

    public int getNumTopics() {
      return numTopics;
    }

    public int getNumSenti() {
      return numSenti;
    }

    public String getOutputDir() {
      return outputDir;
    }
  }

  private double[][] probTable;
  private JstModelData data;
  List<Document> documents;
  HashSet<String> sentiWordSet;

  public static void main(String[] args) throws Exception {
    int numTopics = 7;
    int numIterations = 2000;
    int numSenti = 2;
    int numThreads = 1;
    String dir = "C:/datasets/models/jst/coffeemaker";
    double alpha = 0.1;
    double[] betas = new double[] { 0.01, 0.01, 0.01 };
    double[] gammas = new double[] { 0.1, 0.1 };
    boolean randomInit = false;

    String utf8 = "utf-8";
    String stopFile = "StopStems_en.txt";
    String sentiStemsFile = "senti.txt";
    List<String> stopStems = TextFiles.readLines(dir + "/" + stopFile, utf8);
    HashSet<String> sentiStems = new HashSet<String>();
    // (HashSet<String>) TextFiles.readUniqueLines(dir + "/" + sentiStemsFile,
    // utf8);
    CorpusParser parser = new CorpusParser(dir + "/docs_en.txt", 4, 0, 70,
        sentiStems, stopStems);
    // parser.parse();

    String positiveSeeds = "C:/datasets/models/seedstems/seedstems0(+1).txt";
    String negativeSeeds = "C:/datasets/models/seedstems/seedstems1(+1).txt";
    String wordListFileName = "WordList.txt";
    String wordDocFileName = "BagOfSentences_en.txt";
    Vector<LocaleWord> wordList = Application.readWordList(dir + "/"
        + wordListFileName);
    Vector<Document> documents = new Vector<Document>();
    Application.readDocumentsForJst(documents, dir + "/" + wordDocFileName);
    ArrayList<TreeSet<LocaleWord>> list = new ArrayList<TreeSet<LocaleWord>>(
        numSenti);
    list.add(readWords(positiveSeeds, "utf-8", Locale.ENGLISH));
    list.add(readWords(negativeSeeds, "utf-8", Locale.ENGLISH));
    ArrayList<TreeSet<Integer>> sentiClasses = new ArrayList<TreeSet<Integer>>(
        list.size());
    for (Set<LocaleWord> sentiWordsStr : list) {
      TreeSet<Integer> sentiClass = new TreeSet<Integer>();
      for (LocaleWord word : sentiWordsStr) {
        sentiClass.add(wordList.indexOf(word));
      }
      sentiClasses.add(sentiClass);
    }

    // Print the configuration
    System.out.println("Documents: " + documents.size());
    System.out.println("Unique Words: " + wordList.size());
    System.out.println("Topics: " + numTopics);
    System.out.println("Alpha: " + alpha);
    System.out.println();
    System.out.println("Iterations: " + numIterations);
    System.out.println("Threads: " + numThreads);
    System.out.println("Input Dir: " + dir);
    System.out.println("Seed words: "
        + (sentiClasses.get(0).size() + sentiClasses.get(1).size()));
    String outputDir = dir
        + String.format("/T%d-G%.2f-%.2f(seed1)", numTopics, gammas[0],
            gammas[1]);
    new File(outputDir).mkdir();
    System.out.println("Output Dir: " + outputDir);
    JstModel model = new JstModel(parser.getTwogramsCounter(),
        parser.getSentiWordsSet(), numTopics, numSenti, wordList, documents,
        sentiClasses, alpha, betas, gammas, outputDir);
    model.init(randomInit);
    model.gibbsSampling(numIterations, numThreads);
    model.writeOutput(numIterations);
  }

  public JstModel(TwogramsCounter counter, HashSet<String> sentiWordSet,
      int numTopics, int numSenti, List<LocaleWord> wordList,
      List<Document> documents, List<TreeSet<Integer>> sentiWordsList,
      double alpha, double[] betas, double[] gammas, String outputDir) {
    data = new JstModelData();
    data.counter = counter;
    data.numTopics = numTopics;
    data.numSenti = numSenti;
    data.numUniqueWords = wordList.size();
    data.wordList = wordList;
    data.numDocuments = documents.size();
    data.sentiWordsList = sentiWordsList;
    data.alpha = alpha;
    data.betas = betas;
    data.gammas = gammas;
    data.sumBeta = new double[numSenti];
    data.outputDir = outputDir;
    this.sentiWordSet = sentiWordSet;
    this.documents = documents;
    probTable = new double[numTopics][numSenti];
  }

  /**
   * Initialization
   * 
   * @param randomInit
   */
  public void init(boolean randomInit) {
    data.sumSTW = new int[data.numSenti][data.numTopics];
    data.sumDST = new int[data.numDocuments][data.numSenti];
    data.sumDS = new int[data.numDocuments];

    data.matrixSWT = new IntegerMatrix[data.numSenti];
    for (int i = 0; i < data.numSenti; i++)
      data.matrixSWT[i] = new IntegerMatrix(data.numUniqueWords, data.numTopics);
    data.matrixSDT = new IntegerMatrix[data.numSenti];
    for (int i = 0; i < data.numSenti; i++)
      data.matrixSDT[i] = new IntegerMatrix(data.numDocuments, data.numTopics);
    data.matrixDS = new IntegerMatrix(data.numDocuments, data.numSenti);

    int numTooLongSentences = 0;

    for (Document currentDoc : documents) {
      int docNo = currentDoc.getDocNo();

      for (Sentence sentence : currentDoc.getSentences()) {
        int newSenti = -1;
        int numSentenceSenti = 0;
        for (SamplingWord sWord : sentence.getWords()) {
          SentiWord word = (SentiWord) sWord;

          int wordNo = word.getWordNo();
          for (int s = 0; s < data.sentiWordsList.size(); s++) {
            if (data.sentiWordsList.get(s).contains(wordNo)) {
              if (numSentenceSenti == 0 || s != newSenti)
                numSentenceSenti++;
              word.priorSentiment = s;
              newSenti = s;
            }
          }
        }
        if (randomInit || numSentenceSenti != 1)
          newSenti = (int) (Math.random() * data.numSenti);
        int newTopic = (int) (Math.random() * data.numTopics);

        if (sentence.getWords().size() > data.maxSentenceLength)
          numTooLongSentences++;

        if (!(numSentenceSenti > 1 || sentence.getWords().size() > data.maxSentenceLength)) {
          sentence.setTopic(newTopic);
          sentence.setSenti(newSenti);

          for (SamplingWord sWord : sentence.getWords()) {
            ((SentiWord) sWord).setSentiment(newSenti);
            sWord.setTopic(newTopic);
            data.matrixSWT[newSenti].incValue(sWord.getWordNo(), newTopic);
            data.sumSTW[newSenti][newTopic]++;
          }
          data.matrixSDT[newSenti].incValue(docNo, newTopic);
          data.matrixDS.incValue(docNo, newSenti);

          data.sumDST[docNo][newSenti]++;
          data.sumDS[docNo]++;
        }
      }
    }

    System.out.println("Too Long Sentences: " + numTooLongSentences);
  }

  public void gibbsSampling(int numIterations, int numThreads) throws Exception {
    data.sumAlpha = data.alpha * data.numTopics;
    int numSentiWords = 0;
    for (Set<Integer> sentiWords : data.sentiWordsList)
      numSentiWords += sentiWords.size();
    double sumBetaCommon = data.betas[0]
        * (data.numUniqueWords - numSentiWords);
    for (int s = 0; s < data.numSenti; s++) {
      int numLexiconWords = 0;
      if (data.sentiWordsList.size() > s)
        numLexiconWords = data.sentiWordsList.get(s).size();
      data.sumBeta[s] = sumBetaCommon + data.betas[1] * numLexiconWords
          + data.betas[2] * (numSentiWords - numLexiconWords);
    }
    data.sumGamma = 0;
    for (double gamma : data.gammas)
      data.sumGamma += gamma;

    System.out.println("Gibbs sampling started (Iterations: " + numIterations
        + ", Threads: " + numThreads + ")");

    long startTime = System.currentTimeMillis();
    for (int i = 0; i < numIterations; i++) {
      System.out.print(i + " ");
      if (i % 100 == 0) {
        System.out.println();
      }
      for (Document currentDoc : documents)
        sampleForDoc(currentDoc);

      data.numRealIterations = i + 1;
      if (data.outputInterval != null
          && data.numRealIterations % data.outputInterval == 0
          && data.numRealIterations < numIterations) {
        writeOutput(i + 1);
      }
    }
    System.out.printf("\nGibbs sampling terminated (%d).",
        (System.currentTimeMillis() - startTime) / 1000);
    data.Phi = STO2Util.calculatePhi(data.matrixSWT, data.sumSTW, data.betas,
        data.sumBeta, data.sentiWordsList);
    data.Theta = STO2Util.calculateTheta(data.matrixSDT, data.sumDST,
        data.alpha, data.sumAlpha);
    data.Pi = STO2Util.calculatePi(data.matrixDS, data.sumDS, data.gammas,
        data.sumGamma);
  }

  private void sampleForDoc(Document currentDoc) {
    int docNo = currentDoc.getDocNo();
    for (Sentence sentence : currentDoc.getSentences()) {
      if (sentence.getSenti() == -1
          || sentence.getWords().size() > data.maxSentenceLength)
        continue;
      double sumProb = 0;

      int oldTopic = sentence.getTopic();
      int oldSenti = sentence.getSenti();
      data.matrixSDT[oldSenti].decValue(docNo, oldTopic);
      data.matrixDS.decValue(docNo, oldSenti);
      data.sumDST[docNo][oldSenti]--;
      data.sumDS[docNo]--;
      SamplingWord sWord = sentence.getWord();
      data.matrixSWT[oldSenti].decValue(sWord.getWordNo(), oldTopic);
      data.sumSTW[oldSenti][oldTopic]--;
      // Sampling
      for (int si = 0; si < data.numSenti; si++) {
        for (int ti = 0; ti < data.numTopics; ti++) {
          // notice that sumBeta[si] is same for all topics (indeed, it should
          // have been written as sumBeta[si][ti]
          double beta0 = data.sumSTW[si][ti] + data.sumBeta[si];
          SentiWord word = (SentiWord) sWord;
          double beta = 0.01;
          double betaw = data.matrixSWT[si].getValue(word.getWordNo(), ti)
              + beta;
          double expectTSW = betaw / beta0;
          // Fast version
          probTable[ti][si] = (data.matrixSDT[si].getValue(docNo, ti) + data.alpha)
              / (data.sumDST[docNo][si] + data.sumAlpha)
              * (data.matrixDS.getValue(docNo, si) + data.gammas[si])
              * expectTSW;
          sumProb += probTable[ti][si];
        }
      }

      int newTopic = -1, newSenti = -1;
      double randNo = Math.random() * sumProb;
      double tmpSumProb = 0;
      boolean found = false;
      for (int ti = 0; ti < data.numTopics; ti++) {
        for (int si = 0; si < data.numSenti; si++) {
          tmpSumProb += probTable[ti][si];
          if (randNo < tmpSumProb) {
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
      SentiWord word = (SentiWord) sWord;
      word.setTopic(newTopic);
      word.setSentiment(newSenti);
      data.matrixSWT[newSenti].incValue(word.getWordNo(), newTopic);
      data.sumSTW[newSenti][newTopic]++;
      data.matrixSDT[newSenti].incValue(docNo, newTopic);
      data.matrixDS.incValue(docNo, newSenti);

      data.sumDST[docNo][newSenti]++;
      data.sumDS[docNo]++;
    }
  }

  void writeClassificationSummary(DoubleMatrix pi, String file)
      throws IOException {
    int numPosCorrect = 0, numNegCorrect = 0;
    int numPosWrong = 0, numNegWrong = 0;
    int numNotRated = 0, numNeutral = 0, numPos = 0, numNeg = 0;
    for (int i = 0; i < data.numDocuments; i++) {
      Document document = documents.get(i);
      double rating = document.getRating();
      if (rating != 3.0 && rating != -1.0) {
        int observedSenti = rating > 3.0 ? 0 : 1;
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

  private double computeCoherenceScoreWithProb(int[] cor, DoubleMatrix[] phi,
      int[][][] indice, int kSenti, int kAspect) {
    int nSenti = 25, nAspect = 50;
    double probSentiWord, probAspectWord;
    @SuppressWarnings("unchecked")
    ArrayList<Integer> sentiIdx[] = new ArrayList[2];
    sentiIdx[0] = new ArrayList<Integer>();
    sentiIdx[1] = new ArrayList<Integer>();
    HashMap<Integer, Double> aspectProb = new HashMap<Integer, Double>();
    // get top sentiment words for the topic kSenti
    for (int x : indice[0][kSenti]) {
      if (sentiWordSet.contains(data.wordList.get(x).getValue())) {
        sentiIdx[0].add(x);
      }
      if (sentiIdx[0].size() == nSenti) {
        break;
      }
    }
    for (int x : indice[1][cor[kSenti]]) {
      if (sentiWordSet.contains(data.wordList.get(x).getValue())) {
        sentiIdx[1].add(x);
      }
      if (sentiIdx[1].size() == nSenti) {
        break;
      }
    }
    // get top aspect words for the topic kAspect
    double maxAspectProbability = -10.0;
    for (int idx = 0; idx < data.numProbWords; idx++) {
      int x = indice[0][kAspect][idx];
      if (!sentiWordSet.contains(data.wordList.get(x).getValue())) {
        Double prob = aspectProb.get(x);
        if (prob == null) {
          prob = phi[0].getValue(x, kAspect);
        } else {
          prob = (prob + phi[0].getValue(x, kAspect)) / 2;
        }
        aspectProb.put(x, prob);
        if (maxAspectProbability < prob) {
          maxAspectProbability = prob;
        }
        if (aspectProb.size() == nAspect) {
          break;
        }
      }
      x = indice[1][cor[kAspect]][idx];
      if (!sentiWordSet.contains(data.wordList.get(x).getValue())) {
        Double prob = aspectProb.get(x);
        if (prob == null) {
          prob = phi[1].getValue(x, cor[kAspect]);
        } else {
          prob = (prob + phi[1].getValue(x, cor[kAspect])) / 2;
        }
        aspectProb.put(x, prob);
        if (maxAspectProbability < prob) {
          maxAspectProbability = prob;
        }
        if (aspectProb.size() == nAspect) {
          break;
        }
      }
    }

    // compute score
    double score = 0.0;
    for (Integer sentiWordIdx : sentiIdx[0]) {
      for (Integer aspectWordIdx : aspectProb.keySet()) {
        probSentiWord = (phi[0].getValue(sentiWordIdx, kSenti) / phi[0]
            .getValue(indice[0][kSenti][0], kSenti));
        probAspectWord = aspectProb.get(aspectWordIdx) / maxAspectProbability;
        score += data.counter.getCount(data.wordList.get(sentiWordIdx)
            .getValue(), data.wordList.get(aspectWordIdx).getValue())
            * probSentiWord * probAspectWord;
      }
    }
    for (Integer sentiWordIdx : sentiIdx[1]) {
      for (Integer aspectWordIdx : aspectProb.keySet()) {
        probSentiWord = (phi[1].getValue(sentiWordIdx, cor[kSenti]) / phi[1]
            .getValue(indice[1][cor[kSenti]][0], cor[kSenti]));
        probAspectWord = aspectProb.get(aspectWordIdx) / maxAspectProbability;
        score += data.counter.getCount(data.wordList.get(sentiWordIdx)
            .getValue(), data.wordList.get(aspectWordIdx).getValue())
            * probSentiWord * probAspectWord;
      }
    }

    return score;
  }

  public void writeOutput(int iter) throws Exception {
    data.Phi = STO2Util.calculatePhi(data.matrixSWT, data.sumSTW, data.betas,
        data.sumBeta, data.sentiWordsList);
    data.Theta = STO2Util.calculateTheta(data.matrixSDT, data.sumDST,
        data.alpha, data.sumAlpha);
    data.Pi = STO2Util.calculatePi(data.matrixDS, data.sumDS, data.gammas,
        data.sumGamma);
    String dir = data.outputDir + "/" + iter;
    new File(dir).mkdir();
    BSUtils.saveModel(dir + "/model.gz", data);
    PrintWriter out;
    writeClassificationSummary(data.Pi, dir + "/classification.txt");
    // String prefix = String.format("Jst-I%d-T%d-A%.2f-B%.4f,%.4f-G%.2f,%2.f",
    // numRealIterations, numTopics, alpha, betas[0], betas[1], gammas[0],
    // gammas[1]);

    // Phi
    // System.out.println("Writing Phi...");
    // PrintWriter out = new PrintWriter(new FileWriter(new File(dir + "/"
    // + prefix + "-Phi.csv")));
    // for (int s = 0; s < this.numSenti; s++)
    // for (int t = 0; t < this.numTopics; t++)
    // out.print(",S" + s + "-T" + t);
    // out.println();
    // for (int w = 0; w < this.wordList.size(); w++) {
    // out.print(this.wordList.get(w));
    // for (int s = 0; s < this.numSenti; s++) {
    // for (int t = 0; t < this.numTopics; t++) {
    // out.print("," + this.Phi[s].getValue(w, t));
    // }
    // }
    // out.println();
    // }
    // out.close();

    // Theta
    // System.out.println("Writing Theta...");
    // out = new PrintWriter(new FileWriter(new File(dir + "/" + prefix
    // + "-Theta.csv")));
    // for (int s = 0; s < this.numSenti; s++)
    // for (int t = 0; t < this.numTopics; t++)
    // out.print("S" + s + "-T" + t + ",");
    // out.println();
    // for (int d = 0; d < this.numDocuments; d++) {
    // for (int s = 0; s < this.numSenti; s++) {
    // for (int t = 0; t < this.numTopics; t++) {
    // out.print(this.Theta[s].getValue(d, t) + ",");
    // }
    // }
    // out.println();
    // }
    // out.close();

    // Most probable words
    System.out.println("Writing the most probable words...");
    String prefix = String.valueOf(iter);
    out = new PrintWriter(new FileWriter(new File(dir + "/" + prefix
        + "-ProbWords.csv")));
    for (int s = 0; s < data.numSenti; s++)
      for (int t = 0; t < data.numTopics; t++)
        out.print("S" + s + "-T" + t + ",");
    out.println();
    int[][][] wordIndices = new int[data.numSenti][data.numTopics][data.numProbWords];
    for (int s = 0; s < data.numSenti; s++) {
      for (int t = 0; t < data.numTopics; t++) {
        Vector<Integer> sortedIndexList = data.Phi[s].getSortedColIndex(t,
            data.numProbWords);
        for (int w = 0; w < sortedIndexList.size(); w++)
          wordIndices[s][t][w] = sortedIndexList.get(w);
      }
    }
    for (int w = 0; w < data.numProbWords; w++) {
      for (int s = 0; s < data.numSenti; s++) {
        for (int t = 0; t < data.numTopics; t++) {
          int index = wordIndices[s][t][w];
          out.printf("%s (%.3f),", data.wordList.get(index),
              data.Phi[s].getValue(index, t));
        }
      }
      out.println();
    }
    out.close();

    // Most probable words by term-score
    System.out.println("Writing the most probable words by termscores...");
    out = new PrintWriter(new FileWriter(new File(dir + "/" + prefix
        + "-ProbWordsByTermScore.csv")));
    for (int s = 0; s < data.numSenti; s++)
      for (int t = 0; t < data.numTopics; t++)
        out.print("S" + s + "-T" + t + ",");
    out.println();
    DoubleMatrix[] ts = buildTermScoreMatrix(data.Phi);
    for (int s = 0; s < data.numSenti; s++) {
      for (int t = 0; t < data.numTopics; t++) {
        Vector<Integer> sortedIndexList = ts[s].getSortedColIndex(t,
            data.numProbWords);
        for (int w = 0; w < sortedIndexList.size(); w++)
          wordIndices[s][t][w] = sortedIndexList.get(w);
      }
    }
    for (int w = 0; w < data.numProbWords; w++) {
      for (int s = 0; s < data.numSenti; s++) {
        for (int t = 0; t < data.numTopics; t++) {
          int index = wordIndices[s][t][w];
          out.print(data.wordList.get(index) + " ("
              + String.format("%.3f", ts[s].getValue(index, t)) + "),");
        }
      }
      out.println();
    }
    out.close();

    System.out.println("Writing coherence score");
    int[] correspondingTopic = getCorrespondingTopics();

    out = new PrintWriter(dir + "/coherence.csv");
    for (int kSenti = 0; kSenti < data.numTopics; kSenti++) {
      for (int kAspect = 0; kAspect < data.numTopics; kAspect++) {
        out.printf(
            "%.2f,",
            computeCoherenceScoreWithProb(correspondingTopic, ts, wordIndices,
                kSenti, kAspect));
      }
      out.println();
    }
    // self-score
    // the corresponding topic
    for (int k = 0; k < data.numTopics; k++) {
      out.printf("%d,", correspondingTopic[k]);
    }
    out.println();
    // and the score
    for (int k = 0; k < data.numTopics; k++) {
      out.printf(
          "%.2f,",
          computeCoherenceScoreWithProb(correspondingTopic, ts, wordIndices,
              correspondingTopic[k], k));
    }
    out.close();
    // System.out.println("Visualizing reviews...");
    // String[] sentiColors = { "green", "red", "black" };
    // out = new PrintWriter(new FileWriter(new File(dir + "/" + prefix
    // + "-VisReviews.html")));
    // for (Document doc : documents) {
    // out.println("<h3>Document " + doc.getDocNo() + "</h3>");
    // for (Sentence sentence : doc.getSentences()) {
    // if (sentence.getSenti() < 0 || sentence.getSenti() >= this.numSenti
    // || sentence.getWords().size() > this.maxSentenceLength)
    // continue;
    // out.print("<p style=\"color:" + sentiColors[sentence.getSenti()]
    // + ";\">T" + sentence.getTopic() + ":");
    // for (SamplingWord word : sentence.getWords())
    // out.print(" " + wordList.get(word.getWordNo()));
    // out.println("</p>");
    // }
    // }
    // out.close();
    // // Sentiment lexicon words distribution
    // System.out.println("Calculating sentiment lexicon words distributions...");
    // out = new PrintWriter(new FileWriter(new File(dir + "/" + prefix +
    // "-SentiLexiWords.csv")));
    // for (Set<Integer> sentiWords : this.sentiWordsList) {
    // for (int wordNo : sentiWords) {
    // if (wordNo < 0 || wordNo >= this.wordList.size())
    // continue;
    // out.print(this.wordList.get(wordNo));
    // for (int s = 0; s < numSenti; s++) {
    // int sum = 0;
    // for (int t = 0; t < numTopics; t++)
    // sum += matrixSWT[s].getValue(wordNo, t);
    // out.print(","+sum);
    // }
    // out.println();
    // }
    // out.println();
    // }
    // out.close();
  }

  /**
   * Gets the corresponding topic using cosine similarity
   * 
   * @return
   */
  private int[] getCorrespondingTopics() {
    int max[] = new int[data.numTopics];
    double[][] length = new double[data.numSenti][data.numTopics];
    for (int senti = 0; senti < 2; senti++) {
      for (int k = 0; k < data.numTopics; k++) {
        length[senti][k] = 0;
        for (int w = 0; w < data.numUniqueWords; w++) {
          length[senti][k] += data.Phi[senti].getValue(w, k)
              * data.Phi[senti].getValue(w, k);
        }
        length[senti][k] = Math.sqrt(length[senti][k]);
      }
    }
    for (int k = 0; k < data.numTopics; k++) {
      double cosine, maxCosine = -1.0;
      int kmax = -1;
      for (int h = 0; h < data.numTopics; h++) {
        cosine = 0.0; // cos(k, h)
        for (int w = 0; w < data.numUniqueWords; w++) {
          cosine += data.Phi[0].getValue(w, k) * data.Phi[1].getValue(w, h);
        }
        cosine /= (length[0][k] * length[1][h]);
        if (cosine > maxCosine) {
          maxCosine = cosine;
          kmax = h;
        }
      }
      max[k] = kmax;
    }

    return max;
  }

  /**
   * Builds the term-score matrix from the inferred values of Phi.
   * 
   * @return
   */
  private DoubleMatrix[] buildTermScoreMatrix(DoubleMatrix[] phi) {
    DoubleMatrix[] termScore = new DoubleMatrix[phi.length];
    double sumOfLogs[] = new double[data.numUniqueWords];
    // compute the sum of logs for each word
    for (int w = 0; w < data.numUniqueWords; w++) {
      sumOfLogs[w] = 0.0;
      for (int s = 0; s < data.numSenti; s++) {
        for (int t = 0; t < data.numTopics; t++) {
          sumOfLogs[w] += Math.log(phi[s].getValue(w, t));
        }
      }
    }
    double score, prob;
    // int topics = numTopics * numSenti;
    // TODO(trung): this is a different from the term-score formula (with the
    // assumption that a senti-word has only one senti -> only numTopics)
    int topics = data.numTopics;
    for (int s = 0; s < data.numSenti; s++) {
      termScore[s] = new DoubleMatrix(data.numUniqueWords, data.numTopics);
      for (int t = 0; t < data.numTopics; t++) {
        for (int w = 0; w < data.numUniqueWords; w++) {
          prob = phi[s].getValue(w, t);
          score = prob * (Math.log(prob) - sumOfLogs[w] / topics);
          termScore[s].setValue(w, t, score);
        }
      }
    }
    return termScore;
  }

  public static HashMap<String, Document> getAnnotatedDocuments(String file)
      throws IOException {
    HashMap<String, Document> map = new HashMap<String, Document>();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), "utf-8"));
    String line;
    while ((line = in.readLine()) != null) {
      String[] part = line.split(" ");
      Document doc = new Document(-1);
      doc.setExternalId(part[0]);
      int numSentences = Integer.parseInt(part[1]);
      for (int i = 0; i < numSentences; i++) {
        line = in.readLine();
        int pos = line.indexOf(",");
        Sentence sentence = new Sentence();
        sentence.setText(DocumentUtils.removeNonAlphabetSymbolsAndNegate(line
            .substring(pos + 1).trim().replaceAll(" ,", ",")));
        String sentiment = line.substring(0, pos);
        if (sentiment.equalsIgnoreCase("positive")) {
          sentence.setSenti(0);
        } else if (sentiment.equalsIgnoreCase("negative")) {
          sentence.setSenti(1);
        } else {
          sentence.setSenti(-1);
        }
        doc.addSentence(sentence);
      }
      map.put(doc.getExternalId(), doc);
    }
    in.close();

    return map;
  }

  static TreeSet<LocaleWord> readWords(String file, String charset,
      Locale locale) throws IOException {
    TreeSet<LocaleWord> words = new TreeSet<LocaleWord>();
    String line;
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), charset));
    while ((line = reader.readLine()) != null) {
      words.add(new LocaleWord(line, locale));
    }
    return words;
  }
}
