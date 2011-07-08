package edu.kaist.uilab.bs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Vector;

import edu.kaist.uilab.asc.Inference;
import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.Utils;

/**
 * Gibbs sampling for the BS model.
 * 
 * @author trung
 */
public class BSGibbsSampler {
  BSModel model;
  double[][] probTable;
  int startingIter;

  /**
   * Creates a Gibbs sampler with the given model.
   */
  public BSGibbsSampler(BSModel model) {
    this.model = model;
    probTable = new double[model.numTopics][model.numSenti];
  }

  /**
   * Default constructor
   */
  public BSGibbsSampler() {
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

  void initDocs(int from, int to) {
    for (int docNo = from; docNo < to; docNo++) {
      Document document = model.documents.get(docNo);
      for (Sentence sentence : document.getSentences()) {
        int newSenti = -1;
        int numSentenceSenti = 0;
        for (Integer sentiWord : sentence.getSentiWords()) {
          for (int s = 0; s < model.numSenti; s++) {
            if (model.seedWords[s].contains(sentiWord)) {
              if (numSentenceSenti == 0 || s != newSenti)
                numSentenceSenti++;
              newSenti = s;
            }
          }
        }
        // if sentiment of the sentence is not determined, get random sentiment
        if (numSentenceSenti != 1) {
          newSenti = (int) (Math.random() * model.numSenti);
        }
        if (numSentenceSenti <= 1) {
          int newTopic = (int) (Math.random() * model.numTopics);
          setTopicSentiment(docNo, sentence, newTopic, newSenti);
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
   * @param burnin
   *          burning in period
   */
  public void gibbsSampling(int numIters, int savingInterval, int burnin)
      throws IOException {
    System.out.printf("Gibbs sampling started (Iterations: %d)\n", numIters);
    if (!model.isExisting) {
      initDocs(0, model.numDocuments);
      startingIter = 0;
    }
    double startTime = System.currentTimeMillis();
    for (int iter = startingIter; iter < numIters; iter++) {
      int realIter = iter + 1;
      if (realIter % 50 == 0) {
        System.out.println();
      }
      System.out.printf(" %d ", realIter);
      for (Document document : model.documents) {
        sampleForDoc(document);
      }
      if (realIter > burnin && savingInterval > 0
          && realIter % savingInterval == 0 && realIter != numIters) {
        saveModel(realIter);
        writeModelOutput(realIter);
      }
    }
    System.out.printf("Gibbs sampling terminated. (%.4fs)\n",
        (System.currentTimeMillis() - startTime) / 1000);
    // update beta and save the last sample
    saveModel(numIters);
    writeModelOutput(numIters);
  }

  /**
   * Samples a document.
   * 
   * @param document
   */
  private void sampleForDoc(Document document) {
    int docIdx = document.getDocNo();
    for (Sentence sentence : document.getSentences()) {
      if (sentence.getSenti() == -1) {
        continue;
      }
      unsetTopicSentiment(docIdx, sentence);
      double sumProb = 0;
      for (int s = 0; s < model.numSenti; s++) {
        if (hasOppositeSentiment(sentence, s)) {
          // forced sentiment orientation (by assigning 0 probability to the
          // opposite senti-aspect. example: if a sentences contains the word
          // "excellent", then the probability of being assigned negative
          // sentiment is 0.
          for (int t = 0; t < model.numTopics; t++) {
            probTable[t][s] = Math.exp(-80);
            sumProb += probTable[t][s];
          }
        } else {
          for (int k = 0; k < model.numTopics; k++) {
            double prob = (model.cntDT.getValue(docIdx, k) + model.alpha)
                * (model.cntDS.getValue(docIdx, s) + model.gammas[s]);
            int x = 0;
            for (Integer aspectWord : sentence.getAspectWords()) {
              prob *= (model.cntWT.getValue(aspectWord, k) + model.betaAspect)
                  / (model.sumWT[k] + model.sumBetaAspect + x++);
            }
            x = 0;
            for (Integer sentiWord : sentence.getSentiWords()) {
              prob *= (model.cntSWT[s].getValue(sentiWord, k) + model.betaSenti[s][sentiWord])
                  / (model.sumSTW[s][k] + model.sumBetaSenti[s] + x++);
            }
            probTable[k][s] = prob;
            sumProb += prob;
          }
        }
      }
      // sample from a discrete distribution
      int newTopic = -1, newSenti = -1;
      double randNo = Math.random() * sumProb;
      double sumSoFar = 0;
      boolean found = false;
      for (int k = 0; k < model.numTopics; k++) {
        for (int j = 0; j < model.numSenti; j++) {
          sumSoFar += probTable[k][j];
          if (randNo < sumSoFar) {
            newTopic = k;
            newSenti = j;
            found = true;
            break;
          }
        }
        if (found)
          break;
      }
      setTopicSentiment(docIdx, sentence, newTopic, newSenti);
    }
  }

  /**
   * Unsets the topic and sentiment of the sentence in the given document.
   * 
   * @param docIdx
   * @param sentence
   */
  void unsetTopicSentiment(int docIdx, Sentence sentence) {
    int oldTopic = sentence.getTopic();
    int oldSenti = sentence.getSenti();
    model.cntDT.decValue(docIdx, oldTopic);
    model.cntDS.decValue(docIdx, oldSenti);
    model.sumDT[docIdx]--;
    model.sumDS[docIdx]--;
    for (Integer sentiWord : sentence.getSentiWords()) {
      model.cntSWT[oldSenti].decValue(sentiWord, oldTopic);
      model.sumSTW[oldSenti][oldTopic]--;
    }
    for (Integer aspectWord : sentence.getAspectWords()) {
      model.cntWT.decValue(aspectWord, oldTopic);
      model.sumWT[oldTopic]--;
    }
  }

  /**
   * Sets the sentiment and topic for the sentence in the given document.
   * 
   * @param docIdx
   * @param sentence
   * @param newTopic
   * @param newSenti
   */
  void setTopicSentiment(int docIdx, Sentence sentence, int newTopic,
      int newSenti) {
    sentence.setTopic(newTopic);
    sentence.setSenti(newSenti);
    for (Integer aspectWord : sentence.getAspectWords()) {
      model.cntWT.incValue(aspectWord, newTopic);
      model.sumWT[newTopic]++;
    }
    for (Integer sentiWord : sentence.getSentiWords()) {
      model.cntSWT[newSenti].incValue(sentiWord, newTopic);
      model.sumSTW[newSenti][newTopic]++;
    }
    model.cntDT.incValue(docIdx, newTopic);
    model.cntDS.incValue(docIdx, newSenti);
    model.sumDT[docIdx]++;
    model.sumDS[docIdx]++;
  }

  /**
   * Returns true if the given sentence contains a word of the senti opposite to
   * the given <code>sentiment</code>.
   * 
   * @param sentence
   * @param sentiment
   * @return
   */
  boolean hasOppositeSentiment(Sentence sentence, int sentiment) {
    // TODO(trung): uncomment if see worse behavior
    for (Integer sentiWord : sentence.getSentiWords()) {
      if (model.seedWords[1 - sentiment].contains(sentiWord)) {
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
      DoubleMatrix[] phiSenti = Inference.computePhiSenti(model.cntSWT,
          model.sumSTW, model.betaSenti, model.sumBetaSenti);
      writePhiSenti(phiSenti, dir + "/PhiSenti.csv");
      printTopSentiWords(phiSenti, dir + "/TopSentiWords.csv");
      printTopSentiWords(buildTermscoreMatrix(phiSenti, model.numTopics), dir
          + "/TopSentiWordsByTermscore.csv");
      double[][] phiAspect = Inference.computePhiAspect(model.cntWT,
          model.sumWT, model.betaAspect, model.sumBetaAspect);
      printTopAspectWords(phiAspect, dir + "/TopAspectWords.csv");
      printTopAspectWords(buildTermscore(phiAspect), dir
          + "/TopAspectWordsByTermscore.csv");
      // TODO(trung): print top aspect words by term score and the phiAspect
      // matrix to csv file
      writeTheta(Inference.computeTheta(model.cntDT, model.sumDT, model.alpha,
          model.sumAlpha), dir + "/Theta.csv");
      DoubleMatrix pi = Inference.calculatePi(model.cntDS, model.sumDS,
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
    for (int i = 0; i < model.numDocuments; i++) {
      Document document = model.documents.get(i);
      double rating = document.getRating();
      if (rating != 3.0 && rating != -1.0) {
        numSubjective++;
        observedSenti = rating > 3.0 ? 0 : 1;
        inferedSenti = pi.getValue(i, 0) >= pi.getValue(i, 1) ? 0 : 1;
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
    out.close();
  }

  void writeTheta(double[][] theta, String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int t = 0; t < model.numTopics; t++)
        out.print("T" + t + ",");
    out.println();
    for (int d = 0; d < model.numDocuments; d++) {
      for (int t = 0; t < model.numTopics; t++) {
        out.print(theta[d][t] + ",");
      }
      out.println();
    }
    out.close();
  }

  void writePhiSenti(DoubleMatrix[] phi, String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int s = 0; s < model.numSenti; s++)
      for (int t = 0; t < model.numTopics; t++)
        out.print(",S" + s + "-T" + t);
    out.println();
    for (int w = 0; w < model.numSentiWords; w++) {
      out.print(model.sentiTable.idToSymbol(w));
      for (int s = 0; s < model.numSenti; s++) {
        for (int t = 0; t < model.numTopics; t++) {
          out.print("," + phi[s].getValue(w, t));
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
  void printTopSentiWords(DoubleMatrix[] matrix, String file)
      throws IOException {
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
          out.printf("%s (%.3f),", model.sentiTable.idToSymbol(idx),
              matrix[s].getValue(idx, t));
        }
      }
      out.println();
    }
    out.close();
  }

  /**
   * Prints top words assigned to an entity topic to the file.
   * 
   * @param thetad
   * @param file
   * @throws IOException
   */
  void printTopAspectWords(double[][] phi, String file) throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), "utf-8"));
    for (int topic = 0; topic < model.numTopics; topic++) {
      out.printf("T%d,", topic);
    }
    out.println();
    // ranking according to phi[k][i]
    int[][] topWordMatrix = new int[model.numTopics][];
    for (int topic = 0; topic < model.numTopics; topic++) {
      topWordMatrix[topic] = Utils.topColumns(phi, topic, model.numProbWords);
    }
    // write the inverse of the top word matrix (for easy visualization)
    int wordId;
    for (int i = 0; i < model.numProbWords; i++) {
      for (int topic = 0; topic < model.numTopics; topic++) {
        wordId = topWordMatrix[topic][i];
        out.printf("%s(%.5f),", model.aspectTable.idToSymbol(wordId),
            phi[topic][wordId]);
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
  private DoubleMatrix[] buildTermscoreMatrix(DoubleMatrix[] phi, int topics) {
    DoubleMatrix[] termScore = new DoubleMatrix[phi.length];
    double sumOfLogs[] = new double[model.numSentiWords];
    // compute the sum of logs for each word
    for (int w = 0; w < model.numSentiWords; w++) {
      sumOfLogs[w] = 0.0;
      for (int s = 0; s < model.numSenti; s++) {
        for (int t = 0; t < model.numTopics; t++) {
          sumOfLogs[w] += Math.log(phi[s].getValue(w, t));
        }
      }
    }
    double score, prob;
    for (int s = 0; s < model.numSenti; s++) {
      termScore[s] = new DoubleMatrix(model.numSentiWords, model.numTopics);
      for (int t = 0; t < model.numTopics; t++) {
        for (int w = 0; w < model.numSentiWords; w++) {
          prob = phi[s].getValue(w, t);
          score = prob * (Math.log(prob) - sumOfLogs[w] / topics);
          termScore[s].setValue(w, t, score);
        }
      }
    }
    return termScore;
  }

  private double[][] buildTermscore(double[][] phi) {
    double[][] termscore = new double[model.numTopics][model.numAspectWords];
    double sumOfLogs[] = new double[model.numAspectWords];
    // compute the sum of logs for each word
    for (int w = 0; w < model.numAspectWords; w++) {
      sumOfLogs[w] = 0.0;
      for (int t = 0; t < model.numTopics; t++) {
        sumOfLogs[w] += Math.log(phi[t][w]);
      }
    }
    for (int t = 0; t < model.numTopics; t++) {
      for (int w = 0; w < model.numAspectWords; w++) {
        termscore[t][w] = phi[t][w]
            * (Math.log(phi[t][w]) - sumOfLogs[w] / model.numTopics);
      }
    }

    return termscore;
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
