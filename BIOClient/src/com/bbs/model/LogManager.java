/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Usuario
 */
public class LogManager {
    
    private BufferedWriter bw=null;
    private final  String SEPARATOR=";";
    
    public LogManager(File logDir,String name, boolean withDate ) throws IOException{
        if(withDate){
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String format = formatter.format(new Date());
            name+="."+format;
        }
        bw=new BufferedWriter(new FileWriter(logDir.getAbsolutePath()+"\\"+name));
    }
    
    public synchronized void append(String... args) throws IOException{
        String line="";
        for(String item:args){
            line+=item+SEPARATOR;
        }
        bw.write(line);
        bw.newLine();
        bw.flush();
    }
    public void close() throws IOException{
        bw.close();
    }
}
