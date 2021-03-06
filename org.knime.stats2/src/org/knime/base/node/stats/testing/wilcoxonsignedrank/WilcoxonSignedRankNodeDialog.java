package org.knime.base.node.stats.testing.wilcoxonsignedrank;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnPairsSelectionPanel;

/**
 * @author Patrick Winter, University of Konstanz
 */
class WilcoxonSignedRankNodeDialog extends NodeDialogPane {

    private ColumnPairsSelectionPanel m_columnPairs;

    public WilcoxonSignedRankNodeDialog() {
        m_columnPairs = new ColumnPairsSelectionPanel() {
            private static final long serialVersionUID = -6485698971147583920L;

            @SuppressWarnings({"rawtypes", "unchecked"})
            @Override
            protected void initComboBox(final DataTableSpec spec, final JComboBox comboBox, final String selected) {
                DefaultComboBoxModel comboBoxModel = (DefaultComboBoxModel)comboBox.getModel();
                comboBoxModel.removeAllElements();
                for (DataColumnSpec colSpec : spec) {
                    if (colSpec.getType().isCompatible(DoubleValue.class)) {
                        comboBoxModel.addElement(colSpec);
                        if (null != selected && colSpec.getName().equals(selected)) {
                            comboBoxModel.setSelectedItem(colSpec);
                        }
                    }
                }
            }
        };
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        JScrollPane scrollPane = new JScrollPane(m_columnPairs);
        Component header = m_columnPairs.getHeaderView("Left column", "Right column");
        header.setPreferredSize(new Dimension(300, 20));
        scrollPane.setColumnHeaderView(header);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        scrollPane.setMinimumSize(new Dimension(300, 100));
        m_columnPairs.setBackground(Color.white);
        panel.add(scrollPane, gbc);
        addTab("Config", panel);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        WilcoxonSignedRankNodeConfig config = new WilcoxonSignedRankNodeConfig();
        config.setFirstColumns(objectsToColumnNames(m_columnPairs.getLeftSelectedItems()));
        config.setSecondColumns(objectsToColumnNames(m_columnPairs.getRightSelectedItems()));
        config.save(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        WilcoxonSignedRankNodeConfig config = new WilcoxonSignedRankNodeConfig();
        config.loadInDialog(settings);
        String[] firstColumns = config.getFirstColumns();
        String[] secondColumns = config.getSecondColumns();
        if (firstColumns.length == 0) {
            firstColumns = null;
            secondColumns = null;
        }
        m_columnPairs.updateData(new DataTableSpec[]{specs[0], specs[0]}, firstColumns, secondColumns);
    }

    private String[] objectsToColumnNames(final Object[] objects) {
        String[] names = new String[objects.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = ((DataColumnSpec)objects[i]).getName();
        }
        return names;
    }

}
