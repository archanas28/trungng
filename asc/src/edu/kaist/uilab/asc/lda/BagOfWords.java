package edu.kaist.uilab.asc.lda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

public class BagOfWords {
  private Object keyToBag = new Object(); // For synchronization

  public Vector<String[]> replacePatternList = new Vector<String[]>();
  public Vector<String[]> collapsePatternList = new Vector<String[]>();
  public Vector<String> removeWordPatternList = new Vector<String>();

  public HashSet<String> stopWords = new HashSet<String>();

  public String wordDelimiter = "[\\s]+";
  public int minWordLength = 1;
  public int maxWordLength = 1000;
  public int minWordOccur = 1;
  public int minWordDocFreq = 1;
  public int minDocLength = 1;
  public boolean caseSensitive = false;

  public Vector<TreeMap<Integer, Integer>> bag;
  private TreeMap<String, Integer> wordIndex;
  public Vector<String> docList;

  public static void main(String[] args) throws Exception {
    String dir = "C:/datasets/asc/ldatest";
    BagOfWords bow = new BagOfWords();
    bow.addReplacePattern(
        "(not|n't|without)[\\s]+(very|so|too|much|quite|even|that|as|as much|a|the|to|really|just)[\\s]+",
        " not");
    bow.addReplacePattern("(not|n't|without|no)[\\s]+", " not");
    bow.addReplacePattern("[http|ftp]://[\\S]*", " ");
    bow.addReplacePattern("[?()<>!\\[\\].,~&;:'\"\\-/=*#]", " ");
    bow.removeWordPatternList.add(".*[0-9()<>,&;\"].*");
    bow.minWordLength = 2;
    bow.maxWordLength = 30;
    bow.minWordOccur = 3;
    bow.minDocLength = 10;
    bow.setStopWords(dir + "/Stopwords.txt");

    HashMap<String, String> ratings = new HashMap<String, String>();
    BufferedReader fileReader = new BufferedReader(new InputStreamReader(
        new FileInputStream(dir + "/Reviews.txt"), "utf-8"));
    String docName;
    while ((docName = fileReader.readLine()) != null && !docName.equals("")) {
      ratings.put(docName, fileReader.readLine());
      String document = fileReader.readLine();
      bow.build(document, docName, null);
    }
    bow.filterWords();

    bow.writeOutFiles(dir);
    PrintWriter out = new PrintWriter(new FileWriter(new File(dir
        + "/Polarity.txt")));
    for (String doc : bow.docList)
      out.println(ratings.get(doc));
    out.close();
  }

  public BagOfWords() throws Exception {
    bag = new Vector<TreeMap<Integer, Integer>>();
    wordIndex = new TreeMap<String, Integer>();
    docList = new Vector<String>();
  }

  public void addReplacePattern(String pattern, String replace) {
    String[] rp = new String[2];
    rp[0] = new String(pattern);
    rp[1] = new String(replace);
    this.replacePatternList.add(rp);
  }

  public void addCollapsePattern(String pattern, String collapse) {
    String[] cp = new String[2];
    cp[0] = new String(pattern);
    cp[1] = new String(collapse);
    this.collapsePatternList.add(cp);
  }

  public void setStopWords(String path) throws Exception {
    stopWords.clear();
    BufferedReader stopwordsFile = new BufferedReader(new FileReader(new File(
        path)));
    String line;
    while ((line = stopwordsFile.readLine()) != null) {
      stopWords.add(line);
    }
    stopwordsFile.close();
  }

  public TreeMap<Integer, Integer> build(String document, String documentName)
      throws Exception {
    return build(document, documentName, null);
  }

  public TreeMap<Integer, Integer> build(String document, String documentName,
      String authorName) throws Exception {
    // Replace patterns in replacePatternList
    // if (document.length() == 1) System.out.println(document);
    for (String[] rp : this.replacePatternList) {
      document = Pattern
          .compile(rp[0], Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
          .matcher(document).replaceAll(rp[1]);
    }

    // Tokenize
    // Replace patterns in replacePatternList
    for (String[] rp : this.replacePatternList) {
      document = Pattern
          .compile(rp[0], Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
          .matcher(document).replaceAll(rp[1]);
    }
    for (String[] cp : this.collapsePatternList) {
      document = Pattern
          .compile(cp[0], Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
          .matcher(document).replaceAll(cp[1]);
    }

    TreeMap<String, Integer> wordCount = new TreeMap<String, Integer>();
    int numTotalWords = 0;
    String[] words = document.split(wordDelimiter);
    for (String word : words) {
      word = word.toLowerCase();
      boolean invalidWord = false;
      if (word.length() < this.minWordLength
          || word.length() > this.maxWordLength)
        continue;
      for (String removeWordPattern : removeWordPatternList)
        if (Pattern.matches(removeWordPattern, word)) {
          invalidWord = true;
          break;
        }
      if (invalidWord)
        continue;
      // Stop words
      if (stopWords.contains(word))
        continue;
      Integer count = wordCount.get(word);
      if (count == null)
        wordCount.put(word, 1);
      else
        wordCount.put(word, count + 1);
      numTotalWords++;
    }

    if (numTotalWords < minDocLength)
      return null;

    // Store into the bag
    TreeMap<Integer, Integer> wordIndexCount = new TreeMap<Integer, Integer>();
    synchronized (this.keyToBag) {
      for (String word : wordCount.keySet()) {
        Integer index = wordIndex.get(word);
        if (index == null) {
          index = getNumUniqueWords();
          wordIndex.put(word, index);
        }
        wordIndexCount.put(index, wordCount.get(word));
      }
      this.bag.add(wordIndexCount);
      this.docList.add(documentName);
    }

    return wordIndexCount;
  }

  public void filterWords() {
    System.out.println("Filtering Words...");

    TreeSet<String> removeWords = new TreeSet<String>();
    TreeSet<Integer> removeWordIndices = new TreeSet<Integer>();
    while (true) {
      int[] wordCount = getWordCount();
      int[] wordDocFreq = getWordDocFreq();
      int cntRemoveWords = 0;
      for (String word : wordIndex.keySet()) {
        if (wordCount[wordIndex.get(word)] < minWordOccur
            || wordDocFreq[wordIndex.get(word)] < minWordDocFreq) {
          if (!removeWords.contains(word)) {
            removeWords.add(word);
            removeWordIndices.add(wordIndex.get(word));
            cntRemoveWords++;
          }
        }
      }

      if (cntRemoveWords == 0)
        break;

      TreeSet<Integer> removeDocList = new TreeSet<Integer>();
      Vector<TreeMap<Integer, Integer>> newBag = new Vector<TreeMap<Integer, Integer>>();
      for (int d = 0; d < this.bag.size(); d++) {
        TreeMap<Integer, Integer> document = this.bag.get(d);
        TreeMap<Integer, Integer> newDocument = new TreeMap<Integer, Integer>();
        for (Map.Entry<Integer, Integer> entry : document.entrySet()) {
          if (!removeWordIndices.contains(entry.getKey()))
            newDocument.put(entry.getKey(), entry.getValue());
        }
        if (newDocument.size() >= this.minDocLength)
          newBag.add(newDocument);
        else
          removeDocList.add(d);
      }
      this.bag = newBag;
      int numDeleted = 0;
      for (int d : removeDocList) {
        this.docList.removeElementAt(d - numDeleted);
        numDeleted++;
      }
      assert (this.bag.size() == this.docList.size());

    }

    for (String word : removeWords)
      this.wordIndex.remove(word);
    sort();
  }

  public String[] getWordList() {
    String[] list = new String[getNumUniqueWords()];
    for (String word : wordIndex.keySet()) {
      list[wordIndex.get(word)] = word;
    }
    return list;
  }

  public int[] getWordCount() {
    int[] count = new int[getNumUniqueWords()];
    for (int i = 0; i < count.length; i++)
      count[i] = 0;
    for (TreeMap<Integer, Integer> document : this.bag) {
      for (Integer wordIndex : document.keySet()) {
        count[wordIndex] += document.get(wordIndex);
      }
    }
    return count;
  }

  public int[] getWordDocFreq() {
    int[] count = new int[getNumUniqueWords()];
    for (TreeMap<Integer, Integer> document : this.bag) {
      for (Integer wordIndex : document.keySet())
        count[wordIndex]++;
    }
    return count;
  }

  public Vector<TreeMap<Integer, Integer>> getBagOfWords() {
    return this.bag;
  }

  public int getNumUniqueWords() {
    return this.wordIndex.size();
  }

  public int getNumDocs() {
    return this.bag.size();
  }

  public void sort() {
    System.out.println("Sorting Words...");
    HashMap<Integer, Integer> indexToIndex = new HashMap<Integer, Integer>();
    int newWordIndex = 0;
    for (String word : wordIndex.keySet()) {
      indexToIndex.put(wordIndex.get(word), newWordIndex);
      wordIndex.put(word, newWordIndex++);
    }
    int numTotalWords = 0;
    Vector<TreeMap<Integer, Integer>> newBag = new Vector<TreeMap<Integer, Integer>>();
    for (TreeMap<Integer, Integer> wordsFreq : this.bag) {
      TreeMap<Integer, Integer> newWordsFreq = new TreeMap<Integer, Integer>();
      for (Integer oldIndex : wordsFreq.keySet()) {
        Integer newIndex = indexToIndex.get(oldIndex);
        if (newIndex != null) {
          int wordCount = wordsFreq.get(oldIndex);
          newWordsFreq.put(newIndex, wordCount);
          numTotalWords += wordCount;
        }
      }
      newBag.add(newWordsFreq);
    }
    this.bag = newBag;
  }

  public String toString() {
    String str = "# Unique Words: " + getNumUniqueWords() + "\n";
    str += "# Documents: " + this.bag.size() + "\n";
    str += "------------------------------------------\n";
    for (int i = 0; i < this.bag.size(); i++) {
      str += i + "";
      TreeMap<Integer, Integer> wordIndexCount = this.bag.get(i);
      for (Integer wordIndex : wordIndexCount.keySet())
        str += "\t" + wordIndex + "(" + wordIndexCount.get(wordIndex) + ")";
      str += "\n";
    }
    str += "------------------------------------------\n";
    return str;
  }

  public void writeOutFiles(String outDir) throws Exception {
    String[] wordList = this.getWordList();

    int[] wordCount = this.getWordCount();
    PrintWriter out = new PrintWriter(new FileWriter(new File(outDir
        + "/WordCount.csv")));
    for (int i = 0; i < wordList.length; i++)
      out.println("\"" + wordList[i].replaceAll("\"", "\"\"") + "\","
          + wordCount[i]);
    out.close();

    out = new PrintWriter(new FileWriter(new File(outDir + "/WordList.txt")));
    for (String word : wordList)
      out.println(word);
    out.close();

    Vector<String> docList = this.docList;
    out = new PrintWriter(
        new FileWriter(new File(outDir + "/DocumentList.txt")));
    for (String doc : docList)
      out.println(doc);
    out.close();

    Vector<TreeMap<Integer, Integer>> bagOfWords = getBagOfWords();
    out = new PrintWriter(new FileWriter(new File(outDir + "/BagOfWords.txt")));
    for (TreeMap<Integer, Integer> words : bagOfWords) {
      int nTotalWords = 0;
      String str = "";
      for (Integer wordIndex : words.keySet()) {
        int wordCountPerDoc = words.get(wordIndex);
        str += wordIndex + " " + wordCountPerDoc + " ";
        nTotalWords += wordCountPerDoc;
      }
      out.println(words.keySet().size() + " " + nTotalWords + "\n" + str);
    }
    out.close();
  }

}
