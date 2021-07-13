/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.model;

import com.bbs.client.BBSClientView;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Usuario
 */
public class Configuration {
    private static Properties configuration=null;
    static{
    configuration = new Properties();
        try {
            configuration.load(new FileInputStream(new File("./BIOClient.properties")));
        } catch (IOException ex) {
            Logger.getLogger(BBSClientView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(BBSClientView.class.getName()).log(Level.WARNING, ex.getMessage());
        }
    }
    public static String getProperty(String key){
        return configuration!=null ?configuration.getProperty(key):"";
    }
    public static String getProperty(String key,String defaultValue){
        String value=getProperty(key);
        return value!=null && !value.isEmpty()?value:defaultValue;
    }
    
}
