package edu.kaist.uilab.bs.evaluation;

import java.util.ArrayList;
import java.util.HashSet;

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
   * @param recallThreshold
   *          the minimum recall that a candidate segment must match a reference
   *          in order for the candidate to be counted as correct (true)
   *          instance
   * @param epsilon
   *          a range where a candidate is considered matching a reference
   *          summary (i.e., allow multiple references to be matched rather than
   *          only the one with highest recall)
   * @return an array containing precision, recall, f1, s-precision, and s-
   *         recall score in that order
   */
  public double[] computeRougeSU(ArrayList<String[]> refSummary,
      ArrayList<String[]> candidateSummary, int skipDist,
      double recallThreshold, double epsilon) {
    double[] totalScores = { 0.0, 0.0, 0.0, 0.0, 0.0 };
    HashSet<Integer> matchedReferences = new HashSet<Integer>();
    int cntPositiveCandidates = 0;
    final int recallIdx = 1;
    int numRefs = refSummary.size();
    int numCands = candidateSummary.size();
    // compute ROUGE-su for each unit
    for (String[] y : candidateSummary) {
      double[] maxUnitScores = { 0.0, 0.0, 0.0 }; // prec, rec, f1
      double[] rskip = new double[numRefs];
      ArrayList<Bigram> yBigrams = getBigrams(addDummyMarker(y), skipDist);
      for (int idx = 0; idx < numRefs; idx++) {
        double[] unitScores = computeRougeSU(
            getBigrams(addDummyMarker(refSummary.get(idx)), skipDist), yBigrams);
        rskip[idx] = unitScores[recallIdx];
        // choose the best reference based on recall -- rskip2
        // skip2(y) = argmax skip2(x, y) for x in R
        if (maxUnitScores[recallIdx] < unitScores[recallIdx]) {
          for (int i = 0; i < maxUnitScores.length; i++) {
            maxUnitScores[i] = unitScores[i];
          }
        }
      }
      for (int i = 0; i < 3; i++) {
        totalScores[i] += maxUnitScores[i];
      }
      // TODO(trung): re-factor
      if (maxUnitScores[recallIdx] >= recallThreshold) {
        // rskip(y) > recallThreshold
        for (int idx = 0; idx < refSummary.size(); idx++) {
          if (maxUnitScores[recallIdx] - epsilon < rskip[idx]) {
            matchedReferences.add(idx);
          }
        }
        cntPositiveCandidates++;
      }
    }

    for (int i = 0; i < 3; i++) {
      totalScores[i] = totalScores[i] / candidateSummary.size();
    }

    // coverage s-prec = # positive candidates / # total candidates
    // coverage s-recall = # matched refs / # total refs
    totalScores[totalScores.length - 2] = ((double) cntPositiveCandidates)
        / numCands;
    totalScores[totalScores.length - 1] = ((double) matchedReferences.size())
        / numRefs;

    return totalScores;
  }

  /**
   * Computes skip2 scores for 2 segments.
   * 
   * @param xBigrams
   *          the reference segment
   * @param yBigrams
   *          the candidate segment
   * @return
   */
  private double[] computeRougeSU(ArrayList<Bigram> xBigrams,
      ArrayList<Bigram> yBigrams) {
    double skip2 = countCommonElements(xBigrams, yBigrams);
    double pskip2 = skip2 / yBigrams.size();
    double rskip2 = skip2 / xBigrams.size();
    double fskip2 = (1 + BETA * BETA) * rskip2 * pskip2
        / (rskip2 + BETA * BETA * pskip2);

    return new double[] { pskip2, rskip2, fskip2 };
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
    refSummary.add(new String[] { "difficult", "to", "use", "under", "some",
        "furniture" });
    refSummary.add(new String[] { "food" });
    ArrayList<String[]> candidateSummary = new ArrayList<String[]>();
    candidateSummary.add(new String[] { "difficult", "to", "use" });
    // candidateSummary.add(new String[]{"good", "food"});
    double[] score = rouger.computeRougeSU(refSummary, candidateSummary, 2,
        0.2, 0.1);
    for (double s : score) {
      System.out.printf("%.3f ", s);
    }
  }
}
