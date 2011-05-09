package edu.kaist.uilab.asc.prior;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Dictionary.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Dictionary {
  static final String DIRECTORY = "C:/datasets/asc/dict";
  static final String GOOGLE_TRANSLATE = "https://www.googleapis.com/language/translate/v2?"
      + "key=AIzaSyBfYp3qsYicVnVZSxxfX22sFfN9GHkDN24&source=en&target=fr&q=";

  /**
   * Removes duplication in the file retrieved from the source used by James
   * Petterson.
   * 
   * @param file
   * @param newFile
   * @throws IOException
   */
  public static void removeDuplication(String file, String newFile)
      throws IOException {
    HashMap<String, String> map = new HashMap<String, String>();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), "utf-8"));
    String line, word1, word2;
    StringTokenizer tokenizer;
    while ((line = in.readLine()) != null) {
      tokenizer = new StringTokenizer(line, "\t");
      word1 = tokenizer.nextToken().trim();
      word2 = tokenizer.nextToken().replace("(m)", " ").replace("(f)", " ")
          .trim();
      map.put(word1, word2);
    }
    in.close();
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(newFile), "utf-8"));
    for (Map.Entry<String, String> entry : map.entrySet()) {
      out.printf("%s\t%s\n", entry.getKey(), entry.getValue());
    }
    out.close();
  }

  /**
   * Reads a file which contains English words and translates them into french.
   * 
   * @param englishFile
   * @param outputFile
   */
  public static void englishToFrench(String englishFile, String outputFile) {
    BufferedReader in;
    PrintWriter out = null;
    try {
      in = new BufferedReader(new FileReader(englishFile));
      out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
          outputFile, true), "utf-8"));
      String word, translatedWord;
      HttpClient httpClient = new DefaultHttpClient();
      while ((word = in.readLine()) != null) {
        translatedWord = getFrench(word, httpClient);
        if (translatedWord != null) {
          out.printf("%s\t%s\n", word, translatedWord);
        }
      }
      httpClient.getConnectionManager().shutdown();
      in.close();
    } catch (IOException e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  /**
   * Get French words for the specified word.
   * 
   * @param word
   * @return
   */
  static String getFrench(String word, HttpClient client) {
    HttpGet httpget = new HttpGet(GOOGLE_TRANSLATE + word);
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

    }
    if (translatedWord != null && !translatedWord.contains(" ")) {
      System.out.println(word + "\t" + translatedWord);
      return translatedWord;
    }
    return null;
  }

  public static void main(String args[]) throws Exception {
    Dictionary.englishToFrench("C:/datasets/asc/reviews/WordList_en.txt",
        DIRECTORY + "/en-fr.txt");
    // Dictionary.removeDuplication(DIRECTORY + "/english-french.txt", DIRECTORY
    // + "/en-fr.txt");
  }
}
