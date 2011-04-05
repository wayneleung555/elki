package de.lmu.ifi.dbs.elki.index.preprocessed.preference;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.HiSC;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Preprocessor for HiSC preference vector assignment to objects of a certain
 * database.
 * 
 * @author Elke Achtert
 * 
 * @see HiSC
 */
@Title("HiSC Preprocessor")
@Description("Computes the preference vector of objects of a certain database according to the HiSC algorithm.")
public class HiSCPreferenceVectorIndex<V extends NumberVector<?, ?>> extends AbstractPreferenceVectorIndex<V> implements PreferenceVectorIndex<V> {
  /**
   * Logger to use
   */
  protected static final Logging logger = Logging.getLogger(HiSCPreferenceVectorIndex.class);

  /**
   * Holds the value of parameter alpha.
   */
  protected double alpha;

  /**
   * Holds the value of parameter k.
   */
  protected int k;

  /**
   * Constructor.
   * 
   * @param database
   * @param alpha
   * @param k
   */
  public HiSCPreferenceVectorIndex(Database<V> database, double alpha, int k) {
    super(database);
    this.alpha = alpha;
    this.k = k;
  }

  @Override
  protected void preprocess() {
    if(database == null || database.size() <= 0) {
      throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
    }

    storage = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, BitSet.class);

    StringBuffer msg = new StringBuffer();

    long start = System.currentTimeMillis();
    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Preprocessing preference vector", database.size(), logger) : null;

    KNNQuery<V, DoubleDistance> knnQuery = database.getKNNQuery(EuclideanDistanceFunction.STATIC, k);

    Iterator<DBID> it = database.iterator();
    while(it.hasNext()) {
      DBID id = it.next();

      if(logger.isDebugging()) {
        msg.append("\n\nid = ").append(id);
        msg.append(" ").append(database.getObjectLabelQuery().get(id));
        msg.append("\n knns: ");
      }

      List<DistanceResultPair<DoubleDistance>> knns = knnQuery.getKNNForDBID(id, k);
      ModifiableDBIDs knnIDs = DBIDUtil.newArray(knns.size());
      for(DistanceResultPair<DoubleDistance> knn : knns) {
        knnIDs.add(knn.getID());
        if(logger.isDebugging()) {
          msg.append(database.getObjectLabelQuery().get(knn.getID())).append(" ");
        }
      }

      BitSet preferenceVector = determinePreferenceVector(database, id, knnIDs, msg);
      storage.put(id, preferenceVector);

      if(progress != null) {
        progress.incrementProcessed(logger);
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }

    if(logger.isDebugging()) {
      logger.debugFine(msg.toString());
    }

    long end = System.currentTimeMillis();
    // TODO: re-add timing code!
    if(logger.isVerbose()) {
      long elapsedTime = end - start;
      logger.verbose(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
    }
  }

  /**
   * Determines the preference vector according to the specified neighbor ids.
   * 
   * @param database the database storing the objects
   * @param id the id of the object for which the preference vector should be
   *        determined
   * @param neighborIDs the ids of the neighbors
   * @param msg a string buffer for debug messages
   * @return the preference vector
   */
  private BitSet determinePreferenceVector(Database<V> database, DBID id, DBIDs neighborIDs, StringBuffer msg) {
    // variances
    double[] variances = DatabaseUtil.variances(database, database.get(id), neighborIDs);

    // preference vector
    BitSet preferenceVector = new BitSet(variances.length);
    for(int d = 0; d < variances.length; d++) {
      if(variances[d] < alpha) {
        preferenceVector.set(d);
      }
    }

    if(msg != null && logger.isDebugging()) {
      msg.append("\nalpha " + alpha);
      msg.append("\nvariances ");
      msg.append(FormatUtil.format(variances, ", ", 4));
      msg.append("\npreference ");
      msg.append(FormatUtil.format(variances.length, preferenceVector));
    }

    return preferenceVector;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public String getLongName() {
    return "HiSC Preference Vectors";
  }

  @Override
  public String getShortName() {
    return "hisc-pref";
  }

  /**
   * Factory class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses HiSCPreferenceVectorIndex oneway - - «create»
   * 
   * @param <V> Vector type
   */
  public static class Factory<V extends NumberVector<?, ?>> extends AbstractPreferenceVectorIndex.Factory<V, HiSCPreferenceVectorIndex<V>> {
    /**
     * The default value for alpha.
     */
    public static final double DEFAULT_ALPHA = 0.01;

    /**
     * OptionID for {@link #ALPHA_PARAM}
     */
    public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("hisc.alpha", "The maximum absolute variance along a coordinate axis.");

    /**
     * The maximum absolute variance along a coordinate axis. Must be in the
     * range of [0.0, 1.0).
     * <p>
     * Default value: {@link #DEFAULT_ALPHA}
     * </p>
     * <p>
     * Key: {@code -hisc.alpha}
     * </p>
     */
    private final DoubleParameter ALPHA_PARAM = new DoubleParameter(ALPHA_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN, 1.0, IntervalConstraint.IntervalBoundary.OPEN), DEFAULT_ALPHA);

    /**
     * Holds the value of parameter {@link #ALPHA_PARAM}.
     */
    protected double alpha;

    /**
     * OptionID for {@link #K_PARAM}.
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID("hisc.k", "The number of nearest neighbors considered to determine the preference vector. If this value is not defined, k ist set to three times of the dimensionality of the database objects.");

    /**
     * The number of nearest neighbors considered to determine the preference
     * vector. If this value is not defined, k is set to three times of the
     * dimensionality of the database objects.
     * <p>
     * Key: {@code -hisc.k}
     * </p>
     * <p>
     * Default value: three times of the dimensionality of the database objects
     * </p>
     */
    private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0), true);

    /**
     * Holds the value of parameter {@link #K_PARAM}.
     */
    protected Integer k;

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     * 
     * @param config Parameterization
     */
    public Factory(Parameterization config) {
      super(config);
      config = config.descend(this);

      // parameter alpha
      if(config.grab(ALPHA_PARAM)) {
        alpha = ALPHA_PARAM.getValue();
      }

      // parameter k
      if(config.grab(K_PARAM)) {
        k = K_PARAM.getValue();
      }
    }

    @Override
    public HiSCPreferenceVectorIndex<V> instantiate(Database<V> database) {
      final int usek;
      if(k == null) {
        usek = 3 * DatabaseUtil.dimensionality(database);
      }
      else {
        usek = k;
      }
      return new HiSCPreferenceVectorIndex<V>(database, alpha, usek);
    }
  }
}