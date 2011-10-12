package edu.kaist.uilab.bs.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.kaist.uilab.asc.data.Review;
import edu.kaist.uilab.asc.data.ReviewWithProsAndCons;

/**
 * Converts the URSA dataset into our own format for use.
 * 
 * @author trung
 */
public class UrsaDataset {

  /**
   * Converts all xml reviews into one text file.
   */
  public void xmlsToText(String textFile) {
    try {
      PrintWriter out = new PrintWriter(new OutputStreamWriter(
          new FileOutputStream(textFile), "utf-8"));
      HashMap<String, ArrayList<Review>> map = getReviews();
      for (Entry<String, ArrayList<Review>> entry : map.entrySet()) {
        ArrayList<Review> reviews = entry.getValue();
        for (Review review : reviews) {
          out.println(review);
        }
      }
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns a map between a restaurant (id) and all of its reviews.
   * 
   * @return
   */
  public HashMap<String, ArrayList<Review>> getReviews() {
    HashMap<String, ArrayList<Review>> map = new HashMap<String, ArrayList<Review>>();
    String xmlDir = "C:/datasets/ursa/citysearch_data";
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder db = dbf.newDocumentBuilder();
      File dir = new File(xmlDir);
      for (File xmlFile : dir.listFiles()) {
        Document doc = db.parse(xmlFile);
        String name = xmlFile.getName();
        String restaurantId = name.substring(0, name.length() - 4);
        map.put(restaurantId, documentToReviews(restaurantId, doc));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return map;
  }

  /**
   * Gets all reviews of a restaurant in the given document.
   * 
   * @param doc
   * @return
   */
  private ArrayList<Review> documentToReviews(String restaurantId, Document doc) {
    ArrayList<Review> list = new ArrayList<Review>();
    NodeList reviews = doc.getElementsByTagName("Review");
    for (int idx = 0; idx < reviews.getLength(); idx++) {
      Node node = reviews.item(idx);
      String id = node.getAttributes().getNamedItem("id").getNodeValue();
      if (id.equals("0"))
        continue;
      String content = "";
      String rating = null;
      String pros = "";
      String cons = "";
      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        node = children.item(i);
        String nodeName = node.getNodeName();
        if (nodeName.equals("Rating")) {
          rating = node.getTextContent();
        } else if (nodeName.equals("Body")) {
          content = node.getTextContent().replaceAll("\n", "");
        } else if (nodeName.equals("Pros")) {
          pros = node.getTextContent().replaceAll("\n", "");
        } else if (nodeName.equals("Cons")) {
          cons = node.getTextContent().replaceAll("\n", "");
        }
      }
      if (rating.trim().length() == 0) {
        rating = "-1";
      }
      list.add(new ReviewWithProsAndCons(id, restaurantId, Double
          .parseDouble(rating), pros, cons, content));
    }

    return list;
  }

  /**
   * Writes the content of annotated files to a text file.
   */
  public void annotatedXmlsToTextFile() {
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

  private ArrayList<String> reviewNodeToSentences(Node review) {
    ArrayList<String> list = new ArrayList<String>();
    NodeList sentences = review.getChildNodes();
    for (int i = 0; i < sentences.getLength(); i++) {
      Node node = sentences.item(i);
      while (node != null && !isSentimentNode(node)) {
        node = node.getFirstChild();
      }
      if (node != null) {
        list.add(String.format("%s,%s", node.getNodeName(),
            node.getTextContent()));
        System.out.printf("%s,%s\n", node.getNodeName(), node.getTextContent());
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
  private boolean isSentimentNode(Node node) {
    String name = node.getNodeName();
    return (name.equalsIgnoreCase("positive")
        || name.equalsIgnoreCase("negative") || name
        .equalsIgnoreCase("conflict"));
  }

  public static void main(String args[]) {
    UrsaDataset ursa = new UrsaDataset();
    String textFile = "C:/datasets/ursa/docs.txt";
    ursa.xmlsToText(textFile);
    // ursa.getReviews();
  }
}
