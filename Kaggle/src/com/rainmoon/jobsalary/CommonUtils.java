package com.rainmoon.jobsalary;

public final class CommonUtils {

  /**
   * Prints elements of the array as a line separated by commas.
   * 
   * @param array
   * @return the string printed out
   */
  public static String printArray(Object[] array) {
    StringBuilder builder = new StringBuilder();
    for (Object item : array) {
      builder.append(item).append(",");
    }
    String res = builder.toString();
    System.out.println(res);
    return res;
  }

  public static void main(String args[]) {

  }
}
