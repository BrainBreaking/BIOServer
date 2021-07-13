
package com.bbs.client.ws;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para matchRESULT.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * <p>
 * <pre>
 * &lt;simpleType name="matchRESULT">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="HIT"/>
 *     &lt;enumeration value="NO_HIT"/>
 *     &lt;enumeration value="CROSS_HIT"/>
 *     &lt;enumeration value="UNKNOW"/>
 *     &lt;enumeration value="NOT_FOUND"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "matchRESULT")
@XmlEnum
public enum MatchRESULT {

    HIT,
    NO_HIT,
    CROSS_HIT,
    UNKNOW,
    NOT_FOUND;

    public String value() {
        return name();
    }

    public static MatchRESULT fromValue(String v) {
        return valueOf(v);
    }

}
