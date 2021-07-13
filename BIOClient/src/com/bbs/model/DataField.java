/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.model;

/**
 *
 * @author Arkangel
 */
public class DataField<E extends Object> {
    private String name;
    private E value;

    public DataField(){}
    
    public DataField(String name,E value){
        this.name=name;
        this.value=value;
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the value
     */
    public E getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(E value) {
        this.value = value;
    }
}
