package edu.kaist.uilab.bs.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.kaist.uilab.bs.NgramsCounter;

/**
 * Explores the data sets to find appropriate methods for summarization.
 * <p>
 * Specifically, determine, for each data set, (probably) the smallest n-grams
 * that can be used for meaningful summarization.
 * 
 * @author trung
 */
public class DatasetsExplorer {

  public static void printNgrams(String corpusFile, int n, String ngramsFile)
      throws IOException {
    NgramsCounter counter = new NgramsCounter(n);
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(corpusFile), "utf-8"));
    while (in.readLine() != null) {
      in.readLine();
      counter.incrementCount(DocumentUtils.removesNonAlphabets(in.readLine()),
          null);
    }
    in.close();
    counter.printNgramsCounter(ngramsFile);
  }

  public static void main(String args[]) throws IOException {
    String dir = "C:/datasets/bs/";
    String[] names = {"electronics", "ursa", "movies"};
    String ngramsFile = "ngrams";
    for (String name : names) {
      System.out.println("Exploring " + name);
      String dataset = dir + "/" + name;
      String corpusFile =  dataset + "/docs.txt";
      for (int n = 1; n < 6; n++) {
        System.out.println("Counting " + n + "-grams");
        printNgrams(corpusFile, n, dataset + "/" + ngramsFile + n + ".csv");
      }
    }
  }
}
