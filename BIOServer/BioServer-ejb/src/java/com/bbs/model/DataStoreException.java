/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.model;

import com.sun.faces.context.ExceptionHandlerImpl;
import javax.faces.FacesException;
import javax.faces.context.ExceptionHandler;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.SystemEvent;
import javax.persistence.PersistenceException;

/**
 *
 * @author Usuario
 */
public class DataStoreException extends Exception {

    public DataStoreException(PersistenceException ex) {
        
    }

   

}
