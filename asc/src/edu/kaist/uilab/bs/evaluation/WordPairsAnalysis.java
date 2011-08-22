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

import com.aliasi.util.Counter;
import com.aliasi.util.ObjectToCounterMap;

import edu.kaist.uilab.asc.data.Review;
import edu.kaist.uilab.bs.BSModel;
import edu.kaist.uilab.bs.Document;
import edu.kaist.uilab.bs.DocumentUtils;
import edu.kaist.uilab.bs.MaxentTaggerSingleton;
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
 * <li>Collapse all reviews of a restaurant as a document, i.e., all reviews are
 * equal.
 * <li>Using term frequency to find out the top word pairs for a document (all
 * reviews of a restaurant).
 * <li>Using inverse frequency to find out the top word pairs for a document
 * based on the freq of all reviews.
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
   * Prints out the top word pairs of restaurant whose number of reviews is
   * between the <code>(minReviews, maxReviews)</code> range.
   */
  public void printTopWordPairs(HashMap<String, ArrayList<Review>> map,
      ObjectToCounterMap<WordPair> df, int minReviews, int maxReviews,
      int numRestaurants) {
    int cnt = 0;
    int numDocuments = map.size();
    for (Entry<String, ArrayList<Review>> entry : map.entrySet()) {
      ArrayList<Review> reviews = entry.getValue();
      int numReviews = reviews.size();
      if (numReviews < maxReviews && numReviews >= minReviews) {
        String allReviews = aggregateReviews(reviews);
        ObjectToCounterMap<WordPair> tf = getTermFrequency(allReviews);
        ObjectToCounterMap<WordPair> tfidf = new ObjectToCounterMap<WordPair>();
        for (WordPair pair : tf.keySet()) {
          double idf = Math.log(((double) numDocuments) / df.getCount(pair));
          tfidf.put(pair, new Counter((int) (tf.getCount(pair) * idf)));
        }
        int numPairs = tf.size();
        System.out.printf("Restaurant %s (#reviews = %d, #pairs: %d)\n",
            entry.getKey(), numReviews, numPairs);
        System.err.print("TF\t");
        printTopWordPairs(tf, numPairs);
        System.err.print("TF-IDF\t");
        printTopWordPairs(tfidf, numPairs);
        if (cnt++ > numRestaurants) {
          break;
        }
      }
    }
  }

  private void printTopWordPairs(ObjectToCounterMap<WordPair> score,
      int numPairs) {
    int print = numPairs > 50 ? 50 : numPairs;
    List<WordPair> orderedList = score.keysOrderedByCountList();
    for (int idx = 0; idx < print; idx++) {
      WordPair pair = orderedList.get(idx);
      System.out.printf("%s = %d; ", pair, score.getCount(pair));
    }
    System.out.println();
  }

  /**
   * Summarize reviews trained in the model by word pairs.
   * <p>
   * The method for extracting word pairs from a review is as follows.
   * <ul>
   * <li>First, extract the word pairs of a sentence as in normal approach,
   * i.e., an adjective and a noun forms a candidate pair</li>
   * <li>Then, filter these pairs by retaining only pairs whose both of the
   * sentiment and aspect word are in the top words of the inferred aspect of
   * the sentence.
   * </ul>
   * 
   * @param model
   */
  public void summarizeByWordPairs(BSModel model,
      HashMap<String, ArrayList<Review>> map, int numSamples, String output)
      throws IOException {
    int numTopWords = 100;
    PrintWriter out = new PrintWriter(output + numTopWords + ".html");
    out.println("<html><title>Summary</title><body>");
    String[] sentiColors = { "green", "red" };
    String[][] aspectWords = model.getTopAspectWords(numTopWords);
    String[][][] sentimentWords = model.getTopSentiWords(numTopWords);
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
          wordPairs = filterWordPairs(aspectWords, sentimentWords, wordPairs,
              bsSentence);
          out.printf("&nbsp;&nbsp;&nbsp;&nbsp;\t%s<br />", wordPairs);
        }
      }
    }
    out.println("</body></html>");
    out.close();
  }

  /**
   * Filters the <code>wordPairs</code> of the given sentence
   * <code>bsSentence</code> using the trained model <code>model</code>.
   * <p>
   * This retains in <code>wordPairs</code> only the pair whose both words are
   * in the top sentiment and aspect words of the topic assigned to
   * <code>bsSentence</code>.
   * 
   * @param model
   * @param wordPairs
   * @param bsSentence
   * @return the filtered list of word pairs
   */
  private ArrayList<WordPair> filterWordPairs(String[][] aspectWords,
      String[][][] sentimentWords, ArrayList<WordPair> wordPairs,
      Sentence bsSentence) {
    ArrayList<WordPair> ret = new ArrayList<WordPair>();
    int senti = bsSentence.getSenti();
    int topic = bsSentence.getTopic();
    for (WordPair pair : wordPairs) {
      if (isInArray(aspectWords[topic], pair.stemNoun)
          && isInArray(sentimentWords[senti][topic], pair.stemAdj)) {
        ret.add(pair);
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
      return "(" + adj + ", " + noun + ")";
    }
  }

  public static void main(String args[]) throws Exception {
    WordPairsAnalysis wp = new WordPairsAnalysis();
    UrsaDataset ursa = new UrsaDataset();
    System.out.println("Reading reviews");
    HashMap<String, ArrayList<Review>> map = ursa.getReviews();
    String dir = "C:/datasets/bs/ursa/T2-A0.1-B0.0010-G0.10,0.10-I1000(improvedParser)/1000";
    System.out.println("Loading model at " + dir);
    BSModel model = BSModel.loadModel(dir + "/model.gz");
    wp.summarizeByWordPairs(model, map, 200, dir + "/wordpairs");

    // ArrayList<String> documents = new ArrayList<String>();
    // for (ArrayList<Review> reviews : map.values()) {
    // documents.add(aggregateReviews(reviews));
    // }
    // System.out.println("Computing document frequency");
    // ObjectToCounterMap<WordPair> df = wp.getDocumentFrequency(documents);
    // System.err.println("Restaurants with 0 - 20 reviews");
    // wp.printTopWordPairs(map, df, 0, 20, 5);
    // System.err.println("Restaurants with 20 - 50 reviews");
    // wp.printTopWordPairs(map, df, 20, 50, 5);
    // System.err.println("Restaurants with 50 - 100 reviews");
    // wp.printTopWordPairs(map, df, 50, 100, 5);
    // System.err.println("Restaurants with 100 - 500 reviews");
    // wp.printTopWordPairs(map, df, 100, 500, 5);
    // System.err.println("Restaurants with 500 - 1000 reviews");
    // wp.printTopWordPairs(map, df, 500, 1000, 5);
  }
}
