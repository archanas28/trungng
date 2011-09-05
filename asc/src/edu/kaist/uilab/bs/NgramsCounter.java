package edu.kaist.uilab.bs;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Counter for n-grams of a corpus.
 * <p>
 * This counter is case-insensitive.
 * 
 * @author trung
 */
public class NgramsCounter {
  private static final String SENTENCE_DELIMITER = "[?!.]";
  private HashMap<String, Integer> mCounter;
  int ngrams;

  /**
   * Constructor
   * 
   * @param ngrams
   *          the number of words to use as grams
   */
  public NgramsCounter(int ngrams) {
    mCounter = new HashMap<String, Integer>();
    this.ngrams = ngrams;
  }

  /**
   * Increments count of n-grams for the given document. The document is split
   * into sentences using <code>sentenceDelimiter</code>.
   * 
   * @param document
   *          document content
   * @param sentenceDilimiterExp
   *          regular expression to split <code>document</code> into sentences;
   *          use <code>null</code> to use the default delimiter expression
   *          [?!.].
   */
  public void incrementCount(String document, String sentenceDelimiterExp) {
    if (sentenceDelimiterExp == null) {
      sentenceDelimiterExp = SENTENCE_DELIMITER;
    }
    String[] sentences = document.split(sentenceDelimiterExp);
    for (String sentence : sentences) {
      incrementCountForSentence(sentence);
    }
  }

  /**
   * Increments count of n-grams for the given string <code>str</code>, which is
   * assumed to be a sentence.
   * 
   * @param str
   *          a string
   */
  public void incrementCount(String str) {
    incrementCountForSentence(str);
  }

  /**
   * Prints the counter of all n-grams to a file.
   * 
   * @param file
   */
  public void printNgramsCounter(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (Entry<String, Integer> entry : mCounter.entrySet()) {
      out.printf("%s,%d\n", entry.getKey(), entry.getValue());
    }
    out.close();
  }

  /**
   * Returns the count for the given <code>ngrams</code>.
   * 
   * @param ngrams
   * @return
   */
  public int getCount(String ngrams) {
    Integer ret = mCounter.get(ngrams);
    return ret != null ? mCounter.get(ngrams) : 0;
  }

  /**
   * Increments count of n-grams for a sentence.
   * <p>
   * The sentence is split into words using the space and the comma character.
   * 
   * @param sentence
   */
  void incrementCountForSentence(String sentence) {
    String[] words = sentence.split("[\\s,]");
    for (int i = 0; i <= words.length - ngrams; i++) {
      String key = concats(words, i, i + ngrams);
      Integer cnt = mCounter.get(key);
      if (cnt == null) {
        mCounter.put(key, 1);
      } else {
        mCounter.put(key, cnt + 1);
      }
    }
  }

  /**
   * Returns a concatenation of the elements starting from the index
   * <code>start</code> to the index <code>end - 1</code>. The elements are
   * separated by space.
   * 
   * @param words
   * @param start
   * @param end
   * @return
   */
  String concats(String[] words, int start, int end) {
    if (end > words.length) {
      end = words.length;
    }
    StringBuilder builder = new StringBuilder();
    for (int i = start; i < end; i++) {
      builder.append(words[i].toLowerCase()).append(" ");
    }
    builder.deleteCharAt(builder.length() - 1);

    return builder.toString();
  }
}
