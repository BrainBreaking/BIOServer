/*
 * BBSClientView.java
 */
package com.bbs.client;

import com.bbs.client.ws.BBS;
import com.bbs.client.ws.BBS_Service;
import com.bbs.client.ws.Match;
import com.bbs.client.ws.MatchRESULT;
import com.bbs.client.ws.TFingerprint;
import com.bbs.client.ws.TPerson;
import com.bbs.model.Configuration;
import com.bbs.model.Fingerprint;
import com.bbs.model.FingerprintFormat;
import com.bbs.model.LogManager;
import com.bbs.model.Person;
import com.bbs.neuro.Devices;
import com.bbs.neuro.NeuroManager;
import com.bbs.neuro.fingers.Device;
import com.bbs.neuro.fingers.Scenario;
import com.bbs.neuro.util.BackgroundWorker;
import com.bbs.neuro.util.FingerRecord;
import com.neurotec.biometrics.NFPosition;
import com.neurotec.biometrics.NFTemplate;
import com.neurotec.biometrics.NTemplate;
import com.neurotec.biometrics.standards.BDIFStandard;
import com.neurotec.biometrics.standards.FMRecord;
import com.neurotec.devices.NDevice;
import com.neurotec.devices.NDeviceManager;
import com.neurotec.devices.NFScanner;
import com.neurotec.devices.event.NDeviceEvent;
import com.neurotec.devices.event.NDevicesChangeListener;
import com.neurotec.devices.event.NFScannerPreviewEvent;
import com.neurotec.devices.event.NFScannerPreviewListener;
import com.neurotec.swing.NFView;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.WebServiceException;
import org.jdesktop.application.ApplicationActionMap;
import org.jdesktop.application.Task;

/**
 * The application's main frame.
 */
public class BBSClientView extends FrameView implements NDevicesChangeListener, NFScannerPreviewListener, PropertyChangeListener {

    public static URL BBS_WSDL_LOCATION;
    private TreeMap<String, Person> applicantList = new TreeMap<String, Person>();
    private TreeMap<String, Person> candidatetList = new TreeMap<String, Person>();
    private BCLFormat format = BCLFormat.BCL_ISO_19794_2;
    private CSVProcessor csvProcessor = null;
    private BIOProcessor bioProcessor = null;
    private boolean verified = false;
    private BackgroundWorker backgroundWorker;
    private NFView view;
    //private HandSegmentSelector fingerSelector;
    private Scenario scenario;
    private MatchMODE matchMODE = MatchMODE.FRACTION;
    private MatchMODE liveMODE = MatchMODE.LOCAL;
    private ApplicationActionMap actionMap;
    private ResourceMap resourceMap;
    private List<TFingerprint> applicantTemplates = new ArrayList<TFingerprint>();
    private BBSLogConsole console = null;
    private BBSLogin login = null;
    private StringBuffer consultaReport = new StringBuffer();
    private LogManager consultaLog = null;
    private LogManager liveLog = null;
    private TestTimer timerAlpha = null;
    private TestTimer timerBio = null;

    static {
        URL url = null;
        WebServiceException e = null;

        try {
            String bbsWSDL = Configuration.getProperty("BBS.wsdl", "http://localhost:8080/BBS/BBS?wsdl");
            url = new URL(bbsWSDL);
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        BBS_WSDL_LOCATION = url;

    }

    public enum MatchMODE {

        LOCAL,
        FRACTION,
        REMOTE
    }

    public BBSClientView(SingleFrameApplication app) {
        super(app);

        initComponents();
        postInitComponents();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new javax.swing.Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new javax.swing.Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = BBSClientApp.getApplication().getMainFrame();
            aboutBox = new BBSClientAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        BBSClientApp.getApplication().show(aboutBox);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jPanel9 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        jProgressBar1 = new javax.swing.JProgressBar();
        jButton1 = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jTextField2 = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jProgressBar2 = new javax.swing.JProgressBar();
        jButton13 = new javax.swing.JButton();
        jPanel12 = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        TAlpha = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        TBio = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        TError = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        TTimeCargue = new javax.swing.JLabel();
        jButton14 = new javax.swing.JButton();
        jButton15 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        TCotejos = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        THits = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        TNoHits = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        TUnknow = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        TNotFound = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        TTime = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jPanel11 = new javax.swing.JPanel();
        jTextField4 = new javax.swing.JTextField();
        jButton6 = new javax.swing.JButton();
        jProgressBar3 = new javax.swing.JProgressBar();
        jButton7 = new javax.swing.JButton();
        jPanel13 = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel16 = new javax.swing.JPanel();
        jProgressBar4 = new javax.swing.JProgressBar();
        jLabel6 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        jButton16 = new javax.swing.JButton();
        jSpinner1 = new javax.swing.JSpinner();
        jLabel53 = new javax.swing.JLabel();
        jSpinner3 = new javax.swing.JSpinner();
        jSpinner4 = new javax.swing.JSpinner();
        jLabel54 = new javax.swing.JLabel();
        jLabel55 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jButton22 = new javax.swing.JButton();
        jPanel15 = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        jLabel34 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        jLabel37 = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        jLabel40 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jProgressBar5 = new javax.swing.JProgressBar();
        jLabel44 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jButton17 = new javax.swing.JButton();
        jSpinner2 = new javax.swing.JSpinner();
        jButton23 = new javax.swing.JButton();
        jTextField7 = new javax.swing.JTextField();
        jLabel56 = new javax.swing.JLabel();
        jLabel57 = new javax.swing.JLabel();
        jSpinner5 = new javax.swing.JSpinner();
        jSpinner6 = new javax.swing.JSpinner();
        jLabel58 = new javax.swing.JLabel();
        jPanel18 = new javax.swing.JPanel();
        jPanel19 = new javax.swing.JPanel();
        jLabel46 = new javax.swing.JLabel();
        jTextPinBy = new javax.swing.JTextField();
        jTextNombre1By = new javax.swing.JTextField();
        jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jTextNombre2By = new javax.swing.JTextField();
        jLabel50 = new javax.swing.JLabel();
        jTextApellido1By = new javax.swing.JTextField();
        jTextApellido2By = new javax.swing.JTextField();
        jLabel51 = new javax.swing.JLabel();
        jButton18 = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        jLabel49 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jButton19 = new javax.swing.JButton();
        jButton20 = new javax.swing.JButton();
        jLabel52 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jPanel20 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jButton21 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        devicesComboBox = new javax.swing.JComboBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        statusLabel = new javax.swing.JLabel();
        jButton8 = new javax.swing.JButton();
        jComboBox2 = new javax.swing.JComboBox();
        jButton11 = new javax.swing.JButton();
        statusLabel1 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jTextPin = new javax.swing.JTextField();
        jButton5 = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jTextNombre1 = new javax.swing.JTextField();
        jTextNombre2 = new javax.swing.JTextField();
        jTextApellido1 = new javax.swing.JTextField();
        jTextApellido2 = new javax.swing.JTextField();
        jTextFechaExp = new javax.swing.JTextField();
        jTextMcpioExp = new javax.swing.JTextField();
        jTextParticula = new javax.swing.JTextField();
        jTextVigencia = new javax.swing.JTextField();
        jButton10 = new javax.swing.JButton();
        jLabel21 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox();
        jButton12 = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        jButton9 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jRadioButton4 = new javax.swing.JRadioButton();
        jRadioButton5 = new javax.swing.JRadioButton();
        jButton24 = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();

        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());

        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(861, 500));

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.bbs.client.BBSClientApp.class).getContext().getActionMap(BBSClientView.class, this);
        jButton3.setAction(actionMap.get("uploadData")); // NOI18N
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.bbs.client.BBSClientApp.class).getContext().getResourceMap(BBSClientView.class);
        jButton3.setText(resourceMap.getString("jButton3.text")); // NOI18N
        jButton3.setName("jButton3"); // NOI18N

        jButton4.setAction(actionMap.get("verifyData")); // NOI18N
        jButton4.setText(resourceMap.getString("jButton4.text")); // NOI18N
        jButton4.setEnabled(false);
        jButton4.setName("jButton4"); // NOI18N

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel9.border.title"))); // NOI18N
        jPanel9.setName("jPanel9"); // NOI18N

        jTextField1.setEditable(false);
        jTextField1.setText(resourceMap.getString("jTextField1.text")); // NOI18N
        jTextField1.setName("jTextField1"); // NOI18N

        jProgressBar1.setMaximum(10000);
        jProgressBar1.setName("jProgressBar1"); // NOI18N
        jProgressBar1.setStringPainted(true);

        jButton1.setAction(actionMap.get("browseDataFile")); // NOI18N
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addComponent(jTextField1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1)))
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addGap(18, 18, 18)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(19, Short.MAX_VALUE))
        );

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel10.border.title"))); // NOI18N
        jPanel10.setName("jPanel10"); // NOI18N

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        jComboBox1.setModel(new DefaultComboBoxModel(BCLFormat.values()));
        jComboBox1.setName("jComboBox1"); // NOI18N
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jTextField2.setEditable(false);
        jTextField2.setName("jTextField2"); // NOI18N

        jButton2.setAction(actionMap.get("browseFolder")); // NOI18N
        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setName("jButton2"); // NOI18N

        jProgressBar2.setMaximum(10000);
        jProgressBar2.setName("jProgressBar2"); // NOI18N
        jProgressBar2.setStringPainted(true);

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBar2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addComponent(jTextField2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 606, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(18, 18, 18)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2))
                .addGap(18, 18, 18)
                .addComponent(jProgressBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 27, Short.MAX_VALUE))
        );

        jButton13.setAction(actionMap.get("deleteAll")); // NOI18N
        jButton13.setText(resourceMap.getString("jButton13.text")); // NOI18N
        jButton13.setName("jButton13"); // NOI18N
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });

        jPanel12.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel12.border.title"))); // NOI18N
        jPanel12.setName("jPanel12"); // NOI18N

        jLabel22.setText(resourceMap.getString("jLabel22.text")); // NOI18N
        jLabel22.setName("jLabel22"); // NOI18N

        jLabel23.setText(resourceMap.getString("jLabel23.text")); // NOI18N
        jLabel23.setName("jLabel23"); // NOI18N

        TAlpha.setText(resourceMap.getString("TAlpha.text")); // NOI18N
        TAlpha.setName("TAlpha"); // NOI18N

        jLabel24.setText(resourceMap.getString("jLabel24.text")); // NOI18N
        jLabel24.setName("jLabel24"); // NOI18N

        TBio.setText(resourceMap.getString("TBio.text")); // NOI18N
        TBio.setName("TBio"); // NOI18N

        jLabel26.setText(resourceMap.getString("jLabel26.text")); // NOI18N
        jLabel26.setName("jLabel26"); // NOI18N

        TError.setText(resourceMap.getString("TError.text")); // NOI18N
        TError.setName("TError"); // NOI18N

        jLabel27.setText(resourceMap.getString("jLabel27.text")); // NOI18N
        jLabel27.setName("jLabel27"); // NOI18N

        TTimeCargue.setText(resourceMap.getString("TTimeCargue.text")); // NOI18N
        TTimeCargue.setName("TTimeCargue"); // NOI18N

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel12Layout.createSequentialGroup()
                        .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(TAlpha, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel24, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(TBio, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(TError, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel12Layout.createSequentialGroup()
                        .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TTimeCargue, javax.swing.GroupLayout.DEFAULT_SIZE, 741, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel12Layout.createSequentialGroup()
                        .addComponent(jLabel23)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(TAlpha)
                            .addComponent(jLabel22)))
                    .addGroup(jPanel12Layout.createSequentialGroup()
                        .addComponent(jLabel24)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TBio))
                    .addGroup(jPanel12Layout.createSequentialGroup()
                        .addComponent(jLabel26)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TError)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel27)
                    .addComponent(TTimeCargue))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButton14.setAction(actionMap.get("CountAll")); // NOI18N
        jButton14.setText(resourceMap.getString("jButton14.text")); // NOI18N
        jButton14.setName("jButton14"); // NOI18N
        jButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton14ActionPerformed(evt);
            }
        });

        jButton15.setAction(actionMap.get("deleteLogs")); // NOI18N
        jButton15.setName("jButton15"); // NOI18N
        jButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton15ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(10, 10, 10))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(147, 147, 147)
                .addComponent(jButton15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton14)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton3)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3)
                    .addComponent(jButton4)
                    .addComponent(jButton13)
                    .addComponent(jButton14)
                    .addComponent(jButton15))
                .addGap(11, 11, 11)
                .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel1.TabConstraints.tabTitle"), jPanel1); // NOI18N

        jPanel3.setName("jPanel3"); // NOI18N

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel4.border.title"))); // NOI18N
        jPanel4.setName("jPanel4"); // NOI18N

        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N

        TCotejos.setText(resourceMap.getString("TCotejos.text")); // NOI18N
        TCotejos.setName("TCotejos"); // NOI18N

        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N

        THits.setText(resourceMap.getString("THits.text")); // NOI18N
        THits.setName("THits"); // NOI18N

        jLabel11.setText(resourceMap.getString("jLabel11.text")); // NOI18N
        jLabel11.setName("jLabel11"); // NOI18N

        TNoHits.setText(resourceMap.getString("TNoHits.text")); // NOI18N
        TNoHits.setName("TNoHits"); // NOI18N

        jLabel12.setText(resourceMap.getString("jLabel12.text")); // NOI18N
        jLabel12.setName("jLabel12"); // NOI18N

        TUnknow.setText(resourceMap.getString("TUnknow.text")); // NOI18N
        TUnknow.setName("TUnknow"); // NOI18N

        jLabel13.setText(resourceMap.getString("jLabel13.text")); // NOI18N
        jLabel13.setName("jLabel13"); // NOI18N

        TNotFound.setText(resourceMap.getString("TNotFound.text")); // NOI18N
        TNotFound.setName("TNotFound"); // NOI18N

        jLabel14.setText(resourceMap.getString("jLabel14.text")); // NOI18N
        jLabel14.setName("jLabel14"); // NOI18N

        TTime.setText(resourceMap.getString("TTime.text")); // NOI18N
        TTime.setName("TTime"); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(TCotejos, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(THits, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(TNoHits, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(TUnknow, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(TNotFound, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TTime, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TCotejos))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(THits))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TNoHits))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TUnknow))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TNotFound)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(TTime))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel5.border.title"))); // NOI18N
        jPanel5.setName("jPanel5"); // NOI18N

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setText(resourceMap.getString("jRadioButton1.text")); // NOI18N
        jRadioButton1.setName("jRadioButton1"); // NOI18N
        jRadioButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton1ActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRadioButton2);
        jRadioButton2.setSelected(true);
        jRadioButton2.setText(resourceMap.getString("jRadioButton2.text")); // NOI18N
        jRadioButton2.setName("jRadioButton2"); // NOI18N
        jRadioButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton2ActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRadioButton3);
        jRadioButton3.setText(resourceMap.getString("jRadioButton3.text")); // NOI18N
        jRadioButton3.setName("jRadioButton3"); // NOI18N
        jRadioButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jRadioButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRadioButton3)
                .addGap(18, 18, 18)
                .addComponent(jRadioButton2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton1)
                    .addComponent(jRadioButton2)
                    .addComponent(jRadioButton3))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel11.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel11.border.title"))); // NOI18N
        jPanel11.setName("jPanel11"); // NOI18N

        jTextField4.setEditable(false);
        jTextField4.setName("jTextField4"); // NOI18N

        jButton6.setAction(actionMap.get("browseCandidates")); // NOI18N
        jButton6.setName("jButton6"); // NOI18N

        jProgressBar3.setMaximum(10000);
        jProgressBar3.setName("jProgressBar3"); // NOI18N
        jProgressBar3.setStringPainted(true);

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBar3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addComponent(jTextField4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton6)))
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton6)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jProgressBar3, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(22, Short.MAX_VALUE))
        );

        jButton7.setAction(actionMap.get("matchProcess")); // NOI18N
        jButton7.setText(resourceMap.getString("jButton7.text")); // NOI18N
        jButton7.setName("jButton7"); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(353, 353, 353)
                .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(371, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton7)
                .addContainerGap(702, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel3.TabConstraints.tabTitle"), jPanel3); // NOI18N

        jPanel13.setName("jPanel13"); // NOI18N

        jPanel14.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel14.border.title"))); // NOI18N
        jPanel14.setName("jPanel14"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jPanel16.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel16.border.title"))); // NOI18N
        jPanel16.setName("jPanel16"); // NOI18N

        jProgressBar4.setName("jProgressBar4"); // NOI18N

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        jLabel25.setText(resourceMap.getString("jLabel25.text")); // NOI18N
        jLabel25.setName("jLabel25"); // NOI18N

        jLabel28.setText(resourceMap.getString("jLabel28.text")); // NOI18N
        jLabel28.setName("jLabel28"); // NOI18N

        jLabel29.setText(resourceMap.getString("jLabel29.text")); // NOI18N
        jLabel29.setName("jLabel29"); // NOI18N

        jLabel30.setText(resourceMap.getString("jLabel30.text")); // NOI18N
        jLabel30.setName("jLabel30"); // NOI18N

        jLabel31.setName("jLabel31"); // NOI18N

        jLabel32.setName("jLabel32"); // NOI18N

        jLabel33.setName("jLabel33"); // NOI18N

        jLabel42.setText(resourceMap.getString("jLabel42.text")); // NOI18N
        jLabel42.setName("jLabel42"); // NOI18N

        jLabel43.setName("jLabel43"); // NOI18N

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBar4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel16Layout.createSequentialGroup()
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 78, Short.MAX_VALUE)
                            .addComponent(jLabel30, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel25, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel31, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel28, javax.swing.GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE)
                            .addComponent(jLabel32, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel29, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE)
                            .addComponent(jLabel33, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel42)
                            .addComponent(jLabel43, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 376, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel16Layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel16Layout.createSequentialGroup()
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(jLabel25)
                            .addComponent(jLabel28)
                            .addComponent(jLabel29))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel30, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel33, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel16Layout.createSequentialGroup()
                        .addComponent(jLabel42)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel43, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 29, Short.MAX_VALUE)
                .addComponent(jProgressBar4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jButton16.setAction(actionMap.get("StartAlphaTest")); // NOI18N
        jButton16.setText(resourceMap.getString("jButton16.text")); // NOI18N
        jButton16.setName("jButton16"); // NOI18N

        jSpinner1.setName("jSpinner1"); // NOI18N
        jSpinner1.setValue(new Integer(200));

        jLabel53.setText(resourceMap.getString("jLabel53.text")); // NOI18N
        jLabel53.setName("jLabel53"); // NOI18N

        jSpinner3.setName("jSpinner3"); // NOI18N
        jSpinner3.setValue(new Integer(0));

        jSpinner4.setName("jSpinner4"); // NOI18N
        jSpinner4.setValue(new Integer(0));

        jLabel54.setText(resourceMap.getString("jLabel54.text")); // NOI18N
        jLabel54.setName("jLabel54"); // NOI18N

        jLabel55.setText(resourceMap.getString("jLabel55.text")); // NOI18N
        jLabel55.setName("jLabel55"); // NOI18N

        jTextField6.setEditable(false);
        jTextField6.setText(resourceMap.getString("jTextField6.text")); // NOI18N
        jTextField6.setName("jTextField6"); // NOI18N

        jButton22.setText(resourceMap.getString("jButton22.text")); // NOI18N
        jButton22.setName("jButton22"); // NOI18N
        jButton22.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton22ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel14Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton16)
                        .addGap(103, 103, 103)
                        .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel53)
                            .addComponent(jSpinner3, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel54)
                            .addComponent(jSpinner4, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(35, 35, 35)
                        .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel55, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextField6))
                        .addGap(18, 18, 18)
                        .addComponent(jButton22)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel14Layout.createSequentialGroup()
                        .addComponent(jLabel53)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(jButton16)
                            .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jSpinner3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel14Layout.createSequentialGroup()
                        .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel54)
                            .addComponent(jLabel55))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jSpinner4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton22))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel15.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel15.border.title"))); // NOI18N
        jPanel15.setName("jPanel15"); // NOI18N

        jPanel17.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel17.border.title"))); // NOI18N
        jPanel17.setName("jPanel17"); // NOI18N

        jLabel34.setText(resourceMap.getString("jLabel34.text")); // NOI18N
        jLabel34.setName("jLabel34"); // NOI18N

        jLabel35.setText(resourceMap.getString("jLabel35.text")); // NOI18N
        jLabel35.setName("jLabel35"); // NOI18N

        jLabel36.setText(resourceMap.getString("jLabel36.text")); // NOI18N
        jLabel36.setName("jLabel36"); // NOI18N

        jLabel37.setText(resourceMap.getString("jLabel37.text")); // NOI18N
        jLabel37.setName("jLabel37"); // NOI18N

        jLabel38.setName("jLabel38"); // NOI18N

        jLabel39.setName("jLabel39"); // NOI18N

        jLabel40.setName("jLabel40"); // NOI18N

        jLabel41.setName("jLabel41"); // NOI18N

        jProgressBar5.setName("jProgressBar5"); // NOI18N

        jLabel44.setText(resourceMap.getString("jLabel44.text")); // NOI18N
        jLabel44.setName("jLabel44"); // NOI18N

        jLabel45.setName("jLabel45"); // NOI18N

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBar5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel17Layout.createSequentialGroup()
                        .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel41, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel34, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel35, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel40, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel36, javax.swing.GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE)
                            .addComponent(jLabel39, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel37, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE)
                            .addComponent(jLabel38, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel44)
                            .addComponent(jLabel45, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 376, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel17Layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel17Layout.createSequentialGroup()
                        .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel34)
                            .addComponent(jLabel35)
                            .addComponent(jLabel36)
                            .addComponent(jLabel37))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel41, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel40, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel39, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel38, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel17Layout.createSequentialGroup()
                        .addComponent(jLabel44)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel45, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 29, Short.MAX_VALUE)
                .addComponent(jProgressBar5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        jButton17.setAction(actionMap.get("StartMatchTest")); // NOI18N
        jButton17.setText(resourceMap.getString("jButton17.text")); // NOI18N
        jButton17.setName("jButton17"); // NOI18N

        jSpinner2.setName("jSpinner2"); // NOI18N
        jSpinner2.setValue(new Integer(200));

        jButton23.setText(resourceMap.getString("jButton23.text")); // NOI18N
        jButton23.setName("jButton23"); // NOI18N
        jButton23.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton23ActionPerformed(evt);
            }
        });

        jTextField7.setEditable(false);
        jTextField7.setName("jTextField7"); // NOI18N

        jLabel56.setText(resourceMap.getString("jLabel56.text")); // NOI18N
        jLabel56.setName("jLabel56"); // NOI18N

        jLabel57.setText(resourceMap.getString("jLabel57.text")); // NOI18N
        jLabel57.setName("jLabel57"); // NOI18N

        jSpinner5.setName("jSpinner5"); // NOI18N
        jSpinner5.setValue(new Integer(0));

        jSpinner6.setName("jSpinner6"); // NOI18N
        jSpinner6.setValue(new Integer(0));

        jLabel58.setText(resourceMap.getString("jLabel58.text")); // NOI18N
        jLabel58.setName("jLabel58"); // NOI18N

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel15Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinner2, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(7, 7, 7)
                        .addComponent(jButton17)
                        .addGap(103, 103, 103)
                        .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel58)
                            .addComponent(jSpinner6, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel57)
                            .addComponent(jSpinner5, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(35, 35, 35)
                        .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel56, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jButton23)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(jButton17)
                        .addComponent(jSpinner2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel15Layout.createSequentialGroup()
                            .addComponent(jLabel58)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jSpinner6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel15Layout.createSequentialGroup()
                            .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel57)
                                .addComponent(jLabel56))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jSpinner5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButton23)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel15, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(532, Short.MAX_VALUE))
        );

        jPanel14.getAccessibleContext().setAccessibleName(resourceMap.getString("jPanel14.AccessibleContext.accessibleName")); // NOI18N
        jPanel15.getAccessibleContext().setAccessibleName(resourceMap.getString("jPanel15.AccessibleContext.accessibleName")); // NOI18N

        jTabbedPane1.addTab(resourceMap.getString("jPanel13.TabConstraints.tabTitle"), jPanel13); // NOI18N

        jPanel18.setName("jPanel18"); // NOI18N

        jPanel19.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel19.border.title"))); // NOI18N
        jPanel19.setName("jPanel19"); // NOI18N

        jLabel46.setText(resourceMap.getString("jLabel46.text")); // NOI18N
        jLabel46.setName("jLabel46"); // NOI18N

        jTextPinBy.setBackground(resourceMap.getColor("jTextPinBy.background")); // NOI18N
        jTextPinBy.setName("jTextPinBy"); // NOI18N
        jTextPinBy.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextPinByKeyReleased(evt);
            }
        });

        jTextNombre1By.setBackground(resourceMap.getColor("jTextApellido4.background")); // NOI18N
        jTextNombre1By.setName("jTextNombre1By"); // NOI18N
        jTextNombre1By.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextNombre1ByFocusLost(evt);
            }
        });

        jLabel47.setText(resourceMap.getString("jLabel47.text")); // NOI18N
        jLabel47.setName("jLabel47"); // NOI18N

        jLabel48.setText(resourceMap.getString("jLabel48.text")); // NOI18N
        jLabel48.setName("jLabel48"); // NOI18N

        jTextNombre2By.setBackground(resourceMap.getColor("jTextApellido4.background")); // NOI18N
        jTextNombre2By.setName("jTextNombre2By"); // NOI18N
        jTextNombre2By.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextNombre2ByFocusLost(evt);
            }
        });

        jLabel50.setText(resourceMap.getString("jLabel50.text")); // NOI18N
        jLabel50.setName("jLabel50"); // NOI18N

        jTextApellido1By.setBackground(resourceMap.getColor("jTextApellido4.background")); // NOI18N
        jTextApellido1By.setName("jTextApellido1By"); // NOI18N
        jTextApellido1By.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextApellido1ByFocusLost(evt);
            }
        });

        jTextApellido2By.setBackground(resourceMap.getColor("jTextApellido2By.background")); // NOI18N
        jTextApellido2By.setName("jTextApellido2By"); // NOI18N
        jTextApellido2By.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextApellido2ByFocusLost(evt);
            }
        });

        jLabel51.setText(resourceMap.getString("jLabel51.text")); // NOI18N
        jLabel51.setName("jLabel51"); // NOI18N

        jButton18.setAction(actionMap.get("findPersonBy")); // NOI18N
        jButton18.setText(resourceMap.getString("jButton18.text")); // NOI18N
        jButton18.setName("jButton18"); // NOI18N

        jCheckBox1.setSelected(true);
        jCheckBox1.setText(resourceMap.getString("jCheckBox1.text")); // NOI18N
        jCheckBox1.setName("jCheckBox1"); // NOI18N

        jLabel49.setText(resourceMap.getString("jLabel49.text")); // NOI18N
        jLabel49.setName("jLabel49"); // NOI18N

        jTextField3.setEditable(false);
        jTextField3.setText(resourceMap.getString("jTextField3.text")); // NOI18N
        jTextField3.setName("jTextField3"); // NOI18N

        jButton19.setText(resourceMap.getString("jButton19.text")); // NOI18N
        jButton19.setName("jButton19"); // NOI18N
        jButton19.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton19ActionPerformed(evt);
            }
        });

        jButton20.setText(resourceMap.getString("jButton20.text")); // NOI18N
        jButton20.setName("jButton20"); // NOI18N
        jButton20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton20ActionPerformed(evt);
            }
        });

        jLabel52.setText(resourceMap.getString("jLabel52.text")); // NOI18N
        jLabel52.setName("jLabel52"); // NOI18N

        jTextField5.setEditable(false);
        jTextField5.setText(resourceMap.getString("jTextField5.text")); // NOI18N
        jTextField5.setName("jTextField5"); // NOI18N

        javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel19Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel19Layout.createSequentialGroup()
                        .addComponent(jLabel46, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextPinBy, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel19Layout.createSequentialGroup()
                        .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel19Layout.createSequentialGroup()
                                .addComponent(jLabel49)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(jCheckBox1, javax.swing.GroupLayout.Alignment.LEADING))
                        .addGap(26, 26, 26)
                        .addComponent(jLabel52)))
                .addGap(18, 18, 18)
                .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel19Layout.createSequentialGroup()
                        .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel47, javax.swing.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
                            .addComponent(jLabel50, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextApellido1By, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
                            .addComponent(jTextNombre1By)))
                    .addGroup(jPanel19Layout.createSequentialGroup()
                        .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel19Layout.createSequentialGroup()
                        .addComponent(jLabel48, javax.swing.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextNombre2By, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel19Layout.createSequentialGroup()
                            .addComponent(jButton20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jButton18, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel19Layout.createSequentialGroup()
                            .addComponent(jLabel51, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jTextApellido2By, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        jPanel19Layout.setVerticalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel19Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel46)
                    .addComponent(jTextPinBy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel47)
                    .addComponent(jTextNombre1By, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel48)
                    .addComponent(jTextNombre2By, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel50)
                    .addComponent(jTextApellido1By, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel51)
                    .addComponent(jTextApellido2By, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBox1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton18)
                    .addComponent(jLabel49)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton19)
                    .addComponent(jButton20)
                    .addComponent(jLabel52)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel20.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel20.border.title"))); // NOI18N
        jPanel20.setName("jPanel20"); // NOI18N

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "NUIP/NIP", "Nombre1", "Nombre2", "Particula", "Apellido1", "Apellido2", "ExpLugar", "ExpFecha", "Vigencia"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTable2.setName("jTable2"); // NOI18N
        jScrollPane3.setViewportView(jTable2);

        jButton21.setAction(actionMap.get("exportFindPersonBy")); // NOI18N
        jButton21.setText(resourceMap.getString("jButton21.text")); // NOI18N
        jButton21.setName("jButton21"); // NOI18N

        javax.swing.GroupLayout jPanel20Layout = new javax.swing.GroupLayout(jPanel20);
        jPanel20.setLayout(jPanel20Layout);
        jPanel20Layout.setHorizontalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel20Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel20Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton21, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel20Layout.setVerticalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel20Layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton21)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel18Layout.createSequentialGroup()
                        .addComponent(jPanel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel18Layout.createSequentialGroup()
                        .addComponent(jPanel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(32, 32, 32))))
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jPanel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(529, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel18.TabConstraints.tabTitle"), jPanel18); // NOI18N

        jPanel2.setName("jPanel2"); // NOI18N

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel6.border.title"))); // NOI18N
        jPanel6.setName("jPanel6"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        devicesComboBox.setName("devicesComboBox"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        statusLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        statusLabel.setText(resourceMap.getString("statusLabel.text")); // NOI18N
        statusLabel.setName("statusLabel"); // NOI18N

        jButton8.setAction(actionMap.get("capture")); // NOI18N
        jButton8.setText(resourceMap.getString("jButton8.text")); // NOI18N
        jButton8.setName("jButton8"); // NOI18N

        jComboBox2.setName("jComboBox2"); // NOI18N
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });

        jButton11.setAction(actionMap.get("addFingerprint")); // NOI18N
        jButton11.setName("jButton11"); // NOI18N

        statusLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        statusLabel1.setText(resourceMap.getString("statusLabel1.text")); // NOI18N
        statusLabel1.setName("statusLabel1"); // NOI18N

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(devicesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                                .addComponent(jButton8, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton11, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(statusLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(statusLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );

        jPanel6Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {devicesComboBox, jComboBox2});

        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(devicesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(statusLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton8)
                    .addComponent(jButton11))
                .addGap(8, 8, 8))
        );

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel7.border.title"))); // NOI18N
        jPanel7.setName("jPanel7"); // NOI18N

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        jTextPin.setBackground(resourceMap.getColor("jTextPin.background")); // NOI18N
        jTextPin.setText(resourceMap.getString("jTextPin.text")); // NOI18N
        jTextPin.setName("jTextPin"); // NOI18N
        jTextPin.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextPinKeyReleased(evt);
            }
        });

        jButton5.setAction(actionMap.get("findPerson")); // NOI18N
        jButton5.setText(resourceMap.getString("jButton5.text")); // NOI18N
        jButton5.setName("jButton5"); // NOI18N

        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N

        jLabel10.setText(resourceMap.getString("jLabel10.text")); // NOI18N
        jLabel10.setName("jLabel10"); // NOI18N

        jLabel15.setText(resourceMap.getString("jLabel15.text")); // NOI18N
        jLabel15.setName("jLabel15"); // NOI18N

        jLabel16.setText(resourceMap.getString("jLabel16.text")); // NOI18N
        jLabel16.setName("jLabel16"); // NOI18N

        jLabel17.setText(resourceMap.getString("jLabel17.text")); // NOI18N
        jLabel17.setName("jLabel17"); // NOI18N

        jLabel18.setText(resourceMap.getString("jLabel18.text")); // NOI18N
        jLabel18.setName("jLabel18"); // NOI18N

        jLabel19.setText(resourceMap.getString("jLabel19.text")); // NOI18N
        jLabel19.setName("jLabel19"); // NOI18N

        jLabel20.setText(resourceMap.getString("jLabel20.text")); // NOI18N
        jLabel20.setName("jLabel20"); // NOI18N

        jTextNombre1.setName("jTextNombre1"); // NOI18N

        jTextNombre2.setName("jTextNombre2"); // NOI18N

        jTextApellido1.setName("jTextApellido1"); // NOI18N

        jTextApellido2.setName("jTextApellido2"); // NOI18N

        jTextFechaExp.setName("jTextFechaExp"); // NOI18N

        jTextMcpioExp.setName("jTextMcpioExp"); // NOI18N

        jTextParticula.setName("jTextParticula"); // NOI18N

        jTextVigencia.setName("jTextVigencia"); // NOI18N

        jButton10.setAction(actionMap.get("createPerson")); // NOI18N
        jButton10.setText(resourceMap.getString("jButton10.text")); // NOI18N
        jButton10.setName("jButton10"); // NOI18N

        jLabel21.setText(resourceMap.getString("jLabel21.text")); // NOI18N
        jLabel21.setName("jLabel21"); // NOI18N

        jComboBox3.setName("jComboBox3"); // NOI18N

        jButton12.setAction(actionMap.get("deletePerson")); // NOI18N
        jButton12.setText(resourceMap.getString("jButton12.text")); // NOI18N
        jButton12.setName("jButton12"); // NOI18N

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                        .addComponent(jButton10, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton12, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(jLabel21)
                                .addGap(55, 55, 55))
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel18, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextVigencia)
                            .addComponent(jTextMcpioExp)
                            .addComponent(jTextParticula)
                            .addComponent(jTextNombre1)
                            .addComponent(jComboBox3, 0, 184, Short.MAX_VALUE)
                            .addComponent(jTextNombre2)
                            .addComponent(jTextApellido1)
                            .addComponent(jTextApellido2)
                            .addComponent(jTextFechaExp)
                            .addComponent(jTextPin))))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextPin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jTextNombre1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jTextNombre2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(jTextParticula, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(jTextApellido1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(jTextApellido2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(jTextFechaExp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(jTextMcpioExp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(jTextVigencia, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton5)
                    .addComponent(jButton10)
                    .addComponent(jButton12))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel8.border.title"))); // NOI18N
        jPanel8.setName("jPanel8"); // NOI18N

        jButton9.setAction(actionMap.get("MatchOneOne")); // NOI18N
        jButton9.setText(resourceMap.getString("jButton9.text")); // NOI18N
        jButton9.setName("jButton9"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Dedo", "Puntuación", "Resultado"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTable1.setName("jTable1"); // NOI18N
        jScrollPane2.setViewportView(jTable1);

        buttonGroup2.add(jRadioButton4);
        jRadioButton4.setSelected(true);
        jRadioButton4.setText(resourceMap.getString("jRadioButton4.text")); // NOI18N
        jRadioButton4.setName("jRadioButton4"); // NOI18N
        jRadioButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton4ActionPerformed(evt);
            }
        });

        buttonGroup2.add(jRadioButton5);
        jRadioButton5.setText(resourceMap.getString("jRadioButton5.text")); // NOI18N
        jRadioButton5.setName("jRadioButton5"); // NOI18N
        jRadioButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton5ActionPerformed(evt);
            }
        });

        jButton24.setAction(actionMap.get("MatchOneOne")); // NOI18N
        jButton24.setText(resourceMap.getString("jButton24.text")); // NOI18N
        jButton24.setEnabled(false);
        jButton24.setName("jButton24"); // NOI18N
        jButton24.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton24ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addComponent(jRadioButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton24)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton9, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(34, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 307, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton9)
                    .addComponent(jRadioButton4)
                    .addComponent(jRadioButton5)
                    .addComponent(jButton24))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(28, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel2.TabConstraints.tabTitle"), jPanel2); // NOI18N

        mainPanel.add(jTabbedPane1, java.awt.BorderLayout.PAGE_START);

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        jMenuItem1.setAction(actionMap.get("showLogConsole")); // NOI18N
        jMenuItem1.setText(resourceMap.getString("jMenuItem1.text")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        fileMenu.add(jMenuItem1);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setText(resourceMap.getString("exitMenuItem.text")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setText(resourceMap.getString("aboutMenuItem.text")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 880, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 710, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        format = (BCLFormat) jComboBox1.getSelectedItem();
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton1ActionPerformed
        matchMODE = MatchMODE.LOCAL;
    }//GEN-LAST:event_jRadioButton1ActionPerformed

    private void jRadioButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton2ActionPerformed
        matchMODE = MatchMODE.FRACTION;
    }//GEN-LAST:event_jRadioButton2ActionPerformed

    private void jRadioButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton3ActionPerformed
        matchMODE = MatchMODE.REMOTE;
    }//GEN-LAST:event_jRadioButton3ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
    }//GEN-LAST:event_jButton13ActionPerformed

    private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton14ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton14ActionPerformed

    private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton15ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton15ActionPerformed

    private void jRadioButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton5ActionPerformed
        liveMODE = MatchMODE.REMOTE;
    }//GEN-LAST:event_jRadioButton5ActionPerformed

    private void jRadioButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton4ActionPerformed
        liveMODE = MatchMODE.LOCAL;
    }//GEN-LAST:event_jRadioButton4ActionPerformed

    private void jTextPinKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextPinKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            jButton5.doClick();
        }
    }//GEN-LAST:event_jTextPinKeyReleased

    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed
        ((DefaultTableModel) jTable1.getModel()).setNumRows(0);
    }//GEN-LAST:event_jComboBox2ActionPerformed

    private void jTextPinByKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextPinByKeyReleased
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextPinByKeyReleased

    private void jButton19ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton19ActionPerformed
        jTextField3.setText("0");
    }//GEN-LAST:event_jButton19ActionPerformed

    private void jButton20ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton20ActionPerformed
        jTextPinBy.setText("");
        jTextNombre1By.setText("");
        jTextNombre2By.setText("");
        jTextApellido1By.setText("");
        jTextApellido2By.setText("");
        jTextField5.setText("0");
        ((DefaultTableModel) jTable2.getModel()).setNumRows(0);
    }//GEN-LAST:event_jButton20ActionPerformed

    private void jTextNombre1ByFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextNombre1ByFocusLost
        jTextNombre1By.setText(jTextNombre1By.getText().toUpperCase());
    }//GEN-LAST:event_jTextNombre1ByFocusLost

    private void jTextApellido1ByFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextApellido1ByFocusLost
        jTextApellido1By.setText(jTextApellido1By.getText().toUpperCase());
    }//GEN-LAST:event_jTextApellido1ByFocusLost

    private void jTextNombre2ByFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextNombre2ByFocusLost
        jTextNombre2By.setText(jTextNombre2By.getText().toUpperCase());
    }//GEN-LAST:event_jTextNombre2ByFocusLost

    private void jTextApellido2ByFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextApellido2ByFocusLost
        jTextApellido2By.setText(jTextApellido2By.getText().toUpperCase());
    }//GEN-LAST:event_jTextApellido2ByFocusLost

    private void jButton22ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton22ActionPerformed
        int showConfirmDialog = JOptionPane.showConfirmDialog(null, "¿Desea Programar la Prueba?", "Prueba de concurrencia alfanumérica", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (showConfirmDialog == JOptionPane.YES_OPTION) {
            jButton16.setEnabled(false);
            jSpinner1.setEnabled(false);
            timerAlpha = new TestTimer(1, jTextField6, Integer.parseInt(String.valueOf(jSpinner3.getValue())), Integer.parseInt(String.valueOf(jSpinner4.getValue())), new TimerCallback() {
                public void fire() {
                    testConcurrentAlpha();
                }
            });
            timerAlpha.start();
            jSpinner3.setEnabled(false);
            jSpinner4.setEnabled(false);
        }
    }//GEN-LAST:event_jButton22ActionPerformed

    private void jButton23ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton23ActionPerformed
        int showConfirmDialog = JOptionPane.showConfirmDialog(null, "¿Desea Programar la Prueba?", "Prueba de concurrencia de cotejo", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (showConfirmDialog == JOptionPane.YES_OPTION) {
            jButton17.setEnabled(false);
            jSpinner2.setEnabled(false);
            timerBio = new TestTimer(1, jTextField7, Integer.parseInt(String.valueOf(jSpinner6.getValue())), Integer.parseInt(String.valueOf(jSpinner5.getValue())), new TimerCallback() {
                public void fire() {
                    testConcurrentMatch();
                }
            });
            timerBio.start();
            jSpinner6.setEnabled(false);
            jSpinner5.setEnabled(false);
        }
    }//GEN-LAST:event_jButton23ActionPerformed

    private void jButton24ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton24ActionPerformed
        showView();
    }//GEN-LAST:event_jButton24ActionPerformed

    @Action
    public Task browseDataFile() {
        return new BrowseDataFileTask(getApplication());
    }

    private void postInitComponents() {

        actionMap = org.jdesktop.application.Application.getInstance(com.bbs.client.BBSClientApp.class).getContext().getActionMap(BBSClientView.class, this);
        NeuroManager.getInstance();

        this.getFrame().setTitle(this.getFrame().getTitle() + " :: " + Configuration.getProperty("BIOClient.ID"));

        jRadioButton4.setVisible(false);
        jRadioButton5.setVisible(false);

        timerAlpha = new TestTimer(1, jTextField6);
        timerAlpha.start();
        timerBio = new TestTimer(1, jTextField7);
        timerBio.start();

        login = new BBSLogin(org.jdesktop.application.Application.getInstance(com.bbs.client.BBSClientApp.class).getMainFrame(), true);
        login.setVisible(true);

        console = new BBSLogConsole(org.jdesktop.application.Application.getInstance(com.bbs.client.BBSClientApp.class).getMainFrame(), true);


        synchronized (Devices.getInstance().getFingerScanners().getDevices()) {
            updateDeviceList();
        }
        Devices.getInstance().getFingerScanners().addDevicesChangeListener(this);

        view = new NFView();
        view.setAutofit(true);
        jScrollPane1.setViewportView(view);

        jComboBox1.setSelectedItem(BCLFormat.BCL_ISO_19794_2);

    }

    private void updateDeviceList() {
        NDeviceManager.DeviceCollection devices = Devices.getInstance().getFingerScanners().getDevices();
        DefaultComboBoxModel model = (DefaultComboBoxModel) devicesComboBox.getModel();
        model.removeAllElements();
        if (devices.size() != 0) {
            for (NDevice device : devices) {
                model.addElement(new Device((NFScanner) device));
            }
            if (model.getSize() > 0) {
                devicesComboBox.setSelectedIndex(0);
            }
        }
        DefaultComboBoxModel fingerModel = new DefaultComboBoxModel();
        fingerModel.addElement(BioPosition.parse(NFPosition.UNKNOWN));

        fingerModel.addElement(BioPosition.parse(NFPosition.RIGHT_THUMB));
        fingerModel.addElement(BioPosition.parse(NFPosition.RIGHT_INDEX_FINGER));
        fingerModel.addElement(BioPosition.parse(NFPosition.RIGHT_MIDDLE_FINGER));
        fingerModel.addElement(BioPosition.parse(NFPosition.RIGHT_RING_FINGER));
        fingerModel.addElement(BioPosition.parse(NFPosition.RIGHT_LITTLE_FINGER));

        fingerModel.addElement(BioPosition.parse(NFPosition.LEFT_THUMB));
        fingerModel.addElement(BioPosition.parse(NFPosition.LEFT_INDEX_FINGER));
        fingerModel.addElement(BioPosition.parse(NFPosition.LEFT_MIDDLE_FINGER));
        fingerModel.addElement(BioPosition.parse(NFPosition.LEFT_RING_FINGER));
        fingerModel.addElement(BioPosition.parse(NFPosition.LEFT_LITTLE_FINGER));

        jComboBox2.setModel(fingerModel);

    }

    public enum BioPosition {

        UNKNOWN(NFPosition.UNKNOWN, "Desconocido"), RIGHT_THUMB(NFPosition.RIGHT_THUMB, "Pulgar Derecho"),
        RIGHT_INDEX_FINGER(NFPosition.RIGHT_INDEX_FINGER, "Indice Derecho"), RIGHT_MIDDLE_FINGER(NFPosition.RIGHT_MIDDLE_FINGER, "Medio Derecho"),
        RIGHT_RING_FINGER(NFPosition.RIGHT_RING_FINGER, "Anular Derecho"), RIGHT_LITTLE_FINGER(NFPosition.RIGHT_LITTLE_FINGER, "Meñique Derecho"),
        LEFT_THUMB(NFPosition.LEFT_THUMB, "Pulgar Izquierdo"), LEFT_INDEX_FINGER(NFPosition.LEFT_INDEX_FINGER, "Indice Izquierdo"),
        LEFT_MIDDLE_FINGER(NFPosition.LEFT_MIDDLE_FINGER, "Medio Izquierdo"), LEFT_RING_FINGER(NFPosition.LEFT_RING_FINGER, "Anular Izquierdo"),
        LEFT_LITTLE_FINGER(NFPosition.LEFT_LITTLE_FINGER, "Meñique Izquierdo");
        private NFPosition position;
        private String text;

        BioPosition(NFPosition pos, String text) {
            this.position = pos;
            this.text = text;
        }

        public static BioPosition parse(NFPosition pos) {
            for (BioPosition bpos : BioPosition.values()) {
                if (pos == bpos.getPosition()) {
                    return bpos;
                }
            }
            return BioPosition.UNKNOWN;
        }

        @Override
        public String toString() {
            return text;
        }

        /**
         * @return the position
         */
        public NFPosition getPosition() {
            return position;
        }

        /**
         * @param position the position to set
         */
        public void setPosition(NFPosition position) {
            this.position = position;
        }

        /**
         * @return the text
         */
        public String getText() {
            return text;
        }

        /**
         * @param text the text to set
         */
        public void setText(String text) {
            this.text = text;
        }
    }

    @Override
    public void deviceAdded(NDeviceEvent event) {
        DefaultComboBoxModel model = (DefaultComboBoxModel) devicesComboBox.getModel();
        model.addElement(new Device((NFScanner) event.getDevice()));
    }

    private Device getSelectedDevice() {
        return (Device) devicesComboBox.getSelectedItem();
    }

    public void deviceRemoved(NDeviceEvent event) {
        DefaultComboBoxModel model = (DefaultComboBoxModel) devicesComboBox.getModel();
        Device device = getSelectedDevice();
        if (device != null && event.getDevice().equals(device.getDevice())) {
            finishCapturing();
        }
        for (int i = 0; i < model.getSize(); i++) {
            Device d = (Device) model.getElementAt(i);
            if (d.getDevice().equals(event.getDevice())) {
                model.removeElementAt(i);
            }
        }
    }

    public void devicesRefreshed(EventObject event) {
        updateDeviceList();
    }

    public void changing(EventObject eo) {
    }

    public void changed(EventObject eo) {
    }

    private void startCapturing() {
        List<NFPosition> selectedPositions = new ArrayList<NFPosition>();
        List<NFPosition> missingPositions = new ArrayList<NFPosition>();
        NFPosition selectedFinger = ((BioPosition) jComboBox2.getSelectedItem()).getPosition();
        if (selectedFinger == null) {
            selectedFinger = NFPosition.UNKNOWN;
        }
        selectedPositions.add(selectedFinger);
        scenario = Scenario.PLAIN_FINGER;
        Device device = getSelectedDevice();
        prepareForCapturing();
        device.getDevice().addPreviewListener(this);
        backgroundWorker = new BackgroundWorker(device, scenario, selectedPositions, missingPositions);
        backgroundWorker.addPropertyChangeListener(this);
        backgroundWorker.execute();

        jButton8.setText(resourceMap.getString("jButton8.Stop.text")); // NOI18N



    }

    private void prepareForCapturing() {
        view.setImage(null);
        view.setTemplate(null);
    }

    private void stopCapturing() {
        finishCapturing();
        if (backgroundWorker != null) {
            backgroundWorker.cancel(true);
            backgroundWorker = null;
        }
    }

    private void finishCapturing() {

        Device device = getSelectedDevice();
        if (device != null) {
            if (device.getDevice().isAvailable()) {
                device.getDevice().cancel();
            }
            device.getDevice().removePreviewListener(this);
            jButton8.setText(resourceMap.getString("jButton8.text")); // NOI18N
            statusLabel.setText("Estado:");
        }
    }

    public void preview(NFScannerPreviewEvent event) {
        if (!backgroundWorker.isCancelled()) {
            if (event.getImage() != null) {

                view.setImage(event.getImage().toImage());
                jButton8.setText(resourceMap.getString("jButton8.text")); // NOI18N


            }
            statusLabel.setText("Estado: " + event.getStatus().toString());

        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            //doProgress((Integer) evt.getNewValue() != 100);
        } else if (BackgroundWorker.CURRENT_FINGER_CHANGED_PROPERTY.equals(evt.getPropertyName())) {
            if (!backgroundWorker.isCancelled()) {
                updateHint(true);
            }
        }
        if ("state".equals(evt.getPropertyName())) {
            if (backgroundWorker.getRecord() != null) {
                statusLabel1.setText("Calidad:" + backgroundWorker.getRecord().getRecord().getQuality());
            } else {
                statusLabel1.setText("Calidad:");
            }
        }

    }

    private void updateHint(boolean liveCapturing) {
        Device device = getSelectedDevice();

//        String hint = "Hint: ";
//        NFPosition currentPosition = NFPosition.UNKNOWN;
//        NFPosition highlightedPosition;
//        if (true) {
//            highlightedPosition = fingerSelector.getHightlightedPosition();
//            if (highlightedPosition == null) {
//                if (fingerSelector.getSelectedPositions().size() > 0) {
//                    currentPosition = fingerSelector.getSelectedPositions().get(0);
//                    fingerSelector.setHighlighted(currentPosition, true);
//                }
//            } else {
//                if (fingerSelector.getSelectedPositions().size() > 0) {
//                    fingerSelector.setHighlighted(highlightedPosition, false);
//                    fingerSelector.setSelected(highlightedPosition, false);
//                    currentPosition = fingerSelector.getSelectedPositions().get(0);
//                    fingerSelector.setHighlighted(currentPosition, true);
//                }
//            }
//            Device device = getSelectedDevice();
//            if (liveCapturing) {
//                hint = "Hint: Place " + currentPosition + " on the " + ((device != null) ? device.getDevice().getDisplayName() : "") + " scanner.";
//            } else {
//                hint = "Hint: Open file with " + currentPosition + " position.";
//            }
//        } else {
//            if (liveCapturing) {
//                fingerSelector.clearHighlighting();
//                hint = "Hint: Press Capture button to start capturing.";
//            } else {
//                if (fingerSelector.getSelectedPositions().size() > 0) {
//                    currentPosition = fingerSelector.getSelectedPositions().get(0);
//                    fingerSelector.setHighlighted(currentPosition, true);
//                }
//                hint = "Hint: Open file with " + currentPosition + " position.";
//            }
//        }
//        hintLabel.setText(hint);
    }

    private class BrowseDataFileTask extends org.jdesktop.application.Task<Object, Void> implements Observer {

        BrowseDataFileTask(org.jdesktop.application.Application app) {
            super(app);
        }

        @Override
        protected Object doInBackground() {
            JFileChooser jfc = new JFileChooser();
            jfc.setCurrentDirectory(new File("./"));
            jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            jfc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f != null && f.exists() && (f.isDirectory() || (f.isFile() && f.getName().endsWith(".csv")));
                }

                @Override
                public String getDescription() {
                    return "CSV File";
                }
            });
            jfc.showOpenDialog(null);
            File selectedFile = jfc.getSelectedFile();
            if (selectedFile != null) {
                jProgressBar1.setValue(0);
                jTextField1.setText(selectedFile.getAbsolutePath());
                csvProcessor = new CSVProcessor(selectedFile);
                csvProcessor.addObserver(this);
                csvProcessor.process();
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            jButton4.setEnabled(csvProcessor != null && bioProcessor != null);
        }

        public void update(Observable o, Object arg) {
            jProgressBar1.setValue(jProgressBar1.getValue() + 1);
        }
    }

    @Action
    public Task browseFolder() {
        return new BrowseFolderTask(getApplication());
    }

    private class BrowseFolderTask extends org.jdesktop.application.Task<Object, Void> implements Observer {

        BrowseFolderTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to BrowseFolderTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {
            JFileChooser jfc = new JFileChooser();
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            jfc.showOpenDialog(null);
            File selectedFile = jfc.getSelectedFile();

            if (selectedFile != null && selectedFile.exists() && selectedFile.isDirectory()) {
                jTextField2.setText(selectedFile.getAbsolutePath());
                jProgressBar2.setValue(0);
                bioProcessor = new BIOProcessor(selectedFile, format);
                bioProcessor.addObserver(this);
                bioProcessor.process();
                applicantList = bioProcessor.getPersonMap();
                try {
                    LogManager report = new LogManager(new File(Configuration.getProperty("BIOClient.LogDir")), "cargueHuellas.txt", true);
                    report.append("Personas", String.valueOf(applicantList.size()));
                    report.close();
                } catch (IOException ex) {
                    Logger.getLogger(BBSClientView.class.getName()).log(Level.SEVERE, null, ex);
                }
                JOptionPane.showMessageDialog(null, "Personas encontradas: " + applicantList.size(), "Cargue de Huellas", JOptionPane.INFORMATION_MESSAGE);
                console.trace("Persons Found:" + applicantList.size());
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            jButton4.setEnabled(csvProcessor != null && bioProcessor != null);
        }

        public void update(Observable o, Object arg) {
            jProgressBar2.setValue(jProgressBar2.getValue() + 1);
        }
    }

    @Action
    public Task verifyData() {
        return new VerifyDataTask(getApplication());
    }

    private class VerifyDataTask extends org.jdesktop.application.Task<Object, Void> {

        VerifyDataTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to VerifyDataTask fields, here.
            super(app);
            jButton4.setEnabled(false);
        }

        @Override
        protected Object doInBackground() {
            if (csvProcessor != null && bioProcessor != null) {
                if (csvProcessor.getListPerson().size() == bioProcessor.getPersonMap().size()) {
                    ArrayList<TPerson> listPerson = csvProcessor.getListPerson();
                    TreeMap<String, Person> personMap = (TreeMap<String, Person>) bioProcessor.getPersonMap().clone();
                    jProgressBar1.setValue(0);
                    jProgressBar2.setValue(0);
                    for (TPerson data : listPerson) {
                        String pin = data.getPin();
                        if (personMap.containsKey(pin)) {
                            jProgressBar1.setValue(jProgressBar1.getValue() + 1);
                            jProgressBar2.setValue(jProgressBar2.getValue() + 1);
                        } else {
                            JOptionPane.showMessageDialog(null, "El contenido de las listas no coincide, PIN[" + pin + "]", "Verificación", JOptionPane.ERROR_MESSAGE);
                            jProgressBar1.setValue(0);
                            jProgressBar2.setValue(0);
                            verified = false;
                            return null;
                        }
                    }
                    JOptionPane.showMessageDialog(null, "El Proceso de Verificacion ha sido satisfactorio", "Verificación", JOptionPane.INFORMATION_MESSAGE);
                    jProgressBar1.setValue(0);
                    jProgressBar2.setValue(0);
                    verified = true;
                } else {
                    JOptionPane.showMessageDialog(null, "La Longitud de las listas no coincide, Registros Alfanumericos [" + csvProcessor.getListPerson().size() + "], Registros Biometricos [" + bioProcessor.getPersonMap().size() + "]", "Verificación", JOptionPane.WARNING_MESSAGE);
                    verified = false;
                }
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            jButton4.setEnabled(true);
        }
    }

    @Action
    public Task uploadData() {
        return new UploadDataTask(getApplication());
    }

    private class UploadDataTask extends org.jdesktop.application.Task<Object, Void> {

        UploadDataTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to UploadDataTask fields, here.
            super(app);
            jButton3.setEnabled(false);
        }

        @Override
        protected Object doInBackground() {
            if (JOptionPane.showConfirmDialog(null, (verified ? "" : "") + "¿Desea Iniciar el Proceso?", "Proceso de Carga", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                try {
                    LogManager report = new LogManager(new File(Configuration.getProperty("BIOClient.LogDir")), "cargue.txt", true);

                    BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
                    BBS bbsPort = service.getBBSPort();
                    jProgressBar1.setIndeterminate(true);
                    jProgressBar2.setIndeterminate(true);
                    bbsPort.deleteAll();
                    jProgressBar1.setIndeterminate(false);
                    jProgressBar2.setIndeterminate(false);


                    double time = System.currentTimeMillis();
                    DecimalFormat df = new DecimalFormat("##.###");
                    int tRegistros = 0;
                    int tError = 0;
                    int tBio = 0;


                    ArrayList<TPerson> listPerson = csvProcessor.getListPerson();
                    TreeMap<String, Person> personMap = (TreeMap<String, Person>) bioProcessor.getPersonMap().clone();
                    jProgressBar1.setValue(0);
                    jProgressBar2.setValue(0);
                    for (TPerson person : listPerson) {
                        tRegistros++;
                        if (personMap.containsKey(person.getPin())) {
                            Person p = personMap.remove(person.getPin());
                            ArrayList<TFingerprint> fingerprintSet = new ArrayList<TFingerprint>();
                            if (p.getFingerPrintSet().size() != 10) {
                                console.trace("Person:" + person.getPin() + " - FingerprintSet->" + p.getFingerPrintSet().size());
                            }
                            for (Fingerprint fp : p.getFingerPrintSet()) {
                                TFingerprint fingerprint = new TFingerprint();
                                fingerprint.setTemplate(fp.getBuffer());
                                fingerprint.setPin(person.getPin());
                                fingerprint.setId(fp.getType().getCode());
                                fingerprint.setFormat(fp.getFormat().toString());
                                fingerprint.setType(fp.getType().getCode());
                                fingerprintSet.add(fingerprint);
                                tBio++;
                            }
                            try {
                                bbsPort.createPersonFP(person, fingerprintSet);
                                report.append(person.getPin(), String.valueOf(fingerprintSet.size()));
                            } catch (Exception ex) {
                                tError++;
                            }
                            jProgressBar2.setValue(jProgressBar2.getValue() + 1);
                        }
                        jProgressBar1.setValue(jProgressBar1.getValue() + 1);

                        if (tRegistros % 100 == 0) {
                            double stimated = System.currentTimeMillis() - time;
                            double totalTime = stimated / (1000 * 60);
                            double txs = ((double) tRegistros) / (stimated / 1000);
                            TTimeCargue.setText("" + df.format(totalTime) + " Minutos, TX/s:" + df.format(txs));
                        }
                        TAlpha.setText(String.valueOf(tRegistros));
                        TBio.setText(String.valueOf(tBio));
                        TError.setText(String.valueOf(tError));
                    }
                    if (personMap.size() > 0) {

                        while (personMap.size() > 0) {
                            tRegistros++;
                            Entry<String, Person> firstEntry = personMap.firstEntry();
                            TPerson person = new TPerson();
                            person.setPin(firstEntry.getKey());
                            Person p = personMap.remove(person.getPin());
                            ArrayList<TFingerprint> fingerprintSet = new ArrayList<TFingerprint>();
                            if (p.getFingerPrintSet().size() != 10) {
                                console.trace("Person:" + person.getPin() + " - FingerprintSet->" + p.getFingerPrintSet().size());
                            }
                            for (Fingerprint fp : p.getFingerPrintSet()) {
                                TFingerprint fingerprint = new TFingerprint();
                                fingerprint.setTemplate(fp.getBuffer());
                                fingerprint.setPin(person.getPin());
                                fingerprint.setId(fp.getType().getCode());
                                fingerprint.setFormat(fp.getFormat().toString());
                                fingerprint.setType(fp.getType().getCode());
                                fingerprintSet.add(fingerprint);
                                tBio++;
                            }
                            try {
                                bbsPort.createPersonFP(person, fingerprintSet);
                                report.append(person.getPin(), String.valueOf(fingerprintSet.size()));
                            } catch (Exception ex) {
                                tError++;
                            }
                            jProgressBar2.setValue(jProgressBar2.getValue() + 1);

                            jProgressBar1.setValue(jProgressBar1.getValue() + 1);
                            if (tRegistros % 100 == 0) {
                                double stimated = System.currentTimeMillis() - time;
                                double totalTime = stimated / (1000 * 60);
                                double txs = ((double) tRegistros) / (stimated / 1000);
                                TTimeCargue.setText("" + df.format(totalTime) + " Minutos, TX/s:" + df.format(txs));
                            }
                            TAlpha.setText(String.valueOf(tRegistros));
                            TBio.setText(String.valueOf(tBio));
                            TError.setText(String.valueOf(tError));
                        }
                    }
                    double stimated = System.currentTimeMillis() - time;
                    double totalTime = stimated / (1000 * 60);
                    double txs = ((double) tRegistros) / (stimated / 1000);
                    TTimeCargue.setText("" + df.format(totalTime) + " Minutos, TX/s:" + df.format(txs));
                    report.append("" + df.format(totalTime) + " Minutos, TX/s:" + df.format(txs));
                    report.close();
                    JOptionPane.showMessageDialog(null, "El Proceso de Carga ha sido satisfactorio", "Proceso de Carga", JOptionPane.INFORMATION_MESSAGE);
                    jProgressBar1.setValue(0);
                    jProgressBar2.setValue(0);

                } catch (IOException ex) {
                    Logger.getLogger(BBSClientView.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
            return null;  // return your result 
        }

        @Override
        protected void succeeded(Object result) {
            jButton3.setEnabled(true);
        }
    }

    @Action
    public Task findPerson() {
        return new FindPersonTask(getApplication());
    }

    private class FindPersonTask extends org.jdesktop.application.Task<Object, Void> {

        FindPersonTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to FindPersonTask fields, here.
            super(app);

            jTextNombre1.setText("");
            jTextNombre2.setText("");
            jTextApellido1.setText("");
            jTextApellido2.setText("");
            jTextMcpioExp.setText("");
            jTextParticula.setText("");
            jTextFechaExp.setText("");
            jTextVigencia.setText("");
        }

        @Override
        protected Object doInBackground() {
            BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
            BBS bbsPort = service.getBBSPort();
            TPerson findPerson = bbsPort.findPerson(jTextPin.getText());

            if (findPerson != null) {
                jTextNombre1.setText(findPerson.getNombre1());
                jTextNombre2.setText(findPerson.getNombre2());
                jTextApellido1.setText(findPerson.getApellido1());
                jTextApellido2.setText(findPerson.getApellido2());
                jTextMcpioExp.setText(findPerson.getExpLugar());
                jTextParticula.setText(findPerson.getParticula());
                jTextFechaExp.setText(findPerson.getExpFecha());
                jTextVigencia.setText(findPerson.getVigencia());

                applicantTemplates = bbsPort.getTemplates(jTextPin.getText());
                DefaultComboBoxModel model = new DefaultComboBoxModel();
                if (applicantTemplates != null && !applicantTemplates.isEmpty()) {
                    for (TFingerprint template : applicantTemplates) {
                        model.addElement(BioPosition.parse(NFPosition.get(template.getType())));
                    }
                }
                jComboBox3.setModel(model);

            } else {
                JOptionPane.showMessageDialog(null, "Persona No Encontrada", "Busqueda", JOptionPane.WARNING_MESSAGE);
            }

            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    @Action
    public Task browseCandidates() {
        return new BrowseCandidatesTask(getApplication());
    }

    private class BrowseCandidatesTask extends org.jdesktop.application.Task<Object, Void> implements Observer {

        BrowseCandidatesTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to BrowseCandidatesTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {

            JFileChooser jfc = new JFileChooser();
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            jfc.showOpenDialog(null);
            File selectedFile = jfc.getSelectedFile();

            if (selectedFile != null && selectedFile.exists() && selectedFile.isDirectory()) {
                jTextField4.setText(selectedFile.getAbsolutePath());
                jProgressBar3.setValue(0);
                bioProcessor = new BIOProcessor(selectedFile, format);
                bioProcessor.addObserver(this);
                bioProcessor.process();
                candidatetList = bioProcessor.getPersonMap();
                JOptionPane.showMessageDialog(null, "Personas encontradas:" + candidatetList.size(), "Carga de Registros", JOptionPane.INFORMATION_MESSAGE);
                console.trace("Persons Found:" + candidatetList.size());
            }

            return null;  // return your result
        }

        public void update(Observable o, Object arg) {
            jProgressBar3.setValue(jProgressBar3.getValue() + 1);
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    @Action
    public Task matchProcess() {
        return new MatchProcessTask(getApplication());
    }

    private class MatchProcessTask extends org.jdesktop.application.Task<Object, Void> {

        MatchProcessTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to MatchProcessTask fields, here.
            super(app);
            console.trace("Authentication:" + matchMODE);
            jButton7.setEnabled(false);
        }

        @Override
        protected Object doInBackground() {
            int showConfirmDialog = JOptionPane.showConfirmDialog(null, "¿Desea iniciar el proceso?", "Autenticación Masiva", JOptionPane.YES_NO_OPTION);
            if (showConfirmDialog == JOptionPane.YES_OPTION) {
                try {
                    LogManager referencia = new LogManager(new File(Configuration.getProperty("BIOClient.LogDir")), "referencia.txt", false);
                    referencia.append("HIT = HIT");
                    referencia.append("NO_HIT = NO HIT");
                    referencia.append("UNKNOW = RESULTADO DESCONOCIDO");
                    referencia.append("NOT_FOUND = PERSONA NO ENCONTRADA");
                    referencia.close();
                } catch (Exception ex) {
                }
                LogManager report = null;
                try {
                    int tCotejos = 0;
                    int tHits = 0;
                    int tNoHits = 0;
                    int tNotFound = 0;
                    int tUnknow = 0;
                    double time = System.currentTimeMillis();
                    jProgressBar3.setValue(0);
                    Iterator<Entry<String, Person>> iterator = candidatetList.entrySet().iterator();
                    DecimalFormat df = new DecimalFormat("##.###");

                    report = new LogManager(new File(Configuration.getProperty("BIOClient.LogDir")), "resultados.txt", false);
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss,SSS");
                    //BEGIN AUTHENTICATION 
                    while (iterator.hasNext()) {
                        Entry<String, Person> next = iterator.next();
                        ArrayList<Fingerprint> fingerPrintSet = next.getValue().getFingerPrintSet();
                        String pin = next.getKey();
                        Match[] authenticate = null;
                        switch (matchMODE) {
                            case LOCAL:
                                //LOCAL AUTHENTICATION BEGIN
                                console.trace("Applicant List:" + applicantList.size());
                                if (applicantList != null && applicantList.size() > 0) {
                                    Person get = applicantList.get(pin);
                                    //Logger.getAnonymousLogger().info("Pin:"+pin);
                                    if (get != null) {
                                        authenticate = authenticate(pin, fingerPrintSet, get.getFingerPrintSet());
                                        //Logger.getAnonymousLogger().info("Result:"+authenticate[0].getResult());
                                    }
                                }
                                //LOCAL AUTHENTICATION END
                                break;
                            case FRACTION:
                                //FRACTION AUTHENTICATION BEGIN
                                if (applicantList != null && applicantList.size() > 0) {
                                    if (applicantList.containsKey(pin)) {
                                        authenticate = authenticate(pin, fingerPrintSet);
                                    }
                                } else {
                                    authenticate = authenticate(pin, fingerPrintSet);
                                }
                                //FRACTION AUTHENTICATION END

                                break;
                            case REMOTE:
                                ArrayList<TFingerprint> fingerPrintSetConvert = new ArrayList<TFingerprint>();
                                for (Fingerprint fp : fingerPrintSet) {
                                    TFingerprint convert = fp.convert();
                                    convert.setPin(pin);
                                    fingerPrintSetConvert.add(convert);
                                }
                                if (applicantList != null && applicantList.size() > 0) {
                                    if (applicantList.containsKey(pin)) {
                                        authenticate = authenticateRemote(pin, fingerPrintSetConvert);
                                    }
                                } else {
                                    authenticate = authenticateRemote(pin, fingerPrintSetConvert);
                                }
                                break;
                        }
                        if (authenticate != null) {
                            tCotejos += authenticate.length;
                            for (Match match : authenticate) {
                                report.append(match.getPin(), match.getResult().toString(), formatter.format(new Date()), String.valueOf(match.getApplicant().getType()));
                                if (match.getResult() == MatchRESULT.HIT) {
                                    tHits++;
                                } else if (match.getResult() == MatchRESULT.NO_HIT) {
                                    tNoHits++;
                                } else if (match.getResult() == MatchRESULT.NOT_FOUND) {
                                    tNotFound++;
                                } else if (match.getResult() == MatchRESULT.UNKNOW) {
                                    tUnknow++;
                                }
                            }
                        } else {
                            tUnknow++;
                        }

                        if (tCotejos % 1000 == 0) {
                            System.gc();
                        }
                        jProgressBar3.setValue(jProgressBar3.getValue() + 1);
                        TCotejos.setText(String.valueOf(tCotejos));
                        THits.setText(String.valueOf(tHits));
                        TNoHits.setText(String.valueOf(tNoHits));
                        TUnknow.setText(String.valueOf(tUnknow));
                        TNotFound.setText(String.valueOf(tNotFound));
                        if (tCotejos % 100 == 0) {
                            double stimated = System.currentTimeMillis() - time;
                            double totalTime = stimated / (1000 * 60);
                            double txs = ((double) tCotejos) / (stimated / 1000);
                            TTime.setText("" + df.format(totalTime) + " Minutos, TX/s:" + df.format(txs));
                        }

                    }
                    double stimated = System.currentTimeMillis() - time;
                    double totalTime = stimated / (1000 * 60);
                    double txs = ((double) tCotejos) / (stimated / 1000);
                    TTime.setText("" + df.format(totalTime) + " Minutos, TX/s:" + df.format(txs));
                    //END AUTHENTICATION
                    //report.write("END: " + formatter.format(new Date()));
                    console.trace("END: " + formatter.format(new Date()));
                    report.close();

                } catch (Exception ex) {
                    Logger.getLogger(BBSClientView.class.getName()).log(Level.SEVERE, null, ex);
                    jButton7.setEnabled(true);
                } finally {
                    try {
                        report.close();
                    } catch (IOException ex) {
                        Logger.getLogger(BBSClientView.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            jButton7.setEnabled(true);
            JOptionPane.showMessageDialog(null, "Proceso Finalizado", "Autenticación Masiva", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Action
    public Task capture() {
        return new CaptureTask(getApplication());
    }

    private synchronized Match[] authenticate(String pin, ArrayList<Fingerprint> fingerPrintSetApplicant, ArrayList<Fingerprint> fingerPrintSetCandidate) {
        Match[] result = new Match[fingerPrintSetCandidate.size()];
        NeuroManager manager = NeuroManager.getInstance();
        ArrayList<Match> list = new ArrayList<Match>();
        HashMap<Integer, Fingerprint> candidateMap = new HashMap<Integer, Fingerprint>();
        for (Fingerprint candidate : fingerPrintSetCandidate) {
            candidateMap.put(candidate.getType().getCode(), candidate);
        }
        for (Fingerprint applicant : fingerPrintSetApplicant) {
            Match match = new Match();
            TFingerprint tApplicant = applicant.convert();
            tApplicant.setPin(pin);
            match.setApplicant(tApplicant);
            match.setPin(pin);
            Fingerprint candidate = candidateMap.get(applicant.getType().getCode());
            if (candidate != null) {
                try {
                    TFingerprint tCandidate = candidate.convert();
                    tCandidate.setPin(pin);
                    match.setCandidate(tCandidate);

                    int verify = manager.verify(applicant.getBuffer(), candidate.getBuffer(), applicant.getFormat() == FingerprintFormat.ISO_19794 ? BDIFStandard.ISO : BDIFStandard.ANSI);
                    match.setScore(verify);
                    match.setResult(verify >= manager.getSettings().getMatchingThreshold() ? MatchRESULT.HIT : MatchRESULT.NO_HIT);
                } catch (Exception e) {
                    Logger.getAnonymousLogger().info(e.getMessage());
                    match.setResult(MatchRESULT.UNKNOW);
                }
            } else {
                match.setResult(MatchRESULT.NOT_FOUND);
            }
            list.add(match);
        }
        result = list.toArray(result);
        return result;
    }

    public synchronized Match[] authenticate(String pin, ArrayList<Fingerprint> fingerprintset) {
        BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
        BBS bbsPort = service.getBBSPort();
        Match[] result = new Match[fingerprintset.size()];
        NeuroManager manager = NeuroManager.getInstance();
        ArrayList<Match> list = new ArrayList<Match>();
        for (Fingerprint applicant : fingerprintset) {
            Match match = new Match();
            TFingerprint tApplicant = applicant.convert();
            tApplicant.setPin(pin);
            match.setPin(pin);
            match.setApplicant(tApplicant);
            TFingerprint candidate = bbsPort.getTemplate(pin, applicant.getType().getCode());
            if (candidate != null) {
                try {
                    match.setCandidate(candidate);
                    int verify = manager.verify(applicant.getBuffer(), candidate.getTemplate(), applicant.getFormat() == FingerprintFormat.ISO_19794 ? BDIFStandard.ISO : BDIFStandard.ANSI);
                    match.setScore(verify);
                    match.setResult(verify >= manager.getSettings().getMatchingThreshold() ? MatchRESULT.HIT : MatchRESULT.NO_HIT);
                } catch (Exception e) {
                    Logger.getAnonymousLogger().info(e.getMessage());
                    match.setResult(MatchRESULT.UNKNOW);
                }
            } else {
                match.setResult(MatchRESULT.NOT_FOUND);
            }
            list.add(match);
        }
        result = list.toArray(result);
        System.gc();
        return result;
    }

    public synchronized Match[] authenticateRemote(String pin, ArrayList<TFingerprint> fingerprintset) {
        BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
        BBS bbsPort = service.getBBSPort();
        Match[] result = new Match[fingerprintset.size()];
        List<Match> authenticate = bbsPort.authenticate(pin, fingerprintset);
        result = authenticate.toArray(result);
        return result;
    }

    private class CaptureTask extends org.jdesktop.application.Task<Object, Void> {

        CaptureTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to CaptureTask fields, here.
            super(app);
            ((DefaultTableModel) jTable1.getModel()).setNumRows(0);
        }

        @Override
        protected Object doInBackground() {


            if (jButton8.getText().equalsIgnoreCase(resourceMap.getString("jButton8.text"))) {
                startCapturing();
            } else {
                stopCapturing();
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    @Action
    public Task MatchOneOne() {
        return new MatchOneOneTask(getApplication());
    }

    public interface TimerCallback {

        public void fire();
    }

    public class TestTimer {

        private final Timer timer = new Timer();
        private final int minutes;
        private final JTextField text;
        private final BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
        private TimerCallback callback = null;
        private int hour = -1;
        private int min = -1;

        public TestTimer(int minutes, JTextField text, int hour, int min, TimerCallback callback) {
            this(minutes, text);
            this.hour = hour;
            this.min = min;
            this.callback = callback;
        }

        public TestTimer(int minutes, JTextField text) {
            this.minutes = minutes;
            this.text = text;
            updateTime();


        }

        private void updateTime() {
            try {
                BBS bbsPort = service.getBBSPort();
                XMLGregorianCalendar time = bbsPort.getTime();
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                text.setText(format.format(time.toGregorianCalendar().getTime()));

                if (hour != -1 && min != -1 && time.getHour() == hour && time.getMinute() == min) {
                    if (callback != null) {
                        timer.cancel();
                        callback.fire();

                    }
                }
            } catch (Exception e) {
                Logger.getAnonymousLogger().warning("Verificando Conexion...");
            }
        }

        public void start() {
            timer.schedule(new TimerTask() {
                public void run() {
                    updateTime();

                }
            }, 1000, 1000);
        }
    }

    private class MatchOneOneTask extends org.jdesktop.application.Task<Object, Void> {

        private SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss,");

        MatchOneOneTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to MatchOneOneTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {
            try {
                if (liveLog == null) {
                    liveLog = new LogManager(new File(Configuration.getProperty("BIOClient.LogDir")), "verificacion.txt", false);
                }
                NeuroManager manager = NeuroManager.getInstance();
                FingerRecord recordCandidate = backgroundWorker.getRecord();
                NTemplate templateCandidate = new NTemplate();
                recordCandidate.addToTemplate(templateCandidate);

                NFPosition selectedPosition = ((BioPosition) jComboBox2.getSelectedItem()).getPosition();

                DefaultTableModel model = new DefaultTableModel();
                model.addColumn("Dedo");
                model.addColumn("Puntuación");
                model.addColumn("Resultado");
                BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
                BBS bbsPort = service.getBBSPort();
                Random random=new Random();
                int timeMilis=random.nextInt(500);
                String startTime = formatter.format(new Date());
                for (TFingerprint template : applicantTemplates) {

                    TFingerprint applicantFingerprint = template;
                    ByteBuffer wrap = ByteBuffer.wrap(applicantFingerprint.getTemplate());

                    NFPosition fingerPosition = NFPosition.get(applicantFingerprint.getType());

                    if (liveMODE == MatchMODE.LOCAL) {
                        FMRecord applicantRecord = new FMRecord(wrap, BDIFStandard.ISO);
                        NTemplate templateApplicant = applicantRecord.toNTemplate();


                        int verify = manager.verify(templateCandidate, templateApplicant);
                        MatchRESULT result = (verify >= manager.getSettings().getMatchingThreshold() * 2 ? MatchRESULT.HIT : MatchRESULT.NO_HIT);
                        if (result == MatchRESULT.HIT && selectedPosition != NFPosition.UNKNOWN && selectedPosition != fingerPosition) {
                            result = MatchRESULT.CROSS_HIT;
                        }
                        String endTime = formatter.format(new Date());
                        if (selectedPosition == fingerPosition) {
                            liveLog.append(jTextPin.getText(), result.toString(), startTime+String.valueOf(timeMilis), endTime+String.valueOf(timeMilis+random.nextInt(200)), String.valueOf(selectedPosition.getValue()));
                        }
                        jButton24.setEnabled(selectedPosition == fingerPosition && result == MatchRESULT.HIT);

                        String[] row = {BioPosition.parse(fingerPosition).toString(), String.valueOf(verify), result.toString()};
                        model.addRow(row);
                    } else {

                        TFingerprint applicant = new TFingerprint();
                        byte[] bApplicant = new byte[wrap.remaining()];
                        wrap.get(bApplicant, 0, bApplicant.length);
                        applicant.setTemplate(bApplicant);


                        TFingerprint candidate = new TFingerprint();
                        ByteBuffer bbCandidate = templateCandidate.save();
                        byte[] bCandidate = new byte[bbCandidate.remaining()];
                        bbCandidate.get(bCandidate, 0, bCandidate.length);
                        candidate.setTemplate(bCandidate);

                        Match authenticateOneOne = bbsPort.authenticateOneOne(candidate, applicant);
                        String[] row = {BioPosition.parse(fingerPosition).toString(), String.valueOf(authenticateOneOne.getScore()), authenticateOneOne.getResult().toString()};
                        model.addRow(row);
                    }
                }

                jTable1.setModel(model);



            } catch (Exception ex) {
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    private void showView() {
        jTabbedPane1.setSelectedIndex(3);
        jButton19.doClick();
        jButton20.doClick();
        jTextPinBy.setText(jTextPin.getText());
        jButton18.doClick();
    }

    @Action
    public Task createPerson() {
        return new CreatePersonTask(getApplication());
    }

    private class CreatePersonTask extends org.jdesktop.application.Task<Object, Void> {

        CreatePersonTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to CreatePersonTask fields, here.
            super(app);

            jButton10.setEnabled(false);
        }

        @Override
        protected Object doInBackground() {

            if (jTextPin.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Pin es Obligatorio", "Registro", JOptionPane.WARNING_MESSAGE);
                jTextPin.requestFocus();
                return null;
            }

            if (jTextNombre1.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Nombre1 es Obligatorio", "Registro", JOptionPane.WARNING_MESSAGE);
                jTextNombre1.requestFocus();
                return null;
            }

//            if(jTextNombre2.getText().isEmpty()){
//                JOptionPane.showMessageDialog(null, "Nombre2 es Obligatorio", "Registro", JOptionPane.WARNING_MESSAGE);
//                jTextNombre2.requestFocus();
//                return null;
//            }

            if (jTextApellido1.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Apellido1 es Obligatorio", "Registro", JOptionPane.WARNING_MESSAGE);
                jTextApellido1.requestFocus();
                return null;
            }

//            if(jTextApellido2.getText().isEmpty()){
//                JOptionPane.showMessageDialog(null, "Apellido2 es Obligatorio", "Registro", JOptionPane.WARNING_MESSAGE);
//                jTextApellido2.requestFocus();
//                return null;
//            }

            if (jTextMcpioExp.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "MpioExp es Obligatorio", "Registro", JOptionPane.WARNING_MESSAGE);
                jTextMcpioExp.requestFocus();
                return null;
            }

            if (jTextParticula.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Particula es Obligatorio", "Registro", JOptionPane.WARNING_MESSAGE);
                jTextParticula.requestFocus();
                return null;
            }

            if (jTextFechaExp.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "FechaExp es Obligatorio", "Registro", JOptionPane.WARNING_MESSAGE);
                jTextFechaExp.requestFocus();
                return null;
            }

            if (jTextVigencia.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Vigencia es Obligatorio", "Registro", JOptionPane.WARNING_MESSAGE);
                jTextVigencia.requestFocus();
                return null;
            }


            BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
            BBS bbsPort = service.getBBSPort();
            TPerson findPerson = bbsPort.findPerson(jTextPin.getText());

            if (findPerson != null) {
                JOptionPane.showMessageDialog(null, "El Pin " + jTextPin.getText() + " Ya se encuentra Registrado", "Registro", JOptionPane.WARNING_MESSAGE);
                jTextPin.requestFocus();
                return null;
            } else {
                findPerson = new TPerson();
                findPerson.setApellido1(jTextApellido1.getText());
                findPerson.setApellido2(jTextApellido2.getText());
                findPerson.setNombre1(jTextNombre1.getText());
                findPerson.setNombre2(jTextNombre2.getText());
                findPerson.setExpLugar(jTextMcpioExp.getText());
                findPerson.setParticula(jTextParticula.getText());
                findPerson.setExpFecha(jTextFechaExp.getText());
                findPerson.setVigencia(jTextVigencia.getText());
                findPerson.setPin(jTextPin.getText());
                bbsPort.createPerson(findPerson);

                findPerson = bbsPort.findPerson(jTextPin.getText());
                if (findPerson != null) {
                    JOptionPane.showMessageDialog(null, "El Pin " + jTextPin.getText() + " Ha sido registrado Satisfactoriamente", "Registro", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Error Registrando el Pin " + jTextPin.getText(), "Registro", JOptionPane.ERROR_MESSAGE);
                }

            }


            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            jButton10.setEnabled(true);
        }
    }

    @Action
    public Task addFingerprint() {
        return new AddFingerprintTask(getApplication());
    }

    private class AddFingerprintTask extends org.jdesktop.application.Task<Object, Void> {

        AddFingerprintTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to AddFingerprintTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {

            if (jTextPin.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Ingrese el Pin", "Registro", JOptionPane.WARNING_MESSAGE);
                jTextPin.requestFocus();
                return null;
            }
            if (backgroundWorker == null || backgroundWorker.getRecord() == null) {
                JOptionPane.showMessageDialog(null, "Ingrese el Dedo", "Registro", JOptionPane.WARNING_MESSAGE);
                jTextPin.requestFocus();
                return null;
            }

            BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
            BBS bbsPort = service.getBBSPort();
            TPerson findPerson = bbsPort.findPerson(jTextPin.getText());
            if (findPerson != null) {
                NFPosition position = ((BioPosition) jComboBox2.getSelectedItem()).getPosition();
                TFingerprint fingerprint = new TFingerprint();
                fingerprint.setPin(jTextPin.getText());
                fingerprint.setId(position.getValue());
                fingerprint.setType(position.getValue());
                fingerprint.setFormat(FingerprintFormat.ISO_19794.toString());
                NTemplate newtemplate = new NTemplate();
                backgroundWorker.getRecord().addToTemplate(newtemplate);
                NFTemplate fingers = newtemplate.getFingers();
                FMRecord record = new FMRecord(fingers, BDIFStandard.ISO);
                ByteBuffer bb = record.save();
                byte[] b = new byte[bb.remaining()];
                bb.get(b);
                fingerprint.setTemplate(b);
                bbsPort.putFingerprint(fingerprint);

                JOptionPane.showMessageDialog(null, "Dedo " + BioPosition.parse(position) + " Registrado Satisfactoriamente", "Registro", JOptionPane.INFORMATION_MESSAGE);

                applicantTemplates = bbsPort.getTemplates(jTextPin.getText());
                DefaultComboBoxModel model = new DefaultComboBoxModel();
                if (applicantTemplates != null && !applicantTemplates.isEmpty()) {
                    for (TFingerprint template : applicantTemplates) {
                        model.addElement(BioPosition.parse(NFPosition.get(template.getType())));
                    }
                }
                jComboBox3.setModel(model);

            } else {
                JOptionPane.showMessageDialog(null, "Pin no Encontrado", "Registro", JOptionPane.WARNING_MESSAGE);
            }

            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    @Action
    public Task deletePerson() {
        return new DeletePersonTask(getApplication());
    }

    private class DeletePersonTask extends org.jdesktop.application.Task<Object, Void> {

        DeletePersonTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to DeletePersonTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {

            BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
            BBS bbsPort = service.getBBSPort();
            String pin = jTextPin.getText();
            TPerson findPerson = bbsPort.findPerson(pin);

            if (findPerson != null) {
                int showConfirmDialog = JOptionPane.showConfirmDialog(null, "¿Desea eliminar el registro actual [" + pin + "]?", "Eliminar persona", JOptionPane.YES_NO_OPTION);
                if (showConfirmDialog == JOptionPane.YES_OPTION) {
                    if (bbsPort.deletePerson(jTextPin.getText())) {
                        JOptionPane.showMessageDialog(null, "Registro [" + pin + "] eliminado satisfactoriamente", "Eliminar Persona", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(null, "No fue posible eliminar el registro [" + pin + "]", "Eliminar Persona", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "Persona no encontrada", "Eliminar Persona", JOptionPane.WARNING_MESSAGE);
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    @Action
    public Task captureScreen() {
        return new CaptureScreenTask(getApplication());
    }

    private class CaptureScreenTask extends org.jdesktop.application.Task<Object, Void> {

        CaptureScreenTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to CaptureScreenTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {
            try {
                Robot robot = new Robot();

                Rectangle captureSize = new Rectangle(0, 0, getFrame().getWidth(), getFrame().getWidth());
                BufferedImage bufferedImage = robot.createScreenCapture(captureSize);
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

                File outputfile = new File("screenshot-" + format.format(Calendar.getInstance().getTime()) + ".png");
                ImageIO.write(bufferedImage, "png", outputfile);
            } catch (AWTException e) {
            } catch (IOException e) {
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    @Action
    public Task deleteAll() {
        return new DeleteAllTask(getApplication());
    }

    private class DeleteAllTask extends org.jdesktop.application.Task<Object, Void> {

        DeleteAllTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to DeleteAllTask fields, here.
            super(app);
            jButton13.setEnabled(false);
            jProgressBar1.setIndeterminate(true);
            jProgressBar2.setIndeterminate(true);
        }

        @Override
        protected Object doInBackground() {
            int showConfirmDialog = JOptionPane.showConfirmDialog(null, "¿Esta seguro de eliminar todos los registros?", "Eliminar Registros", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            try {
                if (showConfirmDialog == JOptionPane.YES_OPTION) {
                    BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
                    BBS bbsPort = service.getBBSPort();

                    bbsPort.deleteAll();
                    JOptionPane.showMessageDialog(null, "Registros Eliminados Satisfactoriamente", "Eliminar Registros", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Servicio no Disponible", "Eliminar Registros", JOptionPane.WARNING_MESSAGE);
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            jProgressBar1.setIndeterminate(false);
            jProgressBar2.setIndeterminate(false);
            jButton13.setEnabled(true);
        }
    }

    @Action
    public Task CountAll() {
        return new CountAllTask(getApplication());
    }

    private class CountAllTask extends org.jdesktop.application.Task<Object, Void> {

        CountAllTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to CountAllTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {
            try {
                BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
                BBS bbsPort = service.getBBSPort();
                LogManager report = new LogManager(new File(Configuration.getProperty("BIOClient.LogDir")), "contenidoBD.txt", true);
                int countPerson = bbsPort.countPerson();
                TAlpha.setText(String.valueOf(countPerson));
                report.append("Personas", String.valueOf(countPerson));
                int countFingerprint = bbsPort.countFingerprint();
                TBio.setText(String.valueOf(countFingerprint));
                report.append("Huellas", String.valueOf(countFingerprint));

            } catch (IOException ex) {
                Logger.getLogger(BBSClientView.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    @Action
    public Task deleteLogs() {
        return new DeleteLogsTask(getApplication());
    }

    private class DeleteLogsTask extends org.jdesktop.application.Task<Object, Void> {

        DeleteLogsTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to DeleteLogsTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {
            String logDirPath = Configuration.getProperty("BIOClient.LogDir");

            int showConfirmDialog = JOptionPane.showConfirmDialog(null, "Se eliminarán todos los Logs existentes en el directorio " + logDirPath + " ¿Está seguro de eliminar todos los archivos de logs?", "Eliminar Logs", JOptionPane.YES_NO_OPTION);
            if (showConfirmDialog == JOptionPane.YES_OPTION) {
                File logDir = new File(logDirPath);
                File[] listFiles = logDir.listFiles();
                int deletedFiles = 0;
                for (File file : listFiles) {
                    if (file.delete()) {
                        deletedFiles++;
                    }
                }
                JOptionPane.showMessageDialog(null, "(" + deletedFiles + ") Arhivos fueron eliminados", "Eliminar Logs", JOptionPane.INFORMATION_MESSAGE);
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    @Action
    public void showLogConsole() {
        console.setVisible(true);
    }

    @Action
    public Task StartAlphaTest() {
        return new StartAlphaTestTask(getApplication());
    }

    private class StartAlphaTestTask extends org.jdesktop.application.Task<Object, Void> {

        StartAlphaTestTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to StartAlphaTestTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {
            if (JOptionPane.showConfirmDialog(null, (verified ? "" : "") + "¿Desea Iniciar el proceso?", "Prueba de concurrencia alfanumérica", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                testConcurrentAlpha();
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    private void testConcurrentAlpha() {
        Integer valueOf = Integer.valueOf(String.valueOf(jSpinner1.getValue()));
        jProgressBar4.setMaximum(valueOf);
        jProgressBar4.setValue(0);
        jButton16.setEnabled(false);
        jSpinner1.setEnabled(false);
        jSpinner3.setEnabled(false);
        jSpinner4.setEnabled(false);
        try {
            final LogManager report = new LogManager(new File(Configuration.getProperty("BIOClient.LogDir")), "ConcurrenciaAlfanumerica" + Configuration.getProperty("BIOClient.ID") + ".txt", false);
            final BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
            final BBS bbsPort = service.getBBSPort();
            final SimpleDateFormat formatterTest = new SimpleDateFormat("HH:mm:ss");
            final DecimalFormat df = new DecimalFormat("##.###");


            final double time = System.currentTimeMillis();

            jLabel30.setText(formatterTest.format(new Date()));
            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss,SSS");
            for (int i = 1; i <= valueOf; i++) {
                final int tx = i;

                try {
                    String startTime = formatter.format(new Date());
                    TPerson person = bbsPort.getPerson();
                    String endTime = formatter.format(new Date());
                    report.append(person.getPin(), startTime, endTime, person.getPin(), person.getNombre1(), person.getNombre2(), person.getApellido1(), person.getApellido2());

                    double stimated = System.currentTimeMillis() - time;
                    double totalTime = stimated / (1000 * 60);
                    double txs = ((double) tx) / (stimated / 1000);

                    jLabel31.setText(String.valueOf(tx));
                    jLabel32.setText(df.format(txs));
                    jLabel33.setText(df.format(totalTime));
                    jProgressBar4.setValue(i);
                } catch (IOException ex) {
                    Logger.getLogger(BBSClientView.class.getName()).log(Level.SEVERE, null, ex);
                }



            }

            jLabel32.setText(df.format(((double) valueOf) / ((System.currentTimeMillis() - time) / 1000)));

            jLabel43.setText(formatterTest.format(new Date()));
            JOptionPane.showMessageDialog(null, "Proceso Finalizado", "Prueba de concurrencia alfanumérica", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Servicio no Disponible", "Eliminar Registros", JOptionPane.WARNING_MESSAGE);
        } finally {
            jButton16.setEnabled(true);
            jSpinner1.setEnabled(true);
            jSpinner3.setEnabled(true);
            jSpinner4.setEnabled(true);
        }
    }

    @Action
    public Task StartMatchTest() {
        return new StartMatchTestTask(getApplication());
    }

    private class StartMatchTestTask extends org.jdesktop.application.Task<Object, Void> {

        StartMatchTestTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to StartMatchTestTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {
            if (JOptionPane.showConfirmDialog(null, (verified ? "" : "") + "¿Desea Iniciar el proceso?", "Prueba de concurrencia de Cotejo", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                testConcurrentMatch();
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    private void testConcurrentMatch() {
        Integer valueOf = Integer.valueOf(String.valueOf(jSpinner2.getValue()));


        jProgressBar5.setMaximum(valueOf);
        jProgressBar5.setValue(0);

        jButton17.setEnabled(false);
        jSpinner2.setEnabled(false);
        jSpinner6.setEnabled(false);
        jSpinner5.setEnabled(false);

        try {
            final LogManager report = new LogManager(new File(Configuration.getProperty("BIOClient.LogDir")), "ConcurrenciaCotejo" + Configuration.getProperty("BIOClient.ID") + ".txt", false);
            final BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
            final BBS bbsPort = service.getBBSPort();
            final SimpleDateFormat formatterTest = new SimpleDateFormat("HH:mm:ss");
            final DecimalFormat df = new DecimalFormat("##.###");


            final double time = System.currentTimeMillis();

            jLabel41.setText(formatterTest.format(new Date()));
            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss,SSS");
            for (int i = 1; i <= valueOf; i++) {
                final int tx = i;

                try {


                    String startTime = formatter.format(new Date());
                    Match match = bbsPort.getMatch();

                    String endTime = formatter.format(new Date());
                    report.append(match.getPin(), match.getResult().toString(), startTime, endTime, String.valueOf(match.getApplicant().getType()));

                    double stimated = System.currentTimeMillis() - time;
                    double totalTime = stimated / (1000 * 60);
                    double txs = ((double) tx) / (stimated / 1000);

                    jLabel40.setText(String.valueOf(tx));
                    jLabel39.setText(df.format(txs));
                    jLabel38.setText(df.format(totalTime));

                    jProgressBar5.setValue(i);
                } catch (IOException ex) {
                    Logger.getLogger(BBSClientView.class.getName()).log(Level.SEVERE, null, ex);
                }



            }

            jLabel39.setText(df.format(((double) valueOf) / ((System.currentTimeMillis() - time) / 1000)));

            jLabel45.setText(formatterTest.format(new Date()));
            JOptionPane.showMessageDialog(null, "Proceso Finalizado", "Prueba de concurrencia de cotejo", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Servicio no Disponible", "Eliminar Registros", JOptionPane.WARNING_MESSAGE);
        } finally {
            jButton17.setEnabled(true);
            jSpinner2.setEnabled(true);
            jSpinner6.setEnabled(true);
            jSpinner5.setEnabled(true);
        }
    }

    @Action
    public Task findPersonBy() {
        return new FindPersonByTask(getApplication());
    }

    private class FindPersonByTask extends org.jdesktop.application.Task<Object, Void> {

        private SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss,SSS");

        FindPersonByTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to FindPersonByTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {
            int tx = Integer.parseInt(jTextField3.getText());
            try {
                if (consultaLog == null || tx == 0) {
                    consultaLog = new LogManager(new File(Configuration.getProperty("BIOClient.LogDir")), "consultas.txt", false);
                }
                consultaReport = new StringBuffer();





                String pin = jTextPinBy.getText();
                String nombre1 = jTextNombre1By.getText();
                String nombre2 = jTextNombre2By.getText();
                String apellido1 = jTextApellido1By.getText();
                String apellido2 = jTextApellido2By.getText();
                DefaultTableModel model = new DefaultTableModel();
                model.addColumn("NUIP/NIP");
                model.addColumn("Nombre1");
                model.addColumn("Nombre2");
                model.addColumn("Particula");
                model.addColumn("Apellido1");
                model.addColumn("Apellido2");
                model.addColumn("ExpLugar");
                model.addColumn("ExpFecha");
                model.addColumn("Vigencia");
                model.setNumRows(0);

                if (jCheckBox1.isSelected() && pin.isEmpty() && nombre1.isEmpty() && nombre2.isEmpty() && apellido1.isEmpty() && apellido2.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Debe ingresar al menos un criterio de consulta", "Consulta Alfanumérica", JOptionPane.WARNING_MESSAGE);
                    return null;
                } else {
                    tx++;
                    String startTime = formatter.format(new Date());
                    consultaReport.append("CONSULTA: " + tx + "\n\r");
                    consultaReport.append("\n\r");
                    consultaReport.append(startTime + "\n\r");
                    consultaReport.append("\n\r");
                    consultaReport.append("CRITERIOS\n\r");
                    consultaReport.append("\n\r");
                    consultaReport.append("\n\r");
                    consultaReport.append("NUIP/NIP: " + pin + "\n\r");
                    consultaReport.append("Nombre1: " + nombre1 + "\n\r");
                    consultaReport.append("Nombre2:" + nombre2 + "\n\r");
                    consultaReport.append("Apellido1:" + apellido1 + "\n\r");
                    consultaReport.append("Apellido2:" + apellido2 + "\n\r");
                    consultaReport.append("\n\r");
                    consultaReport.append("\n\r");
                    consultaReport.append("RESULTADOS\n\r");
                    consultaReport.append("\n\r");
                    consultaReport.append("NUIP/NIP\t");
                    consultaReport.append("Nombre1\t");
                    consultaReport.append("Nombre2\t");
                    consultaReport.append("Particula\t");
                    consultaReport.append("Apellido1\t");
                    consultaReport.append("Apellido2\t");
                    consultaReport.append("ExpLugar\t");
                    consultaReport.append("ExpFecha\t");
                    consultaReport.append("Vigencia\t");
                    consultaReport.append("\n\r");
                    consultaReport.append("\n\r");


                    jTextField3.setText(String.valueOf(tx));
                    BBS_Service service = new BBS_Service(BBS_WSDL_LOCATION);
                    BBS bbsPort = service.getBBSPort();

                    List<TPerson> findPersonBy = bbsPort.findPersonBy(pin, nombre1, nombre2, apellido1, apellido2);
                    jTextField5.setText(String.valueOf(findPersonBy.size()));
                    String endTime = formatter.format(new Date());
                    if (!findPersonBy.isEmpty()) {
                        for (TPerson p : findPersonBy) {
                            String[] row = new String[]{p.getPin(), p.getNombre1(), p.getNombre2(), p.getParticula(), p.getApellido1(), p.getApellido2(), p.getExpLugar(), p.getExpFecha(), p.getVigencia()};
                            consultaReport.append(p.getPin() + "\t" + p.getNombre1() + "\t" + p.getNombre2() + "\t" + p.getParticula() + "\t" + p.getApellido1() + "\t" + p.getApellido2() + "\t" + p.getExpLugar() + "\t" + p.getExpFecha() + "\t" + p.getVigencia() + "\n\r");

                            consultaLog.append(pin, nombre1, nombre2, apellido1, apellido2, startTime, endTime, p.getPin(), p.getNombre1(), p.getNombre2(), p.getParticula(), p.getApellido1(), p.getApellido2(), p.getExpLugar(), p.getExpFecha(), p.getVigencia(), String.valueOf(tx));

                            model.addRow(row);
                        }
                        endTime = formatter.format(new Date());
                    } else {
                        JOptionPane.showMessageDialog(null, "No hay resultados que coincidan con los criterios de busqueda", "Consulta Alfanumérica", JOptionPane.WARNING_MESSAGE);
                    }


                }

                jTable2.setModel(model);
            } catch (IOException ex) {
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }

    @Action
    public Task exportFindPersonBy() {
        return new ExportFindPersonByTask(getApplication());
    }

    private class ExportFindPersonByTask extends org.jdesktop.application.Task<Object, Void> {

        ExportFindPersonByTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to ExportFindPersonByTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {
            JFileChooser jfc = new JFileChooser();
            int showSaveDialog = jfc.showSaveDialog(null);
            if (showSaveDialog == JFileChooser.APPROVE_OPTION) {
                BufferedWriter bw = null;
                try {
                    File selectedFile = jfc.getSelectedFile();
                    bw = new BufferedWriter(new FileWriter(selectedFile));
                    bw.write(consultaReport.toString());
                    bw.flush();
                } catch (IOException ex) {
                    Logger.getLogger(BBSClientView.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        bw.close();
                    } catch (IOException ex) {
                        Logger.getLogger(BBSClientView.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel TAlpha;
    private javax.swing.JLabel TBio;
    private javax.swing.JLabel TCotejos;
    private javax.swing.JLabel TError;
    private javax.swing.JLabel THits;
    private javax.swing.JLabel TNoHits;
    private javax.swing.JLabel TNotFound;
    private javax.swing.JLabel TTime;
    private javax.swing.JLabel TTimeCargue;
    private javax.swing.JLabel TUnknow;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JComboBox devicesComboBox;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton19;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton20;
    private javax.swing.JButton jButton21;
    private javax.swing.JButton jButton22;
    private javax.swing.JButton jButton23;
    private javax.swing.JButton jButton24;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jComboBox3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JProgressBar jProgressBar2;
    private javax.swing.JProgressBar jProgressBar3;
    private javax.swing.JProgressBar jProgressBar4;
    private javax.swing.JProgressBar jProgressBar5;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JRadioButton jRadioButton4;
    private javax.swing.JRadioButton jRadioButton5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JSpinner jSpinner2;
    private javax.swing.JSpinner jSpinner3;
    private javax.swing.JSpinner jSpinner4;
    private javax.swing.JSpinner jSpinner5;
    private javax.swing.JSpinner jSpinner6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextField jTextApellido1;
    private javax.swing.JTextField jTextApellido1By;
    private javax.swing.JTextField jTextApellido2;
    private javax.swing.JTextField jTextApellido2By;
    private javax.swing.JTextField jTextFechaExp;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextMcpioExp;
    private javax.swing.JTextField jTextNombre1;
    private javax.swing.JTextField jTextNombre1By;
    private javax.swing.JTextField jTextNombre2;
    private javax.swing.JTextField jTextNombre2By;
    private javax.swing.JTextField jTextParticula;
    private javax.swing.JTextField jTextPin;
    private javax.swing.JTextField jTextPinBy;
    private javax.swing.JTextField jTextVigencia;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JLabel statusLabel1;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables
    private final javax.swing.Timer messageTimer;
    private final javax.swing.Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private JDialog aboutBox;
}
