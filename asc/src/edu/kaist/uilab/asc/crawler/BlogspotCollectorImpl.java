package edu.kaist.uilab.asc.crawler;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeList;

/**
 * Collector for getting posts from blogspot.com.
 * @author trung
 *
 */
public class BlogspotCollectorImpl implements PostCollectorInterface {

  @Override
  public String getPost(String url) {
    String content = "";
    try {
      Parser parser = new Parser(url);
      NodeList nl = parser.extractAllNodesThatMatch(new HasAttributeFilter(
          "class", "post-body entry-content"));
      StringBuilder builder = new StringBuilder();
      if (nl.size() == 1) {
        Node node = nl.elementAt(0);
        nl = node.getChildren();
        for (int i = 0; i < nl.size(); i++) {
          node = nl.elementAt(i);
          builder.append(node.toPlainTextString());
          if (node instanceof Tag) {
            String tagName = ((Tag) node).getTagName();
            if (tagName.equalsIgnoreCase("br") || tagName.equalsIgnoreCase("div")
                || tagName.equalsIgnoreCase("p")
                || tagName.equalsIgnoreCase("span")) {
              builder.append("\n"); // to separate paragraphs
            }
          }
        }
        content = builder.toString().replaceAll("[\\t\\f]+", " ")
            .replaceAll("&#[\\d]+;", "").replaceAll("[http|ftp]://[\\S]*", " ");
      }
    } catch (Exception e) {
      // do nothing
    }
    
    return content;
  }
}
