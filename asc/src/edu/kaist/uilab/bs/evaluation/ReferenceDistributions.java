package edu.kaist.uilab.bs.evaluation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.ObjectToCounterMap;
import com.aliasi.util.ObjectToDoubleMap;

import edu.kaist.uilab.asc.util.TextFiles;
import edu.kaist.uilab.bs.CorpusParserWithTagger.BSTokenizerFactory;

/**
 * The reference topic distributions constructed from the manually annotated
 * restaurant reviews.
 * 
 * @author trung
 */
public class ReferenceDistributions {
  public static final String[] ASPECTS = { "food", "staff", "price",
      "ambience", "miscellaneous", "anecdotes" };

  static final int UNKNOWN_SENTIMENT = -1;
  static int numSenti = 2;
  static int positive = 0, negative = 1, neutral = 2, conflict = 3;
  static int numAspects = 6;

  HashMap<String, Integer> topics;
  ObjectToCounterMap<String>[][] cnt;
  int[][] sumCnt;
  ObjectToDoubleMap<String>[][] phi;
  TokenizerFactory tokenizer;
  int numSentences = 0;
  int sentencesWithMultipleAspects = 0;

  /**
   * For testing purpose.
   */
  protected ReferenceDistributions() {
    tokenizer = BSTokenizerFactory.getInstance(new HashSet<String>());
    init();
  }

  public ReferenceDistributions(String annotatedFile, String stopFile)
      throws IOException {
    List<String> stopStems = TextFiles.readLines(stopFile);
    tokenizer = BSTokenizerFactory.getInstance(new HashSet<String>(stopStems));
    init();
    readAnnotatedReviews(annotatedFile);
    System.out.printf("# sentences with multiple aspects : %d/%d\n",
        sentencesWithMultipleAspects, numSentences);
    computePhiSenti(cnt, sumCnt);
  }

  @SuppressWarnings("unchecked")
  void init() {
    topics = new HashMap<String, Integer>();
    for (int topic = 0; topic < ASPECTS.length; topic++) {
      topics.put(ASPECTS[topic], topic);
    }
    cnt = new ObjectToCounterMap[numSenti][numAspects];
    phi = new ObjectToDoubleMap[numSenti][numAspects];
    for (int i = 0; i < numSenti; i++) {
      for (int j = 0; j < numAspects; j++) {
        cnt[i][j] = new ObjectToCounterMap<String>();
      }
    }
    sumCnt = new int[numSenti][numAspects];
  }

  /**
   * Returns the reference distribution (i.e., the "true" or empirical) for the
   * given <code>sentiment</code> and <code>topic</code>.
   * 
   * @param sentiment
   * @param topic
   * @return
   */
  public ObjectToDoubleMap<String> getReferenceDistribution(int sentiment,
      int topic) {
    return phi[sentiment][topic];
  }

  /**
   * Returns the reference (empirical) distributions.
   * 
   * @return
   */
  public ObjectToDoubleMap<String>[][] getReferenceDistributions() {
    return phi;
  }

  /**
   * Reads the annotated reviews and use them to construct the reference
   * distribution.
   * 
   * @param annotatedFile
   */
  private void readAnnotatedReviews(String annotatedFile) {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(annotatedFile);
      NodeList reviews = doc.getElementsByTagName("Review");
      for (int idx = 0; idx < reviews.getLength(); idx++) {
        Node review = reviews.item(idx);
        processReview(review);
      }
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Handles a review node which consists of annotated sentences.
   * 
   * @param review
   */
  private void processReview(Node review) {
    NodeList sentences = review.getChildNodes();
    for (int i = 0; i < sentences.getLength(); i++) {
      Node node = sentences.item(i);
      ArrayList<Integer> sentenceTopics = new ArrayList<Integer>();
      int sentiment;
      do {
        sentiment = getSentiment(node);
        String name = node.getNodeName().toLowerCase();
        if (topics.containsKey(name)) {
          sentenceTopics.add(topics.get(name));
        }
        node = node.getFirstChild();
      } while (node != null && sentiment == UNKNOWN_SENTIMENT);
      if (node != null && sentiment < conflict) {
        updateWordCount(sentiment, sentenceTopics, node.getTextContent());
      }
    }
  }

  /**
   * Updates the word count by adding count for words in the given sentence.
   * 
   * @param senti
   * @param sentenceTopics
   * @param sentence
   */
  void updateWordCount(int senti, ArrayList<Integer> sentenceTopics,
      String sentence) {
    numSentences++;
    if (sentenceTopics.size() > 2) {
      sentencesWithMultipleAspects++;
    }
    // ignore neutral sentences
    if (senti != neutral) {
      for (int topic : sentenceTopics) {
        char[] cs = sentence.toCharArray();
        for (String token : tokenizer.tokenizer(cs, 0, cs.length)) {
          cnt[senti][topic].increment(token);
          sumCnt[senti][topic]++;
        }
      }
    }
  }

  /**
   * Computes the reference topic distribution, i.e., the "ground truth"
   * distribution.
   * 
   * @param cnt
   * @param sumCnt
   */
  private void computePhiSenti(ObjectToCounterMap<String>[][] cnt,
      int[][] sumCnt) {
    for (int senti = 0; senti < numSenti; senti++) {
      for (int topic = 0; topic < numAspects; topic++) {
        phi[senti][topic] = new ObjectToDoubleMap<String>();
        ObjectToCounterMap<String> counter = cnt[senti][topic];
        for (String word : counter.keySet()) {
          phi[senti][topic].increment(word, ((double) counter.getCount(word))
              / sumCnt[senti][topic]);
        }
      }
    }
  }

  /**
   * Writes the top words according to the reference distributions to the given
   * file.
   * 
   * @param string
   */
  public void writeTopWords(String file, int numTopWords) throws IOException {
    PrintWriter out = new PrintWriter(file);
    @SuppressWarnings("unchecked")
    List<String>[][] topWords = new List[numSenti][numAspects];
    for (int senti = 0; senti < numSenti; senti++) {
      for (int aspect = 0; aspect < numAspects; aspect++) {
        out.printf("S%d-T%d,", senti, aspect);
        topWords[senti][aspect] = phi[senti][aspect].keysOrderedByValueList()
            .subList(0, numTopWords);
      }
    }
    out.println();
    for (int idx = 0; idx < numTopWords; idx++) {
      for (int senti = 0; senti < numSenti; senti++) {
        for (int aspect = 0; aspect < numAspects; aspect++) {
          String word = topWords[senti][aspect].get(idx);
          out.printf("%s (%.3f),", word, phi[senti][aspect].get(word));
        }
      }
      out.println();
    }
    out.close();
  }

  /**
   * Returns the sentiment value of the given node.
   * 
   * @param node
   * @return
   */
  private int getSentiment(Node node) {
    String name = node.getNodeName();
    if (name.equalsIgnoreCase("positive")) {
      return positive;
    }
    if (name.equalsIgnoreCase("negative")) {
      return negative;
    }
    if (name.equalsIgnoreCase("neutral")) {
      return neutral;
    }
    if (name.equalsIgnoreCase("conflict")) {
      return conflict;
    }

    return UNKNOWN_SENTIMENT;
  }
}
