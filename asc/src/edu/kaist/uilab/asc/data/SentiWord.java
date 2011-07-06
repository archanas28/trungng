package edu.kaist.uilab.asc.data;

public class SentiWord extends SamplingWord {
  private static final long serialVersionUID = -6338115900857865882L;
  
  private int mSentiment;
  public Integer priorSentiment = null;

  public SentiWord(int wordNo) {
    super(wordNo);
  }

  public void setSentiment(int sentiment) {
    mSentiment = sentiment;
  }

  public int getSentiment() {
    return mSentiment;
  }
}