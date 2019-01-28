/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   14.04.2015 (Alexander): created
 */
package org.knime.base.node.stats.lda2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.knime.base.node.mine.pca.EigenValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * The class that does the math of a Linear Discriminant Analysis (LDA).
 *
 * @author Alexander Fillbrunn
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
final class LDA2 {

    private static final String MISSING_VALUE_WARNING =
        "Missing values are not supported. Please replace them using a  \"Missing Value\" node.";

    private final int[] m_predVarIndices;

    private RealMatrix m_w;

    private List<EigenValue> m_eigenValues;

    private int m_k = 0;

    /**
     * Constructor for creation from an existing transformation matrix, used only for prediction.
     *
     * @param colIndices the indices of the columns to use for the prediction
     * @param w the transformation matrix
     */
    LDA2(final int[] colIndices, final RealMatrix w) {
        // variables used for the projection
        m_predVarIndices = colIndices;
        m_w = w;
    }

    /**
     * The constructor for an LDA analysis, used to calculate the transformation matrix prior to prediction.
     *
     * @param colIndices the class column's index, used to calculate the transformation and the prediction.
     * @throws InvalidSettingsException when the table has no data
     */
    LDA2(final int[] colIndices) throws InvalidSettingsException {
        if (colIndices == null || colIndices.length == 0) {
            throw new InvalidSettingsException("No column is given to calculate the transformation matrix.");
        }
        m_predVarIndices = colIndices;
    }

    /**
     * Calculates the projection of the data in the row.
     *
     * @param row the row to calculate the projection for. Included fields are those that were given the constructor.
     * @return an array of double cells that constitutes the projection of the data.
     * @throws InvalidSettingsException when there are missing values
     */
    DoubleCell[] getProjection(final DataRow row, final int dim) throws InvalidSettingsException {
        return writeCells(calculateProjection(row), dim);
    }

    /**
     * Calculates the projection of the data in the row.
     *
     * @param row the row to calculate the projection for. Included fields are those that were given the constructor.
     * @return an array of double cells that constitutes the projection of the data.
     * @throws InvalidSettingsException when there are missing values
     */
    DoubleCell[] getProjection(final DataRow row) throws InvalidSettingsException {
        final RealVector y = calculateProjection(row);
        return writeCells(y, y.getDimension());
    }

    private RealVector calculateProjection(final DataRow row) throws InvalidSettingsException {
        if (m_w == null) {
            throw new IllegalStateException("The transformation matrix has not been calculated");
        }
        final RealVector x = rowToRealVector(row);
        final RealVector y = m_w.operate(x);
        return y;
    }


    private static DoubleCell[] writeCells(final RealVector res, final int dim) {
        final DoubleCell[] cells = new DoubleCell[dim];
        for (int i = 0; i < dim; i++) {
            cells[i] = new DoubleCell(res.getEntry(i));
        }
        return cells;
    }

    int getMaxDim() {
        CheckUtils.checkArgument(m_k>0, "Run calculateTransformationMatrix before calling this method");
        return m_k;
    }

    /**
     * @return The transformation matrix or {@code null} if not (yet) calculated.
     */
    RealMatrix getTransformationMatrix() {
        return m_w;
    }

    /**
     * @return the eigenvalues and -vectors as a sorted list
     */
    List<EigenValue> getEigenvalues() {
        return m_eigenValues;
    }

    // "tuple" class to save one HashMap instead of two
    static final class ClassStats {
        private final RealVector m_sum;

        private int m_count;

        public ClassStats(final int length) {
            m_sum = new ArrayRealVector(length, 0.0);
            m_count = 0;
        }

        /**
         * Normalizes the sum to get the mean value of the data.
         */
        public void normalize() {
            m_sum.mapDivideToSelf(m_count);
        }

        /**
         * Add data to this class and update the count.
         *
         * @param data Values to add.
         */
        public void add(final RealVector data) {
            m_count++;
            m_sum.combineToSelf(1, 1, data);
        }

        public RealVector getSum() {
            return m_sum;
        }

        public int getCount() {
            return m_count;
        }
    }

    private Map<String, ClassStats> calculateClassStats(final ExecutionContext exec, final BufferedDataTable inTable,
        final int k, final int classColIndex) throws CanceledExecutionException, InvalidSettingsException {
        final Map<String, ClassStats> classStats = new HashMap<>();
        // Values for calculating the total mean in the end
        long progressCount = 0;
        // First calculate the class means and counts and the total mean and count
        for (final DataRow row : inTable) {
            exec.checkCanceled();
            progressCount++;
            exec.setProgress(progressCount / ((double)inTable.size()), "Calculating class stats - Processed row "
                + progressCount + "/" + inTable.size() + " (\"" + row.getKey() + "\")");

            final DataCell cell = row.getCell(classColIndex);
            if (cell.isMissing()) {
                throw new RuntimeException(MISSING_VALUE_WARNING);
            }
            final String cl = cell.toString();

            final RealVector d = rowToRealVector(row);

            // will be null in the first iteration
            if (!classStats.containsKey(cl)) {
                classStats.put(cl, new ClassStats(m_predVarIndices.length));
            }
            classStats.get(cl).add(d);
        }

        if (classStats.size() == 0) {
            throw new InvalidSettingsException("The table contains no classes");
        }

        if (k >= classStats.size()) {
            throw new InvalidSettingsException("Not enough classes (" + classStats.size() + ") in the class column \""
                + inTable.getSpec().getColumnSpec(classColIndex).getName() + "\": The data can only be reduced to "
                + (classStats.size() - 1) + " or fewer dimensions.");
        }

        return classStats;
    }

    /**
     * Calculates the transformation matrix for the projection of data into the smaller space.
     *
     * @param exec the execution context.
     * @throws CanceledExecutionException when the execution is cancelled by the user.
     * @throws InvalidSettingsException when the settings are not suitable for the data.
     */
    void calculateTransformationMatrix(final ExecutionContext exec, final BufferedDataTable inTable,
        final int classColIndex) throws CanceledExecutionException, InvalidSettingsException {
        checkClassColIdx(classColIndex);
        final Map<String, ClassStats> classStats =
            calculateClassStats(exec.createSubExecutionContext(0.5), inTable, -1, classColIndex);
        m_k = Math.min(classStats.size() - 1, m_predVarIndices.length);
        CheckUtils.checkArgument(m_k > 0, "The class column \"%s\" contains only a single class.",
            classStats.keySet().iterator().next());
        calcTransformationMatrix(exec, inTable, classStats, classColIndex);
    }

    /**
     * Calculates the transformation matrix for the projection of data into the smaller space.
     *
     * @param exec the execution context.
     * @throws CanceledExecutionException when the execution is cancelled by the user.
     * @throws InvalidSettingsException when the settings are not suitable for the data.
     */
    void calculateTransformationMatrix(final ExecutionContext exec, final BufferedDataTable inTable, final int k,
        final int classColIndex) throws CanceledExecutionException, InvalidSettingsException {
        checkClassColIdx(classColIndex);
        CheckUtils.checkSetting(k > 0, "Cannot reduce the number of dimensions to less than one.");
        m_k = k;
        // Map for storing the per-class sum and count to calculate the mean
        final Map<String, ClassStats> classStats =
            calculateClassStats(exec.createSubExecutionContext(0.5), inTable, m_k, classColIndex);

        calcTransformationMatrix(exec, inTable, classStats, classColIndex);
    }

    /**
     * @param exec
     * @param inTable
     * @param classStats
     * @param k
     * @param classColIndex
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     */
    private void calcTransformationMatrix(ExecutionContext exec, final BufferedDataTable inTable,
        final Map<String, ClassStats> classStats, final int classColIndex)
        throws InvalidSettingsException, CanceledExecutionException {
        // calculate the mean
        final RealVector totalMean = new ArrayRealVector(m_predVarIndices.length, 0.0);

        for (final Map.Entry<String, ClassStats> entry : classStats.entrySet()) {
            final int count = entry.getValue().getCount();
            if (count < m_predVarIndices.length) {
                throw new InvalidSettingsException(
                    "The size of the smallest group must be larger than the number of predictor variables ("
                        + m_predVarIndices.length + "). Class \"" + entry.getKey() + "\" has only " + count
                        + " instance" + (count == 1 ? "." : "s."));
            }
            totalMean.combineToSelf(1, 1, entry.getValue().getSum());

            // finally, normalize the class stats
            entry.getValue().normalize();
        }

        totalMean.mapDivideToSelf(inTable.size());

        // Calculate the inter-class scatter matrix
        RealMatrix sw = null;
        long progressCount = 0;
        exec = exec.createSubExecutionContext(0.5);
        for (final DataRow row : inTable) {
            exec.checkCanceled();
            progressCount++;
            exec.setProgress(progressCount / ((double)inTable.size()),
                "Calculating inter-class scatter matrix - Processed row " + progressCount + "/" + inTable.size()
                    + " (\"" + row.getKey() + "\").");

            // subtract mean from data
            final RealVector v = rowToRealVector(row).combineToSelf(1, -1,
                classStats.get(row.getCell(classColIndex).toString()).getSum());

            // make it to a matrix: do the outer product
            final RealMatrix si = v.outerProduct(v);

            sw = (sw != null) ? sw.add(si) : si;
        }

        // Calculate the intra-class scatter matrix
        RealMatrix sb = null;
        progressCount = 0;
        exec = exec.createSubExecutionContext(0.1);
        for (final Map.Entry<String, ClassStats> entry : classStats.entrySet()) {
            exec.checkCanceled();
            progressCount++;
            exec.setProgress(progressCount / ((double)inTable.size()),
                "Calculating intra-class scatter matrix - Processed class " + progressCount + "/" + classStats.size()
                    + " (\"" + entry.getKey() + "\").");

            // subtract total mean from class mean and build the scatter matrix
            final RealVector v = entry.getValue().getSum().combineToSelf(1, -1, totalMean);
            final RealMatrix s = v.outerProduct(v).scalarMultiply(entry.getValue().getCount());

            sb = (sb != null) ? sb.add(s) : s;
        }

        // check for reasonable inter-class scatter matrix and throw reasonable error (AP-6588)
        final DecompositionSolver solver = new LUDecomposition(sw).getSolver();
        if (!solver.isNonSingular()) {
            throw new InvalidSettingsException(
                "Cannot invert the inter-class scatter matrix as it is singular. Most likely, two input columns are"
                    + " linearly dependent, i.e. differ only by a constant factor.");
        }

        // Now extract eigenvalues of sw^-1 * sb
        final RealMatrix mat = solver.getInverse().multiply(sb);
        final EigenDecomposition ed = new EigenDecomposition(mat);
        final double[] eigenValues = ed.getRealEigenvalues();
        final double[][] eigenVectors = new double[m_predVarIndices.length][eigenValues.length];
        for (int i = 0; i < eigenValues.length; i++) {
            final double[] vec = ed.getEigenvector(i).toArray();
            for (int j = 0; j < m_predVarIndices.length; j++) {
                eigenVectors[j][i] = vec[j];
            }
        }

        m_eigenValues = EigenValue.createSortedList(eigenVectors, eigenValues);

        // Create the transformation matrix.
        final double[][] w = new double[m_k][];
        for (int i = 0; i < m_k; i++) {
            final double[] vec = m_eigenValues.get(i).getEigenVector();
            w[i] = vec;
        }

        m_w = MatrixUtils.createRealMatrix(w);
    }

    /**
     * @param classColIndex
     * @throws InvalidSettingsException
     */
    private void checkClassColIdx(final int classColIndex) throws InvalidSettingsException {
        CheckUtils.checkSetting(classColIndex > 0, "No valid class column is given.");
    }

    private RealVector rowToRealVector(final DataRow row) throws InvalidSettingsException {
        final RealVector d = new ArrayRealVector(m_predVarIndices.length);
        for (int c = 0; c < m_predVarIndices.length; c++) {
            final DataCell cell = row.getCell(m_predVarIndices[c]);
            if (cell.isMissing()) {
                throw new InvalidSettingsException(MISSING_VALUE_WARNING);
            }
            d.setEntry(c, ((DoubleValue)cell).getDoubleValue());
        }

        return d;
    }

    /**
     * Creates an explanatory warning why the currently selected dimension is zero. Gives reason whether the selected
     * classes are too low or the selected columns.
     *
     * @param selectedClasses Number of classes in the currently selected class column
     * @param selectedColumns Number of currently selected columns.
     * @param classColName The name of the currently selected class column.
     * @return the explanatory warning message.
     */
    static final String createMaxDimZeroWarning(final int selectedClasses, final int selectedColumns,
        final String classColName) {
        final StringBuilder stb = new StringBuilder("The maximum allowed dimension is 0.");

        if ((selectedClasses - 1) <= 0) {
            if (selectedClasses == -1) {
                stb.append(" There are more than 60 distinct values in the selected class column \"" + classColName
                    + "\", such that the columns domain was not calculated by default. Please use the"
                    + " \"Domain Calculator\" node.");
            } else if (selectedClasses == 0) {
                stb.append(" There are only missing values in the selected class column \"" + classColName
                    + "\". Please provide a class column with at least two distinct values.");
            } else if (selectedClasses == 1) {
                stb.append(" There is only one distinct value in the selected class column \"" + classColName
                    + "\". Please provide a class column with at least two distinct values.");
            }
        }

        if (selectedColumns == 0) {
            if ((selectedClasses - 1) <= 0) {
                stb.append(" Also, t");
            } else {
                stb.append(" T");
            }
            stb.append("here are no columns selected. Please select at least one.");
        }

        return stb.toString();
    }

    /**
     * Creates an explanatory warning why the currently selected dimension is too high compared to the maximum allowed
     * dimension. Gives reason whether the selected classes are too low or the selected columns.
     *
     * @param currentDim Currently selected number of dimensions to project to.
     * @param maximumDim Maximum allowed dimensions.
     * @param selectedClasses Number of classes in the currently selected class column
     * @param selectedColumns Number of currently selected columns.
     * @param classColName The name of the currently selected class column.
     * @return the explanatory warning message.
     */
    static final String createTooHighDimWarning(final int currentDim, final int maximumDim, final int selectedClasses,
        final int selectedColumns, final String classColName) {

        final StringBuilder stb = new StringBuilder("The current number of selected dimensions (" + currentDim
            + ") is higher than the maximum allowed value of " + maximumDim + ".");

        if (maximumDim == (selectedClasses - 1)) {
            stb.append(" The selected class column \"" + classColName + "\" only features " + selectedClasses
                + " distinct value" + (selectedClasses == 1 ? "" : "s"));
            if (maximumDim != selectedColumns) {
                stb.append(".");
            }
        }

        if (maximumDim == selectedColumns) {
            if (maximumDim == (selectedClasses - 1)) {
                stb.append(" and o");
            } else {
                stb.append(" O");
            }
            stb.append("nly " + selectedColumns + " column" + (selectedColumns == 1 ? " is" : "s are") + " selected.");
        }
        return stb.toString();
    }
}