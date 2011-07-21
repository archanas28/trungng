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

  private Vector<Integer> mAspectWords;
  private Vector<Integer> mSentiWords;
  private int mTopic = -1;
  private int mSenti = -1;
  private String mText;

  public Sentence(String text) {
    mText = text;
    mAspectWords = new Vector<Integer>();
    mSentiWords = new Vector<Integer>();
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

  public int getTopic() {
    return mTopic;
  }

  public int getSenti() {
    return mSenti;
  }

  public String getText() {
    return mText;
  }

  public int length() {
    return mAspectWords.size() + mSentiWords.size();
  }

  public Vector<Integer> getAspectWords() {
    return mAspectWords;
  }

  public Vector<Integer> getSentiWords() {
    return mSentiWords;
  }

  public boolean hasAspectAndSenti() {
    return mAspectWords.size() > 0 && mSentiWords.size() > 0;
  }
}
