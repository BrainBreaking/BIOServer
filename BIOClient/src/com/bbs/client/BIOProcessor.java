/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.client;

import com.bbs.model.Fingerprint;
import com.bbs.model.FingerprintFormat;
import com.bbs.model.FingerprintType;
import com.bbs.model.Person;
import java.io.File;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 *
 * @author Usuario
 */
public class BIOProcessor extends Observable {
    
    private TreeMap<String, Person> personMap = new TreeMap<String, Person>();
    private final File selectedFile;
    private final BCLFormat format;
    

    public TreeMap<String, Person> getPersonMap(){
        return personMap;
    }
    BIOProcessor(File selectedFile, BCLFormat format) {
        this.selectedFile=selectedFile;
        this.format=format;
    }
    public void process(){
        processFolder(selectedFile,personMap,format);
    }
    private void processFolder(File selectedFile, TreeMap<String, Person> target, final BCLFormat format) {
        if (selectedFile != null && selectedFile.exists() && selectedFile.isDirectory()) {
            File[] listFiles = selectedFile.listFiles(new java.io.FileFilter() {
                @Override
                public boolean accept(File filepath) {
                    return filepath.isDirectory() || (filepath.isFile() && filepath.getName().endsWith(format.getExtension()));
                }
            });
            Person tmpPerson = new Person();
            for (File item : listFiles) {
                if (item.isDirectory()) {
                    processFolder(item, target, format);
                } else {
                    String[] split = item.getName().split("\\.");
                    String name = split[0];
                    String extension = split[1];
                    FingerprintType fingerprintType = FingerprintType.valueOf(Integer.parseInt(name));
                    Fingerprint fingerprint = new Fingerprint(fingerprintType,extension.equalsIgnoreCase("iso-fmr") ? FingerprintFormat.ISO_19794 : FingerprintFormat.ANSI_378,item);
                    tmpPerson.getFingerPrintSet().add(fingerprint);
                }
            }
            if (tmpPerson.getFingerPrintSet().size() > 0) {
                String name = selectedFile.getName();
                tmpPerson.setPath(selectedFile);
                target.put(name, tmpPerson);
                this.setChanged();
                this.notifyObservers();
            }
        }
    }
}
