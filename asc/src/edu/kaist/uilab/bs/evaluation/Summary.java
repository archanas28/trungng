package edu.kaist.uilab.bs.evaluation;

import java.util.ArrayList;
import java.util.List;

import edu.kaist.uilab.bs.evaluation.WordPairSummarizer.WordPair;

/**
 * A summary for a single or multiple reviews.
 * <p>
 * The summary can be a list of segments or a list of adj-noun pairs.
 * 
 * @author trung
 */
public class Summary {
  private List<String> segments;
  private List<WordPair> pairs;
  private String content;

  public Summary(String content) {
    this.content = content.trim();
    segments = new ArrayList<String>();
    pairs = new ArrayList<WordPair>();
  }

  /**
   * Returns contents of reviews each separated by a star (|) character.
   * 
   * @return
   */
  public String getContent() {
    return content;
  }

  public int getNumSegments() {
    return segments.size();
  }

  public void addSegment(String segment) {
    segments.add(segment);
  }

  public void addWordPair(WordPair wordpair) {
    pairs.add(wordpair);
  }

  public void addWordPairs(List<WordPair> wordpairs) {
    pairs.addAll(wordpairs);
  }

  @Override
  public String toString() {
    // first 6 line: 6 reviews
    // 7th line: segments separated by ',,'
    // 8th line: word pair separated by ','
    StringBuilder builder = new StringBuilder(content);
    builder.append("\n");
    for (String segment : segments) {
      builder.append(segment).append(",");
    }
    builder.append("\n");
    for (WordPair pair : pairs) {
      builder.append(pair).append(",");
    }

    return builder.toString();
  }
}
