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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import edu.kaist.uilab.stemmers.EnglishStemmer;

/**
 * Utilities for various tasks.
 */
public class MiscUtils {
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
        // word = word + " " + map.get(stem);
      }
      map.put(stem, word);
    }
    in.close();
    return map;
  }

  static void convertStemToWords() throws IOException {
    // String dir =
    // "C:/datasets/asc/blogs/obama/(2)Stemmed-InitY0(V4)-T20-S2-A0.10-I2000(rf-0.10)/2000";
    String dir = "C:/datasets/asc/reviews/MovieReviews/Stemmed-InitY0(V4)-T30-S2-A0.10-I2000(rf-0.10)/2000";
    String stemDir = "C:/datasets/asc/reviews/MovieReviews";
    // String stemDir = "C:/datasets/asc/reviews/MovieReviews";
    stemToWords(dir + "/TopWords.csv", stemDir + "/Stem_en.txt", stemDir
        + "/Stem_fr.txt");
    stemToWords(dir + "/TopWordsByHalfTermScore.csv", stemDir + "/Stem_en.txt",
        stemDir + "/Stem_fr.txt");
    stemToWords(dir + "/TopWordsByTermScore.csv", stemDir + "/Stem_en.txt",
        stemDir + "/Stem_fr.txt");
  }

  static void convertWordsToStems() throws IOException {
    // Note that for MovieReviews dataset, SentiWords-0_fr.txt is encoded in
    // ANSI but not utf-8
    String dir = "C:/datasets/asc/ldatest/ElectronicsReviews2";
    // Utils.wordsToStems(dir + "/SentiWords-0_en.txt", dir
    // + "/SentiStems-0_en.txt", "utf-8", new EnglishStemmer());
    // Utils.wordsToStems(dir + "/SentiWords-1_en.txt", dir
    // + "/SentiStems-1_en.txt", "utf-8", new EnglishStemmer());
    // Utils.wordsToStems(dir + "/SentiWords-0_fr.txt", dir
    // + "/SentiStems-0_fr.txt", "utf-8", new FrenchStemmer());
    Utils.wordsToStems(dir + "/en.txt", dir
        + "/enstem.txt", "utf-8", new EnglishStemmer());
  }

  /*
   * Gets polarity words from the MPQA database and output to the specified
   * file.
   */
  static void getPolarityWords(String output) throws Exception {
    String docs = "C:/datasets/asc/mpqa.2.0/docs";
    String annotations = "C:/datasets/asc/mpqa.2.0/man_anns";
    String annotationFile = "gateman.mpqa.lre.2.0";
    File dir = new File(docs);
    HashMap<String, HashSet<String>> map = new HashMap<String, HashSet<String>>();
    for (File dateDir : dir.listFiles()) {
      String docName = dateDir.list()[0];
      String docContent = TextFiles.readFile(dateDir.getPath() + "/" + docName);
      BufferedReader in = new BufferedReader(new FileReader(annotations + "/"
          + dateDir.getName() + "/" + docName + "/" + annotationFile));
      String line, words;
      String field[];
      int pos, start, end;
      while ((line = in.readLine()) != null) {
        if (!line.startsWith("#")
            && line.contains("polarity")
            && (line.contains("GATE_expressive-subjectivity") || line
                .contains("GATE_direct-subjective"))) {
          field = line.split("\t");
          pos = field[1].indexOf(",");
          start = Integer.parseInt(field[1].substring(0, pos));
          end = Integer.parseInt(field[1].substring(pos + 1));
          words = docContent.substring(start, end);
          String attributes = field[field.length - 1];
          // find the substring polarity="xxx"
          pos = attributes.indexOf("polarity");
          start = attributes.indexOf("\"", pos);
          end = attributes.indexOf("\"", start + 1) + 1;
          String polarity = attributes.substring(start, end);
          HashSet<String> set = map.get(polarity);
          if (set == null) {
            set = new HashSet<String>();
            map.put(polarity, set);
          }
          for (String word : words.split(" ")) {
            if (word.length() > 2) {
              set.add(word);
            }
          }
        }
      }
      in.close();
    }
    PrintWriter out = new PrintWriter(output);
    for (Entry<String, HashSet<String>> entry : map.entrySet()) {
      out.print(entry.getKey() + ",");
      for (String word : entry.getValue()) {
        out.printf("%s,", word);
      }
      out.println();
    }
    out.close();
  }

  /**
   * Reads the polarity file (provided by Pang & Lee) and output the ranked list
   * of words for the given polarity.
   * 
   * @param sentiFile
   * @param outputFile
   */
  static void printTopWords(String sentiFile, String outputFile)
      throws Exception {
    List<String> sentences = TextFiles.readLinesAsLowerCase(sentiFile);
    HashMap<String, Integer> wordCnt = new HashMap<String, Integer>();
    for (String sentence : sentences) {
      String words[] = sentence.split("[ \t,.;!]");
      for (String word : words) {
        Integer count = wordCnt.get(word);
        if (count == null) {
          wordCnt.put(word, 1);
        } else {
          wordCnt.put(word, count + 1);
        }
      }
    }
    TreeSet<Word> set = new TreeSet<Word>();
    for (Entry<String, Integer> entry : wordCnt.entrySet()) {
      set.add(new Word(entry.getKey(), entry.getValue()));
    }
    TextFiles.writeCollection(set.descendingSet(), outputFile, UTF8);
  }

  static class Word implements Comparable<Word> {
    String mValue;
    int mCount;

    public Word(String value, int count) {
      mValue = value;
      mCount = count;
    }

    @Override
    public int compareTo(Word other) {
      if (mCount == other.mCount) {
        return mValue.compareTo(other.mValue);
      } else {
        return mCount - other.mCount;
      }
    }

    @Override
    public String toString() {
      return mValue + "," + mCount;
    }
  }

  // convert the european parliament congress (fr-en pair) to documents.
  static void europarlToDocuments() throws Exception {
    BufferedReader in = new BufferedReader(
        new InputStreamReader(new FileInputStream(
            "C:/Users/trung/Downloads/fr-en.txt"), "ISO-8859-1"));
    PrintWriter out = new PrintWriter("europarl.txt");
    int cnt = 0;
    String line;
    do {
      line = in.readLine();
    } while (!line.contains("<CHAPTER ID=2>"));
    do {
      // start of a new session (ignore chapter 1)
      if (line.contains("<CHAPTER ID=2>")) {
        cnt++;
        System.out.println("Session " + cnt);
        // find the first speaker
        do {
          line = in.readLine();
        } while (!line.contains("<SPEAKER ID="));
        HashMap<String, String> map = new HashMap<String, String>();
        // read each speaker's speech until a new chapter
        String speakerName = null;
        do {
          if (line.contains("<SPEAKER ID=")) {
            int pos = line.indexOf("NAME=");
            speakerName = line.substring(pos + "NAME=".length() + 1,
                line.length() - 2);
          } else if (!line.contains("<P>") && !line.contains("CHAPTER ID=")) {
            String speech = map.get(speakerName);
            if (speech == null) {
              map.put(speakerName, line);
            } else {
              map.put(speakerName, speech + " " + line);
            }
          }
          line = in.readLine();
        } while (line != null && !line.contains("<CHAPTER ID=2>"));
        // write each speaker as a document
        map.remove("President");
        map.remove("Le Président");
        for (String value : map.values()) {
          out.println(value);
        }
      }
    } while (line != null);
    in.close();
    out.close();
  }

  static void toUtf8Encoding(String file) throws Exception {
    List<String> lines = TextFiles.readLines(file);
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), UTF8));
    for (String line : lines) {
      out.println(line);
    }
    out.close();
  }

  static void writeDocuments(String originalFile, String subsetFile, int numDocs)
      throws Exception {
    List<String> lines = TextFiles.readLines(originalFile, "utf-8");
    HashSet<Integer> selectedIdx = new HashSet<Integer>();
    do {
      int idx = (int) (Math.random() * lines.size());
      if (selectedIdx.add(idx)) {
        numDocs--;
      }
    } while (numDocs > 0);
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(subsetFile), "utf-8"));
    for (Integer idx : selectedIdx) {
      out.println(idx); // ignore source
      out.println(-1); // ignore rating
      out.println(lines.get(idx));
    }
    out.close();
  }

  // convert all yelp reviews into a corpus file
  static void yelpReviewsToCorpus() throws IOException {
    File yelpDir = new File("C:/datasets/bs/Yelp");
    PrintWriter out = new PrintWriter("C:/datasets/bs/restaurants/docs.txt");
    for (File file : yelpDir.listFiles()) {
      BufferedReader in = new BufferedReader(new FileReader(file));
      String id = in.readLine().substring("ReviewID: ".length());
      System.out.println(id);
      in.readLine(); // ignore reviewer id
      String rating = in.readLine().substring("Rating: ".length());
      in.readLine(); // ignore restaurant
      in.readLine(); // ignore category
      out.println(id);
      out.println(rating);
      out.println(in.readLine()); // review content
      in.close();
    }
    out.close();
  }
  
  public static void main(String args[]) throws Exception {
    yelpReviewsToCorpus();
//    String dir = "C:/datasets/asc/ldatest/europarl";
//    writeDocuments(dir + "/data/europarl_en.txt", dir + "/docs_en3000.txt", 3000);
//    writeDocuments(dir + "/data/europarl_fr.txt", dir + "/docs_other3000.txt", 3000);
  }
}
