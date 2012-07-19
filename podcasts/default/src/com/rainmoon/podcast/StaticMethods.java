package com.rainmoon.podcast;

import android.os.Build;

/**
 * Provides helper static methods.
 * 
 * @author trung nguyen
 * 
 */
public class StaticMethods {
  public static final boolean POSTGINGERBREAD = !Build.VERSION.RELEASE
      .startsWith("1") && !Build.VERSION.RELEASE.startsWith("2");

}
