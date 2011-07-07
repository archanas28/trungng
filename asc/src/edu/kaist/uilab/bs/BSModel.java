package edu.kaist.uilab.bs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;

import com.aliasi.symbol.SymbolTable;

import edu.kaist.uilab.asc.util.IntegerMatrix;

/**
 * Stores all data used in the Gibbs sampling procedure. This model implements
 * {@link Serializable} so that it can be saved and loaded to allow Gibbs
 * sampler to continue from an existing model.
 * 
 * @author trung
 */
public class BSModel implements Serializable {

  private static final long serialVersionUID = 1L;
  boolean isExisting = false;
  String outputDir = ".";
  String extraInfo = ""; // extra info about the model

  int numSentiWords; // V'
  int numAspectWords; // V
  int numTopics; // K
  int numSenti; // S
  int numDocuments; // M
  List<Document> documents;
  SymbolTable sentiTable;
  SymbolTable aspectTable;

  int numProbWords = 100;
  double alpha;
  double sumAlpha;
  double[] gammas;
  double sumGamma;
  double betaAspect;
  double sumBetaAspect;
  double[][] betaSenti;
  double[] sumBetaSenti; // sumBeta[senti]

  HashSet<Integer>[] seedWords;
  IntegerMatrix cntWT; // cwt[V][T]
  int[] sumWT; // sumWT[T]
  IntegerMatrix[] cntSWT; // cstw[S][V'][T]
  int[][] sumSTW; // sumSTW[S][T]
  IntegerMatrix cntDT; // cdt[M][T]
  int[] sumDT; // sumDT[M]
  IntegerMatrix cntDS;
  int[] sumDS; // sumDS[D]

  /**
   * @param numTopics
   * @param numSenti
   * @param symbolTable
   * @param sentiStems
   * @param documents
   * @param numSentiWords
   * @param seedWords
   * @param alpha
   * @param betaAspect
   * @param betaSenti
   * @param gammas
   */
  public BSModel(int numTopics, int numSenti, SymbolTable sentiTable,
      SymbolTable aspectTable, List<Document> documents,
      HashSet<Integer>[] seedWords, double alpha, double betaAspect,
      double[] betaSenti, double[] gammas) {
    this.numTopics = numTopics;
    this.numSenti = numSenti;
    this.sentiTable = sentiTable;
    this.aspectTable = aspectTable;
    this.numSentiWords = sentiTable.numSymbols();
    this.numAspectWords = aspectTable.numSymbols();
    this.documents = documents;
    this.numDocuments = documents.size();
    this.seedWords = seedWords;
    this.alpha = alpha;
    this.betaAspect = betaAspect;
    this.betaSenti = getBetaSenti(betaSenti);
    this.gammas = gammas;
    cntWT = new IntegerMatrix(numAspectWords, numTopics);
    sumWT = new int[numTopics];
    cntSWT = new IntegerMatrix[numSenti];
    for (int i = 0; i < numSenti; i++) {
      cntSWT[i] = new IntegerMatrix(numSentiWords, numTopics);
    }
    sumSTW = new int[numSenti][numTopics];
    cntDT = new IntegerMatrix(numDocuments, numTopics);
    sumDT = new int[numDocuments];
    cntDS = new IntegerMatrix(numDocuments, numSenti);
    sumDS = new int[numDocuments];
    initHyperParameters(betaSenti);
  }

  double[][] getBetaSenti(double betaSenti[]) {
    double[][] ret = new double[numSenti][numSentiWords];
    for (int j = 0; j < numSenti; j++) {
      for (int i = 0; i < numSentiWords; i++) {
        ret[j][i] = getBeta(betaSenti, i, j);
      }
    }

    return ret;
  }

  /**
   * Gets the beta[senti] value for a sentiment word.
   */
  double getBeta(double[] betaSenti, Integer wordIdx, int s) {
    // same senti
    if (seedWords[s].contains(wordIdx)) {
      return betaSenti[0];
    }
    // opposite senti
    if (seedWords[1 - s].contains(wordIdx)) {
      return betaSenti[1];
    }
    return betaSenti[2];
  }

  /**
   * Initializes hyper parameters and related quantities for Gibbs sampling.
   */
  void initHyperParameters(double betaSenti[]) {
    sumAlpha = alpha * numTopics;
    sumGamma = 0;
    for (double gamma : gammas) {
      sumGamma += gamma;
    }
    sumBetaAspect = betaAspect * numAspectWords;
    sumBetaSenti = new double[numSenti];
    int numSeedWords = seedWords[0].size() + seedWords[1].size();
    double sumBetaOther = betaSenti[2] * (numSentiWords - numSeedWords);
    for (int s = 0; s < numSenti; s++) {
      int numSameSentimentWords = seedWords[s].size();
      sumBetaSenti[s] = sumBetaOther + numSameSentimentWords * betaSenti[0]
          + (numSeedWords - numSameSentimentWords) * betaSenti[1];
    }
  }

  /**
   * Creates an existing model from the specified file, continuing the sampling
   * from the <code>iter</code> iteration.
   * 
   * @param savedModel
   */
  public static BSModel loadModel(String savedModel, int iter) {
    BSModel model = null;
    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(new FileInputStream(savedModel));
      model = (BSModel) in.readObject();
      model.isExisting = true;
      in.close();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return model;
  }
}
