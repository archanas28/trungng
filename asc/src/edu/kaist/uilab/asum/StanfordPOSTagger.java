package edu.kaist.uilab.asum;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class StanfordPOSTagger {
  private String model = "D:/java-libs/stanford-postagger-2010-05-26/models/bidirectional-distsim-wsj-0-18.tagger";
  private MaxentTagger tagger;

  public StanfordPOSTagger() throws Exception {
    tagger = new MaxentTagger(model);
  }

  public StanfordPOSTagger(String model) throws Exception {
    tagger = new MaxentTagger(model);
  }

  public List<String> tag(String document) {
    Vector<String> sentenceList = new Vector<String>();
    List<ArrayList<? extends HasWord>> sentences = tagger.tokenizeText(new BufferedReader(new StringReader(document)));
    for (ArrayList<? extends HasWord> sentence : sentences) {
      ArrayList<TaggedWord> tSentence = tagger.tagSentence(sentence);
      for (TaggedWord tWord : tSentence) {
        System.out.print(tWord.word() + "<" + tWord.tag() + ">,");
      }
      sentenceList.add(Sentence.listToString(tSentence, false));
      System.out.println();
    }
    return sentenceList;
  }

  public static void main(String args[]) throws Exception {
    StanfordPOSTagger tagger = new StanfordPOSTagger();
    tagger
        .tag("After comparing multiple products from several different sellers, this seemed like the ideal choice.  When the item arrived (via UPS), the box was pretty beaten up.  When unpacked, it seemed fine and worked on the regular setting.  However when I added the gallons of water it requires for high-effificiency cooling, the water leaked all over my floor within a few minutes.  After closer examination, I found a cracked part in the base.  Amazon promptly sent a replacement (via FedEx).  The box was in much better shape this time, but I immediately examined the interior of the unit.  Again, the exact same piece of plastic had broken leaving a jagged edge.  Apparently, this is a weak spot that snaps somewhere in transit. The saving grace of this transaction was that Amazon responded quickly and efficiently to resolve my problems.");
  }
}