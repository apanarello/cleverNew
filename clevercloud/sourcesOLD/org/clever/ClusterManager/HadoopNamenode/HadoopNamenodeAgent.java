/*
 * The MIT License
 *
 * Copyright (c) 2013 Mariacristina Sinagra
 * Copyright (c) 2014 Giovanni Volpintesta
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

package org.clever.ClusterManager.HadoopNamenode;

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
 * @author Giovanni Volpintesta
 * @author Mariacristina Sinagra
 */

public class HadoopNamenodeAgent extends CmAgent {

	private Class cl;
	private HadoopNamenodePlugin plugin;
	private String hostname;
	private final String notifystart = "HADOOP";
	private final String notifystop = "HADOOP/DOWN";
	private final String agentId = "HadoopNamenodeAgent";
        private String namenodePort;
        private String hadoopTmpDir;
	private String start;
        //private String format;
	private String stop;
	private String coreHadoop;
	private String mapredHadoop;
        private String hdfsHadoop;
        private String dirPermissions;
        private String append;
        private String slaves;
        
        public HadoopNamenodeAgent() {
		super();
		logger = Logger.getLogger("HadoopNamenodeAgent");
	}
        
        @Override
	public void initialization() throws CleverException {
            
            if (super.getAgentName().equals("NoName")) {
                    super.setAgentName("HadoopNamenodeAgent");
            }
            super.start();
            try {
                logger.info("Read Configuration HadoopNamenodeAgent!");
                InputStream inxml = getClass().getResourceAsStream("/org/clever/ClusterManager/HadoopNamenode/configuration_HadoopNamenode.xml");
                FileStreamer fs = new FileStreamer();
                ParserXML pars = new ParserXML(fs.xmlToString(inxml));
                this.start = pars.getElementContent("startHadoop");
                this.stop = pars.getElementContent("stopHadoop");
                //this.format = pars.getElementContent("formatHadoop");
                this.coreHadoop = pars.getElementContent("coreSite");
                this.mapredHadoop = pars.getElementContent("mapredSite");
                this.hdfsHadoop = pars.getElementContent("hdfsSite");
                this.namenodePort = pars.getElementContent("namenodePort");
                this.hadoopTmpDir = pars.getElementContent("hadoopTmpDir");
                this.dirPermissions = pars.getElementContent("dirPermissions");
                this.append = pars.getElementContent("append");
                this.slaves = pars.getElementContent("slaves");
                this.cl = Class.forName(pars.getElementContent("HadoopNamenodePlugin"));
                //register agent namenode to dispatcher agent
                logger.debug("Configuration read.");
                List params2 = new ArrayList();
                params2.add("HadoopNamenodeAgent");
                params2.add(this.notifystart);
                this.invoke("DispatcherAgent", "subscribeNotification", true, params2);
                List params3 = new ArrayList();
                params3.add("HadoopNamenodeAgent");
                params3.add(this.notifystop);
                this.invoke("DispatcherAgent", "subscribeNotification", true, params3);
                logger.debug("notifcation subscribed");
                logger.debug("PluginClass: "+this.cl.getName());
                this.plugin = (HadoopNamenodePlugin) this.cl.newInstance();
                logger.debug("Plugin istantiated");
                this.plugin.setModuleCommunicator(mc);
                this.plugin.setOwner(this);
                this.plugin.init();
                //retrieve hostname
                this.hostname = InetAddress.getLocalHost().getHostName();

                //configuration core-site, mapred-site, hdfs-site
/*                Configuration conf = new Configuration(true);
                conf.set("fs.default.name", "hdfs://" + this.hostname + ":" + this.namenodePort);
		conf.set("mapred.job.tracker", this.hostname + ":8021");
                conf.set("hadoop.tmp.dir", this.hadoopTmpDir);
                conf.set("dfs.datanode.data.dir.perm", this.dirPermissions);
                conf.set("dfs.support.append", this.append);
                try {
                    File coreFile = new File (this.coreHadoop);
                    if (!coreFile.exists())
                        coreFile.createNewFile();
                    File hdfsFile = new File (this.hdfsHadoop);
                    if (!hdfsFile.exists())
                        hdfsFile.createNewFile();
                    File mapredFile = new File (this.mapredHadoop);
                    if (!mapredFile.exists())
                        mapredFile.createNewFile();
                    OutputStream out = new FileOutputStream(this.coreHadoop);
                    conf.writeXml(out);
                    logger.info("file core-site.xml written");
                    OutputStream out1 = new FileOutputStream(this.hdfsHadoop);
                    conf.writeXml(out1);
                    logger.info("file hdfs-site.xml written");
                    OutputStream out2 = new FileOutputStream(this.mapredHadoop);
                    conf.writeXml(out2);
                    logger.info("file mapred-site.xml written");
                    out.close();
                    out1.close();
                    out2.close(); 
                    File hdfsDir = new File (this.hadoopTmpDir+"/dfs");
                    if (!hdfsDir.exists())
                        throw new CleverException (this.hadoopTmpDir+"/dfs directory does not exist. Create it with "+this.dirPermissions+" permissions");
                    File nameDir = new File (this.hadoopTmpDir+"/dfs/name");
                    if (!nameDir.exists())
                        throw new CleverException (this.hadoopTmpDir+"/dfs/name directory does not exist. Create it with "+this.dirPermissions+" permissions");
                    String files[] = nameDir.list();
                    for (String filename : files) {
                        File file = new File (filename);
                        if (file.isDirectory())
                            FileUtils.deleteDirectory(file);
                        else
                            file.delete();
                    }
                    logger.info("Name directory cleared");
                    File slavesFile = new File (this.slaves);
                    FileWriter writer = new FileWriter (slavesFile);
                    writer.write("");
                    writer.close();
                    logger.info("Slaves file cleared");
                } catch (FileNotFoundException ex) {
                    logger.error("One or more of the configuration files were not found: "+ex.getMessage());
                } catch (IOException ex) {
                    logger.error("Error writing configuration files: "+ex.getMessage());
                }
*/                /* try {
                    Runtime r = Runtime.getRuntime();
                    Process p = r.exec(this.format);
                    logger.info("Hdfs formatting started");
                    try {
                        p.waitFor();
                    } catch (InterruptedException ex) {
                        logger.error("Hdfs formatting interrupted");
                    }
                    logger.info ("Hdfs formatting finished");
                    try {
                        logger.info("Launching command: "+this.start);
                        Runtime r1 = Runtime.getRuntime();
                        //String[] cmd = {"/bin/sh", "-c", this.start};
                        Process p1 = r1.exec(this.start);
                        logger.info("Namenode started");
                        try {
                            p1.waitFor();
                        } catch (InterruptedException ex1) {
                            logger.error("Namenode interrupted");
                        }
                        logger.info ("Namenode finished");
                    } catch (IOException e1) {
                        logger.error("Error launching namenode daemon: "+e1.getMessage());
                    }  */
             /* } catch (IOException e) {
                    logger.error("Error formatting hdfs: "+e.getMessage());
                } */
            } catch (Exception e) {
                logger.error("Error initializing HadoopNamenode : " + e.getMessage());
            }
        }

        @Override
        public Class getPluginClass() {
            return this.cl;
        }

        @Override
        public Object getPlugin() {
            return this.plugin;
        }
        
        @Override
        public void shutDown() {}

        @Override
        public void handleNotification(Notification notification) throws CleverException {
            logger.info("Received notification "+notification.toString());
            logger.info ("Src: "+notification.getHostId()+" Type: "+notification.getId());
            if (notification.getId().compareTo(this.notifystart)==0) {
                String src = notification.getHostId(); //the src is both the nick in the chatroom and the hostname
                if (src.compareTo(this.hostname)==0) {
                    logger.info("Don't do anything because the HM is running on the CM machine and there cannot be a namenode and a datanode running on the same machine.");
                    return; //Non posso lanciare insieme namenode e datanode
                }
                logger.info("Stopping HDFS to update the slaves and relaunching it (using a hadoop script that use SSH to authenticate on all the slaves)");
                try {
                    logger.info("Launching command: "+this.stop);
                    Runtime r1 = Runtime.getRuntime();
                    //String[] cmd = {"/bin/sh", "-c", this.start};
                    Process p1 = r1.exec(this.stop);
                    logger.info("HDFS stopped");
                    try {
                        p1.waitFor();
                    } catch (InterruptedException ex1) {
                        logger.error("Stopping HDFS interrupted");
                    }
                    logger.info ("Finished stopping HDFS");
                } catch (IOException e1) {
                    logger.error("Error launching stopping daemon: "+e1.getMessage());
                }
                logger.info ("Initializing the environment on which the datanode will be started");
                ArrayList<Object> params = new ArrayList<Object>();
                params.add(this.hostname);
                params.add(this.namenodePort);
                params.add(this.dirPermissions);
                params.add(this.append);
                this.remoteInvocation(src, "HadoopDatanodeAgent", "initializeDatanode", true, params);
                logger.info("Updating slaves file adding the hostname \""+src+"\"");
                File slavesFile = new File (this.slaves);
                String content = null;
                try {
                    FileReader fileReader = new FileReader (slavesFile);
                    StringWriter stringWriter = new StringWriter();
                    while (true) {
                        int c = fileReader.read();
                        if (c==-1)
                            break;
                        stringWriter.write(c);
                    }
                    fileReader.close();
                    content = stringWriter.toString();
                    stringWriter.close();
                    FileWriter fileWriter = new FileWriter (this.slaves);
                    content += "\n"+src;
                    fileWriter.write(src);
                    fileWriter.close();
                } catch (IOException ex) {
                    throw new CleverException (ex);
                }
                logger.info ("The content of slaves file is now:\n"+content);
                logger.info ("Relaunching all the HDFS nodes (Using a hadoop script that use SSH to authenticate on all the slaves)");
                try {
                    logger.info("Launching command: "+this.start);
                    Runtime r1 = Runtime.getRuntime();
                    //String[] cmd = {"/bin/sh", "-c", this.start};
                    Process p1 = r1.exec(this.start);
                    logger.info("HDFS started");
                    try {
                        p1.waitFor();
                    } catch (InterruptedException ex1) {
                        logger.error("Starting HDFS interrupted");
                    }
                    logger.info ("Finished starting HDFS");
                } catch (IOException e1) {
                    logger.error("Error launching starting daemon: "+e1.getMessage());
                }
            }
        }
        
        
        
        
}