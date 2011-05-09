package edu.kaist.uilab.asc.util;

/**
 * Exception class for invalid arguments.
 * 
 * @author trung
 */
public class InvalidArgumentException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Default constructor
   * 
   * @param msg
   */
  public InvalidArgumentException(String msg) {
    super(msg);
  }
}
