/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
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
@Table(name = "person", catalog = "bbs", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Person.findAll", query = "SELECT p FROM Person p"),
    @NamedQuery(name = "Person.findByPin", query = "SELECT p FROM Person p WHERE p.pin = :pin"),
    @NamedQuery(name = "Person.findByNombre1", query = "SELECT p FROM Person p WHERE p.nombre1 = :nombre1"),
    @NamedQuery(name = "Person.findByNombre2", query = "SELECT p FROM Person p WHERE p.nombre2 = :nombre2"),
    @NamedQuery(name = "Person.findByApellido1", query = "SELECT p FROM Person p WHERE p.apellido1 = :apellido1"),
    @NamedQuery(name = "Person.findByApellido2", query = "SELECT p FROM Person p WHERE p.apellido2 = :apellido2"),
    @NamedQuery(name = "Person.findByExpLugar", query = "SELECT p FROM Person p WHERE p.expLugar = :expLugar"),
    @NamedQuery(name = "Person.findByParticula", query = "SELECT p FROM Person p WHERE p.particula = :particula"),
    @NamedQuery(name = "Person.findByExpFecha", query = "SELECT p FROM Person p WHERE p.expFecha = :expFecha"),
    @NamedQuery(name = "Person.findByVigencia", query = "SELECT p FROM Person p WHERE p.vigencia = :vigencia")})
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "pin")
    private String pin;
    @Size(max = 45)
    @Column(name = "nombre1")
    private String nombre1;
    @Size(max = 45)
    @Column(name = "nombre2")
    private String nombre2;
    @Size(max = 45)
    @Column(name = "apellido1")
    private String apellido1;
    @Size(max = 45)
    @Column(name = "apellido2")
    private String apellido2;
    @Size(max = 45)
    @Column(name = "exp_lugar")
    private String expLugar;
    @Size(max = 45)
    @Column(name = "particula")
    private String particula;
    @Size(max = 45)
    @Column(name = "exp_fecha")
    private String expFecha;
    @Size(max = 45)
    @Column(name = "vigencia")
    private String vigencia;

    public Person() {
    }

    public Person(String pin) {
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (pin != null ? pin.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Person)) {
            return false;
        }
        Person other = (Person) object;
        if ((this.pin == null && other.pin != null) || (this.pin != null && !this.pin.equals(other.pin))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.bbs.entity.Person[ pin=" + pin + " ]";
    }
    
}
