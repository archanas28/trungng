package edu.kaist.uilab.bs.evaluation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import edu.kaist.uilab.asc.data.Review;
import edu.kaist.uilab.asc.data.ReviewWithProsAndCons;
import edu.kaist.uilab.asc.data.ReviewWithProsAndConsReader;
import edu.kaist.uilab.bs.Model;
import edu.kaist.uilab.bs.evaluation.SegmentExtractor.Pattern;
import edu.kaist.uilab.bs.opt.OptimizationModel;
import edu.kaist.uilab.bs.util.BSUtils;

/**
 * Produces summary for the user study. TODO we replace n't with NOT in reviews
 * so that it can be matched with patterns
 * 
 * @author trung
 */
public class SummaryProducer {

  // min #reviews for an entity in the second user study
  public static final int MIN_REVIEWS = 10;
  // #reviews per entity to use in the second user study
  public static final int REVIEWS_PER_ENTITY = 6;

  public static final String SENTENCE_DELIMITER_REGEX = "[.!?]";

  static Random rnd = new Random(123456);
  SummarizationAnalyzer analyzer;
  WordPairSummarizer wpSummarizer;

  /**
   * Constructor
   * 
   * @param analyzer
   */
  public SummaryProducer(SummarizationAnalyzer analyzer) {
    this.analyzer = analyzer;
    wpSummarizer = new WordPairSummarizer();
  }

  /**
   * Prints summaries to the given file.
   * <p>
   * We select a small number of reviews so that each extracted segment can be
   * rated by multiple raters.
   * 
   * @param outfile
   * @param minSegment
   * @param numWordPairs
   */
  public void printSummaries(String outfile, int minSegment, int numWordPairs)
      throws IOException {
    // NOTE: currently best setting is: use 6 reviews / entity; aspect filters
    // (because we don't classify into pros and cons); 200 aspect words (or
    // 300?)
    List<Summary> list = analyzer.getSummaries();
    PrintWriter out = new PrintWriter(outfile);
    int cnt = 0;
    for (Summary summary : list) {
      if (summary.getNumSegments() >= minSegment) {
        summary.addWordPairs(wpSummarizer.getTopWordPairs(summary.getContent(),
            numWordPairs));
        cnt++;
        out.println(summary);
      }
    }
    out.close();
    System.out.println("#summaries: " + cnt);
  }

  public static void main(String args[]) throws IOException {
    String bs = "C:/datasets/models/bs/ursa/optimization/T7-A0.1-B0.0100-G0.10,0.10-I1000()";
    Pattern[] patterns = SegmentExtractor.SERVICE_PATTERNS;
    int maxSegmentLength = 6; // use 6 for restaurants (or even less)
    HashMap<String, ArrayList<Review>> entities = BSUtils.readReviews(
        "C:/datasets/models/bs/ursa/docs.txt",
        new ReviewWithProsAndConsReader());
    Model bsModel = (OptimizationModel) BSUtils
        .loadModel(bs + "/1000/model.gz");
    SegmentFilters filters = new SegmentFilters(bsModel);
    entities = buildReviewsForFirstStudy(entities);
    SummarizationAnalyzer analyzer = new SummarizationAnalyzer(filters,
        entities, maxSegmentLength, patterns, bsModel.getNumTopics());
    SummaryProducer producer = new SummaryProducer(analyzer);
    producer.printSummaries(String.format("%s/summaries1.txt", bs), 3, 5);
  }

  /**
   * Builds reviews for the first user study.
   * <p>
   * We only select a small number of reviews per entity so that the number of
   * extracted segments is small. This way, each segment can be rated by
   * multiple human raters.
   * 
   * @param entities
   */
  static HashMap<String, ArrayList<Review>> buildReviewsForFirstStudy(
      HashMap<String, ArrayList<Review>> entities) {
    HashMap<String, ArrayList<Review>> newMap = new HashMap<String, ArrayList<Review>>();
    for (Entry<String, ArrayList<Review>> entry : entities.entrySet()) {
      int numReviews = entry.getValue().size();
      int[] ids;
      if (numReviews > 5) {
        ids = new int[] { 0, numReviews / 2, numReviews - 1 };
      } else {
        ids = new int[] { 0, numReviews - 1 };
      }
      for (int idx : ids) {
        // each review is treated separately
        ArrayList<Review> newList = new ArrayList<Review>();
        ReviewWithProsAndCons review = (ReviewWithProsAndCons) entry.getValue()
            .get(idx);
        newList.add(new ReviewWithProsAndCons(review.getReviewId(), review
            .getRestaurantId(), review.getRating(), review.getPros(), review
            .getCons(), getFewSentences(review.getContent())));
        newMap.put(entry.getKey() + idx, newList);
      }
    }

    return newMap;
  }

  /**
   * Build reviews for the second user study.
   * <p>
   * We only use entities whose number of reviews is significant so that we can
   * get enough summary for its features.
   * 
   * @param entities
   */
  static HashMap<String, ArrayList<Review>> buildReviewsForSecondStudy(
      HashMap<String, ArrayList<Review>> entities) {
    HashMap<String, ArrayList<Review>> newMap = new HashMap<String, ArrayList<Review>>();
    for (Entry<String, ArrayList<Review>> entry : entities.entrySet()) {
      ArrayList<Review> myReviews = entry.getValue();
      if (myReviews.size() < MIN_REVIEWS)
        continue;
      String newContent = buildReviewContent(myReviews);
      if (newContent != null) {
        ArrayList<Review> newList = new ArrayList<Review>();
        newList.add(new ReviewWithProsAndCons("", entry.getKey(), -1.0, "", "",
            newContent));
        newMap.put(entry.getKey(), newList);
      }
    }

    return newMap;
  }

  /**
   * Builds review for an entity by randomly selecting its reviews.
   * 
   * @param list
   * @return
   */
  static String buildReviewContent(ArrayList<Review> list) {
    final int minSentences = 5;
    final int minSentenceLen = 15;
    final int maxTrials = 100;
    StringBuilder builder = new StringBuilder();
    HashSet<Integer> ids = new HashSet<Integer>();
    int trials = 0;
    do {
      int cand = rnd.nextInt(list.size());
      if (trials++ > maxTrials) {
        return null;
      }
      if (ids.contains(cand))
        continue;
      String content = list.get(cand).getContent();
      if (content.split(SENTENCE_DELIMITER_REGEX).length >= minSentences) {
//        String shortReview = getFewSentences(content);
        String shortReview = content;
        if (shortReview.length() >= minSentenceLen) {
          ids.add(cand);
          builder.append(shortReview).append("\n");
        }
      }
    } while (ids.size() < REVIEWS_PER_ENTITY);

    return builder.toString();
  }

  /**
   * Gets a few sentences from a given review content.
   * 
   * @param content
   * @return
   */
  static String getFewSentences(String content) {
    String[] sentences = content.split(SENTENCE_DELIMITER_REGEX);
    // do not summarize 'bad' reviews - reviews with too many sentences.
    if (sentences.length > 50) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    int start, numSentences;
    if (sentences.length < 6) {
      start = 0;
      numSentences = sentences.length;
    } else {
      numSentences = 4 + rnd.nextInt(3);
      start = rnd.nextInt(sentences.length - numSentences + 1);
    }
    for (int i = start; i < sentences.length; i++) {
      if (sentences[i].length() > 10 && sentences[i].length() < 150) {
        builder.append(sentences[i]).append(". ");
        numSentences--;
      }
      if (numSentences == 0) {
        break;
      }
    }

    return builder.toString().trim();
  }
}
