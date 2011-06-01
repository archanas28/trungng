package edu.kaist.uilab.asc.crawler.blogposts;

/**
 * Interface for collecting blog posts.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public abstract class PostCollectorAbstract {
  
  public abstract String getPost(String url);

  /**
   * Regularizes the given <code>content</code> by removing special html
   * characters.
   * 
   * @param content
   * @return
   */
  public static String regularize(String content) {
    String[] regexs = { "[\\t\\f]+", "&#[\\d]+;", "&nbsp;",
        "[http|ftp]://[\\S]*", "<!--.*-->", "’" };
    String[] replacements = { " ", "", " ", " ", " ", "'" };
    for (int i = 0; i < regexs.length; i++) {
      content = content.replaceAll(regexs[i], replacements[i]);
    }
    return content;
  }
}
