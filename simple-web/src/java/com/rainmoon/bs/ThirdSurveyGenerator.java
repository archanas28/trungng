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
 *
 * @author trung
 */
public class ThirdSurveyGenerator extends HttpServlet {

    static final String[] OPTIONS = {"very useful", "useful", "somewhat useful", "useless"};
    static final String PARAM_SUMMARY_ID = "com.rainmoon.bs.ThirdSurveyGenerator.summaryId";
    static final String PARAM_SUMMARY_IDS = "com.rainmoon.bs.ThirdSurveyGenerator.summaryIds";
    static final String PARAM_SURVEYTH = "com.rainmoon.bs.ThirdSurveyGenerator.survey";
    static final String ATTR_SUMMARYIDS = "com.rainmoon.bs.ThirdSurveyGenerator.summaryIds";
    static final String ATTR_SURVEYTH = "com.rainmoon.bs.ThirdSurveyGenerator.survey";
    static final int NUM_SURVEYS = 6;
    static final String INSTR = "<b>Instruction:</b> Below are text segments extracted from reviews of a coffee maker product."
            + " Please rate a each item on its usefulness, i.e., how much does a text segment help you learn about the coffee maker?";

    private static final String SURVEY_HANDLER_URL = "/bs-summary/done";
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
        int surveyth = getSurveyth(request);
        List<Integer> idxs;
        if (surveyth == 1) {
            idxs = getRandomSummaryIds();
        } else {
            idxs = (ArrayList<Integer>) request.getAttribute(ATTR_SUMMARYIDS);
        }
        int idx = idxs.get(surveyth - 1);
        Summary summary = summaries.get(idx);
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            writeTitleAndInstruction(out);
            out.println(String.format("<h3>Question %d of %d</h3>", surveyth, NUM_SURVEYS));
            out.println("<form id='survey' method='post' action='" + SURVEY_HANDLER_URL + "'>");
            out.println(String.format("<input type='hidden' name='%s' value='%d' />", PARAM_SURVEYTH, surveyth));
            out.println(String.format("<input type='hidden' name='%s' value='%s' />", PARAM_SUMMARY_ID, summary.id));
            out.println(String.format("<input type='hidden' name='%s' value='%s' />",
                    PARAM_SUMMARY_IDS, summaryIdsToString(idxs)));
            if (surveyth <= NUM_SURVEYS / 2) {
                generateOptions(out, summary.id, Summary.TYPE_SEGMENT, summary.segments);
                generateOptions(out, summary.id, Summary.TYPE_WORDPAIR, summary.wordpairs);
            } else {
                generateOptions(out, summary.id, Summary.TYPE_WORDPAIR, summary.wordpairs);
                generateOptions(out, summary.id, Summary.TYPE_SEGMENT, summary.segments);
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

    private void writeTitleAndInstruction(PrintWriter out) {
        out.println("<html><head>");
        out.println("<link rel='stylesheet' type='text/css' href='survey.css'>");
        out.println("<title>Survey about coffee maker reviews</title></head><body>");
        out.println("<p class='instruction'>");
        out.println(INSTR);
        out.println("</p>");
        out.println("<hr class='hr'><br />");
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
     * Converts a list of summary ids to string.
     *
     * <p> Each id is separated by a underscore '_' character.
     * @param paraIds
     * @return
     */
    private String summaryIdsToString(List<Integer> paraIds) {
        StringBuilder builder = new StringBuilder();
        for (int id : paraIds) {
            builder.append(id).append("_");
        }

        return builder.toString();
    }

    /**
     * Returns random summary ids for a user survey.
     *
     * @return
     *       a list of summary ids; the size of the list is the number of surveys
     */
    private List<Integer> getRandomSummaryIds() {
        List<Integer> list = new ArrayList<Integer>(NUM_SURVEYS);
        do {
            int idx = (int) (Math.random() * summaries.size());
            if (!list.contains(idx)) {
                list.add(idx);
            }
        } while (list.size() < NUM_SURVEYS / 2);
        // duplicate each review to check the consistency of answers provided by users
        for (int i = 0; i < NUM_SURVEYS / 2; i++) {
            list.add(list.get(i));
        }

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

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
