package de.lmu.ifi.dbs.elki.datasource.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Parser reads points transposed. Line n gives the n-th attribute for all
 * points.
 * 
 * @author Arthur Zimek
 */
public class DoubleVectorLabelTransposingParser extends DoubleVectorLabelParser {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(DoubleVectorLabelTransposingParser.class);

  /**
   * Constructor.
   * 
   * @param colSep
   * @param quoteChar
   * @param labelIndices
   */
  public DoubleVectorLabelTransposingParser(Pattern colSep, char quoteChar, BitSet labelIndices) {
    super(colSep, quoteChar, labelIndices);
  }

  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    List<Double>[] data = null;
    LabelList[] labels = null;

    int dimensionality = -1;

    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          List<String> entries = tokenize(line);
          if(dimensionality == -1) {
            dimensionality = entries.size();
          }
          else if(entries.size() != dimensionality) {
            throw new IllegalArgumentException("Differing dimensionality in line " + (lineNumber) + ", " + "expected: " + dimensionality + ", read: " + entries.size());
          }

          if(data == null) {
            data = ClassGenericsUtil.newArrayOfEmptyArrayList(dimensionality);
            /*
             * for (int i = 0; i < data.length; i++) { data[i] = new
             * ArrayList<Double>(); }
             */
            labels = ClassGenericsUtil.newArrayOfNull(dimensionality, LabelList.class);
            for(int i = 0; i < labels.length; i++) {
              labels[i] = new LabelList();
            }
          }

          for(int i = 0; i < entries.size(); i++) {
            try {
              Double attribute = Double.valueOf(entries.get(i));
              data[i].add(attribute);
            }
            catch(NumberFormatException e) {
              labels[i].add(entries.get(i));
            }
          }
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    List<Object> vectors = new ArrayList<Object>();
    List<Object> lblc = new ArrayList<Object>();
    for(int i = 0; i < data.length; i++) {
      DoubleVector featureVector = new DoubleVector(data[i]);
      vectors.add(featureVector);
      lblc.add(labels[i]);
    }
    BundleMeta meta = new BundleMeta();
    List<List<Object>> columns = new ArrayList<List<Object>>(2);
    meta.add(getTypeInformation(dimensionality));
    columns.add(vectors);
    meta.add(TypeUtil.LABELLIST);
    columns.add(lblc);
    return new MultipleObjectsBundle(meta, columns);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends NumberVectorLabelParser.Parameterizer<DoubleVector> {
    @Override
    protected DoubleVectorLabelTransposingParser makeInstance() {
      return new DoubleVectorLabelTransposingParser(colSep, quoteChar, labelIndices);
    }
  }
}