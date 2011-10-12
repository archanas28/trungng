package edu.kaist.uilab.asc;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.math.special.Gamma;

import edu.kaist.uilab.asc.data.Document;
import edu.kaist.uilab.asc.util.InvalidArgumentException;

/**
 * Asc implementation with the following prior:
 * <code>beta_{jki} = exp(y_{ki} + y_{ji}</code> where j, k, i denotes a
 * sentiment, topic, and word respectively. However, y_{ji} are considered
 * observed and not part of the optimization process.
 * <p>
 * This regards <code>y_{v}</code> in James' paper as <code>y_{ji}</code>.
 * TODO(trung): this belongs to the class of Asc5 (though with the difference
 * that ySentiment are observed!!).
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Asc6 extends AbstractAscModel {

  private static final long serialVersionUID = 1L;
  double optimizationAccuracy = 0.5;
  double[][] yTopic; // y[k][i] = y[topic][word]
  double[][] ySentiment; // y[j][i] = y[sentiment][word]

  @Override
  void initVariables() {
    yTopic = new double[numTopics][vocabSize];
    ySentiment = new double[numSenti][vocabSize];
    vars = new double[numTopics * vocabSize];
  }

  /**
   * Creates a new ASC model.
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
  public Asc6(int numTopics, int numSenti, Vector<LocaleWord> wordList,
      List<Document> documents, int numEnglishDocuments,
      List<TreeSet<Integer>> sentiWordsList, double alpha, double[] gammas,
      String graphFile) {
    super(numTopics, numSenti, wordList, documents, numEnglishDocuments,
        sentiWordsList, alpha, gammas, graphFile);
    initPrior();
  }

  void initPrior() {
    extraInfo = "initPrior(tw=1,senti=-0.5,0.5)";
    for (int s = 0; s < numSenti; s++) {
      for (int w = 0; w < vocabSize; w++) {
        if (sentiWordsList.get(1 - s).contains(w)) {
          ySentiment[s][w] = -5;
        } else {
          ySentiment[s][w] = 0.5;
        }
      }
    }
    int idx = 0;
    for (int t = 0; t < numTopics; t++) {
      for (int w = 0; w < vocabSize; w++) {
        yTopic[t][w] = 1.0;
        if (w < effectiveVocabSize) {
          vars[idx++] = yTopic[t][w];
        }
      }
    }
    updateBeta();
  }

  @Override
  public double computeFunction(double[] x) throws InvalidArgumentException {
    // compute L_B = - log likelihood = -log p(w,z,s|alpha, beta)
    // this never changes even if we changes the formula for beta because
    // the computation is solely based on beta, not y.
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
        for (int k = 0; k < numTopics; k++) {
          term = yTopic[k][i] - yTopic[k][iprime];
          logPrior += term * term;
        }
      }
    }
    // each edge can be used only once
    logPrior /= 2;
    logPrior *= -0.5; // 0.5lamda^2 where lamda = 1
    return negLogLikelihood - logPrior;
  }

  @Override
  public double[] computeGradient(double[] x) throws InvalidArgumentException {
    double[] grads = new double[vars.length];
    ArrayList<Integer> neighbors;
    // gradients of y_ki
    int idx = 0;
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < effectiveVocabSize; i++) {
        grads[idx] = 0.0;
        double betaj = 0.0;
        for (int j = 0; j < numSenti; j++) {
          betaj = Gamma.digamma(sumSTW[j][k] + sumBeta[j][k])
              - Gamma.digamma(sumBeta[j][k]);
          if (matrixSWT[j].getValue(i, k) > 0) {
            betaj += Gamma.digamma(beta[j][k][i])
                - Gamma.digamma(beta[j][k][i] + matrixSWT[j].getValue(i, k));
          }
          grads[idx] += beta[j][k][i] * betaj;
        }
        neighbors = graph.getNeighbors(i);
        for (int iprime : neighbors) {
          grads[idx] += yTopic[k][i] - yTopic[k][iprime];
        }
        idx++;
      }
    }
    return grads;
  }

  @Override
  double getOptimizationAccuracy() {
    return optimizationAccuracy;
  }

  @Override
  void variablesToY() {
    int idx = 0;
    for (int t = 0; t < numTopics; t++) {
      for (int w = 0; w < effectiveVocabSize; w++) {
        yTopic[t][w] = vars[idx++];
      }
    }
  }

  @Override
  void extendVars() {
    double[] newVars = new double[numTopics * effectiveVocabSize];
    int idx = 0;
    for (int t = 0; t < numTopics; t++) {
      for (int w = 0; w < effectiveVocabSize; w++) {
        newVars[idx++] = yTopic[t][w];
      }
    }
    vars = newVars;
  }

  @Override
  void updateBeta() {
    for (int s = 0; s < numSenti; s++) {
      for (int t = 0; t < numTopics; t++) {
        sumBeta[s][t] = 0;
        for (int w = 0; w < effectiveVocabSize; w++) {
          beta[s][t][w] = Math.exp(yTopic[t][w] + ySentiment[s][w]);
          sumBeta[s][t] += beta[s][t][w];
        }
      }
    }
  }

  @Override
  void writeSampleY(String dir) throws IOException {
    PrintWriter out = new PrintWriter(dir + "/y.txt");
    for (int i = 0; i < 100; i++) {
      int w = (int) (Math.random() * vocabSize);
      out.printf("\nWord no: %d", w);
      out.printf("\nyTopic[t][w]:");
      for (int t = 0; t < numTopics; t++) {
        out.printf("\t%.5f", yTopic[t][w]);
      }
    }
    out.close();
  }
}
