/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.worldwindearth.models;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author sbodmer
 */
public class JModelCellRenderer extends javax.swing.JPanel implements ListCellRenderer, TableCellRenderer {

    /**
     * Creates new form JFileListCellRenderer
     */
    public JModelCellRenderer() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        LB_Name = new javax.swing.JLabel();

        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        add(LB_Name);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JLabel LB_Name;
    // End of variables declaration//GEN-END:variables

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value instanceof Model) {
            Model t = (Model) value;
            LB_Name.setText(t.getTitle());
            setToolTipText(t.getFile().getPath());
        }
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            
        } else {
            setBackground(Color.WHITE);
            
        }
        
        return this;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof Model) {
            Model t = (Model) value;
            LB_Name.setText(t.getTitle());
            setToolTipText(t.getFile().getPath());
        }
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            
        } else {
            setBackground(table.getBackground());
            
        }
        
        return this;
    }
}
