package edu.kaist.uilab.crawler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import edu.kaist.uilab.asc.util.TextFiles;

/**
 * Collects data from Darty, the French online retailer.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class AmazonReviewsCollector {
  static final String UTF8 = "UTF-8";
  static final String OUTPUT_DIR = "C:/datasets/asc/electronics-fr/amazon";
  private String file;
  static List<String> patterns, replaces;

  static {
    try {
      patterns = TextFiles.readLines("reviews/patterns.txt");
      replaces = TextFiles.readLines("reviews/specialchars.txt");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Constructor
   */
  public AmazonReviewsCollector(String file) {
    this.file = file;
  }

  /**
   * Crawls the amazon website for reviews and output them to the specified
   * file.
   * 
   * @param outputFile
   */
  public void crawl(String outputFile) {
    PrintWriter out = null;
    try {
      out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
          outputFile, true), UTF8));
      ArrayList<String> pages = new ArrayList<String>();
      String startPage = TextFiles.readFile(file);
      for (int pn = 30; pn < 90; pn++) {
        pages.add(startPage + "&page=" + pn);
      }
      for (String page : pages) {
        for (String reviewPage : getProductReviewPages(page)) {
          for (int pageNumber = 1; pageNumber < 10; pageNumber++) {
            // try 10 pages (no result if page does not exist)
            getReviews(reviewPage
                + "/ref=cm_cr_pr_top_link_8?ie=UTF8&pageNumber=" + pageNumber,
                out);
          }
        }
      }
      out.close();
    } catch (IOException e) {
      System.err.println(e.getMessage());
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  /**
   * Gets links to review pages of products for the specified web page.
   * 
   * @param pageUrl
   *          url of the page
   * @return
   */
  public HashSet<String> getProductReviewPages(String pageUrl)
      throws IOException {
    System.err.println(pageUrl);
    HashSet<String> set = new HashSet<String>();
    try {
      Parser parser;
      Tag tag;
      NodeList list;
      parser = new Parser(pageUrl);
      list = parser.extractAllNodesThatMatch(new HasAttributeFilter("href"));
      String href;
      for (int i = 0; i < list.size(); i++) {
        tag = (Tag) list.elementAt(i);
        href = tag.getAttribute("href");
        if (href.contains("http://www.amazon.fr/product-reviews/")) {
          int pos = href.indexOf("product-reviews/");
          pos = href.indexOf("/", pos + "product-reviews/".length() + 1);
          set.add(href.substring(0, pos));
        }
      }
    } catch (ParserException e) {
      System.err.println("Parser exception: " + e.getMessage());
      e.printStackTrace();
    }

    return set;
  }

  /**
   * Gets reviews of the product at the specified <code>reviewPage</code>.
   * 
   * @param reviewPage
   * @param out
   */
  void getReviews(String reviewPage, PrintWriter out) {
    System.out.println(reviewPage);
    try {
      Parser parser = new Parser(reviewPage);
      parser.setEncoding(UTF8);
      NodeList list = parser.extractAllNodesThatMatch(new HasAttributeFilter(
          "style", "margin-left:0.5em;"));
      for (int i = 0; i < list.size(); i++) {
        NodeList nl = list.elementAt(i).getChildren();
        StringBuilder builder = new StringBuilder();
        // 4th child is the rating text
        String textNode = nl.elementAt(3).toPlainTextString();
        int pos = textNode.indexOf(".");
        String rating = textNode.substring(pos - 1, pos + 2);
        for (int idx = 8; idx < nl.size(); idx++) {
          textNode = nl.elementAt(idx).toPlainTextString().trim()
              .replaceAll("[\\t\\n\\f\\r]", " ");
          textNode = toFrench(textNode);
          if (textNode.length() > 5) {
            builder.append(textNode).append(".");
          }
        }
        pos = builder.indexOf("Aidez d'autres");
        builder.delete(pos, builder.length());
        // write review to file
        out.println(reviewPage);
        out.println(rating);
        out.println(builder);
      }
    } catch (Exception e) {
      System.err.println("No review available");
    }
  }

  private String toFrench(String textNode) {
    for (int i = 0; i < patterns.size(); i++) {
      textNode = textNode.replaceAll(patterns.get(i), replaces.get(i));
    }
    return textNode;
  }

  static class SmallThread extends Thread {
    private String mProduct;

    public SmallThread(String product) {
      mProduct = product;
    }

    @Override
    public void run() {
      AmazonReviewsCollector collector = new AmazonReviewsCollector(
          "reviews/amazon/" + mProduct);
      collector.crawl(OUTPUT_DIR + "/" + mProduct);
    }
  }

  public static void main(String args[]) throws Exception {
    final String[] products = { "AirConditioners.txt", "CanisterVacuums.txt",
        "CoffeeMachines.txt", "Laptops.txt", };
    for (int i = 0; i < products.length; i++) {
      new SmallThread(products[i]).start();
    }
  }
}
