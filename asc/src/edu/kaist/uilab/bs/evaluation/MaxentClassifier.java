package edu.kaist.uilab.bs.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Random;

import com.aliasi.classify.Classification;
import com.aliasi.classify.Classified;
import com.aliasi.classify.LogisticRegressionClassifier;
import com.aliasi.corpus.ObjectHandler;
import com.aliasi.corpus.XValidatingObjectCorpus;
import com.aliasi.stats.AnnealingSchedule;
import com.aliasi.stats.RegressionPrior;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.TokenFeatureExtractor;
import com.aliasi.util.FeatureExtractor;

import edu.kaist.uilab.bs.BSCorpusParser;

public class MaxentClassifier {
  private static final String POSITIVE = "Positive";
  private static final String NEGATIVE = "Negative";

  private File mTrainingFile;

  public MaxentClassifier(String trainingFile, String[] categories) {
    mTrainingFile = new File(trainingFile);
  }

  public void readTrainingSentences(
      XValidatingObjectCorpus<Classified<CharSequence>> corpus)
      throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(mTrainingFile), "utf-8"));
    String line;
    while ((line = in.readLine()) != null) {
      String[] part = line.split(" ");
      int numSentences = Integer.parseInt(part[1]);
      for (int i = 0; i < numSentences; i++) {
        line = in.readLine();
        int pos = line.indexOf(",");
        String sentence = BSCorpusParser.replacePatterns(line
            .substring(pos + 1).trim());
        String sentiment = line.substring(0, pos);
        if (sentiment.equalsIgnoreCase(POSITIVE)
            || sentiment.equalsIgnoreCase(NEGATIVE)) {
          Classified<CharSequence> classified = new Classified<CharSequence>(
              sentence, new Classification(sentiment));
          corpus.handle(classified);
        }
      }
    }
    in.close();
  }

  public void readTrainingDocuments(
      XValidatingObjectCorpus<Classified<CharSequence>> corpus)
      throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(mTrainingFile), "utf-8"));
    while (in.readLine() != null) {
      Double rating = Double.parseDouble(in.readLine());
      String content = in.readLine();
      String sentiment = null;
      if (rating >= 3) {
        sentiment = POSITIVE;
      } else if (rating > -1 && rating <= 2) {
        sentiment = NEGATIVE;
      }
      if (sentiment != null) {
        Classified<CharSequence> classified = new Classified<CharSequence>(
            content, new Classification(sentiment));
        corpus.handle(classified);
      }
    }
    in.close();
  }

  /**
   * Returns the {@link LogisticRegressionClassifier} for this set of data.
   * 
   * @return
   * @throws IOException
   * @throws FileNotFoundException
   */
  public LogisticRegressionClassifier<CharSequence> getClassifier(String outfile)
      throws IOException, FileNotFoundException {
    int numFolds = 7;
    XValidatingObjectCorpus<Classified<CharSequence>> corpus = new XValidatingObjectCorpus<Classified<CharSequence>>(
        numFolds);
    System.err.println("Reading training data");
    // readTrainingSentences(corpus);
    readTrainingDocuments(corpus);
    // destroys runs of categories
    long seed = 42L;
    corpus.permuteCorpus(new Random(seed));

    // setting parameters
    FeatureExtractor<CharSequence> featureExtractor = new TokenFeatureExtractor(
        new RegExTokenizerFactory("\\p{L}+|\\d+"));
    int minFeatureCount = 2;
    boolean addInterceptFeature = true;
    boolean noninformativeIntercept = true;
    RegressionPrior prior = RegressionPrior.gaussian(1.0,
        noninformativeIntercept);
    AnnealingSchedule annealingSchedule = AnnealingSchedule.exponential(
        0.00025, 0.999);
    double minImprovement = 0.000000001;
    int minEpochs = 100;
    int maxEpochs = 20000;
    PrintWriter out = new PrintWriter(outfile);
    LogisticRegressionClassifier<CharSequence> classifier = null;
    for (int fold = 0; fold < numFolds; fold++) {
      System.err.println("Validating fold " + fold);
      out.printf("Fold %d:", fold);
      corpus.setFold(fold);
      classifier = LogisticRegressionClassifier.<CharSequence> train(corpus,
          featureExtractor, minFeatureCount, addInterceptFeature, prior,
          annealingSchedule, minImprovement, minEpochs, maxEpochs, null);
      TestHandler evaluator = new TestHandler(classifier);
      corpus.visitTest(evaluator);
      out.println(evaluator.getEvaluationSummary());
      out.println("------------------------------");
    }
    out.close();

    return classifier;
  }

  public static void main(String[] args) throws Exception {
    String dir = "C:/datasets/bs/restaurants";
    // MaxentClassifier mc = new MaxentClassifier(dir + "/annotated.txt",
    // new String[] { "Positive", "Negative" });
    MaxentClassifier mc = new MaxentClassifier(dir + "/docs.txt", new String[] {
        "Positive", "Negative" });
    mc.getClassifier(dir + "/lr-documents.txt");
    // ConditionalClassification classified = classifier
    // .classify("This camera is great!");
    // System.out.println(classified.bestCategory());
  }

  class TestHandler implements ObjectHandler<Classified<CharSequence>> {
    LogisticRegressionClassifier<CharSequence> classifier;
    private int posInstances;
    private int negInstances;
    private int posCorrect;
    private int posWrong;
    private int negCorrect;
    private int negWrong;

    public TestHandler(LogisticRegressionClassifier<CharSequence> classifier) {
      this.classifier = classifier;
      posCorrect = 0;
      posWrong = 0;
      negCorrect = 0;
      negWrong = 0;
    }

    public String getEvaluationSummary() {
      StringBuilder builder = new StringBuilder();
      builder.append(String.format("#posInstances = %d, #negInstances = %d\n",
          posInstances, negInstances));
      builder.append(String.format("#posCorrect = %d, #posWrong = %d\n",
          posCorrect, posWrong));
      builder.append(String.format("#negCorrect = %d, #negWrong = %d\n",
          negCorrect, negWrong));
      builder.append(String.format("accuracy (positive + negative): %.3f\n",
          (posCorrect + negCorrect + 0.0) / (posInstances + negInstances)));
      double precision = (posCorrect + 0.0) / (posCorrect + posWrong);
      double recall = (posCorrect + 0.0) / posInstances;
      builder.append("-------------\nPositive\n");
      builder.append(String.format("precision: %.3f\n", precision));
      builder.append(String.format("recall: %.3f\n", recall));
      builder.append(String.format("f-measure: %.3f\n", 2 * precision * recall
          / (precision + recall)));
      precision = (negCorrect + 0.0) / (negCorrect + negWrong);
      recall = (negCorrect + 0.0) / negInstances;
      builder.append("-------------\nNegative\n");
      builder.append(String.format("precision: %.3f\n", precision));
      builder.append(String.format("recall: %.3f\n", recall));
      builder.append(String.format("f-measure: %.3f\n", 2 * precision * recall
          / (precision + recall)));
      System.out.println(builder.toString());

      return builder.toString();
    }

    @Override
    public void handle(Classified<CharSequence> e) {
      // e is the reference category, i.e., the classified (annotated) object
      String refCategory = e.getClassification().bestCategory();
      if (refCategory.equals(POSITIVE)) {
        posInstances++;
      } else {
        negInstances++;
      }
      String classifiedCategory = classifier.classify(e.getObject())
          .bestCategory();
      if (refCategory.equals(classifiedCategory)) {
        if (classifiedCategory.equals(POSITIVE)) {
          posCorrect++;
        } else {
          negCorrect++;
        }
      } else {
        if (classifiedCategory.equals(POSITIVE)) {
          posWrong++;
        } else {
          negWrong++;
        }
      }
    }
  }
}
