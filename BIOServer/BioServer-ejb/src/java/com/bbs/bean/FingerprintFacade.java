/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.bean;

import com.bbs.entity.Fingerprint;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Usuario
 */
@Stateless
public class FingerprintFacade extends AbstractFacade<Fingerprint> implements FingerprintFacadeLocal {
    @PersistenceContext(unitName = "BioServer-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public FingerprintFacade() {
        super(Fingerprint.class);
    }
    
}
