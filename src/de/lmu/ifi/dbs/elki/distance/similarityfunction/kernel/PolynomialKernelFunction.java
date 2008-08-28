package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Provides a polynomial Kernel function that computes
 * a similarity between the two feature vectors V1 and V2 definded by (V1^T*V2)^degree.
 *
 * @author Simon Paradies
 */
public class PolynomialKernelFunction<O extends FeatureVector<O, ? >> extends AbstractDoubleKernelFunction<O> {
  /**
   * The default degree.
   */
  public static final double DEFAULT_DEGREE = 2.0;

  /**
   * Description for parameter degree.
   */
  public static final String DEGREE_D = "The degree of the polynomial kernel function. Default: "
                                        + DEFAULT_DEGREE;
  /**
   * Parameter for degree.
   */
  public static final String DEGREE_P = "degree";

  /**
   * Degree of the polynomial kernel function
   */
  private double degree = 0.0;

  /**
   * Provides a polynomial Kernel function that computes
   * a similarity between the two feature vectors V1 and V2 definded by (V1^T*V2)^degree.
   */
  public PolynomialKernelFunction() {
    super();
    //parameter degree
    DoubleParameter deg = new DoubleParameter(DEGREE_P, DEGREE_D);
    deg.setDefaultValue(DEFAULT_DEGREE);
    addOption(deg);
  }

  @Override
  public String parameterDescription() {
    StringBuffer description = new StringBuffer();
    description.append(super.parameterDescription());
    description.append("Polynomial Kernel for feature vectors. Default degree is " + DEFAULT_DEGREE + ".");
    return description.toString();
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // degree
    degree = (Double)optionHandler.getOptionValue(DEGREE_P);

    return remainingParameters;
  }

  /**
   * Provides the linear kernel similarity between the given two vectors.
   *
   * @param o1 first vector
   * @param o2 second vector
   * @return the linear kernel similarity between the given two vectors as an
   *         instance of {@link DoubleDistance DoubleDistance}.
   * @see DistanceFunction#distance(de.lmu.ifi.dbs.elki.data.DatabaseObject, de.lmu.ifi.dbs.elki.data.DatabaseObject)
   */
  public DoubleDistance similarity(O o1, O o2) {
    if (o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of Feature-Vectors" +
                                         "\n  first argument: " + o1.toString() +
                                         "\n  second argument: " + o2.toString());
    }

    double sim = 0;
    for (int i = 1; i <= o1.getDimensionality(); i++) {
      sim += o1.getValue(i).doubleValue() * o2.getValue(i).doubleValue();
    }
    return new DoubleDistance(Math.pow(sim, degree));
  }
}
