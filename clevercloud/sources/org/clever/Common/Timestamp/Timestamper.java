/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.clever.Common.Timestamp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author giovanni
 */
public class Timestamper {
    
    private final static String directory = "Misure";
    private final static String extension = "csv";
    
    public static void write (String filename) throws IOException {
        File dir = new File (directory);
        if (!dir.exists())
            dir.mkdirs();
        File file = new File (directory+"/"+filename+"."+extension);
        if (!file.exists())
            file.createNewFile();
        long t =System.currentTimeMillis();
        FileOutputStream fo=new FileOutputStream(file,true);
        fo.write((Long.toString(t)+"\n").getBytes());
        
        fo.close();
    }
}
