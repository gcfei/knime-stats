/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 */
package org.knime.ext.tsne.node;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TsneNodeDialog extends DefaultNodeSettingsPane {

    TsneNodeDialog() {
        final DialogComponentColumnFilter2 featureComp =
            new DialogComponentColumnFilter2(TsneNodeModel.createFeaturesModel(), 0);
        final DialogComponentNumber outputDimComp =
            new DialogComponentNumber(TsneNodeModel.createOutputDimensionsModel(), "Dimension(s) to reduce to", 1);
        final DialogComponentNumber iterationsComp =
            new DialogComponentNumber(TsneNodeModel.createIterationsModel(), "Iterations", 10);
        final DialogComponentNumber learningRate =
            new DialogComponentNumber(TsneNodeModel.createThetaModel(), "Theta", 0.1);
        final DialogComponentNumber perplexity =
            new DialogComponentNumber(TsneNodeModel.createPerplexityModel(), "Perplexity", 1.0);
        final DialogComponentBoolean removeOriginalColumns = new DialogComponentBoolean(
            TsneNodeModel.createRemoveOriginalColumnsModel(), "Remove original data columns");
        final DialogComponentBoolean failOnMissingValues = new DialogComponentBoolean(
            TsneNodeModel.createFailOnMissingValuesModel(), "Fail if missing values are encountered");
        final DialogComponentSeed seed = new DialogComponentSeed(TsneNodeModel.createSeedModel(), "Seed");
        addDialogComponent(featureComp);
        addDialogComponent(outputDimComp);
        addDialogComponent(iterationsComp);
        addDialogComponent(learningRate);
        addDialogComponent(perplexity);
        addDialogComponent(removeOriginalColumns);
        addDialogComponent(failOnMissingValues);
        addDialogComponent(seed);
        setDefaultTabTitle("Settings");
    }

}
