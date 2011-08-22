package edu.kaist.uilab.bs.evaluation;

import java.io.IOException;

import edu.kaist.uilab.bs.BSModel;

/**
 * Analysis for senti-aspect pairs.
 * 
 * @author trung
 */
public class SentiAspectAnalysis {

  public static void main(String[] args) throws IOException {
    String dir = "C:/datasets/bs/ursa/T7-A0.1-B0.0010-G0.10,0.10-I1000(top50)";
    BSModel model = BSModel.loadModel(dir + "/1000/model.gz");
    int numTopWords = 100;
//    model.writeSampleSummarizedDocuments(dir + "/1000/summarizeDocs100.html",
//        model.getTopSentiWords(numTopWords),
//        model.getTopAspectWords(numTopWords));
    numTopWords = 200;
    model.writeSampleSummarizedDocuments(dir + "/1000/summarizeDocs200.html",
        model.getTopSentiWords(numTopWords),
        model.getTopAspectWords(numTopWords));
  }
}
