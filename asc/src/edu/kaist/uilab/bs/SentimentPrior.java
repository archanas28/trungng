package edu.kaist.uilab.bs;

/**
 * Class that provides prior knowledge about sentiment for the parser.
 */
public final class SentimentPrior {
  public static final String sentiTags[] = { "JJ", "JJR", "JJS", // adjective
      "RB", "RBR", "RBS" // adverb
  };

  /**
   * Specific prior words that convey sentiment.
   */
  public static final String sentiWords[] = {
    "love", "like", "enjoy", "thank",
      "worth", "recommend", "hate", "annoy", "disappoint", "regret", "wast"
  };

  /**
   * Returns true if the given tag is one of the sentiment tags.
   */
  public static boolean isSentiTag(String tag) {
    for (String sentiTag : sentiTags) {
      if (sentiTag.equals(tag)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns true if the given stemmed word is one of the prior sentiment words.
   * 
   * @param word
   * @return
   */
  public static boolean isSentiWord(String word) {
    for (String sentiWord : sentiWords) {
      if (sentiWord.equals(word)) {
        return true;
      }
    }

    return false;
  }
}
