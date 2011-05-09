package edu.kaist.uilab.asc.crawler;

/**
 * A review to be stored in the dataset.
 * 
 * <p> Each review contains:
 * <li>rating (if available)
 * <li>url (source of the review if available)
 * <li>content
 * </p>
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Review {
  public static final double NO_RATING = -1.0;
  
  Double mRating;
  String mSource;
  String mContent;
  
  /**
   * Constructor
   * 
   * <p> <code>rating</code> and <code>source</code> should be null if not available.
   * 
   * @param rating
   * @param source
   * @param content
   */
  public Review(String source, Double rating, String content) {
    if (rating != null) {
      mRating = rating;
    } else {
      mRating = NO_RATING;
    }
    if (source != null) {
      mSource = source;
    } else {
      source = "";
    }
    mContent = content;
  }
  
  @Override
  /**
   * Returns a printable string which can be used to write this review into
   * a text file.
   */
  public String toString() {
    return String.format("%s\n%.1f\n%s\n", mSource, mRating, mContent);
  }
}
