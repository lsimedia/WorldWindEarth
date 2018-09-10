/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.worldwindearth.groundcursor;

import gov.nasa.fog.*;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.BasicLayerFactory;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.TiledImageLayer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.w3c.dom.Element;
import org.tinyrcp.App;
import org.tinyrcp.TinyFactory;
import org.worldwindearth.WWEPlugin;

/**
 * Create layer from default xml
 * 
 * @author sbodmer
 */
public class JGroundCursorWWEPlugin extends JPanel implements WWEPlugin, ActionListener, ChangeListener {
    App app = null;
    TinyFactory factory = null;
    WorldWindow ww = null;
    
    GroundCursorLayer layer = null;
    
    /**
     * Creates new form JTerminalsLayer
     */
    public JGroundCursorWWEPlugin(TinyFactory factory, WorldWindow ww) {
        super();
        this.factory = factory;
        this.ww = ww;
        
        initComponents();
        
        
    }

    
    
    //**************************************************************************
    //*** Plugin
    //**************************************************************************
    @Override
    public String getPluginName() {
        return factory.getFactoryName();
    }
    
    @Override
    public JComponent getConfigComponent() {
        return this;
    }

    @Override
    public TinyFactory getPluginFactory() {
        return factory;
    }

    @Override
    public void setup(App app, Object arg) {
        this.app = app;
        
        SP_Zoom.addChangeListener(this);
        CB_DrawTileInfo.addActionListener(this);
        
        //--- Create layer form defaut config
        layer = new GroundCursorLayer();
        layer.setName("Ground cursor");
        
    }

    @Override
    public void cleanup() {
        layer.dispose();
        
    }

    @Override
    public void saveConfig(Element config) {
        if (config == null) return;

        config.setAttribute("zoom", "" + SP_Zoom.getValue());
        config.setAttribute("drawTileInfo", "" + CB_DrawTileInfo.isSelected());
        
    }

    @Override
    public Object getProperty(String property) {
        return null;
    }

    @Override
    public void setProperty(String property, Object value) {
        //---
    }

    @Override
    public Object doAction(String action, Object argument, Object subject) {
        return null;
    }

    @Override
    public void setPluginName(String name) {
        layer.setName(name);
    }

    @Override
    public JComponent getVisualComponent() {
        return null;
    }

    @Override
    public void configure(Element config) {
        if (config == null) return;

        try {
            SP_Zoom.setValue(Integer.parseInt(config.getAttribute("zoom")));
            CB_DrawTileInfo.setSelected(config.getAttribute("drawTileInfo").equals("true"));

            layer.setZoom(SP_Zoom.getValue());
            layer.setDrawTileInfo(CB_DrawTileInfo.isSelected());
            
        } catch (NumberFormatException ex) {
            //--- 
        }
    }
    
    //**************************************************************************
    //*** WorldWindLayerPlugin
    //**************************************************************************
    @Override
    public Layer getLayer() {
        return layer;
    }
    
    @Override
    public boolean hasLayerButton() {
        return false;
    }
    
    @Override
    public void layerMouseClicked(MouseEvent e, gov.nasa.worldwind.geom.Position pos) {
        //---
    }
    
    //**************************************************************************
    //*** ActionListener
    //**************************************************************************
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("drawTileInfo")) {
            layer.setDrawTileInfo(CB_DrawTileInfo.isSelected());
            ww.redraw();

        }
    }
    
    //**************************************************************************
    //*** ChangeListener
    //**************************************************************************
    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == SP_Zoom) {
            layer.setZoom(SP_Zoom.getValue());
            ww.redraw();
        }
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        SP_Zoom = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        CB_DrawTileInfo = new javax.swing.JCheckBox();

        SP_Zoom.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        SP_Zoom.setMajorTickSpacing(1);
        SP_Zoom.setMaximum(18);
        SP_Zoom.setMinimum(12);
        SP_Zoom.setMinorTickSpacing(1);
        SP_Zoom.setPaintLabels(true);
        SP_Zoom.setPaintTicks(true);
        SP_Zoom.setSnapToTicks(true);
        SP_Zoom.setToolTipText("Tile zoom");
        SP_Zoom.setValue(15);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("Draw tile info");
        jLabel1.setPreferredSize(new java.awt.Dimension(140, 26));

        CB_DrawTileInfo.setActionCommand("drawTileInfo");
        CB_DrawTileInfo.setPreferredSize(new java.awt.Dimension(26, 26));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SP_Zoom, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(CB_DrawTileInfo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(SP_Zoom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CB_DrawTileInfo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(214, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox CB_DrawTileInfo;
    private javax.swing.JSlider SP_Zoom;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
}
