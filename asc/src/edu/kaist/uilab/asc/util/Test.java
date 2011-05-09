package edu.kaist.uilab.asc.util;

import java.util.regex.Pattern;

public class Test {
  public static void main(String args[]) {
    String document = "(Avatar est LE film 3D isn't good.] ";
    //String pattern = "[()<>\\[\\],~&;:\"\\-/=*#]";
    String pattern = "(not|n't|without|never|no)[\\s]+";
    String replace = " not";
    document = Pattern
        .compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
        .matcher(document).replaceAll(replace);
  }
}
