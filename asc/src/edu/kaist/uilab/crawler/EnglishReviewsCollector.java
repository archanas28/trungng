package edu.kaist.uilab.crawler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import edu.kaist.uilab.asc.data.Review;

/**
 * Collecting reviews in English.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class EnglishReviewsCollector {

  // "http://www.rottentomatoes.com/m/avatar/audience_reviews?&page=";
  static final String UTF8 = "utf-8";
  static final String URL = "http://www.rottentomatoes.com/m/";
  static final String OUTPUT_DIR = "C:/datasets/asc/movies";
  static final int MAX_PAGE = 2000;
  static final String rating[] = { "fixed stars score10",
      "fixed stars score20", "fixed stars score30", "fixed stars score40",
      "fixed stars score50", };

  /**
   * Crawls the review site and store all reviews into the specified text file.
   * 
   * @param output
   * @throws ParserException
   * @throws IOException
   */
  public void crawl(String movieName, int fromPage, int toPage, String output) {
    PrintWriter out = null;
    String source;
    try {
      out = new PrintWriter(new OutputStreamWriter(
          new FileOutputStream(output), UTF8));
      Parser parser;
      for (int page = fromPage; page < toPage; page++) {
        source = URL + movieName + "/audience_reviews?&page=" + page;
        System.out.println("Crawling " + source);
        parser = new Parser(source);
        parser.setEncoding(UTF8);
        writeReviews(source,
            parser.extractAllNodesThatMatch(new HasAttributeFilter("class",
                "media_block_content")), out);
      }
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  /**
   * Writes reviews from the review holders (extracted from the web) into the
   * provided writer.
   * 
   * @param source
   *          the url of the page in which the review resides
   * @param reviewHolders
   * @param out
   */
  void writeReviews(String source, NodeList reviewHolders, PrintWriter out) {
    Node reviewHolder;
    Double rating = null;
    String content = null;
    NodeList list;
    for (int i = 0; i < reviewHolders.size(); i++) {
      reviewHolder = reviewHolders.elementAt(i);
      // get review rating
      list = new NodeList();
      reviewHolder.collectInto(list, new RatingTagFilter());
      if (list.size() > 0) {
        rating = nodeToRating(list.elementAt(0));
      }
      // get review content
      list = new NodeList();
      reviewHolder.collectInto(list, new HasAttributeFilter("class",
          "user_review"));
      if (list.size() > 0) {
        content = normalizeContent(list.elementAt(0).toPlainTextString().trim());
      }
      out.print(new Review(source, rating, content));
    }
  }

  /**
   * Converts the specified node to rating.
   * 
   * @param node
   * @return
   */
  Double nodeToRating(Node node) {
    String s = ((CompositeTag) node).getAttribute("class");
    for (int i = 0; i < rating.length; i++) {
      if (rating[i].equals(s)) {
        return (i + 1) * 1.0;
      }
    }
    return Review.NO_RATING;
  }

  /**
   * Changes the html content into normal text.
   * 
   * @param content
   * @return
   */
  String normalizeContent(String htmlContent) {
    htmlContent = htmlContent.trim();
    htmlContent = htmlContent.replaceAll("[\t\n\r]+", " ");
    htmlContent = htmlContent.replaceAll("/.*/", " ");
    return htmlContent;
  }

  /**
   * Filter for the rating tag.
   */
  static class RatingTagFilter extends OrFilter {
    private static final long serialVersionUID = 1L;

    static HasAttributeFilter[] filters = new HasAttributeFilter[] {
        new HasAttributeFilter("class", "fixed stars score10"),
        new HasAttributeFilter("class", "fixed stars score20"),
        new HasAttributeFilter("class", "fixed stars score30"),
        new HasAttributeFilter("class", "fixed stars score40"),
        new HasAttributeFilter("class", "fixed stars score50"), };

    public RatingTagFilter() {
      super(filters);
    }
  }

  public static class SmallThread extends Thread {
    String movieName;
    int fromPage, toPage, idx;

    public SmallThread(String movieName, int fromPage, int toPage, int idx) {
      this.movieName = movieName;
      this.fromPage = fromPage;
      this.toPage = toPage;
      this.idx = idx;
    }

    @Override
    public void run() {
      EnglishReviewsCollector collector = new EnglishReviewsCollector();
      collector.crawl(movieName, fromPage, toPage, OUTPUT_DIR + "/" + movieName
          + idx + "_en.txt");
    }
  }

  public static void main(String args[]) throws Exception {
//    final String[] movieNames = { "lord_of_the_rings_the_return_of_the_king", // 8.8
//        "toy_story_3", // 8.7
//        "1221547-alice_in_wonderland", // 6.6
//        "pirates_of_the_caribbean_3", // 7.0
//        "star_wars_episode_i_the_phantom_menace", // 6.4
//        "1194515-ice_age_dawn_of_the_dinosaurs", // 7.0
//        "transformers_revenge_of_the_fallen", // 5.9
//        "shrek_3", // 6.1
//    };
    final String[] movieNames = { "1071806-independence_day", // 6.6
        "twilight_saga_new_moon", // 4.5
        "indiana_jones_and_the_kingdom_of_the_crystal_skull", // 6.5
        "2012", // 5.8
        "da_vinci_code", // 6.4
    };
    for (String movie : movieNames) {
      int numBlock = 10;
      int blockSize = MAX_PAGE / numBlock;
      for (int blockIdx = 0; blockIdx < numBlock; blockIdx++) {
        new SmallThread(movie, blockIdx * blockSize,
            (blockIdx + 1) * blockSize, blockIdx).start();
      }
    }
  }
}
