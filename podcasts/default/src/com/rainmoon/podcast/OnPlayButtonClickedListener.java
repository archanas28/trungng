package com.rainmoon.podcast;

/**
 * The listener that is used to request a media player to play from an external 
 * button.
 * 
 * @author trung nguyen
 * 
 */
public interface OnPlayButtonClickedListener {
  
  /**
   * This event is called when a play button is clicked.
   * 
   * @param url the url of the audio in this feed item
   */
  public void onPlayClicked(String url);
}
