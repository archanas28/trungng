package edu.kaist.uilab.asc.data;

public class ReviewWithProsAndCons extends Review {

  private String pros;
  private String cons;

  /**
   * Constructor
   * 
   * @param reviewId
   * @param targetId
   * @param rating
   * @param pros
   * @param cons
   * @param content
   */
  public ReviewWithProsAndCons(String reviewId, String targetId, Double rating,
      String pros, String cons, String content) {
    super(reviewId, targetId, rating, content);
    this.pros = pros != null ? pros : "";
    this.cons = cons != null ? cons : "";
  }

  /**
   * Returns the pros list as provided by customer.
   * 
   * @return the pros string (empty string if not available)
   */
  public String getPros() {
    return pros;
  }

  /**
   * Returns the cons list as provided by customer.
   * 
   * @return the cons string (empty string if not available)
   */
  public String getCons() {
    return cons;
  }

  @Override
  public String toString() {
    return String.format("%s %s\n%.1f\n%s\n%s\n%s", mReviewId, mRestaurantId,
        mRating, pros, cons, mContent);
  }
}
