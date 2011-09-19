package edu.kaist.uilab.bs.evaluation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.aliasi.util.ObjectToCounterMap;
import com.aliasi.util.ObjectToDoubleMap;

import edu.kaist.uilab.bs.MaxentTaggerSingleton;
import edu.kaist.uilab.bs.SentimentPrior;
import edu.kaist.uilab.stemmers.EnglishStemmer;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Reference distributions for comparison against the distributions of BS model.
 * 
 * @author trung nguyen
 * 
 */
public class BSReferenceDistributions extends ReferenceDistributions {
  private MaxentTagger tagger;
  private EnglishStemmer stemmer;
  ObjectToCounterMap<String>[] aspectCnt;
  int[] sumAspectCnt;
  ObjectToDoubleMap<String>[] phiAspect;

  public BSReferenceDistributions(String annotatedFile, String stopFile)
      throws IOException {
    super(annotatedFile, stopFile);
    computePhiAspect(aspectCnt, sumAspectCnt);
  }

  @Override
  @SuppressWarnings("unchecked")
  void init() {
    super.init();
    aspectCnt = new ObjectToCounterMap[numAspects];
    for (int i = 0; i < numAspects; i++) {
      aspectCnt[i] = new ObjectToCounterMap<String>();
    }
    sumAspectCnt = new int[numAspects];
    phiAspect = new ObjectToDoubleMap[numAspects];
    tagger = MaxentTaggerSingleton.getInstance();
    stemmer = new EnglishStemmer();
  }

  /**
   * Computes the empirical distribution based on the counts of words in each
   * topic (aspect).
   * 
   * @param aspectCnt
   * @param sumAspectCnt
   */
  private void computePhiAspect(ObjectToCounterMap<String>[] aspectCnt,
      int[] sumAspectCnt) {
    for (int topic = 0; topic < numAspects; topic++) {
      phiAspect[topic] = new ObjectToDoubleMap<String>();
      ObjectToCounterMap<String> counter = aspectCnt[topic];
      for (String word : counter.keySet()) {
        phiAspect[topic].increment(word, ((double) counter.getCount(word))
            / sumAspectCnt[topic]);
      }
    }
  }

  /**
   * Returns the empirical distributions for all aspects.
   * 
   * @return
   */
  public ObjectToDoubleMap<String>[] getReferenceAspectDistributions() {
    return phiAspect;
  }

  /**
   * Updates the word count by adding count for words in the given sentence.
   * 
   * @param senti
   * @param sentenceTopics
   * @param sentence
   */
  @Override
  void updateWordCount(int senti, ArrayList<Integer> sentenceTopics,
      String sentence) {
    numSentences++;
    if (sentenceTopics.size() > 2) {
      sentencesWithMultipleAspects++;
    }
    char[] cs = sentence.toCharArray();
    HashSet<String> sentiStems = getLowercaseSentimentStems(tagger
        .tagString(sentence));
    for (int topic : sentenceTopics) {
      for (String token : tokenizer.tokenizer(cs, 0, cs.length)) {
        if (sentiStems.contains(token)) {
          cnt[senti][topic].increment(token);
          sumCnt[senti][topic]++;
        } else {
          aspectCnt[topic].increment(token);
          sumAspectCnt[topic]++;
        }
      }
    }
  }

  /**
   * Returns the lowercase sentiment stems in a tagged sentence.
   * 
   * @param taggedString
   * @return
   */
  HashSet<String> getLowercaseSentimentStems(String taggedString) {
    String[] tokens = taggedString.split(" ");
    HashSet<String> set = new HashSet<String>();
    for (int i = 0; i < tokens.length; i++) {
      String token = tokens[i];
      int slashPos = token.indexOf("/");
      String word = token.substring(0, slashPos);
      String tag = token.substring(slashPos + 1);
      String stem = stemmer.getStem(word.toLowerCase());
      if (SentimentPrior.isSentiTag(tag) || SentimentPrior.isSentiWord(stem)) {
        if (i > 0 && tokens[i - 1].contains("not/")) {
          set.add("not_" + stem);
        } else {
          set.add(stem);
        }
      }
    }

    return set;
  }

  /**
   * Writes the top words of each aspect to the specified <code>file</code>.
   * 
   * @param file
   * @param numTopWords
   * @throws IOException
   */
  public void writeTopAspectWords(String file, int numTopWords)
      throws IOException {
    PrintWriter out = new PrintWriter(file);
    @SuppressWarnings("unchecked")
    List<String>[] topWords = new List[numAspects];
    for (int aspect = 0; aspect < numAspects; aspect++) {
      out.printf("T%d,", aspect);
      topWords[aspect] = phiAspect[aspect].keysOrderedByValueList().subList(0,
          numTopWords);
    }
    out.println();
    for (int idx = 0; idx < numTopWords; idx++) {
      for (int aspect = 0; aspect < numAspects; aspect++) {
        String word = topWords[aspect].get(idx);
        out.printf("%s (%.3f),", word, phiAspect[aspect].get(word));
      }
      out.println();
    }
    out.close();
  }

  public static void main(String args[]) throws IOException {
    String annotatedFile = "C:/datasets/ManuallyAnnotated_Corpus.xml";
    String stopStem = "C:/datasets/stop.txt";
    @SuppressWarnings("unused")
    BSReferenceDistributions reference = new BSReferenceDistributions(
        annotatedFile, stopStem);
    
  }
}
