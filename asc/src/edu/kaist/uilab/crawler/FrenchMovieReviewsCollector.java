package edu.kaist.uilab.crawler;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import edu.kaist.uilab.asc.data.Review;

/**
 * Collecting reviews in french.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class FrenchMovieReviewsCollector {
  static final String UTF8 = "UTF-8";

  static final String OUTPUT_DIR = "C:/datasets/asc/movies";
  static final int MAX_PAGE = 200;

  /**
   * Crawls the review site and store all reviews into the specified text file.
   * 
   * @param source
   * @param output
   * @param fromPage
   * @param toPage
   */
  public void crawl(String source, int fromPage, int toPage, String output) {
    PrintWriter out = null;
    try {
      out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output,
          true), UTF8));
      Parser parser;
      for (int page = fromPage; page < toPage; page++) {
        System.out.println("Crawling " + source + page);
        try {
          parser = new Parser(source + page);
          parser.setEncoding(UTF8);
          NodeList reviewHolders = parser
              .extractAllNodesThatMatch(new AndFilter(new HasAttributeFilter(
                  "class", "datablock member"), new HasAttributeFilter("id")));
          writeReviews(source + page, reviewHolders, out);
        } catch (ParserException e) {
          System.err.println("Parser exception: " + e.getMessage());
        }
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
      reviewHolder.collectInto(list, new HasAttributeFilter("class",
          "lighten fs11"));
      if (list.size() > 0) {
        rating = nodeToRating(list.elementAt(0));
      }
      // get review content
      list = new NodeList();
      reviewHolder.collectInto(list, new HasAttributeFilter("class", "fs11"));
      if (list.size() > 1) {
        // the first <p class="fs11"> element is empty
        content = normalizeContent(list.elementAt(1).toPlainTextString().trim());
      }
      out.print(new Review(source, null, rating, content));
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
    for (int i = 0; i <= 5; i++) {
      if (s.equals("(" + i + ")")) {
        return (double) i;
      }
      if (s.equals("(" + i + "," + "5)")) {
        return (i + 0.5);
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
    htmlContent = htmlContent.substring(15);
    return htmlContent.replaceAll("[\t\n\r]+", " ");
  }

  public static class SmallThread extends Thread {
    String movieName, moviePage;
    int fromPage, toPage, idx;

    public SmallThread(String movieName, String moviePage, int fromPage,
        int toPage, int idx) {
      this.movieName = movieName;
      this.moviePage = moviePage;
      this.fromPage = fromPage;
      this.toPage = toPage;
      this.idx = idx;
    }

    @Override
    public void run() {
      FrenchMovieReviewsCollector collector = new FrenchMovieReviewsCollector();
      collector.crawl(moviePage, fromPage, toPage, OUTPUT_DIR + "/" + movieName
          + idx + "_fr.txt");
    }
  }

  public static void main(String args[]) throws Exception {
    final String[] moviePages = {
        "http://www.allocine.fr/film/critiquepublic_gen_cfilm=15336.html?page=",
        "http://www.allocine.fr/film/critiquepublic_gen_cfilm=140991.html?page=",
        "http://www.allocine.fr/film/critiquepublic_gen_cfilm=45890.html?page=",
        "http://www.allocine.fr/film/critiquepublic_gen_cfilm=134539.html?page=",
        "http://www.allocine.fr/film/critiquepublic_gen_cfilm=54226.html?page=", };

    // final String[] movieNames = { "lord_of_the_rings_the_return_of_the_king",
    // // 8.8
    // "toy_story_3", // 8.7
    // "1221547-alice_in_wonderland", // 6.6
    // "pirates_of_the_caribbean_3", // 7.0
    // "star_wars_episode_sti_the_phantom_menace", // 6.4
    // "1194515-ice_age_dawn_of_the_dinosaurs", // 7.0
    // "transformers_revenge_of_the_fallen", // 5.9
    // "shrek_3", // 6.1
    // };
    final String[] movieNames = { "1071806-independence_day", // 6.6
        "twilight_saga_new_moon", // 4.5
        "indiana_jones_and_the_kingdom_of_the_crystal_skull", // 6.5
        "2012", // 5.8
        "da_vinci_code", // 6.4
    };

    FrenchMovieReviewsCollector collector = new FrenchMovieReviewsCollector();
    for (int i = 0; i < movieNames.length; i++) {
      collector.crawl(moviePages[i], 0, MAX_PAGE, OUTPUT_DIR + "/"
          + movieNames[i] + "_fr.txt");
      // int numBlock = 1;
      // int blockSize = MAX_PAGE / numBlock;
      // for (int blockIdx = 0; blockIdx < numBlock; blockIdx++) {
      // new SmallThread(movieNames[i], moviePages[i], blockIdx * blockSize,
      // (blockIdx + 1) * blockSize, blockIdx).start();
      // }
    }
  }
}
