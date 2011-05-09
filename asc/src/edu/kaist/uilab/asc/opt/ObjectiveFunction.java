package edu.kaist.uilab.asc.opt;

import edu.kaist.uilab.asc.util.InvalidArgumentException;

/**
 * Abstract objective function.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public abstract class ObjectiveFunction {
  int numVars;
  
  /**
   * Constructor
   * 
   * @param numVariables
   */
  public ObjectiveFunction(int numVariables) {
    numVars = numVariables;
  }
  
  /**
   * Returns the number of variables.
   * 
   * @return
   */
  public int getNumVariables() {
    return numVars;
  }
  
  /**
   * Computes the value of the objective function at point {@code vars}.
   * 
   * @param vars
   * @return
   * @throws InvalidArgumentException
   *        if the dimension of the input array does not equal the number of variables
   */
  public abstract double computeFunction(double[] vars) throws InvalidArgumentException;

  /**
   * Computes the value of the gradient at point {@code vars}.
   * 
   * @param vars
   * @return
   * @throws InvalidArgumentException
   *        if the dimension of the input array does not equal the number of variables
   */
  public abstract double[] computeGradient(double[] vars) throws InvalidArgumentException;
}
