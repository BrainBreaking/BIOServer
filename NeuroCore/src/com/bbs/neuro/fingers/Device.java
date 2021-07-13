package com.bbs.neuro.fingers;

import java.util.ArrayList;
import java.util.List;

import com.neurotec.biometrics.NFImpressionType;
import com.neurotec.biometrics.NFPosition;
import com.neurotec.devices.NFScanner;

public class Device {

    private NFScanner device;
    private boolean hasRolled;
    private boolean hasSlaps;

    public Device(NFScanner device) {
        this.device = device;
        NFPosition[] modes = device.getSupportedPositions();
        NFImpressionType[] impresions = device.getSupportedImpressionTypes();
        hasRolled = false;
        hasSlaps = false;
        for (NFPosition position : modes) {
            if (NFPosition.isFourFingers(position)) {
                hasSlaps = true;
                break;
            }
        }
        for (NFImpressionType impresion : impresions) {
            if (NFImpressionType.isRolled(impresion)) {
                hasRolled = true;
                break;
            }
        }
    }

    public NFScanner getDevice() {
        return device;
    }

    public boolean hasRolled() {
        return hasRolled;
    }

    public boolean hasSlaps() {
        return hasSlaps;
    }

    public Scenario[] getSupportedScenarios() {
        ArrayList<Scenario> set = new ArrayList<Scenario>();
        Scenario[] result;
        set.add(Scenario.PLAIN_FINGER);
        if (hasRolled) {
            set.add(Scenario.ROLLED_FINGER);
        }
        set.add(Scenario.ALL_PLAIN_FINGERS);
        if (hasRolled) {
            set.add(Scenario.ALL_ROLLED_FINGERS);
        }
        if (hasSlaps) {
            set.add(Scenario.SLAPS_2_THUMBS);
            set.add(Scenario.SLAP_AND_THUMB);
//			if (hasRolled)
//				set.add(Scenario.ROLLED_PLUS_SLAPS);
        }

        result = new Scenario[set.size()];
        return set.toArray(result);
    }

    public NFScanner.CaptureResult captureImage(NFPosition position, NFImpressionType impression, List<NFPosition> missingPositions) {
        NFPosition[] missing = (NFPosition[]) missingPositions.toArray(new NFPosition[missingPositions.size()]);
        return device.capture(impression, position, missing, true, -1, true);
    }

    public NFImpressionType getImpressionType(boolean rolled) {
        NFImpressionType[] impresions = device.getSupportedImpressionTypes();
        for (NFImpressionType impression : impresions) {
            if (NFImpressionType.isRolled(impression) == rolled && !NFImpressionType.isPalm(impression)) {
                return impression;
            }
        }
        return NFImpressionType.UNKNOWN;
    }

    @Override
    public String toString() {
        return device.getDisplayName();
    }
}
