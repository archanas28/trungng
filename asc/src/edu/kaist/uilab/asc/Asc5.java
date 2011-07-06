package edu.kaist.uilab.asc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
 * <code>beta_{jki} = exp(y_{ki} + y_{ji}</code> where j, k, i denotes a
 * sentiment, topic, and word respectively.
 * <p>
 * This regards <code>y_{v}</code> in James' paper as <code>y_{ji}</code>.
 * Log(beta) is now added the term <code>sumof(y_ji^2)</code>.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Asc5 extends AbstractAscModel {

  private static final long serialVersionUID = 1L;
  double optimizationAccuracy = 0.5;
  double[][] yTopic; // y[k][i] = y[topic][word]
  double[][] ySentiment; // y[j][i] = y[sentiment][word]

  @Override
  void initVariables() {
    yTopic = new double[numTopics][vocabSize];
    ySentiment = new double[numSenti][vocabSize];
    vars = new double[numTopics * effectiveVocabSize + numSenti
        * effectiveVocabSize];
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
  public Asc5(int numTopics, int numSenti, Vector<LocaleWord> wordList,
      List<Document> documents, int numEnglishDocuments,
      List<TreeSet<Integer>> sentiWordsList, double alpha, double[] gammas,
      String graphFile) {
    super(numTopics, numSenti, wordList, documents, numEnglishDocuments,
        sentiWordsList, alpha, gammas, graphFile);
//    initY0();
     initPrior();
  }

  void initPrior() {
    extraInfo = "initPrior(tw=0.5,senti=-10,0.5)";
    double betaOppositeSenti = 0.0001;
    int idx = 0;
    for (int t = 0; t < numTopics; t++) {
      for (int w = 0; w < vocabSize; w++) {
        yTopic[t][w] = 0.5;
        if (w < effectiveVocabSize) {
          vars[idx++] = yTopic[t][w];
        }
      }
    }
    for (int s = 0; s < numSenti; s++) {
      for (int w = 0; w < vocabSize; w++) {
        if (sentiWordsList.get(1 - s).contains(w)) {
          ySentiment[s][w] = Math.log(betaOppositeSenti);
        } else if (sentiWordsList.get(s).contains(w)) {
          ySentiment[s][w] = 0.5;
        } else {
          ySentiment[s][w] = 0.0;
        }
        if (w < effectiveVocabSize) {
          vars[idx++] = ySentiment[s][w];
        }
      }
    }
    updateBeta();
  }

  void initY0() {
    extraInfo = "initY0";
    for (int t = 0; t < numTopics; t++) {
      for (int w = 0; w < vocabSize; w++) {
        yTopic[t][w] = 0;
      }
    }
    for (int s = 0; s < numSenti; s++) {
      for (int w = 0; w < vocabSize; w++) {
        ySentiment[s][w] = 0;
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
        negLogLikelihood += MathUtils
            .logGamma(sumSTW[j][k] + sumBeta[j][k])
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
    // add the term sigma(y_{j,i}^2)
    for (int j = 0; j < numSenti; j++) {
      for (int i = 0; i < effectiveVocabSize; i++) {
        logPrior += ySentiment[j][i] * ySentiment[j][i];
      }
    }
    logPrior *= -0.5; // 0.5lamda^2 where lamda = 1
    return negLogLikelihood - logPrior;
  }

  @Override
  public double[] computeGradient(double[] x) throws InvalidArgumentException {
    double[] grads = new double[vars.length];
    double[][][] betaJki = new double[numSenti][numTopics][effectiveVocabSize];
    ArrayList<Integer> neighbors;
    // common beta terms for y_ki, y_ji
    for (int j = 0; j < numSenti; j++) {
      for (int k = 0; k < numTopics; k++) {
        double jk = MathUtils.digamma(sumSTW[j][k] + sumBeta[j][k])
            - MathUtils.digamma(sumBeta[j][k]);
        for (int i = 0; i < effectiveVocabSize; i++) {
          betaJki[j][k][i] = jk;
          if (matrixSWT[j].getValue(i, k) > 0) {
            betaJki[j][k][i] += MathUtils.digamma(beta[j][k][i])
                - MathUtils.digamma(beta[j][k][i]
                    + matrixSWT[j].getValue(i, k));
          }
          betaJki[j][k][i] = beta[j][k][i] * betaJki[j][k][i];
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
        grads[idx] = ySentiment[j][i];
        for (int k = 0; k < numTopics; k++) {
          grads[idx] += betaJki[j][k][i];
        }
        neighbors = graph.getNeighbors(i);
        for (int iprime : neighbors) {
          grads[idx] += ySentiment[j][i] - ySentiment[j][iprime];
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
    for (int s = 0; s < numSenti; s++) {
      for (int w = 0; w < effectiveVocabSize; w++) {
        ySentiment[s][w] = vars[idx++];
      }
    }
  }

  @Override
  void extendVars() {
    double[] newVars = new double[numTopics * effectiveVocabSize
        + numSenti * effectiveVocabSize];
    int idx = 0;
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < effectiveVocabSize; i++) {
        newVars[idx++] = yTopic[k][i];
      }
    }
    for (int s = 0; s < numSenti; s++) {
      for (int w = 0; w < effectiveVocabSize; w++) {
        newVars[idx++] = ySentiment[s][w];
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
    // write word sentiment
    out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dir
        + "/wordSenti.csv"), "utf-8"));
    for (int i = 0; i < vocabSize; i++) {
      out.printf("%s,%.4f,%.4f\n", wordList.get(i),
          ySentiment[0][i], ySentiment[1][i]);
    }
    out.close();
  }
}
