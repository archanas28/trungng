package edu.kaist.uilab.asc.data;

import java.io.Serializable;
import java.util.Vector;

/**
 * Document represented as bag of sentences.
 */
public class Document implements Serializable {
  
  public static final int NO_SENTENCE = -100000;
  private static final long serialVersionUID = -5236933567248150582L;

  Vector<Sentence> mSentences;
  int mDocNo;
  double mRating;
  private String mExternalId;

  public Document(int docNo) {
    mSentences = new Vector<Sentence>();
    mDocNo = docNo;
  }

  public int getDocNo() {
    return mDocNo;
  }

  public void setExternalId(String id) {
    mExternalId = id;
  }

  public String getExternalId() {
    return mExternalId;
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
  
  /**
   * Returns the sentiment of the given <code>sentence</code> provided that the
   * sentence is in the document.
   * 
   * @param sentence
   * @return the sentiment or {@link Document.NO_SENTENCE} if the sentence is not
   *         in the document
   */
  public int getSentenceSentiment(String sentence) {
    for (Sentence sent : mSentences) {
      String text = sent.getText();
      if (text.contains(sentence) || sentence.contains(text)) {
        return sent.getSenti();
      }
    }
    
    return NO_SENTENCE;
  }
}
