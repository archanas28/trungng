package edu.kaist.uilab.asc.data;

import java.io.Serializable;
import java.util.Vector;

/**
 * Document represented as a bag of words.
 */
public class Document implements Serializable {

  private static final long serialVersionUID = -2633043202773898814L;

  private int docNo;

  protected Vector<Word> words;

  public Document() {
    this.words = new Vector<Word>();
  }

  public Document(int docLength) {
    this.words = new Vector<Word>(docLength);
  }

  public int getDocNo() {
    return docNo;
  }

  public void setDocNo(int docNo) {
    this.docNo = docNo;
  }

  public void addWord(Word word) {
    words.add(word);
  }

  public void addWord(int wordNo) {
    addWord(new Word(wordNo));
  }

  public int getLength() {
    return words.size();
  }

  public int getNumWords() {
    return words.size();
  }

  public Vector<Word> getWords() {
    return words;
  }

  public void setWordsList(Vector<Word> wordsList) {
    this.words = wordsList;
  }
}
