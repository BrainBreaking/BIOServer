/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.neuro.util;

import com.bbs.neuro.fingers.Device;
import com.bbs.neuro.fingers.FingerTools;
import com.bbs.neuro.fingers.Scenario;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NFEExtractionStatus;
import com.neurotec.biometrics.NFExtractor;
import com.neurotec.biometrics.NFImpressionType;
import com.neurotec.biometrics.NFPosition;
import com.neurotec.biometrics.tools.NFSegment;
import com.neurotec.biometrics.tools.NFSegmenter;
import com.neurotec.devices.NFScanner;
import com.neurotec.images.NGrayscaleImage;
import com.neurotec.images.NImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import org.jdesktop.swingworker.SwingWorker;

/**
 *
 * @author Usuario
 */
public class BackgroundWorker extends SwingWorker<Boolean, FingerRecord> {

    private boolean liveCapturing = false;
    private int capturedCount = 0;
    private Device device;
    private Scenario scenario;
    private String fileName;
    private List<NFPosition> positions;
    private List<NFPosition> missingPositions;
    private NFPosition currentPosition;
    private NImage image = null;
    private NGrayscaleImage grayscaleImage = null;
    public static final String CURRENT_FINGER_CHANGED_PROPERTY = "currentPosition";
    private FingerRecord record;
    
    public BackgroundWorker(Device device, Scenario scenario, List<NFPosition> positions, List<NFPosition> missingPositions) {
        this.device = device;
        this.scenario = scenario;
        this.positions = positions;
        this.missingPositions = missingPositions;
        liveCapturing = true;
    }
    
    public BackgroundWorker(Scenario scenario, NFPosition position, String fileName, List<NFPosition> missingPositions) {
        this.scenario = scenario;
        this.fileName = fileName;
        this.positions = new ArrayList<NFPosition>();
        this.positions.add(position);
        this.missingPositions = missingPositions;
    }
    
    @Override
    protected Boolean doInBackground() throws Exception {
        if (scenario.equals(Scenario.PLAIN_FINGER) || scenario.equals(Scenario.ROLLED_FINGER)) {
            capture(NFPosition.UNKNOWN);
        } else {
            for (NFPosition position : positions) {
                capture(position);
            }
        }
        return (positions != null) ? capturedCount == positions.size() : true;
    }
    
    private void capture(NFPosition position) {
        if (isCancelled()) {
            return;
        }
        try {
            FingerRecord record;
            if (NFPosition.isSingleFinger(position)) {
                record = captureSingle(position);
            } else {
                record = captureMulti(position);
            }
            if (record != null) {
                
                capturedCount++;
                setRecord(record);
                publish(record);
                
                setProgress(100);
            }
        } catch (Exception e) {
            MessageUtils.showError(null, e);
            cancel(true);
        } finally {
            if (image != null) {
                image.dispose();
            }
            if (grayscaleImage != null) {
                grayscaleImage.dispose();
            }
            setProgress(100);
        }
    }
    
    private FingerRecord captureSingle(NFPosition position) throws IOException {
        if (!NFPosition.isSingleFinger(position)) {
            throw new IllegalArgumentException("Position is not single.");
        }
        if (liveCapturing) {
            setCurrentPosition(position);
            NFScanner.CaptureResult captureResult = device.captureImage(currentPosition, device.getImpressionType(scenario.isRolled()), missingPositions);
            if (captureResult.getObjects()[0].getStatus() == NBiometricStatus.OBJECT_MISSING) {
                capturedCount++;
                return null;
            }
            image = captureResult.getImage();
            
        } else {
            image = NImage.fromFile(fileName);
        }
        if (isCancelled() || (image == null)) {
            return null;
        }
        grayscaleImage = image.toGrayscale();
        setProgress(1);
        
        adjustResolution(grayscaleImage);
        FingerRecord record = new FingerRecord(position);
        record.setImage(grayscaleImage.toImage());
        NFExtractor.ExtractResult extractResult = FingerTools.getInstance().getExtractor().extract(grayscaleImage, position, liveCapturing ? device.getImpressionType(false) : NFImpressionType.NON_LIVE_SCAN_PLAIN);
        if (extractResult.getStatus() != NFEExtractionStatus.TEMPLATE_CREATED) {
            MessageUtils.showError(null, "Template extraction failed: " + extractResult.getStatus());
            cancel(true);
            return null;
        }
        record.setRecord(extractResult.getRecord());
        return record;
    }
    
    private MultiFingerRecord captureMulti(NFPosition position) throws IOException {
        if (NFPosition.isSingleFinger(position)) {
            throw new IllegalArgumentException("Position is single.");
        }
        if (liveCapturing) {
            setCurrentPosition(position);
            NFScanner.CaptureResult captureResult = device.captureImage(currentPosition, device.getImpressionType(scenario.isRolled()), missingPositions);
            image = captureResult.getImage();
        } else {
            image = NImage.fromFile(fileName);
        }
        if (isCancelled() || (image == null)) {
            return null;
        }
        grayscaleImage = image.toGrayscale();
        setProgress(1);
        
        adjustResolution(grayscaleImage);
        MultiFingerRecord record = new MultiFingerRecord(position);
        record.setImage(grayscaleImage.toImage());
        
        try {
            NFSegment[] segments = NFSegmenter.locate(grayscaleImage, record.getPosition(), record.getMissingPositions());
            NGrayscaleImage[] segmentedImages = NFSegmenter.cutMultiple(grayscaleImage, segments);
            List<FingerRecord> records = new ArrayList<FingerRecord>(segments.length);
            for (int i = 0; i < segments.length; i++) {
                NFExtractor.ExtractResult extractResult = FingerTools.getInstance().getExtractor().extract(segmentedImages[i], segments[i].getPosition(), liveCapturing ? device.getImpressionType(false) : NFImpressionType.NON_LIVE_SCAN_PLAIN);
                FingerRecord fn = new FingerRecord(segments[i].getPosition());
                fn.setImage(segmentedImages[i].toImage());
                fn.setRecord(extractResult.getRecord());
                records.add((FingerRecord) fn.clone());
            }
            record.setSegmented(records);
        } catch (Exception e) {
            MessageUtils.showError(null, e);
            cancel(true);
        }
        return record;
    }
    
    private void adjustResolution(NGrayscaleImage image) {
        if (image.isResolutionIsAspectRatio() || image.getHorzResolution() < 250 || image.getVertResolution() < 250) {
            image.setResolutionIsAspectRatio(false);
            if (image.getHorzResolution() < 250) {
                image.setHorzResolution(500);
            }
            if (image.getVertResolution() < 250) {
                image.setVertResolution(500);
            }
        }
    }
    
    @Override
    protected void process(List<FingerRecord> fingers) {
        Logger.getAnonymousLogger().info("PROCESS:" + fingers.size());
//			DefaultListModel model = (DefaultListModel) fingersList.getModel();
//			for (FingerRecord finger : fingers) {
//				if (finger instanceof MultiFingerRecord) {
//					MultiFingerRecord multiFingerRecord = (MultiFingerRecord) finger;
//					if (multiFingerRecord.getSegmented() != null) {
//						for (FingerRecord f : multiFingerRecord.getSegmented()) {
//							model.addElement(f);
//						}
//					}
//				} else {
//					model.addElement(finger);
//				}
//			}
    }
    
    @Override
    protected void done() {
        try {
            if (get()) {
                //fingerSelector.clear();
            }
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (CancellationException e) {
        }

        //finishCapturing();
        Thread.yield();
    }
    
    private void setCurrentPosition(NFPosition currentFinger) {
        NFPosition oldCurrentFinger = this.currentPosition;
        this.currentPosition = currentFinger;
        firePropertyChange(CURRENT_FINGER_CHANGED_PROPERTY, oldCurrentFinger, currentFinger);
    }

    /**
     * @return the record
     */
    public FingerRecord getRecord() {
        return record;
    }

    /**
     * @param record the record to set
     */
    public void setRecord(FingerRecord record) {
        this.record = record;
    }
}
