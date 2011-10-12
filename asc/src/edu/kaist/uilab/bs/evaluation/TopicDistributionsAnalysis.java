package edu.kaist.uilab.bs.evaluation;

import java.io.IOException;
import java.io.PrintWriter;

import com.aliasi.util.ObjectToDoubleMap;

import edu.kaist.uilab.asum.AsumModel.AsumModelData;
import edu.kaist.uilab.asum.JstModel.JstModelData;
import edu.kaist.uilab.bs.Model;
import edu.kaist.uilab.bs.util.BSUtils;

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
        refTopics[senti][topic] = getMostSimilarDistribution(phi[senti][topic],
            refPhi[senti]);
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

  @SuppressWarnings("unchecked")
  public static void analyzeAsumUsingAverage(AsumModelData data,
      ReferenceDistributions reference, String outFile) throws IOException {
    ObjectToDoubleMap<String>[][] phi = data.phiIndexedByWord();
    ObjectToDoubleMap<String>[][] refPhi = reference
        .getReferenceDistributions();
    int numTopics = data.getNumTopics();
    double[][] refTopics = new double[numTopics][];
    for (int topic = 0; topic < numTopics; topic++) {
      int corrTopic = getMostSimilarDistribution(phi[0][topic], phi[1]);
      ObjectToDoubleMap<String>[] p = new ObjectToDoubleMap[] { phi[0][topic],
          phi[1][corrTopic] };
      ObjectToDoubleMap<String>[][] ref = new ObjectToDoubleMap[][] {
          refPhi[0], refPhi[1] };
      refTopics[topic] = matchTopic(p, ref);
    }
    printReferenceTopics(outFile, refTopics);
  }

  /**
   * Prints the given reference topics (and their corresponding cosine
   * similarity) to the specified output file <code>outfile</code>.
   * 
   * @param outfile
   * @param refTopics
   * @throws IOException
   */
  private static void printReferenceTopics(String outfile, double[][] refTopics)
      throws IOException {
    int numTopics = refTopics.length;
    PrintWriter out = new PrintWriter(outfile);
    out.print(",");
    for (int topic = 0; topic < numTopics; topic++) {
      out.printf("T%d,", topic);
    }
    out.println();
    out.print("ref,");
    for (int topic = 0; topic < numTopics; topic++) {
      out.printf("%s,",
          ReferenceDistributions.ASPECTS[(int) refTopics[topic][0]]);
    }
    out.println();
    out.print("cosine,");
    for (int topic = 0; topic < numTopics; topic++) {
      out.printf("%.2f,", refTopics[topic][1]);
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
        refTopics[senti][topic] = getMostSimilarDistribution(phi[senti][topic],
            refPhi[senti]);
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

  @SuppressWarnings("unchecked")
  public static void analyzeJstUsingAverage(JstModelData data,
      ReferenceDistributions reference, String outFile) throws IOException {
    ObjectToDoubleMap<String>[][] phi = data.phiIndexedByWord();
    ObjectToDoubleMap<String>[][] refPhi = reference
        .getReferenceDistributions();
    int numTopics = data.getNumTopics();
    double[][] refTopics = new double[numTopics][];
    for (int topic = 0; topic < numTopics; topic++) {
      int corrTopic = getMostSimilarDistribution(phi[0][topic], phi[1]);
      ObjectToDoubleMap<String>[] p = new ObjectToDoubleMap[] { phi[0][topic],
          phi[1][corrTopic] };
      ObjectToDoubleMap<String>[][] ref = new ObjectToDoubleMap[][] {
          refPhi[0], refPhi[1] };
      refTopics[topic] = matchTopic(p, ref);
    }
    printReferenceTopics(outFile, refTopics);
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
        refTopics[senti][topic] = getMostSimilarDistribution(phi[senti][topic],
            refPhi[senti]);
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
      refAspects[topic] = getMostSimilarDistribution(phiAspect[topic],
          refPhiAspects);
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

  @SuppressWarnings("unchecked")
  public static void analyzeBsUsingAverage(Model data,
      BSReferenceDistributions reference, String avgFile) throws IOException {
    ObjectToDoubleMap<String>[][] phi = data.getPhiSentiIndexedByWord();
    ObjectToDoubleMap<String>[][] refPhi = reference
        .getReferenceDistributions();
    ObjectToDoubleMap<String>[] phiAspect = data.getPhiAspectIndexedByWord();
    ObjectToDoubleMap<String>[] refPhiAspects = reference
        .getReferenceAspectDistributions();
    int numTopics = data.getNumTopics();
    double[][] refTopics = new double[numTopics][2];
    // compute using another metric where aspect dist is used to determine the
    // matching aspect
    for (int topic = 0; topic < numTopics; topic++) {
      int refTopic = getMostSimilarDistribution(phiAspect[topic], refPhiAspects);
      double cosineSimilarity = EvaluationUtils.cosineSimilarity(
          phiAspect[topic], refPhiAspects[refTopic]);
      cosineSimilarity += EvaluationUtils.cosineSimilarity(phi[0][topic],
          refPhi[0][refTopic]);
      cosineSimilarity += EvaluationUtils.cosineSimilarity(phi[1][topic],
          refPhi[1][refTopic]);
      refTopics[topic] = new double[] { refTopic, cosineSimilarity / 3 };
    }
    printReferenceTopics(avgFile, refTopics);
  }

  /**
   * Returns the distribution among the elements of <code>reference</code> that
   * is most similar to <code>p</code> measured by cosine similarity.
   * 
   * @return
   */
  public static int getMostSimilarDistribution(ObjectToDoubleMap<String> p,
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

  /**
   * Returns the reference topic which is most similar to the distributions in
   * <code>p</code> measured by cosine similarity.
   * 
   * @param p
   * @param reference
   * @return a two-elements array; the first element is the matching topic and
   *         the second elment is the max cosine similarity
   */
  public static double[] matchTopic(ObjectToDoubleMap<String>[] p,
      ObjectToDoubleMap<String>[][] reference) {
    double maxCosine = -1.0;
    int maxTopic = -1;
    for (int topic = 0; topic < reference.length; topic++) {
      double cosine = 0;
      for (int i = 0; i < p.length; i++) {
        cosine += EvaluationUtils.cosineSimilarity(p[i], reference[i][topic]);
      }
      if (cosine > maxCosine) {
        maxCosine = cosine;
        maxTopic = topic;
      }
    }

    return new double[] { maxTopic, maxCosine / p.length };
  }

  public static void main(String args[]) throws IOException {
    String annotatedFile = "C:/datasets/ursa/ManuallyAnnotated_Corpus.xml";
    String stopStem = "C:/datasets/models/bs/ursa/stop.txt";
    // String asumDir =
    // "C:/datasets/models/asum/ursa/T10-G0.10-0.10(seed1)/1000";
    // String jstDir = "C:/datasets/models/jst/ursa/T10-G0.10-0.10(seed1)/1000";
    String bsDir = "C:/datasets/models/bs/ursa/T6-A0.1-B0.0010-G0.10,0.10-I1000(seed1)/1000";
    // ReferenceDistributions reference = new ReferenceDistributions(
    // annotatedFile, stopStem);
    // AsumModelData asum = (AsumModelData) BSUtils.loadModel(asumDir
    // + "/model.gz");
    // analyzeAsum(asum, reference, asumDir + "/ref_asum.csv");
    // analyzeAsumUsingAverage(asum, reference, asumDir + "/refavg_asum.csv");
    // JstModelData jst = (JstModelData) BSUtils.loadModel(jstDir +
    // "/model.gz");
    // analyzeJst(jst, reference, jstDir + "/ref_jst.csv");
    // analyzeJstUsingAverage(jst, reference, jstDir + "/refavg_jst.csv");
    BSReferenceDistributions bsReference = new BSReferenceDistributions(
        annotatedFile, stopStem);
    // bsReference.writeTopWords(bsDir + "/empiricalsentiwords.csv", 50);
    // bsReference.writeTopAspectWords(bsDir + "/empiricalaspectwords.csv",
    // 100);
    Model bs = (Model) BSUtils.loadModel(bsDir + "/model.gz");
    analyzeBs(bs, bsReference, bsDir + "/ref_senti.csv", bsDir
        + "/ref_aspects.csv");
    analyzeBsUsingAverage(bs, bsReference, bsDir + "/refavg_bs.csv");
  }
}
