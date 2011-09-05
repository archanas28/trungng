package edu.kaist.uilab.asc;

import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.IntegerMatrix;

/**
 * Posterior inference.
 */
public class Inference {

  /**
   * Computes phi for the ASC model.
   * 
   * @param matrixSWT
   * @param sumSTW
   * @param beta
   * @param sumBeta
   * @return
   */
  public static DoubleMatrix[] computePhi(IntegerMatrix[] matrixSWT,
      int[][] sumSTW, double[][][] beta, double[][] sumBeta) {
    int numSenti = matrixSWT.length;
    int numWords = matrixSWT[0].getNumOfRow();
    int numTopics = matrixSWT[0].getNumOfColumn();
    DoubleMatrix[] phi = new DoubleMatrix[numSenti];
    for (int s = 0; s < numSenti; s++) {
      phi[s] = new DoubleMatrix(numWords, numTopics);
      for (int w = 0; w < numWords; w++) {
        for (int t = 0; t < numTopics; t++) {
          phi[s].setValue(w, t, (matrixSWT[s].getValue(w, t) + beta[s][t][w])
              / (sumSTW[s][t] + sumBeta[s][t]));
        }
      }
    }
    return phi;
  }

  /**
   * Computes phiSenti for the BS model.
   * 
   * @param cntSWT
   * @param sumSTW
   * @param beta
   * @param sumBeta
   * @return
   */
  public static DoubleMatrix[] computePhiSenti(IntegerMatrix[] cntSWT,
      int[][] sumSTW, double[][] beta, double[] sumBeta) {
    int numSenti = cntSWT.length;
    int numWords = cntSWT[0].getNumOfRow();
    int numTopics = cntSWT[0].getNumOfColumn();
    DoubleMatrix[] phi = new DoubleMatrix[numSenti];
    for (int j = 0; j < numSenti; j++) {
      phi[j] = new DoubleMatrix(numWords, numTopics);
      for (int i = 0; i < numWords; i++) {
        for (int k = 0; k < numTopics; k++) {
          phi[j].setValue(i, k, (cntSWT[j].getValue(i, k) + beta[j][i])
              / (sumSTW[j][k] + sumBeta[j]));
        }
      }
    }
    return phi;
  }

  /**
   * Computes phi aspect for the BS model.
   * 
   * @param cntWT
   * @param sumWT
   * @param beta
   * @param sumBeta
   * @return
   *      a <code>numTopics</code> by <code>numWords</code> double array
   */
  public static double[][] computePhiAspect(IntegerMatrix cntWT, int[] sumWT,
      double beta, double sumBeta) {
    int numWords = cntWT.getNumOfRow();
    int numTopics = cntWT.getNumOfColumn();
    double[][] phi = new double[numTopics][numWords];
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < numWords; i++) {
        phi[k][i] = (cntWT.getValue(i, k) + beta) / (sumWT[k] + sumBeta);
      }
    }
    
    return phi;
  }

  /**
   * Computes theta for the ASC model.
   * 
   * @param matrixSDT
   * @param sumDST
   * @param alpha
   * @param sumAlpha
   * @return
   */
  public static double[][][] computeTheta(IntegerMatrix[] matrixSDT,
      int[][] sumDST, double alpha, double sumAlpha) {
    int numSenti = matrixSDT.length;
    int numDocs = matrixSDT[0].getNumOfRow();
    int numTopics = matrixSDT[0].getNumOfColumn();
    double[][][] theta = new double[numSenti][][];
    for (int s = 0; s < numSenti; s++) {
      theta[s] = new double[numDocs][numTopics];
      for (int d = 0; d < numDocs; d++) {
        for (int t = 0; t < numTopics; t++) {
          theta[s][d][t] = (matrixSDT[s].getValue(d, t) + alpha)
              / (sumDST[d][s] + sumAlpha);
        }
      }
    }
    return theta;
  }

  /**
   * Computes theta for the BS model.
   * 
   * @param cntDT
   * @param sumDT
   * @param alpha
   * @param sumAlpha
   * @return
   */
  public static double[][] computeTheta(IntegerMatrix cntDT, int[] sumDT,
      double alpha, double sumAlpha) {
    int numDocs = cntDT.getNumOfRow();
    int numTopics = cntDT.getNumOfColumn();
    double[][] theta = new double[numDocs][numTopics];
    for (int d = 0; d < numDocs; d++) {
      for (int t = 0; t < numTopics; t++) {
        theta[d][t] = (cntDT.getValue(d, t) + alpha) / (sumDT[d] + sumAlpha);
      }
    }
    return theta;
  }

  /**
   * Computes pi for the ASC model.
   * 
   * @param matrixDS
   * @param sumDS
   * @param gammas
   * @param sumGamma
   * @return
   */
  public static DoubleMatrix calculatePi(IntegerMatrix matrixDS, int[] sumDS,
      double[] gammas, double sumGamma) {
    int numDocs = matrixDS.getNumOfRow();
    int numSenti = matrixDS.getNumOfColumn();

    DoubleMatrix Pi = new DoubleMatrix(numDocs, numSenti);

    for (int d = 0; d < numDocs; d++) {
      for (int s = 0; s < numSenti; s++) {
        double value = (matrixDS.getValue(d, s) + gammas[s])
            / (sumDS[d] + sumGamma);
        Pi.setValue(d, s, value);
      }
    }
    return Pi;
  }
}
