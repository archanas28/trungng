package edu.kaist.uilab.bs.evaluation;

import com.aliasi.util.ObjectToDoubleMap;

/**
 * Utility methods for evaluation.
 * 
 * @author trung
 */
public class EvaluationUtils {

  /**
   * Returns the cosine similarity between two distributions <code>p</code> and
   * <code>q</code>.
   * 
   * @param p
   * @param q
   * @return
   */
  public static double cosineSimilarity(ObjectToDoubleMap<String> p,
      ObjectToDoubleMap<String> q) {
    double numerator = 0.0, pMag = 0.0, qMag = 0.0;
    for (String key : p.keySet()) {
      double pi = p.getValue(key);
      double qi = q.getValue(key);
      numerator += pi * qi;
      pMag += pi * pi;
      qMag += qi * qi;
    }

    return numerator / Math.sqrt(pMag) / Math.sqrt(qMag);
  }

  /**
   * Computes the cosine similarity between two distributions <code>p</code> and
   * <code>q</code>.
   * 
   * @param p
   * @param q
   * @return
   */
  public static double cosineSimilarity(double[] p, double[] q) {
    double numerator = 0.0;
    for (int i = 0; i < p.length; i++) {
      numerator += p[i] * q[i];
    }
    double denominator = Math.sqrt(magnitude(p) * magnitude(q));
    return numerator / denominator;
  }

  /**
   * Returns the magnitude of the vector <code>p</code>.
   * 
   * @param p
   * @return
   */
  public static double magnitude(double[] p) {
    double mag = 0.0;
    for (int i = 0; i < p.length; i++) {
      mag += p[i] * p[i];
    }

    return Math.sqrt(mag);
  }

  /**
   * Returns the normalized vector of <code>v</code>.
   * 
   * @param v
   * @return
   */
  public static double[] normalizeVector(double[] v) {
    double length = magnitude(v);
    double[] ret = new double[v.length];
    for (int i = 0; i < v.length; i++) {
      ret[i] = v[i] / length;
    }

    return ret;
  }

  /**
   * Returns the normalized distribution of <code>p</code>.
   * 
   * @param p
   * @return
   */
  public static double[] normalizeDistribution(double[] p) {
    double sum = 0.0;
    for (int i = 0; i < p.length; i++) {
      sum += p[i];
    }
    double[] ret = new double[p.length];
    for (int i = 0; i < p.length; i++) {
      ret[i] = p[i] / sum;
    }

    return ret;
  }

  private static void printArray(double[] p) {
    System.out.print("[");
    for (int i = 0; i < p.length; i++) {
      System.out.printf("%.2f, ", p[i]);
    }
    System.out.println("]");
  }

  public static void main(String args[]) {
    double[] p = { 0.8, 3.2 };
    p = normalizeDistribution(p);
    System.out.println("Normalized distribution");
    printArray(p);
    p = new double[] { 1, 1 };
    p = normalizeVector(p);
    System.out.println("Normalized vector");
    printArray(p);
    System.out.print("Magnitude: " + magnitude(p));
  }
}
