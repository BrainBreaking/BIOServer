/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Usuario
 */
@Entity
@Table(catalog = "bbs", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Fingerprint.findAll", query = "SELECT f FROM Fingerprint f"),
    @NamedQuery(name = "Fingerprint.findById", query = "SELECT f FROM Fingerprint f WHERE f.fingerprintPK.id = :id"),
    @NamedQuery(name = "Fingerprint.findByPin", query = "SELECT f FROM Fingerprint f WHERE f.fingerprintPK.pin = :pin"),
    @NamedQuery(name = "Fingerprint.findByType", query = "SELECT f FROM Fingerprint f WHERE f.type = :type"),
    @NamedQuery(name = "Fingerprint.findByFormat", query = "SELECT f FROM Fingerprint f WHERE f.format = :format")})
public class Fingerprint implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected FingerprintPK fingerprintPK;
    @Basic(optional = false)
    @NotNull
    @Column(nullable = false)
    private int type;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Column(nullable = false)
    private byte[] template;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 10)
    @Column(nullable = false, length = 10)
    private String format;

    public Fingerprint() {
    }

    public Fingerprint(FingerprintPK fingerprintPK) {
        this.fingerprintPK = fingerprintPK;
    }

    public Fingerprint(FingerprintPK fingerprintPK, int type, byte[] template, String format) {
        this.fingerprintPK = fingerprintPK;
        this.type = type;
        this.template = template;
        this.format = format;
    }

    public Fingerprint(int id, String pin) {
        this.fingerprintPK = new FingerprintPK(id, pin);
    }

    public FingerprintPK getFingerprintPK() {
        return fingerprintPK;
    }

    public void setFingerprintPK(FingerprintPK fingerprintPK) {
        this.fingerprintPK = fingerprintPK;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
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
        hash += (fingerprintPK != null ? fingerprintPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Fingerprint)) {
            return false;
        }
        Fingerprint other = (Fingerprint) object;
        if ((this.fingerprintPK == null && other.fingerprintPK != null) || (this.fingerprintPK != null && !this.fingerprintPK.equals(other.fingerprintPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.bbs.entity.Fingerprint[ fingerprintPK=" + fingerprintPK + " ]";
    }
    
}
