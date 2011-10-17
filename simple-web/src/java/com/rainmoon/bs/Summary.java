/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rainmoon.bs;

import java.util.ArrayList;
import java.util.List;

/**
 * A summary consists of one or more reviews, a list of segments, and a list of
 * adj-noun pairs that summarize its content.
 * 
 * @author trung
 */
public class Summary {

    public static final String TYPE_SEGMENT = "segment";
    public static final String TYPE_WORDPAIR = "wordpair";
    
    int id;
    List<String> contents;
    List<String> segments;
    List<String> wordpairs;
    int timesRated;

    /**
     * Constructor
     * 
     * @param id
     */
    public Summary(int id) {
        this.id = id;
        this.contents = new ArrayList<String>();
        this.segments = new ArrayList<String>();
        this.wordpairs = new ArrayList<String>();
        timesRated = 0;
    }

    public void addContent(String content) {
        contents.add(content);
    }

    /**
     * Adds all segments in <code>list</code> to the list of segments for this summary.
     * @param list 
     */
    public final void addSegments(List<String> list) {
        segments.addAll(list);
    }

    /**
     * Adds all word pairs in <code>list</code> to the list of word pairs for this summary.
     * @param list 
     */
    public final void addWordpairs(List<String> list) {
        wordpairs.addAll(list);
    }

    public void increaseRatedTimes() {
        timesRated++;
    }

    /**
     * Returns all segments of this summary as a string.
     * @return 
     */
    public String getSegments() {
        return listToString(segments);
    }

    /**
     * Returns all word pairs of this summary as a string.
     * @return 
     */
    public String getWordpairs() {
        return listToString(wordpairs);
    }

    /**
     * Returns all elements of the given list as a string. Each element is separated
     * by a comma.
     * 
     * @param list
     * @return 
     */
    private String listToString(List<String> list) {
        StringBuilder bld = new StringBuilder();
        int size = list.size();
        if (size > 1) {
            for (int idx = 0; idx < size - 1; idx++) {
                bld.append(list.get(idx)).append(", ");
            }
        }    
        bld.append(list.get(size - 1));
        
        return bld.toString();
    }

    public int getRatedTimes() {
        return timesRated;
    }
}
