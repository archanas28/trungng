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

import edu.kaist.uilab.asc.util.TextFiles;

public class Runner {

  public static void main(String args[]) throws Exception {
    runNewTraining();
  }

  static void runNewTraining() throws Exception {
    String dir = "C:/datasets/bs/ursa";
    String stopFile = "stop.txt";
    String sentiStemsFile = "senti.txt";
    // String sentiStemsFile = "senti_adj.txt";
    String positiveSeeds = "seedstems0(+2).txt";
    String negativeSeeds = "seedstems1(+2).txt";
//     String positiveSeeds = "seedstems0(+1).txt";
//     String negativeSeeds = "seedstems1(+1).txt";
    String annotatedFile = "annotated.txt";
    int minTokenCount = 3;
    int topWordsToRemove = 0;
    int topDocumentTokens = 70;
    int numTopics = 7;
    int numSenti = 2;
    double alpha = 0.1;
    double betaAspect = 0.001;
    double[] betaSenti = new double[] { 0.001, 0.000000001, 0.001 };
    double[] gammas = new double[] { 0.1, 0.1 };

    int numIters = 1000;
    int burnin = 500;
    int savingInterval = 200;
    // int numIters = 10;
    // int burnin = 5;
    // int savingInterval = 5;

    List<String> stopStems = TextFiles.readLines(dir + "/" + stopFile);
    // HashSet<String> sentiStems = (HashSet<String>) TextFiles.readUniqueLines(
    // dir + "/" + sentiStemsFile, utf8);
    // BSCorpusParser parser = new BSCorpusParser(dir + "/docs.txt",
    // minTokenCount, topWordsToRemove, topDocumentTokens, sentiStems,
    // stopStems);
    CorpusParserWithTagger parser = new CorpusParserWithTagger(dir
        + "/docs.txt", minTokenCount, topWordsToRemove,
        topDocumentTokens, stopStems);
    parser.parse();
    // parser.reportCorpus(dir + "/wordcount.csv", dir + "/aspectwords.txt", dir
    // + "/sentiwords.txt");
    @SuppressWarnings("unchecked")
    HashSet<Integer>[] seedWords = new HashSet[2];
    SymbolTable sentiTable = parser.getSentiSymbolTable();
    seedWords[0] = loadSeedWords(sentiTable, dir + "/" + positiveSeeds);
    seedWords[1] = loadSeedWords(sentiTable, dir + "/" + negativeSeeds);
    Model model = new Model(numTopics, numSenti, sentiTable,
        parser.getAspectSymbolTable(), parser.getTwogramsCounter(),
        parser.getDocuments(), seedWords, alpha, betaAspect, betaSenti, gammas);
    // model
    // .setAnnotatedDocuments(getAnnotatedDocuments(dir + "/" + annotatedFile));
    model.extraInfo = "improvedParser";
    GibbsSampler sampler = new GibbsSampler(model);
    sampler.setOutputDir(String.format("%s/T%d-A%.1f-B%.4f-G%.2f,%.2f-I%d",
        dir, numTopics, alpha, betaAspect, gammas[0], gammas[1], numIters));
    sampler.gibbsSampling(numIters, 0, savingInterval, burnin);
  }

  static void runExistingTraining() throws IOException {
    Model model = Model
        .loadModel("C:/datasets/bs/small/T30-A0.1-B0.0010-G5.00,0.10-I1000(adjsenti)/1000/model.gz");
    GibbsSampler sampler = new GibbsSampler(model);
    int maxIters = 2500;
    int startingIter = 1000;
    int savingInterval = 200;
    int burnin = 500;
    sampler.gibbsSampling(maxIters, startingIter, savingInterval, burnin);
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
