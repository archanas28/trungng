package edu.kaist.uilab.bs.evaluation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import com.aliasi.util.ObjectToCounterMap;

import edu.kaist.uilab.asc.data.Review;
import edu.kaist.uilab.asc.data.ReviewReader;
import edu.kaist.uilab.asc.data.ReviewWithProsAndConsReader;
import edu.kaist.uilab.bs.Document;
import edu.kaist.uilab.bs.MaxentTaggerSingleton;
import edu.kaist.uilab.bs.Sentence;
import edu.kaist.uilab.bs.util.BSUtils;
import edu.kaist.uilab.bs.util.DocumentUtils;
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
    List<ArrayList<? extends HasWord>> tokenizedSentences = DocumentUtils
        .tokenizeSentences(
            DocumentUtils.negate(content).replaceAll("not_", "not "), false);
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
   * TODO(trung): use this method for getting adj-noun pairs.
   */
  public void printTopWordPairs(PrintWriter out,
      HashMap<String, ArrayList<Review>> map, ObjectToCounterMap<WordPair> df,
      int minReviews, int maxReviews, int numRestaurants) {
    int cnt = 0;
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
    // String dir =
    // "C:/datasets/models/bs/electronics/T7-A0.1-B0.0010-G1.00,1.00-I1000(senti1)/1000";
    String dir = "C:/datasets/models/bs/vacuum/T10-A0.1-B0.0010-G0.10,0.10-I1000()/1000";
    System.out.println("Reading reviews");
    // UrsaDataset ursa = new UrsaDataset();
    // HashMap<String, ArrayList<Review>> map = ursa.getReviews();
    ReviewReader reader = new ReviewWithProsAndConsReader();
    HashMap<String, ArrayList<Review>> map = BSUtils.readReviews(
        "C:/datasets/models/bs/vacuum/docs.txt", reader);
    WordPairsAnalysis wp = new WordPairsAnalysis();
    wp.experimentWithTF(wp, map, dir);
  }

  public void experimentWithTF(WordPairsAnalysis wp,
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
    ReviewReader reader = new ReviewReader();
    HashMap<String, ArrayList<Review>> map = BSUtils.readReviews(dir
        + "/docs.txt", reader);
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
}
