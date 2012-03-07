package edu.kaist.uilab.asc;

import java.io.Serializable;
import java.util.Locale;

/**
 * A locale word contains its string value and its locale (i.e, the language).
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class LocaleWord implements Comparable<LocaleWord>, Serializable {
  private static final long serialVersionUID = 1L;

  String mValue;
  Locale mLocale;

  /**
   * Creates a word in the specified locale.
   * 
   * @param word
   * @param locale
   */
  public LocaleWord(String word, Locale locale) {
    mValue = word;
    mLocale = locale;
  }

  /**
   * Creates a word with its locale extracted from the given
   * <code>localeWord</code>.
   * 
   * @param localeWord
   */
  public LocaleWord(String localeWord) {
    int pos = localeWord.indexOf("(");
    mValue = localeWord.substring(0, pos);
    mLocale = new Locale(localeWord.substring(pos + 1, localeWord.length() - 1));
  }

  /**
   * Returns the string value of this word without the locale information.
   * 
   * @return
   */
  public String getValue() {
    return mValue;
  }

  @Override
  public int compareTo(LocaleWord o) {
    int ret = mValue.compareTo(o.mValue);
    if (ret == 0) {
      return mLocale.getLanguage().compareTo(o.mLocale.getLanguage());
    }
    return ret;
  }

  @Override
  public boolean equals(Object o) {
    LocaleWord word = (LocaleWord) o;
    return mValue.equals(word.mValue) && mLocale.equals(word.mLocale);
  }

  @Override
  public String toString() {
    return mValue + "(" + mLocale + ")";
  }
}
