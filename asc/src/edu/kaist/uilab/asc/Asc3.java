package edu.kaist.uilab.asc;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import edu.kaist.uilab.asc.data.OrderedDocument;
import edu.kaist.uilab.asc.opt.MathUtils;
import edu.kaist.uilab.asc.prior.SimilarityGraph;
import edu.kaist.uilab.asc.util.InvalidArgumentException;

/**
 * Asc implementation with the following prior:
 * <code>beta_{jki} = exp(y_{ki} + y_{ji} + y_{v}</code> where j, k, i denotes
 * a sentiment, topic, and word respectively.
 * 
 * <p> This does not seem to be a good prior because in many cases,
 * <code>y_{ji}</code> dominates the term <code>y_{ki} + y_{ji}</code>, hence
 * making the <code>ith</code> word appear in many topics.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Asc3 extends Asc2 {

  /**
   * Creates a new Asc3 model.
   * 
   * @param numTopics
   * @param numSenti
   * @param wordList
   * @param documents
   * @param sentiWordsList
   * @param alpha
   * @param gammas
   * @param graph
   */
  public Asc3(int numTopics, int numSenti, List<String> wordList,
      List<OrderedDocument> documents, int numEnglishDocuments,
      List<TreeSet<Integer>> sentiWordsList, double alpha, double[] gammas,
      SimilarityGraph graph) {
    super(numTopics, numSenti, wordList, documents, numEnglishDocuments,
        sentiWordsList, alpha, gammas, graph);
  }

  /**
   * Loads an existing model from the specified file for continuing the sampling
   * from the <code>iter</code> iteration.
   * 
   * @param savedModel
   */
  public Asc3(String savedModel, int iter) {
    super(savedModel, iter);
  }

  @Override
  void initOptimizationVariables() {
    Asc2Model m = (Asc2Model) model;
    m.yTopic = new double[m.numTopics][m.numUniqueWords];
    m.ySentiment = new double[m.numSenti][m.numUniqueWords];
    m.yWord = new double[m.numUniqueWords];
    initOldWay();
    // initNewWay();
    // initY0();
  }

  void initNewWay() {
    Asc2Model m = (Asc2Model) model;
    double commonTopicWord = 0.001;
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
        if (m.sentiWordsList.get(s).contains(w)) {
          m.ySentiment[s][w] = Math.log(0.001) - commonTopicWord;
        } else if (m.sentiWordsList.get(1 - s).contains(w)) {
          m.ySentiment[s][w] = Math.log(0.000001) - commonTopicWord;
        } else {
          m.ySentiment[s][w] = Math.log(0.001) - commonTopicWord;
        }
      }
    }
    for (int s = 0; s < m.numSenti; s++) {
      m.beta[s] = new double[m.numTopics][m.numUniqueWords];
      for (int t = 0; t < m.numTopics; t++) {
        m.sumBeta[s][t] = 0;
        for (int w = 0; w < m.numUniqueWords; w++) {
          m.beta[s][t][w] = Math.exp(m.yTopic[t][w] + m.ySentiment[s][w]
              + m.yWord[w]);
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

  void initOldWay() {
    Asc2Model m = (Asc2Model) model;
    for (int s = 0; s < m.numSenti; s++) {
      m.beta[s] = new double[m.numTopics][m.numUniqueWords];
      for (int t = 0; t < m.numTopics; t++) {
        m.sumBeta[s][t] = 0;
        for (int w = 0; w < m.numUniqueWords; w++) {
          // asymmetric beta
          if (m.sentiWordsList.get(1 - s).contains(w)) {
            m.beta[s][t][w] = 0.0000001;
          } else {
            m.beta[s][t][w] = 0.001;
          }
          // make beta[s][t][w] = exp(y(tw) + y(sw) + y(w)) where y(w) != 0
          m.yWord[w] = Math.log(m.beta[s][t][w]);
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
          m.beta[s][t][w] = Math.exp(m.yTopic[t][w] + m.ySentiment[s][w]
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
            term += +MathUtils.digamma(m.beta[j][k][i])
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
