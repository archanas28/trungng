package edu.kaist.uilab.bs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import com.aliasi.symbol.SymbolTable;

import edu.kaist.uilab.asc.data.ReviewReader;
import edu.kaist.uilab.asc.data.ReviewWithProsAndConsReader;
import edu.kaist.uilab.asc.util.TextFiles;
import edu.kaist.uilab.bs.opt.GibbsSamplerWithOptimizer;
import edu.kaist.uilab.bs.opt.OptimizationModel;

public class Runner {

  public static void main(String args[]) throws Exception {
     runNewTraining();
    //    runExistingTraining();
  }

  static void runNewTraining() throws Exception {
    String dir = "C:/datasets/models/bs/laptop";
    String stopFile = "stop.txt";
    // String positiveSeeds = "seedstems0(+2).txt";
    // String negativeSeeds = "seedstems1(+2).txt";
    String positiveSeeds = "C:/datasets/models/seedstems/seedstems0(+1).txt";
    String negativeSeeds = "C:/datasets/models/seedstems/seedstems1(+1).txt";
    // String annotatedFile = "annotated.txt";
    int minTokenCount = 3;
    int topWordsToRemove = 0;
    int topDocumentTokens = 60;

    List<String> stopStems = TextFiles.readLines(dir + "/" + stopFile);
    ReviewReader reader = new ReviewWithProsAndConsReader();
    CorpusParserWithTagger parser = new CorpusParserWithTagger(dir
        + "/docs.txt", reader, minTokenCount, topWordsToRemove,
        topDocumentTokens, stopStems);
    parser.parse();
    @SuppressWarnings("unchecked")
    HashSet<Integer>[] seedWords = new HashSet[2];
    SymbolTable sentiTable = parser.getSentiSymbolTable();
    seedWords[0] = loadSeedWords(sentiTable, positiveSeeds);
    seedWords[1] = loadSeedWords(sentiTable, negativeSeeds);
    // run(dir, sentiTable, parser, seedWords);
    runWithOptimizer(dir, sentiTable, parser, seedWords);
  }

  static void run(String dir, SymbolTable sentiTable,
      CorpusParserWithTagger parser, HashSet<Integer>[] seedWords)
      throws IOException {
    double alpha = 0.1;
    double betaAspect = 0.001;
    double[] betaSenti = new double[] { 0.001, 0.000000001, 0.001 };
    double[] gammas = new double[] { 0.1, 0.1 };
    int numIters = 1000;
    int burnin = 500;
    int savingInterval = 200;
    int numTopics = 7;
    int numSenti = 2;
    Model model = new Model(numTopics, numSenti, sentiTable,
        parser.getAspectSymbolTable(), parser.getTwogramsCounter(),
        parser.getDocuments(), seedWords, alpha, betaAspect, betaSenti, gammas);
    GibbsSampler sampler = new GibbsSampler(model);
    sampler.setOutputDir(String.format("%s/T%d-A%.1f-B%.4f-G%.2f,%.2f-I%d",
        dir, numTopics, alpha, betaAspect, gammas[0], gammas[1], numIters));
    sampler.gibbsSampling(numIters, 0, savingInterval, burnin);
  }

  static void runWithOptimizer(String dir, SymbolTable sentiTable,
      CorpusParserWithTagger parser, HashSet<Integer>[] seedWords)
      throws IOException {
    double alpha = 0.1;
    // TODO(trung): try 0.01 for beta
    double betaAspect = 0.01;
    double[] betaSenti = new double[] { 0.01, 0.000000001, 0.01 };
    double[] gammas = new double[] { 0.1, 0.1 };
    // int numIters = 20;
    // int burnin = 5;
    // int savingInterval = 5;
    // int optimizationInterval = 5;
    int numIters = 1000;
    int burnin = 500;
    int savingInterval = 200;
    int optimizationInterval = 100;
    int numTopics = 7;
    int numSenti = 2;
    OptimizationModel model = new OptimizationModel(numTopics, numSenti,
        sentiTable, parser.getAspectSymbolTable(), parser.getTwogramsCounter(),
        parser.getDocuments(), seedWords, alpha, betaAspect, betaSenti, gammas);
    GibbsSamplerWithOptimizer sampler = new GibbsSamplerWithOptimizer(model,
        0.001, optimizationInterval);
    sampler.setOutputDir(String.format(
        "%s/optimization2/T%d-A%.1f-B%.4f-G%.2f,%.2f-I%d", dir, numTopics,
        alpha, betaAspect, gammas[0], gammas[1], numIters));
    sampler.gibbsSampling(numIters, 0, savingInterval, burnin);
  }

  static void runExistingTraining() throws IOException {
    Model model = Model
        .loadModel("C:/datasets/models/bs/ursa/optimization/T7-A0.1-B0.0100-G0.10,0.10-I2000(noPriorSentiment)/1000/model.gz");
    model
        .writeDocumentClassificationSummary(
            model.getPi(),
            "C:/datasets/models/bs/ursa/optimization/T7-A0.1-B0.0100-G0.10,0.10-I2000(noPriorSentiment)/1000/asumstyle-docsentiment.txt");
    // AsumModelData model = (AsumModelData) BSUtils
    // .loadModel("C:/datasets/models/asum/vacuum/T7-G0.10-0.10(seed1)/1000/model.gz");
    // Vector<edu.kaist.uilab.asc.data.Document> documents = new
    // Vector<edu.kaist.uilab.asc.data.Document>();
    // Application.readDocuments(documents,
    // "C:/datasets/models/asum/coffeemaker/BagOfSentences_en.txt");
    //
    // model
    // .writeClassificationSummary(
    // "C:/datasets/models/asum/vacuum/T7-G0.10-0.10(seed1)/1000/docclassification2.txt",
    // documents);
  }

  public static HashMap<String, Document> getAnnotatedDocuments(String file)
      throws IOException {
    HashMap<String, Document> map = new HashMap<String, Document>();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), "utf-8"));
    String line;
    while ((line = in.readLine()) != null) {
      String[] part = line.split(" ");
      Document doc = new Document(-1);
      doc.setReviewId(part[0]);
      int numSentences = Integer.parseInt(part[1]);
      for (int i = 0; i < numSentences; i++) {
        line = in.readLine();
        int pos = line.indexOf(",");
        Sentence sentence = new Sentence(replacePatterns(line
            .substring(pos + 1).trim().replaceAll(" ,", ",")));
        String sentiment = line.substring(0, pos);
        if (sentiment.equalsIgnoreCase("positive")) {
          sentence.setSenti(0);
        } else if (sentiment.equalsIgnoreCase("negative")) {
          sentence.setSenti(1);
        } else {
          sentence.setSenti(-1);
        }
        doc.addSentence(sentence);
      }
      map.put(doc.getReviewId(), doc);
    }
    in.close();

    return map;
  }

  public static String replacePatterns(String sentence) {
    ArrayList<String[]> list = new ArrayList<String[]>();
    list.add(new String[] { "[http|ftp]://[\\S]*", " " });
    list.add(new String[] {
        "(not|n't|without|never)[\\s]+(very|so|too|much|"
            + "quite|even|that|as|as much|a|the|to|really|been)[\\s]+", " not_" });
    list.add(new String[] { "(not|n't|without|never|no)[\\s]+", " not_" });
    list.add(new String[] { "[()<>\\[\\],~&;:\"\\-/=*#@^+'`’]", " " });
    list.add(new String[] { "[\\s]+", " " });
    for (String[] rp : list) {
      if (sentence != null) {
        sentence = Pattern
            .compile(rp[0], Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
            .matcher(sentence).replaceAll(rp[1]);
      }
    }

    return sentence;
  }

  static HashSet<Integer> loadSeedWords(SymbolTable sentiTable, String seedFile)
      throws IOException {
    HashSet<Integer> set = new HashSet<Integer>();
    HashSet<String> words = (HashSet<String>) TextFiles
        .readUniqueLinesAsLowerCase(seedFile);
    for (String word : words) {
      int idx = sentiTable.symbolToID(word);
      if (idx >= 0) {
        set.add(idx);
        System.out.print(word + " ");
      }
    }
    System.out.println();
    return set;
  }
}
