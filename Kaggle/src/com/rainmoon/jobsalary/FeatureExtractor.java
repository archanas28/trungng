package com.rainmoon.jobsalary;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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

import au.com.bytecode.opencsv.CSVReader;

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
    CSVReader reader = new CSVReader(new FileReader(directory + "/" + file));
    String[] tokens;
//    id + "," + jobTitle + "," + fullDescription + ","
//    + locationNormalized + "," + company + "," + category + ","
//    + salaryNormalized + "," + sourceName;
    int cnt = 0;
    while ((tokens = reader.readNext()) != null) {
      // try {
      System.out.println(cnt++);
      // skip id, job title,description,salary tokens[0,1,2,6]
      getOrAdd(locationMap, tokens[3].toLowerCase().trim());
      getOrAdd(companyMap, tokens[4].toLowerCase().trim());
      getOrAdd(categoryMap, tokens[5].toLowerCase().trim());
      getOrAdd(sourceMap, tokens[7].toLowerCase().trim());
      // } catch (Exception e) {
      // System.err.println("Errooor " + e.getMessage());
      // }
    }
    reader.close();
    System.out.println("Writing map to files");
    TextFiles.writeMap(locationMap, directory + "/processed/locationMap.csv");
    TextFiles.writeMap(companyMap, directory + "/processed/companyMap.csv");
    TextFiles.writeMap(categoryMap, directory + "/processed/categoryMap.csv");
    TextFiles.writeMap(sourceMap, directory + "/processed/sourceMap.csv");
    
  }

  /**
   * Reads normalized file and write to ad description files.
   * 
   * @param normalizedFile
   * @param descFilefile
   * @return
   * @throws IOException
   */
  static ArrayList<String> readDescriptions(String normalizedFile,
      String descFilefile) throws IOException {
    int defaultSize = 100000;
    ArrayList<String> fullDescs = new ArrayList<String>(defaultSize);
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(normalizedFile), "utf-8"));
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
        System.err.println("Erroooooor" + line);
      }
    }
    reader.close();
    TextFiles.writeCollection(fullDescs, descFilefile);
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
    String trainFile = directory + "/processed/Train_normalized.csv";
    String validFile = directory + "/processed/Valid_normalized.csv";
    String trainDescFile = directory + "/processed/Train_fullDescriptions.txt";
    String validDescFile = directory + "/processed/Valid_fullDescriptions.txt";
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

  /**
   * Reads data and convert into instances of job salary.
   * 
   * @param file
   * @return
   */
  public static void normalizeData(String file, String normalizedFile,
      boolean validation) throws IOException {
    CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(
        new FileInputStream(file), "utf-8")));
    PrintWriter writer = new PrintWriter(normalizedFile);
    reader.readNext(); // skip header
    String[] tokens;
    JobSalaryData item = null;
    int errorCnt = 0;
    while ((tokens = reader.readNext()) != null) {
      try {
        item = JobSalaryData.getInstance(tokens, validation);
        writer.println(item);
      } catch (Exception e) {
        System.err.println(e.getMessage());
        errorCnt++;
      }
    }
    reader.close();
    writer.close();
    System.out.printf("error items: %d\n", errorCnt);
  }

  public static void main(String args[]) throws Exception {
    String directory = "/home/trung/projects/Kaggle/data";
    // Step 1: remove some unnecessary fields
    // System.out.println("reading train");
    // normalizeData(directory + "/Train_rev1.csv", directory +
    // "/processed/Train_normalized.csv",
    // false);
    // System.out.println("reading validation");
    // normalizeData(directory + "/Valid_rev1.csv", directory
    // + "/processed/Valid_normalized.csv", true);

    // Step 2 : create maps from string <-> category
    createMaps(directory, "processed/Train_normalized.csv");
//    readDescriptions(directory + "/processed/Train_normalized.csv", directory
//        + "/processed/Train_fullDescriptions.txt");
//    readDescriptions(directory + "/processed/Valid_normalized.csv", directory
//        + "/processed/Valid_fullDescriptions.txt");
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
