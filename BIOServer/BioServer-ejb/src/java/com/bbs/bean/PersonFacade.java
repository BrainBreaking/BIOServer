/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.bean;

import com.bbs.entity.Person;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 *
 * @author Usuario
 */
@Stateless
public class PersonFacade extends AbstractFacade<Person> implements PersonFacadeLocal {

    @PersistenceContext(unitName = "BioServer-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PersonFacade() {
        super(Person.class);
    }

    @Override
    public List<Person> find(String pin, String nombre1, String nombre2, String apellido1, String apellido2) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        javax.persistence.criteria.CriteriaQuery cq = criteriaBuilder.createQuery();
        Root<Person> from = cq.from(Person.class);

        List<Predicate> where = new ArrayList<Predicate>();
        if (pin != null && !pin.isEmpty()) {
            Predicate equal = criteriaBuilder.equal(from.get("pin"), pin);
            where.add(equal);
        }
        if (nombre1 != null && !nombre1.isEmpty()) {
            Predicate equal = criteriaBuilder.equal(from.get("nombre1"), nombre1);
            where.add(equal);
        }
        if (nombre2 != null && !nombre2.isEmpty()) {
            Predicate equal = criteriaBuilder.equal(from.get("nombre2"), nombre2);
            where.add(equal);
        }
        if (apellido1 != null && !apellido1.isEmpty()) {
            Predicate equal = criteriaBuilder.equal(from.get("apellido1"), apellido1);
            where.add(equal);
        }
        if (apellido2 != null && !apellido2.isEmpty()) {
            Predicate equal = criteriaBuilder.equal(from.get("apellido2"), apellido2);
            where.add(equal);
        }


        if (!where.isEmpty()) {
            Predicate[] predicates=new Predicate[where.size()];
            predicates=where.toArray(predicates);
            cq.select(from).where(predicates);
        }
        javax.persistence.Query q = getEntityManager().createQuery(cq);

        return q.getResultList();
    }
}
