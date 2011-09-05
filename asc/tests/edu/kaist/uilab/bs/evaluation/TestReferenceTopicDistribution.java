package edu.kaist.uilab.bs.evaluation;

import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * Tests for {@link ReferenceDistributions}.
 * 
 * @author trung
 */
public class TestReferenceTopicDistribution extends TestCase {

  ReferenceDistributions reference;

  @Override
  public void setUp() {
    reference = new ReferenceDistributions();
  }

  public void testUpdateWordsCount() {
    int senti = 0;
    int topic = 0;
    ArrayList<Integer> aspects = new ArrayList<Integer>();
    aspects.add(topic);
    String sentence = "The food was good.";
    reference.updateWordCount(senti, aspects, sentence);
    assertEquals(1, reference.cnt[senti][topic].getCount("food"));
    assertEquals(1, reference.cnt[senti][topic].getCount("good"));
    assertEquals(4, reference.sumCnt[senti][topic]);
  }
}
