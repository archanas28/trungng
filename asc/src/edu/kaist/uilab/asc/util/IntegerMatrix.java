package edu.kaist.uilab.asc.util;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;

public class IntegerMatrix implements Serializable {
  private static final long serialVersionUID = 5274639281037182004L;
  
  private int numOfRow;
  private int numOfColumn;
  private int[][] row;

  public IntegerMatrix(int numOfRow, int numOfColumn) {
    this.numOfRow = numOfRow;
    this.numOfColumn = numOfColumn;
    row = new int[numOfRow][numOfColumn];
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

  // set value of matrix(rowIdx, colIdx) to value
  public void setValue(int rowIdx, int colIdx, int value) {
    row[rowIdx][colIdx] = value;
  }

  // get value of matrix(rowIdx, colIdx)
  public int getValue(int rowIdx, int colIdx) {
    return row[rowIdx][colIdx];
  }

  public synchronized void incValue(int rowIdx, int colIdx) {
    row[rowIdx][colIdx]++;
  }

  public synchronized void incValue(int rowIdx, int colIdx, int value) {
    row[rowIdx][colIdx] += value;
  }

  public synchronized void decValue(int rowIdx, int colIdx) {
    row[rowIdx][colIdx]--;
  }

  public int[] getRow(int rowIdx) {
    return row[rowIdx];
  }

  // get sum of row elements
  public int getRowSum(int rowIdx) {
    int sum = 0;
    for (int i = 0; i < numOfColumn; i++)
      sum += row[rowIdx][i];
    return sum;
  }

  public int[] getColumn(int colIdx) {
    if (colIdx > numOfColumn) {
      return null;
    }
    int[] col = new int[numOfRow];
    for (int i = 0; i < numOfRow; i++) {
      col[i] = row[i][colIdx];
    }
    return col;
  }

  // get sum of column elements
  public int getColSum(int colIdx) {
    if (colIdx > numOfColumn) {
      return 0;
    }
    int sum = 0;
    for (int i = 0; i < numOfRow; i++) {
      sum += row[i][colIdx];
    }
    return sum;
  }

  public Vector<Integer> getSortedRowIndex(int row, int n) {
    Vector<Integer> sortedList = new Vector<Integer>();

    for (int i = 0; i < n; i++) {
      int maxValue = Integer.MIN_VALUE;
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

  public Vector<Integer> getSortedColIndex(int col, int n) {
    Vector<Integer> sortedList = new Vector<Integer>();

    for (int i = 0; i < n; i++) {
      int maxValue = Integer.MIN_VALUE;
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

  public void writeMatrixToCSVFile(String outputFilePath) throws Exception {
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

  public IntegerMatrix copy() {
    IntegerMatrix temp = new IntegerMatrix(this.numOfRow, this.numOfColumn);

    for (int row = 0; row < numOfRow; row++) {
      for (int col = 0; col < numOfColumn; col++) {
        temp.setValue(row, col, this.getValue(row, col));
      }
    }

    return temp;
  }

}
