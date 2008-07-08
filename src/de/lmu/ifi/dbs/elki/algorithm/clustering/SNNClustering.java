package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.IntegerDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * <p>Shared nearest neighbor clustering.</p>
 * <p/>
 * <p>This class implements the algorithm proposed in
 * L. Ert&ouml;z, M. Steinbach, V. Kumar: Finding Clusters of Different Sizes, Shapes, and Densities in Noisy, High Dimensional Data. In: Proc. of SIAM Data Mining (SDM), 2003.</p>
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject the algorithm is applied on
 * @param <D> the type of Distance used for the preprocessing of the shared nearest neighbors neighborhood lists
 */
public class SNNClustering<O extends DatabaseObject, D extends Distance<D>> extends AbstractAlgorithm<O> implements Clustering<O> {

    /**
     * OptionID for {@link #EPSILON_PARAM}
     */
    public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID(
        "snn.epsilon",
        "The minimum SNN density."
    );

    /**
     * OptionID for {@link #MINPTS_PARAM}
     */
    public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID(
        "snn.minpts",
        "Threshold for minimum number of points in " +
            "the epsilon-SNN-neighborhood of a point."
    );

    /**
     * Parameter to specify the minimum SNN density,
     * must be an integer greater than 0.
     * <p>Key: {@code -snn.epsilon} </p>
     */
    private final IntParameter EPSILON_PARAM = new IntParameter(
        EPSILON_ID,
        new GreaterConstraint(0));

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-SNN-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -snn.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(
        MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Holds the Epsilon value.
     */
    private IntegerDistance epsilon;

    /**
     * Holds the minimum points value.
     */
    private int minpts;

    /**
     * Holds a list of clusters found.
     */
    protected List<List<Integer>> resultList;

    /**
     * Provides the result of the algorithm.
     */
    protected ClustersPlusNoise<O> result;

    /**
     * Holds a set of noise.
     */
    protected Set<Integer> noise;

    /**
     * Holds a set of processed ids.
     */
    protected Set<Integer> processedIDs;

    /**
     * The similarity function for the shared nearest neighbor similarity.
     */
    private SharedNearestNeighborSimilarityFunction<O, D> similarityFunction = new SharedNearestNeighborSimilarityFunction<O, D>();

    /**
     * Sets epsilon and minimum points to the optionhandler additionally to the
     * parameters provided by super-classes.
     */
    public SNNClustering() {
        super();
        addOption(EPSILON_PARAM);
        addOption(MINPTS_PARAM);
    }

    /**
     * Performs the SNN clustering algorithm on the given database.
     *
     * @see de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.elki.database.Database)
     */
    @Override
    protected void runInTime(Database<O> database) {
        Progress progress = new Progress("Clustering", database.size());
        resultList = new ArrayList<List<Integer>>();
        noise = new HashSet<Integer>();
        processedIDs = new HashSet<Integer>(database.size());
        similarityFunction.setDatabase(database, isVerbose(), isTime());
        if (isVerbose()) {
            verbose("Clustering:");
        }
        if (database.size() >= minpts) {
            for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
                Integer id = iter.next();
                if (!processedIDs.contains(id)) {
                    expandCluster(database, id, progress);
                    if (processedIDs.size() == database.size() && noise.size() == 0) {
                        break;
                    }
                }
                if (isVerbose()) {
                    progress.setProcessed(processedIDs.size());
                    progress(progress, resultList.size());
                }
            }
        }
        else {
            for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
                Integer id = iter.next();
                noise.add(id);
                if (isVerbose()) {
                    progress.setProcessed(noise.size());
                    progress(progress, resultList.size());
                }
            }
        }

        Integer[][] resultArray = new Integer[resultList.size() + 1][];
        int i = 0;
        for (Iterator<List<Integer>> resultListIter = resultList.iterator(); resultListIter.hasNext(); i++) {
            resultArray[i] = resultListIter.next().toArray(new Integer[0]);
        }

        resultArray[resultArray.length - 1] = noise.toArray(new Integer[0]);
        result = new ClustersPlusNoise<O>(resultArray, database);
        if (isVerbose()) {
            verbose("");
        }
    }

    protected List<Integer> findSNNNeighbors(Database<O> database, Integer queryObject) {
        List<Integer> neighbors = new LinkedList<Integer>();
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer id = iter.next();
            if (similarityFunction.similarity(queryObject, id).compareTo(epsilon) >= 0) {
                neighbors.add(id);
            }
        }
        return neighbors;
    }

    /**
     * DBSCAN-function expandCluster adapted to SNN criterion.<p/>
     * <p/>
     * Border-Objects become members of the
     * first possible cluster.
     *
     * @param database      the database on which the algorithm is run
     * @param startObjectID potential seed of a new potential cluster
     * @param progress      the progress object to report about the progress of clustering
     */
    protected void expandCluster(Database<O> database, Integer startObjectID, Progress progress) {
        List<Integer> seeds = findSNNNeighbors(database, startObjectID);

        // startObject is no core-object
        if (seeds.size() < minpts) {
            noise.add(startObjectID);
            processedIDs.add(startObjectID);
            if (isVerbose()) {
                progress.setProcessed(processedIDs.size());
                progress(progress, resultList.size());
            }
            return;
        }

        // try to expand the cluster
        List<Integer> currentCluster = new ArrayList<Integer>();
        for (Integer seed : seeds) {
            if (!processedIDs.contains(seed)) {
                currentCluster.add(seed);
                processedIDs.add(seed);
            }
            else if (noise.contains(seed)) {
                currentCluster.add(seed);
                noise.remove(seed);
            }
        }
        seeds.remove(0);

        while (seeds.size() > 0) {
            Integer o = seeds.remove(0);
            List<Integer> neighborhood = findSNNNeighbors(database, o);

            if (neighborhood.size() >= minpts) {
                for (Integer p : neighborhood) {
                    boolean inNoise = noise.contains(p);
                    boolean unclassified = !processedIDs.contains(p);
                    if (inNoise || unclassified) {
                        if (unclassified) {
                            seeds.add(p);
                        }
                        currentCluster.add(p);
                        processedIDs.add(p);
                        if (inNoise) {
                            noise.remove(p);
                        }
                    }
                }
            }

            if (isVerbose()) {
                progress.setProcessed(processedIDs.size());
                int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
                progress(progress, numClusters);
            }

            if (processedIDs.size() == database.size() && noise.size() == 0) {
                break;
            }
        }
        if (currentCluster.size() >= minpts) {
            resultList.add(currentCluster);
        }
        else {
            for (Integer id : currentCluster) {
                noise.add(id);
            }
            noise.add(startObjectID);
            processedIDs.add(startObjectID);
        }
    }

    /**
     * @see Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description("SNN", "Shared Nearest Neighbor Clustering", "Algorithm to find shared-nearest-neighbors-density-connected sets in a database based on the parameters minPts and epsilon (specifying a volume). These two parameters determine a density threshold for clustering.", "L. Ert\u00F6z, M. Steinbach, V. Kumar: Finding Clusters of Different Sizes, Shapes, and Densities in Noisy, High Dimensional Data. In: Proc. of SIAM Data Mining (SDM), 2003");
    }

    /**
     * Sets the parameters epsilon and minpts additionally to the parameters set
     * by the super-class' method. Both epsilon and minpts are required
     * parameters.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        epsilon = new IntegerDistance(getParameterValue(EPSILON_PARAM));

        // minpts
        minpts = getParameterValue(MINPTS_PARAM);

        remainingParameters = similarityFunction.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getResult()
     */
    public ClustersPlusNoise<O> getResult() {
        return result;
    }

    /*
     *
     *
     *
     * @return
     *
    public Option<?>[] getOptions() {
      return this.getOptions();
    }
    */

    public IntegerDistance getEpsilon() {
        return epsilon;
    }


    @Override
    public String description() {
        StringBuilder description = new StringBuilder();
        description.append(super.description());
        description.append(Description.NEWLINE);
        description.append(similarityFunction.inlineDescription());
        description.append(Description.NEWLINE);
        return description.toString();
    }

    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(similarityFunction.getAttributeSettings());
        return attributeSettings;
    }


}