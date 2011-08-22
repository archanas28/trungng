package edu.kaist.uilab.bs;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * A singleton for the maxent tagger in the stanford pos tagger package.
 * 
 * @author trung
 */
public final class MaxentTaggerSingleton {
  private static String model = "D:/java-libs/stanford-postagger-2010-05-26/models/left3words-wsj-0-18.tagger";
  static MaxentTagger tagger;

  private MaxentTaggerSingleton() {
  }

  static {
    try {
      tagger = new MaxentTagger(model);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static MaxentTagger getInstance() {
    return tagger;
  }
}
