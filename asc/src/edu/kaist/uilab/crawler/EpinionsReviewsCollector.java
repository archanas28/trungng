package edu.kaist.uilab.crawler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.commons.lang3.StringEscapeUtils;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.util.NodeList;

import edu.kaist.uilab.asc.data.ReviewWithProsAndCons;

/**
 * Collects reviews from <a
 * href="http://www.epinions.com">http://www.epinions.com</a>.
 * 
 * @author trung
 */
public class EpinionsReviewsCollector {

  private static final String DOMAIN = "http://www.epinions.com";

  /**
   * Starts the crawling process from the given product listing page.
   */
  public void crawl(String outputDir, String categoryName, int page,
      String productsListingPage) {
    System.err.println("Crawling " + productsListingPage);
    try {
      PrintWriter out = new PrintWriter(outputDir + "/" + categoryName + page
          + ".txt");
      ArrayList<String> productLinks = getProductLinks(productsListingPage);
      ReviewWithProsAndCons review;
      for (String productLink : productLinks) {
        System.out.println(productLink);
        ArrayList<String> reviewLinks = getReviewLinks(productLink);
        for (String reviewLink : reviewLinks) {
          System.out.println(reviewLink);
          review = getReview(reviewLink);
          if (review != null) {
            out.println(review);
          }
        }
      }
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns the links to product pages from the given product
   * <code>category</code> page.
   * 
   * @param category
   *          url of the category page
   * @return
   */
  public ArrayList<String> getProductLinks(String category) {
    ArrayList<String> links = new ArrayList<String>();
    try {
      Parser parser = new Parser(category);
      // a product is enclosed by <td class="rkr"...>
      NodeList list = parser.extractAllNodesThatMatch(new HasAttributeFilter(
          "class", "rkr"));
      for (int i = 0; i < list.size(); i++) {
        Node tdNode = list.elementAt(i);
        NodeList hrefs = new NodeList();
        tdNode.collectInto(hrefs, new TagNameFilter("a"));
        if (hrefs.size() == 2) {
          // the second hyperlink <a href="/reviews/..." is the link to review
          CompositeTag reviewTag = (CompositeTag) hrefs.elementAt(1);
          String candidate = reviewTag.getAttribute("href");
          if (candidate.startsWith("/reviews/")) {
            links.add(DOMAIN + candidate);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return links;
  }

  /**
   * Returns the links to reviews of <code>product</code>.
   * 
   * @param product
   *          url of the product page
   * @return
   */
  public ArrayList<String> getReviewLinks(String product) {
    ArrayList<String> reviewLinks = new ArrayList<String>();
    try {
      int numPages = getNumReviews(product);
      for (int page = 1; page <= numPages; page++) {
        String url = product + "/sec_~opinion_list/pp_~" + page + "#list";
        Parser parser = new Parser(url);
        // each link to a review is contained in the node
        // <span class="rkr">
        NodeList list = parser.extractAllNodesThatMatch(new AndFilter(
            new TagNameFilter("span"), new HasAttributeFilter("class", "rkr")));
        for (int i = 0; i < list.size(); i++) {
          Node spanNode = list.elementAt(i);
          NodeList hrefs = new NodeList();
          spanNode.collectInto(hrefs, new TagNameFilter("a"));
          if (hrefs.size() == 1) {
            CompositeTag reviewTag = (CompositeTag) hrefs.elementAt(0);
            String candidate = reviewTag.getAttribute("href");
            // a proper link should be href="/review/...content_..."
            if (candidate.startsWith("/review/")
                && candidate.contains("content_")) {
              reviewLinks.add(DOMAIN + candidate);
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return reviewLinks;
  }

  /**
   * Gets the number of pages that content review links in the given product
   * page <code>product</code>.
   * 
   * @param product
   * @return
   */
  private int getNumReviews(String product) {
    final int reviewsPerPage = 15;
    try {
      Parser parser = new Parser(product);
      // find the number of reviews
      NodeList list = parser.extractAllNodesThatMatch(new HasAttributeFilter(
          "class", "page_tab_selected"));
      if (list.size() == 1) {
        String text = list.elementAt(0).toPlainTextString();
        int start = text.indexOf("(") + 1;
        int end = text.indexOf(")");
        int numReviews = Integer.parseInt(text.substring(start, end));
        return (numReviews % reviewsPerPage == 0) ? numReviews / reviewsPerPage
            : numReviews / reviewsPerPage + 1;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return 0;
  }

  /**
   * Returns a review from the <code>reviewLink</code>.
   * 
   * @param reviewLink
   *          link to the review page
   * @return
   */
  public ReviewWithProsAndCons getReview(String reviewLink) {
    // format: .../review/productName/content_reviewId
    try {
      Parser parser = new Parser(reviewLink);
      Double rating = getRating(parser
          .extractAllNodesThatMatch(new TagNameFilter("img")));
      parser.reset();
      String[] str = getProsConsAndContent(parser
          .extractAllNodesThatMatch(new AndFilter(new TagNameFilter("span"),
              new HasAttributeFilter("class", "rkr"))));
      int pos = reviewLink.indexOf("content_");
      String reviewId = reviewLink.substring(pos + "content_".length());
      int reviewPos = reviewLink.indexOf("review/");
      String productId = reviewLink.substring(reviewPos + "review/".length(),
          pos - 1);
      return new ReviewWithProsAndCons(reviewId, productId, rating, str[2],
          str[0], str[1]);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Gets the pros, cons, and content of a review.
   * 
   * @param list
   * @return a 3-element array containing pros, cons, and content in that order
   */
  private String[] getProsConsAndContent(NodeList list) {
    String pros = "";
    String cons = "";
    String content = "";
    for (int i = 0; i < list.size(); i++) {
      String text = list.elementAt(i).toPlainTextString();
      if (text.contains("Pros:") && text.contains("Cons:")) {
        text = text.trim();
        int consPos = text.indexOf("Cons:");
        pros = unescapeAndNormalizeText(text.substring("Pros: ".length(),
            consPos));
        int endOfCons = text.indexOf("The Bottom Line: ");
        if (endOfCons == -1) {
          endOfCons = text.length();
        }
        cons = unescapeAndNormalizeText(text.substring(
            consPos + "Cons: ".length(), endOfCons));
      } else if (text.contains("Recommended:")) {
        int pos = text.indexOf("Recommended:");
        content = unescapeAndNormalizeText(text.substring(0, pos));
      }
    }

    return new String[] { pros, cons, content };
  }

  /**
   * Unescapes and normalizes the text by trimming, removing end-of-line
   * characters.
   * 
   * @param text
   * @return
   */
  private String unescapeAndNormalizeText(String text) {
    return StringEscapeUtils.unescapeHtml4(text.trim()).replaceAll(
        "[\\t\\n\\f\\r]", " ");
  }

  /**
   * Gets the review rating of a review based on the stars image.
   * 
   * @param list
   * @return
   */
  private Double getRating(NodeList list) {
    // A review page often contains two img links that contain .*_big_stars.gif
    // The second one contains the review's rating
    boolean nextIsSecond = false;
    for (int i = 0; i < list.size(); i++) {
      String src = getAttribute(list.elementAt(i), "src");
      if (src.contains("_big_stars.gif")) {
        if (!nextIsSecond) {
          nextIsSecond = true;
        } else {
          int lastSlash = src.lastIndexOf("/");
          return Double
              .parseDouble(src.substring(lastSlash + 1, lastSlash + 2));
        }
      }
    }

    return -1.0;
  }

  /**
   * Returns the value of the attribute named <code>attr</code> in the
   * <code>node</code>.
   * 
   * @param node
   * @param attr
   * @return an empty string if the attribute is not contained in the node
   */
  private String getAttribute(Node node, String attr) {
    TagNode tag = (TagNode) node;
    String attribute = tag.getAttribute(attr);
    return attribute != null ? attribute : "";
  }

  /**
   * Each small thread can be used to crawl reviews of products from a products
   * listing page.
   * 
   * @author trung
   */
  public static class SmallThread extends Thread {
    String outputDir;
    private String categoryUrl;
    private String categoryName;
    int page;

    public SmallThread(String outputDir, String categoryName, int page,
        String category) {
      this.outputDir = outputDir;
      this.categoryName = categoryName;
      this.page = page;
      this.categoryUrl = category;
    }

    @Override
    public void run() {
      EpinionsReviewsCollector collector = new EpinionsReviewsCollector();
      collector.crawl(outputDir, categoryName, page, categoryUrl);
    }
  }

  public static void main(String args[]) {
    // collect 50 pages (which gives ~ 50 x 15 products)
    // sample page:
    // http://www.epinions.com/Coffee_and_Espresso_Makers--coffee_maker/sec_~product_list/pp_~1#list
    String coffeeMaker = "http://www.epinions.com/Coffee_and_Espresso_Makers--coffee_maker/sec_~product_list/pp_~";
    String bagVacuum = "http://www.epinions.com/Vacuums--bag_filtration/sec_~product_list/pp_~";
    // EpinionsReviewsCollector collector = new EpinionsReviewsCollector();
    int numPages = 50;
    for (int page = 1; page <= numPages; page++) {
      String listingPage = coffeeMaker + page + "#list";
      new SmallThread("C:/datasets/epinions/coffeemaker", "coffeemaker", page,
          listingPage).start();
      listingPage = bagVacuum + page + "#list";
      new SmallThread("C:/datasets/epinions/vacuum", "vacuum", page,
          listingPage).start();
    }
  }
}
