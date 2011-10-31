/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rainmoon.bs;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author trung
 */
public class ThirdSurveyHandler extends HttpServlet {

    static final String RESULT_FILE = "WEB-INF/third.csv";
    List<Summary> summaries;

    @Override
    public void init() {
        summaries = SummaryFactory.getSummaries(SummaryFactory.FIRST);
    }

    /**
     * Handles a completed user survey.
     *
     * @param request
     */
    private void handleSurvey(HttpServletRequest request) {
        int summaryId = Integer.parseInt(request.getParameter(
                ThirdSurveyGenerator.PARAM_SUMMARY_ID));
        String file = getServletContext().getRealPath(RESULT_FILE);
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileOutputStream(file, true));
            Summary summary = summaries.get(summaryId);
            writeUserRatings(out, request, summary.id, summary.segments,
                    Summary.TYPE_SEGMENT);
            writeUserRatings(out, request, summary.id, summary.wordpairs,
                    Summary.TYPE_WORDPAIR);
            out.close();
        } catch (Exception e) {
            log("Error writing result to file.");
        } finally {
            out.close();
        }
    }

    private <T> void writeUserRatings(PrintWriter out, HttpServletRequest request,
            int summaryId, List<T> list, String summaryType) {
        for (int segmentIdx = 0; segmentIdx < list.size(); segmentIdx++) {
            String name = ThirdSurveyGenerator.radioName(summaryId,
                    summaryType, segmentIdx);
            log(request.getParameter(name));
            String userRating = request.getParameter(name);
            log(String.format("%s: %s", list.get(segmentIdx), userRating));
            out.printf("%s,%s,%s,%s,%s,%s\n", now(), request.getRemoteAddr(), summaryId,
                    summaryType, list.get(segmentIdx), userRating);
        }
    }

    private String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return sdf.format(cal.getTime());
    }

    /**
     * Returns 'order' of the next survey to be generated.
     * @param request
     * @return
     */
    private int getNextSurveyth(HttpServletRequest request) {
        String value = request.getParameter(ThirdSurveyGenerator.PARAM_SURVEYTH);
        if (value != null) {
            return Integer.parseInt(value) + 1;
        } else {
            return 1;
        }
    }

/**
     * Converts a string back into summary ids.
     * @param s
     */
    private List<Integer> stringToSummaryIds(String s) {
        List<Integer> ids = new ArrayList<Integer>();
        for (String id : s.split("_")) {
            ids.add(Integer.parseInt(id));
        }

        return ids;
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
        handleSurvey(request);
        int nextSurveyth = getNextSurveyth(request);
        if (nextSurveyth <= ThirdSurveyGenerator.NUM_SURVEYS) {
            request.setAttribute(ThirdSurveyGenerator.ATTR_SURVEYTH, nextSurveyth);
            request.setAttribute(ThirdSurveyGenerator.ATTR_SUMMARYIDS, stringToSummaryIds(
                    request.getParameter(ThirdSurveyGenerator.PARAM_SUMMARY_IDS)));
            request.getRequestDispatcher("/survey").forward(request, response);
        } else {
            request.getRequestDispatcher("/apple.jsp").forward(request, response);
        }
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
