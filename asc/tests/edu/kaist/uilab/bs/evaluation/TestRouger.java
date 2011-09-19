package edu.kaist.uilab.bs.evaluation;

import java.util.ArrayList;

import junit.framework.TestCase;
import edu.kaist.uilab.bs.evaluation.Rouger.Bigram;

/**
 * Tests for {@link Rouger}.
 * 
 * @author trung
 */
public class TestRouger extends TestCase {
  private Rouger rouger;

  @Override
  public void setUp() {
    rouger = new Rouger();
  }

  public void testAddDummyMaker() {
    String[] x = new String[] { "aaaa", "bbbb" };
    String[] expected = new String[] { Rouger.DUMMY, "aaaa", "bbbb" };
    if (!equals(expected, rouger.addDummyMarker(x))) {
      fail("testAddDummyMaker() failed!");
    }
  }

  public void testCountCommoneElements() {
    Bigram gram1 = new Bigram("left", "right");
    Bigram gram2 = new Bigram("left", "right");
    ArrayList<Bigram> list1 = new ArrayList<Bigram>();
    list1.add(gram1);
    ArrayList<Bigram> list2 = new ArrayList<Bigram>();
    list2.add(gram2);
    assertEquals(1, rouger.countCommonElements(list1, list2));
  }

  public void testGetBigrams() {
    String[] test = { Rouger.DUMMY, "w1", "w2" };
    int skipDistance = 0;
    ArrayList<Bigram> actual = rouger.getBigrams(test, skipDistance);
    // skip distance = 0 means no bi-grams
    assertEquals(0, actual.size());
    skipDistance = test.length;
    actual = rouger.getBigrams(test, skipDistance);
    ArrayList<Bigram> expected = new ArrayList<Bigram>();
    // skip distance > size means all bi-grams are valid
    for (int i = 0; i < test.length - 1; i++) {
      for (int j = i + 1; j < test.length; j++) {
        expected.add(new Bigram(test[i], test[j]));
      }
    }
    if (!equals(expected, actual)) {
      fail("expected = " + expected + ", was " + actual);
    }

  }

  /**
   * Returns true if <code>array1</code> and <code>array2</code> are equal
   * element-wise.
   * 
   * @param <T>
   * @param array1
   * @param array2
   * @return
   */
  <T> boolean equals(T[] array1, T[] array2) {
    if (array1.length != array2.length) {
      return false;
    }
    for (int idx = 0; idx < array1.length; idx++) {
      if (!array1[idx].equals(array2[idx])) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns true if <code>list1</code> and <code>list2</code> are equal
   * element-wise.
   * 
   * @param <T>
   * @param list1
   * @param list2
   * @return
   */
  <T> boolean equals(ArrayList<T> array1, ArrayList<T> array2) {
    if (array1.size() != array2.size()) {
      return false;
    }
    for (int idx = 0; idx < array1.size(); idx++) {
      if (!array1.get(idx).equals(array2.get(idx))) {
        return false;
      }
    }

    return true;
  }
}
