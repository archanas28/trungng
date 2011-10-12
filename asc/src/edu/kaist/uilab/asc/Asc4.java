package edu.kaist.uilab.asc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.math.special.Gamma;

import edu.kaist.uilab.asc.data.Document;
import edu.kaist.uilab.asc.util.InvalidArgumentException;

/**
 * Asc implementation with the following prior:
 * <code>beta_{jki} = exp(y_{ki} + 0.1y_{ji} + y_{v}</code> where j, k, i
 * denotes a sentiment, topic, and word respectively.
 * <p>
 * This adds a multiplicative constant to <code>y_{ji}</code> to limit their
 * contribution to the value of <code>beta_{jki}</code>.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Asc4 extends BaseAscModel {

  private static final long serialVersionUID = 1L;
  private final double reducingFactor = 0.1;
  double optimizationAccuracy = 0.5;

  /**
   * Creates a new Asc4 model.
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
  public Asc4(int numTopics, int numSenti, Vector<LocaleWord> wordList,
      List<Document> documents, int numEnglishDocuments,
      List<TreeSet<Integer>> sentiWordsList, double alpha, double[] gammas,
      String graphFile) {
    super(numTopics, numSenti, wordList, documents, numEnglishDocuments,
        sentiWordsList, alpha, gammas, graphFile);
    changeOutputDir();
  }

  public void changeOutputDir() {
    outputDir = String.format("%s(rf-%.2f)", outputDir, reducingFactor);
    ;
    new File(outputDir).mkdir();
  }

  @Override
  double getOptimizationAccuracy() {
    return optimizationAccuracy;
  }

  @Override
  void updateBeta() {
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        sumBeta[s][t] = 0;
        for (int w = 0; w < effectiveVocabSize; w++) {
          beta[s][t][w] = Math.exp(yTopic[t][w] + reducingFactor
              * ySentiment[s][w] + yWord[w]);
          sumBeta[s][t] += beta[s][t][w];
        }
      }
    }
  }

  @Override
  public double computeFunction(double[] x) throws InvalidArgumentException {
    // compute L_B = - log likelihood = -log p(w,z,s|alpha, beta)
    double negLogLikelihood = 0.0;
    for (int j = 0; j < numSenti; j++) {
      for (int k = 0; k < numTopics; k++) {
        negLogLikelihood += Gamma.logGamma(sumSTW[j][k] + sumBeta[j][k])
            - Gamma.logGamma(sumBeta[j][k]);
        for (int i = 0; i < effectiveVocabSize; i++) {
          if (matrixSWT[j].getValue(i, k) > 0) {
            negLogLikelihood += Gamma.logGamma(beta[j][k][i])
                - Gamma.logGamma(beta[j][k][i] + matrixSWT[j].getValue(i, k));
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
          term = ySentiment[j][i] - ySentiment[j][iprime];
          logPrior += term * term;
        }
        for (int k = 0; k < numTopics; k++) {
          term = yTopic[k][i] - yTopic[k][iprime];
          logPrior += term * term;
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
    double term, jk;
    double[][][] betaJki = new double[numSenti][numTopics][effectiveVocabSize];
    ArrayList<Integer> neighbors;
    // common beta terms for y_ki, y_ji and y_word i
    for (int j = 0; j < numSenti; j++) {
      for (int k = 0; k < numTopics; k++) {
        jk = Gamma.digamma(sumSTW[j][k] + sumBeta[j][k])
            - Gamma.digamma(sumBeta[j][k]);
        for (int i = 0; i < effectiveVocabSize; i++) {
          term = jk;
          if (matrixSWT[j].getValue(i, k) > 0) {
            term += Gamma.digamma(beta[j][k][i])
                - Gamma.digamma(beta[j][k][i] + matrixSWT[j].getValue(i, k));
          }
          betaJki[j][k][i] = beta[j][k][i] * term;
        }
      }
    }

    // gradients of y_ki
    int idx = 0;
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < effectiveVocabSize; i++) {
        grads[idx] = 0;
        for (int j = 0; j < numSenti; j++) {
          grads[idx] += betaJki[j][k][i];
        }
        neighbors = graph.getNeighbors(i);
        for (int iprime : neighbors) {
          grads[idx] += yTopic[k][i] - yTopic[k][iprime];
        }
        idx++;
      }
    }
    // gradient of y_ji
    for (int j = 0; j < numSenti; j++) {
      for (int i = 0; i < effectiveVocabSize; i++) {
        grads[idx] = 0;
        for (int k = 0; k < numTopics; k++) {
          grads[idx] += betaJki[j][k][i];
        }
        grads[idx] *= reducingFactor;
        neighbors = graph.getNeighbors(i);
        for (int iprime : neighbors) {
          grads[idx] += ySentiment[j][i] - ySentiment[j][iprime];
        }
        idx++;
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
}
