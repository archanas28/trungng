package edu.kaist.uilab.asc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Class for many utilities to deal with text file.
 * 
 * @author Trung Nguyen
 */
public class TextFiles {

  /**
   * Aggregates the content of text files of a directory into a single file.
   * 
   * @param dir
   *          the directory to aggregate
   * @param file
   *          the file
   */
  public static void aggregateDirectoryToFile(String dir, String file)
      throws IOException {
    PrintWriter out = new PrintWriter(file);
    File directory = new File(dir);
    int lines = 0;
    for (File f : directory.listFiles()) {
      BufferedReader in = new BufferedReader(new FileReader(f));
      String line;
      while ((line = in.readLine()) != null) {
        out.println(line);
        lines++;
      }
      in.close();
      if (lines % 5 != 0) {
        System.out.println(f.getAbsolutePath());
        break;
      }
    }
    out.close();
  }

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
  public static <T> void writeCollection(Collection<T> col, String file,
      String encoding) throws IOException {
    PrintWriter writer;
    if (encoding != null) {
      writer = new PrintWriter(new OutputStreamWriter(
          new FileOutputStream(file), encoding));
    } else {
      writer = new PrintWriter(file);
    }
    for (T e : col) {
      writer.println(e);
    }
    writer.close();
  }

  /**
   * Writes the elements of {@code iter} to {@code file}, each element on a
   * single line.
   * 
   * @param iter
   * @param file
   */
  public static <T> void writeIterator(Iterator<T> iter, String file,
      String encoding) throws IOException {
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), encoding));
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
  public static List<String> readLines(String fileName) throws IOException {
    List<String> lines = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new FileReader(fileName));
    String line;
    while ((line = reader.readLine()) != null) {
      lines.add(line);
    }
    reader.close();

    return lines;
  }

  /**
   * Returns the list of strings for {@code fileName}, each line as an element
   * of the returned list.
   * <p>
   * The order of elements in the list is same as that of lines in the file.
   * 
   * @param fileName
   * @param encoding
   * @return
   */
  public static List<String> readLines(String fileName, String encoding)
      throws IOException {
    List<String> words = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(fileName), encoding));
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
   * This method is the same as {@link #readLines(String)} except that all
   * elements of the list are in lowercase.
   * 
   * @return
   */
  public static List<String> readLinesAsLowerCase(String file) throws Exception {
    List<String> words = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new FileReader(file));
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
  public static Collection<String> readUniqueLines(String fileName,
      String encoding) throws IOException {
    HashSet<String> words = new HashSet<String>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(fileName), encoding));
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
      words.add(line.toLowerCase());
    }
    reader.close();

    return words;
  }
}
