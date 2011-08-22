package edu.kaist.uilab.bs.evaluation;

import java.io.IOException;
import java.io.PrintWriter;

import com.aliasi.symbol.SymbolTable;

import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.bs.Model;
import edu.kaist.uilab.bs.TwogramsCounter;

/**
 * Analyzes using the coherence score.
 * 
 * @author trung
 */
public class CoherenceAnalysis {
  Model sentiModel;
  Model aspectModel;
  SymbolTable sentiTable;
  int[][][] sentiIndice;
  SymbolTable aspectTable;
  int[][] aspectIndice;
  DoubleMatrix[] phiSenti;
  double[][] phiAspect;

  public CoherenceAnalysis(Model sentiModel, Model aspectModel) {
    this.sentiModel = sentiModel;
    this.aspectModel = aspectModel;
    phiSenti = sentiModel.getPhiSentiByTermscore();
    phiAspect = aspectModel.getPhiAspectByTermscore();
    sentiTable = sentiModel.getSentiTable();
    sentiIndice = sentiModel.getIndiceOfTopSentiWords(phiSenti,
        sentiModel.getNumProbWords());
    aspectTable = aspectModel.getAspectTable();
    aspectIndice = aspectModel.getIndiceOfTopAspectWords(phiAspect,
        aspectModel.getNumProbWords());
  }

  /**
   * Reports the coherence scores between topics from a same corpus.
   * 
   * @param file
   * @param aspectWords
   * @param sentiWords
   */
  public static void writeWithinCorpusTopicCoherence(String dir, Model model)
      throws IOException {
    int nSenti[] = new int[] { 25, 50 };
    int nAspect[] = new int[] { 25, 50 };
    String[][][] sentiWords = model.getTopSentiWords(model.getNumProbWords());
    String[][] aspectWords = model.getTopAspectWords(model.getNumProbWords());
    TwogramsCounter counter = model.getTwogramsCounter();
    for (int sentiments : nSenti) {
      for (int aspects : nAspect) {
        PrintWriter out = new PrintWriter(dir + "/coherence-S" + sentiments
            + "-A" + aspects + ".csv");
        for (int k1 = 0; k1 < model.getNumTopics(); k1++) {
          for (int k2 = 0; k2 < model.getNumTopics(); k2++) {
            out.print(computeCoherenceScore(counter, sentiWords[0][k1],
                sentiWords[1][k1], sentiments, aspectWords[k2], aspects) + ",");
          }
          out.println();
        }
        out.close();
      }
    }
  }

  // for the aspect model
  public void writeWithinCorpusTopicCoherence(String dir) throws IOException {
    int nSenti = 25;
    int nAspect = 50;
    TwogramsCounter counter = aspectModel.getTwogramsCounter();
    DoubleMatrix[] phiSenti2 = aspectModel.getPhiSentiByTermscore();
    SymbolTable sentiTable2 = aspectModel.getSentiTable();
    int[][][] sentiIndice2 = aspectModel.getIndiceOfTopSentiWords(phiSenti2,
        aspectModel.getNumProbWords());
    PrintWriter out = new PrintWriter(dir + "/prob-coherence-S" + nSenti + "-A"
        + nAspect + ".csv");
    for (int k1 = 0; k1 < aspectModel.getNumTopics(); k1++) {
      for (int k2 = 0; k2 < aspectModel.getNumTopics(); k2++) {
        out.printf(
            "%.3f,",
            computeCoherenceScoreWithProb(counter, sentiTable2, phiSenti2,
                sentiIndice2, k1, nSenti, k2, nAspect));
      }
      out.println();
    }
    out.close();
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

  private void writeBetweenCorpusTopicCoherence(String dir) throws IOException {
    // TODO(trung): should only count the frequency of phrase in the corpus for
    // aspect because we want to evaluate that specific domain (i.e., HOW
    // GOOD/RELATED the sentiment of a corpus compared to other corpus). in that
    // case, it should be compared with the set of sentiment words for the
    // aspect corpus.
    // if (senti-aspect) pair is (elec-rest) then compares it with (sent-aspect)
    // for (rest-rest). if same amount of data then does not really matter
    int nSenti[] = new int[] { 25, 50 };
    int nAspect[] = new int[] { 25, 50 };
    String[][][] sentiWords = sentiModel
        .getTopSentiWords(sentiModel.getNumProbWords());
    String[][] aspectWords = aspectModel
        .getTopAspectWords(aspectModel.getNumProbWords());
    TwogramsCounter counter = aspectModel.getTwogramsCounter();
    for (int sentiments : nSenti) {
      for (int aspects : nAspect) {
        PrintWriter out = new PrintWriter(dir + "/coherence-S" + sentiments
            + "-A" + aspects + ".csv");
        for (int k1 = 0; k1 < sentiModel.getNumTopics(); k1++) {
          out.print(",");
          for (int k2 = 0; k2 < aspectModel.getNumTopics(); k2++) {
            out.print(computeCoherenceScore(counter, sentiWords[0][k1],
                sentiWords[1][k1], sentiments, aspectWords[k2], aspects) + ",");
          }
          out.println(computeCoherenceScore(sentiModel.getTwogramsCounter(),
              sentiWords[0][k1], sentiWords[1][k1], sentiments,
              sentiModel.getTopAspectWords(sentiModel.getNumProbWords())[k1],
              aspects));
        }
        out.println("max,");
        out.print("self (senti-k' & aspect-k'),");
        String[][][] sentiWords2 = aspectModel
            .getTopSentiWords(aspectModel.getNumProbWords());
        for (int k2 = 0; k2 < aspectModel.getNumTopics(); k2++) {
          out.print(computeCoherenceScore(counter, sentiWords2[0][k2],
              sentiWords2[1][k2], sentiments, aspectWords[k2], aspects) + ",");
        }
        out.close();
      }
    }
  }

  private void writeBetweenCorporaTopicCoherence(String dir) throws IOException {
    int nSenti = 25;
    int nAspect = 50;
    TwogramsCounter counter = aspectModel.getTwogramsCounter();
    DoubleMatrix[] phiSenti2 = aspectModel.getPhiSentiByTermscore();
    SymbolTable sentiTable2 = aspectModel.getSentiTable();
    int[][][] sentiIndice2 = aspectModel.getIndiceOfTopSentiWords(phiSenti2,
        aspectModel.getNumProbWords());
    PrintWriter out = new PrintWriter(dir + "/prob-coherence-S" + nSenti + "-A"
        + nAspect + ".csv");
    for (int k1 = 0; k1 < sentiModel.getNumTopics(); k1++) {
      out.print(",");
      for (int k2 = 0; k2 < aspectModel.getNumTopics(); k2++) {
        out.printf("%.3f,",
            computeCoherenceScoreWithProb(counter, k1, nSenti, k2, nAspect));
      }
      out.println();
    }
    out.println("max,");
    out.print("self(senti-k' & aspect-k'),");
    for (int k2 = 0; k2 < aspectModel.getNumTopics(); k2++) {
      out.printf(
          "%.3f,",
          computeCoherenceScoreWithProb(counter, sentiTable2, phiSenti2,
              sentiIndice2, k2, nSenti, k2, nAspect));
    }
    out.close();
  }

  // for topics from the same corpus
  private double computeCoherenceScoreWithProb(TwogramsCounter counter,
      SymbolTable sentiTable2, DoubleMatrix[] phiSenti2,
      int[][][] sentiIndice2, int kSenti, int nSenti, int kAspect, int nAspect) {
    double score = 0.0;
    int sentiWordIdx, aspectWordIdx;
    double probSentiWord, probAspectWord;
    for (int i = 0; i < nSenti; i++) {
      for (int j = 0; j < nAspect; j++) {
        for (int senti = 0; senti < 2; senti++) {
          sentiWordIdx = sentiIndice2[senti][kSenti][i];
          aspectWordIdx = aspectIndice[kAspect][j];
          probSentiWord = (phiSenti2[senti].getValue(sentiWordIdx, kSenti) / phiSenti2[senti]
              .getValue(sentiIndice2[senti][kSenti][0], kSenti));
          probAspectWord = (phiAspect[kAspect][aspectWordIdx] / phiAspect[kAspect][aspectIndice[kAspect][0]]);
          score += counter.getCount(sentiTable2.idToSymbol(sentiWordIdx),
              aspectTable.idToSymbol(aspectWordIdx))
              * probSentiWord
              * probAspectWord;
        }
      }
    }

    return score;
  }

  // for topics from 2 corpora
  private double computeCoherenceScoreWithProb(TwogramsCounter counter,
      int kSenti, int nSenti, int kAspect, int nAspect) {
    double score = 0.0;
    int sentiWordIdx, aspectWordIdx;
    double probSentiWord, probAspectWord;
    for (int i = 0; i < nSenti; i++) {
      for (int j = 0; j < nAspect; j++) {
        for (int senti = 0; senti < 2; senti++) {
          sentiWordIdx = sentiIndice[senti][kSenti][i];
          aspectWordIdx = aspectIndice[kAspect][j];
          probSentiWord = (phiSenti[senti].getValue(sentiWordIdx, kSenti) / phiSenti[senti]
              .getValue(sentiIndice[senti][kSenti][0], kSenti));
          probAspectWord = (phiAspect[kAspect][aspectWordIdx] / phiAspect[kAspect][aspectIndice[kAspect][0]]);
          score += counter.getCount(sentiTable.idToSymbol(sentiWordIdx),
              aspectTable.idToSymbol(aspectWordIdx))
              * probSentiWord
              * probAspectWord;
        }
      }
    }

    return score;
  }

  public static void dealingWithCamera(String dir) throws IOException {
    Model small = Model.loadModel(dir + "/model.gz");
    CoherenceAnalysis c = new CoherenceAnalysis(small, small);
    c.writeWithinCorpusTopicCoherence(dir);
  }

  public static void main(String args[]) throws Exception {
    // dealingWithCamera("C:/datasets/bs/camera/T7-A0.1-B0.0010-G0.10,0.10-I1000(newstop)/1000");
    // dealingWithCamera("C:/datasets/bs/camera/T10-A0.1-B0.0010-G0.10,0.10-I1000(newstop)/1000");
    // dealingWithCamera("C:/datasets/bs/camera/T15-A0.1-B0.0010-G0.10,0.10-I1000(newstop)/1000");
    dealingWithCamera("C:/datasets/bs/small/(newstop)T10-A0.1-B0.0010-G0.10,0.10-I1000()/1000");
    dealingWithCamera("C:/datasets/bs/small/(newstop)T15-A0.1-B0.0010-G0.10,0.10-I1000()/1000");
    dealingWithCamera("C:/datasets/bs/small/(newstop)T20-A0.1-B0.0010-G0.10,0.10-I1000()/1000");
    // BSModel electronics = BSModel
    // .loadModel("C:/datasets/bs/big/T30-A0.1-B0.0010-G0.10,0.10-I1000(newstop)--79/1000/model.gz");
    // BSModel ursa = BSModel
    // .loadModel("C:/datasets/bs/ursa/T30-A0.1-B0.0010-G0.10,0.10-I1000(newstop)/1000/model.gz");
    // CoherenceAnalysis c = new CoherenceAnalysis(ursa, electronics);
    // c.writeBetweenCorporaTopicCoherence("C:/datasets/bs/coherence/(newstop)30ursa-elec");
    // c = new CoherenceAnalysis(electronics, ursa);
    // c.writeBetweenCorporaTopicCoherence("C:/datasets/bs/coherence/(newstop)30elec-ursa");
  }
}
