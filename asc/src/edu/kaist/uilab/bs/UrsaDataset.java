package edu.kaist.uilab.bs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.kaist.uilab.asc.data.Review;

/**
 * Converts the URSA dataset into our own format for use.
 * 
 * @author trung
 */
public class UrsaDataset {

  /**
   * Converts all xml reviews into one text file.
   */
  private static void xmlsToText() {
    String xmlDir = "C:/datasets/ursa/citysearch_data";
    String textFile = "C:/datasets/bs/ursa/docs.txt";
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      PrintWriter out = new PrintWriter(new OutputStreamWriter(
          new FileOutputStream(textFile), "utf-8"));
      DocumentBuilder db = dbf.newDocumentBuilder();
      File dir = new File(xmlDir);
      for (File xmlFile : dir.listFiles()) {
        System.out.println(xmlFile.getName());
        Document doc = db.parse(xmlFile);
        ArrayList<Review> reviews = documentToReview(doc);
        for (Review review : reviews) {
          out.print(review);
        }
      }
      out.close();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Gets all reviews of a restaurant in the given document.
   * 
   * @param doc
   * @return
   */
  private static ArrayList<Review> documentToReview(Document doc) {
    ArrayList<Review> list = new ArrayList<Review>();
    NodeList reviews = doc.getElementsByTagName("Review");
    for (int idx = 0; idx < reviews.getLength(); idx++) {
      Node node = reviews.item(idx);
      String id = node.getAttributes().getNamedItem("id").getNodeValue();
      String content = "";
      String rating = null;
      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        node = children.item(i);
        if (node.getNodeName().equals("Rating")) {
          rating = node.getTextContent();
        } else if (node.getNodeName().equals("Body")) {
          content = node.getTextContent();
        }
      }
      if (rating.trim().length() == 0) {
        rating = "-1";
      }
      list.add(new Review(id, Double.parseDouble(rating), content));
    }

    return list;
  }

  private static void writeAnnotatedXmls() {
    String annotatedFile = "C:/datasets/ursa/ManuallyAnnotated_Corpus.xml";
    String textFile = "C:/datasets/bs/ursa/annotated.txt";
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      PrintWriter out = new PrintWriter(new OutputStreamWriter(
          new FileOutputStream(textFile), "utf-8"));
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(annotatedFile);
      NodeList reviews = doc.getElementsByTagName("Review");
      for (int idx = 0; idx < reviews.getLength(); idx++) {
        Node review = reviews.item(idx);
        String id = review.getAttributes().getNamedItem("id").getNodeValue();
        ArrayList<String> sentences = reviewNodeToSentences(review);
        out.printf("%s %d\n", id, sentences.size());
        for (String sentence : sentences) {
          out.println(sentence);
        }
      }
      out.close();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static ArrayList<String> reviewNodeToSentences(Node review) {
    ArrayList<String> list = new ArrayList<String>();
    NodeList sentences = review.getChildNodes();
    for (int i = 0; i < sentences.getLength(); i++) {
      Node node = sentences.item(i);
      while (node != null && !isSentimentNode(node)) {
        node = node.getFirstChild();
      }
      if (node != null) {
        list.add(String.format("%s,%s", node.getNodeName(), node.getTextContent()));
        System.out.printf("%s,%s\n", node.getNodeName(),
            node.getTextContent());
      }
    }
    
    return list;
  }

  /**
   * Returns true if this node is one of the positive, negative, or conflict
   * node.
   * 
   * @param sentence
   * @return
   */
  private static boolean isSentimentNode(Node node) {
    String name = node.getNodeName();
    return (name.equalsIgnoreCase("positive")
        || name.equalsIgnoreCase("negative") || name
        .equalsIgnoreCase("conflict"));
  }

  public static void main(String args[]) {
    // 1. convert all reviews in xml format to one text file.
    // 2. write all annotatated reviews in one file.
    // for accuracy calculation, document (review id) can be used to match.
    // xmlsToText();
    writeAnnotatedXmls();
  }
}
