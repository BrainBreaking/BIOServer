/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.client;

import com.bbs.client.ws.TPerson;
import com.bbs.model.Configuration;
import com.bbs.model.LogManager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Usuario
 */
public class CSVProcessor extends Observable {

    private ArrayList<TPerson> listPerson = new ArrayList<TPerson>();
    private File csvFile = null;

    public CSVProcessor(File csvFile) {
        this.csvFile = csvFile;
    }

    public void process() {

        if (csvFile != null && csvFile.exists() && csvFile.isFile()) {
            try {
                LogManager report=new LogManager(new File(Configuration.getProperty("BIOClient.LogDir")),"CargueAlfanumerico.txt",true);
                int i=1;
                BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile.getAbsolutePath()+".txt"),"cp1252"));
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile.getAbsolutePath()),"cp1252"));
                String readLine = "";
                while ((readLine = br.readLine()) != null && !readLine.isEmpty()) {
                    readLine=new String(readLine.getBytes("cp1252"),"cp1252");
                    readLine=readLine.replace("\"","");
                    String[] split = readLine.split(";");
                    if (split.length == 9) {

                        TPerson person = new TPerson();
                        person.setPin(split[0]);
                        person.setNombre1(split[1]);
                        person.setNombre2(split[2]);
                        person.setParticula(split[3]);
                        person.setApellido1(split[4]);
                        person.setApellido2(split[5]);
                        person.setExpLugar(split[6]);
                        person.setExpFecha(split[7]);
                        person.setVigencia(split[8]);
                        listPerson.add(person);
                        String line=readLine;
                        line=line.replace(split[0], String.valueOf(i));
                        bw.write(line);
                        bw.newLine();
                        i++;
                    }
                    bw.flush();
                    this.setChanged();
                    this.notifyObservers();
                }
                br.close();
                bw.close();
                report.append("Personas",String.valueOf(listPerson.size()));
                report.close();
            } catch (IOException ex) {
                Logger.getLogger(CSVProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            JOptionPane.showMessageDialog(null, "Personas encontradas: "+listPerson.size(), "Carga de Datos Alfanumericos", JOptionPane.INFORMATION_MESSAGE);
            Logger.getAnonymousLogger().info("Total de Registros:" + listPerson.size());
        }
    }

    public ArrayList<TPerson> getListPerson() {
        return listPerson;
    }
}
