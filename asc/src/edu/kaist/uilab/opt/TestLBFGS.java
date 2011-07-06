package edu.kaist.uilab.opt;

import edu.kaist.uilab.asc.util.InvalidArgumentException;
import edu.kaist.uilab.opt.LBFGS.ExceptionWithIflag;

/**
 * Tests the correctness of LBFGS.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class TestLBFGS {

  public static void main(String args[]) {
    int numCorrections = 4;
    int[] iflag = new int[2];
    double accuracy = 0.00001;
    boolean supplyDiag = false;
    double machinePrecision = Math.pow(Math.E, -64);
    int numVariables = 2;
    // starting point
    double[] x = new double[] { 100.0, 100.0 };
    double[] diag = new double[numVariables];
    // iprint[0] = output every iprint[0] iterations
    // iprint[1] = 0~3 : least to most detailed output
    int[] iprint = new int[] { 1, 1 };
    ObjectiveFunction func = new TestFunc1(numVariables);
    do {
      try {
        LBFGS.lbfgs(numVariables, numCorrections, x,
            func.computeFunction(x), func.computeGradient(x), supplyDiag,
            diag, iprint, accuracy, machinePrecision, iflag);
      } catch (ExceptionWithIflag e) {
        e.printStackTrace();
      } catch (InvalidArgumentException e) {

      }
    } while (iflag[0] == 1);

    System.out.printf("Best solution: %.10f %.10f", x[0], x[1]);
  }

  /*
   * A test function:
   * f(x) = 100(y - x^2)^2 + (1-x)^2
   * df/dx = 400x^3 - 400xy + 2x - 2
   * df/dy = 200(y-x^2)
   */
  public static class TestFunc1 implements ObjectiveFunction {
    int numVariables;
    
    public TestFunc1(int numVariables) {
      this.numVariables = numVariables;
    }

    @Override
    public double computeFunction(double[] vars)
        throws InvalidArgumentException {
      double temp = vars[1] - vars[0] * vars[0];
      return 100 * temp * temp + (1 - vars[0]) * (1 - vars[0]);
    }

    @Override
    public double[] computeGradient(double[] vars)
        throws InvalidArgumentException {
      double x = vars[0];
      double y = vars[1];
      double g1 = 400 * x * x * x - 400 * x * y + 2 * x - 2;
      double g2 = 200 * (y - x * x);

      return new double[] { g1, g2 };
    }
  }

  /*
   * A test function:
   * f(x) = x^2 + y^2
   * df/dx = 2x
   * df/dy = 2y
   */
  public static class TestFunc2 implements ObjectiveFunction {
    int numVars;
    
    public TestFunc2(int numVariables) {
      numVars = numVariables;
    }

    @Override
    public double computeFunction(double[] vars)
        throws InvalidArgumentException {
      return vars[0] * vars[0] + vars[1] * vars[1];
    }

    @Override
    public double[] computeGradient(double[] vars)
        throws InvalidArgumentException {
      return new double[] {2 * vars[0], 2 * vars[1]};
    }
  }
}
