package edu.kaist.uilab.bs;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Vector;

/**
 * A document is a bag of {@link Sentence}s. It may optionally have a rating.
 * 
 * @author trung
 */
public class Document implements Serializable {

  public static final int NO_SENTENCE = -100000;
  private static final long serialVersionUID = -5236933567248150582L;

  private Vector<Sentence> mSentences;
  private int mDocNo;
  private double mRating;
  private String mReviewId;
  private String mRestaurantId;

  public Document(int docNo) {
    mSentences = new Vector<Sentence>();
    mDocNo = docNo;
  }

  public Document(int docNo, String reviewId, String restaurantId) {
    this(docNo);
    mReviewId = reviewId;
    mRestaurantId = restaurantId;
  }
  
  public int getDocNo() {
    return mDocNo;
  }

  public void setReviewId(String id) {
    mReviewId = id;
  }

  public String getReviewId() {
    return mReviewId;
  }
  
  public String getRestaurantId() {
    return mRestaurantId;
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
      HashSet<String> set1 = new HashSet<String>();
      for (String w : sent.getText().split("[\\s]+")) {
        set1.add(w);
      }
      HashSet<String> set2 = new HashSet<String>();
      for (String w : sentence.split("[\\s]+")) {
        set2.add(w);
      }
      if (set1.equals(set2)) {
        return sent.getSenti();
      }
    }
    
    return NO_SENTENCE;
  }
}
