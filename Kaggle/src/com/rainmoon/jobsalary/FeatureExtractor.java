package com.rainmoon.jobsalary;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import com.aliasi.symbol.MapSymbolTable;
import com.aliasi.symbol.SymbolTable;
import com.aliasi.tokenizer.TokenFeatureExtractor;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.Counter;
import com.aliasi.util.ObjectToCounterMap;
import com.aliasi.util.ObjectToDoubleMap;

/**
 * Extract features of the job salary data. 1) read documents => ids 2) prune 3)
 * document-term matrix 4) svd categories for location normalized,company name,
 * and source
 * <p>
 * FeaturesSet1:
 * title,locationNormalized,company,category,salaryNormalized,sourceName -
 * Output: csv file where each line is a feature vector
 * <p>
 * Set 2:
 * 
 * @author trung
 */
public class FeatureExtractor {
  // number of word features per description
  static final int WORD_FEATURES = 50;
  static final int TOP_WORDS_PRUNE = 50;
  // prune words appearing less than 10 times in total
  static final int WORDS_MIN = 10;
  // prune words appearing in more than 10 percent of documents
  static final int WORDS_IN_DOCUMENTS_MAX = 10;
  String myheaders = "Id,Title,FullDescription,LocationNormalized,Company,Category,SalaryNormalized,SourceName";

  /**
   * Prunes the tokens that appear in more than a specified number of documents.
   * 
   * @param counter
   * @param percent
   */
  static void pruneTopDocumentTokens(ObjectToCounterMap<String> counter,
      int numDocuments) {
    System.out.println("Pruning words in too many documents");
    int threshold = WORDS_IN_DOCUMENTS_MAX * numDocuments / 100;
    int count = 0;
    Iterator<Map.Entry<String, Counter>> iter = counter.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<String, Counter> entry = iter.next();
      if (entry.getValue().intValue() > threshold) {
        System.out.printf("%s\n", entry.getKey());
        iter.remove();
        count++;
      }
    }
    System.out.printf("%d words pruned.\n", count);
  }

  /**
   * Prunes the top {@code num} tokens from the vocabulary set.
   * 
   * @param tokenCounter
   * @param num
   */
  static void pruneTopWords(ObjectToCounterMap<String> tokenCounter) {
    HashSet<String> topKeys = new HashSet<String>(tokenCounter
        .keysOrderedByCountList().subList(0, TOP_WORDS_PRUNE));
    System.out.printf("Pruning top %d words\n", TOP_WORDS_PRUNE);
    Iterator<Map.Entry<String, Counter>> iter = tokenCounter.entrySet()
        .iterator();
    int count = 0;
    Entry<String, Counter> entry;
    while (iter.hasNext()) {
      entry = iter.next();
      if (topKeys.contains(entry.getKey())) {
        System.out.println(entry.getKey());
        iter.remove();
        count++;
      }
    }
    System.out.printf("%d stop words pruned.\n", count);
  }

  static int[][] tokenizeDocuments(List<String> documents,
      TokenizerFactory tokenizerFactory,
      ObjectToCounterMap<String> tokenCounter, SymbolTable symbolTable) {
    // count #(a token appears in a document)
    ObjectToCounterMap<String> tokDocumentCounter = new ObjectToCounterMap<String>();
    for (String document : documents) {
      HashSet<String> uniqueDocTokens = new HashSet<String>();
      char[] cs = document.toCharArray();
      Tokenizer tokenizer = tokenizerFactory.tokenizer(cs, 0, cs.length);
      for (String token : tokenizer) {
        tokenCounter.increment(token);
        if (uniqueDocTokens.add(token)) {
          tokDocumentCounter.increment(token);
        }
      }
    }
    tokenCounter.prune(WORDS_MIN);
    // pruneTopWords(tokenCounter);
    // pruneTopDocumentTokens(tokDocumentCounter, documents.size());
    Set<String> tokenSet = tokenCounter.keySet();
    for (String token : tokenSet) {
      symbolTable.getOrAddSymbol(token);
    }
    int[][] docTokenId = new int[documents.size()][WORD_FEATURES];
    for (int i = 0; i < docTokenId.length; ++i) {
      docTokenId[i] = tokenizeDocument(documents.get(i), tokenizerFactory,
          tokenCounter, symbolTable);
    }
    return docTokenId;
  }

  /**
   * Tokenizes document and returns the feature vector of this document.
   * <p>
   * The feature vector is constructed by frequency of words in the document.
   * TODO: if same frequency then probably frequency of words in the corpus?
   * 
   * @param document
   * @param tokenizerFactory
   * @param symbolTable
   * @return
   */
  static int[] tokenizeDocument(String document,
      TokenizerFactory tokenizerFactory,
      ObjectToCounterMap<String> tokenCounter, SymbolTable symbolTable) {
    System.out.println("Tokenizing " + document);
    TokenFeatureExtractor extractor = new TokenFeatureExtractor(
        tokenizerFactory);
    ObjectToCounterMap<String> counter = toCounterMap(extractor
        .features(document));
    ObjectToDoubleMap<String> newCounter = new ObjectToDoubleMap<String>();
    // reweight tokens by its global frequency
    for (String key : counter.keySet()) {
      // so that key with equal freq is ordered by word frequency
      newCounter.put(key, counter.getCount(key) + tokenCounter.getCount(key)
          / 10000.0);
    }
    List<String> orderedKeys = newCounter.keysOrderedByValueList();
    List<Integer> idList = new ArrayList<Integer>();
    // get all words if number of words too small
    int cnt = 0;
    for (String key : orderedKeys) {
      int id = symbolTable.symbolToID(key);
      if (id >= 0) {
        idList.add(id);
        cnt = cnt + 1;
        if (cnt == WORD_FEATURES)
          break;
      }
    }

    int[] tokenIds = new int[WORD_FEATURES];
    for (int i = 0; i < idList.size(); ++i)
      tokenIds[i] = idList.get(i);

    return tokenIds;
  }

  static ObjectToCounterMap<String> toCounterMap(Map<String, Counter> map) {
    ObjectToCounterMap<String> newMap = new ObjectToCounterMap<String>();
    newMap.putAll(map);
    return newMap;
  }

  public static void createMaps(String directory, String file)
      throws IOException {
    HashMap<String, Integer> locationMap = new HashMap<String, Integer>();
    HashMap<String, Integer> companyMap = new HashMap<String, Integer>();
    HashMap<String, Integer> categoryMap = new HashMap<String, Integer>();
    HashMap<String, Integer> sourceMap = new HashMap<String, Integer>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(directory + "/" + file), "utf-8"));
    String line;
    while ((line = reader.readLine()) != null) {
      System.out.println(line);
      try {
        StringTokenizer tokenizer = new StringTokenizer(line, ",");
        // skip id, job title and description
        tokenizer.nextToken();
        tokenizer.nextToken();
        tokenizer.nextToken();
        getOrAdd(locationMap, tokenizer.nextToken().toLowerCase().trim());
        getOrAdd(companyMap, tokenizer.nextToken().toLowerCase().trim());
        getOrAdd(categoryMap, tokenizer.nextToken().toLowerCase().trim());
        getOrAdd(sourceMap, tokenizer.nextToken().toLowerCase().trim());
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
    }
    reader.close();
    TextFiles.writeMap(locationMap, directory + "/processed/locationMap.csv");
    TextFiles.writeMap(companyMap, directory + "/processed/companyMap.csv");
    TextFiles.writeMap(categoryMap, directory + "/processed/categoryMap.csv");
    TextFiles.writeMap(sourceMap, directory + "/processed/sourceMap.csv");
  }

  static ArrayList<String> readDescriptions(String directory, String file)
      throws IOException {
    int defaultSize = 100000;
    ArrayList<String> fullDescs = new ArrayList<String>(defaultSize);
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(directory + "/" + file), "utf-8"));
    String line;
    StringTokenizer tokenizer;
    while ((line = reader.readLine()) != null) {
      // System.out.println(line);
      try {
        tokenizer = new StringTokenizer(line, ",");
        tokenizer.nextToken(); // skip id
        // concatenate job title and description
        fullDescs.add(tokenizer.nextToken() + " " + tokenizer.nextToken());
      } catch (Exception e) {
        System.out.println(line);
        System.out.println(e.getMessage());
      }
    }
    reader.close();
    TextFiles.writeCollection(fullDescs, directory
        + "/processed/Train_fullDescriptions.txt");
    return fullDescs;
  }

  /**
   * Creates and writes features.
   * 
   * @param directory
   * @param descFile
   * @param tokenCounter
   *          null for training set
   * @param symbolTable
   *          null for training set
   * @return
   * @throws IOException
   */
  public static void createAndWriteFeatures() throws IOException {
    String directory = "/Users/trung/Kaggle/jobsalary/data";
    String trainFile = directory + "/processed/Train_normalized.csv1";
    String validFile = directory + "/processed/Valid_normalized.csv1";
    String trainDescFile = directory + "/processed/Train_fullDescriptions.txt1";
    String validDescFile = directory + "/processed/Valid_fullDescriptions.txt1";
    String trainFeatures = directory + "/processed/train_features.csv";
    String validFeatures = directory + "/processed/valid_features.csv";

    // load maps for other attributes
    Map<String, Integer> locationMap = TextFiles.readMap(directory
        + "/processed/locationMap.csv");
    Map<String, Integer> companyMap = TextFiles.readMap(directory
        + "/processed/companyMap.csv");
    Map<String, Integer> categoryMap = TextFiles.readMap(directory
        + "/processed/categoryMap.csv");
    Map<String, Integer> sourceMap = TextFiles.readMap(directory
        + "/processed/sourceMap.csv");

    System.out.println("Reading train description file " + trainDescFile);
    List<String> fullDescs = TextFiles.readLines(trainDescFile);
    ObjectToCounterMap<String> tokenCounter = new ObjectToCounterMap<String>();
    SymbolTable symbolTable = new MapSymbolTable();

    System.out.println("tokenizing train documents");
    int[][] trainMatrix = tokenizeDocuments(fullDescs,
        JobSalaryTokenizerFactory.getInstance(), tokenCounter, symbolTable);
    writeObject(tokenCounter, directory + "/processed/tokenCounter.obj");
    writeObject(symbolTable, directory + "/processed/symbolTable.obj");

    System.out.println("writing train features");
    writeFeatures(trainFile, trainFeatures, trainMatrix, locationMap,
        companyMap, categoryMap, sourceMap, false);

    System.out.println("tokenizing valid documents");
    fullDescs = TextFiles.readLines(validDescFile);
    int[][] validMatrix = tokenizeDocuments(fullDescs,
        JobSalaryTokenizerFactory.getInstance(), tokenCounter, symbolTable);
    System.out.println("writing valid features");
    writeFeatures(validFile, validFeatures, validMatrix, locationMap,
        companyMap, categoryMap, sourceMap, true);

    // writeObject(tokenCounter, directory + "/tokenCounter.obj");
    // writeObject(symbolTable, directory + "/symbolTable.obj");
  }

  static void writeFeatures(String csvFile, String featuresFile,
      int[][] documentTermMatrix, Map<String, Integer> locationMap,
      Map<String, Integer> companyMap, Map<String, Integer> categoryMap,
      Map<String, Integer> sourceMap, boolean validation)
      throws FileNotFoundException, UnsupportedEncodingException, IOException {
    PrintWriter writer = new PrintWriter(featuresFile);
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(csvFile), "utf-8"));
    String line, id;
    Integer location, company, category, source;
    Double salary = 0.0;
    for (int i = 0; i < documentTermMatrix.length; i++) {
      line = reader.readLine();
      StringTokenizer tokenizer = new StringTokenizer(line, ",");
      // skip id, job title and description
      id = tokenizer.nextToken();
      tokenizer.nextToken();
      tokenizer.nextToken();
      location = locationMap.get(tokenizer.nextToken().toLowerCase().trim());
      company = companyMap.get(tokenizer.nextToken().toLowerCase().trim());
      category = categoryMap.get(tokenizer.nextToken().toLowerCase().trim());
      if (!validation) {
        salary = Double.parseDouble(tokenizer.nextToken());
      }
      source = sourceMap.get(tokenizer.nextToken().toLowerCase().trim());
      if (!validation) {
        writer.printf("%s,%.5f,%d,%d,%d,%d,%s\n", id, salary, location,
            company, category, source, arrayToString(documentTermMatrix[i]));
      } else {
        writer.printf("%s,%d,%d,%d,%d,%s\n", id, location, company, category,
            source, arrayToString(documentTermMatrix[i]));
      }
    }
    reader.close();
    writer.close();
  }

  static String arrayToString(int[] array) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < array.length; i++) {
      builder.append(array[i]).append(",");
    }
    return builder.toString();
  }

  static void matrixToCsv(int[][] matrix, String file) throws IOException {
    PrintWriter writer = new PrintWriter(file);
    for (int row = 0; row < matrix.length; row++) {
      for (int col = 0; col < matrix[row].length; col++) {
        writer.printf("%d,", matrix[row][col]);
      }
      writer.println();
    }
    writer.close();
  }

  static void writeObject(Object obj, String file) throws IOException {
    ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(
        file));
    writer.writeObject(obj);
    writer.close();
  }

  /*
   * Gets id of a key if it's in the map. If not, adds it to the map and returns
   * the new entry.
   */
  static Integer getOrAdd(Map<String, Integer> map, String key) {
    Integer id = map.get(key);
    if (id == null) {
      id = map.size() + 1;
      map.put(key, id);
    }
    return id;
  }

  static void preprocessData() throws Exception {
    String directory = "/Users/trung/Kaggle/jobsalary/data";
    String file = directory + "/Valid_rev1.csv";
    ArrayList<JobSalaryData> list = readData(file, 100000, true);
    TextFiles.writeCollection(list, directory
        + "/processed/Valid_normalized.csv");
  }

  /**
   * Reads data and convert into instances of job salary.
   * 
   * @param file
   * @return
   */
  public static ArrayList<JobSalaryData> readData(String file, int lines,
      boolean validation) throws IOException {
    ArrayList<JobSalaryData> list = new ArrayList<JobSalaryData>(lines);
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), "utf-8"));
    reader.readLine(); // skip header
    String line;
    JobSalaryData item = null;
    int errorCnt = 0;
    while ((line = reader.readLine()) != null) {
      try {
        item = JobSalaryData.getInstance(line, validation);
        list.add(item);
      } catch (Exception e) {
        errorCnt++;
      }
    }
    reader.close();
    System.out.printf("Valid items: %d\n error items: %d", list.size(),
        errorCnt);
    return list;
  }

  public static void main(String args[]) throws Exception {
    String directory = "/Users/trung/Kaggle/jobsalary/data";
    // createMaps(directory, "processed/Train_normalized.csv");
    // createAndWriteFeatures();

    TokenizerFactory factory = JobSalaryTokenizerFactory.getInstance();
    TokenFeatureExtractor extr = new TokenFeatureExtractor(factory);
    Map<String, Counter> map = extr
        .features("trung nguyen trung nguyenify trung abclify trung nguyen");
    System.out.println(map);
    for (Entry entry : map.entrySet()) {
      System.out.println(entry);
    }
    // ObjectInputStream reader = new ObjectInputStream(new FileInputStream(
    // directory + "/documentTerms.obj"));
    // int[][] my = (int[][]) reader.readObject();
    // System.out.println(my.length);
  }
}
