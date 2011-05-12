package edu.kaist.uilab.asc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import edu.kaist.uilab.asc.data.OrderedDocument;
import edu.kaist.uilab.asc.data.Sentence;
import edu.kaist.uilab.asc.data.SentiWord;
import edu.kaist.uilab.asc.prior.GraphInputProducer;
import edu.kaist.uilab.asc.prior.SimilarityGraph;
import edu.kaist.uilab.asc.util.Utils;

public class Application {
  private static final String UTF_8 = "utf-8";
  private static final String ENGLISH_PATTERN = ".*[^a-zA-Z].*";
  private static final String OTHER_PATTERN = ".*[0-9()<>,&;\"].*";
  String inputDir = "C:/datasets/asc/reviews";
  static final String wordcountFile = "WordCount1.csv";
  static final String wordlistFile = "WordList1.txt";
  static final String englishDocuments = "BagOfSentences_en1.txt";
  static final String otherDocuments = "BagOfSentences_other1.txt";
  // vacuum, coffee, camera
  final String datasetName = "ElectronicsReviews1";
  final String documentListFile = "DocumentList.txt";
  final String englishCorpus = inputDir + "/electronics_en1.txt";
  final String otherCorpus = inputDir + "/electronics_other1.txt";
  final String stopWordFile = inputDir + "/StopWords.txt";
  final String polarityFile = inputDir + "/Polarity.txt";
  final String sentiFilePrefix = "SentiWords-";
  final String englishWordList = "WordList_en.txt";
  final String dictionaryFile = "C:/datasets/asc/dict/en-fr.txt";
  final String similarityGraphFile = "graph.txt";

  int minWordLength = 3;
  int maxWordLength = 30;
  int minSentenceLength = 4;
  int maxSentenceLength = 50;
  int minWordOccur = 3;
  int minDocLength = 20; // used 20

  int numTopics = 15;
  int numSenti = 2;
  double alpha = 0.1;
  double[] gammas = new double[] { 1, 1 };
  int numIterations = 1000;
  int savingInterval = 200;
  int optimizationInterval = 100; // how often should we update y
  int burnIn = 400;
  int numThreads = 1;
  int numEnglishDocuments;

  public static void main(String[] args) throws Exception {
    Application app = new Application();
    app.parseDocuments();
    app.runModel();
  }

  /**
   * Parses all documents from the dataset.
   */
  public void parseDocuments() throws IOException {
    System.out.println("Parsing documents...");
    DocumentParser parser = new DocumentParser(minWordLength, maxWordLength,
        minSentenceLength, maxSentenceLength, minWordOccur, minDocLength);
    parser.addReplacePattern("[http|ftp]://[\\S]*", " ");
    parser.addReplacePattern("[()<>\\[\\],~&;:\"\\-/=*#]", " ");
    parser.addReplacePattern("(not|n't|without|never)[\\s]+(very|so|too|much|"
        + "quite|even|that|as|as much|a|the|to|really)[\\s]+", " not_");
    parser.addReplacePattern("(not|n't|without|never|no)[\\s]+", " not_");
    parser.addReplacePattern("(pas|ne|non|jamais|sans|n'est)[\\s]+", " pas_");
    parser.addReplacePattern(
        "(pas|ne|non|jamais|sans|n'est)[\\s]+(très|si|trop|beaucoup|"
            + "assez|si|que|ausi|vraiment)[\\s]+", " pas_");
    // TODO(trung): add negation patterns for other languages
    parser.setStopWords(stopWordFile);
    ArrayList<String> ratings = new ArrayList<String>();
    parser.setWordReplacePattern(new String[] { ENGLISH_PATTERN, null });
    int fromDocIdx = parseCorpus(true, englishCorpus, parser, ratings, 0);
    parser
        .writeWordList(inputDir + "/" + englishWordList, parser.getWordList());
    parser.setWordReplacePattern(new String[] { OTHER_PATTERN, null });
    parseCorpus(false, otherCorpus, parser, ratings, fromDocIdx);
    parser.filterWords();
    parser.writeOutFiles(inputDir);
    writePolarity(ratings);
    System.out.println(parser);
  }

  int parseCorpus(boolean isEnglishCorpus, String corpus,
      DocumentParser parser, ArrayList<String> ratings, int fromDocIdx)
      throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(corpus), UTF_8));
    while (in.readLine() != null) {
      ratings.add(in.readLine());
      parser.build(isEnglishCorpus, in.readLine(), "doc" + fromDocIdx++);
    }
    in.close();
    return fromDocIdx;
  }

  /**
   * Writes polarity of all reviews to the polarity file.
   * 
   * @param ratings
   * @throws IOException
   */
  void writePolarity(ArrayList<String> ratings) throws IOException {
    PrintWriter out = new PrintWriter(polarityFile);
    for (String rating : ratings) {
      out.println(rating);
    }
    out.close();
  }

  public void runModel() throws IOException {
    Vector<String> wordList = readWordList(inputDir + "/" + wordlistFile);
    Vector<OrderedDocument> documents = readDocuments(inputDir + "/"
        + englishDocuments);
    numEnglishDocuments = documents.size();
    documents.addAll(readDocuments(inputDir + "/" + otherDocuments));
    ArrayList<TreeSet<String>> sentiWordsStrList = new ArrayList<TreeSet<String>>();
    for (int s = 0; s < numSenti; s++) {
      sentiWordsStrList.add(Utils.makeSetOfWordsFromFile(inputDir + "/"
          + sentiFilePrefix + s + ".txt"));
    }
    ArrayList<TreeSet<Integer>> sentiWordsList = new ArrayList<TreeSet<Integer>>(
        sentiWordsStrList.size());
    for (Set<String> sentiWordsStr : sentiWordsStrList) {
      TreeSet<Integer> sentiWords = new TreeSet<Integer>();
      for (String word : sentiWordsStr) {
        sentiWords.add(wordList.indexOf(word));
      }
      sentiWordsList.add(sentiWords);
    }
    printConfiguration(documents.size(), wordList.size());
    GraphInputProducer graphProducer = new GraphInputProducer(inputDir + "/"
        + wordlistFile, dictionaryFile);
    graphProducer.write(inputDir + "/" + similarityGraphFile);
    ASC model = new ASC(numTopics, numSenti, wordList, documents,
        numEnglishDocuments, sentiWordsList, alpha, gammas,
        new SimilarityGraph(wordList.size(), inputDir + "/"
            + similarityGraphFile));
    model.setOutputDir(String.format("%s/%s-T%d-S%d-A%.2f-G%.2f,%f-I%d",
        inputDir, datasetName, numTopics, numSenti, alpha, gammas[0],
        gammas[1], numIterations));
    model.gibbsSampling(numIterations, savingInterval, burnIn,
        optimizationInterval, numThreads);
  }

  void printConfiguration(int numDocuments, int vocabSize) {
    System.out.println("Input Dir: " + inputDir);
    System.out.printf("Documents: %d (en = %d)\n", numDocuments,
        numEnglishDocuments);
    System.out.println("Unique Words: " + vocabSize);
    System.out.println("Topics: " + numTopics);
    System.out.println("Alpha: " + alpha);
    System.out.println();
    System.out.print("Gamma: ");
    for (double gamma : gammas) {
      System.out.printf("%.4f ", gamma);
    }
    System.out.println();
    System.out.println("Iterations: " + numIterations);
    System.out.println("Threads: " + numThreads);
  }

  /**
   * Reads all words from the specified file.
   * 
   * @param file
   * @return
   * @throws IOException
   */
  public static Vector<String> readWordList(String file) throws IOException {
    Vector<String> wordList = new Vector<String>();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), UTF_8));
    String line;
    while ((line = in.readLine()) != null) {
      if (line != "") {
        wordList.add(line);
      }
    }
    in.close();
    return wordList;
  }

  /**
   * Reads documents for ASC.
   * 
   * @param path
   * @return
   * @throws IOException
   */
  public static Vector<OrderedDocument> readDocuments(String path)
      throws IOException {
    Vector<OrderedDocument> documents = new Vector<OrderedDocument>();
    BufferedReader wordDocFile = new BufferedReader(new FileReader(path));
    int docCount = 0;
    String line;
    while ((line = wordDocFile.readLine()) != null) {
      OrderedDocument currentDoc = new OrderedDocument();
      currentDoc.setDocNo(docCount++);
      StringTokenizer st = new StringTokenizer(line);
      int numSentences = Integer.valueOf(st.nextToken());
      for (int s = 0; s < numSentences; s++) {
        Sentence sentence = new Sentence();
        line = wordDocFile.readLine();
        st = new StringTokenizer(line);
        while (st.hasMoreElements()) {
          int wordNo = Integer.valueOf(st.nextToken());
          sentence.addWord(new SentiWord(wordNo));
        }
        currentDoc.addSentence(sentence);
      }
      documents.add(currentDoc);
    }
    wordDocFile.close();
    return documents;
  }
}
