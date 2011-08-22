package edu.kaist.uilab.bs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Gibbs sampling for the BS model.
 * 
 * @author trung
 */
public class BSGibbsSampler {
  private BSModel model;

  /**
   * Creates a Gibbs sampler with the given model.
   */
  public BSGibbsSampler(BSModel model) {
    this.model = model;
  }

  public BSModel getModel() {
    return model;
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

  private void initDocs(int from, int to) {
    int cnt = 0;
    for (int docNo = from; docNo < to; docNo++) {
      Document document = model.getDocuments().get(docNo);
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
          cnt++;
        }
      }
    }
    System.out.printf("\nSentences with more than 1 sentiments: %d\n", cnt);
  }

  /**
   * Samples using Gibbs sampling.
   * 
   * @param numIters
   *          total iterations to run
   * @param startingIter
   *          the starting iteration
   * @param savingInterval
   *          the interval to save the training model (0 if no saving)
   * @param burnin
   *          burning in period
   */
  public void gibbsSampling(int numIters, int startingIter, int savingInterval,
      int burnin) throws IOException {
    System.out.printf("Gibbs sampling started (Iterations: %d)\n", numIters);
    if (!model.isExisting) {
      initDocs(0, model.numDocuments);
    }
    double startTime = System.currentTimeMillis();
    for (int iter = startingIter; iter < numIters; iter++) {
      int realIter = iter + 1;
      if (realIter % 50 == 0) {
        System.out.println();
      }
      System.out.printf(" %d ", realIter);
      for (Document document : model.getDocuments()) {
        sampleForDoc(document);
      }
      if (realIter > burnin && savingInterval > 0
          && realIter % savingInterval == 0 && realIter != numIters) {
        saveModel(realIter);
        model.writeModelOutput(realIter);
      }
    }
    System.out.printf("\nGibbs sampling terminated. (%.4fs)\n",
        (System.currentTimeMillis() - startTime) / 1000);
    // update beta and save the last sample
    saveModel(numIters);
    model.writeModelOutput(numIters);
  }

  /**
   * Samples a document.
   * 
   * @param document
   */
  private void sampleForDoc(Document document) {
    double[][] probTable = new double[model.numTopics][model.numSenti];
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
          for (int k = 0; k < model.numTopics; k++) {
            probTable[k][s] = 0;
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
  private void unsetTopicSentiment(int docIdx, Sentence sentence) {
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
  private void setTopicSentiment(int docIdx, Sentence sentence, int newTopic,
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
  private boolean hasOppositeSentiment(Sentence sentence, int sentiment) {
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
  private void saveModel(int iter) {
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
}
