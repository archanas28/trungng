package edu.kaist.uilab.asc.data;

import java.io.Serializable;
import java.util.Vector;

/**
 * Document represented as bag of sentences.
 */
public class Document implements Serializable {

  private static final long serialVersionUID = -5236933567248150582L;
  
  Vector<Sentence> mSentences;
  int mDocNo;
  double mRating;

  public Document(int docNo) {
    mSentences = new Vector<Sentence>();
    mDocNo = docNo;
  }
  
  public int getDocNo() {
    return mDocNo;
  }

  public void setRating(double rating) {
    mRating = rating;
  }

  public double getRating() {
    return mRating;
  }

  public void addSentence(Sentence sentence) {
    mSentences.add(sentence);
  }

  public Vector<Sentence> getSentences() {
    return mSentences;
  }
}
