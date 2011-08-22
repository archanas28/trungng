package edu.kaist.uilab.bs.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import com.aliasi.symbol.SymbolTable;

import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.SentiWordNet;
import edu.kaist.uilab.asc.util.TextFiles;
import edu.kaist.uilab.bs.BSModel;
import edu.kaist.uilab.stemmers.EnglishStemmer;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Process the data given by Klenner and Fahrni.
 * 
 * @author trung
 */
public class NounphraseAnalysis {

  private static final String UTF8 = "utf-8";
  private static String model = "D:/java-libs/stanford-postagger-2010-05-26/models/left3words-wsj-0-18.tagger";
  private static String[] adjTags = { "JJ", "JJR", "JJS" };

  private static MaxentTagger tagger;
  private static EnglishStemmer stemmer = new EnglishStemmer();
  private static HashSet<String> positiveAdjs = new HashSet<String>();
  private static HashSet<String> negativeAdjs = new HashSet<String>();

  private static void handlePhrase(String phrase, HashSet<String> adjSet) {
    int cntAdj = 0;
    String taggedString = tagger.tagTokenizedString(phrase);
    StringTokenizer tokenizer = new StringTokenizer(taggedString);
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      for (String adjTag : adjTags) {
        if (token.contains(adjTag)) {
          int pos = token.indexOf("_");
          adjSet.add(stemmer.getStem(token.substring(0, pos)));
          cntAdj++;
          break;
        }
      }
    }
    if (cntAdj > 1) {
      System.out.println(phrase);
    }
  }

  @SuppressWarnings("unused")
  private static void writeData() throws IOException {
    System.out.println("Positive adjs: " + positiveAdjs.size());
    TextFiles.writeCollection(positiveAdjs, "C:/datasets/bs/pos.txt", "utf-8");
    System.out.println("Negative adjs: " + negativeAdjs.size());
    TextFiles.writeCollection(negativeAdjs, "C:/datasets/bs/neg.txt", "utf-8");
  }

  static void readData() throws IOException {
    List<String> lines = TextFiles.readLines("C:/datasets/bs/np-eval.data");
    String POS = "pos", NEG = "neg";
    for (String line : lines) {
      int pos = line.indexOf("[");
      String content = line.substring(pos + 1, line.length() - 1)
          .replaceAll(",", " ").toLowerCase();
      if (line.startsWith(POS)) {
        handlePhrase(content, positiveAdjs);
      } else if (line.startsWith(NEG)) {
        handlePhrase(content, negativeAdjs);
      }
    }
  }

  static void classifyWordSentiment() throws IOException {
    String dir = "C:/datasets/bs";
    for (String word : TextFiles.readUniqueLines(dir + "/pos.txt", UTF8)) {
      positiveAdjs.add(stemmer.getStem(word));
    }
    for (String word : TextFiles.readUniqueLines(dir + "/neg.txt", UTF8)) {
      negativeAdjs.add(stemmer.getStem(word));
    }
    HashSet<String> adjs = new HashSet<String>(positiveAdjs);
    adjs.addAll(negativeAdjs);
    BSModel model = BSModel.loadModel(dir
        + "/ursa/T50-A0.1-B0.0010-G0.10,0.10-I1000(newstop)/800/model.gz");
    int numWords = 100; // top words to take
    String[][][] sentiWords = model.getTopSentiWords(numWords);
    int numTopics = model.getNumTopics();
    // get all positive sentiment words
    @SuppressWarnings("unchecked")
    HashMap<String, Integer>[] map = new HashMap[2];
    for (int s = 0; s < model.getNumSentiments(); s++) {
      map[s] = new HashMap<String, Integer>();
      for (int t = 0; t < numTopics; t++) {
        for (int w = 0; w < numWords; w++) {
          String word = sentiWords[s][t][w];
          Integer cnt = map[s].get(word);
          if (cnt == null) {
            map[s].put(word, 1);
          } else {
            map[s].put(word, cnt + 1);
          }
        }
      }
    }
    int numCorrect = 0, numNotClassified = 0, numPolyByBS = 0, numPolyByNP = 0;
    // measure the accuracy
    for (String word : adjs) {
      // 0: pos, 1: neg, -1: not classified, 2: in both
      int classified = -1;
      Integer cntPos = map[0].get(word);
      Integer cntNeg = map[1].get(word);
      if (cntPos != null && cntNeg == null) {
        classified = 0;
      } else if (cntPos == null && cntNeg != null) {
        classified = 1;
      } else if (cntPos != null && cntNeg != null) {
        classified = cntPos > cntNeg ? 0 : 1;
        numPolyByBS++;
        System.out.printf("%s(%d, %d) ", word, cntPos, cntNeg);
      } else {
        classified = -1;
      }
      int annotated = -1;
      if (positiveAdjs.contains(word) && negativeAdjs.contains(word)) {
        numPolyByNP++;
      } else if (positiveAdjs.contains(word)) {
        annotated = 0;
      } else {
        annotated = 1;
      }
      if (classified == -1 || annotated == -1) {
        numNotClassified++;
      } else if (classified == annotated) {
        numCorrect++;
      }
    }
    System.out.printf(
        "\nNum per-topic top words used for classification: %d\n", numWords);
    System.out.printf("Total words in NP: %d (pos = %d, neg = %d)\n",
        adjs.size(), positiveAdjs.size(), negativeAdjs.size());
    System.out.printf("Annotated as both positive and negative: %d\n",
        numPolyByNP);
    System.out.printf("Classified as both positive and negative: %d\n",
        numPolyByBS);
    System.out.printf("Not classified by BS: %d\n", numNotClassified);
    System.out.printf("Classification accuracy: %.2f\n", ((double) numCorrect)
        / (adjs.size() - numNotClassified));
  }

  static void classifyWordSentiment2() throws IOException {
    String dir = "C:/datasets/bs";
    for (String word : TextFiles.readUniqueLines(dir + "/pos.txt", UTF8)) {
      positiveAdjs.add(stemmer.getStem(word));
    }
    for (String word : TextFiles.readUniqueLines(dir + "/neg.txt", UTF8)) {
      negativeAdjs.add(stemmer.getStem(word));
    }
    HashSet<String> adjs = new HashSet<String>(positiveAdjs);
    adjs.addAll(negativeAdjs);
    BSModel model = BSModel.loadModel(dir
        + "/ursa/T50-A0.1-B0.0010-G0.10,0.10-I1000(newstop)/800/model.gz");
    DoubleMatrix[] phi = model.getPhiSentiByTermscore();
    int numWords = 100; // top words to take
    int[][][] indice = model.getIndiceOfTopSentiWords(phi, numWords);
    SymbolTable table = model.getSentiTable();
    int numTopics = model.getNumTopics();
    // get all positive sentiment words
    @SuppressWarnings("unchecked")
    HashMap<String, Double>[] map = new HashMap[2];
    for (int s = 0; s < model.getNumSentiments(); s++) {
      map[s] = new HashMap<String, Double>();
      for (int t = 0; t < numTopics; t++) {
        double topWordProb = phi[s].getValue(indice[s][t][0], t);
        for (int w = 0; w < numWords; w++) {
          String word = table.idToSymbol(indice[s][t][w]);
          Double cnt = map[s].get(word);
          if (cnt == null) {
            cnt = 0.0;
          }
          double incValue = phi[s].getValue(indice[s][t][w], t) / topWordProb;
          map[s].put(word, cnt + incValue);
        }
      }
    }
    int numCorrect = 0, numNotClassified = 0, numPolyByBS = 0, numPolyByNP = 0;
    // measure the accuracy
    for (String word : adjs) {
      // 0: pos, 1: neg, -1: not classified, 2: in both
      int classified = -1;
      Double pos = map[0].get(word);
      Double neg = map[1].get(word);
      if (pos != null && neg == null) {
        classified = 0;
      } else if (pos == null && neg != null) {
        classified = 1;
      } else if (pos != null && neg != null) {
        classified = pos > neg ? 0 : 1;
        numPolyByBS++;
        System.out.printf("%s(%.2f, %.2f) ", word, pos, neg);
      } else {
        classified = -1;
      }
      int annotated = -1;
      if (positiveAdjs.contains(word) && negativeAdjs.contains(word)) {
        numPolyByNP++;
      } else if (positiveAdjs.contains(word)) {
        annotated = 0;
      } else {
        annotated = 1;
      }
      if (classified == -1 || annotated == -1) {
        numNotClassified++;
      } else if (classified == annotated) {
        numCorrect++;
      }
    }
    System.out.printf(
        "\nNum per-topic top words used for classification: %d\n", numWords);
    System.out.printf("Total words in NP: %d (pos = %d, neg = %d)\n",
        adjs.size(), positiveAdjs.size(), negativeAdjs.size());
    System.out.printf("Annotated as both positive and negative: %d\n",
        numPolyByNP);
    System.out.printf("Classified as both positive and negative: %d\n",
        numPolyByBS);
    System.out.printf("Not classified by BS: %d\n", numNotClassified);
    System.out.printf("Classification accuracy: %.2f\n", ((double) numCorrect)
        / (adjs.size() - numNotClassified));
  }

  static void classifyByWordnet(HashSet<String> phrases, String sentiClass,
      int senti) throws IOException {
    SentiWordNet wn = new SentiWordNet();
    int numCorrect = 0, numWrong = 0;
    int numNotClassified = 0;
    ArrayList<String> words;
    for (String phrase : phrases) {
      words = stemPhrase(phrase);
      double score = 0.0;
      boolean hasChanged = false;
      for (int i = 0; i < words.size(); i++) {
        Double wordscore = wn.getPolarity(words.get(i), "a");
        if (wordscore != null) {
          score += wordscore;
          hasChanged = true;
        }
      }
      if (hasChanged) {
        int classifiedSenti = -1;
        if (score > 0) {
          classifiedSenti = 0;
        } else if (score < 0) {
          classifiedSenti = 1;
        }
        if (classifiedSenti == senti) {
          numCorrect++;
        } else {
          numWrong++;
        }
      } else {
        System.err.println("not classified: " + phrase);
        numNotClassified++;
      }
    }
    System.out.printf("#%s phrases: %d\n", sentiClass, phrases.size());
    System.out.printf("#phrases not classified: %d\n", numNotClassified);
    System.out
        .printf("#numCorrect = %d, numWrong = %d\n", numCorrect, numWrong);
    System.out.printf("accuracy (%s): %.3f\n", sentiClass, (numCorrect + 0.0)
        / (phrases.size() - numNotClassified));
  }

  static void classifyPhraseSentiment() throws IOException {
    String dir = "C:/datasets/bs";
    HashSet<String> posPhrases = (HashSet<String>) TextFiles
        .readUniqueLinesAsLowerCase(dir + "/pos.data");
    HashSet<String> negPhrases = (HashSet<String>) TextFiles
        .readUniqueLinesAsLowerCase(dir + "/neg.data");
    BSModel model = BSModel.loadModel(dir
        + "/ursa/T50-A0.1-B0.0010-G0.10,0.10-I1000(top50)/1000/model.gz");
    System.err.println("\nClassification using model:");
    posPhrases.removeAll(classify(model, posPhrases, "positive", 0));
    negPhrases.removeAll(classify(model, negPhrases, "negative", 1));

    System.err.println("\nClassification using SentiWordNet: ");
    classifyByWordnet(posPhrases, "positive", 0);
    classifyByWordnet(negPhrases, "negative", 1);
  }

  static HashSet<String> classify(BSModel model, HashSet<String> phrases,
      String sentiClass, int senti) {
    int numWords = 100;
    System.out.println("Using " + numWords + " words");
    DoubleMatrix[] phiSenti = model.getPhiSentiByTermscore();
    int[][][] sentiIndice = model.getIndiceOfTopSentiWords(phiSenti, numWords);
    double[][] phiAspect = model.getPhiAspectByTermscore();
    int[][] aspectIndice = model.getIndiceOfTopAspectWords(phiAspect, numWords);
    SymbolTable sentiTable = model.getSentiTable();
    SymbolTable aspectTable = model.getAspectTable();
    int numCorrect = 0, numWrong = 0;
    int numNotClassified = 0;
    ArrayList<String> words;
    HashSet<String> notClassifiedPhrases = new HashSet<String>();
    for (String phrase : phrases) {
      words = stemPhrase(phrase);
      double[] score = new double[model.getNumSentiments()];
      // score = sumOfProb((senti, aspect|topic k)) for all topic k and every
      // possible pair contained in the original phrase
      for (int i = 0; i < words.size() - 1; i++) {
        for (int j = i + 1; j < words.size(); j++) {
          int sentiWord = sentiTable.symbolToID(words.get(i));
          int aspectWord = aspectTable.symbolToID(words.get(j));
          if (sentiWord >= 0 && aspectWord >= 0) {
            for (int k = 0; k < model.getNumTopics(); k++) {
              // method 2: use all words
              // for (int s = 0; s < model.numSenti; s++) {
              // score[s] += phiSenti[s].getValue(sentiWord, k)
              // * phiAspect[k][aspectWord];
              // }
              // method 1: use only top words
              if (isInArray(aspectIndice[k], aspectWord)) {
                for (int s = 0; s < model.getNumSentiments(); s++) {
                  if (isInArray(sentiIndice[s][k], sentiWord)) {
                    score[s] += phiSenti[s].getValue(sentiWord, k)
                        * phiAspect[k][aspectWord];
                  }
                }
              }
            }
          }
        }
      }
      // TODO(trung): what if the phrase is in the corpus but not in the
      // senti-aspect pair? this should not happen if we take all words
      if (score[0] > 0 || score[1] > 0) {
        int classifiedSenti = score[0] >= score[1] ? 0 : 1;
        if (classifiedSenti == senti) {
          numCorrect++;
        } else {
          numWrong++;
        }
      } else {
        numNotClassified++;
        notClassifiedPhrases.add(phrase);
      }
    }
    System.out.printf("#%s phrases: %d\n", sentiClass, phrases.size());
    System.out.printf("#phrases not classified: %d\n", numNotClassified);
    System.out
        .printf("#numCorrect = %d, numWrong = %d\n", numCorrect, numWrong);
    System.out.printf("accuracy (%s): %.3f\n", sentiClass, (numCorrect + 0.0)
        / (phrases.size() - numNotClassified));

    return notClassifiedPhrases;
  }

  /**
   * Returns true if <code>value</code> is in <code>array</code>.
   * 
   * @return
   */
  private static boolean isInArray(int[] array, int value) {
    for (int element : array) {
      if (element == value)
        return true;
    }

    return false;
  }

  /**
   * Returns the list of stemmed words from the given phrase <code>s</code>.
   * 
   * @param s
   * @return
   */
  static ArrayList<String> stemPhrase(String s) {
    StringTokenizer tokenizer = new StringTokenizer(s);
    ArrayList<String> list = new ArrayList<String>();
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      list.add(stemmer.getStem(token));
    }

    return list;
  }

  public static void main(String args[]) throws Exception {
    String dir = "C:/datasets/bs";
    // tagger = new MaxentTagger(model);
    classifyPhraseSentiment();
  }
}
