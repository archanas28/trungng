package edu.kaist.uilab.asc;

import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.IntegerMatrix;

/**
 * Posterior inference.
 */
public class Inference {

  public static DoubleMatrix[] calculatePhi(IntegerMatrix[] matrixSWT,
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

  public static double[][][] calculateTheta(IntegerMatrix[] matrixSDT,
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
