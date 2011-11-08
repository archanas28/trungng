package edu.kaist.uilab.bs.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.ObjectToCounterMap;

import edu.kaist.uilab.asc.data.Review;
import edu.kaist.uilab.asc.data.ReviewWithProsAndCons;
import edu.kaist.uilab.asc.data.ReviewWithProsAndConsReader;
import edu.kaist.uilab.asum.AsumModel.AsumModelData;
import edu.kaist.uilab.asum.JstModel.JstModelData;
import edu.kaist.uilab.bs.CorpusParserWithTagger.BSTokenizerFactory;
import edu.kaist.uilab.bs.MaxentTaggerSingleton;
import edu.kaist.uilab.bs.Model;
import edu.kaist.uilab.bs.evaluation.SegmentExtractor.Pattern;
import edu.kaist.uilab.bs.opt.OptimizationModel;
import edu.kaist.uilab.bs.util.BSUtils;
import edu.kaist.uilab.bs.util.DocumentUtils;
import edu.kaist.uilab.stemmers.EnglishStemmer;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Analysis for the summarization tasks. TODO(trung): remove duplicated
 * candidate summary
 * 
 * @author trung
 */
public class SummarizationAnalyzer {

  private static final int MAX_SUMMARY_LENGTH = 6;
  private static final int REFERENCE_THRESHOLD = 2;
  private static final double THRESHOLD = 0.2;
  private static final double EPSILON = 0.05;

  static final String PROSCONS_DELIMITERS = "[.,;]";
  static EnglishStemmer stemmer = new EnglishStemmer();
  static final String STOP_STEMS = "C:/datasets/models/bs/stop.txt";

  static TokenizerFactory tokenizer = BSTokenizerFactory
      .getInstance(STOP_STEMS);
  static MaxentTagger tagger = MaxentTaggerSingleton.getInstance();

  HashMap<String, ArrayList<Review>> entities;
  HashMap<String, ArrayList<String[]>> prosRefSummary;
  HashMap<String, ArrayList<String[]>> consRefSummary;
  HashMap<String, ArrayList<String[]>> candidateSummary;
  SegmentFilters filters;
  SegmentExtractor extractor = new SegmentExtractor();
  Rouger rouger = new Rouger();

  int numAspects;
  int maxSegmentLen;
  Pattern[] patterns;

  /**
   * Constructor
   * 
   * @param filters
   * @param entities
   * @param maxSegmentLength
   * @param patterns
   * @param numAspects
   */
  public SummarizationAnalyzer(SegmentFilters filters,
      HashMap<String, ArrayList<Review>> entities, int maxSegmentLength,
      Pattern[] patterns, int numAspects) {
    this.filters = filters;
    this.entities = entities;
    this.maxSegmentLen = maxSegmentLength;
    this.patterns = patterns;
    this.numAspects = numAspects;
    initReferenceSummary();
    extractCandidateSummary(0, patterns.length - 1);
  }

  private void initReferenceSummary() {
    prosRefSummary = new HashMap<String, ArrayList<String[]>>();
    consRefSummary = new HashMap<String, ArrayList<String[]>>();
    for (Entry<String, ArrayList<Review>> entry : entities.entrySet()) {
      String entityId = entry.getKey();
      ArrayList<String[]> prosRef = new ArrayList<String[]>();
      ArrayList<String[]> consRef = new ArrayList<String[]>();
      for (Review review : entry.getValue()) {
        ReviewWithProsAndCons rpc = (ReviewWithProsAndCons) review;
        prosRef.addAll(tokenizeProsOrConsString(rpc.getPros()));
        consRef.addAll(tokenizeProsOrConsString(rpc.getCons()));
      }
      prosRefSummary.put(entityId, prosRef);
      consRefSummary.put(entityId, consRef);
    }
    removeDuplicatedReferences(prosRefSummary);
    removeDuplicatedReferences(consRefSummary);
    // removeNonfrequentReferences(REFERENCE_THRESHOLD, prosRefSummary);
    // removeNonfrequentReferences(REFERENCE_THRESHOLD, consRefSummary);
  }

  /**
   * Computes the candidate summary using a subset of patterns from
   * <code>from</code> to <code>to</code>.
   * 
   * @param from
   * @param to
   */
  private void extractCandidateSummary(int from, int to) {
    candidateSummary = new HashMap<String, ArrayList<String[]>>();
    for (Entry<String, ArrayList<Review>> entry : entities.entrySet()) {
      ArrayList<String[]> candidate = new ArrayList<String[]>();
      for (Review review : entry.getValue()) {
        List<ArrayList<? extends HasWord>> sentences = MaxentTagger
            .tokenizeText(new BufferedReader(new StringReader(review
                .getContent())));
        for (ArrayList<? extends HasWord> tSentence : sentences) {
          List<String[]> segments = extractor.extractSegmentsAsArrays(patterns,
              tagger.tagSentence(tSentence), maxSegmentLen, from, to);
          candidate.addAll(segments);
        }
      }
      candidateSummary.put(entry.getKey(), candidate);
    }
  }

  private void removeDuplicatedReferences(
      HashMap<String, ArrayList<String[]>> map) {
    int cnt = 0;
    for (Entry<String, ArrayList<String[]>> entry : map.entrySet()) {
      HashSet<String> set = new HashSet<String>();
      ArrayList<String[]> newList = new ArrayList<String[]>();
      for (String[] item : entry.getValue()) {
        StringBuilder builder = new StringBuilder();
        for (String word : item) {
          builder.append(word).append(" ");
        }
        if (set.add(builder.toString())) {
          newList.add(item);
        } else {
          cnt++;
        }
      }
      map.put(entry.getKey(), newList);
    }
    System.out.println("Removed " + cnt);
  }

  /**
   * Removes the references that does not contain a top word.
   * 
   * @param threshold
   *          the number of top words
   * @param map
   */
  @SuppressWarnings("unused")
  private void removeNonfrequentReferences(int threshold,
      HashMap<String, ArrayList<String[]>> map) {
    int total = 0;
    for (Entry<String, ArrayList<String[]>> entry : map.entrySet()) {
      ObjectToCounterMap<String> counter = new ObjectToCounterMap<String>();
      for (String[] words : entry.getValue()) {
        for (String word : words) {
          counter.increment(word);
        }
      }
      ArrayList<String[]> newList = new ArrayList<String[]>();
      for (String[] words : entry.getValue()) {
        boolean hasTopWord = false;
        for (String word : words) {
          if (counter.getCount(word) >= threshold) {
            hasTopWord = true;
            break;
          }
        }
        if (hasTopWord) {
          newList.add(words);
        } else {
          total++;
        }
      }
      map.put(entry.getKey(), newList);
    }
    System.out.println("total references removed: " + total);
  }

  /**
   * Tokenizes the given string <code>str</code> into list of pros or cons.
   * <p>
   * This method returns a list of pros (cons) where each item is represented as
   * an array of lowercase stemmed strings.
   * 
   * @param str
   *          a pros or cons list
   * @return
   */
  private ArrayList<String[]> tokenizeProsOrConsString(String str) {
    String[] items = str.split(PROSCONS_DELIMITERS);
    ArrayList<String[]> list = new ArrayList<String[]>();
    for (String item : items) {
      String[] tokens = DocumentUtils.tokenizeAndStem(item, tokenizer, stemmer);
      if (tokens.length > 0 && isValidProsOrCons(tokens)) {
        list.add(tokens);
      }
    }

    return list;
  }

  /**
   * Returns true if <code>words</code> is a valid pros or cons string.
   * 
   * @param words
   * @return
   */
  private boolean isValidProsOrCons(String[] words) {
    if (words.length == 1 && words[0].equals("none")) {
      return false;
    }

    return (words.length < MAX_SUMMARY_LENGTH);
  }

  /**
   * Computes ROUGE-SU scores for various filters.
   * 
   * @param filter
   * @param category
   *          0 for pros list (or positive category), 1 for cons list (or
   *          negative category)
   * @param outfile
   * @throws IOException
   */
  public void computeAndPrintRougeScores(SegmentFilters.Filter filter,
      int category, String outfile) throws IOException {
    final int skipDist = 5;
    PrintWriter out = new PrintWriter(outfile);
    out.printf("entity, refSize, candSize, skipDist, prec, rec, f1, s-prec, s-rec\n");
    for (Entry<String, ArrayList<Review>> entry : entities.entrySet()) {
      String entityId = entry.getKey();
      System.err.println("Entity id " + entityId);
      ArrayList<String[]> reference = new ArrayList<String[]>();
      if (category == -1) {
        reference.addAll(prosRefSummary.get(entityId));
        reference.addAll(consRefSummary.get(entityId));
      } else if (category == 0) {
        reference.addAll(prosRefSummary.get(entityId));
      } else {
        reference.addAll(consRefSummary.get(entityId));
      }
      List<String[]> candidate = filters.filterSegments(filter,
          candidateSummary.get(entityId), category);
      double[] rouge = rouger.computeRougeSU(reference,
          (ArrayList<String[]>) candidate, skipDist, THRESHOLD, EPSILON);
      int refSize = reference.size();
      int candSize = candidate.size();
      printRougeScores(out, entityId, refSize, candSize, skipDist, rouge);
      printRougeScores(entityId, refSize, candSize, skipDist, rouge);
    }
    out.close();
  }

  /**
   * Prints the extracted segments.
   * 
   * @param outfile
   * @throws IOException
   */
  @SuppressWarnings("unused")
  private void printSummaries(String outfile, ArrayList<String[]>[][] summary)
      throws IOException {
    HashMap<String[], String> map = extractor.getSegmentToTextMap();
    PrintWriter out = new PrintWriter(outfile);
    int numSenti = 2;
    for (int k = 0; k < numAspects; k++) {
      for (int s = 0; s < numSenti; s++) {
        out.printf("ST%d-%d,", k, s);
      }
    }
    out.println();
    int cntNegative = 0;
    for (int idx = 0; idx < 50; idx++) {
      for (int k = 0; k < numAspects; k++) {
        for (int s = 0; s < numSenti; s++) {
          String[] segment = null;
          if (summary[k][s].size() > idx) {
            segment = summary[k][s].get(idx);
          }
          if (segment != null) {
            out.printf("%s,", map.get(segment));
            if (s == 1) {
              cntNegative++;
            }
          } else {
            out.print(",");
          }
        }
      }
      out.println();
    }
    if (cntNegative > 10) {
      out.close();
    }
  }

  /**
   * Prints the various rouge scores for an entity.
   */
  private void printRougeScores(PrintWriter out, String entityId, int refSize,
      int candSize, int skipDist, double[] rouge) {
    out.printf("%s,%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f\n", entityId, refSize,
        candSize, skipDist, rouge[0], rouge[1], rouge[2], rouge[3], rouge[4]);
  }

  /**
   * Prints the various rouge scores for an entity to the standard output
   * stream.
   */
  private void printRougeScores(String entityId, int refSize, int candSize,
      int skipDist, double[] rouge) {
    System.out.printf("%s,%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f\n", entityId,
        refSize, candSize, skipDist, rouge[0], rouge[1], rouge[2], rouge[3],
        rouge[4]);
  }

  /**
   * Outputs the most frequent structures used by users in pros and cons lists.
   * 
   * @param output
   *          the output file
   * @param entities
   *          the mapping between entity (review target) id and its reviews
   */
  public void findFrequentStructures(String output) throws IOException {
    PrintWriter out = new PrintWriter(output);
    ObjectToCounterMap<String> counter = new ObjectToCounterMap<String>();
    for (ArrayList<Review> entityReviews : entities.values()) {
      for (Review review : entityReviews) {
        ReviewWithProsAndCons r = (ReviewWithProsAndCons) review;
        ArrayList<String> structures = textsToStructures(r.getPros().split(
            PROSCONS_DELIMITERS));
        structures.addAll(textsToStructures(r.getCons().split(
            PROSCONS_DELIMITERS)));
        for (String structure : structures) {
          counter.increment(structure);
        }
      }
    }
    for (String key : counter.keysOrderedByCountList()) {
      out.printf("%s, %d\n", key, counter.getCount(key));
    }
    out.close();
  }

  /**
   * Converts the array of texts to a list of corresponding grammatical
   * structures.
   * 
   * @param texts
   * @return
   */
  private ArrayList<String> textsToStructures(String[] texts) {
    ArrayList<String> structures = new ArrayList<String>();
    for (String text : texts) {
      structures.add(getGrammaticalStructure(text));
    }

    return structures;
  }

  /**
   * Returns the grammar structure (i.e., the tags of individual words) of the
   * given text.
   * 
   * @param text
   * @return
   */
  private String getGrammaticalStructure(String text) {
    StringBuilder builder = new StringBuilder();
    String[] tokens = tagger.tagString(text).split(" ");
    for (int i = 0; i < tokens.length; i++) {
      int slashPos = tokens[i].indexOf("/");
      String tag = tokens[i].substring(slashPos + 1);
      builder.append(tag).append(" ");
    }
    System.out.println(text + "\t" + builder.toString());

    return builder.toString();
  }

  /**
   * Returns summaries of all reviews; each summary has a list of extracted
   * segments.
   * 
   * @return
   */
  public List<Summary> getSummaries() {
    List<Summary> list = new ArrayList<Summary>();
    HashMap<String[], String> map = extractor.getSegmentToTextMap();
    for (Entry<String, ArrayList<Review>> entry : entities.entrySet()) {
      String entityId = entry.getKey();
      ArrayList<String[]> candidates = candidateSummary.get(entityId);
      List<String[]> filteredSegments = filters.filterSegments(
          SegmentFilters.Filter.ASPECT, candidates, -1);
//      List<String[]> filteredSegments = filters.filterSegments(
//          SegmentFilters.Filter.BOTH_SENTIMENT, candidates, 0);
//      filteredSegments.addAll(filters.filterSegments(
//          SegmentFilters.Filter.BOTH_SENTIMENT, candidates, 1));
      if (filteredSegments.size() > 0) {
        Summary para = new Summary(entry.getValue().get(0).getContent());
        for (String[] segment : filteredSegments) {
          para.addSegment(map.get(segment));
        }
        list.add(para);
      }
    }

    return list;
  }

  public static void main(String args[]) throws IOException {
    // String bs =
    // "C:/datasets/models/bs/coffeemaker/optimization/T7-A0.1-B0.0100-G0.10,0.10-I1000()";
    // String bs =
    // "C:/datasets/models/bs/laptop/optimization/T7-A0.1-B0.0100-G0.10,0.10-I1000()";
    String bs = "C:/datasets/models/bs/ursa/optimization/T7-A0.1-B0.0100-G0.10,0.10-I1000()";
    new File(bs + "/pros").mkdir();
    new File(bs + "/cons").mkdir();
    Pattern[] patterns = SegmentExtractor.SERVICE_PATTERNS;
    int maxSegmentLength = 6; // use 6 for restaurants (or even less)
    HashMap<String, ArrayList<Review>> entities = BSUtils.readReviews(
        "C:/datasets/models/bs/ursa/docs.txt",
        new ReviewWithProsAndConsReader());
    Model bsModel = (OptimizationModel) BSUtils
        .loadModel(bs + "/1000/model.gz");
    SegmentFilters filters = new SegmentFilters(bsModel);
    SummarizationAnalyzer analyzer = new SummarizationAnalyzer(filters,
        entities, maxSegmentLength, patterns, bsModel.getNumTopics());
    String outfile = null;
    // filter using top aspect words, sentiment, and top sentiment words
    // outfile = String.format(
    // "%s/pros/bothSentiment-s%d-pros-thres%.2f-top%d-eps%.2f.csv", bs,
    // maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS, EPSILON);
    // analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.BOTH_SENTIMENT,
    // 0, outfile);
    // outfile = String.format(
    // "%s/cons/bothSentiment-s%d-cons-thres%.2f-top%d-eps%.2f.csv", bs,
    // maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS, EPSILON);
    // analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.BOTH_SENTIMENT,
    // 1, outfile);

    // no filter - baseline approach
    // outfile = bs + "/baseline-s6-threshold" + THRESHOLD + ".csv";
    // analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.NO_FILTER, -1,
    // outfile);

    // filter using top aspect words
    // outfile = bs + "/aspect-s6-threshold" + THRESHOLD + "-top"
    // + SegmentFilters.TOP_ASPECT_WORDS + ".csv";
    // analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.ASPECT, -1,
    // outfile);

    // filter using swn
    outfile = String.format(
        "%s/pros/baselineSWN-s%d-pros-thres%.2f-top%d-eps%.2f.csv", bs,
        maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS, EPSILON);
    analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.SWN, 0, outfile);
    outfile = String.format(
        "%s/cons/baselineSWN-s%d-cons-thres%.2f-top%d-eps%.2f.csv", bs,
        maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS, EPSILON);
    analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.SWN, 1, outfile);

    // filter using top aspect words and sentiment
    // outfile = String.format(
    // "%s/pros/aspectSentiment-s%d-pros-thres%.2f-top%d-eps%.2f.csv", bs,
    // maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS, EPSILON);
    // analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.ASPECT_SENTIMENT,
    // 0, outfile);
    // outfile = String.format(
    // "%s/cons/aspectSentiment-s%d-cons-thres%.2f-top%d-eps%.2f.csv", bs,
    // maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS, EPSILON);
    // analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.ASPECT_SENTIMENT,
    // 1, outfile);

    // filter using top aspect words and swn for sentiment
    outfile = String.format(
        "%s/pros/aspectSWN-s%d-pros-thres%.2f-top%d-eps%.2f.csv", bs,
        maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS, EPSILON);
    analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.ASPECT_SWN, 0,
        outfile);
    outfile = String.format(
        "%s/cons/aspectSWN-s%d-cons-thres%.2f-top%d-eps%.2f.csv", bs,
        maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS, EPSILON);
    analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.ASPECT_SWN, 1,
        outfile);

    // filter using ranked top aspect words and sentiment
    // outfile = String.format(
    // "%s/pros(opt/)aspectSentimentRanked-s%d-pros-thres%.2f-top%d-eps%.2f.csv",
    // bs, maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS,
    // EPSILON);
    // analyzer.computeAndPrintRougeScores(
    // SegmentFilters.Filter.RANKED_ASPECT_SENTIMENT, 0, outfile);
    // outfile = String.format(
    // "%s/cons/aspectSentimentRanked-s%d-cons-thres%.2f-top%d-eps%.2f.csv",
    // bs, maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS,
    // EPSILON);
    // analyzer.computeAndPrintRougeScores(
    // SegmentFilters.Filter.RANKED_ASPECT_SENTIMENT, 1, outfile);

    // summary for individual patterns
    // int[][] pairs = new int[][] { { 0, 3 }, { 4, 7 }, { 8, 10 }, { 11, 13 },
    // { 14, 16 } };
    // analyzeIndividualPatterns(analyzer, bs, maxSegmentLength, pairs);
  }

  static void runJstAndAsum() throws IOException {
    Pattern[] patterns = SegmentExtractor.SERVICE_PATTERNS;
    HashMap<String, ArrayList<Review>> entities = BSUtils.readReviews(
        "C:/datasets/models/bs/ursa/docs.txt",
        new ReviewWithProsAndConsReader());
    String outfile = null;
    int maxSegmentLength = 6;

    String asum = "C:/datasets/models/asum/ursa/T7-G0.10-0.10(seed1)";
    String jst = "C:/datasets/models/jst/ursa/T7-G0.10-0.10(seed1)";
    // String asum = "C:/datasets/models/asum/coffeemaker/T7-G0.10-0.10(seed1)";
    // String jst = "C:/datasets/models/jst/coffeemaker/T7-G0.10-0.10(seed1)";
    new File(asum + "/pros").mkdir();
    new File(asum + "/cons").mkdir();
    new File(jst + "/pros").mkdir();
    new File(jst + "/cons").mkdir();
    AsumModelData asumModel = (AsumModelData) BSUtils.loadModel(asum
        + "/1000/model.gz");
    JstModelData jstModel = (JstModelData) BSUtils.loadModel(jst
        + "/1000/model.gz");
    SegmentFilters filters = new SegmentFilters(asumModel, jstModel);
    SummarizationAnalyzer analyzer = new SummarizationAnalyzer(filters,
        entities, maxSegmentLength, patterns, asumModel.getNumTopics());
    // asum - pros
    outfile = String.format("%s/pros/s%d-pros-thres%.2f-top%d-eps%.2f.csv",
        asum, maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS,
        EPSILON);
    analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.ASUM, 0, outfile);
    // asum - cons
    outfile = String.format("%s/cons/s%d-cons-thres%.2f-top%d-eps%.2f.csv",
        asum, maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS,
        EPSILON);
    analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.ASUM, 1, outfile);
    // jst - pros
    outfile = String.format("%s/pros/s%d-pros-thres%.2f-top%d-eps%.2f.csv",
        jst, maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS,
        EPSILON);
    analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.JST, 0, outfile);
    // jst - cons
    outfile = String.format("%s/cons/s%d-cons-thres%.2f-top%d-eps%.2f.csv",
        jst, maxSegmentLength, THRESHOLD, SegmentFilters.TOP_ASPECT_WORDS,
        EPSILON);
    analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.JST, 1, outfile);
  }

  static void analyzeIndividualPatterns(SummarizationAnalyzer analyzer,
      String dir, int maxSegmentLength, int[][] pairs) throws IOException {
    String outfile = "";
    for (int idx = 0; idx < pairs.length; idx++) {
      analyzer.extractCandidateSummary(pairs[idx][0], pairs[idx][1]);
      outfile = String.format(
          "%s/patterns/bothSentiment-s%d-pros-top%dpatt%d-%d.csv", dir,
          maxSegmentLength, SegmentFilters.TOP_ASPECT_WORDS, pairs[idx][0],
          pairs[idx][1]);
      analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.BOTH_SENTIMENT,
          0, outfile);
      outfile = String.format(
          "%s/patterns/bothSentiment-s%d-cons-top%d-patt%d-%d.csv", dir,
          maxSegmentLength, SegmentFilters.TOP_ASPECT_WORDS, pairs[idx][0],
          pairs[idx][1]);
      analyzer.computeAndPrintRougeScores(SegmentFilters.Filter.BOTH_SENTIMENT,
          1, outfile);
    }
  }
}
