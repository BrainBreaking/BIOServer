
package com.bbs.client.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para match complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="match">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="applicant" type="{http://ws.bbs.com/}tFingerprint" minOccurs="0"/>
 *         &lt;element name="candidate" type="{http://ws.bbs.com/}tFingerprint" minOccurs="0"/>
 *         &lt;element name="pin" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="result" type="{http://ws.bbs.com/}matchRESULT" minOccurs="0"/>
 *         &lt;element name="score" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "match", propOrder = {
    "applicant",
    "candidate",
    "pin",
    "result",
    "score"
})
public class Match {

    protected TFingerprint applicant;
    protected TFingerprint candidate;
    protected String pin;
    protected MatchRESULT result;
    protected int score;

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
     * Obtiene el valor de la propiedad pin.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPin() {
        return pin;
    }

    /**
     * Define el valor de la propiedad pin.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPin(String value) {
        this.pin = value;
    }

    /**
     * Obtiene el valor de la propiedad result.
     * 
     * @return
     *     possible object is
     *     {@link MatchRESULT }
     *     
     */
    public MatchRESULT getResult() {
        return result;
    }

    /**
     * Define el valor de la propiedad result.
     * 
     * @param value
     *     allowed object is
     *     {@link MatchRESULT }
     *     
     */
    public void setResult(MatchRESULT value) {
        this.result = value;
    }

    /**
     * Obtiene el valor de la propiedad score.
     * 
     */
    public int getScore() {
        return score;
    }

    /**
     * Define el valor de la propiedad score.
     * 
     */
    public void setScore(int value) {
        this.score = value;
    }

}
