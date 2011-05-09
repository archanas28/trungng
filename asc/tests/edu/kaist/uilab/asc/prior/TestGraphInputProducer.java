package edu.kaist.uilab.asc.prior;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * Tests for {@link GraphInputProducer}.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class TestGraphInputProducer extends TestCase {
  static final String WORDLIST = "test/testwordlist.txt";
  GraphInputProducer producer;
  
  @Override
  public void setUp() {
    ArrayList<String> list = new ArrayList<String>();
    list.add("test/eng-french.txt");
    producer = new GraphInputProducer(WORDLIST, list);
  }
  
  @Override
  public void tearDown() {
    producer = null;
  }
  
  public void testReadWords() throws IOException {
    producer.readWords(producer.mFile);
    if (!(producer.map.containsKey("the")
        && producer.map.containsKey("vocabulary")
        && producer.map.containsKey("file"))) {
      fail("Word list does not contain expected words");
    }
  }
  
  public void testWrite() throws IOException {
    producer.write("test/test_graph.txt");
    File file = new File("test/test_graph.txt");
    if (!file.exists()) {
      fail("Expected output file not found");
    }
  }
}
