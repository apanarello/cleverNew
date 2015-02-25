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
package org.clever.ClusterManager.HadoopPlugin;

import org.apache.log4j.Logger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import org.apache.log4j.Logger;
import org.clever.ClusterManager.HadoopNamenode.HadoopNamenodePlugin;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Communicator.ModuleCommunicator;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMLTools.ParserXML;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import org.clever.ClusterManager.HadoopJobtracker.HadoopJobtrackerAgent;
import org.clever.ClusterManager.HadoopNamenode.HadoopNamenodeAgent;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Exceptions.HDFSConnectionException;
import org.clever.Common.Exceptions.HDFSInternalException;
import org.clever.Common.Shared.Support;
import org.clever.Common.StorageRuler.Client;
import org.clever.Common.StorageRuler.FileInfo;
import org.clever.Common.Timestamp.Timestamper;
import org.clever.Common.XMLTools.FileStreamer;
import org.clever.ClusterManager.HadoopJobtracker.*;

/**
 *
 * @author apanarello
 */
public class JobTrackerPlugin implements HadoopJobTrackerPlugin{

    private final Logger logger;
    private Class cl;
    private ModuleCommunicator mc;
    private String hostName;
    private ParserXML pXML;
    private Agent owner;
    private String coreHadoop;
    private String hdfsHadoop;
    private String mapredHadoop;
    private String slave;
    private String master;
    public final static String agentName = "HadoopJobtrackerAgent";
    ClientRuler clientRuler;
    JobRuler jobRouler;

    public JobTrackerPlugin() {

        this.logger = Logger.getLogger("HadoopNamenodePlugin");
        this.jobRouler = new JobRuler();
        try {
            logger.info("Read Configuration HadoopNamenodeAgent!");
            InputStream inxml = getClass().getResourceAsStream("/org/clever/ClusterManager/HadoopNamenode/configuration_HadoopNamenode.xml");
            FileStreamer fs = new FileStreamer();
            ParserXML pars = new ParserXML(fs.xmlToString(inxml));
            this.coreHadoop = pars.getElementContent("coreSite");
            this.hdfsHadoop = pars.getElementContent("hdfsSite");
            this.mapredHadoop = pars.getElementContent("mapredSite");
            this.slave = pars.getElementContent("slaves");
            //retrieve hostname		
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                this.logger.error("Error getting local host name :" + e.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            this.logger.error("Error reading configuration");
        }
    }
    @Override
    public void init(){}

    @Override
    public void setModuleCommunicator(ModuleCommunicator mc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ModuleCommunicator getModuleCommunicator() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String networkIp() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean existsHost(String address, String name) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setHosts(String address, String name) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateHosts(String address, String name) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setSlaves(String username, String name) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean checkHadoopAgent() throws CleverException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void initHadoopAgent() throws CleverException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String submitJob(String input, String output) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setOwner(Agent owner) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String[] retrieveLoginInfo(String domain) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean authenticate(String user, String password) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String registerClient(String client, String type, String password) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void putFile(String fileBuffer, String dstDbPath, String user, String password, String timeout, Boolean forwardable) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getFile(String srcDbPath, String user, String password, String timeout) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean existsFile(String srcDbPath, String user, String password) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
