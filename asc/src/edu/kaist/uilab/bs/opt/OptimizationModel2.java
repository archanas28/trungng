package edu.kaist.uilab.bs.opt;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.math.special.Gamma;

import com.aliasi.symbol.SymbolTable;

import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.InvalidArgumentException;
import edu.kaist.uilab.asc.util.TextFiles;
import edu.kaist.uilab.bs.Document;
import edu.kaist.uilab.bs.Model;
import edu.kaist.uilab.bs.Sentence;
import edu.kaist.uilab.bs.TwogramsCounter;
import edu.kaist.uilab.opt.LBFGS;
import edu.kaist.uilab.opt.ObjectiveFunction;

/**
 * Models that optimize sentiment using
 * <code>beta_jki = exp(x_ji + y_jki)</code>.
 * 
 * @author trung
 */
public class OptimizationModel2 extends Model implements ObjectiveFunction {

  private static final long serialVersionUID = -1001800116325408901L;

  private double[][][] beta; // beta[senti][topic][word]
  private double[][] sumBeta; // sumBeta[senti][topic]
  double[][][] betaJki;

  // variables for optimization
  double[] vars;
  double[][][] y;
  double[][] x;
  double sigmaSquare = 0.5;

  public OptimizationModel2(int numTopics, int numSenti,
      SymbolTable sentiTable, SymbolTable aspectTable, TwogramsCounter counter,
      List<Document> documents, HashSet<Integer>[] seedWords, double alpha,
      double betaAspect, double[] betaSenti, double[] gammas) {
    super(numTopics, numSenti, sentiTable, aspectTable, counter, documents,
        seedWords, alpha, betaAspect, betaSenti, gammas);
    betaJki = new double[numSenti][numTopics][numSentiWords];
  }

  @Override
  protected void initBeta(double betaSenti[]) {
    x = new double[numSenti][numSentiWords];
    y = new double[numSenti][numTopics][numSentiWords];
    beta = new double[numSenti][numTopics][numSentiWords];
    sumBeta = new double[numSenti][numTopics];
    vars = new double[numSenti * numTopics * numSentiWords];
    initX();
    // double[][] priorBeta = getBetaSenti(betaSenti);

    // making sure that y and vars are consistent
    // in case we may change our initialization
    int varIdx = 0;
    for (int j = 0; j < numSenti; j++) {
      for (int k = 0; k < numTopics; k++) {
        for (int i = 0; i < numSentiWords; i++) {
          y[j][k][i] = 0;
          vars[varIdx++] = 0;
        }
      }
    }
    updateBeta();
  }

  /**
   * Inits x_ji using review ratings.
   */
  void initX() {
    extraInfo = "priorSentiment";
    int[][] counter = new int[numSenti][numSentiWords];
    for (Document doc : documents) {
      int polarity = -1;
      if (doc.getRating() > 3.0) {
        polarity = 0;
      } else if (doc.getRating() < 3.0) {
        polarity = 1;
      }
      if (polarity >= 0) {
        for (Sentence sentence : doc.getSentences()) {
          for (int sWord : sentence.getSentiWords()) {
            counter[polarity][sWord]++;
          }
        }
      }
    }
    for (int j = 0; j < numSenti; j++) {
      for (int i = 0; i < numSentiWords; i++) {
        if (counter[0][i] + counter[1][i] == 0) {
          x[j][i] = 0.1;
          System.out.print("+");
        } else {
          // smoothing
          x[j][i] = ((double) counter[j][i] + 1)
              / (counter[0][i] + counter[1][i] + 2);
        }
      }
    }
    System.out.println();
    try {
      List<String> list = new ArrayList<String>();
      for (int i = 0; i < numSentiWords; i++) {
        if (x[0][i] > 0.5) {
          list.add(sentiTable.idToSymbol(i));
        }
      }
      TextFiles.writeCollection(list, "testPos.txt", "utf-8");
      list.clear();
      for (int i = 0; i < numSentiWords; i++) {
        if (x[1][i] > 0.5) {
          list.add(sentiTable.idToSymbol(i));
        }
      }
      TextFiles.writeCollection(list, "testNeg.txt", "utf-8");
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.err.println("Finished!");
  }

  /**
   * Optimizes values of betas over y.
   */
  void optimizeBeta(double optimizationAccuracy) {
    System.out.println("\nOptimizing beta over y...\n");
    double startTime = System.currentTimeMillis();
    if (!optimizeWithLbfgs(optimizationAccuracy)) {
      System.err.println("Error with optimization");
      System.exit(-1);
    }
    System.out.printf("Optimization done (%.4fs)\n",
        (System.currentTimeMillis() - startTime) / 1000);
  }

  /**
   * Optimizes beta over y.
   * 
   * @param optimizationAccuracy
   * @return true if an solution was found, false if an error occurs
   */
  boolean optimizeWithLbfgs(double optimizationAccuracy) {
    // optimize y
    int numCorrections = 4;
    int[] iflag = new int[2];
    boolean supplyDiag = false;
    double machinePrecision = 1.1920929e-7;
    // starting point
    double[] diag = new double[vars.length];
    // iprint[0] = output every iprint[0] iterations
    // iprint[1] = 0~3 : least to most detailed output
    int[] iprint = new int[] { 50, 0 };
    do {
      try {
        LBFGS.lbfgs(vars.length, numCorrections, vars, computeFunction(null),
            computeGradient(null), supplyDiag, diag, iprint,
            optimizationAccuracy, machinePrecision, iflag);
        variablesToY();
        updateBeta();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } while (iflag[0] == 1);

    return (iflag[0] == 0);
  }

  /**
   * Converts the variables used in optimization to the specific internal
   * variables.
   */
  void variablesToY() {
    int idx = 0;
    for (int j = 0; j < numSenti; j++) {
      for (int k = 0; k < numTopics; k++) {
        for (int i = 0; i < numSentiWords; i++) {
          y[j][k][i] = vars[idx++];
        }
      }
    }
  }

  /**
   * Updates betas and their sums.
   * <p>
   * This method must be called whenever the internal variables y (which beta
   * depends on) change.
   */
  void updateBeta() {
    for (int j = 0; j < numSenti; j++) {
      for (int k = 0; k < numTopics; k++) {
        sumBeta[j][k] = 0;
        for (int i = 0; i < numSentiWords; i++) {
          beta[j][k][i] = Math.exp(x[j][i] + y[j][k][i]);
          sumBeta[j][k] += beta[j][k][i];
        }
      }
    }
  }

  @Override
  public double computeFunction(double[] variables)
      throws InvalidArgumentException {
    double negLogLikelihood = 0.0;
    double negLogPrior = 0;
    for (int j = 0; j < numSenti; j++) {
      for (int k = 0; k < numTopics; k++) {
        negLogLikelihood += Gamma.logGamma(sumSTW[j][k] + sumBeta[j][k])
            - Gamma.logGamma(sumBeta[j][k]);
        double jk = Gamma.digamma(sumSTW[j][k] + sumBeta[j][k])
            - Gamma.digamma(sumBeta[j][k]);
        for (int i = 0; i < numSentiWords; i++) {
          double jki = jk;
          if (cntSWT[j].getValue(i, k) > 0) {
            negLogLikelihood += Gamma.logGamma(beta[j][k][i])
                - Gamma.logGamma(beta[j][k][i] + cntSWT[j].getValue(i, k));
            jki += Gamma.digamma(beta[j][k][i])
                - Gamma.digamma(beta[j][k][i] + cntSWT[j].getValue(i, k));
          }
          betaJki[j][k][i] = beta[j][k][i] * jki;
          negLogPrior += x[j][i] + y[j][k][i] + y[j][k][i] * y[j][k][i]
              / (2 * sigmaSquare);
        }
      }
    }

    return negLogLikelihood + negLogPrior;
  }

  @Override
  public double[] computeGradient(double[] variables)
      throws InvalidArgumentException {
    double[] grads = new double[vars.length];
    int idx = 0;
    for (int j = 0; j < numSenti; j++) {
      for (int k = 0; k < numTopics; k++) {
        for (int i = 0; i < numSentiWords; i++) {
          grads[idx++] = betaJki[j][k][i] + 1 + y[j][k][i] / sigmaSquare;
        }
      }
    }

    return grads;
  }

  @Override
  public double getBetaSenti(int s, int k, int sentiWord) {
    return beta[s][k][sentiWord];
  }

  @Override
  public double getSumBetaSenti(int s, int k) {
    return sumBeta[s][k];
  }

  @Override
  public DoubleMatrix[] getPhiSenti() {
    DoubleMatrix[] phi = new DoubleMatrix[numSenti];
    for (int j = 0; j < numSenti; j++) {
      phi[j] = new DoubleMatrix(numSentiWords, numTopics);
      for (int i = 0; i < numSentiWords; i++) {
        for (int k = 0; k < numTopics; k++) {
          phi[j].setValue(i, k, (cntSWT[j].getValue(i, k) + beta[j][k][i])
              / (sumSTW[j][k] + sumBeta[j][k]));
        }
      }
    }

    return phi;
  }

  @Override
  public void writeModelOutput(int iter) {
    super.writeModelOutput(iter);
    String dir = outputDir + "/" + iter;
    try {
      writeBeta(dir + "/beta.csv");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Prints out beta senti.
   * 
   * @param file
   * @throws IOException
   */
  public void writeBeta(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int s = 0; s < numSenti; s++)
      for (int t = 0; t < numTopics; t++)
        out.print(",S" + s + "-T" + t);
    out.println();
    for (int w = 0; w < numSentiWords; w++) {
      out.print(sentiTable.idToSymbol(w) + ",");
      for (int s = 0; s < numSenti; s++) {
        for (int t = 0; t < numTopics; t++) {
          out.printf("%.5f,", Math.log(beta[s][t][w]));
        }
      }
      out.println();
    }
    out.close();
  }
}
