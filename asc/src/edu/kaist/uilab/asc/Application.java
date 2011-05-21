package edu.kaist.uilab.asc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import edu.kaist.uilab.asc.data.OrderedDocument;
import edu.kaist.uilab.asc.data.Sentence;
import edu.kaist.uilab.asc.data.SentiWord;
import edu.kaist.uilab.asc.prior.GraphInputProducer;
import edu.kaist.uilab.asc.prior.SimilarityGraph;
import edu.kaist.uilab.asc.stemmer.EnglishStemmer;
import edu.kaist.uilab.asc.stemmer.FrenchStemmer;
import edu.kaist.uilab.asc.util.Utils;

/**
 * Main application.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Application {
  private static final String UTF8 = "utf-8";
  private static final String EN_PATTERN = ".*[^a-zA-Z].*";
  private static final String FR_PATTERN = ".*[0-9()<>,&;\"].*";
  String inputDir = "C:/datasets/asc/reviews/ElectronicsReviews2";
  // String inputDir = "C:/datasets/asc/reviews/MovieReviews";
  static final String wordcountFile = "WordCount.csv";
  static final String wordlistFile = "WordList.txt";
  static final String enDocuments = "BagOfSentences_en.txt";
  static final String otherDocuments = "BagOfSentences_other.txt";
  // String datasetName = "MovieReviews-Stemmed-InitY0";
  // vacuum, coffee, camera
  // String datasetName = "ElectronicsReviews1-Stemmed-InitY0";
  String datasetName = "ElectronicsReviews2-Stemmed-InitOld";
  final String documentListFile = "DocumentList.txt";
  final String englishCorpus = inputDir + "/docs_en.txt";
  final String frCorpus = inputDir + "/docs_other.txt";
  final String stopWordFile = inputDir + "/StopWords.txt";
  final String enStopWordFile = inputDir + "/StopStems_en.txt";
  final String frStopWordFile = inputDir + "/StopStems_fr.txt";
  final String polarityFile = inputDir + "/Polarity.txt";
  // final String sentiFilePrefix = "SentiWords-";
  final String sentiFilePrefix = "SentiStems-";
  final String enWordList = "WordList_en.txt";
  final String dictionaryFile = "C:/datasets/asc/dict/en-fr.txt";
  final String similarityGraphFile = "graph.txt";

  int minWordLength = 3;
  int maxWordLength = 30;
  int minSentenceLength = 4;
  int maxSentenceLength = 50;
  int minWordOccur = 3;
  int minDocLength = 20; // used 20

  int numTopics = 20;
  int numSenti = 2;
  double alpha = 0.1;
  double[] gammas = new double[] { 1, 1 };
  int numIterations = 2000;
  int savingInterval = 200;
  int optimizationInterval = 100;
  int burnIn = 500;
  int numThreads = 1;
  int numEnglishDocuments;

  public static void main(String[] args) throws Exception {
    Application app = new Application();
    app.parseDocuments();
    app.runModel(3);
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
    ArrayList<String> ratings = new ArrayList<String>();

    // English corpus
    parser.setLocale(Locale.ENGLISH);
    parser.setWordReplacePattern(new String[] { EN_PATTERN, null });
    parser.setStopWords(enStopWordFile);
    parser.setStemmer(new EnglishStemmer());
    int fromDocIdx = parseCorpus(englishCorpus, parser, ratings, 0);
    parser.writeWordList(inputDir + "/" + enWordList, parser.getWordList());
    parser.writeAndClearsStemMap(inputDir + "/Stem_en.txt");

    // French corpus
    parser.setLocale(Locale.FRENCH);
    parser.setWordReplacePattern(new String[] { FR_PATTERN, null });
    parser.setStopWords(frStopWordFile);
    parser.setStemmer(new FrenchStemmer());
    parseCorpus(frCorpus, parser, ratings, fromDocIdx);
    parser.writeAndClearsStemMap(inputDir + "/Stem_fr.txt");

    parser.filterWords();
    parser.writeOutFiles(inputDir);
    writePolarity(ratings);
    System.out.println(parser);
  }

  int parseCorpus(String corpus, DocumentParser parser,
      ArrayList<String> ratings, int fromDocIdx) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(corpus), UTF8));
    while (in.readLine() != null) {
      ratings.add(in.readLine());
      parser.build(in.readLine(), "doc" + fromDocIdx++);
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

  public void runModel(int version) throws IOException {
    Vector<String> wordList = readWordList(inputDir + "/" + wordlistFile);
    Vector<OrderedDocument> documents = readDocuments(inputDir + "/"
        + enDocuments);
    numEnglishDocuments = documents.size();
    documents.addAll(readDocuments(inputDir + "/" + otherDocuments));
    ArrayList<TreeSet<String>> list = new ArrayList<TreeSet<String>>(numSenti);
    for (int s = 0; s < numSenti; s++) {
      list.add(Utils.readWords(
          inputDir + "/" + sentiFilePrefix + s + "_en.txt", UTF8));
      list.get(s).addAll(
          Utils.readWords(inputDir + "/" + sentiFilePrefix + s + "_fr.txt",
              UTF8));
    }
    ArrayList<TreeSet<Integer>> sentiClasses = new ArrayList<TreeSet<Integer>>(
        list.size());
    for (Set<String> sentiWordsStr : list) {
      TreeSet<Integer> sentiClass = new TreeSet<Integer>();
      for (String word : sentiWordsStr) {
        sentiClass.add(wordList.indexOf(word));
      }
      sentiClasses.add(sentiClass);
    }
    printConfiguration(documents.size(), wordList.size());
    GraphInputProducer graphProducer = new GraphInputProducer(inputDir + "/"
        + wordlistFile, dictionaryFile);
    graphProducer.write(inputDir + "/" + similarityGraphFile);
    AbstractAsc model = null;
    switch (version) {
    case 1:
      model = new Asc1(numTopics, numSenti, wordList, documents,
          numEnglishDocuments, sentiClasses, alpha, gammas,
          new SimilarityGraph(wordList.size(), inputDir + "/"
              + similarityGraphFile));
      break;
    case 2:
      model = new Asc2(numTopics, numSenti, wordList, documents,
          numEnglishDocuments, sentiClasses, alpha, gammas,
          new SimilarityGraph(wordList.size(), inputDir + "/"
              + similarityGraphFile));
      break;
    case 3:
      model = new Asc3(numTopics, numSenti, wordList, documents,
          numEnglishDocuments, sentiClasses, alpha, gammas,
          new SimilarityGraph(wordList.size(), inputDir + "/"
              + similarityGraphFile));
      break;
    default:
      break;
    }
    model.setOutputDir(String.format("%s/%s(V%d)-T%d-S%d-A%.2f-I%d", inputDir,
        datasetName, version, numTopics, numSenti, alpha, numIterations));
    model.gibbsSampling(numIterations, savingInterval, burnIn,
        optimizationInterval, numThreads);
  }

  void printConfiguration(int numDocuments, int vocabSize) {
    System.out.println("Dataset name: " + datasetName);
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
        new FileInputStream(file), UTF8));
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
