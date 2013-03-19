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
  public static JobSalaryData getInstance(String[] tokens, boolean validation) {
    JobSalaryData data = new JobSalaryData();
    data.validation = validation;
    data.id = tokens[0];
    data.jobTitle = tokens[1].replaceAll(",", " ").toLowerCase().trim();
    data.fullDescription = tokens[2].replaceAll(",", " ").toLowerCase().trim();
    // skip locationRaw tokens[3]
    data.locationNormalized = tokens[4].replaceAll(",", " ").toLowerCase().trim();
    // skip contract type and time tokens[5] [6]
    data.company = tokens[7].replaceAll(",", " ").toLowerCase().trim();
    data.category = tokens[8].replaceAll(",", " ").toLowerCase().trim();
    if (!validation) {
      // skip salary raw tokens[9]
      data.salaryNormalized = Double.parseDouble(tokens[10]);
      data.sourceName = tokens[11];
    } else {
      data.sourceName = tokens[9];
    }
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
}
