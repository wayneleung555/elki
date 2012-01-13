package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Initialize K-means by using the first k objects as initial means.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class FirstKInitialMeans<V extends NumberVector<V, ?>> extends AbstractKMeansInitialization<V> {
  /**
   * Constructor.
   */
  public FirstKInitialMeans() {
    super(null);
  }

  @Override
  public List<V> chooseInitialMeans(Relation<V> relation, int k, PrimitiveDistanceFunction<? super V, ?> distanceFunction) {
    Iterator<DBID> iter = relation.iterDBIDs();
    List<V> means = new ArrayList<V>(k);
    for(int i = 0; i < k && iter.hasNext(); i++) {
      means.add(relation.get(iter.next()));
    }
    return means;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    @Override
    protected FirstKInitialMeans<V> makeInstance() {
      return new FirstKInitialMeans<V>();
    }
  }
}