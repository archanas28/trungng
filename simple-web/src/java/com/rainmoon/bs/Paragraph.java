/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rainmoon.bs;

import java.util.List;

/**
 * Represents a paragraph and its summary segments.
 * 
 * @author trung
 */
public class Paragraph {
   int id;
   String paragraph;
   List<String> segments;
   int timesRated;
   
   public Paragraph(int id, String paragraph, List<String> segments) {
       this.id = id;
       this.paragraph = paragraph;
       this.segments = segments;
       timesRated = 0;
   }
   
   public void increaseRatedTimes() {
       timesRated++;
   }
   
   public int getRatedTimes() {
       return timesRated;
   }
}
