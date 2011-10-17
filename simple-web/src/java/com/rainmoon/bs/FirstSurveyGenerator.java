/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rainmoon.bs;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Generates surveys.
 * 
 * @author trung
 */
public class FirstSurveyGenerator extends HttpServlet {

    static final String[] OPTIONS = {"very useful", "useful", "somewhat useful", "useless"};
    static final String PARAM_SUMMARYIDS = "summaryIds";
    static final String PARAM_SURVEYTH = "survey";
    static final String ATTR_SURVEYTH = "survey";
    static final int NUM_SURVEYS = 3;
    static final String INSTR = "<b>Instruction:</b> You are going to read a short review about a coffee maker product."
            + " It is followed by several text segments extracted from the review content. Your job is to give each segment"
            + " a rating on its usefulness. Your decision should be based on 2 criteria: ";
    static final String CRITERIA1 = "whether the segment is related to the original review content";
    static final String CRITERIA2 = "whether the segment is informative to your evaluation of the product";
    private static final int SUMMARIES_PER_SURVEY = 1;
    private static final String SURVEY_HANDLER_URL = "/bs-summary/eatApple";
    List<Summary> summaries;

    @Override
    public void init() throws ServletException {
        summaries = SummaryFactory.getSummaries(SummaryFactory.FIRST);
    }

    private int getSurveyth(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_SURVEYTH);
        if (value == null) {
            value = 1;
        }
        return (Integer) value;
    }

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Integer> idxs = getRandomParagraphIds(SUMMARIES_PER_SURVEY);
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            out.println("<html><head>");
            out.println("<link rel='stylesheet' type='text/css' href='survey.css'>");
            out.println("<title>Only a few minutes</title></head><body>");
            out.println("<p class='instruction'>");
            out.println(INSTR);
            out.println("<ul>");
            out.println("<li>" + CRITERIA1 + "</li>");
            out.println("<li>" + CRITERIA2 + "</li>");
            out.println("</ul></p>");
            out.println("<hr class='hr'><br />");
            int surveyth = getSurveyth(request);
            out.println(String.format("<h3>Review %d of %d</h3>", surveyth, NUM_SURVEYS));
            out.println("<form id='survey' method='post' action='" + SURVEY_HANDLER_URL + "'>");
            out.println(String.format("<input type='hidden' name='%s' value='%d' />", PARAM_SURVEYTH, surveyth));
            out.println(String.format("<input type='hidden' name='%s' value='%s' />",
                    PARAM_SUMMARYIDS, paraIdsToString(idxs)));
            for (int idx : idxs) {
                Summary summary = summaries.get(idx);
                out.println("<p class='paragraph'>");
                out.println("<span class='review'>Review: </span>" + summary.contents.get(0));
                out.println("</p>");
                generateOptions(out, summary.id, Summary.TYPE_SEGMENT, summary.segments);
                generateOptions(out, summary.id, Summary.TYPE_WORDPAIR, summary.wordpairs);
            }
            out.println("<div class='paragraph'><span style='margin-left:250px'>");
            String label = "Next question";
            if (surveyth == NUM_SURVEYS) {
                label = "Complete";
            }
            out.println(String.format("<input type='submit' value='%s' />", label));
            out.println("</span></div>");
            out.println("</form>");
            out.println("</body></html>");
        } finally {
            out.close();
        }
    }

    /**
     * Generates options for each summary item.
     * @param <T>
     * @param out
     * @param summaryId
     * @param summaryType
     *       one of the two summary types available in {@link Summary}
     * @param items 
     */
    private <T> void generateOptions(PrintWriter out, int summaryId, String summaryType,
            List<T> items) {
        for (int idx = 0; idx < items.size(); idx++) {
            out.println(String.format("<span class='item'>%s</span>", items.get(idx)));
            out.println("<span class='options'>");
            for (int choice = 0; choice < OPTIONS.length; choice++) {
                // checkbox's name: id_segmentIdx
                out.print(String.format("<input type='radio' name='%s', value='%d' />",
                        radioName(summaryId, summaryType, idx), OPTIONS.length - choice));
                out.print(OPTIONS[choice]);
            }
            out.println("</span>");
            out.println("<br />");
        }
    }

    /**
     * Returns the name of a radio input type given the id of a summary and a segment.
     * 
     * @param summaryId
     * @param summaryType
     * @param segmentId
     * @return 
     */
    public static String radioName(int summaryId, String summaryType, int segmentId) {
        return summaryId + "_" + summaryType + "_" + segmentId;
    }

    /**
     * Converts a list of param ids to string.
     * 
     * <p> Each id is separated by a underscore '_' character.
     * @param paraIds
     * @return 
     */
    private String paraIdsToString(List<Integer> paraIds) {
        StringBuilder builder = new StringBuilder();
        for (int id : paraIds) {
            builder.append(id).append("_");
        }

        return builder.toString();
    }

    /**
     * Returns random paragraph ids for a user survey.
     * @param numParagraphs
     * @return 
     */
    private List<Integer> getRandomParagraphIds(int numParagraphs) {
        List<Integer> list = new ArrayList<Integer>(numParagraphs);
        do {
            int idx = (int) (Math.random() * summaries.size());
            if (!list.contains(idx)) {
                list.add(idx);
            }
        } while (list.size() < numParagraphs);

        return list;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
