package edu.kaist.uilab.bs.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import edu.kaist.uilab.asc.data.Review;
import edu.kaist.uilab.asc.data.ReviewReader;
import edu.kaist.uilab.asc.data.ReviewWithProsAndCons;
import edu.kaist.uilab.asc.data.ReviewWithProsAndConsReader;
import edu.kaist.uilab.asc.util.TextFiles;

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
   * Reads reviews from a corpus.
   * <p>
   * All reviews of each specific review target is read into its own list of
   * reviews, which is mapped by its id in the returned map.
   * 
   * @param corpus
   *          the file containing all reviews
   * @param reader
   *          a review reader
   * @return a map from a target id (i.e., a product or a restaurant) to its
   *         list of reviews
   * @throws IOException
   */
  public static HashMap<String, ArrayList<Review>> readReviews(String corpus,
      ReviewReader reader) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(corpus)));
    HashMap<String, ArrayList<Review>> map = new HashMap<String, ArrayList<Review>>();
    ArrayList<Review> list = new ArrayList<Review>();
    Review review = null;
    do {
      review = reader.readReview(in, false);
      if (review != null) {
        if (map.containsKey(review.getRestaurantId())) {
          list = map.get(review.getRestaurantId());
        } else {
          list = new ArrayList<Review>();
          map.put(review.getRestaurantId(), list);
        }
        list.add(review);
      }
    } while (review != null);
    in.close();

    return map;
  }

  /**
   * Returns true if <code>element</code> is in <code>array</code>.
   * 
   * @param array
   * @param value
   * @return
   */
  public static boolean isInArray(Object[] array, Object value) {
    for (Object element : array) {
      if (element.equals(value)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns a string that is a concatenation of elements of the given array.
   * 
   * @param array
   * @param separator
   * @return
   */
  public static String arrayToString(String[] array, String separator) {
    StringBuilder builder = new StringBuilder();
    for (String s : array) {
      builder.append(s).append(separator);
    }
    return builder.toString();
  }

  /**
   * Converts a map into a list by aggregating all elements in the map's values.
   * 
   * @param <T>
   * @param map
   * @return
   */
  public static <T> ArrayList<T> mapToList(HashMap<String, ArrayList<T>> map) {
    ArrayList<T> list = new ArrayList<T>();
    for (ArrayList<T> elem : map.values()) {
      list.addAll(elem);
    }

    return list;
  }

  /**
   * Fixes a dataset crawled from Epinions.com by removing the reviews whose
   * content is too short.
   * 
   * @param dataset
   * @throws IOException
   */
  static void fixEpinionDataset(String dataset) throws IOException {
    ReviewWithProsAndConsReader reader = new ReviewWithProsAndConsReader();
    ArrayList<Review> reviews = mapToList(readReviews(dataset, reader));
    int cnt = 0;
    ArrayList<Review> newReviews = new ArrayList<Review>();
    for (Review review : reviews) {
      ReviewWithProsAndCons r = (ReviewWithProsAndCons) review;
      if (r.getPros().split(" ").length > 40
          || r.getCons().split(" ").length > 40 || r.getContent().length() < 50) {
        cnt++;
        System.out.println(r.getPros());
      } else {
        newReviews.add(review);
      }
    }
    System.out.println("Bad reviews: " + cnt);
    TextFiles.writeCollection(newReviews, dataset, null);
  }

  public static void main(String args[]) throws IOException {
    TextFiles.aggregateDirectoryToFile("C:/datasets/epinions/laptop",
        "C:/datasets/epinions/laptop2.txt");
//    fixEpinionDataset("C:/datasets/models/asum/coffeemaker/docs_en.txt");
    // EnglishStemmer stemmer = new EnglishStemmer();
    // System.out.println(stemmer.getStem("fresh"));
    // String[] verbs = { "love", "loves", "loved", "like", "liked", "likes",
    // "enjoy", "enjoyed", "enjoys", "recommends" };
    // String[] verbs2 = { "hate", "hates", "hated", "annoy", "annoyed",
    // "annoys",
    // "fail", "fails", "failed", "disappoint", "disappointed", "disappoints",
    // "waste", "regrets", };
    // for (String verb : verbs) {
    // System.out.println(verb + " --> " + stemmer.getStem(verb));
    // }
    // for (String verb : verbs2) {
    // System.out.println(verb + " --> " + stemmer.getStem(verb));
    // }
  }
}
