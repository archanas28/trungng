package edu.kaist.uilab.bs;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Counter for 2-grams in the corpus.
 * 
 * @author trung
 */
public class TwogramsCounter implements Serializable {

  private static final long serialVersionUID = 8226442689458486668L;
  private HashMap<String, Integer> mCounter;

  /**
   * Default constructor
   */
  public TwogramsCounter() {
    mCounter = new HashMap<String, Integer>();
  }

  /**
   * Increases count for the given <code>(word1, word2)</code> pair.
   * 
   * @param word1
   * @param word2
   */
  public void increaseCount(String word1, String word2) {
    String phrase = constructNgram(word1, word2);
    Integer cnt = mCounter.get(phrase);
    if (cnt == null) {
      mCounter.put(phrase, 1);
    } else {
      mCounter.put(phrase, cnt + 1);
    }
  }

  /**
   * Constructs a phrase from the given <code>word1</code> and
   * <code>word2</code>.
   * 
   * @param word1
   * @param word2
   * @return
   */
  private String constructNgram(String word1, String word2) {
    return word1 + " " + word2;
  }

  /**
   * Returns the count for the phrase consisting of the
   * <code>(word1, word2)</code> pair.
   * 
   * @param word1
   * @param word2
   * @return
   */
  public int getCount(String word1, String word2) {
    String phrase = constructNgram(word1, word2);
    Integer cnt = mCounter.get(phrase);
    if (cnt == null) {
      cnt = 0;
    }

    return cnt;
  }
}
