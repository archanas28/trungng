package edu.kaist.uilab.bs;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Static methods for handling and processing the content of a document
 * (review).
 * 
 * @author trung
 */
public final class DocumentUtils {
  
  private static final String SENTI_TAGS[] = { "JJ", "JJR", "JJS", // adjective
  // "RB", "RBR", "RBS" // adverb
  };
  private static final String[] NOUN_TAGS = { "NN", "NNS", "NNP" };
  private static final List<String> sentiTags = Arrays.asList(SENTI_TAGS);
  private static final List<String> nounTags = Arrays.asList(NOUN_TAGS);
  private static MaxentTagger tagger = MaxentTaggerSingleton.getInstance();

  /**
   * Tokenizes the given document <code>content</code> into sentences.
   * <p>
   * This method tokenizes a document into sentences using the common sentence
   * ending punctuation <code>., ?, and !</code>. Furthermore, it also breaks a
   * sentence into 2 clauses if the sentence satisfies two conditions. First, it
   * contains a co-ordinating conjunction (we only consider <code>and</code> and
   * <code>but</code> because of their frequency). Second, each of the 2 parts
   * of the sentence divided by the conjunction must contain at least one pair
   * of sentiment and aspect word, i.e., an adjective and a noun. The original
   * sentence is then considered as two sentences.
   * <p>
   * Classes should use this method to ensure that a document is always
   * tokenized into the same set of sentences.
   * 
   * @param content
   * @return
   */
  public static List<ArrayList<? extends HasWord>> tokenizeSentences(
      String content) {
    List<ArrayList<? extends HasWord>> list = new ArrayList<ArrayList<? extends HasWord>>();
    List<ArrayList<? extends HasWord>> tSentences = MaxentTagger
        .tokenizeText(new BufferedReader(new StringReader(content)));
    final String[] conjunctions = { "but", "and" };
    for (ArrayList<? extends HasWord> sentence : tSentences) {
      boolean broken = false;
      int size = sentence.size();
      for (int idx = size - 1; idx >= 0; idx--) {
        String word = sentence.get(idx).word();
        if (word.equals(conjunctions[0]) || word.equals(conjunctions[1])) {
          if (idx >= 2 && idx < size - 2) {
            ArrayList<? extends HasWord> clause1 = new ArrayList<HasWord>(
                sentence.subList(0, idx));
            ArrayList<? extends HasWord> clause2 = new ArrayList<HasWord>(
                sentence.subList(idx + 1, size));
            if (hasSentiAspectPair(clause1) && hasSentiAspectPair(clause2)) {
              list.add(clause1);
              list.add(clause2);
              broken = true;
              break;
            }
          }
        }
      }
      if (!broken) {
        list.add(sentence);
      }
    }

    return list;
  }

  /**
   * Returns true if the given <code>sentence</code> contains a potential
   * sentiment-aspect pair, i.e., an adjective and a noun.
   * 
   * @param clause
   * @return
   */
  public static boolean hasSentiAspectPair(List<? extends HasWord> sentence) {
    ArrayList<TaggedWord> tWords = tagger.tagSentence(sentence);
    boolean hasAdj = false, hasNoun = false;
    for (TaggedWord tWord : tWords) {
      if (isSentiWord(tWord)) {
        hasAdj = true;
      } else if (isNoun(tWord)) {
        hasNoun = true;
      }
      if (hasAdj && hasNoun) {
        return true;
      }
    }

    return false;
  }

  /**
   * Converts a sentence tokenized by the Stanford POS Tagger to a normal
   * sentence text.
   * <p>
   * This method assumes that negation was not performed on
   * <code>tokenizedSentence</code>. Rather, negation will be applied to the
   * returned sentence text, i.e., <code>not xxx</code> becomes
   * <code>not_xxx</code> in the result.
   * 
   * @param tokenizedSentence
   * @return
   */
  public static String tokenizedSentenceToText(
      ArrayList<? extends HasWord> tokenizedSentence) {
    StringBuilder builder = new StringBuilder();
    for (HasWord w : tokenizedSentence) {
      String word = w.word();
      if (word.equals("not")) {
        builder.append("not_");
      } else {
        builder.append(word).append(" ");
      }
    }

    return builder.toString();
  }

  /**
   * Performs negation for the given text.
   * 
   * @param text
   * @return
   */
  public static String negate(String text) {
    ArrayList<String[]> list = new ArrayList<String[]>();
    list.add(new String[] {
        "(not|n't|without|never)[\\s]+(very|so|too|much|"
            + "quite|even|that|as|as much|a|the|to|really|been|be)[\\s]+",
        " not_" });
    list.add(new String[] { "(not|n't|without|never|no)[\\s]+", " not_" });
    for (String[] rp : list) {
      if (text != null) {
        text = Pattern
            .compile(rp[0], Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
            .matcher(text).replaceAll(rp[1]);
      }
    }

    return text;
  }
  
  /**
   * Replaces non-alphabet characters in the given text.
   * 
   * @param text
   * @return
   */
  public static String removesNonAlphabets(String text) {
    ArrayList<String[]> list = new ArrayList<String[]>();
    list.add(new String[] { "[http|ftp]://[\\S]*", " " });
    list.add(new String[] { "[()<>\\[\\],~&;:\"\\-/=*#@^+'`’]", " " });
    for (String[] rp : list) {
      if (text != null) {
        text = Pattern
            .compile(rp[0], Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
            .matcher(text).replaceAll(rp[1]);
      }
    }

    return text;
  }

  /**
   * Returns true if the given word <code>tWord</code> is tagged with the
   * adjective or adverb tag.
   * 
   * @param tWord
   * @return
   */
  public static boolean isSentiWord(TaggedWord tWord) {
    return sentiTags.contains(tWord.tag());
  }

  /**
   * Returns true if the given word <code>tWord</code> is tagged with the noun
   * tag.
   * 
   * @param tWord
   * @return
   */
  public static boolean isNoun(TaggedWord tWord) {
    return nounTags.contains(tWord.tag());
  }
}
