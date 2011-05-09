package edu.kaist.uilab.asc.crawler;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeList;

/**
 * Collecting reviews in french.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class FrenchMovieReviewsCollector {
  // reviews for Avatar in french(max page = 957; reviews = 9565)
  static final String URL = "http://www.allocine.fr/film/critiquepublic_gen_cfilm=5818.html?page=";
  // reviews for Titanic in french(max page = 140; reviews = 1399)
  static final String UTF8 = "UTF-8";
  
  static final String OUTPUT_DIR = "C:/datasets/asc/titanic";
  static final int MAX_PAGE = 140;
  
  /**
   * Crawls the review site and store all reviews into the specified text file.
   * 
   * @param output
   * @param from
   * @param to
   */
  public void crawl(String output, int from, int to) {
    PrintWriter out = null;
    try {
      out = new PrintWriter(new OutputStreamWriter(
          new FileOutputStream(output), UTF8));
      Parser parser;
      for (int page = from; page <= to; page++) {
        System.out.println("Crawling " + URL + page);
        parser = new Parser(URL + page);
        parser.setEncoding(UTF8);
        NodeList reviewHolders = parser.extractAllNodesThatMatch(
            new AndFilter(new HasAttributeFilter("class", "datablock member"),
                new HasAttributeFilter("id")));
        writeReviews(URL + page, reviewHolders, out);
      }
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
   *       the url of the page in which the review resides
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
       reviewHolder.collectInto(list, new HasAttributeFilter("class",
           "lighten fs11"));
       if (list.size() > 0) {
         rating = nodeToRating(list.elementAt(0));
       }
       // get review content
       list = new NodeList();
       reviewHolder.collectInto(list, new HasAttributeFilter("class",
           "fs11"));
       if (list.size() > 1) {
         // the first <p class="fs11"> element is empty
         content = normalizeContent(list.elementAt(1).toPlainTextString().trim());
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
    String s = node.toPlainTextString();
    if (s.length() == 3) {
      // example of rating text : (4)
      return Double.parseDouble(s.substring(1, 2));
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
    StringBuilder builder = new StringBuilder(htmlContent.trim());
    // delete "Sa critique : "
    builder.delete(0, 15);
    return builder.toString().replace("\t", " ").replace("\n", " ").replace(
        "\r", " ").replace("  ", " ");
  }
  
  public static void main(String args[]) throws Exception {
    FrenchMovieReviewsCollector collector = new FrenchMovieReviewsCollector();
    collector.crawl(OUTPUT_DIR + "/titanic_fr.txt", 0, MAX_PAGE);
  }
}
