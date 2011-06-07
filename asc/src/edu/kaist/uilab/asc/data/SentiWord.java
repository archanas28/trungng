package edu.kaist.uilab.asc.data;

public class SentiWord extends SamplingWord {
  private static final long serialVersionUID = -6338115900857865882L;
  
  private int sentiment;
  public Integer priorSentiment = null;

  public SentiWord(int wordNo) {
    super(wordNo);
  }

  public void setSentiment(int sentiment) {
    this.sentiment = sentiment;
  }

  public int getSentiment() {
    return sentiment;
  }
}