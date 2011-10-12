package edu.kaist.uilab.bs.evaluation;

import java.util.ArrayList;
import java.util.List;

import com.aliasi.util.ObjectToDoubleMap;

import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.asc.util.SentiWordNet;
import edu.kaist.uilab.asum.AsumModel.AsumModelData;
import edu.kaist.uilab.asum.JstModel.JstModelData;
import edu.kaist.uilab.bs.Model;

/**
 * Various segment filters.
 * 
 * @author trung
 */
public class SegmentFilters {
  static final int TOP_ASPECT_WORDS = 200;
  static final int TOP_SENTI_WORDS = 100;

  public enum Filter {
    NO_FILTER, SWN, ASPECT, ASPECT_SWN, ASPECT_SENTIMENT, BOTH_SENTIMENT, RANKED_ASPECT_SENTIMENT, ASUM, JST,
  };

  SentiWordNet swn = new SentiWordNet();
  Model model;
  DoubleMatrix[] phiSenti;
  double[][] phiAspect;
  String[][] topAspectWords;
  String[][][] topSentiWords;
  AsumModelData asum;
  JstModelData jst;
  ObjectToDoubleMap<String>[][] asumPhi;
  String[][][] asumTopWords;
  ObjectToDoubleMap<String>[][] jstPhi;
  String[][][] jstTopWords;
  ArrayList<String[]>[][] summary;

  /**
   * Constructs a segment filter using the BS model.
   * 
   * @param model
   */
  @SuppressWarnings("unchecked")
  public SegmentFilters(Model model) {
    this.model = model;
    phiSenti = model.getPhiSentiByTermscore();
    phiAspect = model.getPhiAspectByTermscore();
    topAspectWords = model.getTopAspectWords(TOP_ASPECT_WORDS);
    topSentiWords = model.getTopSentiWords(TOP_SENTI_WORDS);
    summary = new ArrayList[model.getNumTopics()][model.getNumSentiments()];
  }

  /**
   * Constructs a segment filter using jst or asum model.
   */
  public SegmentFilters(AsumModelData asum, JstModelData jst) {
    this.asum = asum;
    asumPhi = asum.phiIndexedByWord();
    this.jst = jst;
    asumTopWords = asum.topWords(asumPhi, TOP_ASPECT_WORDS);
    jstPhi = jst.phiIndexedByWord();
    jstTopWords = jst.topWords(jstPhi, TOP_ASPECT_WORDS);
  }

  /**
   * Filters the candidate segments using the given filter.
   * 
   * @param filter
   * @param segments
   * @param sentiment
   * @return
   */
  public List<String[]> filterSegments(Filter filter, List<String[]> segments,
      int sentiment) {
    switch (filter) {
    case NO_FILTER:
      return segments;
    case SWN:
      return swnFilter(segments, sentiment);
    case ASPECT:
      return aspectWordsFilter(segments);
    case ASPECT_SWN:
      return aspectWordsSwnFilter(segments, sentiment);
    case ASPECT_SENTIMENT:
      return aspectWordsWithSentiFilter(segments, sentiment);
    case BOTH_SENTIMENT:
      return aspectWordsWithSentiWordsFilter(segments, sentiment);
    case RANKED_ASPECT_SENTIMENT:
      return rankedAspectWordsWithSentiFilter(segments, sentiment, 0.5);
    case ASUM:
      return asumFilter(segments, sentiment);
    case JST:
      return jstFilter(segments, sentiment);
    default:
      return null;
    }
  }

  /**
   * Returns the list of extracted segments for each senti-aspect.
   * <p>
   * Currently only applicable when {@link Filter.BOTH_SENTIMENT} is used.
   * 
   * @return
   */
  public ArrayList<String[]>[][] getExtractedSegments() {
    return summary;
  }

  /**
   * Returns all elements in <code>segments</code> that is classified as the
   * same sentiment with <code>s</code> using the SWN classifier.
   * 
   * @param segments
   * @param s
   * @return
   */
  private ArrayList<String[]> swnFilter(List<String[]> segments, int s) {
    ArrayList<String[]> ret = new ArrayList<String[]>();
    for (String[] segment : segments) {
      int sentiment = swn.classifySegmentSentiment(segment);
      if (sentiment == s) {
        ret.add(segment);
      }
    }

    return ret;
  }

  /**
   * Returns all elements in <code>segments</code> that (i) can be classified
   * into one of the aspect, and (ii) contains at least one word in the top
   * words of the classified aspect.
   * 
   * @param segments
   * @return
   */
  private ArrayList<String[]> aspectWordsFilter(List<String[]> segments) {
    ArrayList<String[]> ret = new ArrayList<String[]>();
    for (String[] segment : segments) {
      int k = model.classifySegmentTopic(phiAspect, phiSenti, segment);
      if (k >= 0 && model.segmentContainsTopWords(topAspectWords[k], segment)) {
        ret.add(segment);
      }
    }

    return ret;
  }

  /**
   * Returns all elements in <code>segments</code> that satisfy the following
   * conditions.
   * <ul>
   * <li>can be classified into one of the aspects</li>
   * <li>contain at least one word in the top words of the classified aspect</li>
   * <li>has classified sentiment equals to the specified sentiment
   * <code>s</code></li>
   * </ul>
   * <p>
   * This is a more restricted filter than {@link #aspectWordsFilter(List)} in
   * that it only keeps the segments that can be classified as having the
   * specified sentiment.
   * 
   * @param segments
   * @return
   */
  private ArrayList<String[]> aspectWordsWithSentiFilter(
      List<String[]> segments, int s) {
    ArrayList<String[]> ret = new ArrayList<String[]>();
    for (String[] segment : segments) {
      int k = model.classifySegmentTopic(phiAspect, phiSenti, segment);
      if (k >= 0 && model.segmentContainsTopWords(topAspectWords[k], segment)) {
        int sentiment = model.classifySegmentSentiment(phiSenti, segment, k);
        if (sentiment == s) {
          ret.add(segment);
        }
      }
    }

    return ret;
  }

  /**
   * Similar to the {@link #aspectWordsWithSentiFilter(List, int)} but use the
   * ASUM model as a filter.
   * 
   * @param segments
   * @return
   */
  private ArrayList<String[]> asumFilter(List<String[]> segments, int s) {
    ArrayList<String[]> ret = new ArrayList<String[]>();
    for (String[] segment : segments) {
      int[] val = asum.classifySegment(asumPhi, segment);
      if (val[0] == s && val[1] >= 0
          && asum.segmentContainsTopWords(asumTopWords[s][val[1]], segment)) {
        ret.add(segment);
      }
    }

    return ret;
  }

  /**
   * Similar to the {@link #aspectWordsWithSentiFilter(List, int)} but use the
   * JST model as a filter.
   * 
   * @param segments
   * @return
   */
  private ArrayList<String[]> jstFilter(List<String[]> segments, int s) {
    ArrayList<String[]> ret = new ArrayList<String[]>();
    for (String[] segment : segments) {
      int[] val = jst.classifySegment(jstPhi, segment);
      if (val[0] == s && val[1] >= 0
          && jst.segmentContainsTopWords(jstTopWords[s][val[1]], segment)) {
        ret.add(segment);
      }
    }

    return ret;
  }

  /**
   * Same as {@link #aspectWordsWithSentiFilter(List, int)} but with additional
   * filter using top sentiment words.
   * 
   * @param segments
   * @param s
   * @return
   */
  private ArrayList<String[]> aspectWordsWithSentiWordsFilter(
      List<String[]> segments, int s) {
    ArrayList<String[]> ret = new ArrayList<String[]>();
    for (String[] segment : segments) {
      int k = model.classifySegmentTopic(phiAspect, phiSenti, segment);
      if (k >= 0 && model.segmentContainsTopWords(topAspectWords[k], segment)) {
        int sentiment = model.classifySegmentSentiment(phiSenti, segment, k);
        if (sentiment == s
            && model.segmentContainsTopWords(topSentiWords[s][k], segment)) {
          ret.add(segment);
//          summary[k][sentiment].add(segment);
        }
      }
    }

    return ret;
  }

  /**
   * Returns all elements in <code>segments</code> that satisfy the following
   * conditions.
   * <ul>
   * <li>can be classified into one of the aspects</li>
   * <li>contain at least one word in the top words of the classified aspect</li>
   * <li>has classified sentiment equals to the specified sentiment
   * <code>s</code></li>
   * </ul>
   * <p>
   * This is a similar to {@link #aspectWordsWithSentiFilter(List, int)} except
   * that it uses SWN to classify sentiment rather than using the model
   * classifer.
   * 
   * @param segments
   * @param s
   * @return
   */
  private ArrayList<String[]> aspectWordsSwnFilter(List<String[]> segments,
      int s) {
    ArrayList<String[]> ret = new ArrayList<String[]>();
    for (String[] segment : segments) {
      int k = model.classifySegmentTopic(phiAspect, phiSenti, segment);
      if (k >= 0 && model.segmentContainsTopWords(topAspectWords[k], segment)) {
        int sentiment = swn.classifySegmentSentiment(segment);
        if (sentiment == s) {
          ret.add(segment);
        }
      }
    }

    return ret;
  }

  /**
   * Returns all elements in <code>segments</code> that satisfy the following
   * conditions.
   * <ul>
   * <li>can be classified into one of the aspects</li>
   * <li>contain at least one word in the top words of the classified aspect</li>
   * <li>has classified sentiment equals to the specified sentiment
   * <code>s</code></li>
   * <li>ranks in the top <code>percent</code> percent of all segments
   * classified into the same aspect and sentiment category.
   * </ul>
   * <p>
   * This is a more restricted filter than
   * {@link #aspectWordsWithSentiFilter(List, int)} in that it only takes the
   * top probability segments
   * 
   * @param segments
   * @param s
   * @param percent
   * @return
   */
  private ArrayList<String[]> rankedAspectWordsWithSentiFilter(
      List<String[]> segments, int s, double percent) {
    int numAspects = model.getNumTopics();
    @SuppressWarnings("unchecked")
    ObjectToDoubleMap<String[]>[] aspects = new ObjectToDoubleMap[numAspects];
    for (int i = 0; i < numAspects; i++) {
      aspects[i] = new ObjectToDoubleMap<String[]>();
    }
    for (String[] segment : segments) {
      int k = model.classifySegmentTopic(phiAspect, phiSenti, segment);
      if (k >= 0 && model.segmentContainsTopWords(topAspectWords[k], segment)) {
        int sentiment = model.classifySegmentSentiment(phiSenti, segment, k);
        if (sentiment == s) {
          aspects[k].increment(
              segment,
              model.getSegmentProb(phiAspect, segment, k)
                  * model.getSentimentProb(phiSenti, segment, k, sentiment));
        }
      }
    }

    ArrayList<String[]> list = new ArrayList<String[]>();
    for (int i = 0; i < numAspects; i++) {
      int size = aspects[i].size();
      list.addAll(aspects[i].keysOrderedByValueList().subList(0,
          (int) (size * percent)));
    }

    return list;
  }

}
