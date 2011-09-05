package edu.kaist.uilab.asc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Vector;

import edu.kaist.uilab.asc.data.Document;
import edu.kaist.uilab.asc.data.SamplingWord;
import edu.kaist.uilab.asc.data.Sentence;
import edu.kaist.uilab.asc.data.SentiWord;
import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.IntegerMatrix;
import edu.kaist.uilab.opt.LBFGS;

/**
 * Gibbs sampling for the ASC model.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class AscGibbsSampler {
  AbstractAscModel model;
  double[][] probTable;
  int startingIter;

  /**
   * Creates a base asc implementation with provided parameters.
   * 
   * @param mTopics
   * @param mNumSentis
   * @param data.wordList
   * @param data.documents
   * @param numEnglishDocuments
   * @param sentiWords
   * @param data.alpha
   * @param data.gammas
   * @param graphFile
   */
  public AscGibbsSampler(AbstractAscModel model) {
    this.model = model;
    probTable = new double[model.numTopics][model.numSenti];
  }

  /**
   * Default constructor
   */
  public AscGibbsSampler() {
  }

  /**
   * Sets the output dir for this model.
   * <p>
   * If an output dir is not set, the model will output to the current
   * directory.
   * 
   * @param dir
   */
  public void setOutputDir(String dir) {
    model.outputDir = dir + "(" + model.extraInfo + ")";
    new File(model.outputDir).mkdir();
  }

  /**
   * Initializes for gibbs sampling.
   */
  void gibbsInit() {
    model.sumSTW = new int[model.numSenti][model.numTopics];
    model.sumDST = new int[model.numDocuments][model.numSenti];
    model.sumDS = new int[model.numDocuments];
    model.matrixSWT = new IntegerMatrix[model.numSenti];
    model.matrixSDT = new IntegerMatrix[model.numSenti];
    for (int i = 0; i < model.numSenti; i++) {
      model.matrixSWT[i] = new IntegerMatrix(model.vocabSize, model.numTopics);
      model.matrixSDT[i] = new IntegerMatrix(model.numDocuments,
          model.numTopics);
    }
    model.matrixDS = new IntegerMatrix(model.numDocuments, model.numSenti);
  }

  void initDocs(int from, int to, boolean randomInit) {
    for (int docNo = from; docNo < to; docNo++) {
      Document document = model.documents.get(docNo);
      for (Sentence sentence : document.getSentences()) {
        int newSenti = -1;
        int numSentenceSenti = 0;
        for (SamplingWord sWord : sentence.getWords()) {
          SentiWord word = (SentiWord) sWord;
          int wordNo = word.getWordNo();
          for (int s = 0; s < model.sentiWordsList.size(); s++) {
            if (model.sentiWordsList.get(s).contains(wordNo)) {
              if (numSentenceSenti == 0 || s != newSenti)
                numSentenceSenti++;
              word.priorSentiment = s;
              newSenti = s;
            }
          }
        }
        // if sentiment of the sentence is not determined, get random sentiment
        if (randomInit || numSentenceSenti != 1) {
          newSenti = (int) (Math.random() * model.numSenti);
        }
        if (numSentenceSenti <= 1) {
          int newTopic = (int) (Math.random() * model.numTopics);
          sentence.setTopic(newTopic);
          sentence.setSenti(newSenti);
          for (SamplingWord sWord : sentence.getWords()) {
            ((SentiWord) sWord).setSentiment(newSenti);
            sWord.setTopic(newTopic);
            model.matrixSWT[newSenti].incValue(sWord.getWordNo(), newTopic);
            model.sumSTW[newSenti][newTopic]++;
          }
          model.matrixSDT[newSenti].incValue(docNo, newTopic);
          model.matrixDS.incValue(docNo, newSenti);
          model.sumDST[docNo][newSenti]++;
          model.sumDS[docNo]++;
        } else {
          // // TODO(trung): this is added to eliminate all sentences with > 2
          // sentiments!!
          sentence.setSenti(-1);
        }
      }
    }
  }

  /**
   * Samples using Gibbs sampling.
   * 
   * @param numIters
   *          total iterations to run
   * @param savingInterval
   *          the interval to save the training model (0 if no saving)
   * @param burnIn
   *          burning in period
   * @param optimizationInterval
   *          interval to optimize beta over y
   * @param numThreads
   *          how many threads to run
   */
  public void gibbsSampling(int numIters, int savingInterval, int burnIn,
      int optimizationInterval, int numThreads) throws IOException {
    System.out.printf("Gibbs sampling started (Iterations: %d)\n", numIters);
    int numEffectiveDocuments = model.numEnglishDocuments;
    if (!model.isExisting) {
      gibbsInit();
      // TODO(trung): this may not work for a saved model
      initDocs(0, model.numEnglishDocuments, false);
      startingIter = 0;
    }
    double startTime = System.currentTimeMillis();
    for (int iter = startingIter; iter < numIters; iter++) {
      int realIter = iter + 1;
      if (realIter % 50 == 0) {
        System.out.println();
      }
      System.out.printf(" %d ", realIter);
      if (realIter * 2 == numIters) {
        numEffectiveDocuments = model.numDocuments;
        initDocs(model.numEnglishDocuments, model.numDocuments, false);
        model.effectiveVocabSize = model.vocabSize;
        model.extendVars();
        model.updateBeta();
        model.graph.initGraph(model.graphFile, "\t");
      }
      if (realIter >= burnIn && realIter % optimizationInterval == 0
          && realIter < numIters) {
        optimizeBeta();
      }
      for (int docNo = 0; docNo < numEffectiveDocuments; docNo++) {
        sampleForDoc(model.documents.get(docNo));
      }
      if (realIter * 2 > numIters && savingInterval > 0
          && realIter % savingInterval == 0 && realIter != numIters) {
        saveModel(realIter);
        writeModelOutput(realIter);
      }
    }
    System.out.printf("Gibbs sampling terminated. (%.4fs)\n",
        (System.currentTimeMillis() - startTime) / 1000);
    // update beta and save the last sample
    optimizeBeta();
    saveModel(numIters);
    writeModelOutput(numIters);
  }

  /**
   * Optimizes values of betas over y.
   */
  void optimizeBeta() {
    System.out.println("\nOptimizing beta over y...\n");
    double startTime = System.currentTimeMillis();
    if (!optimizeWithLbfgs()) {
      System.err.println("Error with optimization");
      System.exit(-1);
    }
    System.out.printf("Optimization done (%.4fs)\n",
        (System.currentTimeMillis() - startTime) / 1000);
  }

  /**
   * Optimizes beta over y.
   * 
   * @param mVars
   *          the solution
   * @return true if an solution was found, false if an error occurs
   */
  boolean optimizeWithLbfgs() {
    // optimize y
    int numCorrections = 4;
    int[] iflag = new int[2];
    boolean supplyDiag = false;
    double machinePrecision = 1.1920929e-7;
    // starting point
    double[] diag = new double[model.vars.length];
    // iprint[0] = output every iprint[0] iterations
    // iprint[1] = 0~3 : least to most detailed output
    int[] iprint = new int[] { 50, 0 };
    do {
      try {
        LBFGS.lbfgs(model.vars.length, numCorrections, model.vars,
            model.computeFunction(null), model.computeGradient(null),
            supplyDiag, diag, iprint, model.getOptimizationAccuracy(),
            machinePrecision, iflag);
        model.variablesToY();
        model.updateBeta();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } while (iflag[0] == 1);
    return (iflag[0] == 0);
  }

  /**
   * Samples a document.
   * 
   * @param currentDoc
   */
  private void sampleForDoc(Document currentDoc) {
    int docNo = currentDoc.getDocNo();
    for (Sentence sentence : currentDoc.getSentences()) {
      if (sentence.getSenti() == -1) {
        continue;
      }
      int oldTopic = sentence.getTopic();
      int oldSenti = sentence.getSenti();
      model.matrixSDT[oldSenti].decValue(docNo, oldTopic);
      model.matrixDS.decValue(docNo, oldSenti);
      model.sumDST[docNo][oldSenti]--;
      model.sumDS[docNo]--;
      for (SamplingWord sWord : sentence.getWords()) {
        model.matrixSWT[oldSenti].decValue(sWord.getWordNo(), oldTopic);
        model.sumSTW[oldSenti][oldTopic]--;
      }

      // Sampling
      Map<SamplingWord, Integer> wordCnt = sentence.getWordCnt();
      double sumProb = 0;
      for (int si = 0; si < model.numSenti; si++) {
        if (trim(wordCnt, si)) {
          // forced sentiment orientation (by assigning 0 probability to the
          // opposite senti-aspect. example: if a sentences contains the word
          // "excellent", then the probability of being assigned sentiment
          // negative is 0.
          for (int ti = 0; ti < model.numTopics; ti++) {
            probTable[ti][si] = Math.exp(-80);
            sumProb += probTable[ti][si];
          }
        } else {
          for (int ti = 0; ti < model.numTopics; ti++) {
            // this quantity is the beta dependent entities in equation for
            // conditional probability in yohan's paper. it is mathematically
            // correct but
            // not straightforward by just looking at the implementation.
            // TODO(trung): in fact Yohan only use sumBeta[si]
            double beta0 = model.sumSTW[si][ti] + model.sumBeta[si][ti];
            int m0 = 0;
            double expectTSW = 1;
            for (SamplingWord sWord : wordCnt.keySet()) {
              SentiWord word = (SentiWord) sWord;
              double betaw = model.matrixSWT[si].getValue(word.getWordNo(), ti)
                  + model.beta[si][ti][word.getWordNo()];
              int cnt = wordCnt.get(word);
              for (int m = 0; m < cnt; m++) {
                expectTSW *= (betaw + m) / (beta0 + m0);
                m0++;
              }
            }
            probTable[ti][si] = (model.matrixSDT[si].getValue(docNo, ti) + model.alpha)
                / (model.sumDST[docNo][si] + model.sumAlpha)
                * (model.matrixDS.getValue(docNo, si) + model.gammas[si])
                * expectTSW;
            sumProb += probTable[ti][si];
          }
        }
      }

      int newTopic = -1, newSenti = -1;
      double randNo = Math.random() * sumProb;
      double tmpSumProb = 0;
      boolean found = false;
      for (int ti = 0; ti < model.numTopics; ti++) {
        for (int si = 0; si < model.numSenti; si++) {
          tmpSumProb += probTable[ti][si];
          if (randNo < tmpSumProb) {
            newTopic = ti;
            newSenti = si;
            found = true;
            break;
          }
        }
        if (found)
          break;
      }

      sentence.setTopic(newTopic);
      sentence.setSenti(newSenti);
      for (SamplingWord sWord : sentence.getWords()) {
        SentiWord word = (SentiWord) sWord;
        word.setTopic(newTopic);
        word.setSentiment(newSenti);
        model.matrixSWT[newSenti].incValue(word.getWordNo(), newTopic);
        model.sumSTW[newSenti][newTopic]++;
      }
      model.matrixSDT[newSenti].incValue(docNo, newTopic);
      model.matrixDS.incValue(docNo, newSenti);
      model.sumDST[docNo][newSenti]++;
      model.sumDS[docNo]++;
    }
  }

  // Check to see if the sentence contains one sentiment (seed) word
  private boolean trim(Map<SamplingWord, Integer> wordCnt, int si) {
    // TODO(trung): uncomment if see worse behavior
    for (SamplingWord sWord : wordCnt.keySet()) {
      SentiWord word = (SentiWord) sWord;
      if (word.priorSentiment != null && word.priorSentiment != si) {
        return true;
      }
    }
    return false;
  }

  /**
   * Saves the model at current the iteration <code>iter</code>.
   * 
   * @param iter
   */
  void saveModel(int iter) {
    try {
      new File(model.outputDir + "/" + iter).mkdir();
      ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
          model.outputDir + "/" + iter + "/model.gz"));
      out.writeObject(model);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Writes output of the model at the specified iteration.
   * 
   * @param dir
   * @throws Exception
   */
  void writeModelOutput(int iter) {
    try {
      String dir = model.outputDir + "/" + iter;
      DoubleMatrix[] phi = Inference.computePhi(model.matrixSWT,
          model.sumSTW, model.beta, model.sumBeta);
      writePhi(phi, dir + "/Phi.csv");
      model.writeSampleY(dir);
      writeBeta(dir + "/logbeta.csv");
      printTopWords(phi, dir + "/TopWords.csv");
      printTopWords(
          buildTermScoreMatrix(phi, model.numTopics * model.numSenti), dir
              + "/TopWordsByTermScore.csv");
      printTopWords(buildTermScoreMatrix(phi, model.numTopics), dir
          + "/TopWordsByHalfTermScore.csv");
      writeTheta(Inference.computeTheta(model.matrixSDT, model.sumDST,
          model.alpha, model.sumAlpha), dir + "/Theta.csv");
      DoubleMatrix pi = Inference.calculatePi(model.matrixDS, model.sumDS,
          model.gammas, model.sumGamma);
      pi.writeMatrixToCSVFile(dir + "/Pi.csv");
      writeClassificationSummary(pi, dir + "/classification.txt");
      System.err.println("\nModel saved and written to " + dir);
    } catch (IOException e) {
      System.err.println("Error writing model output");
      e.printStackTrace();
    }
  }

  void writeClassificationSummary(DoubleMatrix pi, String file)
      throws IOException {
    // get classification accuracy for english documents
    int observedSenti, inferedSenti, numCorrect = 0;
    int numNotRated = 0, numNeutral = 0, numPos = 0, numSubjective = 0;
    for (int i = 0; i < model.numEnglishDocuments; i++) {
      Document document = model.documents.get(i);
      double rating = document.getRating();
      if (rating != 3.0 && rating != -1.0) {
        numSubjective++;
        observedSenti = rating > 3.0 ? 0 : 1;
        inferedSenti = pi.getValue(i, 0) > pi.getValue(i, 1) ? 0 : 1;
        if (observedSenti == inferedSenti) {
          numCorrect++;
        }
        if (observedSenti == 0) {
          numPos++;
        }
      } else {
        if (rating == 3.0) {
          numNeutral++;
        } else {
          numNotRated++;
        }
      }
    }
    PrintWriter out = new PrintWriter(file);
    out.println("English reviews:");
    out.printf("\tSubjective:\t%d\tpos = %d(%.2f)\n", numSubjective, numPos,
        ((double) numPos) / numSubjective);
    out.printf("\tNeutral:\t%d\n", numNeutral);
    out.printf("\tNot rated:\t%d\n", numNotRated);
    out.printf("\tAccuracy:\t%.5f\n", ((double) numCorrect) / numSubjective);
    out.println("-------------------");
    numCorrect = 0;
    numNotRated = 0;
    numNeutral = 0;
    numSubjective = 0;
    numPos = 0;
    for (int i = model.numEnglishDocuments; i < model.numDocuments; i++) {
      Document document = model.documents.get(i);
      double rating = document.getRating();
      if (rating != 3.0 && rating != -1.0) {
        numSubjective++;
        observedSenti = rating > 3.0 ? 0 : 1;
        inferedSenti = pi.getValue(i, 0) > pi.getValue(i, 1) ? 0 : 1;
        if (observedSenti == inferedSenti) {
          numCorrect++;
        }
        if (observedSenti == 0) {
          numPos++;
        }
      } else {
        if (rating == 3.0) {
          numNeutral++;
        } else {
          numNotRated++;
        }
      }
    }
    out.println("French reviews:");
    out.printf("\tSubjective:\t%d\tpos = %d(%.2f)\n", numSubjective, numPos,
        ((double) numPos) / numSubjective);
    out.printf("\tNeutral:\t%d\n", numNeutral);
    out.printf("\tNot rated:\t%d\n", numNotRated);
    out.printf("\tAccuracy:\t%.5f\n", ((double) numCorrect) / numSubjective);
    out.close();
  }

  void writeTheta(double[][][] theta, String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int s = 0; s < model.numSenti; s++)
      for (int t = 0; t < model.numTopics; t++)
        out.print("S" + s + "-T" + t + ",");
    out.println();
    for (int d = 0; d < model.numDocuments; d++) {
      for (int s = 0; s < model.numSenti; s++) {
        for (int t = 0; t < model.numTopics; t++) {
          out.print(theta[s][d][t] + ",");
        }
      }
      out.println();
    }
    out.close();
  }

  void writePhi(DoubleMatrix[] phi, String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int s = 0; s < model.numSenti; s++)
      for (int t = 0; t < model.numTopics; t++)
        out.print(",S" + s + "-T" + t);
    out.println();
    for (int w = 0; w < model.wordList.size(); w++) {
      out.print(model.wordList.get(w));
      for (int s = 0; s < model.numSenti; s++) {
        for (int t = 0; t < model.numTopics; t++) {
          out.print("," + phi[s].getValue(w, t));
        }
      }
      out.println();
    }
    out.close();
  }

  void writeBeta(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int s = 0; s < model.numSenti; s++)
      for (int t = 0; t < model.numTopics; t++)
        out.print(",S" + s + "-T" + t);
    out.println();
    for (int w = 0; w < model.wordList.size(); w++) {
      out.print(model.wordList.get(w) + ",");
      for (int s = 0; s < model.numSenti; s++) {
        for (int t = 0; t < model.numTopics; t++) {
          out.printf("%.5f,", Math.log(model.beta[s][t][w]));
        }
      }
      out.println();
    }
    out.close();
  }

  /**
   * Prints top words.
   * 
   * @param dir
   */
  void printTopWords(DoubleMatrix[] matrix, String file) throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (int s = 0; s < model.numSenti; s++) {
      for (int t = 0; t < model.numTopics; t++) {
        out.print("S" + s + "-T" + t + ",");
      }
    }
    int[][][] indices = getWordIndices(matrix);
    int idx;
    out.println();
    for (int w = 0; w < model.numProbWords; w++) {
      for (int s = 0; s < model.numSenti; s++) {
        for (int t = 0; t < model.numTopics; t++) {
          idx = indices[s][t][w];
          out.printf("%s (%.3f),", model.wordList.get(idx),
              matrix[s].getValue(idx, t));
        }
      }
      out.println();
    }
    out.close();
  }

  /**
   * Builds the term-score matrix from the inferred values of Phi.
   * 
   * @return
   */
  private DoubleMatrix[] buildTermScoreMatrix(DoubleMatrix[] phi, int topics) {
    DoubleMatrix[] termScore = new DoubleMatrix[phi.length];
    double sumOfLogs[] = new double[model.vocabSize];
    // compute the sum of logs for each word
    for (int w = 0; w < model.vocabSize; w++) {
      sumOfLogs[w] = 0.0;
      for (int s = 0; s < model.numSenti; s++) {
        for (int t = 0; t < model.numTopics; t++) {
          sumOfLogs[w] += Math.log(phi[s].getValue(w, t));
        }
      }
    }
    double score, prob;
    for (int s = 0; s < model.numSenti; s++) {
      termScore[s] = new DoubleMatrix(model.vocabSize, model.numTopics);
      for (int t = 0; t < model.numTopics; t++) {
        for (int w = 0; w < model.vocabSize; w++) {
          prob = phi[s].getValue(w, t);
          score = prob * (Math.log(prob) - sumOfLogs[w] / topics);
          termScore[s].setValue(w, t, score);
        }
      }
    }
    return termScore;
  }

  /**
   * Gets indices of top words for each topic.
   * 
   * @return
   */
  private int[][][] getWordIndices(DoubleMatrix[] matrix) {
    int[][][] indices = new int[model.numSenti][model.numTopics][model.numProbWords];
    for (int s = 0; s < model.numSenti; s++) {
      for (int t = 0; t < model.numTopics; t++) {
        Vector<Integer> sortedIndexList = matrix[s].getSortedColIndex(t,
            model.numProbWords);
        for (int w = 0; w < sortedIndexList.size(); w++) {
          indices[s][t][w] = sortedIndexList.get(w);
        }
      }
    }
    return indices;
  }
}
