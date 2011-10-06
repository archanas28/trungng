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
public class SurveyHandler extends HttpServlet {

    static final String RESULT_FILE = "WEB-INF/ratings.csv";
    List<Paragraph> paragraphs;

    @Override
    public void init() {
        paragraphs = ParagraphsFactory.getParagraphs();
    }

    /**
     * Handles a completed user survey.
     * 
     * @param request 
     */
    private void handleSurvey(HttpServletRequest request) {
        request.getHeader("");
        List<Integer> paraIds = stringToParaIds(request.getParameter(
                SurveyGenerator.PARAM_PARAIDS));
        String file = getServletContext().getRealPath(RESULT_FILE);
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileOutputStream(file, true));
            for (int idx : paraIds) {
                Paragraph para = paragraphs.get(idx);
                for (int segmentIdx = 0; segmentIdx < para.segments.size(); segmentIdx++) {
                    log(request.getParameter(SurveyGenerator.radioName(para.id, segmentIdx)));
                    String userRating = request.getParameter(SurveyGenerator.radioName(para.id, segmentIdx));
                    int rating = -1;
                    if (userRating != null) {
                        rating = Integer.parseInt(userRating);
                    }
                    log(String.format("%s: %s", para.segments.get(segmentIdx), rating));
                    out.printf("%s,%s,%s,%s\n", now(), para.id,
                            para.segments.get(segmentIdx), rating);
                }
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
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleSurvey(request);
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Survey completed!</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>You deserve this <img src='apple.jpg' alt='Big Apple'/> for completing the survey. Thank you :-) </h1>");
            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
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
