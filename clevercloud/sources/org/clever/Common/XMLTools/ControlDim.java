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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;
import org.apache.log4j.Logger;
/**
 *
 * @author apanarello
 */
public class ControlDim implements Runnable{
    private final Logger logger;
    public Map<Byte,String> urlMap;
    int numThread;

        public ControlDim(Logger log, int i, Map m) {
            this.urlMap=m;
            this.numThread=i;
            this.logger=log;
            logger.debug("Object "+this+" creato");
            
        }

        @Override
        public void run() {
            try {
                
                //logger.debug("BEFORE WHILE LOOP: "+"URLMAP SIZE: "+urlMap.size()+" Num thread : "+ numThread);
                                logger.debug("BEFORE WHILE LOOP: "+"URLMAP SIZE: ");
                while (urlMap.isEmpty()) 
                    Thread.sleep(8000);
                while (urlMap.size()<numThread){
                    try {
                        logger.debug("Into Cycle controll "+this+"\\\\ Actually Hash table size is: "+urlMap.size()+" and the number of thread which are terminated is: "+numThread);
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        logger.error("error in threar sleep", ex);
                    }
                }
                logger.debug("Create smil object");
                SmilXML smil = new SmilXML(logger,urlMap);
                logger.debug("Created smil object and start to create SMIL XML FILE");
                smil.createSmil();
                logger.debug("Create smil file");
            } catch (FileNotFoundException ex) {
                logger.error("File non found in method smil.createSmil", ex);
            } catch (ClassNotFoundException ex) {
                logger.error("Error in  smil.createSmil", ex);;
            } catch (Exception ex) {
                logger.error("Error in  run method", ex);;
            }
            /*try {
                s3t.uploadFile(fileBuffer, pass, bucketName, fileNameS3);
            } catch (IOException ex) {
                logger.error("Error during upload smil in Amazon S3",ex);
            } catch (CleverException ex) {
                logger.error("Error during upload smil in Amazon S3", ex);
            }
             */      
        }
    }
