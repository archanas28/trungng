package edu.kaist.uilab.bs;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import com.aliasi.symbol.SymbolTable;

import edu.kaist.uilab.asc.util.TextFiles;

public class BSRunner {

  public static void main(String args[]) throws IOException {
    String utf8 = "utf-8";
    String dir = "C:/datasets/bs/big";
    String stopFile = "stop_en.txt";
    String sentiStemsFile = "senti.txt";
    String positiveSeeds = "seedstems0.txt";
    String negativeSeeds = "seedstems1.txt";

    int minTokenCount = 4;
    int topWordsToRemove = 0;
    int topDocumentTokens = 40;
    int numTopics = 70;
    int numSenti = 2;
    double alpha = 0.1;
//    double alpha = 50.0 / numTopics;
    double betaAspect = 0.001;
    double[] betaSenti = new double[] { 0.001, 0.000000001, 0.001 };
    double[] gammas = new double[] { 1.0, 1.0 };

    int numIters = 1000;
    int burnin = 500;
    int savingInterval = 200;

    List<String> stopStems = TextFiles.readLines(dir + "/" + stopFile, utf8);
    HashSet<String> sentiStems = (HashSet<String>) TextFiles.readUniqueLines(
        dir + "/" + sentiStemsFile, utf8);
    BSCorpusParser parser = new BSCorpusParser(dir + "/docs.txt",
        minTokenCount, topWordsToRemove, topDocumentTokens, sentiStems, stopStems);
    parser.parse();
    parser.reportCorpus(dir + "/wordcount.csv", dir + "/aspectwords.txt", dir
        + "/sentiwords.txt");
    @SuppressWarnings("unchecked")
    HashSet<Integer>[] seedWords = new HashSet[2];
    SymbolTable sentiTable = parser.getSentiSymbolTable();
    seedWords[0] = loadSeedWords(sentiTable, dir + "/" + positiveSeeds);
    seedWords[1] = loadSeedWords(sentiTable, dir + "/" + negativeSeeds);
    BSModel model = new BSModel(numTopics, numSenti, sentiTable,
        parser.getAspectSymbolTable(), parser.getDocuments(), seedWords, alpha,
        betaAspect, betaSenti, gammas);
//    model.extraInfo = "moreSeedWords";
    BSGibbsSampler sampler = new BSGibbsSampler(model);
    sampler.setOutputDir(String.format("%s/T%d-A%.1f-G%.2f,%.2f-I%d", dir,
        numTopics, alpha, gammas[0], gammas[1], numIters));
    sampler.gibbsSampling(numIters, savingInterval, burnin);
  }

  static HashSet<Integer> loadSeedWords(SymbolTable sentiTable, String seedFile)
      throws IOException {
    HashSet<Integer> set = new HashSet<Integer>();
    HashSet<String> words = (HashSet<String>) TextFiles.readUniqueLines(
        seedFile, "utf-8");
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
