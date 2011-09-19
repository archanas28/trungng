package edu.kaist.uilab.bs.evaluation;

import java.util.ArrayList;

/**
 * Class for computing the ROUGE evaluation scores.
 * 
 * @author trung
 */
public class Rouger {

  static final String DUMMY = " ";
  static final double BETA = 1.0;

  /**
   * Returns the ROUGE-SU score for a candidate summary C using a reference
   * summary R.
   * <p>
   * The ROUGE-SU score for a summary is the average ROUGE-SU of its summary
   * units (i.e., sentences or text fragments).
   * 
   * @param refSummary
   *          the reference summary R
   * @param candidateSummary
   *          the candidate summary C
   * @param skipDist
   *          the maximum skip distance
   * @return an array containing precision, recall, and f1 score in that order
   */
  public double[] computeRougeSU(ArrayList<String[]> refSummary,
      ArrayList<String[]> candidateSummary, int skipDist) {
    double[] totalScores = { 0.0, 0.0, 0.0 };
    // compute ROUGE-su for each unit
    for (String[] y : candidateSummary) {
      double[] maxUnitScores = { 0.0, 0.0, 0.0 };
      ArrayList<Bigram> yBigrams = getBigrams(addDummyMarker(y), skipDist);
      for (String[] x : refSummary) {
        ArrayList<Bigram> xBigrams = getBigrams(addDummyMarker(x), skipDist);
        double skip2 = countCommonElements(xBigrams, yBigrams);
        double pskip2 = skip2 / yBigrams.size();
        double rskip2 = skip2 / xBigrams.size();
        double fskip2 = (1 + BETA * BETA) * rskip2 * pskip2
            / (rskip2 + BETA * BETA * pskip2);
        double[] unitScores = { pskip2, rskip2, fskip2 };
        // choose the best reference based on recall -- rskip2
        // skip2(y) = argmax skip2(x, y) for x in R
        if (maxUnitScores[2] < unitScores[2]) {
          for (int idx = 0; idx < maxUnitScores.length; idx++) {
            maxUnitScores[idx] = unitScores[idx];
          }
        }
      }
      for (int idx = 0; idx < totalScores.length; idx++) {
        totalScores[idx] += maxUnitScores[idx];
      }
    }

    for (int idx = 0; idx < totalScores.length; idx++) {
      totalScores[idx] = totalScores[idx] / candidateSummary.size();
    }

    return totalScores;
  }

  /**
   * Returns an array with a begin-of-sentence marker added to the beginning of
   * <code>x</code>.
   * 
   * @param x
   * @return
   */
  String[] addDummyMarker(String[] x) {
    String[] ret = new String[x.length + 1];
    ret[0] = DUMMY;
    for (int i = 0; i < x.length; i++) {
      ret[i + 1] = x[i];
    }

    return ret;
  }

  /**
   * Returns the list of bi-grams within some distance in x.
   * 
   * @param x
   * @param skipDistance
   *          maximum distance between 2 words of the bi-grams
   * @return
   */
  ArrayList<Bigram> getBigrams(String[] x, int skipDistance) {
    ArrayList<Bigram> list = new ArrayList<Bigram>();
    for (int start = 0; start < x.length - 1; start++) {
      for (int offset = 1; offset <= skipDistance; offset++) {
        if (start + offset < x.length) {
          list.add(new Bigram(x[start], x[start + offset]));
        }
      }
    }

    return list;
  }

  /**
   * Returns the number of common elements in 2 lists.
   * 
   * @param <T>
   * @param list1
   * @param list2
   * @return
   */
  <T> int countCommonElements(ArrayList<T> list1, ArrayList<T> list2) {
    int cnt = 0;
    for (T elem : list1) {
      if (list2.contains(elem)) {
        cnt++;
      }
    }

    return cnt;
  }

  /**
   * A bigram.
   */
  static final class Bigram {
    String left;
    String right;

    public Bigram(String left, String right) {
      this.left = left;
      this.right = right;
    }

    @Override
    public boolean equals(Object o) {
      Bigram that = (Bigram) o;
      return left.equals(that.left) && right.equals(that.right);
    }

    @Override
    public String toString() {
      return left + " " + right;
    }
  }

  public static void main(String args[]) {
    Rouger rouger = new Rouger();
    ArrayList<String[]> refSummary = new ArrayList<String[]>();
    refSummary.add(new String[]{"difficult", "to", "use", "under", "some", "furniture"});
    refSummary.add(new String[]{"food"});
    ArrayList<String[]> candidateSummary = new ArrayList<String[]>();
    candidateSummary.add(new String[]{"difficult", "to", "use"});
    candidateSummary.add(new String[]{"good", "food"});
    double[] score = rouger.computeRougeSU(refSummary, candidateSummary, 2);
    for (double s : score) {
      System.out.printf("%.3f ", s);
    }
  }
}
