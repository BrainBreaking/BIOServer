/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.model;

import java.io.File;
import java.util.ArrayList;
import java.util.ArrayList;

/**
 *
 * @author Arkangel
 */
public class Person {
    private ArrayList<Fingerprint> fingerPrintSet=new ArrayList<Fingerprint>();
    
    private File path;

    /**
     * @return the fingerPrintSet
     */
    public ArrayList<Fingerprint> getFingerPrintSet() {
        return fingerPrintSet;
    }

    /**
     * @param fingerPrintSet the fingerPrintSet to set
     */
    public void setFingerPrintSet(ArrayList<Fingerprint> fingerPrintSet) {
        this.fingerPrintSet = fingerPrintSet;
    }


    public void setPath(File selectedFile) {
        path=selectedFile;
    }
    public File getPath(){
        return path;
    }
}
