package edu.kaist.uilab.asc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class Test {

  static void amazonToCategories() throws IOException {
    String amazon = "C:/datasets/asc/ASUM/Amazon";
    String cats = "C:/datasets/asc/ASUM/Categories";
    String[] categories = new String[] {
      "MP3Players", "CoffeeMachines", "CanisterVacuums", 
    };
    HashMap<String, PrintWriter> map = new HashMap<String, PrintWriter>();
    for (String c : categories) {
      map.put(c, new PrintWriter(cats + "/" + c + ".txt"));
    }
    File dir = new File(amazon);
    BufferedReader in;
    PrintWriter out;
    String docId, rating, line;
    StringBuilder builder;
    int pos;
    for (File review : dir.listFiles()) {
      in = new BufferedReader(new FileReader(review));
      docId = in.readLine(); // id
      pos = docId.indexOf(":") + 2;
      docId = docId.substring(pos);
      String category = in.readLine(); // category
      for (int i = 0; i < 5; i++) {
        // Product, ReviewerId, ReviewerName, Date, Helpful
        in.readLine();
      }
      // get rating
      rating = in.readLine();
      pos = rating.indexOf(":") + 2;
      rating = rating.substring(pos);
      in.readLine(); // title
      in.readLine(); // Content
      // get content
      builder = new StringBuilder();
      while ((line = in.readLine()) != null) {
        builder.append(line);
      }
      // get category
      pos = category.indexOf(":") + 2;
      category = category.substring(pos);
      out = map.get(category);
      if (out == null) {
        System.out.println(category);
        out = new PrintWriter(cats + "/" + category + ".txt");
        map.put(category, out);
      }
      out.println(docId);
      out.println(rating);
      out.println(builder.toString());
      in.close();
    }
    for (PrintWriter w : map.values()) {
      w.close();
    }
  }
  
  public static void main(String args[]) throws Exception {
    Integer a3 = new Integer(3);
    Integer another3 = 3;
    System.out.println(a3.equals(another3));
  }
}
