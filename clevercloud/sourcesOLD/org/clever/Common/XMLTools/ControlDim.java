/*
 * The MIT License
 *
 * Copyright 2014 apanarello.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.clever.Common.XMLTools;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.S3tools.S3Tools;
import org.clever.Common.Timestamp.Timestamper;

/**
 *
 * @author apanarello
 */
public class ControlDim extends Thread {

    private final Logger logger;
    public ArrayList<Boolean> cloop;
    public HashMap<Byte, String> urlMap;
    int numThread;
    private String fileName;
    ArrayList<String> filePathS;
    S3Tools s3t = null;
    String bucket = null;
    File files = null;
    String fileSmil = "";
    String home = "/home/apanarello/";

    public ControlDim(Logger log, int i, HashMap m, String bucket, String nameF, S3Tools s3t) {
        this.urlMap = m;
        this.numThread = i;
        //this.cloop=loop;
        this.logger = log;
        logger.debug("Object " + this + " creato");
        this.fileName = nameF;
        this.s3t = s3t;
        this.bucket = bucket;
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                Timestamper.write("Time19-InizioAttesaRisposteDominiFederati-");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }

            try {  //logger.debug("BEFORE WHILE LOOP: "+"URLMAP SIZE: "+urlMap.size()+" Num thread : "+ numThread);
                while (urlMap.isEmpty()) {
                    Thread.sleep(10000);
                }
                while (urlMap.size() < numThread) {
                    logger.debug("INTO WHILE LOOP: " + "URLMAP SIZE: " + urlMap.size());
                    try {
                        logger.debug("Into Cycle controll " + this + "\\\\ Actually Hash table size is: " + urlMap.size() + " and the number of thread which are terminated is: " + numThread);
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        logger.error("error in threar sleep", ex);
                    }
                }

                try {
                    Timestamper.write("Time20-FineAttesaRisposteDominiFederati");
                } catch (IOException ex) {
                    this.logger.warn("can't write timestamp log: " + ex.getMessage());
                }
                logger.debug("Create smil object...urlmap-size is: " + urlMap.size());

                try {
                    Timestamper.write("Time21-InizioCreazioneSmileUpLoadSuS3");
                } catch (IOException ex) {
                    this.logger.warn("can't write timestamp log: " + ex.getMessage());
                }
                SmilXML smil = new SmilXML(logger, urlMap, fileName);
                logger.debug("Created smil object and start to create SMIL XML FILE");
                fileSmil = smil.createSmil(this.home);
                s3t.uploadFile(this.home + fileSmil, "outputfederation", fileSmil);

                try {
                    Timestamper.write("Time22-FineCreazioneSmileUpLoadSuS3");
                } catch (IOException ex) {
                    this.logger.warn("can't write timestamp log: " + ex.getMessage());
                }
            //solo per misureeeeeee   
                //this.wait(10000);
                logger.debug("Entro in deleteFile");
                try {
                    s3t.deleteFile("outputfederation");
                } catch (AmazonS3Exception ex) {
                    logger.error("Error in delete smil FILE", ex);
                }
                //solo per misureeeeeee

            } catch (FileNotFoundException ex) {
                logger.error("File non found in method smil.createSmil", ex);
            } catch (ClassNotFoundException ex) {
                logger.error("Error in  smil.createSmil", ex);;
            } catch (Exception ex) {
                logger.error("Error in  run method", ex);
            }
            try {
                files = new File(home + fileSmil);
                if (files.delete()) {
                    logger.debug("deleted file from fisical fs: " + home + fileSmil);
                } else {
                    throw new CleverException("ERROR DURING DELETING FILE");
                }
            } catch (CleverException ex) {
                logger.error("ERROR DURING DELETING SMIL FILE -1 ", ex);
            } catch (Exception ex) {
                logger.error("ERROR DURING DELETING SMIL FILE -2", ex);
            }
            cloop.add(Boolean.TRUE);
            /*try {
             s3t.uploadFile(fileBuffer, pass, bucketName, fileNameS3);
             } catch (IOException ex) {
             logger.error("Error during upload smil in Amazon S3",ex);
             } catch (CleverException ex) {
             logger.error("Error during upload smil in Amazon S3", ex);
             }
             */
            notify();
        }
    }
}
