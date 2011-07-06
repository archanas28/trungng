package edu.kaist.uilab.asc.prior;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import edu.kaist.uilab.stemmers.EnglishStemmer;
import edu.kaist.uilab.stemmers.FrenchStemmer;

/**
 * Dictionary.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Dictionary {
  static final String EN = "en";
  static final String FR = "fr";
  static final String TRANSLATE_API = "https://www.googleapis.com/language/translate/v2?"
      + "key=AIzaSyCjv-p8oZSMwuxOv1Al49pTlDhtNOHZUro&q=";
  static final String TRANSLATE_WEB = "http://translate.google.com/?";
  private static final String UTF8 = "utf-8";

  /**
   * Removes duplicated lines in a text file.
   * 
   * @param file
   * @param newFile
   * @throws IOException
   */
  static void removeDuplication(String file) throws IOException {
    HashSet<String> set = new HashSet<String>();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), UTF8));
    String line;
    while ((line = in.readLine()) != null) {
      set.add(line);
    }
    in.close();
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), UTF8));
    for (String s : set) {
      out.println(s);
    }
    out.close();
  }

  /**
   * Translates words from a source language to a target language.
   * 
   * @deprecated use {@link #make(HashMap, String, String, String, String)}
   *             instead
   * @param srcLangFile
   *          the file that contains all words to be translated each on one line
   * @param srcLang
   *          the source language
   * @param targetLang
   *          the target language
   * @param outputFile
   *          output file to write the translations
   */
  public static void langToLang(String srcLangFile, String srcLang,
      String targetLang, String outputFile) throws IOException {
    PrintWriter out = null;
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(
          new FileInputStream(srcLangFile), UTF8));
      out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
          outputFile, true), UTF8));
      String word, translatedWord;
      HttpClient httpClient = new DefaultHttpClient();
      while ((word = in.readLine()) != null) {
        translatedWord = translatesWord(word, srcLang, targetLang, httpClient);
        if (translatedWord != null) {
          // always write english word first
          if (EN.equals(srcLang)) {
            translatedWord = translatedWord.toLowerCase(Locale.FRENCH).replace(
                "&#39;", "'");
            out.printf("%s\t%s\n", word, translatedWord);
            System.out.println(word + " " + translatedWord);
          } else {
            out.printf("%s\t%s\n", translatedWord.toLowerCase(), word);
            System.out.println(translatedWord + " " + word);
          }
        }
      }
      httpClient.getConnectionManager().shutdown();
      in.close();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  static void getStems(String dictFile, HashSet<String> srcStems,
      HashSet<String> targetStems) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(dictFile), UTF8));
    String line;
    while ((line = in.readLine()) != null) {
      String[] stem = line.split("\t");
      srcStems.add(stem[0]);
      targetStems.add(stem[1]);
    }
    in.close();
  }

  /**
   * Makes a dictionary for the pair of words.
   * <p>
   * New pair of words will be appended to the provided dictionary file.
   * 
   * @param wordCnt
   * @param enStemFile
   *          the stem file (english language) as produced by the parser. Each
   *          line of this file must contains two words separated by space, an
   *          original word and a stemmed word.
   * @param secondStemFile
   *          the stem file (target language) with same format as the
   *          <code>enStemFile</code>
   * @param secondLanguage
   *          language of the second stem file
   * @param dictFile
   *          the dictionary file.
   */
  public static void make(HashMap<String, Integer> wordCnt, String enStemFile,
      String secondStemFile, String secondLanguage, String dictFile)
      throws IOException {
    HashSet<String> enStems = new HashSet<String>();
    HashSet<String> frStems = new HashSet<String>();
    getStems(dictFile, enStems, frStems);
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(dictFile, true), UTF8));
    EnglishStemmer enStemmer = new EnglishStemmer();
    FrenchStemmer frStemmer = new FrenchStemmer();
    // english stem file
    String line, word, stem;
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(enStemFile), UTF8));
    while ((line = in.readLine()) != null) {
      int pos = line.trim().indexOf(" ");
      if (pos <= 0)
        continue;
      word = line.substring(0, pos);
      stem = enStemmer.getStem(word);
//      if (wordCnt.containsKey(stem)) {
      if (wordCnt.containsKey(stem) && !enStems.contains(stem)) {
        wordCnt.remove(stem); // translate a word only once
        boolean negation = false;
        if (word.startsWith("not_")) {
          negation = true;
          word = word.substring(4);
        }
        try {
          ArrayList<String> list = getTranslatedWords(word, EN, secondLanguage);
          for (String translation : list) {
            if (negation) {
              out.printf("%s\tpas_%s\n", stem, frStemmer.getStem(translation));
              System.out.printf("%s\tpas_%s\n", stem,
                  frStemmer.getStem(translation));
            } else {
              out.printf("%s\t%s\n", stem, frStemmer.getStem(translation));
              System.out.printf("%s\t%s\n", stem,
                  frStemmer.getStem(translation));
            }
          }
        } catch (Exception e) {
          System.err.println(e.getMessage());
        }
      }
    }
    in.close();

    // french stem file
    in = new BufferedReader(new InputStreamReader(new FileInputStream(
        secondStemFile), UTF8));
    while ((line = in.readLine()) != null) {
      int pos = line.trim().indexOf(" ");
      if (pos <= 0)
        continue;
      word = line.substring(0, pos);
      stem = frStemmer.getStem(word);
//      if (wordCnt.containsKey(stem)) {
      if (wordCnt.containsKey(stem) && !frStems.contains(stem)) {
        wordCnt.remove(stem); // translate a word only once
        boolean negation = false;
        if (word.startsWith("pas_")) {
          negation = true;
          word = word.substring(4);
        }
        try {
          ArrayList<String> list = getTranslatedWords(word, secondLanguage, EN);
          for (String translation : list) {
            if (negation) {
              out.printf("not_%s\t%s\n", enStemmer.getStem(translation), stem);
              System.out.printf("not_%s\t%s\n", enStemmer.getStem(translation),
                  stem);
            } else {
              out.printf("%s\t%s\n", enStemmer.getStem(translation), stem);
              System.out.printf("%s\t%s\n", enStemmer.getStem(translation),
                  stem);
            }
          }
        } catch (Exception e) {
          System.err.println(e.getMessage());
        }
      }
    }
    in.close();
    out.close();
  }

  /**
   * Returns a map between words and their stems from the specified file.
   * 
   * @param stemFile
   * @return
   * @throws IOException
   */
  @SuppressWarnings("unused")
  private static HashMap<String, String> getStemMap(String stemFile)
      throws IOException {
    HashMap<String, String> map = new HashMap<String, String>();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(stemFile), UTF8));
    String line;
    int pos;
    while ((line = in.readLine()) != null) {
      pos = line.indexOf(" ");
      map.put(line.substring(0, pos), line.substring(pos + 1));
    }
    in.close();
    return map;
  }

  /**
   * Gets the existing English words from the specified dictionary file.
   * <p>
   * The first word of each line in the file is considered the English word with
   * its translation follows.
   * 
   * @param dictFile
   *          the dictionary file
   * @param getEnglish
   *          true to get the English words, false to get the words in the other
   *          language
   * @return
   */
  @SuppressWarnings("unused")
  private static HashSet<String> getExistingWords(String dictFile,
      boolean getEnglish) {
    HashSet<String> set = new HashSet<String>();
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(
          new FileInputStream(dictFile), UTF8));
      String line;
      int tabPosition;
      String word;
      while ((line = in.readLine()) != null) {
        tabPosition = line.indexOf("\t");
        if (getEnglish) {
          word = line.substring(0, tabPosition);
        } else {
          word = line.substring(tabPosition + 1);
        }
        set.add(word);
      }
      in.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return set;
  }

  /**
   * Translates the specified word from <code>src</code> language to
   * <code>target</code> language.
   * 
   * @param word
   * @param src
   * @param target
   * @param client
   * @return
   */
  private static String translatesWord(String word, String src, String target,
      HttpClient client) throws URISyntaxException {
    HttpGet httpget = new HttpGet(TRANSLATE_API + word + "&source=" + src
        + "&target=" + target);
    ResponseHandler<String> responseHandler = new BasicResponseHandler();
    String translatedWord = null;
    try {
      String response = client.execute(httpget, responseHandler);
      int pos = response.indexOf("translatedText") + "translatedText".length()
          + 2;
      int start = response.indexOf("\"", pos);
      int end = response.indexOf("\"", start + 1);
      translatedWord = response.substring(start + 1, end);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
    if (translatedWord != null && !translatedWord.contains(" ")) {
      return translatedWord;
    }
    return null;
  }

  private static ArrayList<String> getTranslatedWords(String word, String sl,
      String tl) {
    ArrayList<String> list = new ArrayList<String>();
    try {
      String url = TRANSLATE_WEB + "sl=" + sl + "&tl=" + tl + "&q="
          + URLEncoder.encode(word, "utf-8");
      Parser parser = new Parser(url);
      NodeList nl = parser.extractAllNodesThatMatch(new HasAttributeFilter(
          "id", "gt-res-dict"));
      if (nl.size() == 1) {
        Node node = nl.elementAt(0);
        nl.removeAll();
        node.collectInto(nl, new TagNameFilter("li"));
        if (nl.size() == 0) {
          // heuristic: if a word appears multiple times but without
          // translation,
          // maybe it is proper noun.
          list.add(word);
        }
        for (int i = 0; i < nl.size(); i++) {
          node = nl.elementAt(i);
          if (node.getChildren().size() == 1) {
            String text = node.getFirstChild().getText();
            if (!text.contains(" ")) {
              list.add(text.replace("&#39;", "'"));
            }
          }
        }
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
    return list;
  }

  /**
   * Gets words in other languages from the results file produced by document
   * parser.
   */
  @SuppressWarnings("unused")
  private static void getWordsInOtherLanguages() throws IOException {
    BufferedReader en = new BufferedReader(new InputStreamReader(
        new FileInputStream("C:/datasets/asc/blogs/obama/WordList_en.txt"),
        UTF8));
    String line;
    HashSet<String> enWords = new HashSet<String>();
    while ((line = en.readLine()) != null) {
      enWords.add(line);
    }
    en.close();
    BufferedReader all = new BufferedReader(new InputStreamReader(
        new FileInputStream("C:/datasets/asc/blogs/obama/WordList.txt"), UTF8));
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream("C:/datasets/asc/blogs/obama/WordList_other.txt",
            true), UTF8));
    while ((line = all.readLine()) != null) {
      if (!enWords.contains(line)) {
        out.println(line);
      }
    }
    all.close();
    out.close();
  }

  /**
   * Gets words whose counts are greater than <code>minCount</code>.
   * 
   * @param file
   * @param minCount
   * @return
   * @throws IOException
   */
  private static HashMap<String, Integer> getWordCount(String file, int minCount)
      throws IOException {
    HashMap<String, Integer> map = new HashMap<String, Integer>();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), "utf-8"));
    String line;
    while ((line = in.readLine()) != null) {
      int pos = line.indexOf("(");
      int count = Integer.parseInt(line.substring(pos + 5));
      if (count >= minCount) {
        map.put(line.substring(0, pos), count);
      }
    }
    in.close();
    return map;
  }

  /**
   * Converts the old dictionary file to new format.
   * 
   * @param dictionaryFile
   * @param localeDictFile
   * @throws IOException
   */
  static void toLocaleDictionary(String dictionaryFile, String localeDictFile)
      throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(dictionaryFile), "utf-8"));
    ArrayList<String> list = new ArrayList<String>();
    String line;
    while ((line = in.readLine()) != null) {
      list.add(line);
    }
    in.close();
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(localeDictFile), UTF8));
    String[] word;
    for (String s : list) {
      word = s.split("\t");
      out.printf("%s(en)\t%s(fr)\n", word[0].toLowerCase(Locale.ENGLISH),
          word[1].toLowerCase(Locale.FRENCH));
    }
    out.close();
  }

  public static void main(String args[]) throws Exception {
//    final String directory = "C:/datasets/asc/reviews/ElectronicsReviews1";
    final String directory = "C:/datasets/asc/ldatest/ElectronicsReviews2";
    String dataset = "C:/datasets/asc/ldatest/ElectronicsReviews2";
    int cnt = 10;
    String dictFile = directory + "/en-fr.txt";
    String localeFile = directory + "/en-fr-locale.txt" + cnt;
    HashMap<String, Integer> wordCnt = getWordCount(dataset + "/WordCount.csv",
        cnt);
    Dictionary.make(wordCnt, dataset + "/Stem_en.txt",
        dataset + "/Stem_fr.txt", FR, dictFile);
    Dictionary.removeDuplication(dictFile);
    toLocaleDictionary(dictFile, localeFile);
  }
}
