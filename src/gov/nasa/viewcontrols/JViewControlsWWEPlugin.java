/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nasa.viewcontrols;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.layers.ViewControlsSelectListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import org.w3c.dom.Element;
import org.tinyrcp.App;
import org.tinyrcp.TinyFactory;
import org.worldwindearth.WWEPlugin;

/**
 * Controls view
 * 
 * 
 * @author sbodmer
 */
public class JViewControlsWWEPlugin extends JPanel implements WWEPlugin, ActionListener {
    App app = null;
    TinyFactory factory = null;
    WorldWindow ww = null;
    
    ViewControlsLayer layer = new ViewControlsLayer();
    ViewControlsSelectListener clistener = null;
    /**
     * Creates new form JTerminalsLayer
     */
    public JViewControlsWWEPlugin(TinyFactory factory, WorldWindow ww) {
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
        return null;
    }

    @Override
    public TinyFactory getPluginFactory() {
        return factory;
    }

    @Override
    public void setup(App app, Object arg) {
        this.app = app;
        
        layer.setName("View controls");
        layer.setValue(AVKEY_WORLDWIND_LAYER_PLUGIN, this);
        clistener = new ViewControlsSelectListener(ww, layer);
        ww.addSelectListener(clistener);
        
    }

    @Override
    public void cleanup() {
        System.out.println("(W) TODO: The wordlwind ViewControlsSelectListener has a timer set, but no way to stop it...");
        ww.removeSelectListener(clistener);
        layer.dispose();
        
    }

    @Override
    public void saveConfig(Element config) {
        //---
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
        //---
    }
    
    //**************************************************************************
    //*** WWEPlugin
    //**************************************************************************
    @Override
    public Layer getLayer() {
        return layer;
    }
    
    @Override
    public boolean hasLayerButton() {
        return false;
    }
    
    //**************************************************************************
    //*** ActionListener
    //**************************************************************************
    @Override
    public void actionPerformed(ActionEvent e) {
        //---
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
