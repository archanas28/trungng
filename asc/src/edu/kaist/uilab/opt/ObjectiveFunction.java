package edu.kaist.uilab.opt;

import edu.kaist.uilab.asc.util.InvalidArgumentException;

/**
 * Interface for objective function.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public interface ObjectiveFunction {
  /**
   * Computes the value of the objective function at point {@code vars}.
   * 
   * @param x
   * @return
   * @throws InvalidArgumentException
   *           if the dimension of the input array does not equal the number of
   *           variables
   */
  public double computeFunction(double[] variables)
      throws InvalidArgumentException;

  /**
   * Computes the value of the gradient at point {@code vars}.
   * 
   * @param vars
   * @return
   * @throws InvalidArgumentException
   *           if the dimension of the input array does not equal the number of
   *           variables
   */
  public double[] computeGradient(double[] variables)
      throws InvalidArgumentException;
}
