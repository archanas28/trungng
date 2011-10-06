/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rainmoon.bs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * A singleton that provides paragraphs for generating user surveys.
 * 
 * @author trung
 */
@WebListener
public class ParagraphsFactory implements ServletContextListener {

    static final String PARAGRAPH_FILE = "WEB-INF/paragraphs.txt";
    static List<Paragraph> paragraphs;

    public ParagraphsFactory() {
    }

    /**
     * Returns all paragraphs for generating surveys.
     */
    public static List<Paragraph> getParagraphs() {
        return paragraphs;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        paragraphs = new ArrayList<Paragraph>();
        BufferedReader in;
        try {
            int idx = 0;
            in = new BufferedReader(new InputStreamReader(context.getResourceAsStream(
                    PARAGRAPH_FILE)));
            String para = null;
            while ((para = in.readLine()) != null) {
                paragraphs.add(new Paragraph(idx++, para, Arrays.asList(in.readLine().split(","))));
            }
            in.close();
        } catch (Exception e) {
            context.log("Problem reading paragraphs");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        paragraphs = null;
    }
}
