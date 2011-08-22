package edu.kaist.uilab.bs.evaluation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import edu.kaist.uilab.asc.util.DoubleMatrix;
import edu.kaist.uilab.bs.BSModel;
import edu.kaist.uilab.bs.Document;

public class SentimentAnalysis {

  static void run(String dir) throws IOException {
    BSModel model = BSModel.loadModel(dir + "/model.gz");
    DoubleMatrix pi = model.getPi();
    int numPosCorrect = 0, numNegCorrect = 0;
    int numPosWrong = 0, numNegWrong = 0;
    int numNotRated = 0, numNeutral = 0, numPos = 0, numNeg = 0;
    List<Document> documents = model.getDocuments();
    for (int i = 0; i < documents.size(); i++) {
      Document document = documents.get(i);
      double rating = document.getRating();
      if (rating != 3.0 && rating != -1.0) {
        int observedSenti = rating > 3.0 ? 0 : 1;
        if (observedSenti == 0) {
          numPos++;
        } else {
          numNeg++;
        }
        int inferedSenti = pi.getValue(i, 0) >= pi.getValue(i, 1) ? 0 : 1;
        if (inferedSenti == observedSenti) {
          if (inferedSenti == 0) {
            numPosCorrect++;
          } else {
            numNegCorrect++;
          }
        } else {
          if (inferedSenti == 0) {
            numPosWrong++;
          } else {
            numNegWrong++;
          }
        }
      } else {
        if (rating == 3.0) {
          numNeutral++;
        } else {
          numNotRated++;
        }
      }
    }

    PrintWriter out = new PrintWriter(dir + "/newdocsentiment.txt");
    out.printf("#positive = %d, #negative = %d, #total subjective =%d\n",
        numPos, numNeg, numPos + numNeg);
    out.printf("#neutral =%d, #not rated = %d, sum = %d\n", numNeutral,
        numNotRated, numNeutral + numNotRated);
    out.printf(
        "#numPosCorrect = %d, numPosWrong = %d, numNegCorrect=%d, numNegWrong=%d\n",
        numPosCorrect, numPosWrong, numNegCorrect, numNegWrong);
    out.printf("accuracy (positive + negative): %.3f\n", (numPosCorrect
        + numNegCorrect + 0.0)
        / (numPos + numNeg));
    out.printf("precision (positive): %.3f\n", (numPosCorrect + 0.0)
        / (numPosCorrect + numPosWrong));
    out.printf("recall (positive): %.3f\n", (numPosCorrect + 0.0) / numPos);
    out.printf("precision (negative): %.3f\n", (numNegCorrect + 0.0)
        / (numNegCorrect + numNegWrong));
    out.printf("recall (negative): %.3f\n", (numNegCorrect + 0.0) / numNeg);
    out.close();
  }

  public static void main(String args[]) throws IOException {
    run("C:/datasets/bs/ursa/T50-A0.1-B0.0010-G0.10,0.10-I1000(newstop)--85.4best/1000");
    run("C:/datasets/bs/ursa/T30-A0.1-B0.0010-G0.10,0.10-I1000(newstop)/1000");
    run("C:/datasets/bs/big/T50-A0.1-B0.0010-G0.10,0.10-I1000(newstop)--79/1000");
    run("C:/datasets/bs/big/T30-A0.1-B0.0010-G0.10,0.10-I1000(newstop)--79/1000");
  }
}
