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

import edu.kaist.uilab.stemmers.SnowballStemmer;

/**
 * Parser that support language-specific parsing of documents.
 * 
 * TODO(trung): This is very messy & sloppy. Use the same parser as BSCopurParser.
 * Also, we could do without re-indexing the words by removing stop words based on
 * words rather than indices.
 * 
 * @author trung
 */
public class LocaleCorpusParser {

  private static final String sentenceDelimiter = "[.!?\\n]";
  private static final String wordDelimiter = "[\\s]+";
  private static final String UTF_8 = "utf-8";

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
  private SnowballStemmer mStemmer = null;

  public static final class Structure {
    Vector<Vector<Integer>> sentences;
    Vector<String> sentenceTexts;
    String id;

    public Structure(String id) {
      this(id, new Vector<Vector<Integer>>(), new Vector<String>());
    }

    public Structure(String id, Vector<Vector<Integer>> sentences,
        Vector<String> sentenceTexts) {
      this.sentences = sentences;
      this.sentenceTexts = sentenceTexts;
      this.id = id;
    }

    public void addSentence(Vector<Integer> sentence, String txt) {
      sentences.add(sentence);
      sentenceTexts.add(txt.replaceAll("[\\s]+", " ").trim());
    }
  }

  private Vector<Structure> mStructures;
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
    mStructures = new Vector<Structure>();
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
   * Splits all sentences into vector of locale words.
   * 
   * @param document
   * @return
   */
  private Vector<Vector<LocaleWord>> sentencesToLocaleWords(String[] sentences) {
    Vector<Vector<LocaleWord>> list = new Vector<Vector<LocaleWord>>();
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
  private void storeIntoBag(String id, Vector<Vector<LocaleWord>> sentenceList,
      Vector<String> sentenceTxts) {
    Vector<Vector<Integer>> indexSentenceList = new Vector<Vector<Integer>>();
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
    mStructures.add(new Structure(id, indexSentenceList, sentenceTxts));
  }

  /**
   * Processes a document.
   * 
   * @param document
   * @param rating
   * @return
   */
  public void build(String id, String document, double rating) {
    document = replacePattern(document);
    if (document != null) {
      String[] sentences = document.split(sentenceDelimiter);
      Vector<Vector<LocaleWord>> list = sentencesToLocaleWords(sentences);
      Vector<Vector<LocaleWord>> sentenceList = new Vector<Vector<LocaleWord>>();
      Vector<String> sentenceTxts = new Vector<String>();
      for (int i = 0; i < list.size(); i++) {
        Vector<LocaleWord> sentence = processSentence(list.get(i));
        if (sentence.size() >= mMinSentenceLength
            && sentence.size() <= mMaxSentenceLength) {
          sentenceList.add(sentence);
          sentenceTxts.add(sentences[i]);
        }
      }
      storeIntoBag(id, sentenceList, sentenceTxts);
      mRatings.add(rating);
    }
  }

  /**
   * Removes words that occur less than the specified occurrence.
   */
  public void filterWords() {
    System.out.print("Filtering words...");
    TreeSet<LocaleWord> removeWords = new TreeSet<LocaleWord>();
    TreeSet<Integer> removeWordIndices = new TreeSet<Integer>();
    while (true) { // why?
      int[] wordCount = new int[mWordIndex.size()];
      getWordCount(wordCount);
      int cntRemoveWords = 0;
      for (LocaleWord word : mWordIndex.keySet()) {
        int idx = mWordIndex.get(word);
        if (wordCount[idx] < mMinWordOccur) {
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
      mStructures = filterBag(mStructures, mRatings, removeWordIndices);
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
   */
  private void getWordCount(int[] corpusCount) {
    for (Structure structure : mStructures) {
      Vector<Vector<Integer>> document = structure.sentences;
      for (Vector<Integer> sentence : document) {
        for (int wordIndex : sentence) {
          corpusCount[wordIndex]++;
        }
      }
    }
  }

  /**
   * Filters bag.
   * 
   * @param bag
   * @param removeWordIndices
   */
  Vector<Structure> filterBag(Vector<Structure> bag,
      ArrayList<Double> oldRating, TreeSet<Integer> removeWordIndices) {
    Vector<Structure> newBag = new Vector<Structure>();
    ArrayList<Double> newRating = new ArrayList<Double>();
    for (int idx = 0; idx < bag.size(); idx++) {
      Structure newDocument = filterDocument(bag.get(idx), removeWordIndices);
      if (newDocument.sentences.size() > 0) {
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
  Structure filterDocument(Structure document,
      TreeSet<Integer> removeWordIndices) {
    Structure newDocument = new Structure(document.id);
    for (int i = 0; i < document.sentences.size(); i++) {
      Vector<Integer> sentence = document.sentences.get(i);
      Vector<Integer> newSentence = new Vector<Integer>();
      for (Integer wordIndex : sentence) {
        if (!removeWordIndices.contains(wordIndex))
          newSentence.add(wordIndex);
      }
      if (newSentence.size() >= mMinSentenceLength
          && newSentence.size() <= mMaxSentenceLength) {
        newDocument.addSentence(newSentence, document.sentenceTexts.get(i));
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
    Vector<Structure> newBag = new Vector<Structure>();
    for (Structure document : mStructures) {
      Structure newDocument = new Structure(document.id);
      for (int i = 0; i < document.sentences.size(); i++) {
        Vector<Integer> sentence = document.sentences.get(i);
        Vector<Integer> newSentence = new Vector<Integer>();
        for (Integer oldWordIndex : sentence) {
          if (map.get(oldWordIndex) != null) {
            newSentence.add(map.get(oldWordIndex));
          }
        }
        newDocument.addSentence(newSentence, document.sentenceTexts.get(i));
      }
      newBag.add(newDocument);
    }
    mStructures = newBag;
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
    for (Structure structure : mStructures) {
      for (Vector<Integer> sentence : structure.sentences) {
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

  public Vector<Structure> getDocuments() {
    return mStructures;
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
    for (Structure document : mStructures) {
      for (Vector<Integer> sentence : document.sentences) {
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
    str += String.format("\n# documents: %d", mStructures.size());
    return str;
  }
}
