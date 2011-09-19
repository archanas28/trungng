package edu.kaist.uilab.bs.evaluation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.ObjectToCounterMap;

import edu.kaist.uilab.asc.data.Review;
import edu.kaist.uilab.asc.data.ReviewWithProsAndCons;
import edu.kaist.uilab.asc.data.ReviewWithProsAndConsReader;
import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.bs.BSUtils;
import edu.kaist.uilab.bs.CorpusParserWithTagger.BSTokenizerFactory;
import edu.kaist.uilab.bs.MaxentTaggerSingleton;
import edu.kaist.uilab.bs.Model;
import edu.kaist.uilab.stemmers.EnglishStemmer;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Analysis for the summarization tasks.
 * 
 * @author trung
 */
public class SummarizationAnalyzer {
  static final String PROSCONS_DELIMITERS = "[.,;]";
  static EnglishStemmer stemmer = new EnglishStemmer();
  static TokenizerFactory tokenizer = BSTokenizerFactory
      .getInstance(new HashSet<String>());
  static MaxentTagger tagger = MaxentTaggerSingleton.getInstance();

  Model model;
  DoubleMatrix[] phiSenti;
  double[][] phiAspect;

  /**
   * Constructor
   * 
   * @param model
   */
  public SummarizationAnalyzer(Model model) {
    this.model = model;
    phiSenti = model.getPhiSentiByTermscore();
    phiAspect = model.getPhiAspectByTermscore();
  }

  /**
   * Analyze the user provided pros and cons lists from reviews.
   * 
   * @param output
   *          the output file
   * @param targets
   *          the map between target id and its list of reviews
   * @param numSamples
   *          number of targets to analyze
   */
  public void analyzeProsAndCons(String output,
      HashMap<String, ArrayList<Review>> targets, int numSamples)
      throws IOException {
    PrintWriter out = new PrintWriter(output);
    out.print("<html><head><title>Pros and cons analysis</title></head><body>");
    for (Entry<String, ArrayList<Review>> entry : targets.entrySet()) {
      ArrayList<Review> reviews = entry.getValue();
      if (reviews.size() > 20) {
        ArrayList<String[]> prosList = new ArrayList<String[]>();
        ArrayList<String[]> consList = new ArrayList<String[]>();
        for (Review review : reviews) {
          ReviewWithProsAndCons r = (ReviewWithProsAndCons) review;
          prosList.addAll(tokenizeProsOrConsString(r.getPros()));
          consList.addAll(tokenizeProsOrConsString(r.getCons()));
        }
        analyzeTarget(out, entry.getKey(), prosList, consList);
      }
    }
    out.print("</body>");
    out.close();
  }

  /**
   * Tokenizes the given string <code>str</code> into list of pros or cons.
   * <p>
   * This method returns a list of pros where each pro item is represented as an
   * array of lowercase stemmed strings.
   * 
   * @param str
   *          a pros or cons list
   * @return
   */
  public static ArrayList<String[]> tokenizeProsOrConsString(String str) {
    String[] pros = str.split(PROSCONS_DELIMITERS);
    ArrayList<String[]> list = new ArrayList<String[]>();
    for (String pro : pros) {
      char[] cs = pro.toCharArray();
      String[] tokens = tokenizer.tokenizer(cs, 0, cs.length).tokenize();
      String[] stems = new String[tokens.length];
      for (int idx = 0; idx < tokens.length; idx++) {
        stems[idx] = stemmer.getStem(tokens[idx].toLowerCase());
      }
      list.add(stems);
    }

    return list;
  }

  /**
   * Analyze the pros and cons lists of a specific target.
   * 
   * @param out
   *          writer to print the output
   * @param id
   *          id of the specific target
   * @param prosList
   *          the list of pros of the target
   * @param consList
   *          the list of cons of the target
   */
  public void analyzeTarget(PrintWriter out, String id,
      ArrayList<String[]> prosList, ArrayList<String[]> consList) {
    out.printf("<h3>Product Id: %s (#reviews = %d)</h3>", id, prosList.size());
    int numTopics = model.getNumTopics();
    HashMap<Integer, ArrayList<String[]>> proMap = classifyProsOrConsList(
        prosList, numTopics);
    HashMap<Integer, ArrayList<String[]>> conMap = classifyProsOrConsList(
        consList, numTopics);
    for (int topic = -1; topic < numTopics; topic++) {
      out.printf("<h4>Aspect %d</h4>", topic);
      out.print("<b>pros: </b>");
      for (String[] item : proMap.get(topic)) {
        out.printf("%s, ", BSUtils.arrayToString(item, " "));
      }
      out.print("<br/><br/><b>cons: </b>");
      for (String[] item : conMap.get(topic)) {
        out.printf("%s, ", BSUtils.arrayToString(item, " "));
      }
    }
  }

  /**
   * Returns a mapping between aspects (i.e., topics) and the corresponding
   * classified pros/cons list .
   * 
   * @param list
   *          a list of pros or cons
   * @param numTopics
   * @return
   */
  private HashMap<Integer, ArrayList<String[]>> classifyProsOrConsList(
      ArrayList<String[]> list, int numTopics) {
    HashMap<Integer, ArrayList<String[]>> map = new HashMap<Integer, ArrayList<String[]>>();
    for (int i = -1; i < numTopics; i++) {
      map.put(i, new ArrayList<String[]>());
    }
    for (String[] item : list) {
      map.get(model.classifySegmentTopic(phiAspect, phiSenti, item)).add(item);
    }

    return map;
  }

  /**
   * Outputs the most frequent structures used by users in pros and cons lists.
   * 
   * @param output
   *          the output file
   * @param entities
   *          the mapping between entity (review target) id and its reviews
   */
  public void findFrequentStructures(String output,
      HashMap<String, ArrayList<Review>> entities) throws IOException {
    PrintWriter out = new PrintWriter(output);
    ObjectToCounterMap<String> counter = new ObjectToCounterMap<String>();
    for (ArrayList<Review> entityReviews : entities.values()) {
      for (Review review : entityReviews) {
        ReviewWithProsAndCons r = (ReviewWithProsAndCons) review;
        ArrayList<String> structures = textsToStructures(r.getPros().split(
            PROSCONS_DELIMITERS));
        structures.addAll(textsToStructures(r.getCons().split(PROSCONS_DELIMITERS)));
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
  
  public static void main(String args[]) throws IOException {
    String dir = "C:/datasets/models/bs/vacuum";
    ReviewWithProsAndConsReader reader = new ReviewWithProsAndConsReader();
    HashMap<String, ArrayList<Review>> entities = BSUtils.readReviews(dir
        + "/docs.txt", reader);
    Model model = (Model) BSUtils.loadModel(dir
        + "/T7-A0.1-B0.0010-G0.10,0.10-I1000(updateSentimentPrior)/1000/model.gz");
    SummarizationAnalyzer analyzer = new SummarizationAnalyzer(model);
    analyzer.findFrequentStructures(dir + "/structureOfProsAndCons_vacuum.csv", entities);
//    int numSamples = 50;
//    analyzer.analyzeProsAndCons(dir + "/prosAndConsAnalysis.html", entities,
//        numSamples);
  }
}
