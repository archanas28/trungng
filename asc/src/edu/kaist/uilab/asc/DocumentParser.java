package edu.kaist.uilab.asc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * Parse the datasets into bag of sentences.
 */
public class DocumentParser {
  private static final String sentenceDelimiter = "[.!?\\n]";
  private static final String wordDelimiter = "[\\s]+";
  private static final String UTF_8 = "utf-8";

  private Object keyToBag = new Object(); // For synchronization
  private Vector<String[]> replacePatternList = new Vector<String[]>();
  private String[] wordReplacePattern;
  private TreeSet<Word> stopWords = new TreeSet<Word>();

  private int minWordLength = 2;
  private int maxWordLength = 40;
  private int minSentenceLength = 1;
  private int maxSentenceLength = 50;
  private int minWordOccur = 1;
  private int minDocLength = 1;

  private Vector<Vector<Vector<Integer>>> englishBag;
  // bag for documents in other languages
  private Vector<Vector<Vector<Integer>>> otherBag;
  private TreeMap<Word, Integer> wordIndex;

  /**
   * Constructor
   * 
   * @param minWordLength
   * @param maxWordLength
   * @param minSentenceLength
   * @param maxSentenceLength
   * @param minWordOccurrence
   * @param minDocumentLength
   */
  public DocumentParser(int minWordLength, int maxWordLength,
      int minSentenceLength, int maxSentenceLength, int minWordOccurrence,
      int minDocumentLength) {
    this.minWordLength = minWordLength;
    this.maxWordLength = maxWordLength;
    this.minSentenceLength = minSentenceLength;
    this.maxSentenceLength = maxSentenceLength;
    this.minWordOccur = minWordOccurrence;
    this.minDocLength = minDocumentLength;
    englishBag = new Vector<Vector<Vector<Integer>>>();
    otherBag = new Vector<Vector<Vector<Integer>>>();
    wordIndex = new TreeMap<Word, Integer>();
  }

  public void addReplacePattern(String pattern, String replace) {
    replacePatternList.add(new String[] { pattern, replace });
  }

  /**
   * Sets a replace pattern for words.
   * <p>
   * Each language should use a different replace pattern.
   * 
   * @param pattern
   */
  public void setWordReplacePattern(String[] pattern) {
    wordReplacePattern = pattern;
  }

  /**
   * Sets stop words as the list of words from the specified file.
   * 
   * @param path
   * @throws IOException
   */
  public void setStopWords(String path) throws IOException {
    stopWords.clear();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(path), UTF_8));
    String line;
    while ((line = in.readLine()) != null) {
      stopWords.add(new Word(line, null));
    }
    in.close();
  }

  private String replacePattern(String document) {
    for (String[] rp : replacePatternList) {
      if (document != null) {
        document = Pattern
            .compile(rp[0], Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
            .matcher(document).replaceAll(rp[1]);
      }
    }
    return document;
  }

  /**
   * Splits a document into a list of sentences.
   * 
   * @param document
   * @return
   */
  private Vector<Vector<Word>> documentToSentences(String document) {
    Vector<Vector<Word>> list = new Vector<Vector<Word>>();
    String[] sentences = document.split(sentenceDelimiter);
    for (String sentence : sentences) {
      Vector<Word> wordList = new Vector<Word>();
      String[] words = sentence.split(wordDelimiter);
      for (String word : words) {
        wordList.add(new Word(word));
      }
      list.add(wordList);
    }
    return list;
  }

  /**
   * Returns a processed sentence.
   * 
   * @param unprocessedSentence
   * @return
   */
  private Vector<Word> processSentence(Vector<Word> unprocessedSentence) {
    Vector<Word> ret = new Vector<Word>();
    for (Word word : unprocessedSentence) {
      if (word.value.length() >= minWordLength
          && word.value.length() <= maxWordLength) {
        word.value = word.value.toLowerCase();
        boolean invalidWord = false;
        String pattern = wordReplacePattern[0];
        String replace = wordReplacePattern[1];
        if (Pattern.matches("^" + pattern + "$", word.value)) {
          if (replace == null) {
            invalidWord = true;
          } else {
            word.value = Pattern.compile("^" + pattern + "$")
                .matcher(word.value).replaceAll(replace);
          }
        }
        if (!invalidWord && !stopWords.contains(word)) {
          ret.add(word);
        }
      }
    }

    return ret;
  }

  /**
   * Stores the sentences list into the bag.
   * 
   * @param sentenceList
   * @param documentName
   * @return
   */
  private void storeIntoBag(boolean isEnglishDocument,
      Vector<Vector<Word>> sentenceList, String documentName) {
    Vector<Vector<Integer>> indexSentenceList = new Vector<Vector<Integer>>();
    synchronized (keyToBag) {
      for (Vector<Word> wordList : sentenceList) {
        Vector<Integer> sentence = new Vector<Integer>();
        for (Word word : wordList) {
          Integer index = wordIndex.get(word);
          if (index == null) {
            index = wordIndex.size();
            wordIndex.put(word, index);
          }
          sentence.add(index);
        }
        indexSentenceList.add(sentence);
      }
      if (isEnglishDocument) {
        englishBag.add(indexSentenceList);
      } else {
        otherBag.add(indexSentenceList);
      }
    }
  }

  /**
   * Processes a document.
   * 
   * @param document
   * @param documentName
   * @return
   * @throws Exception
   */
  public void build(boolean isEnglishDocument, String document,
      String documentName) {
    document = replacePattern(document);
    if (document != null) {
      Vector<Vector<Word>> list = documentToSentences(document);
      Vector<Vector<Word>> sentenceList = new Vector<Vector<Word>>();
      int numWords = 0;
      for (Vector<Word> s : list) {
        Vector<Word> sentence = processSentence(s);
        if (sentence.size() >= minSentenceLength
            && sentence.size() <= maxSentenceLength) {
          sentenceList.add(sentence);
          numWords += sentence.size();
        }
      }
      if (numWords >= minDocLength) {
        storeIntoBag(isEnglishDocument, sentenceList, documentName);
      }
    }  
  }

  /**
   * Removes words that occur less than the specified occurrence.
   */
  public void filterWords() {
    System.out.print("Filtering words...");
    TreeSet<Word> removeWords = new TreeSet<Word>();
    TreeSet<Integer> removeWordIndices = new TreeSet<Integer>();
    int numDocs = englishBag.size() + otherBag.size();
    while (true) {
      // why?
      int[] wordCount = getWordCount();
      int cntRemoveWords = 0;
      for (Word word : wordIndex.keySet()) {
        int idx = wordIndex.get(word);
        if (wordCount[idx] < minWordOccur || wordCount[idx] > numDocs * 0.6) {
          if (!removeWords.contains(word)) {
            removeWords.add(word);
            removeWordIndices.add(idx);
            cntRemoveWords++;
          }
        }
      }

      if (cntRemoveWords == 0) {
        break;
      }
      englishBag = filterBag(englishBag, removeWordIndices);
      otherBag = filterBag(otherBag, removeWordIndices);
      // break;
    }

    for (Word word : removeWords) {
      wordIndex.remove(word);
    }
    HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
    Integer newWordIndex = 0;
    for (Word word : wordIndex.keySet()) {
      map.put(wordIndex.get(word), newWordIndex);
      wordIndex.put(word, newWordIndex++);
    }
    englishBag = rewriteDocuments(englishBag, map);
    otherBag = rewriteDocuments(otherBag, map);
    System.out.println("done");
  }

  /**
   * Filters bag.
   * 
   * @param bag
   * @param removeWordIndices
   */
  Vector<Vector<Vector<Integer>>> filterBag(
      Vector<Vector<Vector<Integer>>> bag, TreeSet<Integer> removeWordIndices) {
    Vector<Vector<Vector<Integer>>> newBag = new Vector<Vector<Vector<Integer>>>();
    for (Vector<Vector<Integer>> document : bag) {
      Vector<Vector<Integer>> newDocument = filterDocument(document,
          removeWordIndices);
      if (newDocument.size() > 0) {
        newBag.add(newDocument);
      }
    }
    return newBag;
  }

  /**
   * Removes all words in the
   * <code>removeWordIndices<code> set from the specified
   * document.
   * 
   * @param document
   * @param removeWordIndices
   * @return
   */
  Vector<Vector<Integer>> filterDocument(Vector<Vector<Integer>> document,
      TreeSet<Integer> removeWordIndices) {
    Vector<Vector<Integer>> newDocument = new Vector<Vector<Integer>>();
    for (Vector<Integer> sentence : document) {
      Vector<Integer> newSentence = new Vector<Integer>();
      for (Integer wordIndex : sentence) {
        if (!removeWordIndices.contains(wordIndex))
          newSentence.add(wordIndex);
      }
      if (newSentence.size() >= minSentenceLength
          && newSentence.size() <= maxSentenceLength) {
        newDocument.add(newSentence);
      }
    }
    return newDocument;
  }

  /**
   * Rewrites documents using the new indices of words.
   * 
   * @param bag
   * @param map
   *          the mapping between old index (key) and new index (value)
   */
  private Vector<Vector<Vector<Integer>>> rewriteDocuments(
      Vector<Vector<Vector<Integer>>> bag, HashMap<Integer, Integer> map) {
    Vector<Vector<Vector<Integer>>> newBag = new Vector<Vector<Vector<Integer>>>();
    for (Vector<Vector<Integer>> document : bag) {
      Vector<Vector<Integer>> newDocument = new Vector<Vector<Integer>>();
      for (Vector<Integer> sentence : document) {
        Vector<Integer> newSentence = new Vector<Integer>();
        for (int oldWordIndex : sentence) {
          if (map.get(oldWordIndex) != null) {
            newSentence.add(map.get(oldWordIndex));
          }
        }
        newDocument.add(newSentence);
      }
      newBag.add(newDocument);
    }
    return newBag;
  }

  /**
   * Returns the number of unique words, i.e., the vocabulary size.
   * 
   * @return
   */
  public int getNumUniqueWords() {
    return wordIndex.size();
  }

  public String[] getWordList() {
    String[] list = new String[wordIndex.size()];
    for (Word word : wordIndex.keySet()) {
      if (word.hasTag())
        list[wordIndex.get(word)] = word.value + "/" + word.tag;
      else
        list[wordIndex.get(word)] = word.value;
    }
    return list;
  }

  /**
   * Gets word count for all words.
   * 
   * @return
   */
  public int[] getWordCount() {
    int[] count = new int[wordIndex.size()];
    for (Vector<Vector<Integer>> sentenceList : englishBag) {
      for (Vector<Integer> sentence : sentenceList) {
        for (int wordIndex : sentence) {
          count[wordIndex]++;
        }
      }
    }
    for (Vector<Vector<Integer>> sentenceList : otherBag) {
      for (Vector<Integer> sentence : sentenceList) {
        for (int wordIndex : sentence) {
          count[wordIndex]++;
        }
      }
    }
    return count;
  }

  /**
   * Gets the total of words in this dataset.
   * 
   * @return
   */
  public int getNumTotalWords() {
    int numTotalWords = 0;
    for (Vector<Vector<Integer>> sentenceList : englishBag) {
      for (Vector<Integer> sentence : sentenceList) {
        numTotalWords += sentence.size();
      }
    }
    for (Vector<Vector<Integer>> sentenceList : otherBag) {
      for (Vector<Integer> sentence : sentenceList) {
        numTotalWords += sentence.size();
      }
    }
    return numTotalWords;
  }

  private void writeWordCount(String file, String[] wordList)
      throws IOException {
    int[] wordCount = getWordCount();
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), UTF_8));
    for (int i = 0; i < wordList.length; i++)
      out.println("\"" + wordList[i].replaceAll("\"", "\"\"") + "\","
          + wordCount[i]);
    out.close();
  }

  public void writeWordList(String file, String[] wordList) throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), UTF_8));
    for (String word : wordList) {
      out.println(word);
    }
    out.close();
  }

  private void writeBagOfSentences(Vector<Vector<Vector<Integer>>> bag,
      String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (Vector<Vector<Integer>> sentenceList : bag) {
      out.println(sentenceList.size());
      for (Vector<Integer> sentence : sentenceList) {
        for (int wordIndex : sentence)
          out.print(wordIndex + " ");
        out.println();
      }
    }
    out.close();
  }

  /**
   * Writes the processed data to files.
   * 
   * @param outDir
   * @throws Exception
   */
  public void writeOutFiles(String outDir) throws IOException {
    String[] wordList = getWordList();
    writeWordCount(outDir + "/" + Application.wordcountFile, wordList);
    writeWordList(outDir + "/" + Application.wordlistFile, wordList);
    writeBagOfSentences(englishBag, outDir + "/" + Application.englishDocuments);
    writeBagOfSentences(otherBag, outDir + "/" + Application.otherDocuments);
  }

  public String toString() {
    String str = "# Vocabulary size: " + getNumUniqueWords() + "\n";
    str += "# Total Words: " + getNumTotalWords() + "\n";
    str += String.format("# Documents: %d (en = %d)\n", englishBag.size()
        + otherBag.size(), englishBag.size());
    return str;
  }
}
