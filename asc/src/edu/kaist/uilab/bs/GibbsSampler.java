package edu.kaist.uilab.bs;

import java.io.File;
import java.io.IOException;

/**
 * Gibbs sampling for the BS model.
 * 
 * @author trung
 */
public class GibbsSampler {
  Model model;

  /**
   * Creates a Gibbs sampler with the given model.
   */
  public GibbsSampler(Model model) {
    this.model = model;
  }

  public Model getModel() {
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

  /**
   * Inits for Gibbs sampling.
   * 
   * @param from
   *          starting document index
   * @param to
   *          end document index
   */
  void initDocs(int from, int to) {
    int cnt = 0;
    for (int docNo = from; docNo < to; docNo++) {
      Document document = model.getDocuments().get(docNo);
      for (Sentence sentence : document.getSentences()) {
        initSentence(docNo, sentence);
        if (sentence.getSenti() == -1) {
          cnt++;
        }
      }
    }
    System.out.printf("\nSentences with more than 1 sentiments: %d\n", cnt);
  }

  /**
   * Initializes a sentiment and topic for <code>sentence</code>.
   * 
   * @param sentence
   */
  void initSentence(int docNo, Sentence sentence) {
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
      setTopic(docNo, sentence, newTopic);
      setSentiment(docNo, sentence, newTopic, newSenti);
    } else {
      sentence.setSenti(-1);
    }
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
        model.writeModelOutput(realIter);
      }
    }
    System.out.printf("\nGibbs sampling terminated. (%.4fs)\n",
        (System.currentTimeMillis() - startTime) / 1000);
    model.writeModelOutput(numIters);
  }

  /**
   * Samples a document.
   * 
   * @param document
   */
  void sampleForDoc(Document document) {
    int docIdx = document.getDocNo();
    for (Sentence sentence : document.getSentences()) {
      if (sentence.getSenti() == -1) {
        continue;
      }
      sampleForSentence(docIdx, sentence);
    }
  }

  /**
   * Samples a new topic and sentiment for <code>sentence</code>.
   * 
   * @param docIdx
   * @param sentence
   */
  void sampleForSentence(int docIdx, Sentence sentence) {
    double[][] probTable = new double[model.numTopics][model.numSenti];
    unsetTopic(docIdx, sentence);
    unsetSentiment(docIdx, sentence);
    double sumProb = 0;
    for (int s = 0; s < model.numSenti; s++) {
      if (hasOppositeSentiment(sentence, s)) {
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
    setTopic(docIdx, sentence, newTopic);
    setSentiment(docIdx, sentence, newTopic, newSenti);
  }

  /**
   * Unsets the sentiment of <code>sentence</code> in the given document.
   * 
   * @param docIdx
   * @param sentence
   */
  void unsetSentiment(int docIdx, Sentence sentence) {
    int oldTopic = sentence.getTopic();
    int oldSenti = sentence.getSenti();
    model.cntDS.decValue(docIdx, oldSenti);
    model.sumDS[docIdx]--;
    for (Integer sentiWord : sentence.getSentiWords()) {
      model.cntSWT[oldSenti].decValue(sentiWord, oldTopic);
      model.sumSTW[oldSenti][oldTopic]--;
    }
  }

  /**
   * Unsets the topic of <code>sentence</code> in the given document.
   * 
   * @param docIdx
   * @param sentence
   */
  void unsetTopic(int docIdx, Sentence sentence) {
    int oldTopic = sentence.getTopic();
    model.cntDT.decValue(docIdx, oldTopic);
    model.sumDT[docIdx]--;
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
   * @param topic
   * @param newSenti
   */
  void setSentiment(int docIdx, Sentence sentence, int topic, int newSenti) {
    sentence.setSenti(newSenti);
    for (Integer sentiWord : sentence.getSentiWords()) {
      model.cntSWT[newSenti].incValue(sentiWord, topic);
      model.sumSTW[newSenti][topic]++;
    }
    model.cntDS.incValue(docIdx, newSenti);
    model.sumDS[docIdx]++;
  }

  /**
   * Sets new topic for <code>sentence</code> in the given document.
   * 
   * @param docIdx
   * @param sentence
   * @param newTopic
   */
  void setTopic(int docIdx, Sentence sentence, int newTopic) {
    sentence.setTopic(newTopic);
    for (Integer aspectWord : sentence.getAspectWords()) {
      model.cntWT.incValue(aspectWord, newTopic);
      model.sumWT[newTopic]++;
    }
    model.cntDT.incValue(docIdx, newTopic);
    model.sumDT[docIdx]++;
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
    for (Integer sentiWord : sentence.getSentiWords()) {
      if (model.seedWords[1 - sentiment].contains(sentiWord)) {
        return true;
      }
    }

    return false;
  }
}
