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
 * <code>beta_{jki} = exp(y_{jki} + y_{v}</code> where j, k, i denotes
 * a sentiment, topic, and word respectively.
 * 
 * <p> This is a "heavy weight" prior for the number of variables are too large
 * (<code>numTopics x numWords x numSentiment</code> variables).
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Asc1 extends AbstractAsc {

  static class Asc1Model extends AscModel {
    private static final long serialVersionUID = 1L;
    double[][][] y; // y[s][topic][word]
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
  public Asc1(int numTopics, int numSenti, List<String> wordList,
      List<OrderedDocument> documents, int numEnglishDocuments,
      List<TreeSet<Integer>> sentiWordsList, double alpha, double[] gammas,
      SimilarityGraph graph) {
    super(numTopics, numSenti, wordList, documents, numEnglishDocuments,
        sentiWordsList, alpha, gammas, graph);
    model = new Asc1Model();
    probTable = new double[numTopics][numSenti];
    model.vars = new double[model.numSenti * model.numTopics
        * model.numUniqueWords + model.numUniqueWords];
    initHyperParameters();
    initOptimizationVariables();
  }

  /**
   * Creates an existing model from the specified file, continuing the sampling
   * from the <code>iter</code> iteration.
   * 
   * @param savedModel
   */
  public Asc1(String savedModel, int iter) {
    startingIteration = iter + 1;
    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(new FileInputStream(savedModel));
      model = (Asc1Model) in.readObject();
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
   * Initializes hyper parameters and related quantities.
   */
  void initOptimizationVariables() {
    Asc1Model model = (Asc1Model) this.model;
    model.y = new double[model.numSenti][][];
    model.yWord = new double[model.numUniqueWords];
    for (int s = 0; s < model.numSenti; s++) {
      model.y[s] = new double[model.numTopics][model.numUniqueWords];
    }
    for (int s = 0; s < model.numSenti; s++) {
      model.beta[s] = new double[model.numTopics][model.numUniqueWords];
      for (int t = 0; t < model.numTopics; t++) {
        model.sumBeta[s][t] = 0;
        for (int w = 0; w < model.numUniqueWords; w++) {
          // asymmetric beta
          if ((s == 0 && model.sentiWordsList.get(1).contains(w))
              || (s == 1 && model.sentiWordsList.get(0).contains(w))) {
            model.beta[s][t][w] = 0.0000001;
          } else {
            model.beta[s][t][w] = 0.001;
          }
          // make beta[s][t][w] = exp(y(stw) + y(w) where y(w) = 0
          model.y[s][t][w] = Math.log(model.beta[s][t][w]);
          model.sumBeta[s][t] += model.beta[s][t][w];
        }
      }
    }
  }

  /**
   * Converts the variables (used in optimization function) to y.
   */
  void variablesToY() {
    int idx = 0;
    Asc1Model model = (Asc1Model) this.model;
    for (int s = 0; s < model.numSenti; s++) {
      for (int t = 0; t < model.numTopics; t++) {
        for (int w = 0; w < model.numUniqueWords; w++) {
          model.y[s][t][w] = model.vars[idx++];
        }
      }
    }
    for (int w = 0; w < model.numUniqueWords; w++) {
      model.yWord[w] = model.vars[idx++];
    }
  }

  /**
   * Updates betas and their sums.
   * <p>
   * This method should be called whenever y_kv and y_word v are changed.
   */
  void updateBeta() {
    Asc1Model model = (Asc1Model) this.model;
    for (int s = 0; s < model.numSenti; s++) {
      for (int t = 0; t < model.numTopics; t++) {
        model.sumBeta[s][t] = 0;
        for (int w = 0; w < model.numUniqueWords; w++) {
          model.beta[s][t][w] = Math.exp(model.y[s][t][w] + model.yWord[w]);
          model.sumBeta[s][t] += model.beta[s][t][w];
        }
      }
    }
  }

  /**
   * Writes out some values of y.
   * 
   * @param file
   * @throws IOException
   */
  void writeSampleY(String file) throws IOException {
    Asc1Model model = (Asc1Model) this.model;
    PrintWriter out = new PrintWriter(file);
    for (int i = 0; i < 100; i++) {
      int w = (int) (Math.random() * model.numUniqueWords);
      out.printf("\nWord no: %d", w);
      out.printf("\nyWord[w]: \t%.5f", model.yWord[w]);
      out.print("\ny[s][t][w]:");
      int s, t;
      for (int j = 0; j < 10; j++) {
        s = (int) (Math.random() * model.numSenti);
        t = (int) (Math.random() * model.numTopics);
        out.printf("\t%.5f", model.y[s][t][w]);
      }
    }
    out.close();
  }

  @Override
  public double computeFunction(double[] vars) throws InvalidArgumentException {
    // compute L_B = - log likelihood = -log p(w,z,s|alpha, beta)
    Asc1Model model = (Asc1Model) this.model;
    double negLogLikelihood = 0.0;
    for (int j = 0; j < model.numSenti; j++) {
      for (int k = 0; k < model.numTopics; k++) {
        negLogLikelihood += MathUtils.logGamma(model.sumSTW[j][k]
            + model.sumBeta[j][k])
            - MathUtils.logGamma(model.sumBeta[j][k]);
        for (int i = 0; i < model.numUniqueWords; i++) {
          if (model.matrixSWT[j].getValue(i, k) > 0) {
            negLogLikelihood += MathUtils.logGamma(model.beta[j][k][i])
                - MathUtils.logGamma(model.beta[j][k][i]
                    + model.matrixSWT[j].getValue(i, k));
          }
        }
      }
    }
    // compute log p(beta)
    double logPrior = 0;
    double term = 0.0;
    for (int i = 0; i < model.numUniqueWords; i++) {
      ArrayList<Integer> neighbors = model.graph.getNeighbors(i);
      // phi(i, iprime) = 1
      for (int iprime : neighbors) {
        for (int j = 0; j < model.numSenti; j++) {
          for (int k = 0; k < model.numTopics; k++) {
            term = model.y[j][k][i] - model.y[j][k][iprime];
            logPrior += term * term;
          }
        }
      }
    }
    // each edge can be used only once
    logPrior /= 2;
    for (int i = 0; i < model.numUniqueWords; i++) {
      logPrior += model.yWord[i] * model.yWord[i];
    }
    logPrior *= -0.5; // 0.5lamda^2 where lamda = 1
    return negLogLikelihood - logPrior;
  }

  @Override
  public double[] computeGradient(double[] vars)
      throws InvalidArgumentException {
    Asc1Model model = (Asc1Model) this.model;
    double[] grads = new double[vars.length];
    double tmp;
    double[][][] betaJki = new double[model.numSenti][model.numTopics][model.numUniqueWords];
    // common beta terms for both y_jki and y_word i
    for (int j = 0; j < model.numSenti; j++) {
      for (int k = 0; k < model.numTopics; k++) {
        for (int i = 0; i < model.numUniqueWords; i++) {
          tmp = MathUtils.digamma(model.sumSTW[j][k] + model.sumBeta[j][k])
              - MathUtils.digamma(model.sumBeta[j][k]);
          if (model.matrixSWT[j].getValue(i, k) > 0) {
            tmp += MathUtils.digamma(model.beta[j][k][i])
                - MathUtils.digamma(model.beta[j][k][i]
                    + model.matrixSWT[j].getValue(i, k));
          }
          betaJki[j][k][i] = model.beta[j][k][i] * tmp;
        }
      }
    }

    // gradients of y_jki
    int idx = 0;
    ArrayList<Integer> neighbors;
    for (int j = 0; j < model.numSenti; j++) {
      for (int k = 0; k < model.numTopics; k++) {
        for (int i = 0; i < model.numUniqueWords; i++) {
          grads[idx] = betaJki[j][k][i];
          neighbors = model.graph.getNeighbors(i);
          for (int iprime : neighbors) {
            grads[idx] += model.y[j][k][i] - model.y[j][k][iprime];
          }
          idx++;
        }
      }
    }
    // gradients of y_word i
    for (int i = 0; i < model.numUniqueWords; i++) {
      grads[idx] = model.yWord[i];
      for (int j = 0; j < model.numSenti; j++) {
        for (int k = 0; k < model.numTopics; k++) {
          grads[idx] += betaJki[j][k][i];
        }
      }
      idx++;
    }
    return grads;
  }
}
