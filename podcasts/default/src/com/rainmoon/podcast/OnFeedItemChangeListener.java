package com.rainmoon.podcast;

/**
 * The listener that is used when the feed item, e.g., one being shown in the
 * {@link FeedItemActivity}, is replaced by another feed item.
 * 
 * @author trung nguyen
 * 
 */
public interface OnFeedItemChangeListener {
  
  /**
   * This event is called whenever a new feed item is shown and hence its associate
   * audio source file become newly available.
   * 
   * @param url the new url of the audio in this feed item
   */
  public void onUrlChange(String url);
}
