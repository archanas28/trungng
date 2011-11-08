package edu.kaist.uilab.bs.evaluation;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.math.stat.StatUtils;

import com.aliasi.util.ObjectToDoubleMap;

import edu.kaist.uilab.asc.util.TextFiles;

/**
 * Processes the results of user study. TODO(trung): eleminate users whose
 * variance is too small
 * 
 * @author trung
 */
public class UserStudyProcessor {

  final long minWorkTimeInSeconds = 100 * 1000;
  final double maxInconsistentRatings = 0.6; // 0.4 percent
  final double minStdOverMeanRatio = 0.0;

  ArrayList<Result> results = new ArrayList<Result>();
  // based on ipaddress
  HashMap<String, ArrayList<Result>> workerMap = new HashMap<String, ArrayList<Result>>();
  // either segment or wordpair
  HashMap<String, ArrayList<Result>> summaryTypeMap = new HashMap<String, ArrayList<Result>>();
  HashMap<String, ArrayList<Integer>> segmentMap = new HashMap<String, ArrayList<Integer>>();
  HashMap<String, ArrayList<Integer>> phraseMap = new HashMap<String, ArrayList<Integer>>();

  /**
   * Reads all results.
   * 
   * @param file
   */
  public void readResults(String file) {
    try {
      ArrayList<String> lines = (ArrayList<String>) TextFiles.readLines(file);
      for (String line : lines) {
        Result res = new Result(line);
        results.add(res);
        // build worker map
        ArrayList<Result> workerResults = workerMap.get(res.ipaddress);
        if (workerResults == null) {
          workerResults = new ArrayList<Result>();
          workerMap.put(res.ipaddress, workerResults);
        }
        workerResults.add(res);
        // build summary type map
        ArrayList<Result> summaryTypeResults = summaryTypeMap
            .get(res.summaryType);
        if (summaryTypeResults == null) {
          summaryTypeResults = new ArrayList<Result>();
          summaryTypeMap.put(res.summaryType, summaryTypeResults);
        }
        // build segment and phrase map
        HashMap<String, ArrayList<Integer>> theMap;
        if (res.summaryType.equals("segment")) {
          theMap = segmentMap;
        } else {
          theMap = phraseMap;
        }
        if (!theMap.containsKey(res.textContent)) {
          theMap.put(res.textContent, new ArrayList<Integer>());
        }
        if (res.rating != null) {
          theMap.get(res.textContent).add(res.rating);
        }  
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Reports workers that were completed in unusually fast time.
   * 
   * @return set of IP Addresses of the disqualified workers
   */
  public HashSet<String> reportWorkersCompletedInUnrealisticTime() {
    HashSet<String> badWorkers = new HashSet<String>();
    for (Entry<String, ArrayList<Result>> entry : workerMap.entrySet()) {
      ArrayList<Result> results = entry.getValue();
      long minTime = System.currentTimeMillis(), maxTime = 0;
      for (Result result : results) {
        long time = result.date.getTime();
        if (time > maxTime) {
          maxTime = time;
        }
        if (time < minTime) {
          minTime = time;
        }
      }
      if (maxTime - minTime < minWorkTimeInSeconds) {
        badWorkers.add(entry.getKey());
      }
    }

    return badWorkers;
  }

  /**
   * Reports workers that provided inconsistent ratings.
   * 
   * @return
   */
  public HashSet<String> reportInconsistentWorkers() {
    HashSet<String> badWorkers = new HashSet<String>();
    for (Entry<String, ArrayList<Result>> entry : workerMap.entrySet()) {
      ArrayList<Result> results = entry.getValue();
      int totalInconsistentRating = 0;
      for (Result result : results) {
        Result other = getOther(result, results);
        if (other != null && other.rating != null && result.rating != null) {
          int diff = Math.abs(other.rating - result.rating);
          totalInconsistentRating += diff;
        }
      }
      totalInconsistentRating = totalInconsistentRating / 2;
      System.out.printf("Inconsistency/Items: %d/%d\n",
          totalInconsistentRating, results.size());
      if (maxInconsistentRatings * results.size() <= totalInconsistentRating) {
        badWorkers.add(entry.getKey());
      }
    }

    return badWorkers;
  }

  /**
   * Reports workers that choose results randomly, i.e., choose same ratings for
   * almost all items.
   * <p>
   * Not counting the bad workers.
   * 
   * @return
   */
  public HashSet<String> reportRandomWorkers(HashSet<String> dishonestWorkers) {
    HashSet<String> badWorkers = new HashSet<String>();
    double[] ratios = new double[workerMap.size() - dishonestWorkers.size()];
    int ratioIdx = 0;
    for (Entry<String, ArrayList<Result>> entry : workerMap.entrySet()) {
      if (dishonestWorkers.contains(entry.getKey())) {
        continue;
      }
      ArrayList<Result> results = entry.getValue();
      ArrayList<Double> ratings = new ArrayList<Double>();
      for (Result result : results) {
        if (result.rating != null) {
          ratings.add((double) result.rating);
        }
      }
      double[] values = new double[ratings.size()];
      for (int idx = 0; idx < ratings.size(); idx++) {
        values[idx] = ratings.get(idx);
      }
      double mean = StatUtils.mean(values);
      double std = Math.sqrt(StatUtils.variance(values));
      double ratio = std / mean;
      ratios[ratioIdx++] = ratio;
      System.out.printf("\n(%.2f, %.2f): %.2f", mean, std, ratio);
      if (ratio <= minStdOverMeanRatio) {
        badWorkers.add(entry.getKey());
        System.out.printf("\n%s: (%.2f, %.2f) : %.2f", entry.getKey(), mean,
            std, ratio);
      }
    }
    System.out.printf("\nAverage(mean) ratio: %.2f", StatUtils.mean(ratios));
    System.out.printf("\nVariance of ratio: %.2f",
        Math.sqrt(StatUtils.variance(ratios)));
    System.out.printf("\n95 percent range: (%.2f, %.2f)",
        StatUtils.mean(ratios) - 2 * Math.sqrt(StatUtils.variance(ratios)),
        StatUtils.mean(ratios) + 2 * Math.sqrt(StatUtils.variance(ratios)));

    return badWorkers;
  }

  /**
   * Prints out the average ratings not counting the results of bad workers.
   * 
   * @param badWorkers
   */
  public void printAverageRatings(HashSet<String> badWorkers) {
    int totalSegmentRating = 0, numSegments = 0, totalWordpairRating = 0, numPairs = 0;
    HashSet<String> uniqueSegments = new HashSet<String>();
    HashSet<String> uniquePairs = new HashSet<String>();
    for (Entry<String, ArrayList<Result>> entry : workerMap.entrySet()) {
      if (!badWorkers.contains(entry.getKey())) {
        ArrayList<Result> results = entry.getValue();
        for (Result result : results) {
          if (result.rating != null) {
            if (result.summaryType.equals("segment")) {
              totalSegmentRating += result.rating;
              numSegments++;
              uniqueSegments.add(result.textContent);
            } else {
              totalWordpairRating += result.rating;
              numPairs++;
              uniquePairs.add(result.textContent);
            }
          }
        }
      }
    }
    double avgSegmentRating = ((double) totalSegmentRating) / numSegments;
    double avgPairRating = ((double) totalWordpairRating) / numPairs;
    System.out.printf("\n#segments = %d (unique = %d); avg rating: %.3f",
        numSegments, uniqueSegments.size(), avgSegmentRating);
    System.out.printf("\n#wordpairs = %d (unique = %d); avg rating: %.3f",
        numPairs, uniquePairs.size(), avgPairRating);
    System.out.printf("\nDifference: %.2f\n", avgSegmentRating - avgPairRating);
  }

  /**
   * Gets the other rating for the same summary item by a worker.
   * 
   * @param result
   * @param results
   * @return
   */
  private Result getOther(Result result, ArrayList<Result> results) {
    Result other = null;
    for (Result r : results) {
      if (r != result && r.textContent.equals(result.textContent)
          && r.summaryType.equals(result.summaryType)
          && r.summaryId.equals(result.summaryId)) {
        other = r;
        break;
      }
    }

    return other;
  }

  /**
   * Prints items with highest rating and lowest rating.
   * 
   * @param map
   */
  public void printItems(HashMap<String, ArrayList<Integer>> map, int numItems) {
    ObjectToDoubleMap<String> items = new ObjectToDoubleMap<String>();
    for (Entry<String, ArrayList<Integer>> entry : map.entrySet()) {
      double sum = 0.0;
      for (int rating : entry.getValue()) {
        sum += rating;
      }
      items.put(entry.getKey(), sum / entry.getValue().size());
    }
    List<String> orderedKeys = items.keysOrderedByValueList();
    for (int i = 0; i < numItems; i++) {
      String item = orderedKeys.get(i);
      System.out.printf("%s: %.1f\n", item, items.get(item));
    }
    for (int i = orderedKeys.size() - 1; i > orderedKeys.size() - 1 - numItems; i--) {
      String item = orderedKeys.get(i);
      System.out.printf("%s: %.1f\n", item, items.get(item));
    }
  }

  public static void main(String args[]) throws IOException {
    String file = "first.csv";
    UserStudyProcessor processor = new UserStudyProcessor();
    processor.readResults(file);
    System.out.println("\nSegments\n------------------------");
    processor.printItems(processor.segmentMap, 30);
    System.out.println("\nPhrases\n------------------------");
    processor.printItems(processor.phraseMap, 30);
    // HashSet<String> badWorkers1 = processor
    // .reportWorkersCompletedInUnrealisticTime();
    // HashSet<String> badWorkers2 = processor.reportInconsistentWorkers();
    // HashSet<String> allBadWorkers = new HashSet<String>();
    // allBadWorkers.addAll(badWorkers1);
    // allBadWorkers.addAll(badWorkers2);
    // HashSet<String> badWorkers3 =
    // processor.reportRandomWorkers(allBadWorkers);
    // allBadWorkers.addAll(badWorkers3);
    // System.out.println("\n\n");
    // System.out.println("#users removed by unrealistic time: "
    // + badWorkers1.size());
    // System.out
    // .println("#users removed by inconsistency: " + badWorkers2.size());
    // System.out.println("#users removed by random-guessing: "
    // + badWorkers3.size());
    // System.out.printf("Total users removed / all users: %d/%d\n",
    // allBadWorkers.size(), processor.workerMap.size());
    // processor.printAverageRatings(allBadWorkers);
    // System.out.println(badWorkers1);
    // System.out.println(badWorkers2);
    // TextFiles.writeCollection(processor.results, "newfirst.csv", "utf-8");
  }

  /**
   * An entry for a user study result.
   */
  static final class Result {
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date;
    String ipaddress;
    String summaryId;
    String summaryType;
    String textContent;
    Integer rating;

    /**
     * Creates a new Result record.
     * 
     * @param s
     */
    public Result(String s) {
      String[] tokens = s.split(",");
      // convert string to time and decrease 9 hours
      long millsecondsDiff = 9 * 60 * 60 * 1000;
      try {
        date = df.parse(tokens[0]);
        date = new Date(date.getTime() - millsecondsDiff);
      } catch (ParseException e) {
        e.printStackTrace();
      }
      ipaddress = tokens[1];
      summaryId = tokens[2];
      summaryType = tokens[3];
      textContent = tokens[4];
      if (!tokens[5].equals("null")) {
        rating = Integer.parseInt(tokens[5]);
      }
    }

    @Override
    public String toString() {
      return String.format("%s,%s,%s,%s,%s,%s", df.format(date), ipaddress,
          summaryId, summaryType, textContent, rating);
    }
  }
}
