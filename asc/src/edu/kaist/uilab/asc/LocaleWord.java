package edu.kaist.uilab.asc;

import java.io.Serializable;
import java.util.Locale;

/**
 * A locale word contains its string value and its locale (the language).
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
class LocaleWord implements Comparable<LocaleWord>, Serializable {
  private static final long serialVersionUID = 1L;
  
  static final int ENGLISH = 1;
  static final int FRENCH = 2;
  static final int GERMAN = 3;
  
  String mValue;
  int mLocale;
  
  /**
   * Creates a word in the specified locale.
   * 
   * @param word
   * @param locale
   */
  public LocaleWord(String word, Locale locale) {
    mValue = word;
    setLocale(locale);
  }
  
  void setLocale(Locale locale) {
    if (locale.equals(Locale.ENGLISH)) {
      mLocale = ENGLISH;
    } else if (locale.equals(Locale.FRENCH)) {
      mLocale = FRENCH;
    } else if (locale.equals(Locale.GERMAN)) {
      mLocale = GERMAN;
    }
  }
  
  /**
   * Creates a word with its locale extracted from the given <code>localeWord</code>.
   * @param localeWord
   */
  public LocaleWord(String localeWord) {
    int pos = localeWord.indexOf("_");
    mValue = localeWord.substring(pos + 1);
    setLocale(new Locale(localeWord.substring(0, pos)));
  }
  
  @Override
  public int compareTo(LocaleWord o) {
    int ret = mValue.compareTo(o.mValue);
    if (ret == 0) {
      ret = mLocale - o.mLocale;
    }
    return ret;
  }
  
  @Override
  public boolean equals(Object o) {
    LocaleWord word = (LocaleWord) o;
    return mValue.equals(word.mValue) && mLocale == word.mLocale;
  }
  
  @Override
  public String toString() {
    return mLocale + "_" + mValue;
  }
}
