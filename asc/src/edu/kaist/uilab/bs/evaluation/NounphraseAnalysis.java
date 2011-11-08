package edu.kaist.uilab.bs.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import com.aliasi.symbol.SymbolTable;

import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.SentiWordNet;
import edu.kaist.uilab.asc.util.TextFiles;
import edu.kaist.uilab.bs.Model;
import edu.kaist.uilab.bs.opt.OptimizationModel;
import edu.kaist.uilab.stemmers.EnglishStemmer;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Process the data given by Klenner and Fahrni.
 * 
 * @author trung
 */
public class NounphraseAnalysis {

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
        if (score >= 0) {
          classifiedSenti = 0;
        } else if (score < -0) {
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
    String dir = "C:/datasets/models/bs";
    HashSet<String> posPhrases = (HashSet<String>) TextFiles
        .readUniqueLinesAsLowerCase(dir + "/pos.data");
    HashSet<String> negPhrases = (HashSet<String>) TextFiles
        .readUniqueLinesAsLowerCase(dir + "/neg.data");
    Model model = Model
        .loadModel(dir
            + "/ursa/optimization/T7-A0.1-B0.0100-G0.10,0.10-I1000()/2000/model.gz");
    System.err.println("\nClassification using model:");
//    posPhrases.removeAll(classify(model, posPhrases, "positive", 0));
//    negPhrases.removeAll(classify(model, negPhrases, "negative", 1));
    posPhrases
        .removeAll(classifyWithoutModel(model, posPhrases, "positive", 0));
    negPhrases
        .removeAll(classifyWithoutModel(model, negPhrases, "negative", 1));

    System.err.println("\nClassification using SentiWordNet: ");
    classifyByWordnet(posPhrases, "positive", 0);
    classifyByWordnet(negPhrases, "negative", 1);
  }

  static HashSet<String> classify(Model model, HashSet<String> phrases,
      String sentiClass, int senti) {
    DoubleMatrix[] phiSenti = model.getPhiSentiByTermscore();
    double[][] phiAspect = model.getPhiAspectByTermscore();
    SymbolTable sentiTable = model.getSentiTable();
    SymbolTable aspectTable = model.getAspectTable();
    int numCorrect = 0, numWrong = 0;
    int numNotClassified = 0;
    ArrayList<String> words;
    HashSet<String> notClassifiedPhrases = new HashSet<String>();
    for (String phrase : phrases) {
      words = stemPhrase(phrase);
      double[] score = new double[model.getNumSentiments()];
      for (int s = 0; s < model.getNumSentiments(); s++) {
        score[s] = 0;
        for (int k = 0; k < model.getNumTopics(); k++) {
          double prob = 1.0;
          for (int i = 0; i < words.size() - 1; i++) {
            int sentiWord = sentiTable.symbolToID(words.get(i));
            if (sentiWord >= 0) {
              prob *= phiSenti[s].getValue(sentiWord, k);
            }
          }
          score[s] += prob;
        }
      }
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

  static HashSet<String> classifyWithoutModel(Model model,
      HashSet<String> phrases, String sentiClass, int senti) {
    SymbolTable sentiTable = model.getSentiTable();
    OptimizationModel m = (OptimizationModel) model;
    double[][] ySentiment = m.getYSentiment();
    int numCorrect = 0, numWrong = 0;
    int numNotClassified = 0;
    ArrayList<String> words;
    HashSet<String> notClassifiedPhrases = new HashSet<String>();
    for (String phrase : phrases) {
      words = stemPhrase(phrase);
      double score = 0.0;
      for (int k = 0; k < model.getNumTopics(); k++) {
        for (int i = 0; i < words.size() - 1; i++) {
          int sentiWord = sentiTable.symbolToID(words.get(i));
          if (sentiWord >= 0) {
            score += ySentiment[0][sentiWord] - ySentiment[1][sentiWord];
          }
        }
      }
      double thres = 0.2;
      if (score >= thres || score <= -thres) {
        int classifiedSenti = score >= thres ? 0 : 1;
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
    // tagger = new MaxentTagger(model);
    classifyPhraseSentiment();
  }
}
