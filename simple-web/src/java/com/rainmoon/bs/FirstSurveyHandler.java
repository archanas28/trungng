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
 * Processes user survey.
 * 
 * @author trung
 */
public class FirstSurveyHandler extends HttpServlet {

    static final String RESULT_FILE = "WEB-INF/first.csv";
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
        List<Integer> summaryIds = stringToParaIds(request.getParameter(
                FirstSurveyGenerator.PARAM_SUMMARYIDS));
        String file = getServletContext().getRealPath(RESULT_FILE);
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileOutputStream(file, true));
            for (int idx : summaryIds) {
                Summary summary = summaries.get(idx);
                writeUserRatings(out, request, summary.id, summary.segments,
                        Summary.TYPE_SEGMENT);
                writeUserRatings(out, request, summary.id, summary.wordpairs,
                        Summary.TYPE_WORDPAIR);
            }
            out.close();
        } catch (Exception e) {
            log("Error writing result to file.");
        } finally {
            out.close();
        }
    }

    private String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return sdf.format(cal.getTime());
    }

    /**
     * Converts a string back into para ids.
     * @param s
     */
    private List<Integer> stringToParaIds(String s) {
        List<Integer> ids = new ArrayList<Integer>();
        for (String id : s.split("_")) {
            ids.add(Integer.parseInt(id));
        }

        return ids;
    }

    /**
     * Returns 'order' of the next survey to be generated.
     * @param request
     * @return 
     */
    private int getNextSurveyth(HttpServletRequest request) {
        String value = request.getParameter(FirstSurveyGenerator.PARAM_SURVEYTH);
        if (value != null) {
            return Integer.parseInt(value) + 1;
        } else {
            return 1;
        }
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
        if (nextSurveyth <= FirstSurveyGenerator.NUM_SURVEYS) {
            request.setAttribute(FirstSurveyGenerator.ATTR_SURVEYTH, nextSurveyth);
            request.getRequestDispatcher("/apple").forward(request, response);
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

    private <T> void writeUserRatings(PrintWriter out, HttpServletRequest request,
            int summaryId, List<T> list, String summaryType) {
        for (int segmentIdx = 0; segmentIdx < list.size(); segmentIdx++) {
            String name = FirstSurveyGenerator.radioName(summaryId,
                    summaryType, segmentIdx);
            log(request.getParameter(name));
            String userRating = request.getParameter(name);
            log(String.format("%s: %s", list.get(segmentIdx), userRating));
            out.printf("%s,%s,%s,%s,%s,%s\n", now(), request.getRemoteAddr(), summaryId,
                    summaryType, list.get(segmentIdx), userRating);
        }
    }
}
