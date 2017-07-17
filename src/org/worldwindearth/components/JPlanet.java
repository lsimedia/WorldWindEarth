/*
 * JNotesPanel.java
 *
 * Created on July 26, 2006, 3:09 PM
 */
package org.worldwindearth.components;

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwindx.examples.ScreenShots;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.tinyrcp.App;
import org.tinyrcp.JSettingsFrame;
import org.tinyrcp.TinyFactory;
import org.w3c.dom.*;
import org.worldwindearth.WWEPlugin;
import org.tinyrcp.TinyPlugin;
import org.worldwindearth.JWWEPluginCellRenderer;
import org.worldwindearth.WWEFactory;
import org.worldwindearth.WorldWindLayersTableModel;
import org.worldwindearth.components.renderers.JCameraCellRenderer;

/**
 * All events are handled and fired by the embedded JQueryPanel
 *
 * @author sbodmer
 */
public class JPlanet extends JPanel implements KeyListener, ComponentListener, ActionListener, ListSelectionListener, MouseListener, MouseWheelListener {

    NumberFormat nf = new DecimalFormat("#.########");

    App app = null;
    ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
    ResourceBundle bundle = null;

    /**
     * World wind window
     */
    // WorldWindowGLCanvas wwd = null;
    WorldWindowGLJPanel wwd = null;

    /**
     * Initial camera position
     */
    Position eye = null;
    DefaultListModel<Camera> cameras = new DefaultListModel<Camera>();

    /**
     * The layers list
     */
    WorldWindLayersTableModel layers = null;

    /**
     * The worldwindmodel
     */
    Model m = null;

    /**
     * Custom layer plugin, the key is the world wind layer hash
     */
    HashMap<String, WWEPlugin> plugins = new HashMap<>();

    /**
     * The refresh tick
     */
    javax.swing.Timer timer = null;

    /**
     * New viewport broadcast timer
     */
    javax.swing.Timer vtimer = null;

    /**
     * Full screen frame
     */
    JFrame jframe = null;         //--- Fullscreen frame

    /**
     * Creates new form JTerminals
     */
    public JPlanet() {
        bundle = ResourceBundle.getBundle("org.worldwindearth.components.Planet");

        initComponents();

        BT_Configure.setVisible(false);
        
        PN_Cameras.setVisible(false);
        LI_Cameras.setModel(cameras);

        //--- Set a custom desktop manager, which will block the layers internal frame if sticky
        TB_Layers.getSelectionModel().addListSelectionListener(this);
        TB_Layers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        //--- Find available screens
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        for (int i = 0;i < gd.length;i++) {
            JRadioButtonMenuItem jitem = new JRadioButtonMenuItem(gd[i].getIDstring());
            jitem.setActionCommand("screen");
            jitem.addActionListener(this);
            jitem.putClientProperty("gd", gd[i]);
            MN_Screens.add(jitem);
            btgscreens.add(jitem);
            if (i == 0) jitem.setSelected(true);
        }
        MN_Fullscreen.addActionListener(this);
        MN_ScreenIdentifier.addActionListener(this);

        //--- Position check (send viewport message after this delay)
        timer = new javax.swing.Timer(2000, this);

        vtimer = new javax.swing.Timer(1000, this);
        vtimer.setRepeats(false);
    }

    //**************************************************************************
    //*** API
    //**************************************************************************
    public void addActionListener(ActionListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
    }

    public void initialize(App app, Model model, Object obj) {
        this.app = app;
        this.m = model;

        wwd = new WorldWindowGLJPanel();
        wwd.setModel(m);

        LI_Cameras.setCellRenderer(new JCameraCellRenderer(app));
        LI_Cameras.addMouseListener(this);
        BT_Cameras.addActionListener(this);
        BT_NewCamera.addActionListener(this);
        BT_RemoveCamera.addActionListener(this);

        BT_AddLayer.addActionListener(this);
        BT_RemoveLayer.addActionListener(this);
        BT_LayerUp.addActionListener(this);
        BT_LayerDown.addActionListener(this);

        BT_Collapse.addActionListener(this);
        BT_More.addActionListener(this);
        BT_Sticky.addActionListener(this);

        BT_ScrollLeft.addActionListener(this);
        BT_ScrollRight.addActionListener(this);

        BT_RenameLayer.addActionListener(this);

        DP_Main.add(wwd, JLayeredPane.DEFAULT_LAYER);
        DP_Main.addComponentListener(this);
        // DP_Main.addMouseListener(this);

        PU_More.add(app.createFactoryMenus(app.getString("word_panels", "App"), TinyFactory.PLUGIN_CATEGORY_PANEL, TinyFactory.PLUGIN_FAMILY_PANEL, this), 0);
        PU_More.add(app.createFactoryMenus(app.getString("word_containers", "App"), TinyFactory.PLUGIN_CATEGORY_PANEL, TinyFactory.PLUGIN_FAMILY_CONTAINER, this), 0);

        MN_HideStatusBar.addActionListener(this);

        InputHandler ih = wwd.getInputHandler();
        ih.addKeyListener(this);
        ih.addMouseListener(this);
        ih.addMouseWheelListener(this);
    }

    public void configure(Element config) {
        //------------------------------------------------------------------------
        //--- Find "layer" category in jars and create the new layer menu
        //------------------------------------------------------------------------
        MN_NewLayers.removeAll();
        JMenu jmenu = app.createFactoryMenus("Worldwind", WWEFactory.PLUGIN_CATEGORY_WORLDWIND_LAYER, WWEFactory.PLUGIN_FAMILY_WORLDWIND_LAYER_WORLDWIND, this);
        MN_NewLayers.add(jmenu, 0);
        jmenu = app.createFactoryMenus("Nasa", WWEFactory.PLUGIN_CATEGORY_WORLDWIND_LAYER, WWEFactory.PLUGIN_FAMILY_WORLDWIND_LAYER_NASA, this);
        MN_NewLayers.add(jmenu, 1);
        jmenu = app.createFactoryMenus("Map Tiles", WWEFactory.PLUGIN_CATEGORY_WORLDWIND_LAYER, WWEFactory.PLUGIN_FAMILY_WORLDWIND_LAYER_MAPTILES, this);
        MN_NewLayers.add(jmenu, 2);
        jmenu = app.createFactoryMenus("Web Map Server (WMS)", WWEFactory.PLUGIN_CATEGORY_WORLDWIND_LAYER, WWEFactory.PLUGIN_FAMILY_WORLDWIND_LAYER_WMS, this);
        MN_NewLayers.add(jmenu, 3);
        jmenu = app.createFactoryMenus("Buildings", WWEFactory.PLUGIN_CATEGORY_WORLDWIND_LAYER, WWEFactory.PLUGIN_FAMILY_WORLDWIND_LAYER_BUILDINGS, this);
        MN_NewLayers.add(jmenu, 4);
        jmenu = app.createFactoryMenus("Graticules", WWEFactory.PLUGIN_CATEGORY_WORLDWIND_LAYER, WWEFactory.PLUGIN_FAMILY_WORLDWIND_LAYER_GRATICULE, this);
        MN_NewLayers.add(jmenu, 5);

        //--- Prepare table
        layers = new WorldWindLayersTableModel(m.getLayers());
        TB_Layers.setModel(layers);
        TB_Layers.setDefaultRenderer(Layer.class, new JWWEPluginCellRenderer(app));
        TB_Layers.getColumnModel().getColumn(0).setPreferredWidth(26);
        TB_Layers.getColumnModel().getColumn(0).setWidth(26);
        TB_Layers.getColumnModel().getColumn(0).setMaxWidth(26);
        TB_Layers.getColumnModel().getColumn(0).setMinWidth(26);

        //--- Instantiate layers
        if (config != null) {
            NodeList nl = config.getChildNodes();
            for (int i = 0;i < nl.getLength();i++) {
                if (nl.item(i).getNodeName().equals("Eye")) {
                    try {
                        //--- Set viewport
                        Element c = (Element) nl.item(i);
                        double lat = Double.parseDouble(c.getAttribute("lat"));
                        double lon = Double.parseDouble(c.getAttribute("lon"));
                        double alt = Double.parseDouble(c.getAttribute("alt"));

                        // double cz = Double.parseDouble(c.getAttribute("cz"));
                        // wwd.getView().goTo(Position.fromDegrees(y, x), z);
                        eye = Position.fromDegrees(lat, lon, alt);

                        // center = Position.fromDegrees(cy, cx, cz);
                    } catch (NumberFormatException ex) {
                        ex.printStackTrace();

                    } catch (NullPointerException ex) {
                        ex.printStackTrace();
                    }

                } else if (nl.item(i).getNodeName().equals("Cameras")) {
                    NodeList nl2 = ((Element) nl.item(i)).getChildNodes();
                    for (int j = 0;j < nl2.getLength();j++) {
                        if (nl2.item(j).getNodeName().equals("Camera")) {
                            Element e = (Element) nl2.item(j);

                            double lat = Double.parseDouble(e.getAttribute("lat"));
                            double lon = Double.parseDouble(e.getAttribute("lon"));
                            double alt = Double.parseDouble(e.getAttribute("alt"));

                            Position p = Position.fromDegrees(lat, lon, alt);
                            Image thumb = null;
                            try {
                                byte b[] = Base64.getDecoder().decode(e.getFirstChild().getNodeValue());
                                thumb = Toolkit.getDefaultToolkit().createImage(b);

                            } catch (NullPointerException ex) {
                                //--- No image
                            }
                            Camera c = new Camera(e.getAttribute("title"), p, thumb);
                            cameras.addElement(c);
                        }
                    }

                } else if (nl.item(i).getNodeName().equals("Layers")) {
                    try {
                        //--- Set layers internal frame position
                        Element e = (Element) nl.item(i);
                        int loc = Integer.parseInt(e.getAttribute("mainDividerLocation"));
                        SP_Main.setDividerLocation(loc);
                        SP_Main.setLastDividerLocation(300);

                        loc = Integer.parseInt(e.getAttribute("dividerLocation"));
                        SP_Layers.setDividerLocation(loc);
                        SP_Layers.setLastDividerLocation(250);

                    } catch (NumberFormatException ex) {
                        // ex.printStackTrace();

                    }

                } else if (nl.item(i).getNodeName().equals("WorldWindLayer")) {
                    Element e = (Element) nl.item(i);
                    TinyFactory fac = app.getFactory(e.getAttribute("factory"));
                    if (fac != null) {
                        WWEPlugin p = (WWEPlugin) fac.newPlugin(wwd);
                        if (p != null) {
                            p.setup(app, DP_Main);
                            p.configure(e);
                            Layer l = p.getLayer();
                            l.setName(e.getAttribute("name"));
                            Boolean b = Boolean.valueOf(e.getAttribute("active"));
                            l.setEnabled(b);
                            plugins.put("" + l.hashCode(), p);

                            installLayer(p, -1);

                        } else {
                            System.err.println("(E) WorldWindLayerPlugin could not be instantiated (" + fac.getFactoryName() + ")");

                        }

                    } else {
                        System.err.println("(E) No factory found for WorldWindPluginLayer " + e.getAttribute("factory"));

                    }
                }
            }

        }

        //--- Refresh list last
        layers.fireTableDataChanged();

        timer.start();
    }

    public void destroy() {

        timer.stop();
        vtimer.stop();

        JInternalFrame fr[] = DP_Main.getAllFrames();
        for (int i = 0;i < fr.length;i++) {
            JComponent jcomp = (JComponent) fr[0].getContentPane().getComponent(0);
            fr[0].removeAll();

            //--- If plugin, cleanup
            TinyPlugin p = (TinyPlugin) jcomp.getClientProperty("plugin");
            if (p != null) p.cleanup();

            fr[0].setVisible(false);
            fr[0].dispose();
        }

        m.getLayers().clear();
        InputHandler ih = wwd.getInputHandler();
        ih.removeKeyListener(this);
        ih.removeMouseListener(this);
        ih.removeMouseWheelListener(this);

        //--- Clear plugins
        Iterator<WWEPlugin> it = plugins.values().iterator();
        while (it.hasNext()) {
            WWEPlugin p = it.next();
            JComponent jcomp = p.getConfigComponent();
            if (jcomp != null) PN_LayersData.remove(jcomp);
            p.cleanup();

        }
        plugins.clear();

        listeners.clear();
        remove(wwd);

        wwd.shutdown();
        wwd.destroy();

    }

    /**
     * Store current view position and frame location
     *
     * @param config
     */
    public void save(Element config) {
        if (config == null) return;

        Element e = config.getOwnerDocument().createElement("Layers");
        e.setAttribute("mainDividerLocation", "" + SP_Main.getDividerLocation());
        e.setAttribute("dividerLocation", "" + SP_Layers.getDividerLocation());
        config.appendChild(e);

        e = config.getOwnerDocument().createElement("Eye");
        Position camera = wwd.getView().getEyePosition();
        e.setAttribute("lon", "" + camera.longitude.degrees);
        e.setAttribute("lat", "" + camera.latitude.degrees);
        e.setAttribute("alt", "" + camera.elevation);
        Vec4 center = wwd.getView().getCenterPoint();
        e.setAttribute("cx", "" + center.x);
        e.setAttribute("cy", "" + center.y);
        e.setAttribute("heading", "" + wwd.getView().getHeading().degrees);
        e.setAttribute("pitch", "" + wwd.getView().getPitch().degrees);
        // e.setAttribute("z", "" + camera.elevation);
        config.appendChild(e);

        //--- Add the saved cameras
        e = config.getOwnerDocument().createElement("Cameras");
        for (int i = 0;i < cameras.size();i++) {
            Camera c = cameras.get(i);
            Element e2 = config.getOwnerDocument().createElement("Camera");
            e2.setAttribute("title", c.getTitle());
            e2.setAttribute("lon", "" + c.getEyePosition().longitude.degrees);
            e2.setAttribute("lat", "" + c.getEyePosition().latitude.degrees);
            e2.setAttribute("alt", "" + c.getEyePosition().elevation);

            try {
                //--- Dump the thumbnail as png
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                Image thumb = c.getThumbnail();
                //--- Convert to BufferedImage to be saved
                BufferedImage newImage = new BufferedImage(thumb.getWidth(null), thumb.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = newImage.createGraphics();
                g.drawImage(thumb, 0, 0, null);
                g.dispose();
                ImageIO.write(newImage, "png", bout);
                e2.appendChild(config.getOwnerDocument().createTextNode(Base64.getEncoder().encodeToString(bout.toByteArray())));

            } catch (IOException ex) {
                //---
            }
            e.appendChild(e2);
        }
        config.appendChild(e);

        //--- Get the layer configs
        LayerList ll = m.getLayers();
        for (int i = 0;i < ll.size();i++) {
            Layer l = ll.get(i);
            WWEPlugin p = plugins.get("" + l.hashCode());
            if (p != null) {
                Element wwp = config.getOwnerDocument().createElement("WorldWindLayer");
                wwp.setAttribute("active", "" + l.isEnabled());
                wwp.setAttribute("factory", p.getPluginFactory().getClass().getName());
                wwp.setAttribute("name", l.getName());
                p.saveConfig(wwp);
                config.appendChild(wwp);
            }
        }

    }

    //**************************************************************************
    //*** ActionListener
    //**************************************************************************
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == timer) {
            if (eye != null) {
                wwd.getView().goTo(eye, eye.elevation);
                eye = null;
                //--- Return because the globe could not yet be created
                return;
            }
            Position cam = wwd.getView().getEyePosition();
            if (cam != null) {
                TF_Altitude.setText("" + (int) cam.getElevation());
                TF_Latitude.setText("" + nf.format(cam.getLatitude().degrees));
                TF_Longitude.setText("" + nf.format(cam.getLongitude().degrees));
                PB_Downloading.setIndeterminate(WorldWind.getRetrievalService().hasActiveTasks());
            }
            wwd.redraw();

        } else if (e.getSource() == vtimer) {
            //--- Forward to plugin the motion stop
            Iterator<WWEPlugin> it = plugins.values().iterator();
            while (it.hasNext()) it.next().doAction(WWEPlugin.DO_ACTION_VIEWPORT, null);

        } else if (e.getActionCommand().equals("newPlugin")) {
            JMenuItem ji = (JMenuItem) e.getSource();
            TinyFactory factory = (TinyFactory) ji.getClientProperty("factory");

            //--- If it's a new layer
            if (factory instanceof WWEFactory) {
                int index = TB_Layers.getSelectedRow();
                JComponent jcomp = (JComponent) e.getSource();
                String name = JOptionPane.showInputDialog(this, "Layer name", factory.getFactoryName());
                if (name != null) {
                    WWEPlugin p = (WWEPlugin) factory.newPlugin(wwd);
                    p.setup(app, DP_Main);
                    p.configure(null);
                    Layer l = p.getLayer();
                    l.setName(name);
                    l.setEnabled(true);
                    plugins.put("" + l.hashCode(), p);

                    installLayer(p, index);

                }
                layers.fireTableDataChanged();

            } else {
                TinyPlugin p = factory.newPlugin(null);
                p.setup(app, null);
                p.configure(null);

                JComponent jcomp = p.getVisualComponent();
                jcomp.putClientProperty("plugin", p);
                JInternalFrame iframe = new JInternalFrame(p.getPluginName());
                iframe.setClosable(true);
                iframe.setMaximizable(true);
                iframe.setResizable(true);
                iframe.setIconifiable(true);
                iframe.getContentPane().add(jcomp);
                iframe.addInternalFrameListener(new InternalFrameAdapter() {
                    @Override
                    public void internalFrameClosing(InternalFrameEvent e) {
                        JInternalFrame ifr = e.getInternalFrame();
                        JComponent jcomp = (JComponent) ifr.getContentPane().getComponent(0);
                        ifr.removeAll();

                        TinyPlugin p = (TinyPlugin) jcomp.getClientProperty("plugin");
                        if (p != null) p.cleanup();

                        ifr.setVisible(false);
                        ifr.dispose();
                    }
                });
                DP_Main.add(iframe, JLayeredPane.PALETTE_LAYER);
                iframe.setBounds(100, 100, 320, 240);
                iframe.setVisible(true);
            }

        } else if (e.getActionCommand().equals("hideStatusBar")) {
            PN_Status.setVisible(!MN_HideStatusBar.isSelected());

        } else if (e.getActionCommand().equals("renameLayer")) {
            int index = TB_Layers.getSelectedRow();
            if (index != -1) {
                Layer l = (Layer) TB_Layers.getValueAt(index, 1);
                String newname = JOptionPane.showInputDialog("Layer name", l.getName());
                if (newname != null) l.setName(newname);
            }
            TB_Layers.repaint();

        } else if (e.getActionCommand().equals("removeLayer")) {
            int index = TB_Layers.getSelectedRow();
            if (index != -1) {
                Layer l = (Layer) TB_Layers.getValueAt(index, 1);
                LayerList ll = m.getLayers();
                ll.remove(l);

                WWEPlugin p = plugins.remove("" + l.hashCode());
                if (p != null) {
                    JComponent jcomp = (JComponent) p.getConfigComponent();
                    if (jcomp != null) PN_LayersData.remove(jcomp);
                    JToggleButton jbutton = p.getLayerButton();
                    if (jbutton != null) {
                        PN_LayersButtons.remove(jbutton);
                        btgblayers.remove(jbutton);
                        jbutton.removeActionListener(this);

                    }
                    PN_LayersButtons.revalidate();

                    p.cleanup();

                }

            }
            layers.fireTableDataChanged();

        } else if (e.getActionCommand().equals("collapse")) {
            if (SP_Layers.getDividerLocation() == 1) {
                SP_Layers.setDividerLocation(SP_Layers.getLastDividerLocation());

            } else {
                SP_Layers.setLastDividerLocation(SP_Layers.getDividerLocation());
                SP_Layers.setDividerLocation(1);
            }

        } else if (e.getActionCommand().equals("activeLayer")) {
            WWEPlugin p = (WWEPlugin) ((JToggleButton) e.getSource()).getClientProperty("plugin");

            for (int i = 0;i < TB_Layers.getRowCount();i++) {
                Layer tmp = (Layer) TB_Layers.getValueAt(i, 1);
                if (tmp == p.getLayer()) {
                    TB_Layers.getSelectionModel().addSelectionInterval(i, i);
                    break;
                }
            }

        } else if (e.getActionCommand().equals("more")) {
            PU_More.show(BT_More, 10, 10);

        } else if (e.getActionCommand().equals("screenIdentifier")) {
            App.showScreenIdentifiers();

        } else if (e.getActionCommand().equals("sticky")) {
            boolean sticky = BT_Sticky.isSelected();
            if (sticky) {
                int old = SP_Main.getLastDividerLocation();
                SP_Main.setDividerLocation(old);
                SP_Main.setDividerSize(10);

            } else {
                int loc = SP_Main.getDividerLocation();
                SP_Main.setLastDividerLocation(loc);
                SP_Main.setDividerLocation(0);
                SP_Main.setDividerSize(0);
            }

        } else if (e.getActionCommand().equals("fullscreen")) {
            if (MN_Fullscreen.isSelected()) {
                //--- Try to find the selected graphics device
                Enumeration<AbstractButton> buttons = btgscreens.getElements();
                while (buttons.hasMoreElements()) {
                    JRadioButtonMenuItem jitem = (JRadioButtonMenuItem) buttons.nextElement();
                    if (jitem.isSelected()) {
                        remove(SP_Main);
                        revalidate();
                        repaint();

                        GraphicsDevice gd = (GraphicsDevice) jitem.getClientProperty("gd");
                        DisplayMode mode = gd.getDisplayMode();
                        jframe = new JFrame("", gd.getDefaultConfiguration());
                        jframe.add(SP_Main);
                        jframe.setSize(mode.getWidth(), mode.getHeight());
                        jframe.setUndecorated(true);
                        jframe.setVisible(true);
                        gd.setFullScreenWindow(jframe);
                        break;
                    }
                }

            } else {
                jframe.setVisible(false);
                jframe.remove(SP_Main);
                jframe.dispose();
                jframe = null;

                add(SP_Main, BorderLayout.CENTER);
                revalidate();
                repaint();
            }

        } else if (e.getActionCommand().equals("scrollRight") || e.getActionCommand().equals("scrollLeft")) {
            Rectangle rect = PN_LayersButtons.getVisibleRect();
            rect.x = rect.x + (e.getActionCommand().equals("scrollRight") ? 22 : -22);
            rect.y = rect.y;
            PN_LayersButtons.scrollRectToVisible(rect);

        } else if (e.getActionCommand().equals("addLayer")) {
            MN_NewLayers.setEnabled(true);
            PU_Layers.show(BT_AddLayer, BT_AddLayer.getX(), BT_AddLayer.getY());

        } else if (e.getActionCommand().equals("upLayer")) {
            int index = TB_Layers.getSelectedRow();
            if (index == -1) return;

            LayerList ll = m.getLayers();
            if (index > 0) {
                Layer l = (Layer) TB_Layers.getValueAt(index, 1);
                ll.moveLower(l);
                layers.fireTableDataChanged();
                TB_Layers.getSelectionModel().setSelectionInterval(index - 1, index - 1);
            }

        } else if (e.getActionCommand().equals("downLayer")) {
            //--- Move down
            int index = TB_Layers.getSelectedRow();
            if (index == -1) return;

            LayerList ll = m.getLayers();
            if (index < TB_Layers.getRowCount() - 1) {
                Layer l = (Layer) TB_Layers.getValueAt(index, 1);
                ll.moveHigher(l);
                layers.fireTableDataChanged();
                TB_Layers.getSelectionModel().setSelectionInterval(index + 1, index + 1);

            }

        } else if (e.getActionCommand().equals("cameras")) {
            PN_Cameras.setVisible(BT_Cameras.isSelected());

        } else if (e.getActionCommand().equals("newCamera")) {
            String title = JOptionPane.showInputDialog(PN_Cameras, "Camera title");
            if (title != null) {
                Camera c = new Camera(title, wwd);
                cameras.addElement(c);

            }

        } else if (e.getActionCommand().equals("removeCamera")) {
            int index = LI_Cameras.getSelectedIndex();
            if (index > -1) cameras.remove(index);

        } else if (e.getActionCommand().equals("updateCamera")) {
            int index = LI_Cameras.getSelectedIndex();
            if (index > -1) {
                Camera c = LI_Cameras.getSelectedValue();
                String title = JOptionPane.showInputDialog(PN_Cameras, "Update title and position", c.getTitle());
                if (title != null) c.set(title, wwd);
            }

        } else if (e.getActionCommand().equals("configure")) {
            JComponent jcomp = (JComponent) e.getSource();
            TinyPlugin p = (TinyPlugin) jcomp.getClientProperty("plugin");
            if (p != null) {
                TinyFactory f = p.getPluginFactory();
                // app.get// .select(f);
                
            }
        }

    }

    //**************************************************************************
    //*** KeyListener
    //**************************************************************************
    @Override
    public void keyTyped(KeyEvent e) {
        //---
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // System.out.println("e:" + e.getKeyCode());
        //---
        if (e.getKeyCode() == KeyEvent.VK_F) {
            boolean selected = MN_Fullscreen.isSelected();
            MN_Fullscreen.setSelected(!selected);
            ActionEvent ae = new ActionEvent(MN_Fullscreen, ActionEvent.ACTION_PERFORMED, MN_Fullscreen.getActionCommand());
            actionPerformed(ae);

        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            boolean sticky = BT_Sticky.isSelected();
            BT_Sticky.setSelected(!sticky);

            actionPerformed(new ActionEvent(BT_Sticky, ActionEvent.ACTION_PERFORMED, BT_Sticky.getActionCommand()));

        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        //---
    }

    //**************************************************************************
    //*** ComponentListener
    //**************************************************************************
    @Override
    public void componentResized(ComponentEvent e) {
        //--- Resize the world wind panel
        wwd.setBounds(0, 0, DP_Main.getWidth(), DP_Main.getHeight());
        // LB_Licence.setBounds(0,DP_Main.getHeight()-LB_Licence.getHeight(), DP_Main.getWidth(), LB_Licence.getHeight());

        //--- Replace the cameras panel
        PN_Cameras.setBounds(DP_Main.getWidth() - 320, 0, 320, DP_Main.getHeight());

        //--- Forward to plugin the resize
        Iterator<WWEPlugin> it = plugins.values().iterator();
        while (it.hasNext()) it.next().doAction(WWEPlugin.DO_ACTION_NEWSIZE, getSize());
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        //---
    }

    @Override
    public void componentShown(ComponentEvent e) {
        //---
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        //---
    }

    //**************************************************************************
    //*** ListSelectionListener
    //*************************************************************************
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() == TB_Layers.getSelectionModel()) {
            if (e.getValueIsAdjusting() == true) return;

            int index = TB_Layers.getSelectedRow();
            if (index != -1) {
                CardLayout layout = (CardLayout) PN_LayersData.getLayout();
                Layer l = (Layer) TB_Layers.getValueAt(index, 1);
                LB_Layer.setText(l.getName());
                
                //--- Display config
                layout.show(PN_LayersData, "empty");
                layout.show(PN_LayersData, "" + l.hashCode());

                //--- Choose button if present
                btgblayers.clearSelection();
                WWEPlugin p = plugins.get("" + l.hashCode());
                if (p != null) {
                    BT_Configure.setVisible(p.getPluginFactory().getFactoryConfigComponent()!=null);
                    BT_Configure.putClientProperty("plugin", p);
                    
                    LB_LayerIcon.setIcon(p.getPluginFactory().getFactoryIcon(TinyFactory.ICON_SIZE_NORMAL));

                    JToggleButton jb = p.getLayerButton();
                    if (jb != null) jb.setSelected(true);

                    //--- Find licence
                    String licence = (String) p.getPluginFactory().getProperty(TinyFactory.PROPERTY_LICENCE_TEXT);
                    if (licence != null) {
                        LB_Licence.setText(licence);
                        LB_Licence.setIcon(p.getPluginFactory().getFactoryIcon(TinyFactory.ICON_SIZE_NORMAL));
                    }

                } else {
                    //--- Worldwind layer ins not managed by WWE
                    BT_Configure.setVisible(false);
                    LB_LayerIcon.setIcon(null);
                }

                /*
                btgblayers.clearSelection();
                Enumeration<AbstractButton> en = btgblayers.getElements();
                while (en.hasMoreElements()) {
                    AbstractButton bt = en.nextElement();
                    WorldWindLayerPlugin tmp = (WorldWindLayerPlugin) bt.getClientProperty("plugin");
                    if (tmp.getLayer() == l) {
                        bt.setSelected(true);
                        break;

                    }
                }
                 */
            }

        }
    }

    //**************************************************************************
    //*** MouseListener
    //**************************************************************************
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getSource() == LI_Cameras) {
            if (e.getClickCount() >= 2) {
                Camera c = LI_Cameras.getSelectedValue();
                if (c != null) wwd.getView().goTo(c.getEyePosition(), c.getEyePosition().elevation);

            }
        }

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getSource() == wwd) {
            if (e.isPopupTrigger() && e.getClickCount() == 2) {
                PU_More.show(DP_Main, e.getX(), e.getY());
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getSource() == wwd) {
            vtimer.restart();

            if (e.isPopupTrigger() && e.getClickCount() == 2) {
                PU_More.show(DP_Main, e.getX(), e.getY());
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        //---
    }

    @Override
    public void mouseExited(MouseEvent e) {
        //---
    }

    //**************************************************************************
    //*** MouseWheel
    //**************************************************************************
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        vtimer.restart();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btgblayers = new javax.swing.ButtonGroup();
        PU_Layers = new javax.swing.JPopupMenu();
        MN_NewLayers = new javax.swing.JMenu();
        jSeparator7 = new javax.swing.JSeparator();
        MN_Rename = new javax.swing.JMenuItem();
        MN_RemoveLayer = new javax.swing.JMenuItem();
        PU_More = new javax.swing.JPopupMenu();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        MN_HideStatusBar = new javax.swing.JCheckBoxMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        MN_Screens = new javax.swing.JMenu();
        MN_Fullscreen = new javax.swing.JCheckBoxMenuItem();
        MN_ScreenIdentifier = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JSeparator();
        btgscreens = new javax.swing.ButtonGroup();
        btgmodel = new javax.swing.ButtonGroup();
        SP_Main = new javax.swing.JSplitPane();
        jPanel5 = new javax.swing.JPanel();
        PN_LayersTop = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        BT_Collapse = new javax.swing.JButton();
        SP_Layers = new javax.swing.JSplitPane();
        PN_LayersFrame = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        TB_Tools = new javax.swing.JToolBar();
        BT_AddLayer = new javax.swing.JButton();
        BT_LayerUp = new javax.swing.JButton();
        BT_LayerDown = new javax.swing.JButton();
        BT_RenameLayer = new javax.swing.JButton();
        jSeparator9 = new javax.swing.JToolBar.Separator();
        BT_RemoveLayer = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        TB_Layers = new javax.swing.JTable();
        PN_LayersCenter = new javax.swing.JPanel();
        PN_LayersData = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        LB_LayerIcon = new javax.swing.JLabel();
        LB_Layer = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        BT_Configure = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        PN_Status = new javax.swing.JPanel();
        PB_Downloading = new javax.swing.JProgressBar();
        jlabel1 = new javax.swing.JLabel();
        TF_Altitude = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jlabel2 = new javax.swing.JLabel();
        TF_Latitude = new javax.swing.JTextField();
        TF_Longitude = new javax.swing.JTextField();
        LB_Licence = new javax.swing.JLabel();
        DP_Main = new javax.swing.JDesktopPane();
        PN_Cameras = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        LI_Cameras = new javax.swing.JList<>();
        TB_CameraTools = new javax.swing.JToolBar();
        BT_NewCamera = new javax.swing.JButton();
        BT_CameraUp = new javax.swing.JButton();
        BT_CameraDown = new javax.swing.JButton();
        BT_UpdateCamera = new javax.swing.JButton();
        jSeparator13 = new javax.swing.JToolBar.Separator();
        BT_RemoveCamera = new javax.swing.JButton();
        PN_Topbar = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        SP_LayersButtons = new javax.swing.JScrollPane();
        PN_LayersButtons = new javax.swing.JPanel();
        BT_ScrollLeft = new javax.swing.JButton();
        BT_ScrollRight = new javax.swing.JButton();
        PN_Tray = new javax.swing.JPanel();
        BT_Cameras = new javax.swing.JToggleButton();
        jPanel7 = new javax.swing.JPanel();
        BT_More = new javax.swing.JButton();
        BT_Sticky = new javax.swing.JToggleButton();

        MN_NewLayers.setText("New layer");
        PU_Layers.add(MN_NewLayers);
        PU_Layers.add(jSeparator7);

        MN_Rename.setText("Rename");
        MN_Rename.setActionCommand("renameLayer");
        PU_Layers.add(MN_Rename);

        MN_RemoveLayer.setText("Remove");
        MN_RemoveLayer.setActionCommand("removeLayer");
        PU_Layers.add(MN_RemoveLayer);

        PU_More.add(jSeparator10);

        MN_HideStatusBar.setText("Hide status bar");
        MN_HideStatusBar.setActionCommand("hideStatusBar");
        PU_More.add(MN_HideStatusBar);
        PU_More.add(jSeparator11);

        MN_Screens.setText("Screens");

        MN_Fullscreen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, 0));
        MN_Fullscreen.setText("Fullscreen");
        MN_Fullscreen.setActionCommand("fullscreen");
        MN_Screens.add(MN_Fullscreen);

        MN_ScreenIdentifier.setText("Screen identifier");
        MN_ScreenIdentifier.setActionCommand("screenIdentifier");
        MN_Screens.add(MN_ScreenIdentifier);
        MN_Screens.add(jSeparator6);

        PU_More.add(MN_Screens);

        setLayout(new java.awt.BorderLayout());

        SP_Main.setDividerLocation(300);

        jPanel5.setLayout(new java.awt.BorderLayout());

        PN_LayersTop.setMaximumSize(new java.awt.Dimension(2147483647, 32));
        PN_LayersTop.setLayout(new java.awt.BorderLayout());

        jPanel2.setOpaque(false);

        BT_Collapse.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        BT_Collapse.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/collapse.png"))); // NOI18N
        BT_Collapse.setActionCommand("collapse");
        BT_Collapse.setPreferredSize(new java.awt.Dimension(32, 32));
        jPanel2.add(BT_Collapse);

        PN_LayersTop.add(jPanel2, java.awt.BorderLayout.WEST);

        jPanel5.add(PN_LayersTop, java.awt.BorderLayout.NORTH);

        SP_Layers.setDividerLocation(200);
        SP_Layers.setDividerSize(5);
        SP_Layers.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        PN_LayersFrame.setMinimumSize(new java.awt.Dimension(135, 0));
        PN_LayersFrame.setLayout(new java.awt.BorderLayout());

        jPanel4.setLayout(new java.awt.BorderLayout());

        TB_Tools.setBorder(null);
        TB_Tools.setFloatable(false);

        BT_AddLayer.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        BT_AddLayer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/add.png"))); // NOI18N
        BT_AddLayer.setToolTipText("new layer");
        BT_AddLayer.setActionCommand("addLayer");
        BT_AddLayer.setBorderPainted(false);
        BT_AddLayer.setFocusPainted(false);
        BT_AddLayer.setFocusable(false);
        BT_AddLayer.setPreferredSize(new java.awt.Dimension(26, 26));
        TB_Tools.add(BT_AddLayer);

        BT_LayerUp.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        BT_LayerUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/up.png"))); // NOI18N
        BT_LayerUp.setToolTipText("move layer up");
        BT_LayerUp.setActionCommand("upLayer");
        BT_LayerUp.setBorderPainted(false);
        BT_LayerUp.setFocusPainted(false);
        BT_LayerUp.setFocusable(false);
        BT_LayerUp.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        BT_LayerUp.setPreferredSize(new java.awt.Dimension(26, 26));
        TB_Tools.add(BT_LayerUp);

        BT_LayerDown.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        BT_LayerDown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/down.png"))); // NOI18N
        BT_LayerDown.setToolTipText("move layer down");
        BT_LayerDown.setActionCommand("downLayer");
        BT_LayerDown.setBorderPainted(false);
        BT_LayerDown.setFocusPainted(false);
        BT_LayerDown.setFocusable(false);
        BT_LayerDown.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        BT_LayerDown.setPreferredSize(new java.awt.Dimension(26, 26));
        TB_Tools.add(BT_LayerDown);

        BT_RenameLayer.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        BT_RenameLayer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/edit.png"))); // NOI18N
        BT_RenameLayer.setToolTipText("rename layer");
        BT_RenameLayer.setActionCommand("renameLayer");
        BT_RenameLayer.setBorderPainted(false);
        BT_RenameLayer.setFocusPainted(false);
        BT_RenameLayer.setFocusable(false);
        BT_RenameLayer.setPreferredSize(new java.awt.Dimension(26, 26));
        TB_Tools.add(BT_RenameLayer);
        TB_Tools.add(jSeparator9);

        BT_RemoveLayer.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        BT_RemoveLayer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/remove.png"))); // NOI18N
        BT_RemoveLayer.setToolTipText("delete layer");
        BT_RemoveLayer.setActionCommand("removeLayer");
        BT_RemoveLayer.setBorderPainted(false);
        BT_RemoveLayer.setFocusPainted(false);
        BT_RemoveLayer.setFocusable(false);
        BT_RemoveLayer.setPreferredSize(new java.awt.Dimension(26, 26));
        TB_Tools.add(BT_RemoveLayer);

        jPanel4.add(TB_Tools, java.awt.BorderLayout.PAGE_START);

        PN_LayersFrame.add(jPanel4, java.awt.BorderLayout.NORTH);

        TB_Layers.setRowHeight(26);
        jScrollPane2.setViewportView(TB_Layers);

        PN_LayersFrame.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        SP_Layers.setLeftComponent(PN_LayersFrame);

        PN_LayersCenter.setMinimumSize(new java.awt.Dimension(19, 0));
        PN_LayersCenter.setLayout(new java.awt.BorderLayout());

        PN_LayersData.setMinimumSize(new java.awt.Dimension(10, 0));
        PN_LayersData.setLayout(new java.awt.CardLayout());
        PN_LayersData.add(jPanel3, "empty");

        PN_LayersCenter.add(PN_LayersData, java.awt.BorderLayout.CENTER);

        jPanel8.setBackground(new java.awt.Color(144, 202, 249));
        jPanel8.setLayout(new java.awt.BorderLayout());

        jPanel11.setBackground(new java.awt.Color(144, 202, 249));
        jPanel11.setMaximumSize(new java.awt.Dimension(32767, 24));
        jPanel11.setOpaque(false);
        jPanel11.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        LB_LayerIcon.setFont(new java.awt.Font("Arial", 1, 11)); // NOI18N
        LB_LayerIcon.setForeground(new java.awt.Color(52, 73, 93));
        LB_LayerIcon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LB_LayerIcon.setPreferredSize(new java.awt.Dimension(32, 32));
        jPanel11.add(LB_LayerIcon);

        LB_Layer.setFont(new java.awt.Font("Arial", 1, 11)); // NOI18N
        LB_Layer.setForeground(new java.awt.Color(52, 73, 93));
        LB_Layer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LB_Layer.setText("...");
        jPanel11.add(LB_Layer);

        jPanel8.add(jPanel11, java.awt.BorderLayout.CENTER);

        jPanel9.setOpaque(false);

        BT_Configure.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        BT_Configure.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/configure.png"))); // NOI18N
        BT_Configure.setToolTipText("Configure");
        BT_Configure.setActionCommand("configure");
        BT_Configure.setPreferredSize(new java.awt.Dimension(32, 32));
        jPanel9.add(BT_Configure);

        jPanel8.add(jPanel9, java.awt.BorderLayout.EAST);

        PN_LayersCenter.add(jPanel8, java.awt.BorderLayout.NORTH);

        SP_Layers.setRightComponent(PN_LayersCenter);

        jPanel5.add(SP_Layers, java.awt.BorderLayout.CENTER);

        SP_Main.setLeftComponent(jPanel5);

        jPanel6.setLayout(new java.awt.BorderLayout());

        PN_Status.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        PB_Downloading.setBackground(java.awt.Color.lightGray);
        PB_Downloading.setForeground(java.awt.Color.red);
        PB_Downloading.setToolTipText("Downloading");
        PB_Downloading.setIndeterminate(true);
        PB_Downloading.setPreferredSize(new java.awt.Dimension(50, 24));
        PN_Status.add(PB_Downloading);

        jlabel1.setText("Alt [m]");
        jlabel1.setToolTipText("Altitude");
        PN_Status.add(jlabel1);

        TF_Altitude.setEditable(false);
        TF_Altitude.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        TF_Altitude.setPreferredSize(new java.awt.Dimension(100, 26));
        PN_Status.add(TF_Altitude);

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator1.setPreferredSize(new java.awt.Dimension(18, 18));
        PN_Status.add(jSeparator1);

        jlabel2.setText("Lat / Long [°]");
        jlabel2.setToolTipText("Latitude / Longitude");
        PN_Status.add(jlabel2);

        TF_Latitude.setEditable(false);
        TF_Latitude.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        TF_Latitude.setPreferredSize(new java.awt.Dimension(100, 26));
        PN_Status.add(TF_Latitude);

        TF_Longitude.setEditable(false);
        TF_Longitude.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        TF_Longitude.setPreferredSize(new java.awt.Dimension(100, 26));
        PN_Status.add(TF_Longitude);

        LB_Licence.setText("...");
        PN_Status.add(LB_Licence);

        jPanel6.add(PN_Status, java.awt.BorderLayout.SOUTH);

        PN_Cameras.setLayout(new java.awt.BorderLayout());

        LI_Cameras.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(LI_Cameras);

        PN_Cameras.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        TB_CameraTools.setBorder(null);
        TB_CameraTools.setFloatable(false);

        BT_NewCamera.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        BT_NewCamera.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/add.png"))); // NOI18N
        BT_NewCamera.setToolTipText("New camera");
        BT_NewCamera.setActionCommand("newCamera");
        BT_NewCamera.setBorderPainted(false);
        BT_NewCamera.setFocusPainted(false);
        BT_NewCamera.setFocusable(false);
        BT_NewCamera.setPreferredSize(new java.awt.Dimension(26, 26));
        TB_CameraTools.add(BT_NewCamera);

        BT_CameraUp.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        BT_CameraUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/up.png"))); // NOI18N
        BT_CameraUp.setToolTipText("move layer up");
        BT_CameraUp.setActionCommand("upLayer");
        BT_CameraUp.setBorderPainted(false);
        BT_CameraUp.setFocusPainted(false);
        BT_CameraUp.setFocusable(false);
        BT_CameraUp.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        BT_CameraUp.setPreferredSize(new java.awt.Dimension(26, 26));
        TB_CameraTools.add(BT_CameraUp);

        BT_CameraDown.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        BT_CameraDown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/down.png"))); // NOI18N
        BT_CameraDown.setToolTipText("move layer down");
        BT_CameraDown.setActionCommand("downLayer");
        BT_CameraDown.setBorderPainted(false);
        BT_CameraDown.setFocusPainted(false);
        BT_CameraDown.setFocusable(false);
        BT_CameraDown.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        BT_CameraDown.setPreferredSize(new java.awt.Dimension(26, 26));
        TB_CameraTools.add(BT_CameraDown);

        BT_UpdateCamera.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        BT_UpdateCamera.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/edit.png"))); // NOI18N
        BT_UpdateCamera.setToolTipText("Update camera title and position");
        BT_UpdateCamera.setActionCommand("updateCamera");
        BT_UpdateCamera.setBorderPainted(false);
        BT_UpdateCamera.setFocusPainted(false);
        BT_UpdateCamera.setFocusable(false);
        BT_UpdateCamera.setPreferredSize(new java.awt.Dimension(26, 26));
        TB_CameraTools.add(BT_UpdateCamera);

        jSeparator13.setSeparatorSize(new java.awt.Dimension(10, 20));
        TB_CameraTools.add(jSeparator13);

        BT_RemoveCamera.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        BT_RemoveCamera.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/remove.png"))); // NOI18N
        BT_RemoveCamera.setToolTipText("Remove camera");
        BT_RemoveCamera.setActionCommand("removeCamera");
        BT_RemoveCamera.setBorderPainted(false);
        BT_RemoveCamera.setFocusPainted(false);
        BT_RemoveCamera.setFocusable(false);
        BT_RemoveCamera.setPreferredSize(new java.awt.Dimension(26, 26));
        TB_CameraTools.add(BT_RemoveCamera);

        PN_Cameras.add(TB_CameraTools, java.awt.BorderLayout.PAGE_START);

        DP_Main.setLayer(PN_Cameras, javax.swing.JLayeredPane.PALETTE_LAYER);
        DP_Main.add(PN_Cameras);
        PN_Cameras.setBounds(420, 0, 200, 480);

        jPanel6.add(DP_Main, java.awt.BorderLayout.CENTER);

        PN_Topbar.setLayout(new java.awt.BorderLayout());

        jPanel1.setOpaque(false);
        jPanel1.setLayout(new java.awt.BorderLayout());

        SP_LayersButtons.setBorder(null);
        SP_LayersButtons.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        SP_LayersButtons.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        SP_LayersButtons.setAutoscrolls(true);
        SP_LayersButtons.setHorizontalScrollBar(null);

        PN_LayersButtons.setAutoscrolls(true);
        PN_LayersButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        SP_LayersButtons.setViewportView(PN_LayersButtons);

        jPanel1.add(SP_LayersButtons, java.awt.BorderLayout.CENTER);

        BT_ScrollLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/previous.png"))); // NOI18N
        BT_ScrollLeft.setActionCommand("scrollLeft");
        BT_ScrollLeft.setBorderPainted(false);
        BT_ScrollLeft.setContentAreaFilled(false);
        BT_ScrollLeft.setMargin(new java.awt.Insets(2, 2, 2, 2));
        BT_ScrollLeft.setMaximumSize(new java.awt.Dimension(22, 22));
        BT_ScrollLeft.setMinimumSize(new java.awt.Dimension(22, 22));
        BT_ScrollLeft.setPreferredSize(new java.awt.Dimension(22, 22));
        jPanel1.add(BT_ScrollLeft, java.awt.BorderLayout.WEST);

        BT_ScrollRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/next.png"))); // NOI18N
        BT_ScrollRight.setToolTipText("");
        BT_ScrollRight.setActionCommand("scrollRight");
        BT_ScrollRight.setBorderPainted(false);
        BT_ScrollRight.setContentAreaFilled(false);
        BT_ScrollRight.setMargin(new java.awt.Insets(2, 2, 2, 2));
        BT_ScrollRight.setMaximumSize(new java.awt.Dimension(22, 22));
        BT_ScrollRight.setMinimumSize(new java.awt.Dimension(22, 22));
        BT_ScrollRight.setPreferredSize(new java.awt.Dimension(22, 22));
        jPanel1.add(BT_ScrollRight, java.awt.BorderLayout.EAST);

        PN_Topbar.add(jPanel1, java.awt.BorderLayout.CENTER);

        PN_Tray.setOpaque(false);
        PN_Tray.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        BT_Cameras.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/components/Resources/Icons/22x22/camera.png"))); // NOI18N
        BT_Cameras.setActionCommand("cameras");
        BT_Cameras.setPreferredSize(new java.awt.Dimension(32, 32));
        PN_Tray.add(BT_Cameras);

        PN_Topbar.add(PN_Tray, java.awt.BorderLayout.EAST);

        BT_More.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        BT_More.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/down.png"))); // NOI18N
        BT_More.setActionCommand("more");
        BT_More.setPreferredSize(new java.awt.Dimension(32, 32));
        jPanel7.add(BT_More);

        BT_Sticky.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/Resources/Icons/sticky.png"))); // NOI18N
        BT_Sticky.setSelected(true);
        BT_Sticky.setActionCommand("sticky");
        BT_Sticky.setPreferredSize(new java.awt.Dimension(32, 32));
        jPanel7.add(BT_Sticky);

        PN_Topbar.add(jPanel7, java.awt.BorderLayout.WEST);

        jPanel6.add(PN_Topbar, java.awt.BorderLayout.PAGE_START);

        SP_Main.setRightComponent(jPanel6);

        add(SP_Main, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BT_AddLayer;
    private javax.swing.JButton BT_CameraDown;
    private javax.swing.JButton BT_CameraUp;
    private javax.swing.JToggleButton BT_Cameras;
    private javax.swing.JButton BT_Collapse;
    private javax.swing.JButton BT_Configure;
    private javax.swing.JButton BT_LayerDown;
    private javax.swing.JButton BT_LayerUp;
    private javax.swing.JButton BT_More;
    private javax.swing.JButton BT_NewCamera;
    private javax.swing.JButton BT_RemoveCamera;
    private javax.swing.JButton BT_RemoveLayer;
    private javax.swing.JButton BT_RenameLayer;
    private javax.swing.JButton BT_ScrollLeft;
    private javax.swing.JButton BT_ScrollRight;
    private javax.swing.JToggleButton BT_Sticky;
    private javax.swing.JButton BT_UpdateCamera;
    private javax.swing.JDesktopPane DP_Main;
    private javax.swing.JLabel LB_Layer;
    private javax.swing.JLabel LB_LayerIcon;
    private javax.swing.JLabel LB_Licence;
    private javax.swing.JList<Camera> LI_Cameras;
    private javax.swing.JCheckBoxMenuItem MN_Fullscreen;
    private javax.swing.JCheckBoxMenuItem MN_HideStatusBar;
    private javax.swing.JMenu MN_NewLayers;
    private javax.swing.JMenuItem MN_RemoveLayer;
    private javax.swing.JMenuItem MN_Rename;
    private javax.swing.JMenuItem MN_ScreenIdentifier;
    private javax.swing.JMenu MN_Screens;
    private javax.swing.JProgressBar PB_Downloading;
    private javax.swing.JPanel PN_Cameras;
    private javax.swing.JPanel PN_LayersButtons;
    private javax.swing.JPanel PN_LayersCenter;
    private javax.swing.JPanel PN_LayersData;
    private javax.swing.JPanel PN_LayersFrame;
    private javax.swing.JPanel PN_LayersTop;
    private javax.swing.JPanel PN_Status;
    private javax.swing.JPanel PN_Topbar;
    private javax.swing.JPanel PN_Tray;
    private javax.swing.JPopupMenu PU_Layers;
    private javax.swing.JPopupMenu PU_More;
    private javax.swing.JSplitPane SP_Layers;
    private javax.swing.JScrollPane SP_LayersButtons;
    private javax.swing.JSplitPane SP_Main;
    private javax.swing.JToolBar TB_CameraTools;
    private javax.swing.JTable TB_Layers;
    private javax.swing.JToolBar TB_Tools;
    private javax.swing.JTextField TF_Altitude;
    private javax.swing.JTextField TF_Latitude;
    private javax.swing.JTextField TF_Longitude;
    private javax.swing.ButtonGroup btgblayers;
    private javax.swing.ButtonGroup btgmodel;
    private javax.swing.ButtonGroup btgscreens;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JToolBar.Separator jSeparator13;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator9;
    private javax.swing.JLabel jlabel1;
    private javax.swing.JLabel jlabel2;
    // End of variables declaration//GEN-END:variables

    //**************************************************************************
    //*** Private
    //**************************************************************************
    /**
     * Add the layer to ww panels, also create the button<p>
     *
     * @param p
     * @param index
     */
    private void installLayer(WWEPlugin p, int index) {
        //--- Add layer
        LayerList ll = m.getLayers();
        Layer l = p.getLayer();
        if (index == -1) {
            ll.add(l);

        } else {
            ll.add(index, l);
        }
        // System.out.println("INSTALL LAYER:" + l + " active:" + l.isEnabled() + " class:" + l.getClass().getName());

        //--- Store the hash code of the layer to show the layer config panel
        JComponent jcomp = (JComponent) p.getConfigComponent();
        if (jcomp != null) PN_LayersData.add(jcomp, "" + l.hashCode());

        JToggleButton jbutton = p.getLayerButton();
        if (jbutton != null) {
            jbutton.setActionCommand("activeLayer");
            jbutton.putClientProperty("plugin", p);
            jbutton.addActionListener(this);
            jbutton.setToolTipText(l.getName());
            // jbutton.setText("");
            // jbutton.setPreferredSize(new Dimension(22, 22));
            btgblayers.add(jbutton);
            PN_LayersButtons.add(jbutton);

        }

    }

    /*
    private class ImmovableDesktopManager extends DefaultDesktopManager {

        @Override
        public void dragFrame(JComponent f, int x, int y) {
            super.dragFrame(f, x, y);

        }
    }
     */
    //**************************************************************************
    //*** For debug
    //**************************************************************************
}
