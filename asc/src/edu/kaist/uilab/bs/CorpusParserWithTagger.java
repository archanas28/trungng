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
import edu.kaist.uilab.asc.data.ReviewReader;
import edu.kaist.uilab.asc.util.TextFiles;
import edu.kaist.uilab.bs.util.DocumentUtils;
import edu.kaist.uilab.stemmers.EnglishStemmer;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Parser for parsing a corpus. This parser uses a a part-of-speech tagger to
 * determine if a word in a sentence has the function of conveying a sentiment.
 * 
 * @author trung
 */
public class CorpusParserWithTagger {

  private static final int MAX_SENTENCE_LENGTH = 40;
  private static final String UTF8 = "utf-8";
  private final String[] nounTags = { "NN", "NNS" };

  private MaxentTagger tagger = MaxentTaggerSingleton.getInstance();

  int mMinTokenCount;
  int mTopStopWords;
  int mTopDocumentTokens;
  String mCorpus;
  ReviewReader mReviewReader;
  TokenizerFactory mTokenizerFactory;
  HashSet<String> mStopStems;
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
   * @param reviewReader
   *          a reader to read reviews
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
  public CorpusParserWithTagger(String corpus, ReviewReader reviewReader,
      int minTokenCount, int topStopWords, int topDocumentTokens,
      List<String> stopStems) throws Exception {
    mCorpus = corpus;
    mReviewReader = reviewReader;
    mMinTokenCount = minTokenCount;
    mTopStopWords = topStopWords;
    mTopDocumentTokens = topDocumentTokens;
    mStopStems = new HashSet<String>(stopStems);
    mTokenizerFactory = BSTokenizerFactory.getInstance(mStopStems);
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
    mReviews = readCorpus(mCorpus);
    tokenizeReviews(mReviews);
    System.out.printf("\n#sentences with no sentiments %d/%d\n",
        noSentimentSentenceCnt, sentenceCnt);
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
   * Reads documents in the corpus and returns them as a list of {@link Review}
   * s.
   * 
   * @throws IOException
   */
  public ArrayList<Review> readCorpus(String corpus) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(corpus), UTF8));
    ArrayList<Review> list = new ArrayList<Review>();
    Review review = null;
    do {
      review = mReviewReader.readReview(in, true);
      if (review != null) {
        list.add(review);
      }
    } while (review != null);
    in.close();

    return list;
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
  void tokenizeReviews(ArrayList<Review> reviews) {
    // count #documents a token appears in
    ObjectToCounterMap<String> tokDocumentCounter = new ObjectToCounterMap<String>();
    for (Review review : reviews) {
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
    // convert documents into a form usable in BS
    for (Review review : reviews) {
      Document document = tokenizeDocument(review);
      if (document != null) {
        document.setReviewId(review.getReviewId());
        document.setRating(review.getRating());
      }
    }
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
   * Prunes the top {@code num} tokens from the vocabulary set.
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
   * Tokenizes the specified text document using the same tokenizer factory
   * returning only tokens that exist in the symbol table constructed in
   * previous step.
   * 
   * @param review
   *          a review
   * @return the document, null if the document does not contain any word
   */
  private Document tokenizeDocument(Review review) {
    String docContent = review.getContent();
    Document document = new Document(mDocuments.size(), review.getReviewId(),
        review.getRestaurantId());
    List<ArrayList<? extends HasWord>> tokenizedSentences = DocumentUtils
        .tokenizeSentences(docContent.replaceAll("not_", "not "), false);
    for (int idx = 0; idx < tokenizedSentences.size(); idx++) {
      sentenceCnt++;
      ArrayList<? extends HasWord> tokenizedSentence = tokenizedSentences
          .get(idx);
      ArrayList<TaggedWord> tSentence = tagger.tagSentence(tokenizedSentence);
      ArrayList<String> sentimentStems = getLowercaseSentimentStems(tSentence);
      if (sentimentStems.isEmpty()) {
        noSentimentSentenceCnt++;
        continue;
      }
      String txt = DocumentUtils.tokenizedSentenceToText(tokenizedSentence);
      Sentence sentence = new Sentence(txt);
      for (String token : mTokenizerFactory.tokenizer(Strings.toCharArray(txt),
          0, txt.length())) {
        if (sentimentStems.contains(token)) {
          sentence.addSentiWord(mSentiTable.getOrAddSymbol(token));
        } else {
          sentence.addAspectWord(mAspectTable.getOrAddSymbol(token));
        }
      }
      if (sentence.length() > 0 && sentence.length() < MAX_SENTENCE_LENGTH) {
        document.addSentence(sentence);
        updatePhraseCount(tSentence);
      }
    }
    if (document.getNumSentences() > 0) {
      mDocuments.add(document);
    } else {
      document = null;
    }
    return document;
  }

  /**
   * Returns the sentiment stems of the specified tagged sentence.
   * 
   * @param tWords
   * @return
   */
  private ArrayList<String> getLowercaseSentimentStems(
      ArrayList<TaggedWord> tSentence) {
    ArrayList<String> list = new ArrayList<String>();
    for (int idx = 0; idx < tSentence.size(); idx++) {
      TaggedWord tWord = tSentence.get(idx);
      String stem = BSTokenizerFactory.stemmer.getStem(tWord.word()
          .toLowerCase());
      tWord.setWord(stem);
      if (stem.length() < 3 || !mWordCnt.containsKey(stem))
        continue;
      if (SentimentPrior.isSentiTag(tWord.tag())
          || SentimentPrior.isSentiWord(stem)) {
        if (idx > 0 && tSentence.get(idx - 1).word().equals("not")) {
          list.add("not_" + stem);
        } else {
          list.add(stem);
        }
      }
    }

    return list;
  }

  /**
   * Counts senti-aspect pairs in a sentence.
   * <p>
   * A valid phrase must contain a sentiment word. The non-sentiment word, i.e.
   * aspect, is the word within 2 steps from the sentiment word. This accounts
   * for the cases such as "food was fantastic", "drinks were flowing", "love
   * the pleasant atmosphere", "friendly staff outshine the rest".
   * 
   * @param tSentence
   */
  private void updatePhraseCount(ArrayList<TaggedWord> tSentence) {
    int size = tSentence.size();
    for (int idx = 0; idx < size; idx++) {
      TaggedWord tWord = tSentence.get(idx);
      String word = tWord.word();
      if (idx > 0 && tSentence.get(idx - 1).word().equals("not")) {
        word = "not_" + word;
      }
      if (SentimentPrior.isSentiTag(tWord.tag())) {
        if (idx < size - 1 && isNounTag(tSentence.get(idx + 1).tag())) {
          // case 1: sentiment aspect
          mCounter.increaseCount(word, tSentence.get(idx + 1).word());
        } else if (idx > 1) {
          // case 2: aspect tobe sentiment
          for (int nounIdx = idx - 1; nounIdx >= 0; nounIdx--) {
            if (isNounTag(tSentence.get(nounIdx).tag())) {
              mCounter.increaseCount(word, tSentence.get(nounIdx).word());
              break;
            }
          }
        }
      }
    }
  }

  boolean isNounTag(String tag) {
    for (String nTag : nounTags) {
      if (nTag.equals(tag)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Tokenizer factory for the BS model.
   * <p>
   * This tokenizer stems every word using Porter stemmer.
   */
  public static final class BSTokenizerFactory extends
      ModifyTokenTokenizerFactory {
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

    /**
     * Returns an instance of this factory using <code>stopStems</code> as the
     * set of stop stems.
     * 
     * @param stopStems
     * @return
     */
    public static TokenizerFactory getInstance(HashSet<String> stopStems) {
      return new StopTokenizerFactory(instance, stopStems);
    }

    /**
     * Returns an instance of this factory using the stems from
     * <code>stopfile</code> as the set of stop stems.
     * 
     * @param stopfile
     * @return
     */
    public static TokenizerFactory getInstance(String stopfile) {
      HashSet<String> stopStems = new HashSet<String>();
      try {
        stopStems = new HashSet<String>(TextFiles.readLines(stopfile));
      } catch (IOException e) {
        e.printStackTrace();
      }

      return new StopTokenizerFactory(instance, stopStems);
    }
  }

  public static void main(String[] args) throws Exception {
    // BSCorpusParserWithTagger bs = new BSCorpusParserWithTagger(null, 0, 0, 0,
    // new ArrayList<String>());
  }
}
