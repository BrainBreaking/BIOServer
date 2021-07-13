/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.bean;

import com.bbs.entity.User;
import java.util.List;
import javax.ejb.Local;

/**
 *
 * @author Usuario
 */
@Local
public interface UserFacadeLocal {

    void create(User user);

    void edit(User user);

    void remove(User user);

    User find(Object id);
    
    List<User> find(String user);

    List<User> findAll();

    List<User> findRange(int[] range);

    int count();
    
}
