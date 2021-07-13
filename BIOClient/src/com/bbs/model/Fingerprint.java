/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.model;

import com.bbs.client.ws.TFingerprint;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Arkangel
 */
public class Fingerprint implements Comparable<Fingerprint> {
    
    private FingerprintFormat format;
    private FingerprintType type;
    private byte[] buffer = null;
    private int minutiaes = 0;
    private File source = null;
    
    public Fingerprint() {
    }
    
    public TFingerprint convert() {
        TFingerprint tfp = new TFingerprint();
        tfp.setFormat(this.getFormat().toString());
        tfp.setId(this.getType().getCode());
        tfp.setTemplate(this.getBuffer());
        tfp.setType(this.getType().getCode());
        return tfp;
    }
    
    public Fingerprint(FingerprintType type, FingerprintFormat format, byte[] buffer) {
        this.type = type;
        this.format = format;
        this.buffer = buffer = null;
    }

    public Fingerprint(FingerprintType type, FingerprintFormat format, File source) {
        this.type = type;
        this.format = format;
        this.source = source;
    }

    /**
     * @return the format
     */
    public FingerprintFormat getFormat() {
        return format;
    }

    /**
     * @param format the format to set
     */
    public void setFormat(FingerprintFormat format) {
        this.format = format;
    }

    /**
     * @return the type
     */
    public FingerprintType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(FingerprintType type) {
        this.type = type;
    }

    /**
     * @return the buffer
     */
    public byte[] getBuffer() {
        if (buffer == null && source != null && source.exists() && source.isFile()) {
            setBuffer(source);
        }
        return buffer;
    }

    /**
     * @param buffer the buffer to set
     */
    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    /**
     * @param buffer the buffer to set
     */
    public void setBuffer(File fileBuffer) {
        try {
            FileInputStream fis = new FileInputStream(fileBuffer);
            byte[] tmpBuffer = new byte[fis.available()];
            fis.read(tmpBuffer);
            fis.close();
            setBuffer(tmpBuffer);
        } catch (IOException ex) {
            Logger.getLogger(Fingerprint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public int compareTo(Fingerprint target) {
        return this.getFormat() == target.getFormat() && this.getType() == target.getType() ? 0 : -1;
    }

    /**
     * @return the minutieas
     */
    public int getMinutiaes() {
        return minutiaes;
    }

    /**
     * @param minutieas the minutieas to set
     */
    public void setMinutiaes(int minutiaes) {
        this.minutiaes = minutiaes;
    }
}
