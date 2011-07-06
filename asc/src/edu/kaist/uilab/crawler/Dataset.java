package edu.kaist.uilab.crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Dataset {

  static void getBalancedDataset() throws Exception {
    String inputDir = "C:/datasets/asc/electronics";
    File batch = new File(inputDir + "/batch");
    ArrayList<File> files = new ArrayList<File>();
    for (File f : batch.listFiles()) {
//      if (f.getName().contains("_en.txt")) {
         if (f.getName().contains("_fr.txt")) {
        files.add(f);
      }
    }
//    batch = new File(inputDir + "/batch2");
//    for (File f : batch.listFiles()) {
//      if (f.getName().contains("_en.txt")) {
////         if (f.getName().contains("_fr.txt")) {
//        files.add(f);
//      }
//    }
    ArrayList<Review> reviews = new ArrayList<Review>();
    for (File file : files) {
      BufferedReader in = new BufferedReader(new InputStreamReader(
          new FileInputStream(file), "utf-8"));
      Review review;
      String source, rating, content;
      String[] words;
      while ((source = in.readLine()) != null) {
        rating = in.readLine();
        content = in.readLine();
        try {
          review = new Review(source, Double.parseDouble(rating), content);
        } catch (NumberFormatException e) {
          review = new Review(source, -1.0, content);
        }
        words = review.mContent.split("[; ,.\t!':]");
        if (words.length > 20) {
          reviews.add(review);
        }
      }
      in.close();
    }
    reviews = shuffle(reviews);
    int numPos = 4000 + (int) (Math.random() * 500);
    int numNeg = 2000 + (int) (Math.random() * 500);
//     int numPos = 2000 + (int) (Math.random() * 200);
//     int numNeg = 1500 + (int) (Math.random() * 200);
    int numNeutral = (int) (Math.random() * 5000);
    System.out.printf("\npos = %d, neg = %d, neu = %d\n", numPos, numNeg,
        numNeutral);
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
//        new FileOutputStream(inputDir + "/docs_en.txt", false), "utf-8"));
     new FileOutputStream(inputDir + "/docs_other.txt", false), "utf-8"));
    for (Review review : reviews) {
      if (review.mRating >= 0) {
        if (review.mRating > 3.0 && numPos > 0) {
          out.print(review);
          numPos--;
        } else if (review.mRating < 3.0 && numNeg > 0) {
          out.print(review);
          numNeg--;
        } else if (numNeutral > 0) {
          out.print(review);
          numNeutral--;
        }
        if (numPos == 0 && numNeg == 0 && numNeutral == 0) {
          break;
        }
      }
    }
    System.out.printf("\npos = %d, neg = %d, neu = %d\n", numPos, numNeg,
        numNeutral);
    out.close();
  }

  public static void main(String args[]) throws Exception {
    getBalancedDataset();
  }

  void getDocuments() throws Exception {
    String inputDir = "C:/datasets/asc/electronics";
    File batch = new File(inputDir + "/batch");
    ArrayList<File> files = new ArrayList<File>();
    for (File f : batch.listFiles()) {
      // if (f.getName().contains("_en.txt")) {
      if (f.getName().contains("_fr.txt")) {
        files.add(f);
      }
    }
    ArrayList<Review> reviews = new ArrayList<Review>();
    for (File file : files) {
      BufferedReader in = new BufferedReader(new InputStreamReader(
          new FileInputStream(file), "utf-8"));
      Review review;
      String source, rating, content;
      String[] words;
      while ((source = in.readLine()) != null) {
        rating = in.readLine();
        content = in.readLine();
        try {
          review = new Review(source, Double.parseDouble(rating), content);
        } catch (NumberFormatException e) {
          review = new Review(source, -1.0, content);
        }
        words = review.mContent.split("[,.\t]");
        if (words.length > 15) {
          reviews.add(review);
        }
      }
      in.close();
    }
    reviews = shuffle(reviews);
    int numReviews = 2500 > reviews.size() ? reviews.size() : 2500;
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
    // new FileOutputStream(inputDir + "/docs_en.txt", false), "utf-8"));
        new FileOutputStream(inputDir + "/docs_other.txt", false), "utf-8"));
    for (int i = 0; i < numReviews; i++) {
      out.print(reviews.get(i));
    }
    out.close();
  }
  
  private static ArrayList<Review> shuffle(ArrayList<Review> reviews) {
    ArrayList<Review> res = new ArrayList<Review>();
    for (int i = 0; i < reviews.size(); i++) {
      int idx = (int) (Math.random() * reviews.size());
      res.add(reviews.get(idx));
      reviews.remove(idx);
    }
    return res;
  }
}
