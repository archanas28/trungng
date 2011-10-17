/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rainmoon.bs;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Processes second survey.
 * 
 * @author trung
 */
public class SecondSurveyHandler extends HttpServlet {

    static final String RESULT_FILE = "WEB-INF/second.csv";
    List<Summary> summaries;

    @Override
    public void init() {
        summaries = SummaryFactory.getSummaries(SummaryFactory.SECOND);
    }

    /**
     * Handles a completed user survey.
     * 
     * @param request 
     */
    private void handleSurvey(HttpServletRequest request) {
        int index = Integer.parseInt(request.getParameter(
                SecondSurveyGenerator.PARAM_SUMMARYID));
        String file = getServletContext().getRealPath(RESULT_FILE);
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileOutputStream(file, true));
            Summary summary = summaries.get(index);
            writeUserRating(out, request, summary.id, Summary.TYPE_SEGMENT);
            writeUserRating(out, request, summary.id, Summary.TYPE_WORDPAIR);
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
        if (nextSurveyth <= SecondSurveyGenerator.NUM_SURVEYS) {
            request.setAttribute(SecondSurveyGenerator.ATTR_SURVEYTH, nextSurveyth);
            request.getRequestDispatcher("/orange").forward(request, response);
        } else {
            request.getRequestDispatcher("/orange.jsp").forward(request, response);
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

    private void writeUserRating(PrintWriter out, HttpServletRequest request,
            int summaryId, String summaryType) {
        String userRating = request.getParameter(summaryType);
        log(String.format("%s: %s", summaryType, userRating));
        out.printf("%s,%s,%s,%s,%s\n", now(), request.getRemoteAddr(), summaryId,
                summaryType, userRating);
    }
}
