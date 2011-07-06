package edu.kaist.uilab.crawler.blogposts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * Helps to get data from spinn3r.
 * 
 * @author trung
 */
public class SpinnHelper {

  /* Collects post urls from Spinn3r datasets and writes to separate files. */
  static void getPostUrls(String file, String outputDir) throws IOException {
    HashMap<String, PrintWriter> domains = new HashMap<String, PrintWriter>();
    BufferedReader in = new BufferedReader(new FileReader(file));
    String line;
    String[] fields;
    PrintWriter out;
    int wordCount;
    while ((line = in.readLine()) != null) {
      fields = line.split(" ");
      try {
        wordCount = Integer.parseInt(fields[5]);
        if (wordCount > 70) {
          out = domains.get(fields[0]); // blog domain
          if (out == null) {
            out = new PrintWriter(outputDir + "/" + fields[0] + ".txt");
            domains.put(fields[0], out);
          }
          if (fields[2].contains("http://")) {
            out.println(fields[2]);
            System.out.println(fields[2]);
          }
        }
      } catch (NumberFormatException e) {
        // do nothing
      }
    }
    in.close();
    for (PrintWriter pw : domains.values()) {
      pw.close();
    }
  }

  public static void main(String args[]) throws Exception {
    String dir = "C:/datasets/asc";
    getPostUrls(dir + "/share/blog_posts.txt", dir + "/blogs/");
  }
}
