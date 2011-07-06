package edu.kaist.uilab.asc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import edu.kaist.uilab.asc.data.Document;

/**
 * A base Asc implementation for all models that used y_{ki}, y_{ji}, y_{i} as
 * dependent variables for beta. Here j, k, i denotes a sentiment, topic, and
 * word respectively.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public abstract class BaseAscModel extends AbstractAscModel {

  private static final long serialVersionUID = 1L;
  double[][] yTopic; // y[k][i] = y[topic][word]
  double[][] ySentiment; // y[j][i] = y[sentiment][word]
  double[] yWord; // y[word]

  @Override
  void initVariables() {
    yTopic = new double[numTopics][vocabSize];
    ySentiment = new double[numSenti][vocabSize];
    yWord = new double[vocabSize];
    vars = new double[numTopics * effectiveVocabSize + numSenti
        * effectiveVocabSize + effectiveVocabSize];
  }

  /**
   * Creates a new base asc model.
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
  public BaseAscModel(int numTopics, int numSenti, Vector<LocaleWord> wordList,
      List<Document> documents, int numEnglishDocuments,
      List<TreeSet<Integer>> sentiWordsList, double alpha, double[] gammas,
      String graphFile) {
    super(numTopics, numSenti, wordList, documents, numEnglishDocuments,
        sentiWordsList, alpha, gammas, graphFile);
    initPrior();
    // initY0();
  }

  /**
   * Writes out some values of y.
   * 
   * @param dir
   * @throws IOException
   */
  void writeSampleY(String dir) throws IOException {
    PrintWriter out = new PrintWriter(dir + "/y.txt");
    for (int i = 0; i < 100; i++) {
      int w = (int) (Math.random() * vocabSize);
      out.printf("\nWord no: %d", w);
      out.printf("\nyWord[w]: \t%.5f", yWord[w]);
      out.printf("\nyTopic[t][w]:");
      for (int t = 0; t < numTopics; t++) {
        out.printf("\t%.5f", yTopic[t][w]);
      }
    }
    out.close();
    // write word sentiment
    out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dir
        + "/wordSenti.csv"), "utf-8"));
    for (int i = 0; i < vocabSize; i++) {
      out.printf("%s,%.4f,%.4f\n", wordList.get(i), ySentiment[0][i],
          ySentiment[1][i]);
    }
    out.close();
  }

  // init with sentiment prior
  void initPrior() {
    extraInfo = "initPrior(tw=0.5,senti=-10,0,0)";
    int idx = 0;
    double commonTopicWord = 0.5;
    for (int t = 0; t < numTopics; t++) {
      for (int w = 0; w < vocabSize; w++) {
        yTopic[t][w] = commonTopicWord;
        if (w < effectiveVocabSize) {
          vars[idx++] = yTopic[t][w];
        }
      }
    }
    for (int s = 0; s < numSenti; s++) {
      for (int w = 0; w < vocabSize; w++) {
        if (sentiWordsList.get(1 - s).contains(w)) {
          ySentiment[s][w] = -10;
        } else if (sentiWordsList.get(s).contains(w)) {
          ySentiment[s][w] = 0.5;
        } else {
          ySentiment[s][w] = 0.0;
        }
        if (w < effectiveVocabSize) {
          vars[idx++] = ySentiment[s][w];
        }
      }
    }
    for (int w = 0; w < vocabSize; w++) {
      yWord[w] = 0;
      if (w < effectiveVocabSize) {
        vars[idx++] = yWord[w];
      }
    }
    updateBeta();
  }

  // init all variables to 0
  void initY0() {
    extraInfo = "initY0";
    for (int t = 0; t < numTopics; t++) {
      for (int w = 0; w < vocabSize; w++) {
        yTopic[t][w] = 0;
      }
    }
    for (int w = 0; w < vocabSize; w++) {
      yWord[w] = 0;
    }
    for (int s = 0; s < numSenti; s++) {
      for (int w = 0; w < vocabSize; w++) {
        ySentiment[s][w] = 0;
      }
    }
    updateBeta();
  }

  /**
   * Converts the variables (used in optimization function) to y.
   */
  @Override
  void variablesToY() {
    int idx = 0;
    for (int t = 0; t < numTopics; t++) {
      for (int w = 0; w < effectiveVocabSize; w++) {
        yTopic[t][w] = vars[idx++];
      }
    }
    for (int s = 0; s < numSenti; s++) {
      for (int w = 0; w < effectiveVocabSize; w++) {
        ySentiment[s][w] = vars[idx++];
      }
    }
    for (int w = 0; w < effectiveVocabSize; w++) {
      yWord[w] = vars[idx++];
    }
  }

  @Override
  void extendVars() {
    double[] newVars = new double[numTopics * effectiveVocabSize + numSenti
        * effectiveVocabSize + effectiveVocabSize];
    int idx = 0;
    for (int k = 0; k < numTopics; k++) {
      for (int i = 0; i < effectiveVocabSize; i++) {
        newVars[idx++] = yTopic[k][i];
      }
    }
    for (int s = 0; s < numSenti; s++) {
      for (int w = 0; w < effectiveVocabSize; w++) {
        newVars[idx++] = ySentiment[s][w];
      }
    }
    for (int w = 0; w < effectiveVocabSize; w++) {
      newVars[idx++] = yWord[w];
    }
    vars = newVars;
  }
}
