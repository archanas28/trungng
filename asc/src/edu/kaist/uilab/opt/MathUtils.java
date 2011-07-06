package edu.kaist.uilab.opt;

/**
 * Utility class.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class MathUtils {

  /**
   * The &gamma; constant for computing the digamma function.
   * <p>
   * The value is defined as the negative of the digamma funtion evaluated at 1:
   * <blockquote>
   * 
   * <pre>
   * &gamma; = - &Psi;(1)
   */
  static double NEGATIVE_DIGAMMA_1 = 0.5772156649015328606065120900824024;

  private static final double DIGAMMA_COEFFS[] = {
      .30459198558715155634315638246624251,
      .72037977439182833573548891941219706,
      -.12454959243861367729528855995001087,
      .27769457331927827002810119567456810e-1,
      -.67762371439822456447373550186163070e-2,
      .17238755142247705209823876688592170e-2,
      -.44817699064252933515310345718960928e-3,
      .11793660000155572716272710617753373e-3,
      -.31253894280980134452125172274246963e-4,
      .83173997012173283398932708991137488e-5,
      -.22191427643780045431149221890172210e-5,
      .59302266729329346291029599913617915e-6,
      -.15863051191470655433559920279603632e-6,
      .42459203983193603241777510648681429e-7,
      -.11369129616951114238848106591780146e-7,
      .304502217295931698401459168423403510e-8,
      -.81568455080753152802915013641723686e-9,
      .21852324749975455125936715817306383e-9,
      -.58546491441689515680751900276454407e-10,
      .15686348450871204869813586459513648e-10,
      -.42029496273143231373796179302482033e-11,
      .11261435719264907097227520956710754e-11,
      -.30174353636860279765375177200637590e-12,
      .80850955256389526647406571868193768e-13,
      -.21663779809421233144009565199997351e-13,
      .58047634271339391495076374966835526e-14,
      -.15553767189204733561108869588173845e-14,
      .41676108598040807753707828039353330e-15,
      -.11167065064221317094734023242188463e-15 };

  /**
   * Computes digamma(x), where <code>digamma(x) := dlog gamma(x)/dx </code>
   * <p>
   * The function is approximated according to the J.M. Bernardo AS 103
   * algorithm. The returned value is accurate to 8 decimal places.
   * 
   * @param x
   * @return
   */
  public static double digamma8(double x) {
    double xsquare = x * x;
    return Math.log(x) - 1 / (2 * x) - 1 / (12 * xsquare) + 1
        / (120 * xsquare * xsquare) - 1 / (252 * xsquare * xsquare * xsquare);
  }

  /**
   * Returns the value of the digamma function for the specified value. The
   * returned values are accurate to at least 13 decimal places.
   * <p>
   * The digamma function is the derivative of the log of the gamma function.
   * <blockquote>
   * 
   * <pre>
   * &Psi;(z)
   * = <i>d</i> log &Gamma;(z) / <i>d</i>z
   * = &Gamma;'(z) / &Gamma;(z)
   * </pre>
   * 
   * </blockquote>
   * <p>
   * The numerical approximation is derived from:
   * <ul>
   * <li>Richard J. Mathar. 2005. <a
   * href="http://arxiv.org/abs/math/0403344">Chebyshev Series Expansion of
   * Inverse Polynomials</a>.
   * <li>
   * <li>Richard J. Mathar. 2005. <a
   * href="http://www.strw.leidenuniv.nl/~mathar/progs/digamma.c">digamma.c</a>.
   * (C Program implementing algorithm.)</li>
   * </ul>
   * <i>Implementation Note:</i> The recursive calls in the C implementation
   * have been transformed into loops and accumulators, and the recursion for
   * values greater than three replaced with a simpler reduction. The number of
   * loops required before the fixed length expansion is approximately integer
   * value of the absolute value of the input. Each loop requires a floating
   * point division, two additions and a local variable assignment. The fixed
   * portion of the algorithm is roughly 30 steps requiring four
   * multiplications, three additions, one static final array lookup, and four
   * assignments per loop iteration.
   * 
   * @param x
   *          Value at which to evaluate the digamma function.
   * @return The value of the digamma function at the specified value.
   */
  public static double digamma(double x) {
    if (x <= 0.0 && (x == (double) ((long) x)))
      return Double.NaN;

    double accum = 0.0;
    if (x < 0.0) {
      accum += java.lang.Math.PI
          / java.lang.Math.tan(java.lang.Math.PI * (1.0 - x));
      x = 1.0 - x;
    }

    if (x < 1.0) {
      while (x < 1.0)
        accum -= 1.0 / x++;
    }

    if (x == 1.0)
      return accum - NEGATIVE_DIGAMMA_1;

    if (x == 2.0)
      return accum + 1.0 - NEGATIVE_DIGAMMA_1;

    if (x == 3.0)
      return accum + 1.5 - NEGATIVE_DIGAMMA_1;

    // simpler recursion than Mahar to reduce recursion
    if (x > 3.0) {
      while (x > 3.0)
        accum += 1.0 / --x;
      return accum + digamma(x);
    }

    x -= 2.0;
    double tNMinus1 = 1.0;
    double tN = x;
    double digamma = DIGAMMA_COEFFS[0] + DIGAMMA_COEFFS[1] * tN;
    for (int n = 2; n < DIGAMMA_COEFFS.length; n++) {
      double tN1 = 2.0 * x * tN - tNMinus1;
      digamma += DIGAMMA_COEFFS[n] * tN1;
      tNMinus1 = tN;
      tN = tN1;
    }
    return accum + digamma;
  }

  /**
   * Returns the log (base 2) of the &Gamma; function. The &Gamma; function is
   * defined by: <blockquote>
   * 
   * <pre>
   * &Gamma;(z) = <big><big><big><big>&#8747;</big></big></big></big><sub><sub><sub><big>0</big></sub></sub></sub><sup><sup><sup><big>&#8734;</big></sup></sup></sup> t<sup>z-1</sup> * e<sup>-t</sup> <i>d</i>t
   * </pre>
   * 
   * </blockquote>
   * <p>
   * The &Gamma; function is the continuous generalization of the factorial
   * function, so that for real numbers <code>z &gt; 0</code>: <blockquote>
   * <code>&Gamma;(z+1) = z * &Gamma;(z)</code></blockquote> In particular,
   * integers <code>n &gt;= 0</code>, we have: <blockquote>
   * <code>&Gamma;(n+1) = n!</code></blockquote>
   * <p>
   * In general, &Gamma; satisfies: <blockquote>
   * 
   * <pre>
   * &Gamma;(z) = &pi; / (sin(&pi; * z) * &Gamma;(1-z))
   * </pre>
   * 
   * </blockquote>
   * <p>
   * This method uses the Lanczos approximation which is accurate nearly to the
   * full power of double-precision arithmetic. The Lanczos approximation is
   * used for inputs in the range <code>[0.5,1.5]</code>, converting numbers
   * less than 0.5 using the above formulas, and reducing arguments greater than
   * 1.5 using the factorial-like expansion above.
   * <p>
   * For more information on the &Gamma; function and its computation, see:
   * <ul>
   * <li>Weisstein, Eric W. <a
   * href="http://mathworld.wolfram.com/GammaFunction.html">Gamma Function</a>.
   * From MathWorld--A Wolfram Web Resource.</li>
   * <li>Weisstein, Eric W. <a
   * href="http://mathworld.wolfram.com/LanczosApproximation.html">Lanczos
   * Approximation</a>. From MathWorld--A Wolfram Web Resource.
   * <li>Wikipedia. <a href="http://en.wikipedia.org/wiki/Gamma_function">Gamma
   * Function</a>.</li>
   * <li>Wikipedia. <a
   * href="http://en.wikipedia.org/wiki/Lanczos_approximation">Lanczos
   * Approximation</a>.</li>
   * </ul>
   * 
   * @param z
   *          The argument to the gamma function.
   * @return The value of <code>&Gamma;(z)</code>.
   */
  public static double log2Gamma(double z) {
    if (z < 0.5) {
      return log2(java.lang.Math.PI) - log2(Math.sin(Math.PI * z))
          - log2Gamma(1.0 - z);
    }
    double result = 0.0;
    while (z > 1.5) {
      result += log2(z - 1);
      z -= 1.0;
    }
    return result + log2(lanczosGamma(z));
  }

  /**
   * Returns the log (base <code>e</code>) of the &Gamma; function. The &Gamma; function is
   * defined by: <blockquote>
   * 
   * <pre>
   * &Gamma;(z) = <big><big><big><big>&#8747;</big></big></big></big><sub><sub><sub><big>0</big></sub></sub></sub><sup><sup><sup><big>&#8734;</big></sup></sup></sup> t<sup>z-1</sup> * e<sup>-t</sup> <i>d</i>t
   * </pre>
   * 
   * </blockquote>
   */
  public static double logGamma(double z) {
    return Math.log(2) * log2Gamma(z);
  }

  /**
   * Returns the log (base 2) of <code>x</code>
   * 
   * @param x
   * @return
   */
  private static double log2(double x) {
    return Math.log(x) / Math.log(2);
  }

  static double[] LANCZOS_COEFFS = new double[] { 0.99999999999980993,
      676.5203681218851, -1259.1392167224028, 771.32342877765313,
      -176.61502916214059, 12.507343278686905, -0.13857109526572012,
      9.9843695780195716e-6, 1.5056327351493116e-7 };

  static double SQRT_2_PI = Math.sqrt(2.0 * Math.PI);

  // assumes input in [0.5,1.5] inclusive
  static double lanczosGamma(double z) {
    double zMinus1 = z - 1;
    double x = LANCZOS_COEFFS[0];
    for (int i = 1; i < LANCZOS_COEFFS.length - 2; ++i)
      x += LANCZOS_COEFFS[i] / (zMinus1 + i);
    double t = zMinus1 + (LANCZOS_COEFFS.length - 2) + 0.5;
    return SQRT_2_PI * java.lang.Math.pow(t, zMinus1 + 0.5)
        * java.lang.Math.exp(-t) * x;
  }

  public static void main(String args[]) {
    System.out.println(digamma8(1));
    System.out.println(digamma(1));
  }
}
