package edu.kaist.uilab.asc.crawler.blogposts;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.util.NodeList;

/**
 * Collector for getting posts from livejournal.com.
 * 
 * @author trung
 */
public class LivejournalCollectorImpl implements PostCollectorInterface {

  @Override
  public String getPost(String url) {
    String content = "";
    try {
      Parser parser = new Parser(url);
      NodeList nl = parser.extractAllNodesThatMatch(new HasAttributeFilter(
          "class", "entry-content"));

      StringBuilder builder = new StringBuilder();
      if (nl.size() == 1) {
        Node node = nl.elementAt(0);
        nl.removeAll();
        node.collectInto(nl, new NodeClassFilter(TextNode.class));
        for (int i = 0; i < nl.size(); i++) {
          node = nl.elementAt(i);
          builder.append(node.toPlainTextString());
          if (node.getNextSibling() instanceof Tag) {
            String tagName = ((Tag) node.getNextSibling()).getTagName();
            if (tagName.equalsIgnoreCase("br")
                || tagName.equalsIgnoreCase("br /")
                || tagName.equalsIgnoreCase("div")
                || tagName.equalsIgnoreCase("p")
                || tagName.equalsIgnoreCase("span")) {
              builder.append(".\n"); // to separate paragraphs
            }
          }
        }
        content = PostCollector.removeFooter(builder.toString())
            .replaceAll("[\\t|\\f]", " ").replaceAll("&#[\\d]+;", "")
            .replaceAll("[http|ftp]://[\\S]*", " ");
      }
    } catch (Exception e) {
      // do nothing
    }
    return content;
  }
}
