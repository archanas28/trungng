package edu.kaist.uilab.asc.data;

import java.io.Serializable;

/**
 * A sampling word that is used for Gibbs sampling which contains:
 * <li>a topic (this is the topic that generates an instance of
 * the word in some document)
 * <li>a word number (the index of this word in the
 * vocabulary)
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class SamplingWord implements Comparable<SamplingWord>, Serializable {
  private static final long serialVersionUID = -1867797173766544440L;
  
  int mTopic;
  int mWordNo;

  public SamplingWord() {
  }

  public SamplingWord(int wordNo) {
    mWordNo = wordNo;
  }

  public SamplingWord(int wordNo, int topic) {
    mTopic = topic;
    mWordNo = wordNo;
  }

  public int getTopic() {
    return mTopic;
  }

  public void setTopic(int topic) {
    mTopic = topic;
  }

  public int getWordNo() {
    return mWordNo;
  }

  @Override
  public boolean equals(Object o) {
    SamplingWord w = (SamplingWord) o;
    return (mWordNo == w.mWordNo);
  }

  @Override
  public int compareTo(SamplingWord word) {
    return mWordNo - word.mWordNo;
  }
}
