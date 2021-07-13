/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.model;

/**
 *
 * @author Arkangel
 */
public enum FingerprintFormat {

    ISO_19794("iso-fmr"),
    ANSI_378("ansi-fmr");
    private String extension;

    FingerprintFormat(String extension) {
        this.extension = extension;
    }
}
