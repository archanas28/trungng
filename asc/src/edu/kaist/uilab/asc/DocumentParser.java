package edu.kaist.uilab.asc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import edu.kaist.uilab.asc.stemmer.SnowballStemmer;

/**
 * Parse the datasets into bag of sentences. TODO(trung): use local lowercase
 * for each language
 */
public class DocumentParser {

  private static final String sentenceDelimiter = "[.!?\\n]";
  private static final String wordDelimiter = "[\\s]+";
  private static final String UTF_8 = "utf-8";

  private Object keyToBag = new Object(); // For synchronization
  private Vector<String[]> replacePatternList = new Vector<String[]>();
  private String[] wordReplacePattern;
  private TreeSet<String> stopStems = new TreeSet<String>();
  private HashMap<String, String> stemMap;

  private Locale locale = Locale.ENGLISH;
  private int minWordLength = 2;
  private int maxWordLength = 40;
  private int minSentenceLength = 1;
  private int maxSentenceLength = 50;
  private int minWordOccur = 1;
  private int minDocLength = 1;
  private SnowballStemmer stemmer = null;

  private Vector<Vector<Vector<Integer>>> englishBag;
  private ArrayList<Double> englishRatings;
  private ArrayList<Double> otherRatings;
  // bag for documents in other languages
  private Vector<Vector<Vector<Integer>>> otherBag;
  private TreeMap<LocaleWord, Integer> wordIndex;

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
    englishRatings = new ArrayList<Double>();
    otherRatings = new ArrayList<Double>();
    wordIndex = new TreeMap<LocaleWord, Integer>();
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
   * Sets the language of the corpus being processed.
   * 
   * @param lang
   */
  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  /**
   * Sets stop words as the list of words from the specified file.
   * 
   * @param path
   * @throws IOException
   */
  public void setStopWords(String path) throws IOException {
    stopStems.clear();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(path), UTF_8));
    String line;
    while ((line = in.readLine()) != null) {
      stopStems.add(line);
    }
    in.close();
  }

  /**
   * Sets the stemmer to use for parsing the current corpus.
   * 
   * @param stemmer
   */
  public void setStemmer(SnowballStemmer stemmer) {
    this.stemmer = stemmer;
    stemMap = new HashMap<String, String>();
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
  private Vector<Vector<LocaleWord>> documentToSentences(String document) {
    Vector<Vector<LocaleWord>> list = new Vector<Vector<LocaleWord>>();
    String[] sentences = document.split(sentenceDelimiter);
    for (String sentence : sentences) {
      Vector<LocaleWord> wordList = new Vector<LocaleWord>();
      String[] words = sentence.split(wordDelimiter);
      for (String word : words) {
        wordList.add(new LocaleWord(word.toLowerCase(locale), locale));
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
  private Vector<LocaleWord> processSentence(
      Vector<LocaleWord> unprocessedSentence) {
    Vector<LocaleWord> ret = new Vector<LocaleWord>();
    for (LocaleWord word : unprocessedSentence) {
      if (word.mValue.length() >= minWordLength
          && word.mValue.length() <= maxWordLength) {
        boolean invalidWord = false;
        String pattern = wordReplacePattern[0];
        String replace = wordReplacePattern[1];
        if (Pattern.matches("^" + pattern + "$", word.mValue)) {
          if (replace == null) {
            invalidWord = true;
          } else {
            word.mValue = Pattern.compile("^" + pattern + "$")
                .matcher(word.mValue).replaceAll(replace);
          }
        }
        if (!invalidWord) {
          if (stemmer != null) {
            String stem = stemmer.getStem(word.mValue);
            stemMap.put(word.mValue, stem);
            word.mValue = stem;
          }
          if (!stopStems.contains(word.mValue)) {
            ret.add(word);
          }
        }
      }
    }

    return ret;
  }

  /**
   * Stores the sentences list into the bag.
   * 
   * @param sentenceList
   * @return
   */
  private void storeIntoBag(Vector<Vector<LocaleWord>> sentenceList) {
    Vector<Vector<Integer>> indexSentenceList = new Vector<Vector<Integer>>();
    synchronized (keyToBag) {
      for (Vector<LocaleWord> wordList : sentenceList) {
        Vector<Integer> sentence = new Vector<Integer>();
        for (LocaleWord word : wordList) {
          Integer index = wordIndex.get(word);
          if (index == null) {
            index = wordIndex.size();
            wordIndex.put(word, index);
          }
          sentence.add(index);
        }
        indexSentenceList.add(sentence);
      }
      if (locale.equals(Locale.ENGLISH)) {
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
   * @param rating
   * @return
   */
  public void build(String document, double rating) {
    document = replacePattern(document);
    if (document != null) {
      Vector<Vector<LocaleWord>> list = documentToSentences(document);
      Vector<Vector<LocaleWord>> sentenceList = new Vector<Vector<LocaleWord>>();
      int numWords = 0;
      for (Vector<LocaleWord> s : list) {
        Vector<LocaleWord> sentence = processSentence(s);
        if (sentence.size() >= minSentenceLength
            && sentence.size() <= maxSentenceLength) {
          sentenceList.add(sentence);
          numWords += sentence.size();
        }
      }
      if (numWords >= minDocLength) {
        storeIntoBag(sentenceList);
        if (locale.equals(Locale.ENGLISH)) {
          englishRatings.add(rating);
        } else {
          otherRatings.add(rating);
        }
      }
    }
  }

  /**
   * Removes words that occur less than the specified occurrence.
   */
  public void filterWords() {
    System.out.print("Filtering words...");
    TreeSet<LocaleWord> removeWords = new TreeSet<LocaleWord>();
    TreeSet<Integer> removeWordIndices = new TreeSet<Integer>();
    int numDocs = englishBag.size() + otherBag.size();
    while (true) {
      // why?
      int[] wordCount = getWordCount();
      int cntRemoveWords = 0;
      for (LocaleWord word : wordIndex.keySet()) {
        int idx = wordIndex.get(word);
        if (wordCount[idx] < minWordOccur || wordCount[idx] > numDocs * 0.3) {
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
      englishBag = filterBag(englishBag, englishRatings, removeWordIndices);
      otherBag = filterBag(otherBag, otherRatings, removeWordIndices);
      // break;
    }

    for (LocaleWord word : removeWords) {
      wordIndex.remove(word);
    }
    HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
    Integer newWordIndex = 0;
    for (LocaleWord word : wordIndex.keySet()) {
      map.put(wordIndex.get(word), newWordIndex);
      wordIndex.put(word, newWordIndex++);
    }
    englishBag = reindexDocuments(englishBag, map);
    otherBag = reindexDocuments(otherBag, map);
    System.out.println("done");
  }

  /**
   * Filters bag.
   * 
   * @param bag
   * @param removeWordIndices
   */
  Vector<Vector<Vector<Integer>>> filterBag(
      Vector<Vector<Vector<Integer>>> bag, ArrayList<Double> oldRating,
      TreeSet<Integer> removeWordIndices) {
    Vector<Vector<Vector<Integer>>> newBag = new Vector<Vector<Vector<Integer>>>();
    ArrayList<Double> newRating = new ArrayList<Double>();
    for (int idx = 0; idx < bag.size(); idx++) {
      Vector<Vector<Integer>> newDocument = filterDocument(bag.get(idx),
          removeWordIndices);
      if (newDocument.size() > 0) {
        newBag.add(newDocument);
        newRating.add(oldRating.get(idx));
      }
    }
    oldRating.clear();
    oldRating.addAll(newRating);
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
   * Re-indexs documents using the new indices of words.
   * 
   * @param bag
   * @param map
   *          the mapping between old index (key) and new index (value)
   */
  private Vector<Vector<Vector<Integer>>> reindexDocuments(
      Vector<Vector<Vector<Integer>>> bag, HashMap<Integer, Integer> map) {
    Vector<Vector<Vector<Integer>>> newBag = new Vector<Vector<Vector<Integer>>>();
    for (Vector<Vector<Integer>> document : bag) {
      Vector<Vector<Integer>> newDocument = new Vector<Vector<Integer>>();
      for (Vector<Integer> sentence : document) {
        Vector<Integer> newSentence = new Vector<Integer>();
        for (Integer oldWordIndex : sentence) {
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

  LocaleWord[] getWordList() {
    LocaleWord[] list = new LocaleWord[wordIndex.size()];
    for (LocaleWord word : wordIndex.keySet()) {
      list[wordIndex.get(word)] = word;
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

  private void writeWordCount(String file, LocaleWord[] wordList)
      throws IOException {
    int[] wordCount = getWordCount();
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), UTF_8));
    for (int i = 0; i < wordList.length; i++)
      out.printf("%s,%d\n", wordList[i], wordCount[i]);
    out.close();
  }

  public void writeWordList(String file, LocaleWord[] wordList)
      throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), UTF_8));
    for (LocaleWord word : wordList) {
      out.println(word);
    }
    out.close();
  }

  /**
   * Writes documents together with their ratings to the specified file.
   * 
   * @param documents
   * @param file
   * @throws IOException
   */
  private void writeDocuments(Vector<Vector<Vector<Integer>>> documents,
      ArrayList<Double> ratings, String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int idx = 0; idx < documents.size(); idx++) {
      Vector<Vector<Integer>> document = documents.get(idx);
      out.printf("%f %d\n", ratings.get(idx), document.size());
      for (Vector<Integer> sentence : document) {
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
    LocaleWord[] wordList = getWordList();
    writeWordCount(outDir + "/" + Application.wordcountFile, wordList);
    writeWordList(outDir + "/" + Application.wordlistFile, wordList);
    writeDocuments(englishBag, englishRatings, outDir + "/"
        + Application.enDocuments);
    writeDocuments(otherBag, otherRatings, outDir + "/"
        + Application.otherDocuments);
    printSubjectivityStatistics();
  }

  void printSubjectivityStatistics() {
    int numPos = 0, numNeg = 0, numNeutral = 0;
    for (Double rating : englishRatings) {
      if (rating > 3.0) {
        numPos++;
      } else if (rating >= 0 && rating < 3.0) {
        numNeg++;
      } else {
        numNeutral++;
      }
    }
    double size = englishRatings.size();
    System.out
        .printf(
            "English corpus(total=%d):\tpos = %d\tneg = %d\tneutral or not rated = %d\n",
            (int) size, numPos, numNeg, numNeutral);
    numPos = 0;
    numNeg = 0;
    numNeutral = 0;
    for (Double rating : otherRatings) {
      if (rating > 3.0) {
        numPos++;
      } else if (rating >= 0 && rating < 3.0) {
        numNeg++;
      } else {
        numNeutral++;
      }
    }
    size = otherRatings.size();
    System.out
        .printf(
            "French corpus(total=%d):\tpos = %d\tneg = %d\tneutral or not rated = %d\n",
            (int) size, numPos, numNeg, numNeutral);
  }

  /**
   * Writes the map of the original words and their stems to the specified file.
   * <p>
   * The stem map is clear after the writing process so that it can be used with
   * other lingual corpus.
   */
  public void writeAndClearsStemMap(String file) throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), UTF_8));
    for (Entry<String, String> entry : stemMap.entrySet()) {
      out.printf("%s %s\n", entry.getKey(), entry.getValue());
    }
    out.close();
    stemMap.clear();
  }

  public String toString() {
    String str = "# Vocabulary size: " + getNumUniqueWords() + "\n";
    str += "# Total Words: " + getNumTotalWords() + "\n";
    str += String.format("# Documents: %d (en = %d)\n", englishBag.size()
        + otherBag.size(), englishBag.size());
    return str;
  }
}
