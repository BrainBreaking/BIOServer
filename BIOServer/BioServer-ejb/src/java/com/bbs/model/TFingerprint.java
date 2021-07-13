/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.model;

import com.bbs.entity.*;
import java.io.Serializable;

/**
 *
 * @author Usuario
 */
public class TFingerprint extends TAdapter<Fingerprint> implements Serializable {
    private Integer id=null;
    private String pin;
    private Integer type;
    private byte[] template;
    private String format;

    public TFingerprint() {
    }

    public TFingerprint(Integer id) {
        this.id = id;
    }

    public TFingerprint(Fingerprint fp) {
        FingerprintPK fingerprintPK = fp.getFingerprintPK();
        this.id = fingerprintPK.getId();
        this.pin = fingerprintPK.getPin();
        this.type = fp.getType();
        this.template = fp.getTemplate();
        this.format = fp.getFormat();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public byte[] getTemplate() {
        return template;
    }

    public void setTemplate(byte[] template) {
        this.template = template;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

@Override
    public int hashCode() {
        int hash = 0;
        hash += (pin != null ? pin.hashCode() : 0) +(id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Fingerprint)) {
            return false;
        }
        Fingerprint other = (Fingerprint) object;
        if ((this.id == null && other.getFingerprintPK() != null) || (this.id != null && !this.id.equals(other.getFingerprintPK().getId()) && !this.pin.equals(other.getFingerprintPK().getPin()))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.bbs.model.TFingerprint[ id=" + id + " ]";
    }

    @Override
    public Fingerprint convert() {
        Fingerprint fp=new Fingerprint();
        fp.setTemplate(template);
        fp.setFormat(format);
        FingerprintPK fpk=new FingerprintPK(id,pin);
        fp.setFingerprintPK(fpk);
        fp.setType(type);
        return fp;
    }
    
}
