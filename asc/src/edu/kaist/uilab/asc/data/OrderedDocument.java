package edu.kaist.uilab.asc.data;

import java.util.Vector;

/**
 * Document represented as bag of sentences.
 */
public class OrderedDocument extends Document {

  private static final long serialVersionUID = -5236933567248150582L;
  private Vector<Sentence> sentences;

  public OrderedDocument() {
    super();
    sentences = new Vector<Sentence>();
  }

  public void addWord(Word word) {
    super.addWord(word);
    sentences.lastElement().addWord(word);
  }

  public void addWord(int wordNo) {
    addWord(new Word(wordNo));
  }

  public void addSentence(Sentence sentence) {
    sentences.add(sentence);
    for (Word word : sentence.getWords())
      words.add(word);
  }

  public Vector<Sentence> getSentences() {
    return sentences;
  }
}
