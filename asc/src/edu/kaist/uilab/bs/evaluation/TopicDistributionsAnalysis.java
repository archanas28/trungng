package edu.kaist.uilab.bs.evaluation;

import java.io.IOException;
import java.io.PrintWriter;

import com.aliasi.util.ObjectToDoubleMap;

import edu.kaist.uilab.asum.AsumModel.AsumModelData;
import edu.kaist.uilab.asum.JstModel.JstModelData;
import edu.kaist.uilab.bs.BSUtils;
import edu.kaist.uilab.bs.Model;

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
    for (int senti = 0; senti < numSenti; senti++) {
      for (int topic = 0; topic < numTopics; topic++) {
        out.printf("S%d-T%d,", senti, topic);
      }
    }
    out.println();
    out.print("ref,");
    int[][] refTopics = new int[numSenti][numTopics];
    for (int senti = 0; senti < numSenti; senti++) {
      for (int topic = 0; topic < numTopics; topic++) {
        refTopics[senti][topic] = matchTopic(phi[senti][topic], refPhi[senti]);
        out.printf("%s,",
            ReferenceDistributions.ASPECTS[refTopics[senti][topic]]);
      }
    }
    out.println();
    out.print("cosine,");
    for (int senti = 0; senti < numSenti; senti++) {
      for (int topic = 0; topic < numTopics; topic++) {
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
    for (int senti = 0; senti < numSenti; senti++) {
      for (int topic = 0; topic < numTopics; topic++) {
        out.printf("S%d-T%d,", senti, topic);
      }
    }
    out.println();
    out.print("ref,");
    int[][] refTopics = new int[numSenti][numTopics];
    for (int senti = 0; senti < numSenti; senti++) {
      for (int topic = 0; topic < numTopics; topic++) {
        refTopics[senti][topic] = matchTopic(phi[senti][topic], refPhi[senti]);
        out.printf("%s,",
            ReferenceDistributions.ASPECTS[refTopics[senti][topic]]);
      }
    }
    out.println();
    out.print("cosine,");
    for (int senti = 0; senti < numSenti; senti++) {
      for (int topic = 0; topic < numTopics; topic++) {
        out.printf("%.2f,", EvaluationUtils.cosineSimilarity(phi[senti][topic],
            refPhi[senti][refTopics[senti][topic]]));
      }
    }
    out.println();
    out.close();
  }

  public static void analyzeBs(Model data, BSReferenceDistributions reference,
      String sentiFile, String aspectFile) throws IOException {
    ObjectToDoubleMap<String>[][] phi = data.getPhiSentiIndexedByWord();
    ObjectToDoubleMap<String>[][] refPhi = reference
        .getReferenceDistributions();
    int numSenti = data.getNumSentiments();
    int numTopics = data.getNumTopics();
    PrintWriter out = new PrintWriter(sentiFile);
    out.print(",");
    for (int senti = 0; senti < numSenti; senti++) {
      for (int topic = 0; topic < numTopics; topic++) {
        out.printf("S%d-T%d,", senti, topic);
      }
    }
    out.println();
    out.print("ref,");
    int[][] refTopics = new int[numSenti][numTopics];
    for (int senti = 0; senti < numSenti; senti++) {
      for (int topic = 0; topic < numTopics; topic++) {
        refTopics[senti][topic] = matchTopic(phi[senti][topic], refPhi[senti]);
        out.printf("%s,",
            ReferenceDistributions.ASPECTS[refTopics[senti][topic]]);
      }
    }
    out.println();
    out.print("cosine,");
    for (int senti = 0; senti < numSenti; senti++) {
      for (int topic = 0; topic < numTopics; topic++) {
        out.printf("%.2f,", EvaluationUtils.cosineSimilarity(phi[senti][topic],
            refPhi[senti][refTopics[senti][topic]]));
      }
    }
    out.println();
    out.close();

    // print comparison for aspects
    ObjectToDoubleMap<String>[] phiAspect = data.getPhiAspectIndexedByWord();
    ObjectToDoubleMap<String>[] refPhiAspects = reference
        .getReferenceAspectDistributions();
    out = new PrintWriter(aspectFile);
    out.print(",");
    for (int topic = 0; topic < numTopics; topic++) {
      out.printf("T%d,", topic);
    }
    out.println();
    out.print("ref,");
    int[] refAspects = new int[numTopics];
    for (int topic = 0; topic < numTopics; topic++) {
      refAspects[topic] = matchTopic(phiAspect[topic], refPhiAspects);
      out.printf("%s,", ReferenceDistributions.ASPECTS[refAspects[topic]]);
    }
    out.println();
    out.print("cosine,");
    for (int topic = 0; topic < numTopics; topic++) {
      out.printf("%.2f,", EvaluationUtils.cosineSimilarity(phiAspect[topic],
          refPhiAspects[refAspects[topic]]));
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
    String annotatedFile = "C:/datasets/ManuallyAnnotated_Corpus.xml";
    String stopStem = "C:/datasets/stop.txt";
    // String asumDir = "C:/datasets";
    // String jstDir = "C:/datasets";
    String bsDir = "C:/datasets";
    // ReferenceDistributions reference = new ReferenceDistributions(
    // annotatedFile, stopStem);
    // AsumModelData asum = (AsumModelData) BSUtils
    // .loadModel(asumDir + "/asum.gz");
    // analyzeAsum(asum, reference, asumDir + "/asum.csv");
    // JstModelData jst = (JstModelData) BSUtils.loadModel(jstDir + "/jst.gz");
    // analyzeJst(jst, reference, jstDir + "/jst.csv");
    BSReferenceDistributions reference = new BSReferenceDistributions(
        annotatedFile, stopStem);
//    reference.writeTopWords(bsDir + "/bssentiwords.csv", 50);
//    reference.writeTopAspectWords(bsDir + "/bsaspectwords.csv", 100);
     Model bs = (Model) BSUtils.loadModel(bsDir + "/bs.gz");
    analyzeBs(bs, reference, bsDir + "/bssenti.csv", bsDir + "/bsaspect.csv");
  }

  // public static void main(String args[]) throws IOException {
  // String annotatedFile = "C:/datasets/ursa/ManuallyAnnotated_Corpus.xml";
  // String stopStem = "C:/datasets/models/bs/ursa/stop.txt";
  // String asumDir = "C:/datasets/models/asum/ursa/T7G0.10-0.10(seed1)/1000";
  // String jstDir = "C:/datasets/models/jst/ursa/T7-G0.10-0.10(seed1)/1000";
  // ReferenceDistributions reference = new ReferenceDistributions(
  // annotatedFile, stopStem);
  // AsumModelData asum = (AsumModelData) BSUtils.loadModel(asumDir
  // + "/model.gz");
  // analyzeAsum(asum, reference, asumDir + "/reference.csv");
  // JstModelData jst = (JstModelData) BSUtils.loadModel(jstDir + "/model.gz");
  // analyzeJst(jst, reference, jstDir + "/reference.csv");
  // }

}
