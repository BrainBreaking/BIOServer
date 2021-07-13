/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.model;

import com.bbs.entity.Person;
import java.io.Serializable;

/**
 *
 * @author Usuario
 */
public final class TPerson extends TAdapter<Person> implements Serializable {

    private String pin;
    private String nombre1;
    private String nombre2;
    private String particula;
    private String apellido1;
    private String apellido2;
    private String expLugar;
    private String expFecha;
    private String vigencia;

    public TPerson() {
    }

    public TPerson(Person person) {
        if (person != null) {
            this.setApellido1(person.getApellido1());
            this.setApellido2(person.getApellido2());
            this.setParticula(person.getParticula());
            this.setExpFecha(person.getExpFecha());
            this.setExpLugar(person.getExpLugar());
            this.setNombre1(person.getNombre1());
            this.setNombre2(person.getNombre2());
            this.setPin(person.getPin());
            this.setVigencia(person.getVigencia());
        }
    }

    public TPerson(String pin) {
        this.pin = pin;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getNombre1() {
        return nombre1;
    }

    public void setNombre1(String nombre1) {
        this.nombre1 = nombre1;
    }

    public String getNombre2() {
        return nombre2;
    }

    public void setNombre2(String nombre2) {
        this.nombre2 = nombre2;
    }

    public String getApellido1() {
        return apellido1;
    }

    public void setApellido1(String apellido1) {
        this.apellido1 = apellido1;
    }

    public String getApellido2() {
        return apellido2;
    }

    public void setApellido2(String apellido2) {
        this.apellido2 = apellido2;
    }

    public String getExpLugar() {
        return expLugar;
    }

    public void setExpLugar(String expLugar) {
        this.expLugar = expLugar;
    }

    public String getParticula() {
        return particula;
    }

    public void setParticula(String particula) {
        this.particula = particula;
    }

    public String getExpFecha() {
        return expFecha;
    }

    public void setExpFecha(String expFecha) {
        this.expFecha = expFecha;
    }

    public String getVigencia() {
        return vigencia;
    }

    public void setVigencia(String vigencia) {
        this.vigencia = vigencia;
    }

//    @XmlTransient
//    public Collection<Fingerprint> getFingerprintCollection() {
//        return fingerprintCollection;
//    }
//
//    public void setFingerprintCollection(Collection<Fingerprint> fingerprintCollection) {
//        this.fingerprintCollection = fingerprintCollection;
//    }
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (pin != null ? pin.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TPerson)) {
            return false;
        }
        TPerson other = (TPerson) object;
        if ((this.pin == null && other.pin != null) || (this.pin != null && !this.pin.equals(other.pin))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.bbs.model.TPerson[ pin=" + pin + " ]";
    }

    @Override
    public Person convert() {
        Person person = new Person();
        person.setApellido1(this.getApellido1());
        person.setApellido2(this.getApellido2());
        person.setParticula(this.getParticula());
        person.setExpFecha(this.getExpFecha());
        person.setExpLugar(this.getExpLugar());
        person.setNombre1(this.getNombre1());
        person.setNombre2(this.getNombre2());
        person.setPin(this.getPin());
        person.setVigencia(this.getVigencia());
        return person;
    }
}
