package edu.kaist.uilab.asc.crawler;

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

/**
 * Collecting reviews in English.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class EnglishReviewsCollector {
  
  // reviews for Avatar in English (max page = 757; total reviews = )
  // static final String URL = "http://www.rottentomatoes.com/m/avatar/audience_reviews?&page=";
  // reviews for Titanic in English (max page = 32718)
  static final String URL = "http://www.rottentomatoes.com/m/titanic/audience_reviews?&page=";
  static final String OUTPUT_DIR = "C:/datasets/asc/titanic";
  static final int MAX_PAGE = 32718;
  static final String rating[] = {
    "fixed stars score10",
    "fixed stars score20",
    "fixed stars score30",
    "fixed stars score40",
    "fixed stars score50",
  };
  
  /**
   * Crawls the review site and store all reviews into the specified text file.
   * 
   * @param output
   * @throws ParserException
   * @throws IOException
   */
  public void crawl(String output) {
    PrintWriter out = null;
    try {
      out = new PrintWriter(new OutputStreamWriter(
          new FileOutputStream(output), "UTF-8"));
      Parser parser;
      for (int page = 1; page <= MAX_PAGE; page++) {
        System.out.println("Crawling " + URL + page);
        parser = new Parser(URL + page);
        parser.setEncoding("utf-8");
        NodeList reviewHolders = parser.extractAllNodesThatMatch(
            new HasAttributeFilter("class", "media_block_content"));
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
    StringBuilder builder = new StringBuilder(htmlContent.trim());
    // remove the CDATA section
    int start = builder.indexOf("/*");
    if (start > 0) {
      int end = builder.lastIndexOf("*/");
      builder.delete(start, end + 2);
    }
    return builder.toString().replace("\t", " ").replace("\n", " ").replace(
        "\r", " ").replace("  ", " ");
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
      new HasAttributeFilter("class", "fixed stars score50"),
    };
    
    public RatingTagFilter() {
      super(filters);
    }
  }
  
  public static void main(String args[]) throws Exception {
    EnglishReviewsCollector collector = new EnglishReviewsCollector();
    collector.crawl(OUTPUT_DIR + "/titanic_en.txt");
  }
}
