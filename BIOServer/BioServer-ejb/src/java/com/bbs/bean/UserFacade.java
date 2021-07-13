/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.bean;

import com.bbs.entity.User;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

/**
 *
 * @author Usuario
 */
@Stateless
public class UserFacade extends AbstractFacade<User> implements UserFacadeLocal {
    @PersistenceContext(unitName = "BioServer-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public UserFacade() {
        super(User.class);
    }

    @Override
    public List<User> find(String user) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        javax.persistence.criteria.CriteriaQuery cq = criteriaBuilder.createQuery();
        Root<User> from = cq.from(User.class);
        ParameterExpression<String> p=criteriaBuilder.parameter(String.class);
        
        cq.select(from).where(criteriaBuilder.equal(from.get("user"), user));
        
        javax.persistence.Query q = getEntityManager().createQuery(cq);
        
        return q.getResultList();
        
    }
    
}
