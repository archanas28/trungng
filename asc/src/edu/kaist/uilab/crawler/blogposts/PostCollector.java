package edu.kaist.uilab.crawler.blogposts;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeList;

import edu.kaist.uilab.asc.util.TextFiles;

/**
 * Class for collecting blog posts. TODO(trung): filter "opinionated" documents
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class PostCollector {

  static final String keywords[] = { "élection", "terroristes", "économie",
      "éducation", "immigration", "soins de santé", "environnement", "taxe",
      "leadership", "pakistan" };
  // String keywords[] = { "election", "terrorist", "economics", "education",
  // "immigration", "healthcare", "environment", "tax", "leadership",
  // "pakistan" };
  static final String domains[] = { "blogspot.com", "wordpress.com",
      "livejournal.com", };
  static List<String> patterns, replaces;

  static {
    try {
      patterns = TextFiles.readLines("reviews/patterns.txt");
      replaces = TextFiles.readLines("reviews/specialchars.txt");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String toFrench(String text) {
    for (int i = 0; i < patterns.size(); i++) {
      text = text.replaceAll(patterns.get(i), replaces.get(i));
    }
    return text;
  }

  /* Removes the footer section normally appears at the end of a post. */
  static String removeFooter(String content) {
    // String[] footer = { "Your email address", "You must be logged in",
    // "Posted on", "Submitted by", "Tags", };
    String[] footer = { "Votre adresse de messagerie ",
        "Vous devez être connecté", "Publié le", "Propos recueillis par",
        "Soumis par", "Posté par", "Tags", "Plus d'informations" };
    int pos = -1;
    for (String s : footer) {
      pos = content.indexOf(s);
      if (pos >= 0)
        break;
    }
    if (pos > 0) {
      return content.substring(0, pos);
    } else {
      return content;
    }
  }

  /**
   * Gets 10 search results (urls) from google blog search.
   */
  static ArrayList<String> getUrls(String query) {
    ArrayList<String> res = new ArrayList<String>();
    try {
      Parser parser = new Parser(query);
      parser.setEncoding("utf-8");
      NodeList list = parser.extractAllNodesThatMatch(new HasAttributeFilter(
          "id", "res"));
      Node resultNode = list.elementAt(0);
      list.removeAll();
      resultNode.collectInto(list, new TagNameFilter("a"));
      for (int i = 0; i < list.size(); i++) {
        String href = ((TagNode) list.elementAt(i)).getAttribute("href");
        if (href.startsWith("http://") && !href.contains(".google.com")) {
          res.add(href);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println(res);
    return res;
  }

  /**
   * Crawls the urls for a keyword from a specific blog domain.
   */
  static class DomainUrlCrawler extends Thread {
    private static final String DIR = "C:/datasets/asc/blogs";
    private String mKeyword;
    private String mDomain;

    public DomainUrlCrawler(String keyword, String domain) {
      mKeyword = keyword;
      mDomain = domain;
    }

    @Override
    public void run() {
      ArrayList<String> urls = new ArrayList<String>();
      try {
        for (int start = 0; start < 1000; start += 10) {
          String query = "http://www.google.com/search?hl=fr&lr=lang_fr&ie=UTF-8&q=barack+obama+%22"
              + URLEncoder.encode(mKeyword, "utf-8")
              + "%22+blogurl:"
              + mDomain
              + "&tbm=blg&start=" + start;
          urls.addAll(PostCollector.getUrls(query));
        }
        // TextFiles.writeCollection(urls,
        // String.format("%s/urls_en/%s.%s.txt", DIR, mKeyword, mDomain));
        TextFiles.writeCollection(urls,
            String.format("%s/urls_fr/%s.%s.txt", DIR, mKeyword, mDomain));
        System.err.printf("Urls saved to %s.%s.txt\n", mKeyword, mDomain);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Crawl posts.
   */
  static class PostCrawler extends Thread {
    private String urlFile;
    private String outputDir;
    private PostCollectorAbstract collector;

    public PostCrawler(String urlFile, String outputDir,
        PostCollectorAbstract collector) {
      this.urlFile = urlFile;
      this.outputDir = outputDir;
      this.collector = collector;
    }

    @Override
    public void run() {
      System.err.println("Started thread for " + urlFile);
      final int MIN_POST_LENGTH = 50;
      try {
        BufferedReader in = new BufferedReader(new InputStreamReader(
            new FileInputStream(urlFile), "utf-8"));
        int idx = 0;
        String line;
        PrintWriter out = null;
        int pos = urlFile.lastIndexOf("/");
        String subname = urlFile.substring(pos + 1);
        while ((line = in.readLine()) != null) {
          String content = collector.getPost(line);
          if (content.length() > MIN_POST_LENGTH) {
            System.out.println(line);
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
                outputDir + "/" + idx++ + subname), "utf-8"));
            out.printf("URL: %s\n", line);
            content = toFrench(removeShortParagraphs(content));
            out.print(content);
            out.close();
          }
        }
        in.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Removes short paragraphs from a blogpost.
   * 
   * @param post
   * @return
   */
  static String removeShortParagraphs(String post) {
    final int MIN_WORDS = 15;
    String[] paras = post.split("\n");
    StringBuilder builder = new StringBuilder();
    for (String para : paras) {
      int spaceCnt = 0;
      for (int i = 0; i < para.length(); i++) {
        if (para.charAt(i) == ' ') {
          spaceCnt++;
        }
      }
      if (spaceCnt > MIN_WORDS) {
        builder.append(para).append("\n");
      }
    }
    return builder.toString();
  }

  /* Gets urls for some test keywords */
  static void getUrls() {
    for (String keyword : keywords) {
      for (String domain : domains) {
        new DomainUrlCrawler(keyword, domain).start();
      }
    }
  }

  /* Gets a blogpost collector for the specified domain. */
  static PostCollectorAbstract getCollector(String domain) {
    if ("blogspot.com".equals(domain)) {
      return new BlogspotCollectorImpl();
    }
    if ("wordpress.com".equals(domain)) {
      return new WordpressCollectorImpl();
    }
    if ("livejournal.com".equals(domain)) {
      return new LivejournalCollectorImpl();
    }

    return null;
  }

  static void getBlogposts() {
    for (String keyword : keywords) {
      for (String domain : domains) {
        String filename = keyword + "." + domain + ".txt";
        // new PostCrawler("C:/datasets/asc/blogs/urls_en/" + filename,
        // "C:/datasets/asc/blogs/obama_en", getCollector(domain)).start();
        new PostCrawler("C:/datasets/asc/blogs/urls_fr/" + filename,
            "C:/datasets/asc/blogs/obama_fr", getCollector(domain)).start();
      }
    }
  }

  public static void main(String args[]) throws Exception {
//    getUrls();
//     getBlogposts();
    
  }
}
