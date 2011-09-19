package edu.kaist.uilab.bs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.aliasi.symbol.MapSymbolTable;
import com.aliasi.symbol.SymbolTable;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.ModifyTokenTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.Counter;
import com.aliasi.util.ObjectToCounterMap;
import com.aliasi.util.Strings;

import edu.kaist.uilab.asc.data.Review;
import edu.kaist.uilab.stemmers.EnglishStemmer;

/**
 * Parsers for corpus used in the BS model.
 * 
 * @author trung
 */
public class CorpusParser {

  private static final String UTF8 = "utf-8";
  private static final String sentenceDelimiter = "[.!?\\n]";
  private static final int MAX_SENTENCE_LENGTH = 40;

  int mMinTokenCount;
  int mTopStopWords;
  int mTopDocumentTokens;
  String mCorpus;
  TokenizerFactory mTokenizerFactory;
  HashSet<String> mStopStems;
  HashSet<String> mSentiStems;
  SymbolTable mAspectTable;
  SymbolTable mSentiTable;
  TwogramsCounter mCounter;
  ObjectToCounterMap<String> mWordCnt;
  ArrayList<Document> mDocuments;
  ArrayList<Review> mReviews;

  int sentenceCnt = 0;
  int noSentimentSentenceCnt = 0;

  /**
   * Constructor
   * 
   * @param corpus
   *          the file that contains the corpus
   * @param minTokenCount
   *          the minimum count of a token to be retained as one word in the
   *          vocabulary
   * @param topStopWords
   *          the number of words which has highest frequency to be removed
   * @param topDocumentTokens
   *          the maximum percent of documents in which a word can appear
   * @param sentiStems
   *          the set of sentiment stems
   * @param aspectStems
   *          the set of aspect stems
   * @param stopStems
   *          the list of stop words (in addition to the standard stop words
   *          used in lingpipe)
   */
  public CorpusParser(String corpus, int minTokenCount, int topStopWords,
      int topDocumentTokens, HashSet<String> sentiStems, List<String> stopStems) {
    mCorpus = corpus;
    mMinTokenCount = minTokenCount;
    mTopStopWords = topStopWords;
    mTopDocumentTokens = topDocumentTokens;
    mStopStems = new HashSet<String>(stopStems);
    mTokenizerFactory = BSTokenizerFactory.getInstance(mStopStems);
    mSentiStems = sentiStems;
    mAspectTable = new MapSymbolTable();
    mSentiTable = new MapSymbolTable();
    mCounter = new TwogramsCounter();
    mWordCnt = new ObjectToCounterMap<String>();
    mReviews = new ArrayList<Review>();
    mDocuments = new ArrayList<Document>();
  }

  /**
   * Parses data in this corpus.
   * <p>
   * After calling this method, all properties of the corpus can be queried
   * using the various getter methods.
   */
  public void parse() throws IOException {
    readCorpus();
    tokenizeDocuments();
  }

  /**
   * Reports statistics about the corpus.
   * 
   * @param corpusFile
   *          file to store corpus information
   * @param documentsFile
   *          file to store the names of documents
   * @param aspectFile
   *          file to store all aspect tokens in the corpus
   * @param sentiFile
   *          file to store all senti tokens in the corpus
   */
  public void reportCorpus(String corpusFile, String aspectFile,
      String sentiFile) throws IOException {
    System.out.println("\nReporting corpus...");
    writeWordCount(corpusFile);
    writeTokens(mAspectTable, aspectFile);
    writeTokens(mSentiTable, sentiFile);
    System.out.printf("Sentences with no sentiment: %d/%d\n",
        noSentimentSentenceCnt, sentenceCnt);
  }

  void writeWordCount(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (String token : mWordCnt.keySet()) {
      out.printf("%s,%d\n", token, mWordCnt.getCount(token));
    }
    out.close();
  }

  void writeTokens(SymbolTable table, String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int id = 0; id < table.numSymbols(); id++) {
      out.println(table.idToSymbol(id));
    }
    out.close();
  }

  public HashSet<String> getSentiWordsSet() {
    HashSet<String> set = new HashSet<String>();
    for (int id = 0; id < mSentiTable.numSymbols(); id++) {
      set.add(mSentiTable.idToSymbol(id));
    }

    return set;
  }

  /**
   * Returns the vocabulary size of this corpus.
   * 
   * @return
   */
  public int getVocabularySize() {
    return mAspectTable.numSymbols();
  }

  /**
   * Returns the number of documents in this corpus.
   * 
   * @return
   */
  public int getNumDocuments() {
    return mDocuments.size();
  }

  /**
   * Returns the list documents in the corpus.
   */
  public ArrayList<Document> getDocuments() throws IOException {
    return mDocuments;
  }

  /**
   * Returns the symbol table of aspect words in this corpus.
   * 
   * @return
   */
  public SymbolTable getAspectSymbolTable() {
    return mAspectTable;
  }

  /**
   * Returns the symbol table for sentiment words in this corpus.
   * 
   * @return
   */
  public SymbolTable getSentiSymbolTable() {
    return mSentiTable;
  }

  /**
   * Returns the two-grams counter of this corpus.
   * 
   * @return
   */
  public TwogramsCounter getTwogramsCounter() {
    return mCounter;
  }

  /**
   * Reads documents in the corpus.
   * 
   * @throws IOException
   */
  void readCorpus() throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(mCorpus), UTF8));
    double rating;
    String line;
    while ((line = in.readLine()) != null) {
      String[] ids = line.split(" ");
      try {
        rating = Double.parseDouble(in.readLine());
      } catch (NumberFormatException e) {
        rating = -1.0;
      }
      mReviews.add(new Review(ids[0], ids[1], rating, replacePatterns(in
          .readLine())));
    }
    in.close();
  }

  /**
   * Replaces non-meaningful and negates words in a document.
   * 
   * @param document
   * @return
   */
  public static String replacePatterns(String document) {
    ArrayList<String[]> list = new ArrayList<String[]>();
    list.add(new String[] { "[http|ftp]://[\\S]*", " " });
    list.add(new String[] {
        "(not|n't|without|never)[\\s]+(very|so|too|much|"
            + "quite|even|that|as|as much|a|the|to|really|been|be)[\\s]+",
        " not_" });
    list.add(new String[] { "(not|n't|without|never|no)[\\s]+", " not_" });
    list.add(new String[] { "[()<>\\[\\],~&;:\"\\-/=*#@^+'`’]", " " });
    for (String[] rp : list) {
      if (document != null) {
        document = Pattern
            .compile(rp[0], Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
            .matcher(document).replaceAll(rp[1]);
      }
    }

    return document;
  }

  /**
   * Tokenize an array of text documents represented as character sequences into
   * a form usable by the BS model.
   * <p>
   * The symbol table should be constructed fresh for this application, but may
   * be used after this method is called for further token to symbol
   * conversions. Only tokens whose count is equal to or larger the specified
   * minimum count are included. Only tokens whose count exceeds the minimum are
   * added to the symbol table, thus producing a compact set of symbol
   * assignments to tokens for downstream processing.
   */
  void tokenizeDocuments() {
    // count #documents a token appears in
    ObjectToCounterMap<String> tokDocumentCounter = new ObjectToCounterMap<String>();
    for (Review review : mReviews) {
      HashSet<String> uniqueDocTokens = new HashSet<String>();
      char[] cs = Strings.toCharArray(review.getContent());
      Tokenizer tokenizer = mTokenizerFactory.tokenizer(cs, 0, cs.length);
      for (String token : tokenizer) {
        mWordCnt.increment(token);
        if (uniqueDocTokens.add(token)) {
          tokDocumentCounter.increment(token);
        }
      }
    }
    mWordCnt.prune(mMinTokenCount);
    pruneTopTokens(mWordCnt, mTopStopWords);
    pruneTokensInManyDocuments(tokDocumentCounter, mTopDocumentTokens);
    // index all remaining words
    Set<String> tokenSet = mWordCnt.keySet();
    for (String token : tokenSet) {
      if (isSentimentWord(token)) {
        mSentiTable.getOrAddSymbol(token);
      } else {
        mAspectTable.getOrAddSymbol(token);
      }
    }
    // convert documents into SB form
    for (Review review : mReviews) {
      tokenizeDocument(review);
    }
  }

  /**
   * Returns true if the given word might indicate a sentiment.
   * 
   * @param word
   * @return
   */
  boolean isSentimentWord(String word) {
    String negate = "not_";
    return mSentiStems.contains(word)
        || (word.startsWith(negate) && mSentiStems.contains(word
            .substring(negate.length())));
  }

  /**
   * Prunes the tokens that appear in more than a specified percentage of
   * documents.
   * 
   * @param counter
   * @param percent
   */
  private void pruneTokensInManyDocuments(ObjectToCounterMap<String> counter,
      int percent) {
    System.out.printf("\nPruning tokens appearing in many documents\n");
    int threshold = percent * mReviews.size() / 100;
    int count = 0;
    Iterator<Map.Entry<String, Counter>> iter = counter.entrySet().iterator();
    Map.Entry<String, Counter> element;
    while (iter.hasNext()) {
      element = iter.next();
      if (element.getValue().intValue() > threshold) {
        // must removes word in the wordCount
        mWordCnt.remove(element.getKey());
        System.out.print(element.getKey() + " ");
        count++;
      }
    }
    System.err.printf("\n%d words pruned.\n", count);
  }

  /**
   * Prunes the top {@code num} tokens from the vocabulary set. TODO(trung): we
   * may not need to prune these words
   * 
   * @param tokenCounter
   * @param num
   */
  void pruneTopTokens(ObjectToCounterMap<String> tokenCounter, int num) {
    System.out.printf("Pruning top %d words in the corpus\n", num);
    HashSet<String> topKeys = new HashSet<String>(tokenCounter
        .keysOrderedByCountList().subList(0, num));
    Iterator<Map.Entry<String, Counter>> iter = tokenCounter.entrySet()
        .iterator();
    while (iter.hasNext()) {
      Map.Entry<String, Counter> entry = iter.next();
      if (topKeys.contains(entry.getKey())) {
        System.out.print(entry.getKey() + " ");
        iter.remove();
      }
    }
  }

  /**
   * Tokenizes the specified text document using the specified tokenizer factory
   * returning only tokens that exist in the symbol table constructed in
   * previous step. This method is useful within a given LDA model for
   * tokenizing new documents into lists of words.
   * 
   * @param review
   *          a review
   * @return the document, null if the document does not contain any word
   */
  private Document tokenizeDocument(Review review) {
    String docContent = review.getContent();
    Document document = new Document(mDocuments.size());
    document.setReviewId(review.getReviewId());
    document.setRating(review.getRating());
    char[] cs = null;
    Tokenizer tokenizer = null;
    String[] sentences = docContent.split(sentenceDelimiter);
    for (String sentence : sentences) {
      Sentence sent = new Sentence(sentence.replaceAll("[\\s]+", " ").trim());
      cs = Strings.toCharArray(sentence);
      tokenizer = mTokenizerFactory.tokenizer(cs, 0, cs.length);
      ArrayList<String> tokens = getTokensIfHasSentiment(tokenizer);
      for (String token : tokens) {
        int id = mAspectTable.symbolToID(token);
        if (id >= 0) {
          sent.addAspectWord(id);
        }
        id = mSentiTable.symbolToID(token);
        if (id >= 0) {
          sent.addSentiWord(id);
        }
      }
      if (sent.length() > 0 && sent.length() < MAX_SENTENCE_LENGTH) {
        document.addSentence(sent);
        // updatePhraseCount(tokens);
        updatePhraseCount2(tokens);
      }
      // if (sent.hasAspectAndSenti() && sent.length() < MAX_SENTENCE_LENGTH) {
      // document.addSentence(sent);
      // updatePhraseCount(tokens);
      // }
    }
    if (document.getNumSentences() > 0) {
      mDocuments.add(document);
    } else {
      document = null;
    }
    return document;
  }

  /**
   * Update counts for phrase in the given words of a sentence.
   * 
   * @param words
   */
  private void updatePhraseCount(ArrayList<String> words) {
    int size = words.size();
    for (int idx = 0; idx < size - 1; idx++) {
      mCounter.increaseCount(words.get(idx), words.get(idx + 1));
    }
  }

  /**
   * Counts 'phrases' in a sentence.
   * <p>
   * A valid phrase must contain a sentiment word. The non-sentiment word, i.e.
   * aspect, is the word within 2 steps from the sentiment word. This accounts
   * for the cases such as "food was fantastic", "drinks were flowing", "love
   * the pleasant atmosphere", "friendly staff outshine the rest".
   * 
   * @param words
   */
  private void updatePhraseCount2(ArrayList<String> words) {
    int size = words.size();
    for (int idx = 0; idx < size; idx++) {
      String word = words.get(idx);
      if (mSentiStems.contains(word)) {
        // case 1: aspect tobe sentiment
        if (idx > 1) {
          mCounter.increaseCount(word, words.get(idx - 2));
        }
        // case 2: sentiment aspect
        if (idx < size - 1) {
          mCounter.increaseCount(word, words.get(idx + 1));
        }
      }
    }
  }

  /**
   * Returns true if the given tokenizer (a sentence) expresses a sentiment.
   * 
   * @param tokenizer
   * @return
   */
  ArrayList<String> getTokensIfHasSentiment(Tokenizer tokenizer) {
    sentenceCnt++;
    ArrayList<String> tokens = new ArrayList<String>();
    boolean hasSentiment = false;
    for (String token : tokenizer) {
      tokens.add(token);
      if (isSentimentWord(token)) {
        hasSentiment = true;
      }
    }
    if (!hasSentiment) {
      tokens.clear();
      noSentimentSentenceCnt++;
    }

    return tokens;
  }

  /**
   * Tokenizer factory for the BS model.
   * <p>
   * This tokenizer stems every word using Porter stemmer.
   */
  static final class BSTokenizerFactory extends ModifyTokenTokenizerFactory {
    static final long serialVersionUID = -3401639068551227864L;
    static final EnglishStemmer stemmer = new EnglishStemmer();
    static final int MIN_WORD_LENGTH = 2;
    static final int MAX_WORD_LENGTH = 40;

    static TokenizerFactory instance = new BSTokenizerFactory();

    private BSTokenizerFactory() {
      super(new LowerCaseTokenizerFactory(new RegExTokenizerFactory(
          "[$a-zA-Z_]+")));
    }

    @Override
    public String modifyToken(String token) {
      token = stemmer.getStem(token);
      return stop(token) ? null : token;
    }

    boolean stop(String token) {
      if (token.length() <= MIN_WORD_LENGTH
          || token.length() >= MAX_WORD_LENGTH)
        return true;
      // contains at least one letter
      for (int i = 0; i < token.length(); ++i)
        if (Character.isLetter(token.charAt(i)))
          return false;
      return true;
    }

    static TokenizerFactory getInstance(HashSet<String> stopStems) {
      return new StopTokenizerFactory(instance, stopStems);
    }
  }
}
