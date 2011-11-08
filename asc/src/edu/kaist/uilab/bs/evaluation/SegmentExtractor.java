package edu.kaist.uilab.bs.evaluation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.aliasi.tokenizer.TokenizerFactory;

import edu.kaist.uilab.asc.data.Review;
import edu.kaist.uilab.asc.data.ReviewWithProsAndConsReader;
import edu.kaist.uilab.asc.util.TextFiles;
import edu.kaist.uilab.bs.CorpusParserWithTagger.BSTokenizerFactory;
import edu.kaist.uilab.bs.MaxentTaggerSingleton;
import edu.kaist.uilab.bs.evaluation.SegmentExtractor.Pattern.Part;
import edu.kaist.uilab.bs.util.BSUtils;
import edu.kaist.uilab.bs.util.DocumentUtils;
import edu.kaist.uilab.stemmers.EnglishStemmer;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Class that extracts candidate text segments for summary from a sentence
 * according to pre-defined patterns.
 * 
 * @author trung
 */
public class SegmentExtractor {

  /**
   * Pre-defined extraction patterns for services.
   */
  public static final Pattern[] SERVICE_PATTERNS = new Pattern[] {
      // 1. n vb adv* adj n
      // coffee maker makes great coffee
      new Pattern(new Part[] { Part.NOUN, Part.ADV, Part.VERB, Part.DT,
          Part.ADV, Part.ADJ, Part.NOUN }, new int[] { 1, 0, 1, 0, 0, 1, 1 }),

      // 1'. (prp) vb adv* adj n
      // it makes great coffee / appreciate the stainless steel
      new Pattern(new Part[] { Part.VERB, Part.DT, Part.ADV, Part.ADJ,
          Part.NOUN }, new int[] { 1, 0, 0, 1, 1 }),

      // negation of 1' for regular verb
      new Pattern(new Part[] { Part.NOT, Part.VERB, Part.DT, Part.ADV,
          Part.ADJ, Part.NOUN }, new int[] { 1, 1, 0, 0, 1, 1 }),
      // negation of 1' for to be
      new Pattern(new Part[] { Part.VERB, Part.NOT, Part.DT, Part.ADV,
          Part.ADJ, Part.NOUN }, new int[] { 1, 1, 0, 0, 1, 1 }),

      // 3. n vb adv* adj
      // food is good, table looks great
      new Pattern(new Part[] { Part.NOUN, Part.VERB, Part.ADV, Part.ADJ },
          new int[] { 1, 1, 0, 1 }),
      // because we separate 'not' and other adverbs
      new Pattern(new Part[] { Part.NOUN, Part.VERB, Part.NOT, Part.ADJ },
          new int[] { 1, 1, 0, 1 }),
      // 3' (prp) vb adv* adj
      new Pattern(new Part[] { Part.VERB, Part.ADV, Part.ADJ }, new int[] { 1,
          0, 1 }),

      // 5. adv* adj n
      // very good food
      new Pattern(new Part[] { Part.ADV, Part.ADJ, Part.NOUN }, new int[] { 1,
          1, 1 }),
      // 5'. good food
      new Pattern(new Part[] { Part.ADJ, Part.NOUN }, new int[] { 1, 1 }),
      // negation of 5
      new Pattern(new Part[] { Part.NOT, Part.ADV, Part.ADJ, Part.NOUN },
          new int[] { 1, 1, 1, 1 }),

  };

  /**
   * Pre-defined extraction patterns for products.
   */
  public static final Pattern[] PRODUCT_PATTERNS = new Pattern[] {
      // 1. n vb adv* adj n
      // coffee maker makes great coffee
      new Pattern(new Part[] { Part.NOUN, Part.VERB, Part.DT, Part.ADV,
          Part.ADJ, Part.NOUN }, new int[] { 1, 1, 0, 0, 1, 1 }),

      // 1'. (prp) vb adv* adj n
      // it makes great coffee / appreciate the stainless steel
      new Pattern(new Part[] { Part.VERB, Part.DT, Part.ADV, Part.ADJ,
          Part.NOUN }, new int[] { 1, 0, 0, 1, 1 }),

      // negation of 1' for regular verb
      new Pattern(new Part[] { Part.NOT, Part.VERB, Part.DT, Part.ADV,
          Part.ADJ, Part.NOUN }, new int[] { 1, 1, 0, 0, 1, 1 }),
      // negation of 1' for to be
      new Pattern(new Part[] { Part.VERB, Part.NOT, Part.DT, Part.ADV,
          Part.ADJ, Part.NOUN }, new int[] { 1, 1, 0, 0, 1, 1 }),

      // 2. n vb adv* adj to vb
      // coffee maker is hard to use
      new Pattern(new Part[] { Part.NOUN, Part.VERB, Part.ADV, Part.ADJ,
          Part.TO, Part.VERB }, new int[] { 1, 1, 0, 1, 1, 1 }),
      // 2'. (prp) vb adv* adj to vb
      // is difficult to use
      new Pattern(new Part[] { Part.VERB, Part.ADV, Part.ADJ, Part.TO,
          Part.VERB }, new int[] { 1, 0, 1, 1, 1 }),

      // negation of 2
      new Pattern(new Part[] { Part.NOUN, Part.VERB, Part.NOT, Part.ADV,
          Part.ADJ, Part.TO, Part.VERB }, new int[] { 1, 1, 1, 0, 1, 1, 1 }),
      // negation of 2'
      new Pattern(new Part[] { Part.VERB, Part.NOT, Part.ADV, Part.ADJ,
          Part.TO, Part.VERB }, new int[] { 1, 1, 0, 1, 1, 1 }),

      // 3. n vb adv* adj
      // food is good, table looks great
      new Pattern(new Part[] { Part.NOUN, Part.VERB, Part.ADV, Part.ADJ },
          new int[] { 1, 1, 0, 1 }),
      // because we separate 'not' and other adverbs
      new Pattern(new Part[] { Part.NOUN, Part.VERB, Part.NOT, Part.ADJ },
          new int[] { 1, 1, 0, 1 }),

      // 4. adv* adj to vb n*
      // very easy to push
      new Pattern(new Part[] { Part.ADV, Part.ADJ, Part.TO, Part.VERB,
          Part.NOUN }, new int[] { 1, 1, 1, 1, 0 }),
      new Pattern(new Part[] { Part.ADJ, Part.TO, Part.VERB, Part.NOUN },
          new int[] { 1, 1, 1, 0 }),
      // negation of 4
      new Pattern(new Part[] { Part.NOT, Part.ADJ, Part.TO, Part.VERB,
          Part.NOUN }, new int[] { 1, 1, 1, 1, 0 }),

      // 5. adv* adj n
      // very good food
      new Pattern(new Part[] { Part.ADV, Part.ADJ, Part.NOUN }, new int[] { 1,
          1, 1 }),
      new Pattern(new Part[] { Part.NOT, Part.ADV, Part.ADJ, Part.NOUN },
          new int[] { 1, 1, 1, 1 }), };

  public static final Pattern[] ALL_PATTERNS = new Pattern[] {
      // 1. n vb adv* adj n
      // coffee maker makes great coffee
      new Pattern(new Part[] { Part.NOUN, Part.VERB, Part.DT, Part.ADV,
          Part.ADJ, Part.NOUN }, new int[] { 1, 1, 0, 0, 1, 1 }),

      // 1'. (prp) vb adv* adj n
      // it makes great coffee / appreciate the stainless steel
      new Pattern(new Part[] { Part.VERB, Part.DT, Part.ADV, Part.ADJ,
          Part.NOUN }, new int[] { 1, 0, 0, 1, 1 }),

      // negation of 1' for regular verb
      new Pattern(new Part[] { Part.NOT, Part.VERB, Part.DT, Part.ADV,
          Part.ADJ, Part.NOUN }, new int[] { 1, 1, 0, 0, 1, 1 }),
      // negation of 1' for to be
      new Pattern(new Part[] { Part.VERB, Part.NOT, Part.DT, Part.ADV,
          Part.ADJ, Part.NOUN }, new int[] { 1, 1, 0, 0, 1, 1 }),

      // 2. n vb adv* adj to vb
      // coffee maker is hard to use
      new Pattern(new Part[] { Part.NOUN, Part.VERB, Part.ADV, Part.ADJ,
          Part.TO, Part.VERB }, new int[] { 1, 1, 0, 1, 1, 1 }),
      // 2'. (prp) vb adv* adj to vb
      // is difficult to use
      new Pattern(new Part[] { Part.VERB, Part.ADV, Part.ADJ, Part.TO,
          Part.VERB }, new int[] { 1, 0, 1, 1, 1 }),

      // negation of 2
      new Pattern(new Part[] { Part.NOUN, Part.VERB, Part.NOT, Part.ADV,
          Part.ADJ, Part.TO, Part.VERB }, new int[] { 1, 1, 1, 0, 1, 1, 1 }),
      // negation of 2'
      new Pattern(new Part[] { Part.VERB, Part.NOT, Part.ADV, Part.ADJ,
          Part.TO, Part.VERB }, new int[] { 1, 1, 0, 1, 1, 1 }),

      // 3. n vb adv* adj
      // food is good, table looks great
      new Pattern(new Part[] { Part.NOUN, Part.VERB, Part.ADV, Part.ADJ },
          new int[] { 1, 1, 0, 1 }),
      // because we separate 'not' and other adverbs
      new Pattern(new Part[] { Part.NOUN, Part.VERB, Part.NOT, Part.ADJ },
          new int[] { 1, 1, 0, 1 }),
      // 3' (prp) vb adv* adj
      new Pattern(new Part[] { Part.VERB, Part.ADV, Part.ADJ }, new int[] { 1,
          0, 1 }),

      // 4. adv* adj to vb n*
      // very easy to push
      new Pattern(new Part[] { Part.ADV, Part.ADJ, Part.TO, Part.VERB,
          Part.NOUN }, new int[] { 1, 1, 1, 1, 0 }),
      new Pattern(new Part[] { Part.ADJ, Part.TO, Part.VERB, Part.NOUN },
          new int[] { 1, 1, 1, 0 }),
      // negation of 4
      new Pattern(new Part[] { Part.NOT, Part.ADJ, Part.TO, Part.VERB,
          Part.NOUN }, new int[] { 1, 1, 1, 1, 0 }),

      // 5. adv* adj n
      // very good food
      new Pattern(new Part[] { Part.ADV, Part.ADJ, Part.NOUN }, new int[] { 1,
          1, 1 }),
      // 5'. good food
      new Pattern(new Part[] { Part.ADJ, Part.NOUN }, new int[] { 1, 1 }),
      // negation of 5
      new Pattern(new Part[] { Part.NOT, Part.ADV, Part.ADJ, Part.NOUN },
          new int[] { 1, 1, 1, 1 }), };

  static final String STOP_STEMS = "C:/datasets/models/bs/stop.txt";
  static final EnglishStemmer stemmer = new EnglishStemmer();
  static TokenizerFactory tokenizer = BSTokenizerFactory
      .getInstance(STOP_STEMS);
  static final MaxentTagger tagger = MaxentTaggerSingleton.getInstance();

  HashMap<String[], String> segmentToTextMap = new HashMap<String[], String>();

  /**
   * Returns the mapping between a segment (a sequence of words) and its
   * original text string (the one upon which no stemming or tokenizing has been
   * performed).
   * 
   * @return
   */
  public HashMap<String[], String> getSegmentToTextMap() {
    return segmentToTextMap;
  }

  /**
   * Returns all extraction patterns in the given sentence.
   * 
   * @param patterns
   * @param sentence
   * @param maxLength
   *          maximum length of extracted fragments
   * @param fromPattIdx
   * @param toPattIdx
   * @return
   */
  public HashMap<List<TaggedWord>, Pattern> extractPatterns(Pattern[] patterns,
      ArrayList<TaggedWord> sentence, int maxLength, int fromPattIdx,
      int toPattIdx) {
    HashMap<List<TaggedWord>, Pattern> map = new HashMap<List<TaggedWord>, Pattern>();
    int currentPos = 0;
    while (currentPos < sentence.size()) {
      TaggedWord firstWord = sentence.get(currentPos);
      boolean posChanged = false;
      for (int idx = fromPattIdx; idx <= toPattIdx; idx++) {
        Pattern pattern = patterns[idx];
        Part part = Pattern.tagToPart(firstWord.tag(), firstWord.word());
        if (part == pattern.syntacticParts[0]) {
          int end = match(sentence, currentPos, pattern, 0,
              pattern.minAppearance[0]);
          if (end >= 0 && end - currentPos <= maxLength) {
            // remove non-meaningful parts (dt, prep)
            while (!Pattern.isSemanticPart(sentence.get(end - 1).tag())) {
              end -= 1;
            }
            // to be at the beginning of the phrase can be removed
            if (BSUtils
                .isInArray(Pattern.TOBE, sentence.get(currentPos).word())) {
              currentPos += 1;
            }
            // once a pattern is matched, the matched string cannot be used to
            // match any other patterns
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
   * Returns true if the given <code>tag</code> matches the last syntactic part
   * of the given <code>pattern</code>.
   * 
   * @param tag
   * @param pattern
   * @return
   */
  boolean matchLastPattern(String tag, Pattern pattern) {
    return pattern.syntacticParts[pattern.syntacticParts.length - 1]
        .equals(Pattern.tagToPart(tag, ""));
  }

  /**
   * Returns all text fragments in the given sentence that matches one of the
   * extraction patterns.
   * 
   * @param sentence
   * @param maxLength
   * @param fromPattIdx
   *          index of the first pattern to use
   * @param toPattIdx
   *          index of the last pattern to use
   * @return
   */
  public List<List<TaggedWord>> extractSegments(Pattern[] patterns,
      ArrayList<TaggedWord> sentence, int maxLength, int fromPattIdx,
      int toPattIdx) {
    HashMap<List<TaggedWord>, Pattern> map = extractPatterns(patterns,
        sentence, maxLength, fromPattIdx, toPattIdx);
    List<List<TaggedWord>> list = new ArrayList<List<TaggedWord>>();
    for (List<TaggedWord> fragment : map.keySet()) {
      list.add(fragment);
    }

    return list;
  }

  /**
   * Returns all text fragments in the given sentence that matches one of the
   * extraction patterns.
   * <p>
   * The words returned are all in lowercase and are stemmed.
   * <p>
   * This method works in the same way as
   * {@link #extractSegments(ArrayList, int)} except that each returned segment
   * is represented as an array instead of a list of tagged words.
   * 
   * @param sentence
   * @param maxLength
   * @param fromPattIdx
   *          index of the first pattern to use
   * @param toPattIdx
   *          index of the last pattern to use
   * @return
   */
  public List<String[]> extractSegmentsAsArrays(Pattern[] patterns,
      ArrayList<TaggedWord> sentence, int maxLength, int fromPattIdx,
      int toPattIdx) {
    List<List<TaggedWord>> list = extractSegments(patterns, sentence,
        maxLength, fromPattIdx, toPattIdx);
    List<String[]> ret = new ArrayList<String[]>();
    for (List<TaggedWord> words : list) {
      ret.add(taggedWordsToArray(words));
    }

    return ret;
  }

  /**
   * Converts a list of tagged words into an array of words.
   * 
   * @param words
   * @return
   */
  private String[] taggedWordsToArray(List<TaggedWord> words) {
    StringBuilder builder = new StringBuilder();
    for (TaggedWord word : words) {
      builder.append(word.word()).append(" ");
    }
    String text = builder.toString();
    String[] segment = DocumentUtils.tokenizeAndStem(text, tokenizer, stemmer);
    segmentToTextMap.put(segment, text);

    return segment;
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
    Part part = Pattern.tagToPart(sentence.get(wordIdx).tag(),
        sentence.get(wordIdx).word());
    // determiners and prepositions are 'attached' to noun without
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
    static final String[] TOBE = new String[] { "is", "am", "are", "be",
        "been", "was", "were", "'s", "'re", "'m" };

    /**
     * The constituent parts.
     */
    public enum Part {
      NOUN("noun"), ADJ("adj"), ADV("adv"), VERB("verb"), TO("to"), UNKNOWN(
          "unknown"), DT("dt"), IN("prep"), NOT("not");

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
     * Returns true if the given tag is not one of the functional tags including
     * determiners, prepositions, and conjunctions.
     * 
     * @param tag
     * @return
     */
    public static boolean isSemanticPart(String tag) {
      return (!tag.equals(prepositionTag) && !tag.equals(determinerTag));
    }

    /**
     * Returns the corresponding part of the given tag.
     * 
     * @param tag
     * @return
     */
    public static Part tagToPart(String tag, String word) {
      if (tag.startsWith(adjTag)) {
        return Part.ADJ;
      }
      if (tag.startsWith(advTag)) {
        if (word.equals("not")) {
          return Part.NOT;
        } else {
          return Part.ADV;
        }
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
      if (tag.equals(prepositionTag)
          && !(word.equals("as") || word.equals("if"))) {
        // 'as' and 'if' is not part of a noun group/phrase
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

  static void writeAllExtractedPatterns(Pattern[] patterns, String dataset)
      throws IOException {
    SegmentExtractor extractor = new SegmentExtractor();
    MaxentTagger tagger = MaxentTaggerSingleton.getInstance();
    HashMap<String, ArrayList<Review>> reviewMap = BSUtils.readReviews(dataset,
        new ReviewWithProsAndConsReader());
    HashMap<Pattern, List<List<TaggedWord>>> map = new HashMap<Pattern, List<List<TaggedWord>>>();
    for (Pattern patt : patterns) {
      map.put(patt, new ArrayList<List<TaggedWord>>());
    }
    for (ArrayList<Review> reviews : reviewMap.values()) {
      for (Review review : reviews) {
        List<ArrayList<? extends HasWord>> tSentences = MaxentTagger
            .tokenizeText(new BufferedReader(new StringReader(review
                .getContent())));
        for (ArrayList<? extends HasWord> tSentence : tSentences) {
          ArrayList<TaggedWord> sentence = tagger.tagSentence(tSentence);
          HashMap<List<TaggedWord>, Pattern> patts = extractor.extractPatterns(
              patterns, sentence, 7, 0, patterns.length - 1);
          for (Entry<List<TaggedWord>, Pattern> entry : patts.entrySet()) {
            map.get(entry.getValue()).add(entry.getKey());
          }
        }
      }
    }
    for (int i = 0; i < patterns.length; i++) {
      Pattern patt = patterns[i];
      TextFiles.writeCollection(map.get(patt), "patt" + i + ".txt", "utf-8");
    }
  }

  static void countPatternOccurences(String dataset) throws IOException {
    int[][] pairs = new int[][] { { 0, 3 }, { 4, 7 }, { 8, 10 }, { 11, 13 },
        { 14, 16 } };
    int[] cnt = new int[pairs.length];
    SegmentExtractor extractor = new SegmentExtractor();
    MaxentTagger tagger = MaxentTaggerSingleton.getInstance();
    HashMap<String, ArrayList<Review>> entityMap = BSUtils.readReviews(dataset,
        new ReviewWithProsAndConsReader());
    for (int idx = 0; idx < pairs.length; idx++) {
      for (Entry<String, ArrayList<Review>> entry : entityMap.entrySet()) {
        for (Review review : entry.getValue()) {
          List<ArrayList<? extends HasWord>> sentences = MaxentTagger
              .tokenizeText(new BufferedReader(new StringReader(review
                  .getContent())));
          for (ArrayList<? extends HasWord> tSentence : sentences) {
            cnt[idx] += extractor.extractSegmentsAsArrays(ALL_PATTERNS,
                tagger.tagSentence(tSentence), 7, pairs[idx][0], pairs[idx][1])
                .size();
          }
        }
      }
    }
    for (int idx = 0; idx < pairs.length; idx++) {
      System.out.println(cnt[idx]);
    }
  }

  public static void main(String args[]) throws IOException {
//    String dataset = "C:/datasets/models/bs/ursa/docs.txt";
//    countPatternOccurences(dataset);
    Pattern[] patterns = ALL_PATTERNS;
    // writeAllExtractedPatterns(patterns, dataset);

    SegmentExtractor extractor = new SegmentExtractor();
    MaxentTagger tagger = MaxentTaggerSingleton.getInstance();
    List<ArrayList<? extends HasWord>> tSentences = MaxentTagger
        .tokenizeText(new BufferedReader(
            new StringReader(
                "am saving a ton of money")));
    for (ArrayList<? extends HasWord> tSentence : tSentences) {
      ArrayList<TaggedWord> sentence = tagger.tagSentence(tSentence);
      System.out.println(sentence);
      List<String[]> extractions = extractor.extractSegmentsAsArrays(patterns,
          sentence, 5, 0, patterns.length - 1);
      for (String[] extraction : extractions) {
        System.out.printf("\n%s", BSUtils.arrayToString(extraction, " "));
      }
      HashMap<List<TaggedWord>, Pattern> patts = extractor.extractPatterns(
          patterns, sentence, 5, 0, patterns.length - 1);
      for (Entry<List<TaggedWord>, Pattern> entry : patts.entrySet()) {
        System.out.printf("\n\t%s\t%s", entry.getValue(), entry.getKey());
      }
    }
  }
}
