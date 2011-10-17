/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rainmoon.bs;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Generates survey for the second user study.
 * 
 * @author trung
 */
public class SecondSurveyGenerator extends HttpServlet {

    public static final String TYPE_SEGMENT = "segment";
    public static final String TYPE_WORDPAIR = "wordpair";
    static final String PARAM_SUMMARYID = "summaryId";
    static final String PARAM_SURVEYTH = "survey";
    static final String ATTR_SURVEYTH = "survey";
    static final int NUM_SURVEYS = 3;
    static final String[] OPTIONS = {"very useful", "useful", "somewhat useful", "useless"};
    static final String INSTR = "<b>Instruction:</b> Below are short reviews of a coffee maker. Please read them carefully and"
            + " then scroll down to the bottom of the page to answer the question.";
    static final String RATING_INSTR = "Following are 2 summaries of the reviews that you just read. Your job is to rate each summary"
            + " on its usefulness. Your decision should be based on 2 criteria:";
    static final String CRITERIA1 = "whether the summary is related to the reviews";
    static final String CRITERIA2 = "whether the summary is informative to your evaluation of the product";
    
    List<Summary> summaries;
    private static final String SURVEY_HANDLER_URL = "/bs-summary/eatOrange";

    @Override
    public void init() throws ServletException {
        summaries = SummaryFactory.getSummaries(SummaryFactory.SECOND);
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
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            out.println("<html><head>");
            out.println("<link rel='stylesheet' type='text/css' href='survey.css'>");
            out.println("<title>Only a few minutes</title></head><body>");
            out.println("<p class='instruction'>");
            out.println(INSTR);
            out.println("</p>");
            out.println("<hr class='hr'><br />");
            int surveyth = getSurveyth(request);
            out.println(String.format("<h3>Question %d of %d</h3>", surveyth, NUM_SURVEYS));
            out.println("<form id='survey' method='post' action='" + SURVEY_HANDLER_URL + "'>");
            Summary summary = summaries.get((int) (Math.random() * summaries.size()));
            out.println(String.format("<input type='hidden' name='%s' value='%s' />",
                    PARAM_SUMMARYID, summary.id));
            out.println(String.format("<input type='hidden' name='%s' value='%d' />", PARAM_SURVEYTH, surveyth));
            for (int idx = 0; idx < summary.contents.size(); idx++) {
                out.printf("<div class='paragraph'><span class='review'>Review %d: </span>%s</div>\n",
                        idx + 1, summary.contents.get(idx));
            }
            // rating part
            out.println("<hr class='hr'>");
            out.println("<p class='instruction'>");
            out.println(RATING_INSTR);
            out.println("<ul>");
            out.println("<li>" + CRITERIA1 + "</li>");
            out.println("<li>" + CRITERIA2 + "</li>");
            out.println("</ul></p>");
            out.print(String.format("<p><div class='items'>%s</div>", summary.getSegments()));
            generateOptions(out, TYPE_SEGMENT);
            out.println("</p>");
            out.print(String.format("<p><div class='items'>%s</div>", summary.getWordpairs()));
            generateOptions(out, TYPE_WORDPAIR);
            out.println("</p>");
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
     * Generates options for a set of summary item.
     * @param out
     * @param summaryId
     */
    private void generateOptions(PrintWriter out, String summaryType) {
        out.println("<span class='options'>");
        for (int choice = 0; choice < OPTIONS.length; choice++) {
            // checkbox's name: id_segmentIdx
            out.print(String.format("<input type='radio' name='%s', value='%d' />",
                    summaryType, OPTIONS.length - choice));
            out.print(OPTIONS[choice]);
        }
        out.println("</span>");
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
