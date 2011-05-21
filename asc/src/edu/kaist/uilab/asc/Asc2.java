package edu.kaist.uilab.asc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import edu.kaist.uilab.asc.data.OrderedDocument;
import edu.kaist.uilab.asc.opt.MathUtils;
import edu.kaist.uilab.asc.prior.SimilarityGraph;
import edu.kaist.uilab.asc.util.InvalidArgumentException;

/**
 * Asc implementation with the following prior:
 * <code>beta_{jki} = exp(y_{ki} * y_{ji} + y_{v}</code> where j, k, i denotes
 * a sentiment, topic, and word respectively.
 * 
 * <p> This is a bad prior for the obvious solutions are <code>y_{ki} = 0</code>
 * and <code>y_{ji} = 0</code>.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Asc2 extends AbstractAsc {

  static class Asc2Model extends AscModel {
    private static final long serialVersionUID = 1L;
    double[][] yTopic; // y[k][i] = y[topic][word]
    double[][] ySentiment; // y[j][i] = y[sentiment][word]
    double[] yWord; // y[word]
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
   * @param graph
   */
  public Asc2(int numTopics, int numSenti, List<String> wordList,
      List<OrderedDocument> documents, int numEnglishDocuments,
      List<TreeSet<Integer>> sentiWordsList, double alpha, double[] gammas,
      SimilarityGraph graph) {
    super(numTopics, numSenti, wordList, documents, numEnglishDocuments,
        sentiWordsList, alpha, gammas, graph);
    model = new Asc2Model();
    probTable = new double[numTopics][numSenti];
    model.vars = new double[model.numSenti * model.numUniqueWords
        + model.numTopics * model.numUniqueWords + model.numUniqueWords];
    initHyperParameters();
    initOptimizationVariables();
  }

  /**
   * Creates an existing model from the specified file, continuing the sampling
   * from the <code>iter</code> iteration.
   * 
   * @param savedModel
   */
  public Asc2(String savedModel, int iter) {
    startingIteration = iter + 1;
    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(new FileInputStream(savedModel));
      model = (Asc2Model) in.readObject();
      model.isExisting = true;
      in.close();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    probTable = new double[model.numTopics][model.numSenti];
  }

  /**
   * Writes out some values of y.
   * 
   * @param file
   * @throws IOException
   */
  void writeSampleY(String file) throws IOException {
    Asc2Model model = (Asc2Model) this.model;
    PrintWriter out = new PrintWriter(file);
    for (int i = 0; i < 100; i++) {
      int w = (int) (Math.random() * model.numUniqueWords);
      out.printf("\nWord no: %d", w);
      out.printf("\nyWord[w]: \t%.5f", model.yWord[w]);
      out.print("\nySentiment[s][w]:");
      for (int s = 0; s < model.numSenti; s++) {
        out.printf("\t%.5f", model.ySentiment[s][w]);
      }
      out.printf("\nyTopic[t][w]:");
      for (int t = 0; t < model.numTopics; t++) {
        out.printf("\t%.5f", model.yTopic[t][w]);
      }
    }
    out.close();
  }

  /**
   * Initializes optimization variables.
   */
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

  /**
   * Converts the variables (used in optimization function) to y.
   */
  void variablesToY() {
    int idx = 0;
    Asc2Model m = (Asc2Model) model;
    for (int t = 0; t < m.numTopics; t++) {
      for (int w = 0; w < m.numUniqueWords; w++) {
        m.yTopic[t][w] = m.vars[idx++];
      }
    }
    for (int s = 0; s < m.numSenti; s++) {
      for (int w = 0; w < m.numUniqueWords; w++) {
        m.ySentiment[s][w] = m.vars[idx++];
      }
    }
    for (int w = 0; w < m.numUniqueWords; w++) {
      m.yWord[w] = m.vars[idx++];
    }
  }

  /**
   * Updates betas and their sums.
   * <p>
   * This method should be called whenever y_kv and y_word v are changed.
   */
  void updateBeta() {
    Asc2Model m = (Asc2Model) model;
    for (int s = 0; s < m.numSenti; s++) {
      for (int t = 0; t < m.numTopics; t++) {
        m.sumBeta[s][t] = 0;
        for (int w = 0; w < m.numUniqueWords; w++) {
          m.beta[s][t][w] = Math.exp(m.yTopic[t][w] * m.ySentiment[s][w]
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
          for (int k = 0; k < m.numTopics; k++) {
            term = m.yTopic[k][i] * m.ySentiment[j][i] - m.yTopic[k][iprime]
                * m.ySentiment[j][iprime];
            logPrior += term * term;
          }
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
    double[] grads = new double[vars.length];
    double term, jk;
    Asc2Model m = (Asc2Model) model;
    double[][][] betaJki = new double[model.numSenti][model.numTopics][model.numUniqueWords];
    double[][][] c = new double[model.numSenti][model.numTopics][model.numUniqueWords];
    ArrayList<Integer> neighbors;
    // common beta terms for y_ki, y_ji and y_word i
    for (int j = 0; j < model.numSenti; j++) {
      for (int k = 0; k < model.numTopics; k++) {
        jk = MathUtils.digamma(model.sumSTW[j][k] + model.sumBeta[j][k])
            - MathUtils.digamma(model.sumBeta[j][k]);
        for (int i = 0; i < model.numUniqueWords; i++) {
          if (model.matrixSWT[j].getValue(i, k) > 0) {
            term = jk
                + MathUtils.digamma(model.beta[j][k][i])
                - MathUtils.digamma(model.beta[j][k][i]
                    + model.matrixSWT[j].getValue(i, k));
          } else {
            term = jk;
          }
          betaJki[j][k][i] = m.beta[j][k][i] * term;
          c[j][k][i] = betaJki[j][k][i];
          neighbors = m.graph.getNeighbors(i);
          for (int iprime : neighbors) {
            c[j][k][i] += m.yTopic[k][i] * m.ySentiment[j][i]
                - m.yTopic[k][iprime] * m.ySentiment[j][iprime];
          }
        }
      }
    }

    // gradients of y_ki
    int idx = 0;
    for (int k = 0; k < m.numTopics; k++) {
      for (int i = 0; i < m.numUniqueWords; i++) {
        grads[idx] = 0;
        for (int j = 0; j < m.numSenti; j++) {
          grads[idx] += c[j][k][i] * m.ySentiment[j][i];
        }
        idx++;
      }
    }

    // gradient of y_ji
    for (int j = 0; j < m.numSenti; j++) {
      for (int i = 0; i < m.numUniqueWords; i++) {
        grads[idx] = 0;
        for (int k = 0; k < m.numTopics; k++) {
          grads[idx] += c[j][k][i] * m.yTopic[k][i];
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
