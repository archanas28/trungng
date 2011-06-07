package edu.kaist.uilab.asc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import edu.kaist.uilab.asc.data.Document;
import edu.kaist.uilab.asc.opt.MathUtils;
import edu.kaist.uilab.asc.util.InvalidArgumentException;

/**
 * Asc implementation with the following prior:
 * <code>beta_{jki} = exp((epsilon + y_{ki}) * (epsilon + y_{ji}) + y_{v}</code>
 * where j, k, i denotes a sentiment, topic, and word respectively.
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

  static final double epsilon = 0.00001;
  double optimizationAccuracy = 0.5;

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
  public Asc2(int numTopics, int numSenti, Vector<LocaleWord> wordList,
      List<Document> documents, int numEnglishDocuments,
      List<TreeSet<Integer>> sentiWordsList, double alpha, double[] gammas,
      String graphFile) {
    super(new Asc2Model(), numTopics, numSenti, wordList, documents,
        numEnglishDocuments, sentiWordsList, alpha, gammas, graphFile);
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
   * @param dir
   * @throws IOException
   */
  void writeSampleY(String dir) throws IOException {
    Asc2Model model = (Asc2Model) this.model;
    PrintWriter out = new PrintWriter(dir + "/y.txt");
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

    // write word sentiment
    out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dir
        + "/wordSenti.csv"), "utf-8"));
    for (int i = 0; i < model.numUniqueWords; i++) {
      out.printf("%s,%.4f,%.4f\n", model.wordList.get(i),
          model.ySentiment[0][i], model.ySentiment[1][i]);
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
//    initY0();
    initNewWay();
  }

  void initNewWay() {
    Asc2Model m = (Asc2Model) model;
    double commonTopicWord = 0.1;
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
          m.ySentiment[s][w] = -5;
        } else {
          m.ySentiment[s][w] = 1;
        }
      }
    }
    for (int s = 0; s < m.numSenti; s++) {
      m.beta[s] = new double[m.numTopics][m.numUniqueWords];
      for (int t = 0; t < m.numTopics; t++) {
        m.sumBeta[s][t] = 0;
        for (int w = 0; w < m.numUniqueWords; w++) {
          m.beta[s][t][w] = Math.exp((m.yTopic[t][w] + epsilon)
              * (m.ySentiment[s][w] + epsilon) + m.yWord[w]);
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
          m.beta[s][t][w] = Math.exp(epsilon * epsilon);
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
          m.beta[s][t][w] = Math.exp((m.yTopic[t][w] + epsilon)
              * (m.ySentiment[s][w] + epsilon) + m.yWord[w]);
          m.sumBeta[s][t] += m.beta[s][t][w];
        }
      }
    }
  }

  @Override
  public double computeFunction(double[] vars) throws InvalidArgumentException {
    Asc2Model m = (Asc2Model) model;
    // compute L_B = - log likelihood = -log p(w,z,s|alpha, beta)
    // this never changes even if we changes the formula for beta because
    // the computation is solely based on beta, not y.
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
    double[] grads = new double[vars.length];
    Asc2Model m = (Asc2Model) model;
    double[][][] betaJki = new double[model.numSenti][model.numTopics][model.numUniqueWords];
    // common beta terms for y_ki, y_ji and y_word i
    for (int j = 0; j < model.numSenti; j++) {
      for (int k = 0; k < model.numTopics; k++) {
        double jk = MathUtils.digamma(model.sumSTW[j][k] + model.sumBeta[j][k])
            - MathUtils.digamma(model.sumBeta[j][k]);
        for (int i = 0; i < model.numUniqueWords; i++) {
          betaJki[j][k][i] = jk;
          if (model.matrixSWT[j].getValue(i, k) > 0) {
            betaJki[j][k][i] += MathUtils.digamma(model.beta[j][k][i])
                - MathUtils.digamma(model.beta[j][k][i]
                    + model.matrixSWT[j].getValue(i, k));
          }
          betaJki[j][k][i] = betaJki[j][k][i] * m.beta[j][k][i];
        }
      }
    }

    ArrayList<Integer> neighbors;
    // gradients of y_ki
    int idx = 0;
    for (int k = 0; k < m.numTopics; k++) {
      for (int i = 0; i < m.numUniqueWords; i++) {
        grads[idx] = 0;
        for (int j = 0; j < m.numSenti; j++) {
          grads[idx] += (epsilon + m.ySentiment[j][i]) * betaJki[j][k][i];
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
          grads[idx] += (epsilon + m.yTopic[k][i]) * betaJki[j][k][i];
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

  @Override
  double getOptimizationAccuracy() {
    return optimizationAccuracy;
  }
}
