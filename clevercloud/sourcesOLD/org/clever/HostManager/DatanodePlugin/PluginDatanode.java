/*
 * The MIT License
 *
 * Copyright 2013 mariacristina sinagra.
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
package org.clever.HostManager.DatanodePlugin;

/**
 *
 * @author cristina
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import org.apache.log4j.Logger;
import org.clever.ClusterManager.HadoopNamenode.HadoopNamenodePlugin;
import org.clever.ClusterManager.StorageManager.StorageManagerPlugin;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Communicator.ModuleCommunicator;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMLTools.ParserXML;
import org.jdom.Element;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.clever.Common.XMLTools.FileStreamer;
import org.clever.HostManager.HadoopDatanode.HadoopDatanodePlugin;

public class PluginDatanode implements HadoopDatanodePlugin {

    private Logger logger;
    private Class cl;
    private ModuleCommunicator mc;
    private String hostName;
    private ParserXML pXML;
    private Agent owner;
    private String nodoHadoop = "Hadoop";
    private String coreHadoop;
    private String hdfsHadoop;
    private String mapredHadoop;
    private String namespaceID;
    private String start;
    private String stop;
    private String hadoopTmpDir;
    
    public PluginDatanode()  {
        this.logger = Logger.getLogger("PluginDatanode");
        try {
            logger.info("Read Configuration HadoopDatanodeAgent!");
            InputStream inxml = getClass().getResourceAsStream("/org/clever/HostManager/HadoopDatanode/configuration_datanode.xml");
            FileStreamer fs = new FileStreamer();
            ParserXML pars = new ParserXML(fs.xmlToString(inxml));
            this.coreHadoop = pars.getElementContent("coreSite");
            this.hdfsHadoop = pars.getElementContent("hdfsSite");
            this.mapredHadoop = pars.getElementContent("mapredSite");
            this.hadoopTmpDir = pars.getElementContent("hadoopTmpDir");
            logger.info(coreHadoop);
            try {
                this.hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                this.logger.error("Error getting local hostname :" + e.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            this.logger.error("Error reading configuration");
        }
    }

    @Override
    public void setOwner(Agent owner) {
        this.owner = owner;
    }

    @Override
    public void initializeDatanode(String namenodeHostname, String namenodePort, String dirPermissions, String appendFlag) throws CleverException {
        this.logger.debug("Entrato in initializeDatanode");
        Configuration c = new Configuration(true);
        this.logger.debug("Lancio");
        c.set("fs.default.name", "hdfs://"+namenodeHostname+":"+namenodePort);
        this.logger.debug("Aggiunta prima proprietà");
        c.set("hadoop.tmp.dir", this.hadoopTmpDir);
        this.logger.debug("Aggiunta seconda proprietà");
        c.set("dfs.datanode.data.dir.perm", dirPermissions);
        c.set("dfs.support.append", appendFlag);
        String confString = c.toString();
        this.logger.debug("Configuration:\n"+confString);
      /*  String tmpDir = c.get("hadoop.tmp.dir");
        this.logger.debug("Recuperata proprietà hadoop.tmp.dir: "+tmpDir);
        String dataDir = c.get("dfs.data.dir");
        this.logger.debug("Recuperata proprietà dfs.data.dir: "+dataDir);
        if (dataDir.startsWith("${hadoop.tmp.dir}"))
            dataDir = c.get("hadoop.tmp.dir") + dataDir.substring(17, dataDir.length());
        this.logger.debug("dataDir :"+dataDir);*/
        
        File tmpDir = new File (this.hadoopTmpDir+"/dfs");
	this.logger.info("Deleting old data dir...");
        boolean exists = tmpDir.exists();
        this.logger.debug("exists: "+exists);
        if (!exists)
            throw new CleverException (this.hadoopTmpDir+"/dfs directory doesn't exist. You must manually create it with "+dirPermissions+" permissions");
        File dataDir = new File (this.hadoopTmpDir+"/dfs/data");
        if (!dataDir.exists())
            throw new CleverException (this.hadoopTmpDir+"/dfs/data directory does not exist. You must manually create it with "+dirPermissions+" permissions");
        this.logger.debug("exists: "+dataDir.exists());
        String files[] = dataDir.list();
        for (String filename : files) {
            File file = new File (filename);
            if (file.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(file);
                } catch (IOException ex) {
                    throw new CleverException (ex);
                }
            } else
                file.delete();
        }
        logger.info("Data directory cleared"); 
        //Updating conf files
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
            c.writeXml(out);
            this.logger.info("file core-site.xml written");
            OutputStream out1 = new FileOutputStream(this.hdfsHadoop);
            c.writeXml(out1);
            this.logger.info("file hdfs-site.xml written");
            OutputStream out2 = new FileOutputStream(this.mapredHadoop);
            c.writeXml(out2);
            this.logger.info("file mapred-site.xml written");
            out.close();
            out1.close();
            out2.close();
            /* File hdfsDir = new File (this.hadoopTmpDir+"/dfs");
            if (hdfsDir.exists())
                FileUtils.deleteDirectory(hdfsDir);
            hdfsDir.mkdirs();
            hdfsDir = new File (this.hadoopTmpDir+"/dfs/name");
            hdfsDir.mkdirs();
            hdfsDir = new File (this.hadoopTmpDir+"/dfs/data");
            hdfsDir.mkdirs();
            this.logger.info("Hdfs directories deleted"); */
        } catch (FileNotFoundException ex) {
            logger.error("One or more of the configuration files were not found: "+ex.getMessage());
        } catch (IOException ex) {
            logger.error("Error writing configuration files: "+ex.getMessage());
        }
        //Finished to prepare for datanode launching. Return
    }

    @Override
    public void setStartCommand(String command) {
        this.start = command;
    }

    @Override
    public void setStopCommand(String command) {
        this.stop = command;
    }
}
