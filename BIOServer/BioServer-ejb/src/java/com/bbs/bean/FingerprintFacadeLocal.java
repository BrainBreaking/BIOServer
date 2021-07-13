/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.bean;

import com.bbs.entity.Fingerprint;
import java.util.List;
import javax.ejb.Local;

/**
 *
 * @author Usuario
 */
@Local
public interface FingerprintFacadeLocal {

    void create(Fingerprint fingerprint);

    void edit(Fingerprint fingerprint);

    void remove(Fingerprint fingerprint);

    Fingerprint find(Object id);

    List<Fingerprint> findAll();

    List<Fingerprint> findRange(int[] range);

    int count();
    
}
