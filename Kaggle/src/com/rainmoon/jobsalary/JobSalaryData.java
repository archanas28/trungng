package com.rainmoon.jobsalary;

import java.util.StringTokenizer;

/**
 * One instance of the job salary advertisement.
 * 
 * @author trung
 */
public class JobSalaryData {
  String id;
  String jobTitle;
  String fullDescription;
  String locationNormalized;
  String contractType;
  String contractTime;
  String company;
  String category;
  double salaryNormalized;
  String sourceName;
  boolean validation;

  /**
   * Construct an training data instance from the job ad.
   * 
   * @param ad
   */
  public JobSalaryData(String ad) {
    StringTokenizer tokenizer = new StringTokenizer(ad, ",");
    this.id = tokenizer.nextToken();
    this.jobTitle = tokenizer.nextToken();
    this.fullDescription = tokenizer.nextToken();
    this.locationNormalized = tokenizer.nextToken();
    this.company = tokenizer.nextToken();
    this.category = tokenizer.nextToken();
    this.salaryNormalized = Double.parseDouble(tokenizer.nextToken());
    this.sourceName = tokenizer.nextToken();
  }

  private JobSalaryData() {
  };

  /**
   * Parse ad into salary data for the original data.
   * 
   * @param ad
   * @return
   */
  public static JobSalaryData getInstance(String ad, boolean validation) {
    JobSalaryData data = new JobSalaryData();
    data.validation = validation;
    // remove comma inside quotes
    String normalizedAd = ad;
    int firstQuote = -1, secondQuote = -1;
    while ((firstQuote = normalizedAd.indexOf("\"")) > 0) {
      secondQuote = normalizedAd.indexOf("\"", firstQuote + 1);
      String oldString = normalizedAd.substring(firstQuote + 1, secondQuote);
      String newString = oldString.replaceAll(",", " ");
      normalizedAd = normalizedAd.replace("\"" + oldString + "\"", newString);
    }
    normalizedAd = normalizedAd.replaceAll(",,", ", ,");
    normalizedAd = normalizedAd.replaceAll(",,", ", ,");
    if (normalizedAd.endsWith(", ,")) {
      normalizedAd = normalizedAd.substring(0, normalizedAd.length() - 2);
    }
    StringTokenizer tokenizer = new StringTokenizer(normalizedAd, ",");
    System.out.println(normalizedAd);
    data.id = tokenizer.nextToken();
    data.jobTitle = tokenizer.nextToken();
    data.fullDescription = tokenizer.nextToken();
    tokenizer.nextToken(); // skip locationRaw
    data.locationNormalized = tokenizer.nextToken();
    tokenizer.nextToken(); // skip contract type
    tokenizer.nextToken(); // skip contract time
    data.company = tokenizer.nextToken();
    data.category = tokenizer.nextToken();
    if (!validation) {
      tokenizer.nextToken(); // skip salary raw
      data.salaryNormalized = Double.parseDouble(tokenizer.nextToken());
    }
    data.sourceName = tokenizer.nextToken();
    return data;
  }

  @Override
  /**
   * Returns the normalized string of the job ad.
   * Fields include (id,jobtitle,fullDescription,locationNormalized,company,
   * category,salaryNormalized,sourceName).
   */
  public String toString() {
    if (!validation) {
      return id + "," + jobTitle + "," + fullDescription + ","
          + locationNormalized + "," + company + "," + category + ","
          + salaryNormalized + "," + sourceName;
    }
    return id + "," + jobTitle + "," + fullDescription + ","
        + locationNormalized + "," + company + "," + category + ","
        + sourceName;
  }

  /**
   * Returns the feature vector for this instance. Features are separated by
   * comma.
   * 
   * @return
   */
  public String toFeatureVector() {
    return "";
  }
}
