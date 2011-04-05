package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.AbstractDatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Instance for a particular database, invoking the preprocessor.
 * 
 * @author Erich Schubert
 */
public class PreprocessorKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractDatabaseQuery<O> implements KNNQuery<O, D> {
  /**
   * The last preprocessor result
   */
  final private MaterializeKNNPreprocessor<O, D> preprocessor;
  
  /**
   * Warn only once.
   */
  private boolean warned = false;

  /**
   * Constructor.
   * 
   * @param database Database to query
   * @param preprocessor Preprocessor instance to use
   */
  public PreprocessorKNNQuery(Database<O> database, MaterializeKNNPreprocessor<O, D> preprocessor) {
    super(database);
    this.preprocessor = preprocessor;
  }

  /**
   * Constructor.
   * 
   * @param database Database to query
   * @param preprocessor Preprocessor to use
   */
  public PreprocessorKNNQuery(Database<O> database, MaterializeKNNPreprocessor.Factory<O, D> preprocessor) {
    this(database, preprocessor.instantiate(database));
  }

  @Override
  public List<DistanceResultPair<D>> getKNNForDBID(DBID id, int k) {
    if (!warned && k > preprocessor.getK()) {
      LoggingUtil.warning("Requested more neighbors than preprocessed!");
    }
    if (!warned && k < preprocessor.getK()) {
      LoggingUtil.warning("FIXME: we're returning too many neighbors!");
    }
    return preprocessor.get(id);
  }

  @Override
  public List<List<DistanceResultPair<D>>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    if (!warned && k > preprocessor.getK()) {
      LoggingUtil.warning("Requested more neighbors than preprocessed!");
    }
    if (!warned && k < preprocessor.getK()) {
      LoggingUtil.warning("FIXME: we're returning too many neighbors!");
    }
    List<List<DistanceResultPair<D>>> result = new ArrayList<List<DistanceResultPair<D>>>(ids.size());
    for(DBID id : ids) {
      result.add(preprocessor.get(id));
    }
    return result;
  }

  @Override
  public List<DistanceResultPair<D>> getKNNForObject(O obj, int k) {
    DBID id = obj.getID();
    if(id != null) {
      return getKNNForDBID(id, k);
    }
    throw new AbortException("Preprocessor KNN query used with previously unseen objects.");
  }

  /**
   * Get the preprocessor instance.
   * 
   * @return preprocessor instance
   */
  public MaterializeKNNPreprocessor<O, D> getPreprocessor() {
    return preprocessor;
  }

  @Override
  public D getDistanceFactory() {
    return preprocessor.getDistanceFactory();
  }

  @Override
  public DistanceQuery<O, D> getDistanceQuery() {
    // TODO: remove? throw an exception?
    return preprocessor.getDistanceQuery();
  }
}