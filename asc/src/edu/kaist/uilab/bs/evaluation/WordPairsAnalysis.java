package edu.kaist.uilab.bs.evaluation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import com.aliasi.symbol.SymbolTable;
import com.aliasi.util.ObjectToCounterMap;

import edu.kaist.uilab.asc.data.Review;
import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.bs.Document;
import edu.kaist.uilab.bs.DocumentUtils;
import edu.kaist.uilab.bs.MaxentTaggerSingleton;
import edu.kaist.uilab.bs.Model;
import edu.kaist.uilab.bs.Sentence;
import edu.kaist.uilab.bs.UrsaDataset;
import edu.kaist.uilab.stemmers.EnglishStemmer;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Analyzes the noun-adjective word pairs using different scoring methods.
 * <p>
 * Following is the procedure presumably used by the Review Spotlight paper.
 * <ul>
 * <li>Aggregate all reviews of a restaurant as a document, i.e., all reviews
 * are equal.
 * <li>Using term frequency to find the top word pairs for a document (all
 * reviews of a restaurant).
 * <li>Using inverse frequency to find the top word pairs for a document based
 * on the freq of all reviews.
 * <li>For evaluation, select the top 5 pairs (from each method) and display to
 * users.
 * </ul>
 * 
 * @author trung
 */
public class WordPairsAnalysis {
  private MaxentTagger tagger = MaxentTaggerSingleton.getInstance();
  Model model;
  String[][] aspectWords;
  String[][][] sentimentWords;
  DoubleMatrix[] phiSenti;
  double[][] phiAspect;
  SymbolTable sentiTable, aspectTable;
  int numTopWords;

  public WordPairsAnalysis() {
    aspectWords = new String[0][0];
    sentimentWords = new String[0][0][0];
  }

  /**
   * Constructor
   * 
   * @param model
   *          a trained model that can be used for filtering word pairs
   * @param numTopWords
   */
  public WordPairsAnalysis(Model model, int numTopWords) {
    this.model = model;
    this.numTopWords = numTopWords;
    aspectWords = model.getTopAspectWords(numTopWords);
    sentimentWords = model.getTopSentiWords(numTopWords);
    phiSenti = model.getPhiSentiByTermscore();
    phiAspect = model.getPhiAspectByTermscore();
    sentiTable = model.getSentiTable();
    aspectTable = model.getAspectTable();
  }

  /**
   * Returns the document frequency (df) of word pairs in the entire corpus.
   * 
   * @param documents
   *          a list of string with each element corresponds to the content of a
   *          document
   * @return
   */
  public ObjectToCounterMap<WordPair> getDocumentFrequency(
      ArrayList<String> documents) {
    ObjectToCounterMap<WordPair> df = new ObjectToCounterMap<WordPair>();
    for (String document : documents) {
      getTermFrequency(document, df);
    }

    return df;
  }

  /**
   * Returns the term frequency (tf) of word pairs in the document given its
   * <code>content</code>.
   * 
   * @param content
   *          the document content
   * @param df
   *          the document frequency (df) of word pairs in the entire corpus,
   *          <code>null</code> if not available
   * @return
   */
  ObjectToCounterMap<WordPair> getTermFrequency(String content,
      ObjectToCounterMap<WordPair> df) {
    ObjectToCounterMap<WordPair> counter = new ObjectToCounterMap<WordPair>();
    HashSet<WordPair> countedSet = new HashSet<WordPair>();
    List<ArrayList<? extends HasWord>> tokenizedSentences = MaxentTagger
        .tokenizeText(new BufferedReader(new StringReader(content)));
    for (ArrayList<? extends HasWord> tokenizedSentence : tokenizedSentences) {
      ArrayList<WordPair> pairs = getWordPairs(tagger
          .tagSentence(tokenizedSentence));
      for (WordPair pair : pairs) {
        counter.increment(pair);
        if (df != null) {
          if (!countedSet.contains(pair)) {
            df.increment(pair);
          } else {
            countedSet.add(pair);
          }
        }
      }
    }

    return counter;
  }

  /**
   * Returns a counter of word pairs in the document with given
   * <code>content</code>.
   * 
   * @param content
   * @return
   */
  public ObjectToCounterMap<WordPair> getTermFrequency(String content) {
    return getTermFrequency(content, null);
  }

  /**
   * Returns pair of (adj, noun) in the given sentence.
   * 
   * @param sentence
   * @return
   */
  ArrayList<WordPair> getWordPairs(ArrayList<TaggedWord> tWords) {
    ArrayList<WordPair> list = new ArrayList<WordPair>();
    int size = tWords.size();
    for (int idx = 0; idx < size; idx++) {
      TaggedWord tWord = tWords.get(idx);
      if (DocumentUtils.isSentiWord(tWord)) {
        int cursor = 1;
        do {
          TaggedWord candidate = idx + cursor < size ? tWords.get(idx + cursor)
              : null;
          if (candidate != null && DocumentUtils.isNoun(candidate)) {
            list.add(new WordPair(tWord.word(), candidate.word()));
            break;
          }
          candidate = idx - cursor >= 0 ? tWords.get(idx - cursor) : null;
          if (candidate != null && DocumentUtils.isNoun(candidate)) {
            list.add(new WordPair(tWord.word(), candidate.word()));
            break;
          }
          cursor++;
        } while (idx - cursor >= 0 || idx + cursor < size);
      }
    }

    return list;
  }

  /**
   * Prints out the top word pairs of restaurants whose number of reviews is
   * between the <code>(minReviews, maxReviews)</code> range.
   * <p>
   * The word pairs used in this method is obtained using the simple approach.
   */
  public void printTopWordPairs(PrintWriter out,
      HashMap<String, ArrayList<Review>> map, ObjectToCounterMap<WordPair> df,
      int minReviews, int maxReviews, int numRestaurants) {
    int cnt = 0;
    int numDocuments = map.size();
    int printSize = 100;
    for (Entry<String, ArrayList<Review>> entry : map.entrySet()) {
      ArrayList<Review> reviews = entry.getValue();
      int numReviews = reviews.size();
      if (numReviews < maxReviews && numReviews >= minReviews) {
        String allReviews = aggregateReviews(reviews);
        ObjectToCounterMap<WordPair> tf = getTermFrequency(allReviews);
        // ObjectToCounterMap<WordPair> tfidf = new
        // ObjectToCounterMap<WordPair>();
        // for (WordPair pair : tf.keySet()) {
        // double idf = Math.log(((double) numDocuments) / df.getCount(pair));
        // tfidf.put(pair, new Counter((int) (tf.getCount(pair) * idf)));
        // }
        out.printf("<h4>Restaurant %s (#reviews = %d, #pairs: %d)</h4>",
            entry.getKey(), numReviews, printSize);
        out.print("<b>Top word pairs ranked by term frequency:&nbsp;&nbsp;</b>");
        printTopElements(out, tf, printSize);
        out.print("<b>Top aspects ranked by term frequency:&nbsp;&nbsp;</b>");
        printTopAspects(out, tf, printSize * 2);
        // System.err.print("TF-IDF\t");
        // printTopWordPairs(out, tfidf, printSize);
        if (cnt++ > numRestaurants) {
          break;
        }
      }
    }
  }

  /**
   * Prints the top aspects of a restaurant given its word pair
   * <code>counter</code>.
   * 
   * @param out
   * @param counter
   * @param printSize
   */
  private void printTopAspects(PrintWriter out,
      ObjectToCounterMap<WordPair> counter, int printSize) {
    ObjectToCounterMap<String> aspectCounter = new ObjectToCounterMap<String>();
    for (WordPair pair : counter.keySet()) {
      aspectCounter.increment(pair.stemNoun, counter.getCount(pair));
    }
    printTopElements(out, aspectCounter, printSize);
  }

  /**
   * Prints the top elements of <code>counter</code> to the given print writer.
   * 
   * @param out
   * @param counter
   * @param printSize
   */
  private <T> void printTopElements(PrintWriter out,
      ObjectToCounterMap<T> counter, int printSize) {
    int size = printSize > counter.size() ? counter.size() : printSize;
    List<T> orderedList = counter.keysOrderedByCountList();
    for (int idx = 0; idx < size; idx++) {
      T pair = orderedList.get(idx);
      out.printf("%s = %d; ", pair, counter.getCount(pair));
    }
    out.print("<br /><br />");
  }

  /**
   * Summarize reviews trained in the model by word pairs.
   * <p>
   * The method for extracting word pairs from a review is as follows.
   * <ul>
   * <li>First, extract the word pairs of a sentence as in normal approach,
   * i.e., an adjective and a noun forms a candidate pair</li>.
   * <li>Then, filter these pairs by retaining only pairs whose both of the
   * sentiment and aspect word are in the top words of the inferred aspect of
   * the sentence.
   * </ul>
   * 
   * @param model
   */
  public void summarizeByWordPairs(HashMap<String, ArrayList<Review>> map,
      int numSamples, String output) throws IOException {
    int numTopWords = 100;
    PrintWriter out = new PrintWriter(output + numTopWords + ".html");
    out.println("<html><title>Summary</title><body>");
    String[] sentiColors = { "green", "red" };
    List<Document> documents = model.getDocuments();
    for (int idx = 0; idx < numSamples; idx++) {
      Document document = documents.get(idx);
      String restId = document.getRestaurantId();
      out.printf("<h4>Restaurant = %s[%d], Review = %s</h4>", restId,
          map.get(restId).size(), document.getReviewId());
      Review review = getReview(map.get(restId), document.getReviewId());
      List<ArrayList<? extends HasWord>> tSentences = DocumentUtils
          .tokenizeSentences(DocumentUtils.negate(review.getContent())
              .replaceAll("not_", "not "));
      for (ArrayList<? extends HasWord> tSentence : tSentences) {
        ArrayList<WordPair> wordPairs = getWordPairs(tagger
            .tagSentence(tSentence));
        Sentence bsSentence = findSentence(document, tSentence);
        if (bsSentence != null && bsSentence.getSenti() >= 0) {
          out.printf("Aspect %d, <span style=\"color:%s\">%s.</span><br />",
              bsSentence.getTopic(), sentiColors[bsSentence.getSenti()],
              bsSentence.getText());
          out.printf("&nbsp;&nbsp;&nbsp;&nbsp;%s<br />", wordPairs);
          wordPairs = filterWordPairs(null, wordPairs, bsSentence);
          out.printf("&nbsp;&nbsp;&nbsp;&nbsp;\t%s<br />", wordPairs);
        }
      }
    }
    out.println("</body></html>");
    out.close();
  }

  /**
   * Prints aspect-based word pairs summary for a number of sample reviews.
   * 
   * @param model
   * @param map
   * @param numSamples
   * @param output
   * @throws IOException
   */
  public void summarizeReviews(HashMap<String, ArrayList<Review>> map,
      int numSamples, String output) throws IOException {
    int maxPrintSize = 40;
    PrintWriter out = new PrintWriter(output + numTopWords + ".html");
    out.println("<html><title>Summary of reviews</title><body>");
    List<Document> documents = model.getDocuments();
    for (int idx = 0; idx < numSamples; idx++) {
      Document document = documents.get(idx);
      String restId = document.getRestaurantId();
      Review review = getReview(map.get(restId), document.getReviewId());
      ArrayList<ObjectToCounterMap<WordPair>> list = reviewToWordPairs(null,
          documents.get(idx), review, model.getNumTopics());
      out.printf("<h4>Restaurant = %s[%d], Review = %s</h4>", restId,
          map.get(restId).size(), document.getReviewId());
      out.printf(
          "<span style=\"width:700px;display:block;color:#6B8E23\">%s</span>",
          review.getContent());
      printWordPairsByAspects(out, maxPrintSize, list);
    }
    out.println("</body></html>");
    out.close();
  }

  /**
   * Prints the word pairs of all aspects.
   * 
   * @param out
   *          a print writer for printing
   * @param maxPrintSize
   *          maximum number of pairs to print for each aspect
   * @param list
   *          list of word pair counters for all aspects
   */
  private void printWordPairsByAspects(PrintWriter out, int maxPrintSize,
      ArrayList<ObjectToCounterMap<WordPair>> list) {
    int numAspects = list.size();
    for (int aspect = 0; aspect < numAspects; aspect++) {
      out.print("<span style=\"width:800px\">");
      out.printf("Aspect %d: ", aspect);
      ObjectToCounterMap<WordPair> counter = list.get(aspect);
      int printSize = maxPrintSize > counter.size() ? counter.size()
          : maxPrintSize;
      List<WordPair> orderedList = counter.keysOrderedByCountList();
      for (int idx = 0; idx < printSize; idx++) {
        WordPair pair = orderedList.get(idx);
        out.printf("%s = %d,&nbsp;&nbsp;&nbsp;", pair, counter.getCount(pair));
      }
      out.print("</span>");
      out.print("<br /><br />");
    }
  }

  /**
   * Prints aspect-based word pairs summary for a number of sample restaurants.
   * 
   * @param numRestaurants
   *          number of sample restaurants to print
   * @param minReviews
   *          minimum number of reviews for a sample restaurant
   * @param maxReviews
   *          maximum number of reviews for a sample restaurant
   * @param map
   *          a map between restaurant id and its reviews
   * @param out
   *          a writer to print out the summary
   */
  public void summarizeRestaurants(int numRestaurants, int minReviews,
      int maxReviews, HashMap<String, ArrayList<Review>> map, PrintWriter out) {
    int cnt = 0, pairsPerAspect = 50;
    List<Document> documents = model.getDocuments();
    int numTopics = model.getNumTopics();
    ArrayList<ObjectToCounterMap<WordPair>> restaurant = new ArrayList<ObjectToCounterMap<WordPair>>();
    for (Entry<String, ArrayList<Review>> entry : map.entrySet()) {
      String restaurantId = entry.getKey();
      HashSet<WordPair> filteredPairs = new HashSet<WordPair>();
      // HashSet<WordPair> filteredPairs = null;
      ArrayList<Review> reviews = entry.getValue();
      int numReviews = reviews.size();
      if (numReviews < maxReviews && numReviews >= minReviews) {
        restaurant.clear();
        for (int i = 0; i < numTopics; i++) {
          restaurant.add(new ObjectToCounterMap<WordPair>());
        }
        int firstId = findFirstReview(documents, restaurantId);
        if (firstId < 0)
          continue;
        for (int offset = 0; offset < numReviews; offset++) {
          addByElements(
              restaurant,
              reviewToWordPairs(filteredPairs, documents.get(firstId + offset),
                  reviews.get(offset), numTopics));
        }
        out.printf("<h4>Restaurant %s (#reviews = %d)</h4>", restaurantId,
            numReviews);
        printWordPairsByAspects(out, pairsPerAspect, restaurant);
        out.printf("<br /><b>Filtered pairs:</b>&nbsp;%s<br />", filteredPairs);
        if (cnt++ > numRestaurants) {
          break;
        }
      }
    }
  }

  /**
   * Adds the second list <code>list2</code> to the first list
   * <code>list1</code> element-wise.
   * <p>
   * Since each element of the list is a counter, element-wise addition simply
   * means aggregating two counters into one.
   * 
   * @param <T>
   * @param list1
   * @param list2
   */
  private <T> void addByElements(ArrayList<ObjectToCounterMap<T>> list1,
      ArrayList<ObjectToCounterMap<T>> list2) {
    for (int idx = 0; idx < list1.size(); idx++) {
      ObjectToCounterMap<T> counter1 = list1.get(idx);
      ObjectToCounterMap<T> counter2 = list2.get(idx);
      for (T element : counter2.keySet()) {
        counter1.increment(element, counter2.getCount(element));
      }
    }
  }

  /**
   * Returns the first review (document) of a the restaurant with given id
   * <code>restaurantId</code>.
   * 
   * @param documents
   * @param restaurantId
   * @return index of the first review
   */
  private int findFirstReview(List<Document> documents, String restaurantId) {
    for (int idx = 0; idx < documents.size(); idx++) {
      if (documents.get(idx).getRestaurantId().equals(restaurantId)) {
        return idx;
      }
    }

    return -1;
  }

  /**
   * Returns the word pairs of a review after the filtering process.
   * 
   * @param filteredSet
   *          a set to store the word pairs that are filtered out (removed);
   *          <code>null</code> to abort filtering
   * @param document
   * @param review
   * @param numTopics
   * @return a list; each element is a counter of pairs with assigned aspect
   *         corresponding to the index
   */
  private ArrayList<ObjectToCounterMap<WordPair>> reviewToWordPairs(
      HashSet<WordPair> filteredSet, Document document, Review review,
      int numTopics) {
    ArrayList<ObjectToCounterMap<WordPair>> list = new ArrayList<ObjectToCounterMap<WordPair>>();
    for (int i = 0; i < numTopics; i++) {
      list.add(new ObjectToCounterMap<WordPair>());
    }
    List<ArrayList<? extends HasWord>> tSentences = DocumentUtils
        .tokenizeSentences(DocumentUtils.negate(review.getContent())
            .replaceAll("not_", "not "));
    for (ArrayList<? extends HasWord> tSentence : tSentences) {
      Sentence bsSentence = findSentence(document, tSentence);
      if (bsSentence != null && bsSentence.getSenti() >= 0) {
        ArrayList<WordPair> wordPairs = getWordPairs(tagger
            .tagSentence(tSentence));
        // if (filteredSet != null) {
        // wordPairs = filterWordPairs(filteredSet, wordPairs, bsSentence);
        // }
        // for (WordPair pair : wordPairs) {
        // list.get(bsSentence.getTopic()).increment(pair);
        // }
        for (WordPair pair : wordPairs) {
          // int classifiedTopic = classifyWordPair(pair,
          // bsSentence.getSenti());
          int classifiedTopic = classifyWordPair(pair);
          if (classifiedTopic >= 0) {
            list.get(classifiedTopic).increment(pair);
          } else {
            if (filteredSet != null) {
              filteredSet.add(pair);
            }
          }
        }
      }
    }

    return list;
  }

  /**
   * Returns the classified topic of the given word pair <code>pair</code>.
   * TODO(trung): this classifier uses the sentiment of word pair as that of the
   * sentence.
   * 
   * @param pair
   * @return
   */
  private int classifyWordPair(WordPair pair, int senti) {
    double maxProb = -1.0;
    int maxTopic = -1;
    int sentiWord = sentiTable.symbolToID(pair.stemAdj);
    int aspectWord = aspectTable.symbolToID(pair.stemNoun);
    if (sentiWord >= 0 && aspectWord >= 0) {
      for (int topic = 0; topic < model.getNumTopics(); topic++) {
        if (isInArray(aspectWords[topic], pair.stemNoun)
            && isInArray(sentimentWords[senti][topic], pair.stemAdj)) {
          double prob = phiSenti[senti].getValue(sentiWord, topic)
              * phiAspect[topic][aspectWord];
          if (maxProb < prob) {
            maxProb = prob;
            maxTopic = topic;
          }
        }
      }
    }

    return maxTopic;
  }

  /**
   * Returns the classified topic of the given word pair <code>pair</code>.
   * 
   * @param pair
   * @return
   */
  private int classifyWordPair(WordPair pair) {
    double maxProb = -1.0;
    int maxTopic = -1;
    int sentiWord = sentiTable.symbolToID(pair.stemAdj);
    int aspectWord = aspectTable.symbolToID(pair.stemNoun);
    if (sentiWord < 0 || aspectWord < 0) {
      return maxTopic;
    }

    for (int topic = 0; topic < model.getNumTopics(); topic++) {
      if (isInArray(aspectWords[topic], pair.stemNoun)) {
        for (int senti = 0; senti < model.getNumSentiments(); senti++) {
          if (isInArray(sentimentWords[senti][topic], pair.stemAdj)) {
            double prob = phiSenti[senti].getValue(sentiWord, topic)
                * phiAspect[topic][aspectWord];
            if (maxProb < prob) {
              maxProb = prob;
              maxTopic = topic;
            }
          }
        }
      }
    }

    return maxTopic;
  }

  /**
   * Filters the <code>wordPairs</code> of the given sentence
   * <code>bsSentence</code> using the trained model.
   * <p>
   * This method returns a the subset of <code>wordPairs</code>. Its elements
   * are pairs whose both sentiment and aspect word are in the top sentiment and
   * aspect words of the topic assigned to <code>bsSentence</code>.
   * 
   * @param wordPairs
   * @param bsSentence
   * @return the filtered list of word pairs
   */
  private ArrayList<WordPair> filterWordPairs(HashSet<WordPair> filteredSet,
      ArrayList<WordPair> wordPairs, Sentence bsSentence) {
    ArrayList<WordPair> ret = new ArrayList<WordPair>();
    int senti = bsSentence.getSenti();
    int topic = bsSentence.getTopic();
    for (WordPair pair : wordPairs) {
      if (isInArray(aspectWords[topic], pair.stemNoun)) {
        // && isInArray(sentimentWords[senti][topic], pair.stemAdj)) {
        ret.add(pair);
      } else {
        if (filteredSet != null) {
          filteredSet.add(pair);
        }
      }
    }

    return ret;
  }

  /**
   * Returns true if <code>element</code> is in <code>array</code>.
   * 
   * @param array
   * @param value
   * @return
   */
  private boolean isInArray(String[] array, String value) {
    for (String element : array) {
      if (element.equals(value)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Finds the corresponding sentence of the tokenized sentence
   * <code>tSentence</code> in <code>document</code>.
   * 
   * @param document
   * @param tSentence
   * @return
   */
  private Sentence findSentence(Document document,
      ArrayList<? extends HasWord> tSentence) {
    String sentenceTxt = DocumentUtils.tokenizedSentenceToText(tSentence);
    for (Sentence sentence : document.getSentences()) {
      if (sentence.getText().equals(sentenceTxt)) {
        return sentence;
      }
    }

    return null;
  }

  /**
   * Gets the review with specified <code>id</code>.
   * 
   * @param list
   * @param id
   * @return
   */
  private Review getReview(ArrayList<Review> list, String id) {
    for (Review review : list) {
      if (review.getReviewId().equals(id)) {
        return review;
      }
    }

    return null;
  }

  /**
   * Aggregates content of all reviews into one string.
   * 
   * @param reviews
   * @return
   */
  static String aggregateReviews(ArrayList<Review> reviews) {
    StringBuilder builder = new StringBuilder();
    for (Review review : reviews) {
      builder.append(review.getContent()).append(" ");
    }

    return builder.toString();
  }

  /**
   * A word pair is a pair of adjective and noun.
   */
  static final class WordPair {
    static final EnglishStemmer stemmer = new EnglishStemmer();
    String adj;
    String noun;
    String stemAdj;
    String stemNoun;

    public WordPair(String adj, String noun) {
      this.adj = adj;
      this.noun = noun;
      stemAdj = stemmer.getStem(adj.toLowerCase());
      stemNoun = stemmer.getStem(noun.toLowerCase());
    }

    @Override
    public boolean equals(Object o) {
      WordPair that = (WordPair) o;
      return (stemAdj.equals(that.stemAdj) && stemNoun.equals(that.stemNoun));
    }

    @Override
    public int hashCode() {
      return (stemAdj + stemNoun).hashCode();
    }

    @Override
    public String toString() {
      return "(" + adj + " " + noun + ")";
    }
  }

  public static void main(String args[]) throws Exception {
    // String dir =
    // "C:/datasets/models/bs/ursa/T7-A0.1-B0.0010-G0.10,0.10-I1000/1000";
    // String dir =
    // "C:/datasets/models/bs/movies/T7-A0.1-B0.0010-G1.00,1.00-I1000(senti1)/1000";
    String dir = "C:/datasets/models/bs/electronics/T7-A0.1-B0.0010-G1.00,1.00-I1000(senti1)/1000";
    System.out.println("Loading model at " + dir);
    Model model = Model.loadModel(dir + "/model.gz");
    System.out.println("Reading reviews");
    // UrsaDataset ursa = new UrsaDataset();
    // HashMap<String, ArrayList<Review>> map = ursa.getReviews();
    HashMap<String, ArrayList<Review>> map = readReviews("C:/datasets/models/bs/electronics/docs.txt");
    WordPairsAnalysis wp = new WordPairsAnalysis(model, 200);
    int numSamples = 20;
    // wp.summarizeByWordPairs(model, map, numSamples, dir + "/wordpairs");
    // wp.summarizeReviews(map, numSamples, dir + "/reviewByWordpairs");
    PrintWriter out = new PrintWriter(dir
        + "/summaryTargets200IgnoreSenti.html");
    out.println("<html><title>Summary of reviews</title><body>");
    out.println("<h2 style='color:red'>Restaurants with 0 - 20 reviews</h2>");
    wp.summarizeRestaurants(numSamples, 0, 20, map, out);
    out.println("<h2 style='color:red'>Restaurants with 20 - 50 reviews</h2>");
    wp.summarizeRestaurants(numSamples, 20, 50, map, out);
    out.println("<h2 style='color:red'>Restaurants with 50 - 100 reviews</h2>");
    wp.summarizeRestaurants(numSamples, 50, 100, map, out);
    out.println("<h2 style='color:red'>Restaurants with 100 - 500 reviews</h2>");
    wp.summarizeRestaurants(numSamples, 100, 500, map, out);
    out.println("</body></html>");
    out.close();
    // numSamples = 10;
    // experimentWithTF(wp, map, dir);
  }

  static void experimentWithTF(WordPairsAnalysis wp,
      HashMap<String, ArrayList<Review>> map, String dir) throws IOException {
    int numSamples = 10;
    // ArrayList<String> documents = new ArrayList<String>();
    // for (ArrayList<Review> reviews : map.values()) {
    // documents.add(aggregateReviews(reviews));
    // }
    // System.out.println("Computing document frequency");
    // ObjectToCounterMap<WordPair> df = wp.getDocumentFrequency(documents);
    PrintWriter out = new PrintWriter(dir + "/summaryRestaurantsByTF.html");
    out.println("<html><title>Summary of reviews by term frequency</title><body>");
    out.print("<h2>Restaurants with 0 - 20 reviews</h2>");
    wp.printTopWordPairs(out, map, null, 0, 20, numSamples);
    out.print("<h2>Restaurants with 20 - 50 reviews</h2>");
    wp.printTopWordPairs(out, map, null, 20, 50, numSamples);
    out.print("<h2>Restaurants with 50 - 100 reviews</h2>");
    wp.printTopWordPairs(out, map, null, 50, 100, numSamples);
    out.print("<h2>Restaurants with 100 - 500 reviews</h2>");
    wp.printTopWordPairs(out, map, null, 100, 500, numSamples);
    out.print("<h2>Restaurants with 500 - 1000 reviews</h2>");
    wp.printTopWordPairs(out, map, null, 500, 1000, numSamples);
    out.print("</body></html>");
    out.close();
  }

  static void testWithWordPairs(String dir) throws IOException {
    PrintWriter out = new PrintWriter(dir + "/summaryByTF.html");
    out.println("<html><title>Summary of reviews by term frequency</title><body>");
    WordPairsAnalysis wp = new WordPairsAnalysis();
    int numSamples = 20;
    HashMap<String, ArrayList<Review>> map = readReviews(dir + "/docs.txt");
    out.print("<h2>Review targets with 0 - 20 reviews</h2>");
    wp.printTopWordPairs(out, map, null, 0, 20, numSamples);
    out.print("<h2>Review targets with 20 - 50 reviews</h2>");
    wp.printTopWordPairs(out, map, null, 20, 50, numSamples);
    out.print("<h2>Review targets with 50 - 100 reviews</h2>");
    wp.printTopWordPairs(out, map, null, 50, 100, numSamples);
    out.print("<h2>Review targets with 100 - 500 reviews</h2>");
    wp.printTopWordPairs(out, map, null, 100, 500, numSamples);
    out.print("<h2>Review targets with 500 - 1000 reviews</h2>");
    wp.printTopWordPairs(out, map, null, 500, 1000, numSamples);
    out.print("<h2>Review targets with over 1000 reviews</h2>");
    wp.printTopWordPairs(out, map, null, 1000, 10000, numSamples);
    out.print("</body></html>");
    out.close();
  }

  /**
   * Reads reviews of the review targets for the given corpus.
   * 
   * @throws IOException
   */
  public static HashMap<String, ArrayList<Review>> readReviews(String corpus)
      throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(corpus), "utf-8"));
    String line;
    double rating;
    HashMap<String, ArrayList<Review>> map = new HashMap<String, ArrayList<Review>>();
    ArrayList<Review> list = new ArrayList<Review>();
    while ((line = in.readLine()) != null) {
      String[] ids = line.split(" ");
      try {
        rating = Double.parseDouble(in.readLine());
      } catch (NumberFormatException e) {
        rating = -1.0;
      }
      if (map.containsKey(ids[1])) {
        list = map.get(ids[1]);
      } else {
        list = new ArrayList<Review>();
        map.put(ids[1], list);
      }
      list.add(new Review(ids[0], ids[1], rating, in.readLine()));
    }
    in.close();

    return map;
  }
}
