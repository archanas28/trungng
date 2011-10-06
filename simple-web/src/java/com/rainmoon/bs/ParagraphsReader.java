/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rainmoon.bs;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads content of the local file containing paragraphs and its extracted segments.
 * 
 * @author trung
 */
public class ParagraphsReader {

    /**
     * Reads all paragraphs from the given input stream <code>istream</code>.
     */
    public static List<Paragraph> readParagraphs(InputStream istream) {
        List<Paragraph> list = new ArrayList<Paragraph>();
        BufferedReader in;
        try {
            int idx = 0;
            in = new BufferedReader(new InputStreamReader(istream));
            String para = null;
            while ((para = in.readLine()) != null) {
                list.add(new Paragraph(idx++, para, Arrays.asList(in.readLine().split(","))));
            }
            in.close();
        } catch (Exception e) {
        }

        return list;
    }
}
