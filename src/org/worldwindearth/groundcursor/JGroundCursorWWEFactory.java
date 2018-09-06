package org.worldwindearth.groundcursor;

import gov.nasa.fog.*;
import gov.nasa.earthatnight.*;
import gov.nasa.bmng.*;
import gov.nasa.worldwind.WorldWindow;
import java.util.ResourceBundle;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.tinyrcp.App;
import org.w3c.dom.Element;
import org.worldwindearth.WWEPlugin;
import org.worldwindearth.WWEFactory;

/**
 *
 * @author sbodmer
 */
public class JGroundCursorWWEFactory extends JPanel implements WWEFactory {

    ResourceBundle bundle = null;
    App app = null;

    /**
     * Creates new form JGridsLMFactory
     */
    public JGroundCursorWWEFactory() {
        bundle = ResourceBundle.getBundle("org.worldwindearth.groundcursor.GroundCursor");

        initComponents();

    }

    //***************************************************************************
    //*** WWELayerfactory
    //***************************************************************************
    @Override
    public String getFactoryCategory() {
        return WWEFactory.PLUGIN_CATEGORY_WORLDWIND_LAYER;
    }

    @Override
    public String getFactoryFamily() {
        return PLUGIN_FAMILY_WORLDWIND_LAYER_GRATICULE;
    }

    @Override
    public void initialize(App app) {
        this.app = app;

    }

    @Override
    public JComponent getFactoryConfigComponent() {
        return null;
    }
    
    @Override
    public void configure(Element config) {
        //---

    }
    
    @Override
    public void store(Element config) {
        //---
    }

    @Override
    public Icon getFactoryIcon(int size) {
        return LB_Name.getIcon();
    }

    @Override
    public String getFactoryName() {
        return LB_Name.getText();
    }

    @Override
    public String getFactoryDescription() {
        return LB_Description.getText();
    }
    
    /**
     * The pass argument is the WorldWindow
     *
     * @param arg
     * @return
     */
    @Override
    public WWEPlugin newPlugin(Object arg) {
        return new JGroundCursorWWEPlugin(this, (WorldWindow) arg);

    }

    @Override
    public void destroy() {
        //---
    }

    @Override
    public Object getProperty(String property) {
        if (property.equals(PROPERTY_AUTHOR)) {
            return "Stephan Bodmer";
        }
        return null;
    }
    
    public boolean doesFactorySupport(Object obj) {
        if (obj != null) return obj.toString().equals(WWEFactory.PLANET_EARTH);
        return false;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        LB_Name = new javax.swing.JLabel();
        LB_Description = new javax.swing.JLabel();

        LB_Name.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/groundcursor/Resources/Icons/22x22/groundCursor.png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/worldwindearth/groundcursor/GroundCursor"); // NOI18N
        LB_Name.setText(bundle.getString("factory_name")); // NOI18N

        LB_Description.setText("<html><body>\nCenter of the viewprt ground cursor\n</body></html>");
        LB_Description.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JLabel LB_Description;
    public javax.swing.JLabel LB_Name;
    // End of variables declaration//GEN-END:variables
}
