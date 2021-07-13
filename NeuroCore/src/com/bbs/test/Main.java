/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.test;

import com.bbs.neuro.NeuroManager;
import com.neurotec.biometrics.standards.BDIFStandard;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Usuario
 */
public class Main {

    public static void main(String... args) {
        FileInputStream fis1 = null;
        FileInputStream fis2 = null;
        try {
            NeuroManager manager = new NeuroManager();
            Logger.getAnonymousLogger().info("Leyendo Archivos");
            File file1 = new File("C:\\COTS\\JuegoDePruebas\\appl\\1\\1.iso-fmr");
            fis1 = new FileInputStream(file1);
            byte[] buffer1 = new byte[fis1.available()];
            fis1.read(buffer1);
            fis1.close();

            File file2 = new File("C:\\COTS\\JuegoDePruebas\\appl\\1\\1.iso-fmr");
            fis2 = new FileInputStream(file2);
            byte[] buffer2 = new byte[fis2.available()];
            fis2.read(buffer2);
            fis2.close();
            Logger.getAnonymousLogger().info("Autenticando");
            int verify = manager.verify(buffer1, buffer2, BDIFStandard.ISO);
            Logger.getAnonymousLogger().info("Resultado:" + verify);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
