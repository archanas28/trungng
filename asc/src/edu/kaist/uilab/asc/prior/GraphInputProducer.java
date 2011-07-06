package edu.kaist.uilab.asc.prior;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

/**
 * Construct the input file for {@link SimilarityGraph} from the vocabulary (of
 * all languages) and the language pairs.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class GraphInputProducer {

  private static final String UTF8 = "utf-8";

  String mFile;
  HashMap<String, Integer> map;
  ArrayList<String> mList;

  /**
   * Constructor
   * 
   * @param vocabFile
   *          the file that contains all words
   * @param list
   *          the list of language pair files
   */
  public GraphInputProducer(String vocabFile, ArrayList<String> list) {
    mFile = vocabFile;
    mList = list;
    map = new HashMap<String, Integer>();
  }

  /**
   * Constructor
   * 
   * @param vocabFile
   *          the file that contains all words
   * @param langPair
   *          the language pair file
   */
  public GraphInputProducer(String vocabFile, String langPair) {
    mFile = vocabFile;
    mList = new ArrayList<String>();
    mList.add(langPair);
    map = new HashMap<String, Integer>();
  }

  /**
   * Writes all edges of the graph to the specified output file.
   * 
   * @param output
   *          the output file
   */
  public void write(String output, String delimiter) throws IOException {
    System.out.println("Constructing similarity graph...");
    readWords(mFile);
    PrintWriter out = new PrintWriter(output);
    for (String langPair : mList) {
      processLangPair(langPair, out, delimiter);
    }
    out.close();
    System.out.println("Graph constructed.");
  }

  /**
   * Converts the given string <code>pair</code> to two strings separated by one
   * of the space character.
   * 
   * @param pair
   * @param word
   */
  void toWordPair(String pair, String[] word) {
    StringTokenizer tokenizer = new StringTokenizer(pair);
    word[0] = tokenizer.nextToken();
    word[1] = tokenizer.nextToken();
  }
  
  /**
   * Reads a language pair file and creates an edge for each unique pair of
   * words.
   * <p>
   * Also writes the edges to the specified writer <code>out</code>.
   * 
   * @param pairFile
   *          the language pair file
   * @param out
   *          the writer
   */
  void processLangPair(String pairFile, PrintWriter out, String delimiter)
      throws IOException {
    System.out.printf("\nReading language pair %s...", pairFile);
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(pairFile), UTF8));
    String pair;
    String[] word = new String[2];
    Integer idx1, idx2;
    HashSet<String> set = new HashSet<String>();
    String s;
    while ((pair = in.readLine()) != null) {
      toWordPair(pair, word);
      idx1 = map.get(word[0]);
      idx2 = map.get(word[1]);
      if (idx1 != null && idx2 != null && !idx1.equals(idx2)) {
        s = idx1 < idx2 ? String.format("%d%s%d", idx1, delimiter, idx2)
            : String.format("%d%s%d", idx2, delimiter, idx1);
        if (set.add(s)) {
          out.println(s);
        }
      }
    }
    in.close();
    System.out.println("done");
  }

  /**
   * Reads all words from the specified file.
   * 
   * @param file
   */
  void readWords(String file) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), UTF8));
    String line;
    int idx = 0;
    while ((line = in.readLine()) != null) {
      if (line != "") {
        map.put(line, idx++);
      }
    }
    in.close();
  }
}
