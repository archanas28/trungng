package edu.kaist.uilab.bs.evaluation;

import java.io.IOException;
import java.io.PrintWriter;

import com.aliasi.util.ObjectToDoubleMap;

import edu.kaist.uilab.asum.AsumModel.AsumModelData;
import edu.kaist.uilab.asum.JstModel.JstModelData;
import edu.kaist.uilab.bs.BSUtils;

/**
 * Analysis of topic distributions for different models. TODO(trung): refactor
 * code to allow sharing of model between asum and jst.
 * 
 * @author trung
 */
public class TopicDistributionsAnalysis {

  public static void analyzeAsum(AsumModelData data,
      ReferenceDistributions reference, String outFile) throws IOException {
    ObjectToDoubleMap<String>[][] phi = data.phiIndexedByWord();
    ObjectToDoubleMap<String>[][] refPhi = reference
        .getReferenceDistributions();
    int numSenti = data.getNumSenti();
    int numTopics = data.getNumTopics();
    PrintWriter out = new PrintWriter(outFile);
    out.print(",");
    for (int senti = 0; senti < data.getNumSenti(); senti++) {
      for (int topic = 0; topic < data.getNumTopics(); topic++) {
        out.printf("S%d-T%d,", senti, topic);
      }
    }
    out.println();
    out.print("ref,");
    int[][] refTopics = new int[numSenti][numTopics];
    for (int senti = 0; senti < data.getNumSenti(); senti++) {
      for (int topic = 0; topic < data.getNumTopics(); topic++) {
        refTopics[senti][topic] = matchTopic(phi[senti][topic], refPhi[senti]);
        out.printf("%s,",
            ReferenceDistributions.ASPECTS[refTopics[senti][topic]]);
      }
    }
    out.println();
    out.print("cosine,");
    for (int senti = 0; senti < data.getNumSenti(); senti++) {
      for (int topic = 0; topic < data.getNumTopics(); topic++) {
        out.printf("%.2f,", EvaluationUtils.cosineSimilarity(phi[senti][topic],
            refPhi[senti][refTopics[senti][topic]]));
      }
    }
    out.println();
    out.close();
  }

  public static void analyzeJst(JstModelData data,
      ReferenceDistributions reference, String outFile) throws IOException {
    ObjectToDoubleMap<String>[][] phi = data.phiIndexedByWord();
    ObjectToDoubleMap<String>[][] refPhi = reference
        .getReferenceDistributions();
    int numSenti = data.getNumSenti();
    int numTopics = data.getNumTopics();
    PrintWriter out = new PrintWriter(outFile);
    out.print(",");
    for (int senti = 0; senti < data.getNumSenti(); senti++) {
      for (int topic = 0; topic < data.getNumTopics(); topic++) {
        out.printf("S%d-T%d,", senti, topic);
      }
    }
    out.println();
    out.print("ref,");
    int[][] refTopics = new int[numSenti][numTopics];
    for (int senti = 0; senti < data.getNumSenti(); senti++) {
      for (int topic = 0; topic < data.getNumTopics(); topic++) {
        refTopics[senti][topic] = matchTopic(phi[senti][topic], refPhi[senti]);
        out.printf("%s,",
            ReferenceDistributions.ASPECTS[refTopics[senti][topic]]);
      }
    }
    out.println();
    out.print("cosine,");
    for (int senti = 0; senti < data.getNumSenti(); senti++) {
      for (int topic = 0; topic < data.getNumTopics(); topic++) {
        out.printf("%.2f,", EvaluationUtils.cosineSimilarity(phi[senti][topic],
            refPhi[senti][refTopics[senti][topic]]));
      }
    }
    out.println();
    out.close();
  }

  /**
   * Returns the reference topic which is most similar to the distribution
   * <code>p</code> measured by cosine similarity.
   * 
   * @return
   */
  public static int matchTopic(ObjectToDoubleMap<String> p,
      ObjectToDoubleMap<String>[] reference) {
    double maxCosine = -1.0;
    int maxTopic = -1;
    for (int topic = 0; topic < reference.length; topic++) {
      double cosine = EvaluationUtils.cosineSimilarity(p, reference[topic]);
      if (cosine > maxCosine) {
        maxCosine = cosine;
        maxTopic = topic;
      }
    }

    return maxTopic;
  }

  public static void main(String args[]) throws IOException {
    String annotatedFile = "C:/datasets/ursa/ManuallyAnnotated_Corpus.xml";
    String stopStem = "C:/datasets/models/bs/ursa/stop.txt";
    String asumDir = "C:/datasets/models/asum/ursa/T7G0.10-0.10(seed1)/1000";
    String jstDir = "C:/datasets/models/jst/ursa/T7-G0.10-0.10(seed1)/1000";
    ReferenceDistributions reference = new ReferenceDistributions(
        annotatedFile, stopStem);
    AsumModelData asum = (AsumModelData) BSUtils.loadModel(asumDir
        + "/model.gz");
    analyzeAsum(asum, reference, asumDir + "/reference.csv");
    JstModelData jst = (JstModelData) BSUtils.loadModel(jstDir + "/model.gz");
    analyzeJst(jst, reference, jstDir + "/reference.csv");
  }
}
