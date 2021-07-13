/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.neuro;

import com.bbs.neuro.fingers.BiometricRecord;
import com.bbs.neuro.util.FingerRecord;
import com.bbs.neuro.fingers.FingersSettings;
import com.bbs.neuro.util.BiometricStandardsConverter;
import com.bbs.neuro.util.LibraryManager;
import com.bbs.neuro.util.LicenseManager;
import com.bbs.neuro.util.MessageUtils;
import com.neurotec.biometrics.NFRecord;
import com.neurotec.biometrics.NMatcher;
import com.neurotec.biometrics.NMatchingSpeed;
import com.neurotec.biometrics.NTemplate;
import com.neurotec.biometrics.standards.BDIFStandard;
import com.neurotec.biometrics.standards.FMRecord;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ProgressMonitor;

/**
 *
 * @author Usuario
 */
class NeuroMatcher implements PropertyChangeListener {

    private ProgressMonitor progressMonitor;
    private List<String> components;
    private NMatcher matcher = null;
    private BiometricStandardsConverter converter = null;
    private List<FingerRecord> fingers;

    public NeuroMatcher() {

        LibraryManager.initLibraryPath();
        components = new ArrayList<String>();
        components.add("NCore");
        components.add("NLicensing");
        components.add("NMedia");
        components.add("NMediaProc");
        components.add("NBiometrics");
        components.add("NBiometricTools");
        components.add("NSmartCards");
        components.add("NBiometricStandards");
        components.add("NDevices");
        components.add("NCluster");
        components.add("NClusterJni");
        LibraryManager.loadLibraries(components);

        progressMonitor = new ProgressMonitor(null, "License obtain", "", 0, LicenseManager.getInstance().getLicenseCount());
        LicenseManager.getInstance().addPropertyChangeListener(this);
        try {
            LicenseManager.getInstance().obtain();
        } catch (Exception e) {
           // MessageUtils.showError(null, e);
            System.out.println(e.getMessage());
            System.exit(0);
        }
        matcher = new NMatcher();
        converter = new BiometricStandardsConverter();
        updateMatcherFingerSettings();

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (LicenseManager.PROGRESS_CHANGED_PROPERTY.equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            progressMonitor.setProgress(progress);
            String message = String.format("# of analyzed licenses: %d\n", progress);
            progressMonitor.setNote(message);
        }
    }

    public synchronized int verify(byte[] template1, byte[] template2, BDIFStandard format) {
        NTemplate nTemplate1 = new FMRecord(toByteBuffer(template1), format).toNTemplate();
        NTemplate nTemplate2 = new FMRecord(toByteBuffer(template2), format).toNTemplate();
        return verify(nTemplate1, nTemplate2);
    }

    private ByteBuffer toByteBuffer(byte[] buffer) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.length);
        byteBuffer.put(buffer);
        byteBuffer.flip();
        return byteBuffer;
    }

    public synchronized int verify(NTemplate template1, NTemplate template2) {
        return matcher.verify(template1.save(), template2.save());
    }
    
    public FingersSettings getSettings(){
        return FingersSettings.getInstance();
    }

    public void updateMatcherFingerSettings(FingersSettings fingerSettings) {
        
        Logger.getAnonymousLogger().info("Matching Settings...");
        
        /*      
        fingerSettings.setMatcherSpeed(NMatchingSpeed.LOW);
        fingerSettings.setMatcherMaximalRotation(45);
        fingerSettings.setMatcherMinimalMatchedFingerCount(10);
        fingerSettings.setMatcherMinimalMatchedFingerThreshold(20);
        fingerSettings.setMatcherMode(0);
        fingerSettings.setMatchingThreshold(15);
        */
        matcher.setFingersMatchingSpeed(fingerSettings.getMatcherSpeed());
        matcher.setFingersMaximalRotation(fingerSettings.getMatcherMaximalRotation());
        matcher.setFingersMinMatchedCount(fingerSettings.getMatcherMinimalMatchedFingerCount());
        matcher.setFingersMinMatchedCountThreshold(fingerSettings.getMatcherMinimalMatchedFingerThreshold());
        matcher.setFingersMode(fingerSettings.getMatcherMode());
        matcher.setMatchingThreshold(fingerSettings.getMatchingThreshold());
        Logger.getAnonymousLogger().info("Matcher speed:"+fingerSettings.getMatcherSpeed());
        Logger.getAnonymousLogger().info("Matcher Max Rotation:"+fingerSettings.getMatcherMaximalRotation());
        Logger.getAnonymousLogger().info("Matcher Min Matched Count:"+fingerSettings.getMatcherMinimalMatchedFingerCount());
        Logger.getAnonymousLogger().info("Matcher Min Matched Threshold:"+fingerSettings.getMatcherMinimalMatchedFingerThreshold());
        Logger.getAnonymousLogger().info("Matcher Mode:"+fingerSettings.getMatcherMode());
        Logger.getAnonymousLogger().info("Matching Threshold:"+fingerSettings.getMatchingThreshold());
        
    }
    
    public void updateMatcherFingerSettings() {        
        updateMatcherFingerSettings(FingersSettings.getInstance());
    }

    public NTemplate getTemplateFromRecords() {
        NTemplate template = new NTemplate();
        if (getFingers() != null) {
            for (BiometricRecord item : getFingers()) {
                item.addToTemplate(template);
            }
        }
        return template;
    }

    public boolean hasNoRecords() {
        return (getFingers().isEmpty());
    }

    public void setRecordsFromTemplate(NTemplate template) {
        if (template.getFingers() != null && template.getFingers().getRecords().size() > 0) {
            List<FingerRecord> list = new ArrayList<FingerRecord>();
            for (NFRecord item : template.getFingers().getRecords()) {
                list.add(new FingerRecord(item));
            }
            setFingers(list);
        }


    }

    public void remove(BiometricRecord record) {
        if (record instanceof FingerRecord) {
            getFingers().remove(record);
        }
    }

    /**
     * @return the fingers
     */
    public List<FingerRecord> getFingers() {
        return fingers;
    }

    /**
     * @param fingers the fingers to set
     */
    public void setFingers(List<FingerRecord> fingers) {
        this.fingers = fingers;
    }
}
