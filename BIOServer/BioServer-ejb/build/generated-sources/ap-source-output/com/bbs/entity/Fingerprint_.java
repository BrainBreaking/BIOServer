package com.bbs.entity;

import com.bbs.entity.FingerprintPK;
import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="EclipseLink-2.3.2.v20111125-r10461", date="2013-01-20T18:40:35")
@StaticMetamodel(Fingerprint.class)
public class Fingerprint_ { 

    public static volatile SingularAttribute<Fingerprint, byte[]> template;
    public static volatile SingularAttribute<Fingerprint, String> format;
    public static volatile SingularAttribute<Fingerprint, Integer> type;
    public static volatile SingularAttribute<Fingerprint, FingerprintPK> fingerprintPK;

}