package edu.kaist.uilab.bs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import edu.kaist.uilab.asc.data.Review;
import edu.kaist.uilab.asc.util.TextFiles;
import edu.kaist.uilab.stemmers.EnglishStemmer;

/**
 * Utility methods for this package.
 * 
 * @author trung
 */
public class BSUtils {

  /**
   * Saves the given serializable object <code>data</code> to the specified
   * file.
   * 
   * @param file
   * @param data
   */
  public static void saveModel(String file, Serializable data) {
    System.err.println("Saving model to " + file);
    try {
      ObjectOutputStream out = new ObjectOutputStream(
          new FileOutputStream(file));
      out.writeObject(data);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Loads an existing model from the specified file.
   * 
   * @param savedModel
   */
  public static Serializable loadModel(String savedModel) {
    Serializable model = null;
    try {
      System.err.println("Loading model from " + savedModel);
      ObjectInputStream in = new ObjectInputStream(new FileInputStream(
          savedModel));
      model = (Serializable) in.readObject();
      in.close();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return model;
  }

  /**
   * Re-writes the electronics corpus to make in consistent with the format.
   * 
   * @throws IOException
   */
  public static void temp(String inputDir, String outputFile)
      throws IOException {
    String[] files = new File(inputDir).list();
    ArrayList<Review> list = new ArrayList<Review>();
    int count = 0;
    String targetId;
    for (String file : files) {
      if (!file.equals("avatar.txt"))
        continue;
      targetId = file.substring(0, file.length() - 4);
      BufferedReader in = new BufferedReader(new InputStreamReader(
          new FileInputStream(inputDir + "/" + file), "utf-8"));
      double rating;
      while (in.readLine() != null) {
        count++;
        try {
          rating = Double.parseDouble(in.readLine());
        } catch (NumberFormatException e) {
          rating = -1.0;
        }
        list.add(new Review(String.valueOf(count), targetId, rating, in
            .readLine()));
      }
      in.close();
    }

    TextFiles.writeCollection(list, outputFile, "utf-8");
  }

  public static void main(String args[]) throws IOException {
    // temp("C:/datasets/movies/eng", "C:/datasets/models/bs/movies/docs.txt");
    EnglishStemmer stemmer = new EnglishStemmer();
    System.out.println(stemmer.getStem("useful"));
  }
}
