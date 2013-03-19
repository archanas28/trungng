package com.rainmoon.jobsalary;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class for many utilities to deal with text file.
 * 
 * @author Trung Nguyen
 */
public class TextFiles {

  /**
   * Returns the text content of a file.
   * 
   * @return
   * @throws IOException
   */
  public static String readFile(String file) throws IOException {
    StringBuilder builder = new StringBuilder();
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String line;
    while ((line = reader.readLine()) != null) {
      builder.append(line).append(" ");
    }
    reader.close();
    builder.deleteCharAt(builder.length() - 1);
    return builder.toString();
  }

  /**
   * Writes the string {@code content} to the given file.
   * 
   * @param file
   * @param content
   * @throws IOException
   */
  public static void writeFile(String file, String content) throws IOException {
    PrintWriter writer = new PrintWriter(file);
    writer.println(content);
    writer.close();
  }

  /**
   * Writes the collection to {@code file}, each element on a single line.
   * 
   * @param col
   * @param file
   */
  public static <T> void writeCollection(Collection<T> col, String file)
      throws IOException {
    PrintWriter writer = new PrintWriter(file, "utf-8");
    for (T e : col) {
      writer.println(e);
    }
    writer.close();
  }

  /**
   * Writes the map to a csv file. Each entry is on one line. Each line contains
   * key and value separated by commas.
   * 
   * @param map
   * @param file
   * @throws IOException
   */
  public static <K, V> void writeMap(Map<K, V> map, String file)
      throws IOException {
    PrintWriter writer = new PrintWriter(file, "utf-8");
    for (Entry<K, V> e : map.entrySet()) {
      writer.printf("%s,%s\n", e.getKey(), e.getValue());
    }
    writer.close();
  }

  public static Map<String, Integer> readMap(String filename)
      throws IOException {
    Map<String, Integer> map = new HashMap<String, Integer>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(filename), "utf-8"));
    String line;
    String[] tokens;
    while ((line = reader.readLine()) != null) {
      tokens = line.split(",");
      map.put(tokens[0], Integer.parseInt(tokens[1]));
    }
    reader.close();

    return map;
  }

  /**
   * Writes the elements of {@code iter} to {@code file}, each element on a
   * single line.
   * 
   * @param iter
   * @param file
   */
  public static <T> void writeIterator(Iterator<T> iter, String file)
      throws Exception {
    PrintWriter writer = new PrintWriter(file);
    while (iter.hasNext()) {
      writer.println(iter.next());
    }
    writer.close();
  }

  /**
   * Returns the list of strings for {@code fileName}, each line as an element
   * of the returned list.
   * <p>
   * The order of elements in the list is same as that of lines in the file.
   * 
   * @return
   */
  public static ArrayList<String> readLines(String fileName) throws IOException {
    ArrayList<String> words = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(fileName), "utf-8"));
    String line;
    while ((line = reader.readLine()) != null) {
      words.add(line);
    }
    reader.close();

    return words;
  }

  /**
   * Returns the list of <b> lowercase</b> strings for {@code fileName}, each
   * line as an element of the returned list.
   * <p>
   * The order of elements in the list is same as that of lines in the file.
   * <p>
   * This method is the same as {{@link #readLines(String)} except that all
   * elements of the list are in lowercase.
   * 
   * @return
   */
  public static List<String> readLinesAsLowerCase(String fileName)
      throws Exception {
    List<String> words = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new FileReader(fileName));
    String line;
    while ((line = reader.readLine()) != null) {
      words.add(line.toLowerCase());
    }
    reader.close();

    return words;
  }

  /**
   * Returns the collection (or in mathematical terms, set) of strings for
   * {@code fileName}, each line as an element of the returned collection.
   * <p>
   * The order of elements in the list might not be same as that of lines in the
   * file.
   * 
   * @return
   */
  public static Collection<String> readUniqueLines(String fileName)
      throws Exception {
    HashSet<String> words = new HashSet<String>();
    BufferedReader reader = new BufferedReader(new FileReader(fileName));
    String line;
    while ((line = reader.readLine()) != null) {
      words.add(line);
    }
    reader.close();

    return words;
  }

  /**
   * Returns the collection (or in mathematical terms, set) of strings for
   * {@code fileName}, each line as an element of the returned collection.
   * <p>
   * The order of elements in the list might not be same as that of lines in the
   * file.
   * <p>
   * This method is the same as {{@link #readUniqueLines(String)} except that
   * all elements of the collection are in lower-case.
   * 
   * @return
   */
  public static Collection<String> readUniqueLinesAsLowerCase(String fileName)
      throws IOException {
    HashSet<String> words = new HashSet<String>();
    BufferedReader reader = new BufferedReader(new FileReader(fileName));
    String line;
    while ((line = reader.readLine()) != null) {
      words.add(line);
    }
    reader.close();

    return words;
  }
}
