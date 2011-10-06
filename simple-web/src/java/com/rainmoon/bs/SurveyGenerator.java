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
public class SurveyGenerator extends HttpServlet {

    static final String PARAGRAPH_FILE = "WEB-INF/paragraphs.txt";
    static final String[] OPTIONS = { "very useful", "useful", "useless" };
    static final String PARAM_PARAIDS = "paraids";
    List<Paragraph> paragraphs;

    @Override
    public void init() throws ServletException {
        paragraphs = ParagraphsReader.readParagraphs(
                getServletContext().getResourceAsStream(PARAGRAPH_FILE));
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
            out.println("<title>A beautiful summary survey</title></head><body>");
            out.println("<h2>Instruction goes here</h2>");
            out.println("<form id='survey' method='post' action='/bs-summary/handleSurvey'>");
            int numParagraphs = 5;
            List<Integer> idxs = getRandomParagraphIds(numParagraphs);
            out.println(String.format("<input type='hidden' name='%s' value='%s' />",
                    PARAM_PARAIDS, paraIdsToString(idxs)));
            for (int idx : idxs) {
                Paragraph para = paragraphs.get(idx);
                out.println("<div class='paragraph'>");
                out.println("<span class='review'>Review: </span>" + para.paragraph);
                out.println("<br />");
                for (int segmentIdx = 0; segmentIdx < para.segments.size(); segmentIdx++) {
                    String segment = para.segments.get(segmentIdx);
                    out.println("<span class='segment'>");
                    out.println(segment);
                    // segment's name: id_segmentIdx
                    out.println(String.format("<input type='hidden' name='%s' value='%s' />",
                            para.id + "_" + segmentIdx, segment));
                    out.println("</span>");
                    for (int choice = 0; choice < 3; choice++) {
                        // checkbox's name: id_segmentIdx_radio
                        out.print(String.format("<input type='radio' name='%s', value='%d' />",
                                para.id + "_" + segmentIdx + "_radio", choice + 1));
                        out.print(OPTIONS[choice]);
                    }
                    out.println("<br />");
                }
                
                out.println("</div>");
            }
            out.println("<div class='paragraph'><span style='margin-left:250px'> <input type='submit' value='Complete' /></span></div>");
            out.println("</form>");
            out.println("</body></html>");
        } finally {
            out.close();
        }
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
            int idx = (int) (Math.random() * paragraphs.size());
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
