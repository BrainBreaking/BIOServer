
package com.bbs.client.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para putFingerprint complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="putFingerprint">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="fingerprint" type="{http://ws.bbs.com/}tFingerprint" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "putFingerprint", propOrder = {
    "fingerprint"
})
public class PutFingerprint {

    protected TFingerprint fingerprint;

    /**
     * Obtiene el valor de la propiedad fingerprint.
     * 
     * @return
     *     possible object is
     *     {@link TFingerprint }
     *     
     */
    public TFingerprint getFingerprint() {
        return fingerprint;
    }

    /**
     * Define el valor de la propiedad fingerprint.
     * 
     * @param value
     *     allowed object is
     *     {@link TFingerprint }
     *     
     */
    public void setFingerprint(TFingerprint value) {
        this.fingerprint = value;
    }

}
