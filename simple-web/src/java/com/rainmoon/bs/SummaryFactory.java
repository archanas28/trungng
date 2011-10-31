/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rainmoon.bs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * A singleton that provides summaries for generating user surveys.
 * 
 * @author trung
 */
@WebListener
public class SummaryFactory implements ServletContextListener {

    public static final int FIRST = 1;
    public static final int SECOND = 2;
    static final int FIRST_DUMMY_SUMMARY_ID = -1;
    static final int SECOND_DUMMY_SUMMARY_ID = -2;
    private static final int REVIEWS_PER_ENTITY = 6;
    private static final String DELIMITER = ",";
    
    private static final String FIRST_STUDY = "WEB-INF/summaries1.txt";
    private static final String SECOND_STUDY = "WEB-INF/summaries2.txt";
    private static List<Summary> firstSummaries;
    private static List<Summary> secondSummaries;

    public SummaryFactory() {
    }

    /**
     * Returns all summaries for a user study.
     * 
     * @param type
     *       first or second user study
     */
    public static List<Summary> getSummaries(int type) {
        if (type == FIRST) {
            return firstSummaries;
        }
        if (type == SECOND) {
            return secondSummaries;
        }

        return Collections.EMPTY_LIST;
    }
    
    /**
     * Returns a dummy summary.
     * 
     * @return 
     */
    public static Summary getDummySummary() {
       Summary dummy = new Summary(FIRST_DUMMY_SUMMARY_ID);
       dummy.addContent("This is an affordable and fairly basic coffeemaker. It makes very hot coffee. It also keeps the coffee consistently hot through the final cup.");
       List<String> segments = new ArrayList<String>();
       segments.add("price is way too expensive");
       segments.add("design is out of this world");
       List<String> wordpairs = new ArrayList<String>();
       wordpairs.add("cheap design");
       wordpairs.add("poor customer service");
       dummy.addSegments(segments);
       dummy.addWordpairs(wordpairs);
       
       return dummy;
    }

    /**
     * Returns another dummy summary.
     * 
     * @return 
     */
    public static Summary getSecondDummySummary() {
       Summary dummy = new Summary(SECOND_DUMMY_SUMMARY_ID);
       dummy.addContent("This is a highly efficient coffeemaker. I purchased it two months ago and it has never failed to work. The Mr. Coffee CG12 Drip Coffee Maker gets the job done; it offers full control over the brewing process and many extra features that make it worthwhile.");
       List<String> segments = new ArrayList<String>();
       segments.add("coffee maker makes terrible coffee");
       segments.add("coffee tasted lousy");
       List<String> wordpairs = new ArrayList<String>();
       wordpairs.add("not recommended");
       wordpairs.add("poor quality");
       dummy.addSegments(segments);
       dummy.addWordpairs(wordpairs);
       
       return dummy;
    }
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        firstSummaries = new ArrayList<Summary>();
        secondSummaries = new ArrayList<Summary>();
        readSummaries(context, FIRST_STUDY, FIRST, firstSummaries);
        readSummaries(context, SECOND_STUDY, SECOND, secondSummaries);
    }

    /**
     * Reads summaries from <code>textFile</code> into the list of summaries <code>summaries</code>
     * for the given user study <code>type</code>.
     * 
     * @param textFile
     * @param type
     * @param summaries 
     */
    private void readSummaries(ServletContext context, String textFile, int type,
            List<Summary> summaries) {
        BufferedReader in;
        try {
            int idx = 0;
            in = new BufferedReader(new InputStreamReader(context.getResourceAsStream(
                    textFile)));
            String line = null;
            while ((line = in.readLine()) != null) {
                Summary summary = new Summary(idx++);
                // read content
                summary.addContent(line);
                if (type == SECOND) {
                    for (int i = 0; i < REVIEWS_PER_ENTITY - 1; i++) {
                        summary.addContent(in.readLine());
                    }
                }
                summary.addSegments(Arrays.asList(in.readLine().split(DELIMITER)));
                summary.addWordpairs(Arrays.asList(in.readLine().split(DELIMITER)));
                summaries.add(summary);
            }
            in.close();
        } catch (Exception e) {
            context.log("Problem reading summaries.");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        firstSummaries = null;
        secondSummaries = null;
    }
}
