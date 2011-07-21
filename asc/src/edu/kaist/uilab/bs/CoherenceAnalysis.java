package edu.kaist.uilab.bs;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Analyzes using the coherence score.
 * 
 * @author trung
 */
public class CoherenceAnalysis {

  /**
   * Reports the coherence scores between topics.
   * 
   * @param file
   * @param aspectWords
   * @param sentiWords
   */
  private static void writeWithinCorpusTopicCoherence(String dir, BSModel model)
      throws IOException {
    int nSenti[] = new int[] { 25, 50 };
    int nAspect[] = new int[] { 25, 50 };
    String[][][] sentiWords = model.getTopSentiWords();
    String[][] aspectWords = model.getTopAspectWords();
    TwogramsCounter counter = model.getTwogramsCounter();
    for (int sentiments : nSenti) {
      for (int aspects : nAspect) {
        PrintWriter out = new PrintWriter(dir + "/coherence-S" + sentiments
            + "-A" + aspects + ".csv");
        for (int k1 = 0; k1 < model.numTopics; k1++) {
          for (int k2 = 0; k2 < model.numTopics; k2++) {
            out.print(computeCoherenceScore(counter, sentiWords[0][k1],
                sentiWords[1][k1], sentiments, aspectWords[k2], aspects) + ",");
          }
          out.println();
        }
        out.close();
      }
    }
  }

  private static void writeBetweenCorpusTopicCoherence(String dir,
      BSModel model1, BSModel model2) throws IOException {
    // TODO(trung): should only count the frequency of phrase in the corpus for
    // aspect because we want to evaluate that specific domain (i.e., HOW
    // GOOG/RELATED the sentiment of a corpus compared to other corpus). in that
    // case, it should be compared with the set of sentiment words for the
    // aspect corpus.
    // if (senti-aspect) pair is (elec-rest) then compares it with (sent-aspect)
    // for (rest-rest). if same amount of data then does not really matter
    int nSenti[] = new int[] { 25, 50 };
    int nAspect[] = new int[] { 25, 50 };
    String[][][] sentiWords = model1.getTopSentiWords();
    String[][] aspectWords = model2.getTopAspectWords();
    TwogramsCounter counter = model2.getTwogramsCounter();
    for (int sentiments : nSenti) {
      for (int aspects : nAspect) {
        PrintWriter out = new PrintWriter(dir + "/coherence-S" + sentiments
            + "-A" + aspects + ".csv");
        for (int k1 = 0; k1 < model1.numTopics; k1++) {
          out.print(",");
          for (int k2 = 0; k2 < model2.numTopics; k2++) {
            out.print(computeCoherenceScore(counter, sentiWords[0][k1],
                sentiWords[1][k1], sentiments, aspectWords[k2], aspects) + ",");
          }
          out.println(computeCoherenceScore(model1.getTwogramsCounter(),
              sentiWords[0][k1], sentiWords[1][k1], sentiments,
              model1.getTopAspectWords()[k1], aspects));
        }
        out.println("max,");
        out.print("corresponding (senti-k' & aspect-k'),");
        String[][][] sentiWords2 = model2.getTopSentiWords();
        for (int k2 = 0; k2 < model2.numTopics; k2++) {
          out.print(computeCoherenceScore(counter, sentiWords2[0][k2],
              sentiWords2[1][k2], sentiments, aspectWords[k2], aspects) + ",");
        }
        out.close();
      }
    }
  }

  /**
   * Computes the coherence score of the sentiment words of a topic (topic1)
   * with the aspect words of another topic (topic2).
   * 
   * @param counter
   *          the {@link TwogramsCounter} used for counting the phrases
   * @param senti0Words
   *          top positive sentiment words of <code>topic1</code>
   * @param senti1Words
   *          top negative sentiment words of <code>topic1</code>
   * @param nSenti
   *          number of sentiment words of <code>topic1</code> to use (which is
   *          same for both positive and negative sentiment)
   * @param aspectWords
   *          top aspect words of <code>topic2</code>
   * @param nAspect
   *          number of aspect words of <code>topic2</code> to use
   * @return
   */
  private static int computeCoherenceScore(TwogramsCounter counter,
      String[] senti0Words, String[] senti1Words, int nSenti,
      String[] aspectWords, int nAspect) {
    int cnt = 0;
    for (int wordIdx = 0; wordIdx < nSenti; wordIdx++) {
      for (int aspectIdx = 0; aspectIdx < nAspect; aspectIdx++) {
        cnt += counter.getCount(senti0Words[wordIdx], aspectWords[aspectIdx]);
        cnt += counter.getCount(senti1Words[wordIdx], aspectWords[aspectIdx]);
      }
    }

    return cnt;
  }

  public static void main(String args[]) throws Exception {
    // TODO(trung): this is two way (elec-rest) and (rest-elec)
    // question is whether to test the trained models on 2 corpora or to train
    // the two corpora together
    BSModel electronics = BSModel
        .loadModel("C:/datasets/bs/big/T70-A0.1-B0.0010-G1.00,0.10-I1000()--77.7/1000/model.gz");
    BSModel restaurants = BSModel
        .loadModel("C:/datasets/bs/restaurants/T70-A0.1-B0.0010-G0.50,0.10-I1000()--84/1000/model.gz");
    writeBetweenCorpusTopicCoherence("C:/datasets/bs/coherence/rest-elec",
        restaurants, electronics);
  }
}
