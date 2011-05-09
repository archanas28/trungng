package edu.kaist.uilab.asc.asum;

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
  private String model = "D:/java-libs/stanford-postagger-2010-05-26/models/left3words-wsj-0-18.tagger";
  private MaxentTagger tagger;
  
  public StanfordPOSTagger() throws Exception {
     tagger = new MaxentTagger(this.model);
  }
  
  public StanfordPOSTagger(String model) throws Exception{
     tagger = new MaxentTagger(model);
  }
  
//  public List<Sentence<TaggedWord>> posTaggingTextFile(File file) throws Exception{
//    List<Sentence<TaggedWord>> tSentences = new Vector<Sentence<TaggedWord>>();
//      List<Sentence<? extends HasWord>> sentences = MaxentTagger.tokenizeText(new BufferedReader(new FileReader(file)));
//      for (Sentence<? extends HasWord> sentence : sentences) {
//        Sentence<TaggedWord> tSentence = MaxentTagger.tagSentence(sentence);
//        tSentences.add(tSentence);
//      }
//      return tSentences;
//  }
  
  public List<String> tag(String document) {
    Vector<String> sentenceList = new Vector<String>();
    List<ArrayList<? extends HasWord>> sentences = MaxentTagger.tokenizeText(
        new BufferedReader(new StringReader(document)));
    for (ArrayList<? extends HasWord> sentence : sentences) {
      ArrayList<TaggedWord> tSentence = tagger.tagSentence(sentence);
//      for (TaggedWord tWord : tSentence) {
//        tWord.word();
//        tWord.tag();
//      }
      sentenceList.add(Sentence.listToString(tSentence, false));
    }
    return sentenceList;
  }
}