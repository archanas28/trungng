package edu.kaist.uilab.bs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import edu.kaist.uilab.asc.util.TextFiles;
import edu.kaist.uilab.stemmers.EnglishStemmer;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Breaks the words in a corpus into two sets, the aspect-indicating words and
 * the sentiment-indicating words.
 * <p>
 * 3 ways to deal with words that have multiple functions are:
 * <ul>
 * <li>Treat the word as a sentiment word, not allowing to be in the aspect
 * words category.</li>
 * <li>Choose as the category the function which is more frequently associated
 * with the word. If equals then treat the word as a sentiment word.</li>
 * <li>No special treatmean is needed; a common word can be in both categories.</li>
 * </ul>
 * <p>
 * Based on the document-level sentiment classification task, it seems that the
 * model is not sensitive to the 2 different settings. If that is the case, use
 * approach 2 as it is more efficient in terms of time and memory space.
 * 
 * @author trung
 */
public class WordsBreaker {

  private static final String UTF8 = "utf-8";
  private final String sentiTags[] = { "JJ", "JJR", "JJS", // adjective
      "RB", "RBR", "RBS" // adverb
  };

  private MaxentTagger tagger = MaxentTaggerSingleton.getInstance();
  private HashMap<String, Integer> sentiWords;
  private HashMap<String, Integer> aspectWords;
  private EnglishStemmer stemmer;

  public WordsBreaker(String model) throws Exception {
    sentiWords = new HashMap<String, Integer>();
    aspectWords = new HashMap<String, Integer>();
    stemmer = new EnglishStemmer();
  }

  /**
   * Default constructor
   */
  public WordsBreaker() {
  }

  /**
   * Divides the words in <code>corpus</code> into two sets of words.
   * 
   * @param corpus
   * @param aspectWordsFile
   * @param sentiWordsFile
   */
  public void divide(String corpus, String aspectWordsFile,
      String sentiWordsFile, String commonWordsFile) throws Exception {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(corpus), UTF8));
    while (in.readLine() != null) {
      in.readLine();
      tagDocument(in.readLine());
    }
    in.close();
    HashSet<String> commonWords = new HashSet<String>(sentiWords.keySet());
    commonWords.retainAll(aspectWords.keySet());
    TextFiles.writeCollection(commonWords, commonWordsFile, UTF8);
    // categorizeCommonWords1(commonWords);
    // categorizeCommonWords2(commonWords);
    TextFiles.writeCollection(sentiWords.keySet(), sentiWordsFile, UTF8);
    TextFiles.writeCollection(aspectWords.keySet(), aspectWordsFile, UTF8);
  }

  /**
   * Categorizes the common words using the first approach.
   * 
   * @param commonWords
   */
  @SuppressWarnings("unused")
  private void categorizeCommonWords1(HashSet<String> commonWords) {
    aspectWords.keySet().removeAll(commonWords);
  }

  /**
   * Categorizes the common words using the second approach.
   */
  @SuppressWarnings("unused")
  private void categorizeCommonWords2(HashSet<String> commonWords) {
    for (String word : commonWords) {
      int sentiCnt = sentiWords.get(word);
      int aspectCnt = aspectWords.get(word);
      if (sentiCnt < aspectCnt) {
        sentiWords.remove(word);
      } else {
        aspectWords.remove(word);
      }
    }
  }

  public void tagDocument(String document) {
    List<ArrayList<? extends HasWord>> sentences = MaxentTagger
        .tokenizeText(new BufferedReader(new StringReader(document)));
    for (ArrayList<? extends HasWord> sentence : sentences) {
      ArrayList<TaggedWord> tSentence = tagger.tagSentence(sentence);
      for (TaggedWord tWord : tSentence) {
        String stem = stemmer.getStem(tWord.word().toLowerCase());
        if (stem.length() < 3)
          continue;
        Integer count = null;
        if (isSentiTag(tWord.tag())) {
          count = sentiWords.get(stem);
          if (count == null) {
            sentiWords.put(stem, 0);
          } else {
            sentiWords.put(stem, count + 1);
          }
        } else {
          count = aspectWords.get(stem);
          if (count == null) {
            aspectWords.put(stem, 0);
          } else {
            aspectWords.put(stem, count + 1);
          }
        }
      }
    }
  }

  /*
   * Returns true if the given tag is one of the senti tag.
   */
  boolean isSentiTag(String tag) {
    for (String sentiTag : sentiTags) {
      if (sentiTag.equals(tag)) {
        return true;
      }
    }

    return false;
  }

  public static void main(String args[]) throws Exception {
    WordsBreaker wb = new WordsBreaker();
    String[] strs = new String[] {
        "On this night the Duck L'Orange was exceptional, and the Chilian Seabass moist and flavorful",
        "This is a relaxed country inn in the middle of Manhattan.",
        "They will offer suggestions when asked, and were on the mark with the recommendation of the chocolate souffle' for dessert.",
        "Immediately was offered menus and bar service. Served a wonderful roasted eggplant dip and crostini within minutes of sitting down. Had the goat cheese and potato appetizer and the chicken with capers entree.",
        "Had difficulty getting a cab from the Waldorf, so we were about 15 mins late, but they still greeted us warmly as though we were precisely on time",
        "The food was better than expected, the service attentive without making us feel like we had to rush through the meal, and the ambiance superb.",
        "Our waiter, while not the most personable, did his job, and the food was excellent",
        "We were not given menus, waited fifteen minutes and had to ask for them.",
        "Our tapanade and crustini were wisked off the table before we had a chance to try them.",
        "Food at Columbia Cottage is tasty and well prepared.",
        "My favorites include Orange Beef, Beef Lo Mein, and Crispy Shredded Beef.",
        "The ambience is pleasant and relaxing.",
        "Where else in the city can you go get completely tanked and have a good meal for the price of chinese takeout.",
        "The comfort foods, kind employees and hustle and bustle remind me of Sundays at Grandma's with the entire extended family.",
        "I leave feeling cared for and content.",
        "I grew up in the neighborhood, but I still schlep back for the best kosher deli on the planet.",
        "I didn't even eat one and sent it back, but they still put it on my bill, which I had to contest",
        "Chicken alfredo was too buttery and thick in all the wrong ways, chicken bites tasted boiled",
        "Joe Mineo is often behind the counter, providing fun, laughs and love to all his customers!",
        "Even the mashed potatoes and gravy tasted like they were made in a high-school cafeteria",
        "The place is a little dirty and there are kids running around everywhere but the food makes up for it",
        "The atmosphere was mostly of an older crowd, but there were a few groups of mid 20's/30 year olds also.",
    };
    for (String s : strs) {
      System.out.println(wb.tagger.tagString(s));
    }
//    String dir = "C:/datasets/bs/restaurants";
//    wb.divide(dir + "/docs.txt", dir + "/aspects.txt", dir + "/senti.txt", dir
//        + "/common.txt");
    // wb.divide(dir + "/docs.txt", dir + "/aspects_adj.txt", dir +
    // "/senti_adj.txt", dir
    // + "/common_adj.txt");
  }
}
