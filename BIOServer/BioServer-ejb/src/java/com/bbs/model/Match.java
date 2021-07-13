/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.model;

/**
 *
 * @author Usuario
 */
public class Match {

    private String pin;
    private TFingerprint applicant;
    private TFingerprint candidate;
    private int score;
    private MATCH_RESULT result;

    /**
     * @return the pin
     */
    public String getPin() {
        return pin;
    }

    /**
     * @param pin the pin to set
     */
    public void setPin(String pin) {
        this.pin = pin;
    }

    /**
     * @return the applicant
     */
    public TFingerprint getApplicant() {
        return applicant;
    }

    /**
     * @param applicant the applicant to set
     */
    public void setApplicant(TFingerprint applicant) {
        this.applicant = applicant;
    }

    /**
     * @return the candidate
     */
    public TFingerprint getCandidate() {
        return candidate;
    }

    /**
     * @param candidate the candidate to set
     */
    public void setCandidate(TFingerprint candidate) {
        this.candidate = candidate;
    }

    /**
     * @return the score
     */
    public int getScore() {
        return score;
    }

    /**
     * @param score the score to set
     */
    public void setScore(int score) {
        this.score = score;
        
    }

    /**
     * @return the result
     */
    public MATCH_RESULT getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(MATCH_RESULT result) {
        this.result = result;
    }

    public enum MATCH_RESULT {

        HIT,
        NO_HIT,
        CROSS_HIT,
        UNKNOW,
        NOT_FOUND
    }
}
