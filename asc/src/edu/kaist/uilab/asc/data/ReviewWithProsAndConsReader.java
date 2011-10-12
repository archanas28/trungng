package edu.kaist.uilab.asc.data;

import java.io.BufferedReader;
import java.io.IOException;

import edu.kaist.uilab.bs.util.DocumentUtils;

/**
 * A reader that reads reviews with pros and cons.
 * 
 * @author trung
 */
public class ReviewWithProsAndConsReader extends ReviewReader {

  @Override
  public Review readReview(BufferedReader reader, boolean negateContent)
      throws IOException {
    String line;
    Double rating;
    ReviewWithProsAndCons review = null;
    if ((line = reader.readLine()) != null) {
      String[] ids = line.split(" ");
      try {
        rating = Double.parseDouble(reader.readLine());
      } catch (NumberFormatException e) {
        rating = -1.0;
      }
      if (negateContent) {
        review = new ReviewWithProsAndCons(ids[0], ids[1], rating,
            reader.readLine(), reader.readLine(), DocumentUtils.negate(reader
                .readLine()));
      } else {
        review = new ReviewWithProsAndCons(ids[0], ids[1], rating,
            reader.readLine(), reader.readLine(), reader.readLine());
      }
    }

    return review;

  }
}
