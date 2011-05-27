package edu.kaist.uilab.asc.crawler.blogposts;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

/**
 * Collector for getting posts from wordpress.com.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class WordpressCollectorImpl implements PostCollectorInterface {

  @Override
  public String getPost(String url) {
    StringBuilder builder = new StringBuilder();
    try {
      Parser parser = new Parser(url);
      NodeList list = parser.extractAllNodesThatMatch(new OrFilter(
          new HasAttributeFilter("class", "content"), new HasAttributeFilter(
              "id", "content")));
      for (int i = 0; i < list.size(); i++) {
        Node node = list.elementAt(i);
        NodeList nl = new NodeList();
        String content = "";
        node.collectInto(nl, new TagNameFilter("p"));
        for (int j = 0; j < nl.size(); j++) {
          content += nl.elementAt(j).toPlainTextString() + ".\n";
        }
        builder.append(PostCollector.removeFooter(content));
      }
    } catch (Exception e) {
      // do nothing
    }
    String result = builder.toString().replaceAll("[\\t|\\f]+", " ")
        .replaceAll("&#[\\d]+;", "").replaceAll("[http|ftp]://[\\S]*", " ");
    return result;
  }
}
