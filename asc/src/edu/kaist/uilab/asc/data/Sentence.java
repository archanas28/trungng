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
  
  private Vector<Word> words;
  private TreeMap<Word, Integer> wordCnt; // Somehow HashMap doesn't work
                                          // correctly
  private int topic;
  private int senti;
  public int numSenti;

  public Sentence() {
    words = new Vector<Word>();
    wordCnt = new TreeMap<Word, Integer>();
  }

  public void addWord(Word word) {
    words.add(word);
    Integer cnt = wordCnt.get(word);
    if (cnt == null)
      wordCnt.put(word, 1);
    else
      wordCnt.put(word, cnt + 1);
  }

  public Vector<Word> getWords() {
    return words;
  }

  public TreeMap<Word, Integer> getWordCnt() {
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
