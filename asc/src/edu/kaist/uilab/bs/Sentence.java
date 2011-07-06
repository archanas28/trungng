package edu.kaist.uilab.bs;

import java.io.Serializable;
import java.util.Vector;

/**
 * A sentence has the aspect-related words and the sentiment-related words.
 * 
 * @author trung
 */
public class Sentence implements Serializable {
  private static final long serialVersionUID = 6117469973757059170L;
  
  Vector<Integer> mAspectWords;
  Vector<Integer> mSentiWords;
  int mTopic = -1;
  int mSenti = -1;

  public Sentence() {
    mAspectWords = new Vector<Integer>();
    mSentiWords = new Vector<Integer>();
  }

  public Vector<Integer> getAspectWords() {
    return mAspectWords;
  }
  
  public Vector<Integer> getSentiWords() {
    return mSentiWords;
  }

  public int getTopic() {
    return mTopic;
  }

  public int getSenti() {
    return mSenti;
  }

  /**
   * Adds an aspect-related word to this sentence.
   */
  public void addAspectWord(Integer idx) {
    mAspectWords.add(idx);
  }
  
  /**
   * Adds an senti-related word to this sentence.
   */
  public void addSentiWord(Integer idx) {
    mSentiWords.add(idx);
  }

  /**
   * Sets topic for all words in this sentence.
   * 
   * @param topic
   */
  public void setTopic(int topic) {
    mTopic = topic;
  }

  /**
   * Sets sentiment for all sentiment words in this sentence.
   * 
   * @param senti
   */
  public void setSenti(int senti) {
    mSenti = senti;
  }
}
