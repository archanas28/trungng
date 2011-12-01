package edu.kaist.cs408.cdms.util;

import java.net.URLEncoder;

/**
 * Util class to build encoded urls.
 * 
 * @author Trung
 *
 */
public class UrlBuilder {
  
  private StringBuilder builder;
  private boolean firstParam = true;
  
  /**
   * Constructor
   * 
   * @param url
   */
  public UrlBuilder(String url) {
    builder = new StringBuilder(url);
  }
  
  /**
   * Appends {@code param} to this url.
   * 
   * @param param
   * @param value
   * @return
   */
  public UrlBuilder appendParam(String param, String value) {
    if (firstParam) {
      builder.append("?");
      firstParam = false;
    } else {
      builder.append("&");
    }
    builder.append(URLEncoder.encode(param)).append("=").append(
        URLEncoder.encode(value));
    return this;
  }
  
  /**
   * Returns the encoded url.
   */
  public String toString() {
    return builder.toString();
  }
}
