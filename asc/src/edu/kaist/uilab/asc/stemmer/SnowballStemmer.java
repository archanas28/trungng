package edu.kaist.uilab.asc.stemmer;

public abstract class SnowballStemmer extends SnowballProgram {
  public abstract boolean stem();
  
  public String getStem(String s) {
    setCurrent(s);
    stem();
    return getCurrent();
  }
};
