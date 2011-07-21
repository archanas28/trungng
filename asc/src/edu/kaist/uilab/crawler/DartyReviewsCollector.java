package edu.kaist.uilab.crawler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import edu.kaist.uilab.asc.data.Review;
import edu.kaist.uilab.asc.util.TextFiles;

/**
 * Collects data from Darty, the French online retailer.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class DartyReviewsCollector {
  static final String DARTY = "http://www.darty.com";
  static final String PAGESIZE = "&pageSize=100";
  static final String UTF8 = "UTF-8";
  static final String OUTPUT_DIR = "C:/datasets/asc/darty";

  private String categoryFile;

  /**
   * Constructor
   * 
   * @param categoryFile
   *          the file that contains link to main product categories/models
   */
  public DartyReviewsCollector(String categoryFile) {
    this.categoryFile = categoryFile;
  }

  /**
   * Crawls the Darty website for reviews and output them to the specified file.
   * 
   * @param outputFile
   */
  public void crawl(String outputFile) {
    PrintWriter out = null;
    try {
      out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
          outputFile), UTF8));
      List<String> modelLinks = TextFiles.readLines(categoryFile);
      ArrayList<String> productLinks;
      for (String modelLink : modelLinks) {
        productLinks = getProductLinks(modelLink + PAGESIZE);
        for (String productLink : productLinks) {
          getReviews(productLink, out);
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
   * Gets links to products for the specified <code>category</code>
   * 
   * @param category
   * @return
   */
  public ArrayList<String> getProductLinks(String category) {
    System.err.println(category);
    ArrayList<String> links = new ArrayList<String>();
    Parser parser;
    Node node;
    NodeList list;
    try {
      parser = new Parser(category);
      list = parser.extractAllNodesThatMatch(new HasAttributeFilter("class",
          "produit"));
      for (int i = 0; i < list.size(); i++) {
        node = list.elementAt(i);
        NodeList nl = new NodeList();
        node.collectInto(nl, new TagNameFilter("a"));
        if (nl.size() > 0) {
          CompositeTag hrefTag = (CompositeTag) nl.elementAt(0);
          links.add(DARTY + hrefTag.getAttribute("href"));
        }
      }
    } catch (ParserException e) {
      System.err.println("Parser exception: " + e.getMessage());
      e.printStackTrace();
    }

    return links;
  }

  /**
   * Gets reviews of the product at the specified <code>productLink</code>.
   * 
   * @param productLink
   * @param out
   */
  void getReviews(String productLink, PrintWriter out) {
    System.out.println(productLink);
    Parser parser;
    NodeList list;
    try {
      parser = new Parser(productLink);
      list = parser.extractAllNodesThatMatch(new AndFilter(new TagNameFilter(
          "iframe"), new HasAttributeFilter("src")));
      if (list.size() > 0) {
        String reviewLink = ((TagNode) list.elementAt(0)).getAttribute("src");
        parser = new Parser(reviewLink);
        parser.setEncoding(UTF8);
        // review body
        list = parser.extractAllNodesThatMatch(new HasAttributeFilter("class",
            "BVRRReviewDisplayStyle3"));
        for (int i = 0; i < list.size(); i++) {
          getReview(productLink, list.elementAt(i), out);
        }
      }
    } catch (ParserException e) {
      e.printStackTrace();
    }
  }

  /**
   * Gets the review from the given <code>node</code>.
   * 
   * @param node
   * @param out
   */
  void getReview(String productLink, Node node, PrintWriter out) {
    double rating = Review.NO_RATING;
    String content = "";
    NodeList nl = new NodeList();
    node.collectInto(nl, new HasAttributeFilter("class",
        "BVRRRatingNormalImage"));
    if (nl.size() > 0) {
      rating = nodeToRating(nl.elementAt(0));
    }
    nl.removeAll();
    node.collectInto(nl, new HasAttributeFilter("class", "BVRRReviewText"));
    if (nl.size() > 0) {
      content = nl.elementAt(0).toPlainTextString();
    }
    out.print(new Review(productLink, rating, content));
    System.out.println(content);
  }

  /**
   * Converts the specified node to rating.
   * 
   * @param node
   * @return
   */
  Double nodeToRating(Node node) {
    ImageTag imgTag = (ImageTag) node.getFirstChild().getNextSibling();
    String s = imgTag.getAttribute("alt");
    return Double.parseDouble(s.substring(0, 1));
  }

  static class SmallThread extends Thread {
    private String mProduct;

    public SmallThread(String product) {
      mProduct = product;
    }

    @Override
    public void run() {
      DartyReviewsCollector collector = new DartyReviewsCollector("reviews/darty/"
          + mProduct);
      collector.crawl(OUTPUT_DIR + "/" + mProduct);
    }
  }

  public static void main(String args[]) throws Exception {
    final String[] products = { "airconditioner.txt", "camera.txt",
        "coffee.txt", "cooker.txt", "dishwasher.txt", "dvdblueray.txt",
        "fridge.txt", "mobilephone.txt", "tv.txt", "vacuum.txt", "washing.txt" };
    for (int i = 0; i < products.length; i++) {
      new SmallThread(products[i]).start();
    }
  }
}
