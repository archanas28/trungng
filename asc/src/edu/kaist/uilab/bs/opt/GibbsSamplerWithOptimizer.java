package edu.kaist.uilab.bs.opt;

import java.io.IOException;

import edu.kaist.uilab.bs.Document;
import edu.kaist.uilab.bs.GibbsSampler;
import edu.kaist.uilab.bs.Model;

/**
 * Gibbs sampling with optimization.
 * 
 * @author trung
 */
public class GibbsSamplerWithOptimizer extends GibbsSampler {
  double optimizationAccuracy;
  int optimizationInterval;

  public GibbsSamplerWithOptimizer(Model model, double optimizationAccuracy,
      int optimizationInterval) {
    super(model);
    this.optimizationAccuracy = optimizationAccuracy;
    this.optimizationInterval = optimizationInterval;
  }

  @Override
  public void gibbsSampling(int numIters, int startingIter, int savingInterval,
      int burnin) throws IOException {
    System.out.printf("Gibbs sampling started (Iterations: %d)\n", numIters);
    if (startingIter == 0) {
      initDocs(0, model.getNumDocuments());
    }  
    double startTime = System.currentTimeMillis();
    for (int iter = startingIter; iter < numIters; iter++) {
      int realIter = iter + 1;
      if (realIter % 50 == 0) {
        System.out.println();
      }
      System.out.printf(" %d ", realIter);
      if (realIter >= burnin && realIter % optimizationInterval == 0) {
        ((OptimizationModel) model).optimizeBeta(optimizationAccuracy);
      }
      for (Document document : model.getDocuments()) {
        sampleForDoc(document);
      }
      if (realIter > burnin && realIter % savingInterval == 0
          && realIter != numIters) {
        model.writeModelOutput(realIter);
      }
    }
    System.out.printf("\nGibbs sampling terminated. (%.4fs)\n",
        (System.currentTimeMillis() - startTime) / 1000);
    model.writeModelOutput(numIters);
  }
}
