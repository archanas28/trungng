package edu.kaist.uilab.bs;

import junit.framework.TestCase;

public class TestNgramsCounter extends TestCase {
  private NgramsCounter counter;

  @Override
  public void setUp() {
    counter = new NgramsCounter(3);
  }

  public void testConcats() {
    String[] words = { "These", "are", "test", "words" };
    int start = 0;
    int end = 3;
    assertEquals("these are test", counter.concats(words, start, end));
    end = 10;
    assertEquals("these are test words", counter.concats(words, start, end));
  }

  public void testGetCount() {
    String str = "a test string";
    counter.incrementCount(str);
    assertEquals(1, counter.getCount(str));
    assertEquals(0, counter.getCount("random string"));
  }

  public void testIncrementCount() {
    String sentence = "This is a test sentence, really a test sentence";
    counter.incrementCount(sentence);
    assertEquals(2, counter.getCount("a test sentence"));
    assertEquals(0, counter.getCount("not exist"));
  }

  public void testIncrementCountForDocument() {
    String document = "This is a test sentence. Another test sentence. Yet another test sentence.";
    counter.incrementCount(document, null);
    assertEquals(1, counter.getCount("a test sentence"));
    assertEquals(1, counter.getCount("this is a"));
    assertEquals(0, counter.getCount("test sentence Yet"));
  }
}
