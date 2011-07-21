package edu.kaist.uilab.bs;

import java.io.Serializable;
import java.util.Vector;

/**
 * A document is a bag of {@link Sentence}s. It may optionally have a rating.
 * 
 * @author trung
 */
public class Document implements Serializable {

  private static final long serialVersionUID = -5236933567248150582L;

  private Vector<Sentence> mSentences;
  private int mDocNo;
  private double mRating;

  public Document(int docNo) {
    mSentences = new Vector<Sentence>();
    mDocNo = docNo;
  }

  public int getDocNo() {
    return mDocNo;
  }

  public Vector<Sentence> getSentences() {
    return mSentences;
  }

  public int getNumSentences() {
    return mSentences.size();
  }

  public void setRating(double rating) {
    mRating = rating;
  }

  public double getRating() {
    return mRating;
  }

  /**
   * Adds a sentence to this document.
   * 
   * @param sentence
   *          the sentence
   */
  public void addSentence(Sentence sentence) {
    mSentences.add(sentence);
  }
}
