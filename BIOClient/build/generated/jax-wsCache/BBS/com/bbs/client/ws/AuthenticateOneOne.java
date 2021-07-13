
package com.bbs.client.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para authenticateOneOne complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="authenticateOneOne">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="candidate" type="{http://ws.bbs.com/}tFingerprint" minOccurs="0"/>
 *         &lt;element name="applicant" type="{http://ws.bbs.com/}tFingerprint" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "authenticateOneOne", propOrder = {
    "candidate",
    "applicant"
})
public class AuthenticateOneOne {

    protected TFingerprint candidate;
    protected TFingerprint applicant;

    /**
     * Obtiene el valor de la propiedad candidate.
     * 
     * @return
     *     possible object is
     *     {@link TFingerprint }
     *     
     */
    public TFingerprint getCandidate() {
        return candidate;
    }

    /**
     * Define el valor de la propiedad candidate.
     * 
     * @param value
     *     allowed object is
     *     {@link TFingerprint }
     *     
     */
    public void setCandidate(TFingerprint value) {
        this.candidate = value;
    }

    /**
     * Obtiene el valor de la propiedad applicant.
     * 
     * @return
     *     possible object is
     *     {@link TFingerprint }
     *     
     */
    public TFingerprint getApplicant() {
        return applicant;
    }

    /**
     * Define el valor de la propiedad applicant.
     * 
     * @param value
     *     allowed object is
     *     {@link TFingerprint }
     *     
     */
    public void setApplicant(TFingerprint value) {
        this.applicant = value;
    }

}
