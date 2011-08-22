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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

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

public class AsumModel {
  private int numUniqueWords; // vocabulary size
  private int numTopics; // K
  private int numSenti; // S
  private int numRealIterations;
  private int numDocuments;
  private List<LocaleWord> wordList = null;
  private int numProbWords = 100;

  public String inputDir = null;
  public String outputDir = null;
  private Integer intvalTmpOutput = null;

  private double alpha;
  private double sumAlpha;
  // betas[3]: Common Words, Corresponding Lexicon, The other lexicons
  private double[] betas;
  private double[] sumBeta; // sumBeta[senti]
  private double[] gammas;
  private double sumGamma;

  public DoubleMatrix[] Phi; // Phi[senti][word][topic]
  public DoubleMatrix[] Theta; // Theta[senti][document][topic]
  public DoubleMatrix Pi;

  public List<TreeSet<Integer>> sentiWordsList;

  private IntegerMatrix[] matrixSWT;
  private IntegerMatrix[] matrixSDT;
  private IntegerMatrix matrixDS;

  private int[][] sumSTW; // sumSTW[S][T]
  private int[][] sumDST; // sumDST[D][S]
  private int[] sumDS; // sumDS[D]

  private double[][] probTable;

  private List<Document> documents;
  private HashSet<String> sentiWordSet;
  private TwogramsCounter counter;

  HashMap<String, Document> annotatedDocuments;
  final private int maxSentenceLength = 40;

  public static void main(String[] args) throws Exception {
    int numTopics = 2;
    int numIterations = 1000;
    int numSenti = 2;
    int numThreads = 1;
    String dir = "C:/datasets/models/asum/ursa";
    String dicDir = dir;
    double alpha = 0.1;
    double[] betas = new double[] { 0.001, 0.001, 0.000000001 };
    double[] gammas = new double[] { 0.1, 0.1 };
    boolean randomInit = false;

    String utf8 = "utf-8";
    String stopFile = "StopStems_en.txt";
    String sentiStemsFile = "senti.txt";
    List<String> stopStems = TextFiles.readLines(dir + "/" + stopFile, utf8);
    HashSet<String> sentiStems = (HashSet<String>) TextFiles.readUniqueLines(
        dir + "/" + sentiStemsFile, utf8);
    CorpusParser parser = new CorpusParser(dir + "/docs_en.txt", 4, 0, 70,
        sentiStems, stopStems);
    parser.parse();

    // String sentiFilePrefix = "SentiWords-";
    String sentiFilePrefix = "SentiStems2-";
    String wordListFileName = "WordList.txt";
    String wordDocFileName = "BagOfSentences_en.txt";
    Vector<LocaleWord> wordList = Application.readWordList(dir + "/"
        + wordListFileName);
    Vector<Document> documents = new Vector<Document>();
    Application.readDocuments(documents, dir + "/" + wordDocFileName);
    ArrayList<TreeSet<LocaleWord>> list = new ArrayList<TreeSet<LocaleWord>>(
        numSenti);
    for (int s = 0; s < numSenti; s++) {
      list.add(readWords(dir + "/" + sentiFilePrefix + s + ".txt", "utf-8",
          Locale.ENGLISH));
    }
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
    System.out.println("Dictionary Dir: " + dicDir);
    String outputDir = dir
        + String.format("/T%dG%.2f-%.2f", numTopics, gammas[0], gammas[1]);
    new File(outputDir).mkdir();
    System.out.println("Output Dir: " + outputDir);
    HashMap<String, Document> annotatedDocuments = null;
    annotatedDocuments = getAnnotatedDocuments("C:/datasets/bs/ursa/annotated.txt");
    AsumModel core = new AsumModel(parser.getTwogramsCounter(),
        parser.getSentiWordsSet(), annotatedDocuments, numTopics, numSenti,
        wordList, documents, sentiClasses, alpha, betas, gammas);
    core.setTmpOutputFiles(dir, outputDir, 500);
    core.initialization(randomInit);
    core.gibbsSampling(numIterations, numThreads);
    core.generateOutputFiles(outputDir, numIterations);
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
        sentence.setText(replacePatterns(line.substring(pos + 1).trim()
            .replaceAll(" ,", ",")));
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

  public static String replacePatterns(String sentence) {
    ArrayList<String[]> list = new ArrayList<String[]>();
    list.add(new String[] { "[http|ftp]://[\\S]*", " " });
    list.add(new String[] {
        "(not|n't|without|never)[\\s]+(very|so|too|much|"
            + "quite|even|that|as|as much|a|the|to|really|been)[\\s]+", " not_" });
    list.add(new String[] { "(not|n't|without|never|no)[\\s]+", " not_" });
    list.add(new String[] { "[()<>\\[\\],~&;:\"\\-/=*#@^+'`’]", " " });
    list.add(new String[] { "[\\s]+", " " });
    for (String[] rp : list) {
      if (sentence != null) {
        sentence = Pattern
            .compile(rp[0], Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
            .matcher(sentence).replaceAll(rp[1]);
      }
    }

    return sentence;
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

  public AsumModel(TwogramsCounter counter, HashSet<String> sentiWordSet,
      HashMap<String, Document> annotatedDocuments, int numTopics,
      int numSenti, List<LocaleWord> wordList, List<Document> documents,
      List<TreeSet<Integer>> sentiWordsList, double alpha, double[] betas,
      double[] gammas) {
    this.counter = counter;
    this.sentiWordSet = sentiWordSet;
    this.annotatedDocuments = annotatedDocuments;
    this.numTopics = numTopics;
    this.numSenti = numSenti;
    this.numUniqueWords = wordList.size();
    this.numDocuments = documents.size();
    this.documents = documents;
    this.wordList = wordList;
    this.sentiWordsList = sentiWordsList;
    this.alpha = alpha;
    this.betas = betas;
    this.gammas = gammas;
    this.sumBeta = new double[numSenti];
    probTable = new double[numTopics][numSenti];
  }

  public void initialization(boolean randomInit) {
    sumSTW = new int[numSenti][numTopics];
    sumDST = new int[numDocuments][numSenti];
    sumDS = new int[numDocuments];

    matrixSWT = new IntegerMatrix[numSenti];
    for (int i = 0; i < numSenti; i++)
      matrixSWT[i] = new IntegerMatrix(numUniqueWords, numTopics);
    matrixSDT = new IntegerMatrix[numSenti];
    for (int i = 0; i < numSenti; i++)
      matrixSDT[i] = new IntegerMatrix(numDocuments, numTopics);
    matrixDS = new IntegerMatrix(numDocuments, numSenti);

    int numTooLongSentences = 0;

    for (Document currentDoc : documents) {
      int docNo = currentDoc.getDocNo();

      for (Sentence sentence : currentDoc.getSentences()) {
        int newSenti = -1;
        int numSentenceSenti = 0;
        for (SamplingWord sWord : sentence.getWords()) {
          SentiWord word = (SentiWord) sWord;

          int wordNo = word.getWordNo();
          for (int s = 0; s < sentiWordsList.size(); s++) {
            if (sentiWordsList.get(s).contains(wordNo)) {
              if (numSentenceSenti == 0 || s != newSenti)
                numSentenceSenti++;
              word.priorSentiment = s;
              newSenti = s;
            }
          }
        }
        if (randomInit || numSentenceSenti != 1)
          newSenti = (int) (Math.random() * numSenti);
        int newTopic = (int) (Math.random() * numTopics);

        if (sentence.getWords().size() > this.maxSentenceLength)
          numTooLongSentences++;

        if (!(numSentenceSenti > 1 || sentence.getWords().size() > this.maxSentenceLength)) {
          sentence.setTopic(newTopic);
          sentence.setSenti(newSenti);

          for (SamplingWord sWord : sentence.getWords()) {
            ((SentiWord) sWord).setSentiment(newSenti);
            sWord.setTopic(newTopic);
            matrixSWT[newSenti].incValue(sWord.getWordNo(), newTopic);
            sumSTW[newSenti][newTopic]++;
          }
          matrixSDT[newSenti].incValue(docNo, newTopic);
          matrixDS.incValue(docNo, newSenti);

          sumDST[docNo][newSenti]++;
          sumDS[docNo]++;
        }
      }
    }

    System.out.println("Too Long Sentences: " + numTooLongSentences);
  }

  public void gibbsSampling(int numIterations, int numThreads) throws Exception {
    this.sumAlpha = this.alpha * this.numTopics;
    int numSentiWords = 0;
    for (Set<Integer> sentiWords : sentiWordsList)
      numSentiWords += sentiWords.size();
    double sumBetaCommon = this.betas[0]
        * (this.numUniqueWords - numSentiWords);
    for (int s = 0; s < numSenti; s++) {
      int numLexiconWords = 0;
      if (this.sentiWordsList.size() > s)
        numLexiconWords = this.sentiWordsList.get(s).size();
      this.sumBeta[s] = sumBetaCommon + this.betas[1] * numLexiconWords
          + this.betas[2] * (numSentiWords - numLexiconWords);
    }
    this.sumGamma = 0;
    for (double gamma : this.gammas)
      this.sumGamma += gamma;

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

      this.numRealIterations = i + 1;
      if (this.intvalTmpOutput != null
          && this.numRealIterations % this.intvalTmpOutput == 0
          && this.numRealIterations < numIterations) {
        this.Phi = STO2Util.calculatePhi(matrixSWT, sumSTW, this.betas,
            this.sumBeta, this.sentiWordsList);
        this.Theta = STO2Util.calculateTheta(matrixSDT, sumDST, this.alpha,
            this.sumAlpha);
        this.Pi = STO2Util.calculatePi(matrixDS, sumDS, this.gammas,
            this.sumGamma);
        generateOutputFiles(outputDir, i + 1);
      }
    }
    System.out.printf("\nGibbs sampling terminated (%d).",
        (System.currentTimeMillis() - startTime) / 1000);
    this.Phi = STO2Util.calculatePhi(matrixSWT, sumSTW, this.betas,
        this.sumBeta, this.sentiWordsList);
    this.Theta = STO2Util.calculateTheta(matrixSDT, sumDST, this.alpha,
        this.sumAlpha);
    this.Pi = STO2Util.calculatePi(matrixDS, sumDS, this.gammas, this.sumGamma);
  }

  private void sampleForDoc(Document currentDoc) {
    int docNo = currentDoc.getDocNo();
    for (Sentence sentence : currentDoc.getSentences()) {
      if (sentence.getSenti() == -1) {
        continue;
      }

      Map<SamplingWord, Integer> wordCnt = sentence.getWordCnt();

      double sumProb = 0;

      int oldTopic = sentence.getTopic();
      int oldSenti = sentence.getSenti();

      matrixSDT[oldSenti].decValue(docNo, oldTopic);
      matrixDS.decValue(docNo, oldSenti);

      sumDST[docNo][oldSenti]--;
      sumDS[docNo]--;

      for (SamplingWord sWord : sentence.getWords()) {
        matrixSWT[oldSenti].decValue(sWord.getWordNo(), oldTopic);
        sumSTW[oldSenti][oldTopic]--;
      }

      // Sampling
      for (int si = 0; si < numSenti; si++) {
        boolean trim = false;

        // Fast Trimming
        for (SamplingWord sWord : wordCnt.keySet()) {
          SentiWord word = (SentiWord) sWord;
          if (word.priorSentiment != null && word.priorSentiment != si) {
            trim = true;
            break;
          }
        }
        if (trim) {
          for (int ti = 0; ti < numTopics; ti++)
            probTable[ti][si] = 0;
        } else {
          for (int ti = 0; ti < numTopics; ti++) {
            // notice that sumBeta[si] is same for all topics (indeed, it should
            // have
            // been written as sumBeta[si][ti]
            double beta0 = sumSTW[si][ti] + sumBeta[si];
            int m0 = 0;
            double expectTSW = 1;

            for (SamplingWord sWord : wordCnt.keySet()) {
              SentiWord word = (SentiWord) sWord;

              double beta;
              if (word.priorSentiment == null)
                beta = this.betas[0];
              else if (word.priorSentiment == si)
                beta = this.betas[1];
              else
                beta = this.betas[2];

              double betaw = matrixSWT[si].getValue(word.getWordNo(), ti)
                  + beta;

              int cnt = wordCnt.get(word);
              for (int m = 0; m < cnt; m++) {
                expectTSW *= (betaw + m) / (beta0 + m0);
                m0++;
              }

              // if (word.lexicon != null && word.lexicon != si && expectTSW >
              // 0) {
              // System.err.println(this.wordList.get(word.wordNo)+": "+ti+", "+si+", "+matrixTWS[ti].getValue(word.wordNo,
              // si)+", "+beta);
              // }
            }
            // probTable[ti][si] = (matrixSDT[si].getValue(docNo, ti) +
            // this.alpha) / (sumDST[docNo][si] + this.sumAlpha)
            // * (matrixDS.getValue(docNo, si) + this.gammas[si]) /
            // (sumDS[docNo] + this.sumGamma)
            // * expectTSW;
            // Fast version
            probTable[ti][si] = (matrixSDT[si].getValue(docNo, ti) + this.alpha)
                / (sumDST[docNo][si] + this.sumAlpha)
                * (matrixDS.getValue(docNo, si) + this.gammas[si]) * expectTSW;

            sumProb += probTable[ti][si];
          }
        }
      }

      int newTopic = -1, newSenti = -1;
      double randNo = Math.random() * sumProb;
      double tmpSumProb = 0;
      boolean found = false;
      for (int ti = 0; ti < numTopics; ti++) {
        for (int si = 0; si < numSenti; si++) {
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

      for (SamplingWord sWord : sentence.getWords()) {
        SentiWord word = (SentiWord) sWord;
        word.setTopic(newTopic);
        word.setSentiment(newSenti);
        matrixSWT[newSenti].incValue(word.getWordNo(), newTopic);
        sumSTW[newSenti][newTopic]++;
      }
      matrixSDT[newSenti].incValue(docNo, newTopic);
      matrixDS.incValue(docNo, newSenti);

      sumDST[docNo][newSenti]++;
      sumDS[docNo]++;
    }
  }

  public void setTmpOutputFiles(String inputDir, String outputDir, int interval)
      throws Exception {
    if (inputDir == null || outputDir == null)
      throw new Exception(
          "Should specify the input and output dirs for tmp output files");
    if (interval <= 0)
      throw new Exception(
          "The interval of writing tmp output files should be greater than 0");
    this.inputDir = inputDir;
    this.outputDir = outputDir;
    this.intvalTmpOutput = interval;
  }

  void writeClassificationSummary(DoubleMatrix pi, String file)
      throws IOException {
    int numPosCorrect = 0, numNegCorrect = 0;
    int numPosWrong = 0, numNegWrong = 0;
    int numNotRated = 0, numNeutral = 0, numPos = 0, numNeg = 0;
    for (int i = 0; i < numDocuments; i++) {
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

  private void writeSentenceClassificationSummary(String file)
      throws IOException {
    int[] sentimentClass = countSentimentClasses(annotatedDocuments);
    int numPosCorrect = 0, numPosWrong = 0;
    int numNegCorrect = 0, numNegWrong = 0;
    int numSentencesNotMatched = 0;
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (Document document : documents) {
      if (annotatedDocuments.containsKey(document.getExternalId())) {
        Document annotatedDoc = annotatedDocuments
            .get(document.getExternalId());
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

  public void generateOutputFiles(String dir, int iter) throws Exception {
    PrintWriter out;
    String prefix = String.valueOf(iter);
    if (annotatedDocuments != null) {
      writeSentenceClassificationSummary(dir + "/" + iter
          + "sentenceClassification.txt");
    }
    writeClassificationSummary(Pi, dir + "/" + iter + "classification.txt");
    // String prefix = "STO2-T" + numTopics + "-S" + numSenti + "("
    // + sentiWordsList.size() + ")-A" + alpha + "-B" + betas[0];
    // for (int i = 1; i < betas.length; i++)
    // prefix += "," + betas[i];
    // prefix += "-G" + gammas[0];
    // for (int i = 1; i < numSenti; i++)
    // prefix += "," + gammas[i];
    // prefix += "-I" + numRealIterations;
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
    // System.out.println("Writing the most probable words...");
    // out = new PrintWriter(new FileWriter(new File(dir + "/" + prefix
    // + "-ProbWords.csv")));
    // for (int s = 0; s < this.numSenti; s++)
    // for (int t = 0; t < this.numTopics; t++)
    // out.print("S" + s + "-T" + t + ",");
    // out.println();
    // int[][][] wordIndices = new
    // int[this.numSenti][this.numTopics][this.numProbWords];
    // for (int s = 0; s < this.numSenti; s++) {
    // for (int t = 0; t < this.numTopics; t++) {
    // Vector<Integer> sortedIndexList = Phi[s].getSortedColIndex(t,
    // this.numProbWords);
    // for (int w = 0; w < sortedIndexList.size(); w++)
    // wordIndices[s][t][w] = sortedIndexList.get(w);
    // }
    // }
    // for (int w = 0; w < this.numProbWords; w++) {
    // for (int s = 0; s < this.numSenti; s++) {
    // for (int t = 0; t < this.numTopics; t++) {
    // int index = wordIndices[s][t][w];
    // out.print(this.wordList.get(index) + " ("
    // + String.format("%.3f", Phi[s].getValue(index, t)) + "),");
    // }
    // }
    // out.println();
    // }
    // out.close();

    // Most probable words by term-score
    int[][][] wordIndices = new int[this.numSenti][this.numTopics][this.numProbWords];
    System.out.println("Writing the most probable words by termscores...");
    out = new PrintWriter(new FileWriter(new File(dir + "/" + prefix
        + "-ProbWordsByTermScore.csv")));
    for (int s = 0; s < this.numSenti; s++)
      for (int t = 0; t < this.numTopics; t++)
        out.print("S" + s + "-T" + t + ",");
    out.println();
    DoubleMatrix[] ts = buildTermScoreMatrix(Phi);
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        Vector<Integer> sortedIndexList = ts[s].getSortedColIndex(t,
            numProbWords);
        for (int w = 0; w < sortedIndexList.size(); w++)
          wordIndices[s][t][w] = sortedIndexList.get(w);
      }
    }
    for (int w = 0; w < this.numProbWords; w++) {
      for (int s = 0; s < this.numSenti; s++) {
        for (int t = 0; t < this.numTopics; t++) {
          int index = wordIndices[s][t][w];
          out.print(this.wordList.get(index) + " ("
              + String.format("%.3f", ts[s].getValue(index, t)) + "),");
        }
      }
      out.println();
    }
    out.close();

    System.out.println("Writing coherence score");
    int[] correspondingTopic = getCorrespondingTopics();
    out = new PrintWriter(dir + "/" + iter + "coherence.csv");
    for (int kSenti = 0; kSenti < numTopics; kSenti++) {
      for (int kAspect = 0; kAspect < numTopics; kAspect++) {
        out.printf(
            "%.2f,",
            computeCoherenceScoreWithProb(correspondingTopic, ts, wordIndices,
                kSenti, kAspect));
      }
      out.println();
    }

    // self-score
    // the corresponding topic
    for (int k = 0; k < numTopics; k++) {
      out.printf("%d,", correspondingTopic[k]);
    }
    out.println();
    // and the score
    for (int k = 0; k < numTopics; k++) {
      out.printf(
          "%.2f,",
          computeCoherenceScoreWithProb(correspondingTopic, ts, wordIndices,
              correspondingTopic[k], k));
    }
    out.close();

    //
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
    // // Sentence probabilities
    // System.out.println("Calculating sentence probabilities...");
    // out = new PrintWriter(new FileWriter(new File(dir + "/" + prefix +
    // "-SentenceProb.csv")));
    // out.print("Document,Sentence,Length");
    // for (int s = 0; s < this.numSenti; s++)
    // for (int t = 0; t < this.numTopics; t++)
    // out.print(",S"+s+"-T"+t);
    // out.println();
    // for (int d = 0; d < this.documents.size(); d++) {
    // OrderedDocument doc = this.documents.get(d);
    // for (int sen = 0; sen < doc.getSentences().size(); sen++) {
    // Sentence sentence = doc.getSentences().get(sen);
    // if (sentence.numSenti > 1 || sentence.getWords().size() > 50)
    // continue;
    // if (sentence.getWords().size() == 0) throw new Exception("WHAT???");
    // out.print(d+",\"");
    // for (Word word : sentence.getWords())
    // out.print(this.wordList.get(word.wordNo)+" ");
    // out.print("\","+sentence.getWords().size());
    // double [][] prod = new double[this.numSenti][this.numTopics];
    // double sum = 0;
    // for (int s = 0; s < this.numSenti; s++) {
    // for (int t = 0; t < this.numTopics; t++) {
    // prod[s][t] = 1;
    // for (Word word : sentence.getWords())
    // prod[s][t] *= this.Phi[s].getValue(word.wordNo, t);
    // sum += prod[s][t];
    // }
    // }
    // for (int s = 0; s < this.numSenti; s++) {
    // for (int t = 0; t < this.numTopics; t++) {
    // out.print("," + (prod[s][t] / sum));
    // }
    // }
    // out.println();
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
    double[][] length = new double[numSenti][numTopics];
    for (int senti = 0; senti < 2; senti++) {
      for (int k = 0; k < numTopics; k++) {
        length[senti][k] = 0;
        for (int w = 0; w < numUniqueWords; w++) {
          length[senti][k] += Phi[senti].getValue(w, k)
              * Phi[senti].getValue(w, k);
        }
        length[senti][k] = Math.sqrt(length[senti][k]);
      }
    }

    int max[] = new int[numTopics];
    for (int k = 0; k < numTopics; k++) {
      double cosine, maxCosine = -1.0;
      int kmax = -1;
      for (int h = 0; h < numTopics; h++) {
        cosine = 0.0; // cos(k, h)
        for (int w = 0; w < numUniqueWords; w++) {
          cosine += Phi[0].getValue(w, k) * Phi[1].getValue(w, h);
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
      if (sentiWordSet.contains(wordList.get(x).getValue())) {
        sentiIdx[0].add(x);
      }
      if (sentiIdx[0].size() == nSenti) {
        break;
      }
    }
    for (int x : indice[1][cor[kSenti]]) {
      if (sentiWordSet.contains(wordList.get(x).getValue())) {
        sentiIdx[1].add(x);
      }
      if (sentiIdx[1].size() == nSenti) {
        break;
      }
    }
    // get top aspect words for the topic kAspect
    double maxAspectProbability = -10.0;
    for (int idx = 0; idx < numProbWords; idx++) {
      int x = indice[0][kAspect][idx];
      if (!sentiWordSet.contains(wordList.get(x).getValue())) {
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
      if (!sentiWordSet.contains(wordList.get(x).getValue())) {
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
        score += counter.getCount(wordList.get(sentiWordIdx).getValue(),
            wordList.get(aspectWordIdx).getValue())
            * probSentiWord
            * probAspectWord;
      }
    }
    for (Integer sentiWordIdx : sentiIdx[1]) {
      for (Integer aspectWordIdx : aspectProb.keySet()) {
        probSentiWord = (phi[1].getValue(sentiWordIdx, cor[kSenti]) / phi[1]
            .getValue(indice[1][cor[kSenti]][0], cor[kSenti]));
        probAspectWord = aspectProb.get(aspectWordIdx) / maxAspectProbability;
        score += counter.getCount(wordList.get(sentiWordIdx).getValue(),
            wordList.get(aspectWordIdx).getValue())
            * probSentiWord
            * probAspectWord;
      }
    }

    return score;
  }

  /**
   * Builds the term-score matrix from the inferred values of Phi.
   * 
   * @return
   */
  private DoubleMatrix[] buildTermScoreMatrix(DoubleMatrix[] phi) {
    DoubleMatrix[] termScore = new DoubleMatrix[phi.length];
    double sumOfLogs[] = new double[numUniqueWords];
    // compute the sum of logs for each word
    for (int w = 0; w < numUniqueWords; w++) {
      sumOfLogs[w] = 0.0;
      for (int s = 0; s < numSenti; s++) {
        for (int t = 0; t < numTopics; t++) {
          sumOfLogs[w] += Math.log(phi[s].getValue(w, t));
        }
      }
    }
    double score, prob;
    // int topics = numTopics * numSenti;
    // TODO(trung): this is a different from the term-score formula (with the
    // assumption that a senti-word has only one senti -> only numTopics)
    int topics = numTopics;
    for (int s = 0; s < numSenti; s++) {
      termScore[s] = new DoubleMatrix(numUniqueWords, numTopics);
      for (int t = 0; t < numTopics; t++) {
        for (int w = 0; w < numUniqueWords; w++) {
          prob = phi[s].getValue(w, t);
          score = prob * (Math.log(prob) - sumOfLogs[w] / topics);
          termScore[s].setValue(w, t, score);
        }
      }
    }
    return termScore;
  }

}
