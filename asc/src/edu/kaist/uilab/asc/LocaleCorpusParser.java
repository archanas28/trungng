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
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import edu.kaist.uilab.stemmer.SnowballStemmer;

/**
 * Parser that support language-specific parsing of documents.
 */
public class LocaleCorpusParser {

  private static final String sentenceDelimiter = "[.!?\\n]";
  private static final String wordDelimiter = "[\\s]+";
  private static final String UTF_8 = "utf-8";

  private Object keyToBag = new Object(); // For synchronization
  private Vector<String[]> mReplacePatternList = new Vector<String[]>();
  private String[] mWordReplacePattern;
  private TreeSet<String> mStopStems = new TreeSet<String>();
  private HashMap<String, String> mStemMap;

  private Locale mLocale = Locale.ENGLISH;
  private int mMinWordLength = 2;
  private int mMaxWordLength = 40;
  private int mMinSentenceLength = 1;
  private int mMaxSentenceLength = 50;
  private int mMinWordOccur = 1;
  private int mMinDocLength = 1;
  private SnowballStemmer mStemmer = null;

  private Vector<Vector<Vector<Integer>>> mDocuments;
  private ArrayList<Double> mRatings;
  private TreeMap<LocaleWord, Integer> mWordIndex;

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
  public LocaleCorpusParser(int minWordLength, int maxWordLength,
      int minSentenceLength, int maxSentenceLength, int minWordOccurrence,
      int minDocumentLength) {
    mMinWordLength = minWordLength;
    mMaxWordLength = maxWordLength;
    mMinSentenceLength = minSentenceLength;
    mMaxSentenceLength = maxSentenceLength;
    mMinWordOccur = minWordOccurrence;
    mMinDocLength = minDocumentLength;
    mDocuments = new Vector<Vector<Vector<Integer>>>();
    mRatings = new ArrayList<Double>();
    mWordIndex = new TreeMap<LocaleWord, Integer>();
  }

  public void addReplacePattern(String pattern, String replace) {
    mReplacePatternList.add(new String[] { pattern, replace });
  }

  /**
   * Sets a replace pattern for words.
   * <p>
   * Each language should use a different replace pattern.
   * 
   * @param pattern
   */
  public void setWordReplacePattern(String[] pattern) {
    mWordReplacePattern = pattern;
  }

  /**
   * Sets the language of the corpus being processed.
   * 
   * @param lang
   */
  public void setLocale(Locale locale) {
    mLocale = locale;
  }

  /**
   * Sets stop words as the list of words from the specified file.
   * 
   * @param path
   * @throws IOException
   */
  public void setStopStems(String path) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(path), UTF_8));
    String line;
    while ((line = in.readLine()) != null) {
      mStopStems.add(line);
    }
    in.close();
  }

  /**
   * Sets the stemmer to use for parsing the current corpus.
   * 
   * @param stemmer
   */
  public void setStemmer(SnowballStemmer stemmer) {
    this.mStemmer = stemmer;
    mStemMap = new HashMap<String, String>();
  }

  private String replacePattern(String document) {
    for (String[] rp : mReplacePatternList) {
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
        wordList.add(new LocaleWord(word.toLowerCase(mLocale), mLocale));
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
      if (word.mValue.length() >= mMinWordLength
          && word.mValue.length() <= mMaxWordLength) {
        boolean invalidWord = false;
        String pattern = mWordReplacePattern[0];
        String replace = mWordReplacePattern[1];
        if (Pattern.matches("^" + pattern + "$", word.mValue)) {
          if (replace == null) {
            invalidWord = true;
          } else {
            word.mValue = Pattern.compile("^" + pattern + "$")
                .matcher(word.mValue).replaceAll(replace);
          }
        }
        if (!invalidWord) {
          if (mStemmer != null) {
            String stem = mStemmer.getStem(word.mValue);
            mStemMap.put(word.mValue, stem);
            word.mValue = stem;
          }
          if (!mStopStems.contains(word.mValue)) {
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
          Integer index = mWordIndex.get(word);
          if (index == null) {
            index = mWordIndex.size();
            mWordIndex.put(word, index);
          }
          sentence.add(index);
        }
        indexSentenceList.add(sentence);
      }
      mDocuments.add(indexSentenceList);
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
        if (sentence.size() >= mMinSentenceLength
            && sentence.size() <= mMaxSentenceLength) {
          sentenceList.add(sentence);
          numWords += sentence.size();
        }
      }
      if (numWords >= mMinDocLength) {
        storeIntoBag(sentenceList);
        mRatings.add(rating);
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
    int numDocs = mDocuments.size();
    while (true) { // why?
      int[] wordCount = new int[mWordIndex.size()];
      int[] docCount = new int[mWordIndex.size()];
      getWordCount(wordCount, docCount);
      int cntRemoveWords = 0;
      for (LocaleWord word : mWordIndex.keySet()) {
        int idx = mWordIndex.get(word);
        if (wordCount[idx] < mMinWordOccur || docCount[idx] > numDocs * 0.1) {
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
      mDocuments = filterBag(mDocuments, mRatings, removeWordIndices);
      // break;
    }
    for (LocaleWord word : removeWords) {
      mWordIndex.remove(word);
    }
  }

  /**
   * Gets word count for all words (before re-indexing documents).
   * 
   * @param corpusCount
   *          array to store the count of a word in the entire corpus
   * @param docCount
   *          array to store the total number of documents that a word appears
   *          in
   */
  private void getWordCount(int[] corpusCount, int[] docCount) {
    for (Vector<Vector<Integer>> document : mDocuments) {
      HashSet<Integer> uniqueWords = new HashSet<Integer>();
      for (Vector<Integer> sentence : document) {
        for (int wordIndex : sentence) {
          corpusCount[wordIndex]++;
          uniqueWords.add(wordIndex);
        }
      }
      for (Integer index : uniqueWords) {
        docCount[index]++;
      }
    }
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
      if (newSentence.size() >= mMinSentenceLength
          && newSentence.size() <= mMaxSentenceLength) {
        newDocument.add(newSentence);
      }
    }
    return newDocument;
  }

  /**
   * Re-indexs documents with new indice of words. The word indice now are part
   * of a global indexing for corpora of all languages.
   * 
   * @param startingIdx
   *          the starting index for re-indexing
   * @return the last index used in the process
   */
  public int reindexDocuments(int startingIdx) {
    int newIdx = startingIdx;
    HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
    for (LocaleWord word : mWordIndex.keySet()) {
      map.put(mWordIndex.get(word), newIdx);
      mWordIndex.put(word, newIdx++);
    }
    Vector<Vector<Vector<Integer>>> newBag = new Vector<Vector<Vector<Integer>>>();
    for (Vector<Vector<Integer>> document : mDocuments) {
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
    mDocuments = newBag;
    return newIdx;
  }

  /**
   * Returns the number of unique words in this specific corpus.
   * 
   * @return
   */
  public int getNumUniqueWords() {
    return mWordIndex.size();
  }

  public TreeMap<LocaleWord, Integer> getWordIndex() {
    return mWordIndex;
  }

  /**
   * Gets word count for all words in this language-specific corpus (after
   * re-indexing documents).
   */
  HashMap<Integer, Integer> getWordCountMap() {
    HashMap<Integer, Integer> wordCnt = new HashMap<Integer, Integer>();
    for (Vector<Vector<Integer>> sentenceList : mDocuments) {
      for (Vector<Integer> sentence : sentenceList) {
        for (int wordIdx : sentence) {
          Integer count = wordCnt.get(wordIdx);
          if (count == null) {
            wordCnt.put(wordIdx, 1);
          } else {
            wordCnt.put(wordIdx, count + 1);
          }
        }
      }
    }

    return wordCnt;
  }

  /**
   * Writes word count for this language-specific corpus into the provided
   * output stream.
   * 
   * @param out
   * @throws IOException
   */
  public void writeWordCount(PrintWriter out) throws IOException {
    HashMap<Integer, Integer> wordCount = getWordCountMap();
    for (LocaleWord word : mWordIndex.keySet()) {
      out.printf("%s,%d\n", word, wordCount.get(mWordIndex.get(word)));
    }
  }

  public Vector<Vector<Vector<Integer>>> getDocuments() {
    return mDocuments;
  }

  public ArrayList<Double> getRatings() {
    return mRatings;
  }

  /**
   * Gets the total of words in this language-specific corpus.
   * 
   * @return
   */
  public int getNumTotalWords() {
    int numTotalWords = 0;
    for (Vector<Vector<Integer>> sentenceList : mDocuments) {
      for (Vector<Integer> sentence : sentenceList) {
        numTotalWords += sentence.size();
      }
    }
    return numTotalWords;
  }

  /**
   * Prints statistics about subjectivity of documents.
   */
  public void printSubjectivityStatistics() {
    int numPos = 0, numNeg = 0, numNeutral = 0;
    for (Double rating : mRatings) {
      if (rating > 3.0) {
        numPos++;
      } else if (rating >= 0 && rating < 3.0) {
        numNeg++;
      } else {
        numNeutral++;
      }
    }
    System.out.printf(
        "\nSubjectivity:\tpos = %d\tneg = %d\tneu/not rated = %d\n", numPos,
        numNeg, numNeutral);
  }

  /**
   * Writes the map of the original words and their stems to the specified file.
   */
  public void writeStemMap(String file) throws IOException {
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), UTF_8));
    for (Entry<String, String> entry : mStemMap.entrySet()) {
      out.printf("%s %s\n", entry.getKey(), entry.getValue());
    }
    out.close();
    mStemMap.clear();
  }

  public String toString() {
    String str = "\nCorpus information: ";
    str += "\n# unique words: " + getNumUniqueWords();
    str += "\n# total words: " + getNumTotalWords();
    str += String.format("\n# documents: %d", mDocuments.size());
    return str;
  }
}
