package edu.kaist.uilab.asc.data;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.Vector;

/**
 * A sentence has:
 * <li> the set of words
 * <li> the counts of each word in the sentence
 * <li> a topic
 * <li> a sentiment
 */
public class Sentence implements Serializable {
  private static final long serialVersionUID = 6117469973757059170L;
  
  private Vector<SamplingWord> words;
  private TreeMap<SamplingWord, Integer> wordCnt; // Somehow HashMap doesn't work
                                          // correctly
  private int topic = -1;
  private int senti = -1;

  public Sentence() {
    words = new Vector<SamplingWord>();
    wordCnt = new TreeMap<SamplingWord, Integer>();
  }

  /**
   * Adds a word to this sentence.
   * 
   * <p> This automatically updates the count for the word.
   * 
   * @param word
   */
  public void addWord(SamplingWord word) {
    words.add(word);
    Integer cnt = wordCnt.get(word);
    if (cnt == null)
      wordCnt.put(word, 1);
    else
      wordCnt.put(word, cnt + 1);
  }

  public Vector<SamplingWord> getWords() {
    return words;
  }

  public TreeMap<SamplingWord, Integer> getWordCnt() {
    return wordCnt;
  }

  public int getTopic() {
    return topic;
  }

  public int getSenti() {
    return senti;
  }

  public void setTopic(int topic) {
    this.topic = topic;
  }

  public void setSenti(int senti) {
    this.senti = senti;
  }
}
