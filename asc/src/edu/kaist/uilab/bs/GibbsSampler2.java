package edu.kaist.uilab.bs;

/**
 * Gibbs sampler for the BS model that allows a sentence to have no sentiment.
 * 
 * @author trung
 */
public class GibbsSampler2 extends GibbsSampler {

  /**
   * Constructor
   * 
   * @param model
   */
  public GibbsSampler2(Model model) {
    super(model);
  }

  @Override
  void initDocs(int from, int to) {
    int cnt = 0;
    for (int docNo = from; docNo < to; docNo++) {
      Document document = model.getDocuments().get(docNo);
      for (Sentence sentence : document.getSentences()) {
        if (sentence.hasSentimentWords()) {
          initSentence(docNo, sentence);
          if (sentence.getSenti() == -1) {
            cnt++;
          }
        } else {
          int newTopic = (int) (Math.random() * model.numTopics);
          setTopic(docNo, sentence, newTopic);
        }
      }
    }
    System.out.printf("\nSentences with more than 1 sentiments: %d\n", cnt);
  }

  /**
   * Samples a document.
   * 
   * @param document
   */
  void sampleForDoc(Document document) {
    int docIdx = document.getDocNo();
    for (Sentence sentence : document.getSentences()) {
      if (sentence.hasSentimentWords()) {
        if (sentence.getSenti() != -1) {
          sampleForSentence(docIdx, sentence);
        }
      } else {
        sampleForSentenceWithNoSentiment(docIdx, sentence);
      }
    }
  }

  /**
   * Samples a new topic for <code>sentence</code>.
   * 
   * @param docIdx
   * @param sentence
   */
  void sampleForSentenceWithNoSentiment(int docIdx, Sentence sentence) {
    double[] probTable = new double[model.numTopics];
    unsetTopic(docIdx, sentence);
    double sumProb = 0;
    for (int k = 0; k < model.numTopics; k++) {
      double prob = (model.cntDT.getValue(docIdx, k) + model.alpha);
      int x = 0;
      for (Integer aspectWord : sentence.getAspectWords()) {
        prob *= (model.cntWT.getValue(aspectWord, k) + model.betaAspect)
            / (model.sumWT[k] + model.sumBetaAspect + x++);
      }
      probTable[k] = prob;
      sumProb += prob;
    }
    // sample from a discrete distribution
    int newTopic = -1;
    double randNo = Math.random() * sumProb;
    double sumSoFar = 0;
    for (int k = 0; k < model.numTopics; k++) {
      sumSoFar += probTable[k];
      if (randNo < sumSoFar) {
        newTopic = k;
        break;
      }
    }
    setTopic(docIdx, sentence, newTopic);
  }
}
