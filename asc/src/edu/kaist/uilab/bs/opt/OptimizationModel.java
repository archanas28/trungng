package edu.kaist.uilab.bs.opt;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.math.special.Gamma;

import com.aliasi.symbol.SymbolTable;

import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.InvalidArgumentException;
import edu.kaist.uilab.bs.Document;
import edu.kaist.uilab.bs.Model;
import edu.kaist.uilab.bs.TwogramsCounter;
import edu.kaist.uilab.bs.util.BSUtils;
import edu.kaist.uilab.opt.LBFGS;
import edu.kaist.uilab.opt.ObjectiveFunction;

/**
 * Model that optimizes sentiment for sentiment words.
 * 
 * @author trung
 */
public class OptimizationModel extends Model implements ObjectiveFunction {

  private static final long serialVersionUID = 1L;

  private double[][][] beta; // beta[senti][topic][word]
  private double[][][] logBeta; // logBeta[jki] = y[ki] + y[ji]
  private double[][] sumBeta; // sumBeta[senti][topic]
  double[][][] betaJki;

  // variables for optimization
  double[] vars;
  double[][] yTopic; // y[k][i]
  double[][] ySentiment; // y[j][i]
  final double sigma1Square = 0.5;
  final double sigma2Square = 0.5;
  double sigmaSquare = sigma1Square + sigma2Square;

  public OptimizationModel(int numTopics, int numSenti, SymbolTable sentiTable,
      SymbolTable aspectTable, TwogramsCounter counter,
      List<Document> documents, HashSet<Integer>[] seedWords, double alpha,
      double betaAspect, double[] betaSenti, double[] gammas) {
    super(numTopics, numSenti, sentiTable, aspectTable, counter, documents,
        seedWords, alpha, betaAspect, betaSenti, gammas);
    betaJki = new double[numSenti][numTopics][numSentiWords];
  }

  public double[][] getYSentiment() {
    return ySentiment;
  }

  /**
   * Classify segment sentiment using y_{ji}.
   * 
   * @param phiSenti
   *          not used
   * @param segment
   * @param topic
   *          not used
   * @return
   */
  @Override
  public int classifySegmentSentiment(DoubleMatrix[] phiSenti,
      String[] segment, int topic) {
    double score = 0.0;
    for (int k = 0; k < numTopics; k++) {
      for (String word : segment) {
        int sentiWord = sentiTable.symbolToID(word);
        if (sentiWord >= 0) {
          score += ySentiment[0][sentiWord] - ySentiment[1][sentiWord];
        }
      }
    }

    double thres = 0.5;
    int sentiment = -1;
    if (score >= thres) {
      sentiment = 0;
    } else if (score < -thres) {
      sentiment = 1;
    }

    // negate the sentiment
    if (sentiment >= 0 && BSUtils.isInArray(segment, "not")) {
      sentiment = 1 - sentiment;
    }

    return sentiment;
  }

  @Override
  protected void initBeta(double betaSenti[]) {
    yTopic = new double[numTopics][numSentiWords];
    ySentiment = new double[numSenti][numSentiWords];
    logBeta = new double[numSenti][numTopics][numSentiWords];
    beta = new double[numSenti][numTopics][numSentiWords];
    sumBeta = new double[numSenti][numTopics];
    vars = new double[numTopics * numSentiWords + numSenti * numSentiWords];

    // TODO(trung): initialize beta, sumBeta, logBeta, vars here
    // uniform prior belief for y_ki
    int varIdx = 0;
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < numSentiWords; i++) {
        yTopic[k][i] = 0.0;
        vars[varIdx++] = yTopic[k][i];
      }
    }
    // the 'uniform' beta (i.e., beta[j][k][i] = beta[j][i] for all topics k
    double[][] uniformBeta = getBetaSenti(betaSenti);
    for (int j = 0; j < numSenti; j++) {
      for (int i = 0; i < numSentiWords; i++) {
        ySentiment[j][i] = Math.log(uniformBeta[j][i]);
        vars[varIdx++] = ySentiment[j][i];
      }
    }
    updateBeta();
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
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < numSentiWords; i++) {
        yTopic[k][i] = vars[idx++];
      }
    }
    for (int j = 0; j < numSenti; j++) {
      for (int i = 0; i < numSentiWords; i++) {
        ySentiment[j][i] = vars[idx++];
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
          logBeta[j][k][i] = yTopic[k][i] + ySentiment[j][i];
          beta[j][k][i] = Math.exp(logBeta[j][k][i]);
          sumBeta[j][k] += beta[j][k][i];
        }
      }
    }
  }

  @Override
  public double computeFunction(double[] variables)
      throws InvalidArgumentException {
    // compute L_B = - log likelihood = -log p(w,z,s|alpha, beta)
    // this never changes even if we changes the formula for beta because
    // the computation is solely based on beta, not y.
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
          negLogPrior += logBeta[j][k][i] + logBeta[j][k][i] * logBeta[j][k][i]
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
    // gradients of y_ki
    int idx = 0;
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < numSentiWords; i++) {
        grads[idx] = 0;
        for (int j = 0; j < numSenti; j++) {
          grads[idx] += betaJki[j][k][i] + logBeta[j][k][i] / sigmaSquare;
        }
        grads[idx] += numSenti;
        idx++;
      }
    }
    // gradients of y_ji
    for (int j = 0; j < numSenti; j++) {
      for (int i = 0; i < numSentiWords; i++) {
        grads[idx] = 0;
        double sumLogBeta = 0.0;
        for (int k = 0; k < numTopics; k++) {
          grads[idx] += betaJki[j][k][i];
          sumLogBeta += logBeta[j][k][i];
        }
        grads[idx] += sumLogBeta / sigmaSquare + numTopics;
        idx++;
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
      writeWordSenti(dir + "/wordsenti.csv");
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
          out.printf("%.5f,", logBeta[s][t][w]);
        }
      }
      out.println();
    }
    out.close();
  }

  /**
   * Writes the optimized word senti.
   * 
   * @param file
   * @throws IOException
   */
  public void writeWordSenti(String file) throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (int i = 0; i < numSentiWords; i++) {
      out.printf("%s,%.4f,%.4f\n", sentiTable.idToSymbol(i), ySentiment[0][i],
          ySentiment[1][i]);
    }
    out.close();
  }
}
