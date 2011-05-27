package edu.kaist.uilab.asc.crawler;

/**
 * Interface for collecting blog posts.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public interface PostCollectorInterface {
  public String getPost(String url);
}
