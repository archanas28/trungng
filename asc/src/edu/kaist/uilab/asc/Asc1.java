package edu.kaist.uilab.asc;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import edu.kaist.uilab.asc.data.Document;
import edu.kaist.uilab.asc.util.InvalidArgumentException;
import edu.kaist.uilab.opt.MathUtils;

/**
 * Asc implementation with the following prior:
 * <code>beta_{jki} = exp(y_{jki} + y_{v}</code> where j, k, i denotes a
 * sentiment, topic, and word respectively.
 * <p>
 * This is a "heavy weight" prior for the number of variables are too large (
 * <code>numTopics x numWords x numSentiment</code> variables).
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Asc1 extends AbstractAscModel {

  private static final long serialVersionUID = 1L;
  double optimizationAccuracy = 0.5;
  double[][][] y; // y[s][topic][word]
  double[] yWord; // y[word]

  @Override
  void initVariables() {
    y = new double[numSenti][numTopics][vocabSize];
    yWord = new double[vocabSize];
    vars = new double[numSenti * numTopics * effectiveVocabSize
        + effectiveVocabSize];
  }

  /**
   * Creates a new ASC
   * 
   * @param numTopics
   * @param numSenti
   * @param wordList
   * @param documents
   * @param sentiWordsList
   * @param alpha
   * @param gammas
   * @param graphFile
   */
  public Asc1(int numTopics, int numSenti, Vector<LocaleWord> wordList,
      List<Document> documents, int numEnglishDocuments,
      List<TreeSet<Integer>> sentiWordsList, double alpha, double[] gammas,
      String graphFile) {
    super(numTopics, numSenti, wordList, documents, numEnglishDocuments,
        sentiWordsList, alpha, gammas, graphFile);
    initPrior();
  }

  /**
   * Initializes hyper parameters and related quantities.
   */
  void initPrior() {
    extraInfo = "initPrior";
    int idx = 0;
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        for (int w = 0; w < vocabSize; w++) {
          // asymmetric beta
          if (sentiWordsList.get(1 - s).contains(w)){
            y[s][t][w] = -5;
          } else {
            y[s][t][w] = 0;
          }
          if (w < effectiveVocabSize) {
            vars[idx++] = y[s][t][w];
          }
        }
      }
    }
    for (int w = 0; w < effectiveVocabSize; w++) {
      yWord[w] = 0;
      if (w < effectiveVocabSize) {
        vars[idx++] = yWord[w];
      }
    }
    updateBeta();
  }

  /**
   * Converts the variables (used in optimization function) to y.
   */
  void variablesToY() {
    int idx = 0;
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        for (int w = 0; w < effectiveVocabSize; w++) {
          y[s][t][w] = vars[idx++];
        }
      }
    }
    for (int w = 0; w < effectiveVocabSize; w++) {
      yWord[w] = vars[idx++];
    }
  }

  @Override
  void extendVars() {
    double[] newVars = new double[numSenti * numTopics * effectiveVocabSize
        + effectiveVocabSize];
    int idx = 0;
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        for (int w = 0; w < effectiveVocabSize; w++) {
          newVars[idx++] = y[s][t][w];
        }
      }
    }
    for (int w = 0; w < effectiveVocabSize; w++) {
      newVars[idx++] = yWord[w];
    }
    vars = newVars;
  }

  /**
   * Updates betas and their sums.
   * <p>
   * This method should be called whenever y_kv and y_word v are changed.
   */
  void updateBeta() {
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        sumBeta[s][t] = 0;
        for (int w = 0; w < effectiveVocabSize; w++) {
          beta[s][t][w] = Math.exp(y[s][t][w] + yWord[w]);
          sumBeta[s][t] += beta[s][t][w];
        }
      }
    }
  }

  /**
   * Writes out some values of y.
   * 
   * @param dir
   * @throws IOException
   */
  void writeSampleY(String dir) throws IOException {
    PrintWriter out = new PrintWriter(dir + "/y.txt");
    for (int i = 0; i < 100; i++) {
      int w = (int) (Math.random() * vocabSize);
      out.printf("\nWord no: %d", w);
      out.printf("\nyWord[w]: \t%.5f", yWord[w]);
      out.print("\ny[s][t][w]:");
      int s, t;
      for (int j = 0; j < 10; j++) {
        s = (int) (Math.random() * numSenti);
        t = (int) (Math.random() * numTopics);
        out.printf("\t%.5f", y[s][t][w]);
      }
    }
    out.close();
  }

  @Override
  public double computeFunction(double[] x) throws InvalidArgumentException {
    // compute L_B = - log likelihood = -log p(w,z,s|alpha, beta)
    double negLogLikelihood = 0.0;
    for (int j = 0; j < numSenti; j++) {
      for (int k = 0; k < numTopics; k++) {
        negLogLikelihood += MathUtils.logGamma(sumSTW[j][k] + sumBeta[j][k])
            - MathUtils.logGamma(sumBeta[j][k]);
        for (int i = 0; i < effectiveVocabSize; i++) {
          if (matrixSWT[j].getValue(i, k) > 0) {
            negLogLikelihood += MathUtils.logGamma(beta[j][k][i])
                - MathUtils.logGamma(beta[j][k][i]
                    + matrixSWT[j].getValue(i, k));
          }
        }
      }
    }
    // compute log p(beta)
    double logPrior = 0;
    double term = 0.0;
    for (int i = 0; i < effectiveVocabSize; i++) {
      ArrayList<Integer> neighbors = graph.getNeighbors(i);
      // phi(i, iprime) = 1
      for (int iprime : neighbors) {
        for (int j = 0; j < numSenti; j++) {
          for (int k = 0; k < numTopics; k++) {
            term = y[j][k][i] - y[j][k][iprime];
            logPrior += term * term;
          }
        }
      }
    }
    // each edge can be used only once
    logPrior /= 2;
    for (int i = 0; i < effectiveVocabSize; i++) {
      logPrior += yWord[i] * yWord[i];
    }
    logPrior *= -0.5; // 0.5lamda^2 where lamda = 1
    return negLogLikelihood - logPrior;
  }

  @Override
  public double[] computeGradient(double[] x) throws InvalidArgumentException {
    double[] grads = new double[vars.length];
    double tmp;
    double[][][] betaJki = new double[numSenti][numTopics][effectiveVocabSize];
    // common beta terms for both y_jki and y_word i
    for (int j = 0; j < numSenti; j++) {
      for (int k = 0; k < numTopics; k++) {
        for (int i = 0; i < effectiveVocabSize; i++) {
          tmp = MathUtils.digamma(sumSTW[j][k] + sumBeta[j][k])
              - MathUtils.digamma(sumBeta[j][k]);
          if (matrixSWT[j].getValue(i, k) > 0) {
            tmp += MathUtils.digamma(beta[j][k][i])
                - MathUtils
                    .digamma(beta[j][k][i] + matrixSWT[j].getValue(i, k));
          }
          betaJki[j][k][i] = beta[j][k][i] * tmp;
        }
      }
    }

    // gradients of y_jki
    int idx = 0;
    ArrayList<Integer> neighbors;
    for (int j = 0; j < numSenti; j++) {
      for (int k = 0; k < numTopics; k++) {
        for (int i = 0; i < effectiveVocabSize; i++) {
          grads[idx] = betaJki[j][k][i];
          neighbors = graph.getNeighbors(i);
          for (int iprime : neighbors) {
            grads[idx] += y[j][k][i] - y[j][k][iprime];
          }
          idx++;
        }
      }
    }
    // gradients of y_word i
    for (int i = 0; i < effectiveVocabSize; i++) {
      grads[idx] = yWord[i];
      for (int j = 0; j < numSenti; j++) {
        for (int k = 0; k < numTopics; k++) {
          grads[idx] += betaJki[j][k][i];
        }
      }
      idx++;
    }
    return grads;
  }

  @Override
  double getOptimizationAccuracy() {
    return optimizationAccuracy;
  }
}
