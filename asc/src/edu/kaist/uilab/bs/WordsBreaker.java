package edu.kaist.uilab.bs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import edu.kaist.uilab.asc.util.TextFiles;
import edu.kaist.uilab.stemmers.EnglishStemmer;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Breaks the words in a corpus into two sets, the aspect-indicating words and
 * the sentiment-indicating words. TODO(trung): one potential issue is words
 * that have multiple functions. Looking at the words it seems that most of them
 * are indeed adj. For now we consider them as adjective.
 * 
 * @author trung
 */
public class WordsBreaker {

  private static final String UTF8 = "utf-8";
  private final String sentiTags[] = { "JJ", "JJR", "JJS", // adjective
      "RB", "RBR", "RBS" // adverb
  };
  private static String model = "D:/java-libs/stanford-postagger-2010-05-26/models/left3words-wsj-0-18.tagger";

  private MaxentTagger tagger;
  private HashSet<String> sentiWords;
  private HashSet<String> aspectWords;
  private EnglishStemmer stemmer;

  public WordsBreaker() throws Exception {
    this(model);
  }

  public WordsBreaker(String model) throws Exception {
    tagger = new MaxentTagger(model);
    sentiWords = new HashSet<String>();
    aspectWords = new HashSet<String>();
    stemmer = new EnglishStemmer();
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
    HashSet<String> copy = new HashSet<String>(aspectWords);
    aspectWords.removeAll(sentiWords);
    TextFiles.writeCollection(sentiWords, sentiWordsFile, UTF8);
    TextFiles.writeCollection(aspectWords, aspectWordsFile, UTF8);
    copy.retainAll(sentiWords);
    TextFiles.writeCollection(copy, commonWordsFile, UTF8);
  }

  public void tagDocument(String document) {
    List<ArrayList<? extends HasWord>> sentences = MaxentTagger
        .tokenizeText(new BufferedReader(new StringReader(document)));
    for (ArrayList<? extends HasWord> sentence : sentences) {
      ArrayList<TaggedWord> tSentence = tagger.tagSentence(sentence);
      for (TaggedWord tWord : tSentence) {
        String stem = stemmer.getStem(tWord.word().toLowerCase());
        if (stem.length() < 2)
          continue;
        if (isSentiTag(tWord.tag())) {
          sentiWords.add(stem);
        } else {
          aspectWords.add(stem);
        }
      }
    }
  }

  void test() {
    System.out.println(tagger.tagString("The battery is not good"));
    System.out.println(tagger.tagString("The battery is notgood"));
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
    String dir = "C:/datasets/bs/small";
    wb.divide(dir + "/docs.txt", dir + "/aspects.txt", dir + "/senti.txt", dir
        + "/common.txt");
  }
}
