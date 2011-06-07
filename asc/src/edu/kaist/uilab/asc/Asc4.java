package edu.kaist.uilab.asc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import edu.kaist.uilab.asc.data.Document;
import edu.kaist.uilab.asc.opt.MathUtils;
import edu.kaist.uilab.asc.util.InvalidArgumentException;

/**
 * Asc implementation with the following prior:
 * <code>beta_{jki} = exp(y_{ki} + 0.1y_{ji} + y_{v}</code> where j, k, i
 * denotes a sentiment, topic, and word respectively.
 * <p>
 * This adds a multiplicative constant to <code>y_{ji}</code> to limit their
 * contribution to the value of <code>beta_{jki}</code> as in {@link Asc3}.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Asc4 extends Asc2 {

  private final double reducingFactor = 0.1;
  private double optimizationAccuracy = 5;

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
  }

  /**
   * Loads an existing model from the specified file for continuing the sampling
   * from the <code>iter</code> iteration.
   * 
   * @param savedModel
   */
  public Asc4(String savedModel, int iter) {
    super(savedModel, iter);
  }

  @Override
  public void setOutputDir(String dir) {
    dir = String.format("%s(rf-%.2f)", dir, reducingFactor);
    model.outputDir = dir;
    new File(dir).mkdir();
  }

  @Override
  double getOptimizationAccuracy() {
    return optimizationAccuracy;
  }
  
  @Override
  void initOptimizationVariables() {
    Asc2Model m = (Asc2Model) model;
    m.yTopic = new double[m.numTopics][m.numUniqueWords];
    m.ySentiment = new double[m.numSenti][m.numUniqueWords];
    m.yWord = new double[m.numUniqueWords];
//    initNewWay();
     initY0();
  }

  void initNewWay() {
    Asc2Model m = (Asc2Model) model;
    double commonTopicWord = 0.0;
    for (int t = 0; t < m.numTopics; t++) {
      for (int w = 0; w < m.numUniqueWords; w++) {
        m.yTopic[t][w] = commonTopicWord;
      }
    }
    for (int w = 0; w < m.numUniqueWords; w++) {
      m.yWord[w] = 0;
    }
    for (int s = 0; s < m.numSenti; s++) {
      for (int w = 0; w < m.numUniqueWords; w++) {
        if (m.sentiWordsList.get(1 - s).contains(w)) {
          m.ySentiment[s][w] = -25;
        } else {
          m.ySentiment[s][w] = 2;
        }
      }
    }
    for (int s = 0; s < m.numSenti; s++) {
      m.beta[s] = new double[m.numTopics][m.numUniqueWords];
      for (int t = 0; t < m.numTopics; t++) {
        m.sumBeta[s][t] = 0;
        for (int w = 0; w < m.numUniqueWords; w++) {
          m.beta[s][t][w] = Math.exp(m.yTopic[t][w] + reducingFactor
              * m.ySentiment[s][w] + m.yWord[w]);
          m.sumBeta[s][t] += m.beta[s][t][w];
        }
      }
    }
  }

  void initY0() {
    Asc2Model m = (Asc2Model) model;
    for (int t = 0; t < m.numTopics; t++) {
      for (int w = 0; w < m.numUniqueWords; w++) {
        m.yTopic[t][w] = 0;
      }
    }
    for (int w = 0; w < m.numUniqueWords; w++) {
      m.yWord[w] = 0;
    }
    for (int s = 0; s < m.numSenti; s++) {
      for (int w = 0; w < m.numUniqueWords; w++) {
        m.ySentiment[s][w] = 0;
      }
    }
    for (int s = 0; s < m.numSenti; s++) {
      m.beta[s] = new double[m.numTopics][m.numUniqueWords];
      for (int t = 0; t < m.numTopics; t++) {
        m.sumBeta[s][t] = 0;
        for (int w = 0; w < m.numUniqueWords; w++) {
          m.beta[s][t][w] = 1.0;
          m.sumBeta[s][t] += m.beta[s][t][w];
        }
      }
    }
  }

  @Override
  void updateBeta() {
    Asc2Model m = (Asc2Model) model;
    for (int s = 0; s < m.numSenti; s++) {
      for (int t = 0; t < m.numTopics; t++) {
        m.sumBeta[s][t] = 0;
        for (int w = 0; w < m.numUniqueWords; w++) {
          m.beta[s][t][w] = Math.exp(m.yTopic[t][w] + reducingFactor * m.ySentiment[s][w]
              + m.yWord[w]);
          m.sumBeta[s][t] += m.beta[s][t][w];
        }
      }
    }
  }

  @Override
  public double computeFunction(double[] vars) throws InvalidArgumentException {
    Asc2Model m = (Asc2Model) model;
    // compute L_B = - log likelihood = -log p(w,z,s|alpha, beta)
    double negLogLikelihood = 0.0;
    for (int j = 0; j < m.numSenti; j++) {
      for (int k = 0; k < m.numTopics; k++) {
        negLogLikelihood += MathUtils
            .logGamma(m.sumSTW[j][k] + m.sumBeta[j][k])
            - MathUtils.logGamma(m.sumBeta[j][k]);
        for (int i = 0; i < m.numUniqueWords; i++) {
          if (m.matrixSWT[j].getValue(i, k) > 0) {
            negLogLikelihood += MathUtils.logGamma(m.beta[j][k][i])
                - MathUtils.logGamma(m.beta[j][k][i]
                    + m.matrixSWT[j].getValue(i, k));
          }
        }
      }
    }
    // compute log p(beta)
    double logPrior = 0;
    double term = 0.0;
    for (int i = 0; i < m.numUniqueWords; i++) {
      ArrayList<Integer> neighbors = m.graph.getNeighbors(i);
      // phi(i, iprime) = 1
      for (int iprime : neighbors) {
        for (int j = 0; j < m.numSenti; j++) {
          term = m.ySentiment[j][i] - m.ySentiment[j][iprime];
          logPrior += term * term;
        }
        for (int k = 0; k < m.numTopics; k++) {
          term = m.yTopic[k][i] - m.yTopic[k][iprime];
          logPrior += term * term;
        }
      }
    }
    // each edge can be used only once
    logPrior /= 2;
    for (int i = 0; i < m.numUniqueWords; i++) {
      logPrior += m.yWord[i] * m.yWord[i];
    }
    logPrior *= -0.5; // 0.5lamda^2 where lamda = 1
    return negLogLikelihood - logPrior;
  }

  @Override
  public double[] computeGradient(double[] vars)
      throws InvalidArgumentException {
    Asc2Model m = (Asc2Model) model;
    double[] grads = new double[vars.length];
    double term, jk;
    double[][][] betaJki = new double[m.numSenti][m.numTopics][m.numUniqueWords];
    ArrayList<Integer> neighbors;
    // common beta terms for y_ki, y_ji and y_word i
    for (int j = 0; j < m.numSenti; j++) {
      for (int k = 0; k < m.numTopics; k++) {
        jk = MathUtils.digamma(m.sumSTW[j][k] + m.sumBeta[j][k])
            - MathUtils.digamma(m.sumBeta[j][k]);
        for (int i = 0; i < m.numUniqueWords; i++) {
          term = jk;
          if (m.matrixSWT[j].getValue(i, k) > 0) {
            term += MathUtils.digamma(m.beta[j][k][i])
                - MathUtils.digamma(m.beta[j][k][i]
                    + m.matrixSWT[j].getValue(i, k));
          }
          betaJki[j][k][i] = m.beta[j][k][i] * term;
        }
      }
    }

    // gradients of y_ki
    int idx = 0;
    for (int k = 0; k < m.numTopics; k++) {
      for (int i = 0; i < m.numUniqueWords; i++) {
        grads[idx] = 0;
        for (int j = 0; j < m.numSenti; j++) {
          grads[idx] += betaJki[j][k][i];
        }
        neighbors = m.graph.getNeighbors(i);
        for (int iprime : neighbors) {
          grads[idx] += m.yTopic[k][i] - m.yTopic[k][iprime];
        }
        idx++;
      }
    }
    // gradient of y_ji
    for (int j = 0; j < m.numSenti; j++) {
      for (int i = 0; i < m.numUniqueWords; i++) {
        grads[idx] = 0;
        for (int k = 0; k < m.numTopics; k++) {
          grads[idx] += betaJki[j][k][i];
        }
        grads[idx] *= reducingFactor;
        neighbors = m.graph.getNeighbors(i);
        for (int iprime : neighbors) {
          grads[idx] += m.ySentiment[j][i] - m.ySentiment[j][iprime];
        }
        idx++;
      }
    }

    // gradients of y_word i
    for (int i = 0; i < m.numUniqueWords; i++) {
      grads[idx] = m.yWord[i];
      for (int j = 0; j < m.numSenti; j++) {
        for (int k = 0; k < m.numTopics; k++) {
          grads[idx] += betaJki[j][k][i];
        }
      }
      idx++;
    }
    return grads;
  }
}
