package edu.kaist.uilab.asc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.StringTokenizer;

import edu.kaist.uilab.asc.stemmer.EnglishStemmer;
import edu.kaist.uilab.asc.stemmer.FrenchStemmer;

public class Test {
  private static final String UTF8 = "utf-8";

  static void amazonToCategories() throws IOException {
    String amazon = "C:/datasets/asc/ASUM/Amazon";
    String cats = "C:/datasets/asc/ASUM/Categories";
    String[] categories = new String[] { "MP3Players", "CoffeeMachines",
        "CanisterVacuums", };
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

  /**
   * Converts the stemmed CSV file to English or French file.
   * 
   * @param csvFile
   * @param enStemFile
   * @param frStemFile
   */
  static void stemToWords(String csvFile, String enStemFile, String frStemFile)
      throws IOException {
    File file = new File(csvFile);
    String outputFile = file.getParent() + "/NotStemmed-" + file.getName();
    System.out.println(outputFile);
    HashMap<String, String> enStemMap = getStemMap(enStemFile);
    HashMap<String, String> frStemMap = getStemMap(frStemFile);
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(outputFile), UTF8));
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(csvFile), UTF8));
    out.println(in.readLine()); // header
    String[] words;
    String line, stem;
    int pos;
    while ((line = in.readLine()) != null) {
      words = line.split(",");
      for (String word : words) {
        pos = word.indexOf(" ");
        stem = word.substring(0, pos);
        if (enStemMap.containsKey(stem)) {
          out.printf("%s,", enStemMap.get(stem) + word.substring(pos));
        } else if (frStemMap.containsKey(stem)) {
          out.printf("%s,", frStemMap.get(stem) + word.substring(pos));
        }
      }
      out.println();
    }
    in.close();
    out.close();
  }

  /**
   * Returns a map between words and their stems from the specified file.
   * 
   * @param stemFile
   * @return
   * @throws IOException
   */
  private static HashMap<String, String> getStemMap(String stemFile)
      throws IOException {
    HashMap<String, String> map = new HashMap<String, String>();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(stemFile), UTF8));
    String line;
    int pos;
    String word, stem;
    while ((line = in.readLine()) != null) {
      pos = line.indexOf(" ");
      stem = line.substring(pos + 1);
      word = line.substring(0, pos);
      if (map.containsKey(stem)) {
//        word = word + " " + map.get(stem);
      }
      map.put(stem, word);
    }
    in.close();
    return map;
  }
  
  static void convertStemToWords() throws IOException {
//  String dir = "C:/datasets/asc/reviews/ElectronicsReviews2/ElectronicsReviews2-Stemmed-InitY0(V2)-T15-S2-A0.10-I2000/2000";
    String dir = "C:/datasets/asc/reviews/MovieReviews/MovieReviews-Stemmed-InitY0(V3)-T10-S2-A0.10-I2000/1200";
//    String stemDir = "C:/datasets/asc/reviews/ElectronicsReviews2";
    String stemDir = "C:/datasets/asc/reviews/MovieReviews";
    stemToWords(dir + "/TopWords.csv", stemDir + "/Stem_en.txt", stemDir
        + "/Stem_fr.txt");
    stemToWords(dir + "/TopWordsByHalfTermScore.csv", stemDir + "/Stem_en.txt",
        stemDir + "/Stem_fr.txt");
    stemToWords(dir + "/TopWordsByTermScore.csv", stemDir + "/Stem_en.txt",
        stemDir + "/Stem_fr.txt");
  }

  public static void main(String args[]) throws Exception {
    convertStemToWords();
//    String dir = "C:/datasets/asc/reviews/MovieReviews";
//    Utils.wordsToStems(dir + "/SentiWords-0_en.txt", dir + "/SentiStems-0_en.txt",
//        null, new EnglishStemmer());
//    Utils.wordsToStems(dir + "/SentiWords-1_en.txt", dir + "/SentiStems-1_en.txt",
//        null, new EnglishStemmer());
//    Utils.wordsToStems(dir + "/SentiWords-0_fr.txt", dir + "/SentiStems-0_fr.txt",
//        null, new FrenchStemmer());
//    Utils.wordsToStems(dir + "/SentiWords-1_fr.txt", dir + "/SentiStems-1_fr.txt",
//        null, new FrenchStemmer());    
  }
}
