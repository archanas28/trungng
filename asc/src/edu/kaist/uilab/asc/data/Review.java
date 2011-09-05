package edu.kaist.uilab.asc.data;

/**
 * A review to be stored in the dataset.
 * <p>
 * Each review contains:
 * <li>rating (if available)
 * <li>url (source of the review if available)
 * <li>content
 * </p>
 *
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Review {
  public static final double NO_RATING = -1.0;

  private Double mRating;
  String mReviewId;
  // TODO(trung): rename mRestaurantId to mTargetId
  String mRestaurantId;
  private String mContent;

  /**
   * Constructor
   * <p>
   * All parameters that are not available must be null.
   * 
   * @param reviewid
   * @param restaurantId
   * @param rating
   * @param content
   */
  public Review(String reviewId, String restaurantId, Double rating,
      String content) {
    mRating = rating != null ? rating : NO_RATING;
    mReviewId = reviewId != null ? reviewId : "";
    mRestaurantId = restaurantId != null ? restaurantId : "";
    mContent = content;
  }

  public String getReviewId() {
    return mReviewId;
  }

  public String getRestaurantId() {
    return mRestaurantId;
  }
  
  public Double getRating() {
    return mRating;
  }

  public String getContent() {
    return mContent;
  }

  @Override
  /**
   * Returns a printable string which can be used to write this review into
   * a text file.
   */
  public String toString() {
    return String.format("%s %s\n%.1f\n%s", mReviewId, mRestaurantId,
        mRating, mContent);
  }
}
