/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.neuro;

import com.bbs.neuro.fingers.FingersSettings;
import com.neurotec.biometrics.NTemplate;
import com.neurotec.biometrics.standards.BDIFStandard;

/**
 *
 * @author Usuario
 */
public class NeuroManager {

    private static NeuroManager instance = null;
    private static NeuroMatcher neuroMatcher = new NeuroMatcher();

    public static NeuroManager getInstance() {
        return instance == null ? (instance = new NeuroManager()) : instance;
    }
    
    public FingersSettings getSettings(){
        return neuroMatcher.getSettings();
    }

    public synchronized int verify(byte[] template1, byte[] template2, BDIFStandard format) {
        return neuroMatcher.verify(template1, template2, format);
    }
    public synchronized int verify(NTemplate template1, NTemplate template2) {
        return neuroMatcher.verify(template1, template2);
        
    }
}
