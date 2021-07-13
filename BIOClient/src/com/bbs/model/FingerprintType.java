/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.model;

/**
 *
 * @author Arkangel
 */
public enum FingerprintType {

    RIGHT_THUMB(1),
    RIGHT_INDEX(2),
    RIGHT_MIDDLE(3),
    RIGHT_RING(4),
    RIGHT_LITTLE(5),
    LEFT_THUMB(6),
    LEFT_INDEX(7),
    LEFT_MIDDLE(8),
    LEFT_RING(9),
    LEFT_LITTLE(10);
    private int code;

    FingerprintType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static FingerprintType valueOf(int code) {
        for (FingerprintType type : FingerprintType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }
}
