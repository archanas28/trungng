package com.rainmoon.nextnumber;

import java.util.Random;

/**
 * Sequence generator.
 * 
 * TODO: add more games
 * <br/> Select one not belong to a set: sum equals 15, ends with 6, symmetric
 * <br/> Construct seq f[n] = f[n-1] +(-,*,/).
 *
 * @author trung
 */
public class SequencesGenerator {
  public static final int SEQ_LENGTH = 10;
  public static final int MAX_LEVEL = 20;

  // naturals, odds, evens, endings (pos, neg, decreasing, increasing)
  static final int[][] levels1to5 = levels1to5();
  static final int[][] levels6to10 = levels6to10();
  static final int[][] levels11to15 = levels11to15();
  static final int[][] levels16to20 = levels16to20();
  private static final SequencesGenerator instance = new SequencesGenerator();

  private SequencesGenerator() {
  }

  /**
   * Gets an instance.
   * 
   * @return
   */
  public static SequencesGenerator getInstance() {
    return instance;
  }

  /**
   * Gets a sequence for the given level.
   * 
   * @param level
   * @return
   */
  public int[] getGame(int level) {
    if (level > MAX_LEVEL)
      return null;

    Random random = new Random();
    int[] sequence;
    if (level <= 5) {
      sequence = levels1to5[random.nextInt(levels1to5.length)];
    } else if (level <= 10) {
      sequence = levels6to10[random.nextInt(levels6to10.length)];
    } else if (level <= 15) {
      sequence = levels11to15[random.nextInt(levels11to15.length)];
    } else {
      sequence = levels16to20[random.nextInt(levels16to20.length)];
    }
    int step = getStep(random, sequence.length, SEQ_LENGTH);

    int startIdx = 0;
    if (sequence.length > step * SEQ_LENGTH) {
      startIdx = random.nextInt(sequence.length - step * SEQ_LENGTH);
    }
    int[] res = new int[SEQ_LENGTH];
    for (int i = 0; i < SEQ_LENGTH; i++) {
      res[i] = sequence[startIdx + step * i];
    }
    return res;
  }

  int[] naturals(int num) {
    int sign = sign(num);
    num = Math.abs(num);
    int[] res = new int[num];
    for (int i = 1; i <= num; i++) {
      res[i - 1] = sign * i;
    }
    return res;
  }

  int[] odds(int num) {
    int sign = sign(num);
    num = Math.abs(num);
    int[] res = new int[num];
    int val = 1;
    for (int i = 1; i <= num; i++) {
      res[i - 1] = sign * val;
      val = val + 2;
    }
    return res;
  }

  int[] evens(int num) {
    int sign = sign(num);
    num = Math.abs(num);
    int[] res = new int[num];
    int val = 2;
    for (int i = 1; i <= num; i++) {
      res[i - 1] = sign * val;
      val = val + 2;
    }
    return res;
  }

  int[] squares(int num) {
    int sign = sign(num);
    num = Math.abs(num);
    int[] res = new int[num];
    for (int i = 1; i <= num; i++) {
      res[i - 1] = sign * i * i;
    }
    return res;
  }

  int[] mults(int base, int num) {
    int sign = sign(num);
    num = Math.abs(num);
    int[] res = new int[num];
    for (int i = 1; i <= num; i++) {
      res[i - 1] = sign * i * base;
    }
    return res;
  }

  int[] powers(int base, int num) {
    int sign = sign(num);
    num = Math.abs(num);
    int[] res = new int[num];
    res[0] = sign * 1; // base^0
    for (int i = 1; i < num; i++) {
      res[i] = res[i - 1] * base;
    }
    return res;
  }

  int[] endings(int ending, int num) {
    int sign = sign(num);
    num = Math.abs(num);
    int[] res = new int[num];
    res[0] = sign * ending;
    for (int i = 1; i < num; i++) {
      res[i] = sign * (ending + 10 * i);
    }
    return res;
  }

  int[] primes(int num) {
    int sign = sign(num);
    num = Math.abs(num);
    int[] res = new int[num];
    int cnt = 0;
    int val = 2;
    while (cnt < num) {
      if (isPrime(val)) {
        res[cnt] = sign * val;
        cnt = cnt + 1;
      }
      val = val + 1;
    }
    return res;
  }

  /*
   * fn = k*f(n-1) + f(n-2)
   */
  int[] fibonacci(int f0, int k, int num) {
    int sign = sign(num);
    num = Math.abs(num);
    int[] seq = new int[num];
    seq[0] = f0;
    seq[1] = f0 + 1;
    for (int i = 2; i < num; i++) {
      seq[i] = sign * (seq[i - 1] + k * seq[i - 2]);
    }
    return seq;
  }

  boolean isPrime(int x) {
    double max = Math.sqrt(x);
    for (int factor = 2; factor <= max; factor++) {
      if (x % factor == 0) {
        return false;
      }
    }
    return true;
  }

  int[] reverse(int[] x) {
    int[] reverse = new int[x.length];
    for (int i = x.length - 1; i >= 0; i--) {
      reverse[x.length - i - 1] = x[i];
    }
    return reverse;
  }

  int sign(int num) {
    if (num < 0) {
      return -1;
    }
    return 1;
  }

  // get valid step
  int getStep(Random random, int total, int seqLength) {
    return 1 + Math.min(random.nextInt(total / seqLength), 1);
  }

  public static int[][] levels1to5() {
    SequencesGenerator g = new SequencesGenerator();
    // level 1 - 5
    int[][] seqs = new int[10 * 3 * 4 + 10 * 4 + 8 * 4][];
    int cnt = 0;
    for (int i = 0; i < 10; i++) {
      seqs[cnt++] = g.reverse(g.naturals(100));
      seqs[cnt++] = g.reverse(g.odds(100));
      seqs[cnt++] = g.reverse(g.evens(100));
      seqs[cnt++] = g.naturals(100);
      seqs[cnt++] = g.odds(100);
      seqs[cnt++] = g.evens(100);

      seqs[cnt++] = g.reverse(g.naturals(-100));
      seqs[cnt++] = g.reverse(g.odds(-100));
      seqs[cnt++] = g.reverse(g.evens(-100));
      seqs[cnt++] = g.naturals(-100);
      seqs[cnt++] = g.odds(-100);
      seqs[cnt++] = g.evens(-100);
    }

    for (int ending = 0; ending <= 9; ending++) {
      seqs[cnt++] = g.endings(ending, 100);
      seqs[cnt++] = g.reverse(g.endings(ending, 100));
      seqs[cnt++] = g.endings(ending, -100);
      seqs[cnt++] = g.reverse(g.endings(ending, -100));
    }

    for (int base = 3; base <= 10; base++) {
      seqs[cnt++] = g.mults(base, 100);
      seqs[cnt++] = g.reverse(g.mults(base, 100));
      seqs[cnt++] = g.mults(base, -100);
      seqs[cnt++] = g.reverse(g.mults(base, -100));
    }

    return seqs;
  }

  // square, power, mult, primes
  public static int[][] levels6to10() {
    SequencesGenerator g = new SequencesGenerator();
    int[][] seqs = new int[(2 * 5 + 5 + 2 * 2) * 4][];
    int cnt = 0;
    for (int i = 0; i < 5; i++) {
      seqs[cnt++] = g.squares(21);
      seqs[cnt++] = g.reverse(g.squares(21));
      seqs[cnt++] = g.squares(-21);
      seqs[cnt++] = g.reverse(g.squares(-21));

      seqs[cnt++] = g.primes(100);
      seqs[cnt++] = g.reverse(g.primes(100));
      seqs[cnt++] = g.primes(-100);
      seqs[cnt++] = g.reverse(g.primes(-100));
    }

    int[] bases = { 13, 17, 19, 21, 23 };
    for (int idx = 0; idx < bases.length; idx++) {
      seqs[cnt++] = g.mults(bases[idx], 50);
      seqs[cnt++] = g.reverse(g.mults(bases[idx], 50));
      seqs[cnt++] = g.mults(bases[idx], -50);
      seqs[cnt++] = g.reverse(g.mults(bases[idx], -50));
    }

    for (int i = 0; i < 2; i++) {
      for (int base = 2; base <= 3; base++) {
        seqs[cnt++] = g.powers(base, 10);
        seqs[cnt++] = g.reverse(g.powers(base, 10));
        seqs[cnt++] = g.powers(base, -10);
        seqs[cnt++] = g.reverse(g.powers(base, -10));
      }
    }

    return seqs;
  }

  // bigger power, mults, fibonacci
  public static int[][] levels11to15() {
    SequencesGenerator g = new SequencesGenerator();
    int[][] seqs = new int[8 + 8 + 16 + 24][];
    int cnt = 0;

    seqs[cnt++] = g.squares(21);
    seqs[cnt++] = g.reverse(g.squares(21));
    seqs[cnt++] = g.squares(-21);
    seqs[cnt++] = g.reverse(g.squares(-21));

    seqs[cnt++] = g.primes(100);
    seqs[cnt++] = g.reverse(g.primes(100));
    seqs[cnt++] = g.primes(-100);
    seqs[cnt++] = g.reverse(g.primes(-100));

    for (int base = 2; base <= 3; base++) {
      seqs[cnt++] = g.powers(base, 10);
      seqs[cnt++] = g.reverse(g.powers(base, 10));
      seqs[cnt++] = g.powers(base, -10);
      seqs[cnt++] = g.reverse(g.powers(base, -10));
    }

    int[] bases = { 23, 51, 47, 79 };
    for (int idx = 0; idx < bases.length; idx++) {
      seqs[cnt++] = g.mults(bases[idx], 50);
      seqs[cnt++] = g.reverse(g.mults(bases[idx], 50));
      seqs[cnt++] = g.mults(bases[idx], -50);
      seqs[cnt++] = g.reverse(g.mults(bases[idx], -50));
    }

    // fibonacci
    for (int f0 = 0; f0 <= 2; f0++) {
      for (int k = 1; k <= 2; k = k + 1) {
        seqs[cnt++] = g.fibonacci(f0, k, 15);
        seqs[cnt++] = g.reverse(g.fibonacci(f0, k, 15));
        seqs[cnt++] = g.fibonacci(f0, k, -15);
        seqs[cnt++] = g.reverse(g.fibonacci(f0, k, -15));
      }
    }

    return seqs;
  }

  public static int[][] levels16to20() {
    SequencesGenerator g = new SequencesGenerator();
    int[][] levels = new int[16 + 16][];
    int cnt = 0;
    // fibonacci
    for (int f0 = 1; f0 <= 2; f0++) {
      for (int k = 1; k <= 2; k = k + 1) {
        levels[cnt++] = g.fibonacci(f0, k, 15);
        levels[cnt++] = g.reverse(g.fibonacci(f0, k, 15));
        levels[cnt++] = g.fibonacci(f0, k, -15);
        levels[cnt++] = g.reverse(g.fibonacci(f0, k, -15));
      }
    }

    // primes
    for (int i = 0; i < 4; i++) {
      levels[cnt++] = g.primes(50);
      levels[cnt++] = g.reverse(g.primes(50));
      levels[cnt++] = g.primes(-50);
      levels[cnt++] = g.reverse(g.primes(-50));
    }

    int[][] seqs = new int[levels.length * 2][];
    cnt = 0;
    for (int x = 0; x < 2; x++) {
      for (int level = 0; level < levels.length; level++) {
        seqs[cnt] = new int[levels[level].length];
        for (int n = 0; n < levels[level].length; n++) {
          seqs[cnt][n] = levels[level][n] * (x + 2);
        }
        cnt = cnt + 1;
      }
    }
    return seqs;
  }
}
