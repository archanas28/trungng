package edu.kaist.uilab.asc.data;

public class SentiWord extends Word {
  private static final long serialVersionUID = -6338115900857865882L;
  private int topic;
  private int sentiment;
  public Integer lexicon = null;

  public SentiWord(int wordNo) {
    this.wordNo = wordNo;
  }

  public SentiWord(int wordNo, int topic) {
    this.topic = topic;
    this.wordNo = wordNo;
  }

  public int getTopic() {
    return topic;
  }

  public void setTopic(int topic) {
    this.topic = topic;
  }

  public void setSentiment(int sentiment) {
    this.sentiment = sentiment;
  }

  public int getSentiment() {
    return sentiment;
  }
}