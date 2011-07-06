package edu.kaist.uilab.asc.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * A double matrix with some utilities for handling its elements.
 */
public class DoubleMatrix implements Serializable {
  private static final long serialVersionUID = 7206976292687405488L;
  private int numOfRow;
  private int numOfColumn;
  private double[][] row;

  /**
   * Constructs a new double matrix with all of its elements initialized to 0.0
   * 
   * @param numOfRow
   * @param numOfColumn
   */
  public DoubleMatrix(int numOfRow, int numOfColumn) {
    this.numOfRow = numOfRow;
    this.numOfColumn = numOfColumn;
    row = new double[numOfRow][numOfColumn];
    for (int i = 0; i < numOfRow; i++)
      for (int j = 0; j < numOfColumn; j++)
        row[i][j] = 0;
  }

  public int getNumOfRow() {
    return numOfRow;
  }

  public int getNumOfColumn() {
    return numOfColumn;
  }

  /**
   * Sets value of an element.
   * 
   * @param rowIdx
   * @param colIdx
   * @param value
   */
  public void setValue(int rowIdx, int colIdx, double value) {
    row[rowIdx][colIdx] = value;
  }

  /**
   * Gets the value of an element.
   * 
   * @param rowIdx
   * @param colIdx
   * @return
   */
  public double getValue(int rowIdx, int colIdx) {
    return row[rowIdx][colIdx];
  }

  /**
   * Increases the value of an element by one.
   * 
   * @param rowIdx
   * @param colIdx
   */
  public void incValue(int rowIdx, int colIdx) {
    row[rowIdx][colIdx]++;
  }

  /**
   * Increases the value of an element by some amount {@code value}.
   * 
   * @param rowIdx
   * @param colIdx
   * @param value
   */
  public void incValue(int rowIdx, int colIdx, double value) {
    row[rowIdx][colIdx] += value;
  }

  /**
   * Decreases the value of an element by one.
   * 
   * @param rowIdx
   * @param colIdx
   */
  public void decValue(int rowIdx, int colIdx) {
    row[rowIdx][colIdx]--;
  }

  /**
   * Gets a row of this matrix.
   * 
   * @param rowIdx
   * @return
   */
  public double[] getRow(int rowIdx) {
    return row[rowIdx];
  }

  /**
   * Gets the sum of elements of a row.
   * 
   * @param rowIdx
   * @return
   */
  public double getRowSum(int rowIdx) {
    double sum = 0;
    for (int i = 0; i < numOfColumn; i++)
      sum += row[rowIdx][i];
    return sum;
  }

  /**
   * Gets a column of this matrix.
   * 
   * @param colIdx
   * @return
   */
  public double[] getColumn(int colIdx) {
    if (colIdx > numOfColumn) {
      return null;
    }
    double[] col = new double[numOfRow];
    for (int i = 0; i < numOfRow; i++) {
      col[i] = row[i][colIdx];
    }
    return col;
  }

  /**
   * Gets the sum of elements of a column.
   * 
   * @param colIdx
   * @return
   */
  public double getColSum(int colIdx) {
    if (colIdx > numOfColumn) {
      return 0;
    }
    double sum = 0;
    for (int i = 0; i < numOfRow; i++) {
      sum += row[i][colIdx];
    }
    return sum;
  }

  /**
   * Gets this matrix.
   * 
   * @return
   */
  public double[][] getMatrix() {
    return row;
  }

  /**
   * Returns the indices of top <code>n</code> columns in the specified row
   * <code>row</code>.
   * 
   * @param row
   * @param n
   * @return
   */
  public Vector<Integer> getSortedRowIndex(int row, int n) {
    Vector<Integer> sortedList = new Vector<Integer>();

    for (int i = 0; i < n; i++) {
      double maxValue = Integer.MIN_VALUE;
      int maxIndex = -1;
      for (int col = 0; col < numOfColumn; col++) {
        if (getValue(row, col) > maxValue) {
          boolean exist = false;
          for (int j = 0; j < sortedList.size(); j++) {
            if (sortedList.get(j) == col) {
              exist = true;
              break;
            }
          }
          if (!exist) {
            maxValue = getValue(row, col);
            maxIndex = col;
          }
        }
      }
      sortedList.add(maxIndex);
    }

    return sortedList;
  }

  /**
   * Returns the indices of top <code>n</code> rows in the specified column
   * <code>col</code>.
   * 
   * @param col
   * @param n
   * @return
   */
  public Vector<Integer> getSortedColIndex(int col, int n) {
    Vector<Integer> sortedList = new Vector<Integer>();
    for (int i = 0; i < n; i++) {
      double maxValue = Integer.MIN_VALUE;
      int maxIndex = -1;
      for (int row = 0; row < numOfRow; row++) {
        if (getValue(row, col) > maxValue) {
          boolean exist = false;
          for (int j = 0; j < sortedList.size(); j++) {
            if (sortedList.get(j) == row) {
              exist = true;
              break;
            }
          }
          if (!exist) {
            maxValue = getValue(row, col);
            maxIndex = row;
          }
        }
      }
      sortedList.add(maxIndex);
    }

    return sortedList;
  }

  public void writeMatrixToCSVFile(String outputFilePath) throws IOException {
    PrintWriter out = new PrintWriter(new FileWriter(new File(outputFilePath)));

    for (int row = 0; row < numOfRow; row++) {
      for (int col = 0; col < numOfColumn; col++) {
        if (col == 0)
          out.print(getValue(row, col));
        else
          out.print("," + getValue(row, col));
      }
      out.println();
    }

    out.close();
  }

  public DoubleMatrix copy() {
    DoubleMatrix temp = new DoubleMatrix(this.numOfRow, this.numOfColumn);

    for (int row = 0; row < numOfRow; row++) {
      for (int col = 0; col < numOfColumn; col++) {
        temp.setValue(row, col, this.getValue(row, col));
      }
    }

    return temp;
  }

  public List<Double> getRowList(int row) {
    List<Double> list = new ArrayList<Double>();

    for (int i = 0; i < numOfColumn; i++) {
      list.add(getValue(row, i));
    }
    return list;
  }

  public List<Double> getColList(int col) {
    List<Double> list = new ArrayList<Double>();

    for (int i = 0; i < numOfRow; i++) {
      list.add(getValue(i, col));
    }
    return list;
  }

}
