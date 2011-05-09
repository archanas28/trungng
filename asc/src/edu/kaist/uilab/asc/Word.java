package edu.kaist.uilab.asc;

class Word implements Comparable<Word> {
  public String value = null;
  public String tag = null;
  public String originalWord = null;
  
  public Word(String word) {
    this.value = new String(word);
    this.originalWord = new String(word);
  }
  
  public Word(String word, String tag) {
    this.value = new String(word);
    this.originalWord = new String(word);
    if (tag != null) this.tag = new String(tag);
  }

  public int compareTo(Word o) {
    int ret = this.value.compareTo(o.value);
    if (ret != 0) return ret;
    else {
      if (this.tag != null && o.tag != null) return this.tag.compareTo(o.tag);
      else if (this.tag != null && o.tag == null) return 1;
      else if (this.tag == null && o.tag != null) return -1;
      else return 0;
    }
  }
  
  public boolean equals(Word o) {
    return this.value == o.value && this.tag == o.tag;
  }
  
  public boolean hasTag() {
    return this.tag != null;
  }
  
  public String toString() {
    return "Word: "+value+", Tag: "+tag;
  }
}
