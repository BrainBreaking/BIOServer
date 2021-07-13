/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.bean;

import com.bbs.entity.Person;
import java.util.List;
import javax.ejb.Local;

/**
 *
 * @author Usuario
 */
@Local
public interface PersonFacadeLocal {

    void create(Person person);

    void edit(Person person);

    void remove(Person person);

    Person find(Object id);
    
    List<Person> find(String pin,String nombre1,String nombre2,String apellido1, String apellido2);

    List<Person> findAll();

    List<Person> findRange(int[] range);

    int count();
    
}
