package edu.kaist.uilab.bs.evaluation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import edu.kaist.uilab.asc.data.Review;
import edu.kaist.uilab.asc.data.ReviewWithProsAndConsReader;
import edu.kaist.uilab.bs.BSUtils;
import edu.kaist.uilab.bs.MaxentTaggerSingleton;
import edu.kaist.uilab.bs.evaluation.SegmentExtractor.Pattern.Part;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Class that extracts candidate text segments for summary from a sentence
 * according to some pre-defined patterns. TODO(trung): filter the sub-strings
 * where the last words are not noun (i.e., DT or IN) for patterns that end with
 * nouns.
 * 
 * @author trung
 */
public class SegmentExtractor {

  /**
   * The maximum length of a segment that can be extracted, only counting the
   * main syntactic categories (aka parts of speech).
   */
  static final int MAX_SEGMENT_LENGTH = 6;
  static Pattern[] patterns = new Pattern[] {
      // very good food
      new Pattern(new Part[] { Part.ADV, Part.ADJ, Part.NOUN }, new int[] { 1,
          1, 1 }),
      // good food (same as the previous one but without adv)
      new Pattern(new Part[] { Part.ADJ, Part.NOUN }, new int[] { 1, 1 }),
      // easy to push
      new Pattern(new Part[] { Part.ADJ, Part.TO, Part.VERB, Part.NOUN },
          new int[] { 1, 1, 1, 0 }),
      // food is good, table looks great
      new Pattern(new Part[] { Part.NOUN, Part.VERB, Part.ADV, Part.ADJ },
          new int[] { 1, 1, 0, 1 }),
      // looks incredibly good
      new Pattern(new Part[] { Part.VERB, Part.ADV, Part.ADJ }, new int[] { 1,
          0, 1 }) };

  /**
   * Returns all extraction patterns in the given sentence.
   * 
   * @param sentence
   * @return
   */
  public HashMap<List<TaggedWord>, Pattern> extractPatterns(
      ArrayList<TaggedWord> sentence) {
    HashMap<List<TaggedWord>, Pattern> map = new HashMap<List<TaggedWord>, Pattern>();
    int currentPos = 0;
    while (currentPos < sentence.size()) {
      TaggedWord firstWord = sentence.get(currentPos);
      boolean posChanged = false;
      for (Pattern pattern : patterns) {
        Part part = Pattern.tagToPart(firstWord.tag());
        if (part == pattern.syntacticParts[0]) {
          int end = match(sentence, currentPos, pattern, 0,
              pattern.minAppearance[0]);
          // once a pattern is matched, the matched string cannot be used to
          // match any other patterns
          if (end >= 0) {
            map.put(sentence.subList(currentPos, end), pattern);
            currentPos = end;
            posChanged = true;
          }
        }
      }
      if (!posChanged) {
        currentPos++;
      }
    }

    return map;
  }

  /**
   * Matches the current word in a sentence (given by its index
   * <code>wordIdx</code>) against the current constituent part in a pattern
   * (given by its index <code>pattIdx</code>) for at least <code>min</code>
   * time.
   * 
   * @param sentence
   *          a tagged sentence
   * @param wordIdx
   *          index of the current word
   * @param pattern
   *          the matching pattern
   * @param pattIdx
   *          index of the constituent part being matched
   * @param min
   *          minimum number of appearances of the constituent part that must be
   *          matched
   * @return index of the word whose immediately preceding word matches the last
   *         pattern's part, -1 if no match was found
   */
  private int match(ArrayList<TaggedWord> sentence, int wordIdx,
      Pattern pattern, int pattIdx, int min) {
    if (wordIdx == sentence.size()) {
      return -1;
    }
    // all constituent parts have been matched
    if (pattIdx == pattern.syntacticParts.length) {
      return wordIdx;
    }
    Part part = Pattern.tagToPart(sentence.get(wordIdx).tag());
    // determiners and prepositions are 'attached' to nouns without
    // explicitly matching with the pattern
    if (pattern.syntacticParts[pattIdx] == Part.NOUN
        && (part == Part.DT || part == Part.IN)) {
      return match(sentence, wordIdx + 1, pattern, pattIdx, min);
    }
    /*
     * Matching is performed recursively: if current word matches the syntactic
     * part, keep matching the current part. otherwise if min appearance was not
     * reached, the pattern cannot be matched; else match the next constituent
     * part.
     */
    if (part == pattern.syntacticParts[pattIdx]) {
      return match(sentence, wordIdx + 1, pattern, pattIdx, min - 1);
    } else if (min <= 0) {
      if (pattIdx == pattern.syntacticParts.length - 1) {
        return wordIdx;
      } else {
        return match(sentence, wordIdx, pattern, pattIdx + 1,
            pattern.minAppearance[pattIdx + 1]);
      }
    }

    return -1;
  }

  /**
   * An extraction pattern.
   */
  public static final class Pattern {
    static final String nounTag = "NN";
    static final String advTag = "RB";
    static final String adjTag = "JJ";
    static final String verbTag = "VB";
    static final String determinerTag = "DT";
    static final String prepositionTag = "IN";
    static final String toTag = "TO";

    /**
     * The constituent parts.
     */
    public enum Part {
      NOUN("noun"), ADJ("adj"), ADV("adv"), VERB("verb"), TO("to"), UNKNOWN(
          "unknown"), DT("dt"), IN("prep");

      private String representation;

      Part(String s) {
        this.representation = s;
      }

      public String representation() {
        return representation;
      }
    };

    Part[] syntacticParts;
    int[] minAppearance;

    /**
     * Constructor
     * 
     * @param syntacticParts
     *          the consecutive constituent parts of this pattern (represented
     *          by the part-of-speech tags specified by the PenTree bank)
     * @param minAppearance
     *          the minimum appearance of the corresponding constituent part
     */
    public Pattern(Part[] syntacticParts, int[] minAppearance) {
      this.syntacticParts = syntacticParts;
      this.minAppearance = minAppearance;
    }

    /**
     * Returns the corresponding part of the given tag.
     * 
     * @param tag
     * @return
     */
    public static Part tagToPart(String tag) {
      if (tag.startsWith(adjTag)) {
        return Part.ADJ;
      }
      if (tag.startsWith(advTag)) {
        return Part.ADV;
      }
      if (tag.startsWith(nounTag)) {
        return Part.NOUN;
      }
      if (tag.startsWith(verbTag)) {
        return Part.VERB;
      }
      if (tag.equals(toTag)) {
        return Part.TO;
      }
      if (tag.equals(determinerTag)) {
        return Part.DT;
      }
      if (tag.equals(prepositionTag)) {
        return Part.IN;
      }

      return Part.UNKNOWN;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      for (int idx = 0; idx < syntacticParts.length; idx++) {
        builder.append(syntacticParts[idx].representation());
        if (minAppearance[idx] == 0) {
          builder.append("* ");
        } else {
          builder.append(" ");
        }
      }

      return builder.toString();
    }
  }

  public static void main(String args[]) throws IOException {
    SegmentExtractor extractor = new SegmentExtractor();
    MaxentTagger tagger = MaxentTaggerSingleton.getInstance();
    HashMap<String, ArrayList<Review>> reviewMap = BSUtils.readReviews(
        "C:/datasets/models/bs/coffeemaker/docs.txt",
        new ReviewWithProsAndConsReader());
    for (ArrayList<Review> reviews : reviewMap.values()) {
      for (Review review : reviews) {
        List<ArrayList<? extends HasWord>> tSentences = MaxentTagger
            .tokenizeText(new BufferedReader(new StringReader(review
                .getContent())));
        for (ArrayList<? extends HasWord> tSentence : tSentences) {
          ArrayList<TaggedWord> sentence = tagger.tagSentence(tSentence);
          HashMap<List<TaggedWord>, Pattern> map = extractor
              .extractPatterns(sentence);
          System.out.print("\n\nSentence: " + sentence);
          for (Entry<List<TaggedWord>, Pattern> entry : map.entrySet()) {
            System.out.printf("\n\t%s\t%s", entry.getValue(), entry.getKey());
          }
        }
      }
    }
  }
}
