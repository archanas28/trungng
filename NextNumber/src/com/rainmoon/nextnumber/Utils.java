package com.rainmoon.nextnumber;

public class Utils {

  /**
   * String from first element to <code>end - 1</code>. Put new line at the
   * <code>newline</code> position.
   * 
   * @param array
   * @param end
   * @param newline
   *          position where to break sequence
   * @return
   */
  public static String getString(int[] array, int end, int newline) {
    StringBuilder builder = new StringBuilder();
    for (int idx = 0; idx < end; idx++) {
      builder.append(String.format("%d, ", array[idx]));
      if (idx == newline) {
        builder.append("\n");
      }
    }
    return builder.toString();
  }

}
