package edu.kaist.uilab.asc.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import edu.kaist.uilab.stemmer.SnowballStemmer;

/**
 * Utility class.
 */
public class Utils {

  /**
   * Converts a text file of words into a text file of stems.
   * 
   * @param wordFile
   *          the text file that must contain each word per line
   * @param stemFile
   *          the output file to store stems
   * @param encoding
   *          encoding of the <code>wordFile</code>
   * @param stemmer
   *          a stemmer (specific to the language in text file)
   * @throws IOException
   */
  public static void wordsToStems(String wordFile, String stemFile,
      String encoding, SnowballStemmer stemmer) throws IOException {
    BufferedReader in;
    if (encoding != null) {
      in = new BufferedReader(new InputStreamReader(new FileInputStream(
          wordFile), encoding));
    } else {
      in = new BufferedReader(new InputStreamReader(new FileInputStream(
          wordFile)));
    }
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(stemFile), "utf-8"));
    String line;
    while ((line = in.readLine()) != null) {
      out.println(stemmer.getStem(line));
    }
    in.close();
    out.close();
  }

  /**
   * Returns the indices of top <code>n</code> columns in the specified row
   * <code>row</code>.
   * 
   * @param matrix
   * @param row
   * @param n
   * @return
   */
  public static int[] topColumns(double[][] matrix, int row, int n) {
    int[] res = new int[n];
    boolean[] isCollected = new boolean[matrix[row].length];
    for (int i = 0; i < isCollected.length; i++) {
      isCollected[i] = false;
    }
    for (int i = 0; i < n; i++) {
      double max = Double.MIN_VALUE;
      int maxIdx = -1;
      for (int col = 0; col < matrix[row].length; col++) {
        if (matrix[row][col] > max && !isCollected[col]) {
          max = matrix[row][col];
          maxIdx = col;
        }
      }
      res[i] = maxIdx;
      isCollected[maxIdx] = true;
    }
    return res;
  }

  public static double calculateCosineSimilarity(List<Double> set1,
      List<Double> set2) {
    double ret = 0;

    double[] s1 = new double[set1.size()];
    double[] s2 = new double[set2.size()];

    for (int i = 0; i < s1.length; i++)
      s1[i] = set1.get(i);
    for (int i = 0; i < s2.length; i++)
      s2[i] = set2.get(i);

    double upper = 0;
    for (int i = 0; i < s1.length; i++) {
      upper += s1[i] * s2[i];
    }

    double lower = 0;
    double magnitude1 = 0, magnitude2 = 0;

    for (int i = 0; i < s1.length; i++)
      magnitude1 += s1[i] * s1[i];
    magnitude1 = Math.sqrt(magnitude1);
    for (int i = 0; i < s2.length; i++)
      magnitude2 += s2[i] * s2[i];
    magnitude2 = Math.sqrt(magnitude2);

    lower = magnitude1 * magnitude2;

    ret = upper / lower;

    return ret;
  }

  public static List<Double> normalizeTopicDistribution(List<Double> weights) {
    List<Double> ret = new ArrayList<Double>();

    double sum = 0;
    for (int i = 0; i < weights.size(); i++)
      sum += weights.get(i).doubleValue();

    for (int i = 0; i < weights.size(); i++)
      ret.add(weights.get(i).doubleValue() / sum);

    return ret;
  }

  public static double calculateMSEbetweenTopicDistributions(List<Double> t1,
      List<Double> t2) {
    double ret = 0;

    for (int i = 0; i < t1.size(); i++) {
      double error = t1.get(i).doubleValue() - t2.get(i).doubleValue();
      ret += error * error;
    }

    double size = t1.size();
    ret = ret / size;

    return ret;
  }

  public static int[] createRandomSequence(int size, int seed, int iteration) {
    int[] ret = new int[size];

    for (int i = 0; i < ret.length; i++)
      ret[i] = i;

    Random rand = new Random(seed);
    for (int i = 0; i < iteration; i++) {
      int src = rand.nextInt(size);
      int tar = rand.nextInt(size);

      int temp = ret[src];
      ret[src] = ret[tar];
      ret[tar] = temp;
    }

    return ret;
  }

  /**
   * Reads the content of the specified file as a set of words where each line
   * corresponds to a word.
   * 
   * @param file
   *          the input file
   * @param charset
   *          the charset of the file
   * @return
   * @throws IOException
   */
  public static TreeSet<String> readWords(String file, String charset)
      throws IOException {
    TreeSet<String> words = new TreeSet<String>();
    String line;
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), charset));
    while ((line = reader.readLine()) != null) {
      words.add(line);
    }
    return words;
  }
}
