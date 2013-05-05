package com.rainmoon.nextnumber;

import java.io.Serializable;

/**
 * A game instance for playing.
 * <p>
 * A game is uniquely determined by a sequence.
 * 
 * @author trung
 */
public class Game implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 5917420971594297225L;

  public Game(int gameNumber) {
  }

  /**
   * Gets the next number at position <code>pos</code> for this game.
   * 
   * @param pos
   * @return
   */
  public int getNumber(int pos) {
    return pos;
  }

}
