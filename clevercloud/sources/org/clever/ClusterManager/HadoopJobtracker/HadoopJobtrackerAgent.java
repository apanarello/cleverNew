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

package org.clever.ClusterManager.HadoopJobtracker;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Communicator.Notification;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMLTools.FileStreamer;
import org.clever.Common.XMLTools.ParserXML;
/**
 *
 * @author apanarello
 */
public class HadoopJobtrackerAgent extends CmAgent{
    
    private Class cl;
    private HadoopJobTrackerPlugin plugin;
    private String hostname;
    private final String agentId = "HadoopJobtrackerAgent";
    private String jobTrackePort ;
    private String hadoopTmpDir;
    //private String start;
        //private String format;
    //private String stop;
    private String coreHadoop;
    private String mapredHadoop;
    private String hdfsHadoop;
    private String dirPermissions;
    private String append;
    private String slaves, master;

    public HadoopJobtrackerAgent() {
        
        super();
        logger = Logger.getLogger("HadoopJobtrackerAgent");
    }

    @Override
    public void initialization() throws CleverException {
        if (super.getAgentName().equals("NoName")){
            super.setAgentName("HadoopJobtrakcerAgent");
                }
        super.start();
        try{
        logger.info("Read Configuration HadoopJobtrackerAgent");
        InputStream inxml = getClass().getResourceAsStream("/org/clever/ClusterManager/HadoopJobTracker/configuration_HadoopJobTracker.xml");
        FileStreamer fileStreamer = new FileStreamer();
        ParserXML pars = new ParserXML(fileStreamer.xmlToString(inxml));
        this.coreHadoop=pars.getElementContent("coreSite");
        this.dirPermissions=pars.getElementContent("dirPermissions");
        this.mapredHadoop = pars.getElementContent("mapredSite");
                this.hdfsHadoop = pars.getElementContent("hdfsSite");
                this.jobTrackePort = pars.getElementContent("namenodePort");
                this.hadoopTmpDir = pars.getElementContent("hadoopTmpDir");
                this.append = pars.getElementContent("append");
                this.slaves = pars.getElementContent("slaves");
                this.master = pars.getElementContent("master");
        this.cl = Class.forName(pars.getElementContent("HadoopJobTrackerPlugin"));       
        logger.debug("Configuration read.");
        //logger.debug("Start JobTracker Registration to dispatcher agent");
        //List params2 =new ArrayList();
        //params2.add(agentId);
        logger.debug("PluginClass: "+this.cl.getName());
        this.plugin=(HadoopJobTrackerPlugin) this.cl.newInstance();
        logger.debug(plugin.toString()+ "Plugin istantiated: ");
        this.plugin.setModuleCommunicator(mc);
        this.plugin.setOwner(this);
        this.plugin.init();
                //retrieve hostname
        this.hostname = InetAddress.getLocalHost().getHostName();
        
        }
        catch(Exception e){
        logger.error("Error initializing HadoopNamenode : " + e.getMessage());
        }
    }
    

    @Override
    public Class getPluginClass() {
    
        return this.cl;
    }
/*
    public Class startPlugin(){
    
    
    }
    */
    @Override
    public Object getPlugin() {
        return this.plugin;
    }

    @Override
    public void shutDown() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void handleNotification(Notification notification) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
}
