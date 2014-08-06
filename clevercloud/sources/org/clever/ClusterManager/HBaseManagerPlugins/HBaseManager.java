/*
 * The MIT License
 *
 * Copyright 2013 giovanni.
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

//AGGIUNGERE LA PROPRIETA' PER I FILE DEL SECONDARY NAMENODE E I METODI DI LANCIO DEL SECONDARY NAMENODE

package org.clever.ClusterManager.HBaseManagerPlugins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.logging.Level;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.log4j.Logger;
import org.clever.ClusterManager.HBaseManager.HBaseManagerPlugin;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.Shared.HostEntityInfo;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author giovanni
 */
public class HBaseManager implements HBaseManagerPlugin {

    private CmAgent owner;
    private Logger logger;
    private int checkpointTimeout;
    private ArrayList<HostManagerNodes> availableHMList;
    private HostManagerWatcher hostManagerWatcher;
    private Timer checkpointCountdown;
    
    public static final String HBASE_LAUNCHER_AGENT_CLASS_NAME = "HBaseLauncherAgent";
    
    public static final String HDFS_LOCATION_PROPERTY_NAME = "fs.default.name"; // hdfs://namenode:9000
    public static final int DEFAULT_NAMENODE_PORT = 9000;
    public static final String ZOOKEEPER_QUORUM_PROPERTY_NAME = "hbase.zookeeper.quorum"; // zookeeper1,zookeeper2,zookeeper3,...
    public static final String HADOOP_APPEND_FILES_PROPERTY_NAME = "dfs.support.append"; // true
    public static final boolean DEFAULT_HADOOP_APPEND_FILES_PROPERTY = true;
    public static final String HBASE_DIRECTORY_PROPERTY_NAME = "hbase.rootdir"; // hdfs://namenode:9000/hbase
    public static final String HBASE_CLUSTER_DISTRIBUITED_PROPERTY_NAME = "hbase.cluster.distributed"; //true
    public static final boolean DEFAULT_HBASE_CLUSTER_DISTRIBUITED_PROPERTY = true;
    public static final String HADOOP_NAMENODE_FILES_DIRECTORY_PROPERTY_NAME = "dfs.name.dir";
    public static final String DEFAULT_HADOOP_NAMENODE_FILES_DIRECTORY = "${hadoop.tmp.dir}/dfs/name";
    public static final String HADOOP_DATANODE_FILES_DIRECTORY_PROPERTY_NAME = "dfs.data.dir";
    public static final String DEFAULT_HADOOP_DATANODE_FILES_DIRECTORY = "${hadoop.tmp.dir}/dfs/data";
    public static final String HADOOP_MAX_XCIEVERS_NAME_PROPERTY = "dfs.datanode.max.xcievers";
    public static final int DEFAULT_HADOOP_MAX_XCIEVRES = 4096;
    public static final String HADOOP_FILES_REPLICATION_PROPERTY_NAME = "dfs.replication";
    public static final int DEFAULT_HADOOP_FILES_REPLICATION = 3;
    
    public static final int DEFAULT_BACKUP_MASTERS_NUMBER = 2;
    public static final int DEFAULT_ZOOKEEPER_PEERS_NUMBER = 5;
    public static final boolean DEFAULT_USE_SECONDARY_NAMENODE = true;
    public static final int DEFAULT_NODES_PER_HOST_MAX_NUMBER = 1;
    public static final boolean DEFAULT_LAUNCH_ONLY_ONE_MASTER_PER_HOST = true;
    public static final boolean DEFAULT_KEEP_CLUSTERS_SEPARED = false;
    public static final boolean DEFAULT_LAUNCH_SLAVES_ON_MASTER_NODES = false;
    public static final float DEFAULT_CLEVER_OCCUPATION_RATIO = 1;
    public static final float DEFAULT_HBASE_HADOOP_SLAVES_RATIO = 1;
    public static final boolean DEFAULT_JOIN_REGIONSERVERS_AND_DATANODES = true;
    
    private int requiredNamenodePort = HBaseManager.DEFAULT_NAMENODE_PORT;
    private boolean requiredHadoopAppendFiles = HBaseManager.DEFAULT_HADOOP_APPEND_FILES_PROPERTY;
    private boolean requiredHbaseClusterDistributed = HBaseManager.DEFAULT_HBASE_CLUSTER_DISTRIBUITED_PROPERTY;
    private String requiredHadoopNamenodeFilesDirectory = HBaseManager.DEFAULT_HADOOP_NAMENODE_FILES_DIRECTORY;
    private String requiredHadoopDatanodeFilesDirectory = HBaseManager.DEFAULT_HADOOP_DATANODE_FILES_DIRECTORY;

    
    private int requiredHadoopMaxXcievers = HBaseManager.DEFAULT_HADOOP_MAX_XCIEVRES;
    private int requiredHadoopFilesReplication = HBaseManager.DEFAULT_HADOOP_FILES_REPLICATION;
    private int requiredBackupMasterNumber = HBaseManager.DEFAULT_BACKUP_MASTERS_NUMBER;
    private int requiredZookeeperPeersNumber = HBaseManager.DEFAULT_ZOOKEEPER_PEERS_NUMBER;
    private boolean requiredUseSecondaryNamenode = HBaseManager.DEFAULT_USE_SECONDARY_NAMENODE;
    private int requiredNodesPerHostMaxNumber = HBaseManager.DEFAULT_NODES_PER_HOST_MAX_NUMBER;
    private boolean requiredLaunchOnlyOneMasterPerHost = HBaseManager.DEFAULT_LAUNCH_ONLY_ONE_MASTER_PER_HOST;
    private boolean requiredKeepClustersSepared = HBaseManager.DEFAULT_KEEP_CLUSTERS_SEPARED;
    private boolean requiredLaunchSlavesOnMasterNodes = HBaseManager.DEFAULT_LAUNCH_SLAVES_ON_MASTER_NODES;
    private float requiredCleverOccupationRatio = HBaseManager.DEFAULT_CLEVER_OCCUPATION_RATIO;
    private float requiredHbaseHadoopSlavesRatio = HBaseManager.DEFAULT_HBASE_HADOOP_SLAVES_RATIO;
    private boolean requiredJoinRegionserversAndDatanodes = HBaseManager.DEFAULT_JOIN_REGIONSERVERS_AND_DATANODES;

    private Configuration hadoopConfiguration;
    private Configuration hbaseConfiguration;
    
    private HostManagerNodes namenodeHost; //to create configurations
    private ArrayList<String> zookeeperPeersIP; //to create configurations
    
    
    
    
    
    @Override
    public void setOwner(Agent owner) {
        this.owner = (CmAgent) owner;
    }
    
    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
   
    @Override
    public Integer getRequiredNamenodePort() {
        return Integer.valueOf(requiredNamenodePort);
    }

    @Override
    public Boolean isRequiredHadoopAppendFiles() {
        return Boolean.valueOf(requiredHadoopAppendFiles);
    }
  
    @Override
    public Boolean isRequiredHbaseClusterDistributed() {
        return Boolean.valueOf(requiredHbaseClusterDistributed);
    }

    @Override
    public String getRequiredHadoopNamenodeFilesDirectory() {
        return requiredHadoopNamenodeFilesDirectory;
    }

    @Override
    public String getRequiredHadoopDatanodeFilesDirectory() {
        return requiredHadoopDatanodeFilesDirectory;
    }

    @Override
    public Integer getRequiredHadoopMaxXcievers() {
        return Integer.valueOf(requiredHadoopMaxXcievers);
    }

    @Override
    public Integer getRequiredHadoopFilesReplication() {
        return Integer.valueOf(requiredHadoopFilesReplication);
    }

    @Override
    public Integer getRequiredBackupMasterNumber() {
        return Integer.valueOf(requiredBackupMasterNumber);
    }

    @Override
    public Integer getRequiredZookeeperPeersNumber() {
        return Integer.valueOf(requiredZookeeperPeersNumber);
    }

    @Override
    public Boolean isRequiredUseSecondaryNamenode() {
        return Boolean.valueOf(requiredUseSecondaryNamenode);
    }

    @Override
    public Integer getRequiredNodesPerHostMaxNumber() {
        return Integer.valueOf(requiredNodesPerHostMaxNumber);
    }

    @Override
    public Boolean isRequiredLaunchOnlyOneMasterPerHost() {
        return Boolean.valueOf(requiredLaunchOnlyOneMasterPerHost);
    }

    @Override
    public Boolean isRequiredKeepClustersSepared() {
        return Boolean.valueOf(requiredKeepClustersSepared);
    }

    @Override
    public Boolean isRequiredLaunchSlavesOnMasterNodes() {
        return Boolean.valueOf(requiredLaunchSlavesOnMasterNodes);
    }

    @Override
    public Float getRequiredCleverOccupationRatio() {
        return Float.valueOf(requiredCleverOccupationRatio);
    }

    @Override
    public Float getRequiredHbaseHadoopSlavesRatio() {
        return Float.valueOf(requiredHbaseHadoopSlavesRatio);
    }

    @Override
    public Boolean isRequiredJoinRegionserversAndDatanodes() {
        return Boolean.valueOf(requiredJoinRegionserversAndDatanodes);
    }

    @Override
    public void setRequiredNamenodePort(Integer requiredNamenodePort) throws CleverException {
        if (this.isHadoopRunning() || this.isHBaseRunning())
            throw new CleverException ("Impossibile cambiare il valore di questa proprietà con nodi Hadoop o HBase in esecuzione");
        this.requiredNamenodePort = requiredNamenodePort.intValue();
    }

    @Override
    public void setRequiredHadoopAppendFiles(Boolean requiredHadoopAppendFiles) throws CleverException {
        if (this.isHadoopRunning() || this.isHBaseRunning())
            throw new CleverException ("Impossibile cambiare il valore di questa proprietà con nodi Hadoop o HBase in esecuzione");
        this.requiredHadoopAppendFiles = requiredHadoopAppendFiles.booleanValue();
    }

    @Override
    public void setRequiredHbaseClusterDistributed(Boolean requiredHbaseClusterDistributed) throws CleverException {
        if (this.isHBaseRunning())
            throw new CleverException ("Impossibile cambiare il valore di questa proprietà con nodi HBase in esecuzione");
        this.requiredHbaseClusterDistributed = requiredHbaseClusterDistributed.booleanValue();    
    }

    @Override
    public void setRequiredHadoopNamenodeFilesDirectory(String requiredHadoopNamenodeFilesDirectory) throws CleverException {
        if (this.isHadoopRunning())
            throw new CleverException ("Impossibile cambiare il valore di questa proprietà con nodi Hadoop in esecuzione");
        this.requiredHadoopNamenodeFilesDirectory = requiredHadoopNamenodeFilesDirectory;
    }

    @Override
    public void setRequiredHadoopDatanodeFilesDirectory(String requiredHadoopDatanodeFilesDirectory) throws CleverException {
        if (this.isHadoopRunning())
            throw new CleverException ("Impossibile cambiare il valore di questa proprietà con nodi Hadoop in esecuzione");
        this.requiredHadoopDatanodeFilesDirectory = requiredHadoopDatanodeFilesDirectory;
    }

    @Override
    public void setRequiredHadoopMaxXcievers(Integer requiredHadoopMaxXcievers) throws CleverException {
        if (this.isHadoopRunning())
            throw new CleverException ("Impossibile cambiare il valore di questa proprietà con nodi Hadoop in esecuzione");
        this.requiredHadoopMaxXcievers = requiredHadoopMaxXcievers.intValue();
    }

    @Override
    public void setRequiredHadoopFilesReplication(Integer requiredHadoopFilesReplication) throws CleverException {
        if (this.isHadoopRunning())
            throw new CleverException ("Impossibile cambiare il valore di questa proprietà con nodi Hadoop in esecuzione");
        this.requiredHadoopFilesReplication = requiredHadoopFilesReplication.intValue();
    }

    @Override
    public void setRequiredBackupMasterNumber(Integer requiredBackupMasterNumber) {
        this.requiredBackupMasterNumber = requiredBackupMasterNumber.intValue();
        //dire di gestire il nuovo numero richiesto di masters
    }

    @Override
    public void setRequiredZookeeperPeersNumber(Integer requiredZookeeperPeersNumber) throws CleverException {
        if (this.isHBaseRunning() || this.isZookeeperRunning())
            throw new CleverException ("Impossibile cambiare il valore di questa proprietà con nodi HBase o Zookeeper in esecuzione");
        this.requiredZookeeperPeersNumber = requiredZookeeperPeersNumber.intValue();
    }

    @Override
    public void setRequiredUseSecondaryNamenode(Boolean requiredUseSecondaryNamenode) {
        this.requiredUseSecondaryNamenode = requiredUseSecondaryNamenode.booleanValue();
        //dire di gestire la proprietà
    }

    @Override
    public void setRequiredNodesPerHostMaxNumber(Integer requiredNodesPerHostMaxNumber) {
        this.requiredNodesPerHostMaxNumber = requiredNodesPerHostMaxNumber.intValue();
        //dire di gestire la proprietà
    }

    @Override
    public void setRequiredLaunchOnlyOneMasterPerHost(Boolean requiredLaunchOnlyOneMasterPerHost) {
        this.requiredLaunchOnlyOneMasterPerHost = requiredLaunchOnlyOneMasterPerHost.booleanValue();
        //dire di gestire la proprietà
    }

    @Override
    public void setRequiredKeepClustersSepared(Boolean requiredKeepClustersSepared) {
        this.requiredKeepClustersSepared = requiredKeepClustersSepared.booleanValue();
        //dire di gestire la proprietà
    }

    @Override
    public void setRequiredLaunchSlavesOnMasterNodes(Boolean requiredLaunchSlavesOnMasterNodes) {
        this.requiredLaunchSlavesOnMasterNodes = requiredLaunchSlavesOnMasterNodes.booleanValue();
        //dire di gestire la proprietà
    }

    @Override
    public void setRequiredCleverOccupationRatio(Float requiredCleverOccupationRatio) {
        this.requiredCleverOccupationRatio = requiredCleverOccupationRatio.floatValue();
        //dire di gestire la proprietà
    }

    @Override
    public void setRequiredHbaseHadoopSlavesRatio(Float requiredHbaseHadoopSlavesRatio) {
        this.requiredHbaseHadoopSlavesRatio = requiredHbaseHadoopSlavesRatio.floatValue();
        //dire di gestire la proprietà
    }

    @Override
    public void setRequiredJoinRegionserversAndDatanodes(Boolean requiredJoinRegionserversAndDatanodes) {
        this.requiredJoinRegionserversAndDatanodes = requiredJoinRegionserversAndDatanodes.booleanValue();
        //dire di gestire la proprietà
    }

    @Override
    public String getHBasePropertyValue(String name) {
        return this.hbaseConfiguration.get(name);
    }

    @Override
    public String getHadoopPropertyValue(String name) {
        return this.hadoopConfiguration.get(name);
    }

    @Override
    public void setHBasePropertyValue(String name, String value) throws CleverException {
        String property = this.hbaseConfiguration.get(name); //se la proprietà è null vuol dire che non esiste
        if (property==null)
            throw new CleverException ("La proprietà specificata non fa parte della configurazione. Perciò non le può essere assegnato un valore.");
        this.hbaseConfiguration.set(name, value);
    }

    @Override
    public void setHadoopPropertyValue(String name, String value) throws CleverException {
        String property = this.hadoopConfiguration.get(name); //se la proprietà è null vuol dire che non esiste
        if (property==null)
            throw new CleverException ("La proprietà specificata non fa parte della configurazione. Perciò non le può essere assegnato un valore.");
        this.hadoopConfiguration.set(name, value);
    }

  /*  @Override
    public void initializeHadoopConfiguration(String conf) throws CleverException {
        this.hadoopConfiguration = new Configuration(true); //inserisce i valori di default delle proprietà di Hadoop
    wer    this.hadoopConfiguration.addResource(conf); //sovrascrive i valori di defautl con quelli specificati nel file di configuratione dell'Agent.
    }

    @Override
    public void initializeHBaseConfiguration(String conf) throws CleverException {
        this.hbaseConfiguration = HBaseConfiguration.create(); //inserisce i valori di default delle proprietà di HBase
    wer    this.hbaseConfiguration.addResource(conf); //sovrascrive i valori di defautl con quelli specificati nel file di configuratione dell'Agent.
    } */

    @Override
    public void setCheckpointTimeout(int millisecs) {
        this.checkpointTimeout = millisecs;
    }
    
    //DA MIGLIORARE
    public void addAvailableHMs (ArrayList<HostEntityInfo> newHostManagers) {
        for (HostEntityInfo o : newHostManagers) {
            HostManagerNodes newHMNodes = new HostManagerNodes();
            String target = o.getNick();
            String agent = HBaseManager.HBASE_LAUNCHER_AGENT_CLASS_NAME;
            String method = "getIpAddress";
            String IP = null;
            try {
                IP = (String) this.owner.remoteInvocation(target, agent, method, true, new ArrayList());
            } catch (CleverException ex) {
                this.logger.error("Error during IP request on Host Manager \""+target+"\": " + ex.getMessage());
            }
            newHMNodes.setHM(o);
            newHMNodes.setIP(IP);
            this.availableHMList.add(newHMNodes);
        }
    }
    
    public void removeAvailableHMs (int decrement) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void reDistribuiteNodes () {
        // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    @Override
    public String forceCheckpoint() throws CleverException{
        this.hostManagerWatcher.forceCheckpoint();
        int availableHMNumber = this.availableHMList.size();
        return Integer.toString(availableHMNumber);
    }

    @Override
    public void initialize (Element element) throws Exception {
        this.logger.debug("HBASEMANAGER DEBUG: entrato in init()");
        this.availableHMList = new ArrayList<HostManagerNodes>();
        this.hostManagerWatcher = new HostManagerWatcher ( /* this.checkpointTimeout, */ this.availableHMList, this.requiredCleverOccupationRatio, this, this.owner, this.logger);
        // this.hostManagerWatcher.start(); //<--- Se extends Thread
        this.checkpointCountdown = new Timer(true);
        this.checkpointCountdown.scheduleAtFixedRate(this.hostManagerWatcher, this.checkpointTimeout, this.checkpointTimeout);
        this.logger.debug("HBASEMANAGER DEBUG: HostManagerWatcher lauched!");
        
        //CREO LE CONFIGURAZIONI DI HBASE E HADOOP CON I LORO VALORI DI DEFAULT
        this.hadoopConfiguration = new Configuration (true);
        this.hbaseConfiguration = HBaseConfiguration.create();
        this.logger.debug("HBASEMANAGER DEBUG: Hadoop configuration: "+this.hadoopConfiguration);
        if (this.hadoopConfiguration!=null)  {  
            this.logger.debug("HBASEMANAGER DEBUG: Hadoop configuration: "+this.hadoopConfiguration.toString());
            this.logger.debug("HBASEMANAGER DEBUG: fs.default.name: "+this.hadoopConfiguration.get("fs.default.name"));
        }
        this.logger.debug("HBASEMANAGER DEBUG: HBase configuration:"+this.hbaseConfiguration);
        if (this.hbaseConfiguration!=null) {
            this.logger.debug("HBASEMANAGER DEBUG: HBase configuration:"+this.hbaseConfiguration.toString());
            this.logger.debug("HBASEMANAGER DEBUG: hbase.rootdir:"+this.hbaseConfiguration.get("hbase.rootdir"));
        }
        //INSERISCO I VALORI DELLE PROPRIETÀ DI HBASE E HADOOP SPECIFICATE NEL FILE DI CONFIGURAZIONE
        NodeList nodeList = element.getElementsByTagName("HadoopProperties"); //se esiste deve essere unico
        if (nodeList!=null && nodeList.getLength()>0) {
            NodeList properties = ((Element)nodeList.item(0)).getElementsByTagName("property");
            if (properties!=null && properties.getLength()>0) {
                for (int i=0; i<properties.getLength(); i++) {
                    Element property = (Element) properties.item(i);
                    NodeList nameList = property.getElementsByTagName("name");
                    if (nameList.getLength()>1)
                        throw new CleverException ("Formatting error in tag \"HadoopProperties\" of plugin configuration file.");
                    NodeList valueList = property.getElementsByTagName("value");
                    if (valueList.getLength()!=1)
                        throw new CleverException ("Formatting error in tag \"HadoopProperties\" of plugin configuration file.");
                    this.hadoopConfiguration.set(nameList.item(0).getTextContent(), valueList.item(0).getTextContent());
                }  
            }
        }
        nodeList = element.getElementsByTagName("HBaseProperties"); //se esiste deve essere unico
        if (nodeList!=null && nodeList.getLength()>0) {
            NodeList properties = ((Element)nodeList.item(0)).getElementsByTagName("property");
            if (properties!=null && properties.getLength()>0) {
                for (int i=0; i<properties.getLength(); i++) {
                    Element property = (Element) properties.item(i);
                    NodeList nameList = property.getElementsByTagName("name");
                    if (nameList.getLength()>1)
                        throw new CleverException ("Formatting error in tag \"HBaseProperties\" of plugin configuration file.");
                    NodeList valueList = property.getElementsByTagName("value");
                    if (valueList.getLength()!=1)
                        throw new CleverException ("Formatting error in tag \"HBaseProperties\" of plugin configuration file.");
                    this.hbaseConfiguration.set(nameList.item(0).getTextContent(), valueList.item(0).getTextContent());
                }  
            }
        }
        
        //INSERISCO I VALORI DEI TAG DELLE CONFIGURAZIONI DEL PLUGIN E, SE ALCUNI DI ESSI
        //ERANO VALORI DI CONFIGURAZIONE DI HBASE E HADOOP, SOVRASCRIVERANNO QUELLI INSERITI
        //PRECEDENTEMENTE.
        nodeList = element.getElementsByTagName("namenodePort"); //se esiste deve essere unico
        if (nodeList!=null && nodeList.getLength()>0) {
            String optionValue = nodeList.item(0).getTextContent();
            this.logger.debug("HBASEMANAGER DEBUG: namenodePort:"+optionValue);
            if (optionValue!=null && !optionValue.isEmpty())
                this.requiredNamenodePort = Integer.parseInt(optionValue);
        }
        nodeList = element.getElementsByTagName("backupMastersNumber"); //se esiste deve essere unico
        if (nodeList!=null && nodeList.getLength()>0) {
            String optionValue = nodeList.item(0).getTextContent();
            if (optionValue!=null && !optionValue.isEmpty())
                this.requiredBackupMasterNumber = Integer.parseInt(optionValue);
        }
        nodeList = element.getElementsByTagName("zookeeperPeersNumber"); //se esiste deve essere unico
        if (nodeList!=null && nodeList.getLength()>0) {
            String optionValue = nodeList.item(0).getTextContent();
            if (optionValue!=null && !optionValue.isEmpty())
                this.requiredZookeeperPeersNumber = Integer.parseInt(optionValue);
        }
        nodeList = element.getElementsByTagName("useSecondaryNamenode"); //se esiste deve essere unico
        if (nodeList!=null && nodeList.getLength()>0) {
            String optionValue = nodeList.item(0).getTextContent();
            if (optionValue!=null && !optionValue.isEmpty())
                this.requiredUseSecondaryNamenode = Boolean.parseBoolean(optionValue);
        }
        nodeList = element.getElementsByTagName("nodesPerHostMaxNumber"); //se esiste deve essere unico
        if (nodeList!=null && nodeList.getLength()>0) {
            String optionValue = nodeList.item(0).getTextContent();
            if (optionValue!=null && !optionValue.isEmpty())
                this.requiredNodesPerHostMaxNumber = Integer.parseInt(optionValue);
        }
        nodeList = element.getElementsByTagName("launchOnlyOneMasterPerHost"); //se esiste deve essere unico
        if (nodeList!=null && nodeList.getLength()>0) {
            String optionValue = nodeList.item(0).getTextContent();
            if (optionValue!=null && !optionValue.isEmpty())
                this.requiredLaunchOnlyOneMasterPerHost = Boolean.parseBoolean(optionValue);
        }
        nodeList = element.getElementsByTagName("keepClustersSepared"); //se esiste deve essere unico
        if (nodeList!=null && nodeList.getLength()>0) {
            String optionValue = nodeList.item(0).getTextContent();
            if (optionValue!=null && !optionValue.isEmpty())
                this.requiredKeepClustersSepared = Boolean.parseBoolean(optionValue);
        }
        nodeList = element.getElementsByTagName("launchSlavesOnMasterNodes"); //se esiste deve essere unico
        if (nodeList!=null && nodeList.getLength()>0) {
            String optionValue = nodeList.item(0).getTextContent();
            if (optionValue!=null && !optionValue.isEmpty())
                this.requiredLaunchSlavesOnMasterNodes = Boolean.parseBoolean(optionValue);
        }
        nodeList = element.getElementsByTagName("cleverOccupationRatio"); //se esiste deve essere unico
        if (nodeList!=null && nodeList.getLength()>0) {
            String optionValue = nodeList.item(0).getTextContent();
            if (optionValue!=null && !optionValue.isEmpty())
                this.requiredCleverOccupationRatio = Integer.parseInt(optionValue);
        }
        nodeList = element.getElementsByTagName("HBase-HadoopSlavesRatio"); //se esiste deve essere unico
        if (nodeList!=null && nodeList.getLength()>0) {
            String optionValue = nodeList.item(0).getTextContent();
            if (optionValue!=null && !optionValue.isEmpty())
                this.requiredHbaseHadoopSlavesRatio = Integer.parseInt(optionValue);
        }
        nodeList = element.getElementsByTagName("joinRegionserversAndDatanodes"); //se esiste deve essere unico
        if (nodeList!=null && nodeList.getLength()>0) {
            String optionValue = nodeList.item(0).getTextContent();
            if (optionValue!=null && !optionValue.isEmpty())
                this.requiredJoinRegionserversAndDatanodes = Boolean.parseBoolean(optionValue);
        }
        
    }
    
    
    private boolean isHadoopRunning() {
        boolean isRunning=false;
        for (HostManagerNodes h : this.availableHMList) {
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.NAMENODE)==0 || n.compareTo(HostManagerNodes.SECONDARY_NAMENODE)==0 || n.compareTo(HostManagerNodes.DATANODE)==0) {
                    isRunning = true;
                    break;
                }
            }
            if (isRunning)
                break;
        }
        return isRunning;
    }
    
    private boolean isZookeeperRunning() {
        boolean isRunning=false;
        for (HostManagerNodes h : this.availableHMList) {
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.ZOOKEEPER)==0) {
                    isRunning = true;
                    break;
                }
            }
            if (isRunning)
                break;
        }
        return isRunning;
    }
    
    private boolean isHBaseRunning() {
        boolean isRunning=false;
        for (HostManagerNodes h : this.availableHMList) {
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.HBASE_MASTER)==0 || n.compareTo(HostManagerNodes.REGIONSERVER)==0 ) {
                    isRunning = true;
                    break;
                }
            }
            if (isRunning)
                break;
        }
        return isRunning;
    }
    
    private boolean isHBaseMasterRunning() {
        ArrayList<HostManagerNodes> hbaseHosts = this.getHostsWithHBase(this.availableHMList);
        ArrayList<HostManagerNodes> hbaseMasterHosts = this.getHostsWithHBaseMasters(hbaseHosts);
        if (!hbaseMasterHosts.isEmpty())
            return true;
        else
            return false;
    }
    
    private ArrayList<HostManagerNodes> getHostsWithNodes (int number, ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            int numNodes = h.getRunningNodes().size();
            if (numNodes == number)
                result.add(h);
        }
        return result;
    }
    
    private ArrayList<HostManagerNodes> getHostsWithNodesOrLess (int number, ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            int numNodes = h.getRunningNodes().size();
            if (numNodes <= number)
                result.add(h);
        }
        return result;
    }
    
    private ArrayList<HostManagerNodes> getHostsWithoutHadoop (ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            boolean hasHadoopNode = false;
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.NAMENODE)==0 || n.compareTo(HostManagerNodes.SECONDARY_NAMENODE)==0 || n.compareTo(HostManagerNodes.DATANODE)==0) {
                    hasHadoopNode = true;
                    break;
                }
            }
            if (!hasHadoopNode)
                result.add(h);
        }
        return result;
    }
    
    private ArrayList<HostManagerNodes> getHostsWithoutZookeeper (ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            boolean hasZookeeperNode = false;
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.ZOOKEEPER)==0) {
                    hasZookeeperNode = true;
                    break;
                }
            }
            if (!hasZookeeperNode)
                result.add(h);
        }
        return result;
    }
    
    private ArrayList<HostManagerNodes> getHostsWithoutHBase (ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            boolean hasHBaseNode = false;
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.HBASE_MASTER)==0 || n.compareTo(HostManagerNodes.REGIONSERVER)==0 ) {
                    hasHBaseNode = true;
                    break;
                }
            }
            if (!hasHBaseNode)
                result.add(h);
        }
        return result;
    }
    
    private ArrayList<HostManagerNodes> getHostsWithHadoop (ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.NAMENODE)==0 || n.compareTo(HostManagerNodes.SECONDARY_NAMENODE)==0 || n.compareTo(HostManagerNodes.DATANODE)==0) {
                    result.add(h);
                    break;
                }
            }
        }
        return result;
    }
    
    private ArrayList<HostManagerNodes> getHostsWithZookeeper (ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.ZOOKEEPER)==0) {
                    result.add(h);
                    break;
                }
            }
        }
        return result;
    }
    
    private ArrayList<HostManagerNodes> getHostsWithHBase (ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.HBASE_MASTER)==0 || n.compareTo(HostManagerNodes.REGIONSERVER)==0 ) {
                    result.add(h);
                    break;
                }
            }
        }
        return result;
    }
    
    private ArrayList<HostManagerNodes> getHostsWithMasters (ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.NAMENODE)==0 || n.compareTo(HostManagerNodes.SECONDARY_NAMENODE)==0 || n.compareTo(HostManagerNodes.HBASE_MASTER)==0 || n.compareTo(HostManagerNodes.ZOOKEEPER)==0 ) {
                    result.add(h);
                    break;
                }
            }
        }
        return result;
    }
    
    private ArrayList<HostManagerNodes> getHostsWithHadoopMasters (ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.NAMENODE)==0 || n.compareTo(HostManagerNodes.SECONDARY_NAMENODE)==0 ) {
                    result.add(h);
                    break;
                }
            }
        }
        return result;
    }
    
    private ArrayList<HostManagerNodes> getHostsWithHBaseMasters (ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.HBASE_MASTER)==0) {
                    result.add(h);
                    break;
                }
            }
        }
        return result;
    }
    
    private ArrayList<HostManagerNodes> getHostsWithoutMasters (ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            boolean hasMasterNode = false;
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.NAMENODE)==0 || n.compareTo(HostManagerNodes.SECONDARY_NAMENODE)==0 || n.compareTo(HostManagerNodes.HBASE_MASTER)==0 || n.compareTo(HostManagerNodes.ZOOKEEPER)==0 ) {
                    hasMasterNode = true;
                    break;
                }
            }
            if (!hasMasterNode)
                result.add(h);
        }
        return result;
    }
    
    private ArrayList<HostManagerNodes> getHostsWithoutHBaseMasters (ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            boolean hasMasterNode = false;
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.HBASE_MASTER)==0) {
                    hasMasterNode = true;
                    break;
                }
            }
            if (!hasMasterNode)
                result.add(h);
        }
        return result;
    }

    private ArrayList<HostManagerNodes> getHostsWithoutHadoopMasters (ArrayList<HostManagerNodes> list) {
        ArrayList<HostManagerNodes> result = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : list) {
            boolean hasMasterNode = false;
            for (String n : h.getRunningNodes()) {
                if (n.compareTo(HostManagerNodes.NAMENODE)==0 || n.compareTo(HostManagerNodes.SECONDARY_NAMENODE)==0 ) {
                    hasMasterNode = true;
                    break;
                }
            }
            if (!hasMasterNode)
                result.add(h);
        }
        return result;
    }
    
    private int getMinimumRunningNodesNumber (ArrayList<HostManagerNodes> list) {
        int minNodesNumber = Integer.MAX_VALUE; //impossibile
        for (HostManagerNodes h : list) {
            if (h.getRunningNodes().size() < minNodesNumber) {
                minNodesNumber = h.getRunningNodes().size();
            }
        }
        return minNodesNumber;
    }
    
    private HostManagerNodes getNamenodeHost () {
        HostManagerNodes result = null;
        for (HostManagerNodes h : this.availableHMList) {
            boolean trovato = false;
            for (String n : h.getRunningNodes()) 
                if (n.compareTo(HostManagerNodes.NAMENODE)==0) {
                    trovato = true;
                    break;
                }
            if (trovato) {
                result = h;
                break;
            }
        }
        return result;
    }
    
    private HostManagerNodes getSecondaryNamenodeHost () {
        HostManagerNodes result = null;
        for (HostManagerNodes h : this.availableHMList) {
            boolean trovato = false;
            for (String n : h.getRunningNodes()) 
                if (n.compareTo(HostManagerNodes.SECONDARY_NAMENODE)==0) {
                    trovato = true;
                    break;
                }
            if (trovato) {
                result = h;
                break;
            }
        }
        return result;
    }
    
    /**
     * Da usare solo nel metodo caluculareRequiredFsDefaultName().
     * @return 
     */
    private String getFsDefaultNamePropertyValue() {
        return "hdfs://"+this.namenodeHost+":"+this.requiredNamenodePort;
    }
    
    /**
     * Da usare solo nel metodo caluclateZookeeperQuorumProperty().
     * @return 
     */
    private String getZookeeperQuorumPropertyValue() {
        String value = "";
        for (String ip : this.zookeeperPeersIP)
            value += ","+ip;
        return value.substring(1); //tolgo la prima "," e ritorno
    }
    
    /**
     * Da usare solo nel metodo getHBaseRootdirProperty().
     * @return 
     */
    private String getHBaseRootdirPropertyValue() {
        return "hdfs://"+this.getNamenodeHost().getIP()+":"+this.requiredNamenodePort+"/hbase";
    }
    
    /**
     * Da usare solo nel lancio del namenode, nel metodo startNamenode().
     */
    private void completeHadoopConfiguration() {
        this.hadoopConfiguration.set(HBaseManager.HDFS_LOCATION_PROPERTY_NAME, this.getFsDefaultNamePropertyValue());
        this.hadoopConfiguration.set(HBaseManager.HADOOP_FILES_REPLICATION_PROPERTY_NAME, Integer.toString(this.requiredHadoopFilesReplication));
        this.hadoopConfiguration.set(HBaseManager.HADOOP_MAX_XCIEVERS_NAME_PROPERTY, Integer.toString(this.requiredHadoopMaxXcievers));
        this.hadoopConfiguration.set(HBaseManager.HADOOP_DATANODE_FILES_DIRECTORY_PROPERTY_NAME, this.requiredHadoopDatanodeFilesDirectory);
        this.hadoopConfiguration.set(HBaseManager.HADOOP_NAMENODE_FILES_DIRECTORY_PROPERTY_NAME, this.requiredHadoopNamenodeFilesDirectory);
        this.requiredHadoopAppendFiles = true; //forzo questa proprietà a true
        this.hadoopConfiguration.set(HBaseManager.HADOOP_APPEND_FILES_PROPERTY_NAME, Boolean.toString(this.requiredHadoopAppendFiles));
    }
    
    /**
     * Da usare solo nel lancio del cluster zookeeper, nel metodo startZookeeperQuorum().
     */
    private void completeZookeeperConfiguration() {
        this.hbaseConfiguration.set(HBaseManager.ZOOKEEPER_QUORUM_PROPERTY_NAME, this.getZookeeperQuorumPropertyValue());
    }
    
    /**
     * Da usare solo nel lancio del master di HBase, nel metodo startHBaseMaster().
     */
    private void completeHBaseConfiguration () {
        //si da per scontato che sia già stato chiamato il metodo "completeZookeeperConfiguration()"
        //che non viene chiamato più per non generare confusioni.
        this.hbaseConfiguration.set(HBaseManager.HBASE_DIRECTORY_PROPERTY_NAME, this.getHBaseRootdirPropertyValue());
        //considero che sia già stata forzata la proprietà dfs.append.files a true
        this.hbaseConfiguration.set(HBaseManager.HADOOP_APPEND_FILES_PROPERTY_NAME, Boolean.toString(this.requiredHadoopAppendFiles));
        //forzo la proprietà a true
        this.requiredHbaseClusterDistributed = true;
        this.hbaseConfiguration.set(HBaseManager.HBASE_CLUSTER_DISTRIBUITED_PROPERTY_NAME, Boolean.toString(this.requiredHbaseClusterDistributed));
    }
    
    private String writeConfigurationXmlString (Configuration conf) {
        String configurationString = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream ();
        try {
            conf.writeXml(out);
        } catch (IOException ex) {
            this.logger.error("Error while creating Configuration xml :"+ex.getMessage());
        }
        try {
            configurationString = out.toString("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            this.logger.error("Error while creating Configuration xml :"+ex.getMessage());
        }
        return configurationString;
    }

    @Override
    public void startNamenode() throws CleverException {
        this.logger.debug("HBASEMANAGER DEBUG: entrato in startNamenode(): ");
        this.forceCheckpoint(); //aggiorno gli host.
        //Per il namenode scelgo il primo Host Manager con meno nodi lanciati
        //Vedo il numero minimo di nodi lanciati sugli host manager e scelgo un host
        //manager con quel numero di nodi per lanciare il namenode
        this.logger.debug("HBASEMANAGER DEBUG: startNamenode(): fatto checkpoint");
        if (this.isHadoopRunning()) {
            throw new CleverException ("Impossibile lanciare il namenode con il cluster Hadoop già in esecuzione");
        }
        //cerco un host manager con il minimo numero di nodi in esecuzione e
        //in cui non sia in esecuzione un altro nodo di Hadoop.
        ArrayList<HostManagerNodes> hostWithoutHadoop = this.getHostsWithoutHadoop(this.availableHMList);
        if (hostWithoutHadoop.isEmpty())
            throw new CleverException("Impossibile lanciare il namenode. Non ci sono host disponibili.");
        //Se non è stata prima lanciata la formattazione ritorno errore
        if (this.namenodeHost==null)
            throw new CleverException("Impossibile lanciare il namenode. Non è stato formattato l'HadoopFS.");
        //lancio il namenode sullo stesso in cui ho formattato la cartella name
        HostManagerNodes newNamenodeHost = this.namenodeHost;
        this.logger.debug("HBASEMANAGER DEBUG: startNamenode(): scelto l'host per il namenode: "+newNamenodeHost.getHM().getNick());
        this.logger.debug("HBASEMANAGER DEBUG: startNamenode(): letto l'ip del namenode"+this.namenodeHost.getIP());
        this.completeHadoopConfiguration(); //completo la configurazione delle proprietà di Hadoop con l'IP del namenode.
        this.logger.debug("HBASEMANAGER DEBUG: startNamenode(): completate le configurazioni di Hadoop");
        String target = newNamenodeHost.getHM().getNick();
        String agent = HBaseManager.HBASE_LAUNCHER_AGENT_CLASS_NAME;
        String method = "startNamenode";
        
        //Creo la stringa con le proprietà da passare
        String configurationString = this.writeConfigurationXmlString(this.hadoopConfiguration);
        this.logger.debug("HBASEMANAGER DEBUG: startNamenode(): configurazione:\n"+configurationString);

        ArrayList<Object> launchingParams = new ArrayList<Object>();
        launchingParams.add(configurationString);
        this.logger.debug("HBASEMANAGER DEBUG: startNamenode(): preparata la formattazione e il lancio del namenode");
 
        newNamenodeHost.getRunningNodes().add(HostManagerNodes.NAMENODE); //dico che su quell'host ho lanciato il namenode
        this.logger.debug("HBASEMANAGER DEBUG: dopo lancio namenode: " + this.availableHMList.get(0).getRunningNodes().get(0));
        
        this.owner.remoteInvocation(target, agent, method, false, launchingParams); //lancio il namenode
        this.logger.debug("HBASEMANAGER DEBUG: startNamenode(): lancio del namenode effettuto");
    }

    @Override
    public void stopNamenode() throws CleverException {
        this.forceCheckpoint(); //aggiorno gli host.
        //Cerco il namenode
        if (this.namenodeHost==null)
            throw new CleverException ("Non esiste nessun namenode.");
        //Se i cluster Hadoop e HBase sono attivi non si può stoppare il namenode
        if (this.getSecondaryNamenodeHost()!=null || this.isDatanodeRunning() || this.isHBaseRunning())
            throw new CleverException ("Impossibile stoppare manualmente il namenode se sono attivi dei nodi dei cluster Hadoop o HBase");
        //Stoppo il namenode
        String target = this.namenodeHost.getHM().getNick();
        String agent = HBaseManager.HBASE_LAUNCHER_AGENT_CLASS_NAME;
        String method = "stopNamenode";
        //dico che in questo nodo non c'è più il namenode
        for (String n : this.namenodeHost.getRunningNodes())
            if (n.compareTo(HostManagerNodes.NAMENODE)==0) {
                this.namenodeHost.getRunningNodes().remove(n);
                break;
            }
        this.owner.remoteInvocation(target, agent, method, false, new ArrayList());
        this.namenodeHost = null;
    }

    @Override
    public void startDatanode() throws CleverException {
        this.logger.debug("HBASEMANAGER DEBUG: Entrato in startDatanode()");
        this.forceCheckpoint(); //aggiorno gli host.
        //Per il datanode scelgo il un nodo su cui non gira nessun altro nodo di Hadoop.
        //Tra di essi scelgo quello con carico minore.
        if (this.getNamenodeHost()==null)
            throw new CleverException("Impossibile lanciare il datanode se non c'è un namenode in esecuzione. Lanciare prima il namenode");
        ArrayList<HostManagerNodes> hostWithoutHadoop = this.getHostsWithoutHadoop(this.availableHMList);
        if (hostWithoutHadoop.isEmpty())
            throw new CleverException("Impossibile lanciare il datanode. Non ci sono host disponibili.");
        int minNodesNumber = this.getMinimumRunningNodesNumber(hostWithoutHadoop);
        ArrayList<HostManagerNodes> foundHosts = this.getHostsWithNodes(minNodesNumber, hostWithoutHadoop);
        HostManagerNodes newDatanodeHost = foundHosts.get(0); //esiste sicuramente
        String target = newDatanodeHost.getHM().getNick();
        String agent = HBaseManager.HBASE_LAUNCHER_AGENT_CLASS_NAME;
        String method = "startDatanode";
        //parametri del metodo
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.writeConfigurationXmlString(this.hadoopConfiguration));
        this.owner.remoteInvocation(target, agent, method, false, params); //lancio il datanode
        newDatanodeHost.getRunningNodes().add(HostManagerNodes.DATANODE); //dico che su quell'host ho lanciato il datanode
    }

    @Override
    public void stopDatanode() throws CleverException {
        this.forceCheckpoint(); //aggiorno gli host.
        //Cerco i datanode.
        ArrayList<HostManagerNodes> hadoopNodes = this.getHostsWithHadoop(this.availableHMList);
        if (hadoopNodes.isEmpty())
            throw new CleverException ("Non è in esecuzione nessun datanode");
        ArrayList<HostManagerNodes> datanodes = this.getHostsWithoutHadoopMasters(hadoopNodes);
        if (datanodes.isEmpty())
            throw new CleverException ("Non è in esecuzione nessun datanode");
        //blocco lo stop se è in esecuzione HBase e non è in esecuzione almeno un altro datanode
        if (this.isHBaseRunning()) {
            if (datanodes.size()<=1) {
                //se non c'è un altro datanode
                throw new CleverException ("Impossibile stoppare l'unico datanode in esecuzione se HBase è in esecuzione");
            }
        }
        HostManagerNodes datanodeHost = datanodes.get(0);
        String target = datanodeHost.getHM().getNick();
        String agent = HBaseManager.HBASE_LAUNCHER_AGENT_CLASS_NAME;
        String method = "stopDatanode";
        this.owner.remoteInvocation(target, agent, method, false, new ArrayList());
        //dico che in questo nodo non c'è più il datanode
        for (String n : datanodeHost.getRunningNodes())
            if (n.compareTo(HostManagerNodes.DATANODE)==0) {
                datanodeHost.getRunningNodes().remove(n);
                break;
            }
        //se non è più rispettata la replication lo comunico con una Exception a fine metodo
        if (datanodes.size()<=this.requiredHadoopFilesReplication) { //= perchè datanodes comprende il nodo appena stoppato
            throw new CleverException ("Datanode stoppato correttamente. Tuttavia non è più rispettata ridondanza dei dati richiesta");
        }
    }

    @Override
    public void startZookeeperQuorum() throws CleverException {
        //Leggo dalla proprietà relativa quanti peer zookeeper vanno lanciati
        //e cerco un tale numero di peers zookeeper.
        this.forceCheckpoint(); //aggiorno gli host.
        if (this.isZookeeperRunning() || this.isHBaseRunning()) 
            throw new CleverException ("Impossibile lanciare un quorum zookeeper se sono attivi nodi Zookeeper o HBase");
        if (this.requiredZookeeperPeersNumber<1)
            throw new CleverException ("E' obbligatorio che il quorum Zookeeper richiesto abbia almeno un host. Con più di 3 peer si ha anche tolleranza al crash di alcuni di essi. Ricontrollare la proprietà relativa.");
        ArrayList<HostManagerNodes> zookeeperNodes = new ArrayList<HostManagerNodes>();
        //Cerco tanti nodi quanti sono quelli richiesti dalla proprietà. Li cerco prendendo
        //sempre quelli con il minor numero possibile di nodi già attivi.
        int activeNodes = 0; //inizio con gli host con 0 nodi attivi
        //continuo finchè non ho preso tutti gli host disponibili (nel caso peggiore)
        while (zookeeperNodes.size() < this.availableHMList.size()) {
            ArrayList<HostManagerNodes> hosts = this.getHostsWithNodes(activeNodes, this.availableHMList);
            boolean finished = false;
            if (hosts==null || !hosts.isEmpty()) { //se non sono stati trovati host...
                activeNodes++; //...passo a host con un numero maggiore di nodi attivi
                continue;
            }
            for (HostManagerNodes h : hosts) {
                zookeeperNodes.add(h); //aggiungo l'host
                if (zookeeperNodes.size() >= this.requiredZookeeperPeersNumber) {
                    finished = true; //se ho raggiunto il numero di nodi richiesto
                    break; //dico che ho terminato e esco dal ciclo interno
                }
            }
            if (finished) //se ho raggiunto il numero di nodi richiesto
                break; //termino il ciclo.
            activeNodes++;
        }
        if (zookeeperNodes.size() < this.requiredZookeeperPeersNumber) //se non ho trovato abbastanza hosts
            throw new CleverException ("Numero di host insufficente per il lancio del quorum zookeeper richiesto");
        //Estraggo gli IP dei peer Zookeeper
        this.zookeeperPeersIP.clear();
        for (HostManagerNodes h : zookeeperNodes)
            this.zookeeperPeersIP.add(h.getIP());
        //Creo le configurazioni per Zookeeper
        this.completeZookeeperConfiguration();
        String agent = HBaseManager.HBASE_LAUNCHER_AGENT_CLASS_NAME;
        String method = "startZookeeperPeer";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.writeConfigurationXmlString(this.hbaseConfiguration));
        //lancio i peers Zookeeper
        for (HostManagerNodes h : zookeeperNodes) {
            String target = h.getHM().getNick();
            this.owner.remoteInvocation(target, agent, method, false, params); //lancio il peer
            h.getRunningNodes().add(HostManagerNodes.ZOOKEEPER); //indico che sull'host ho lanciato un peer zookeeper
        }
    }

    @Override
    public void stopZookeeperQuorum() throws CleverException {
        //Controllo se il quorum zookeeper è attivo
        this.forceCheckpoint(); //aggiorno gli host
        if (!this.isZookeeperRunning())
            throw new CleverException ("Non c'è nessun quorum Zookeeper attivo da stoppare.");
        if (this.isHBaseRunning())
            throw new CleverException ("Impossibile stoppare manualmente il cluster Zookeeper se ci sono nodi HBase attivi");
        ArrayList<HostManagerNodes> zookeeperNodes = this.getHostsWithZookeeper(this.availableHMList);
        if (zookeeperNodes==null || zookeeperNodes.isEmpty())
            throw new CleverException ("Non c'è nessun quorum Zookeeper attivo da stoppare.");
        String agent = HBaseManager.HBASE_LAUNCHER_AGENT_CLASS_NAME;
        String method = "stopZookeeperPeer";
        for (HostManagerNodes h : zookeeperNodes) {
            String target = h.getHM().getNick();
            //stoppo il peer zookeeper
            this.owner.remoteInvocation(target, agent, method, false, new ArrayList());
            //indico che sull'host non c'è più il nodo zookeeper.
            for (String node : h.getRunningNodes())
                if (node.compareTo(HostManagerNodes.ZOOKEEPER)==0) {
                    h.getRunningNodes().remove(node);
                    break;
                }
        }
    }

    private boolean isDatanodeRunning() {
        ArrayList<HostManagerNodes> hadoopNodes = this.getHostsWithHadoop(this.availableHMList);
        ArrayList<HostManagerNodes> datanodes = this.getHostsWithoutHadoopMasters(hadoopNodes);
        if (!datanodes.isEmpty())
            return true;
        else
            return false;
    }
    
    @Override
    public void startHBaseMaster() throws CleverException {
        this.forceCheckpoint(); //aggiorno gli host
        //Controllo che siano attivi sia i cluster Hadoop che Zookeeper Di Hadoop deve essere attivo sia il namenode che un datanode
        if (!this.isHadoopRunning())
            throw new CleverException("Impossibile lanciare il master di HBase se non è attivo un cluster Hadoop.");
        if (this.getNamenodeHost()==null)
            throw new CleverException("Impossibile lanciare il master di HBase se non è attivo il namenode e almeno un datanode di Hadoop.");
        if (!this.isDatanodeRunning())
            throw new CleverException("Impossibile lanciare il master di HBase se non è attivo il namenode e almeno un datanode di Hadoop.");
        if (!this.isZookeeperRunning())
            throw new CleverException("Impossibile lanciare il master di HBase se non è attivo il quorum Zookeeper.");
        //Cerco un nodo in cui non giri HBase con il minimo numero di nodi attivi e lancio il master
        ArrayList<HostManagerNodes> availableHosts = this.getHostsWithoutHBase(this.availableHMList);
        if (availableHosts==null || availableHosts.isEmpty())
            throw new CleverException ("Non ci sono host disponibili per il lancio del nodo");
        int minNodesNumber = this.getMinimumRunningNodesNumber(availableHosts);
        ArrayList<HostManagerNodes> possibleHosts = this.getHostsWithNodes(minNodesNumber, availableHosts);
        HostManagerNodes masterHost = possibleHosts.get(0);
        //Se non ci sono nodi di HBase attivi completo le proprietà di configurazione di HBase
        //Do per scontato che il quorum zookeeper sia stato inserito al lancio del sluster Zookeeper
        //perchè se si arriva qui vuol dire che il cluster zookeeper è attivo.
        if (!this.isHBaseRunning())
            this.completeHBaseConfiguration();
        //Lancio il metodo
        String target = masterHost.getHM().getNick();
        String agent = HBaseManager.HBASE_LAUNCHER_AGENT_CLASS_NAME;
        String method = "startHBaseMaster";
        ArrayList<Object> params = new ArrayList <Object>();
        params.add(this.writeConfigurationXmlString(this.hbaseConfiguration));
        this.owner.remoteInvocation(target, agent, method, false, params);
        //indico che su questo host ho lanciato il master.
        masterHost.getRunningNodes().add(HostManagerNodes.HBASE_MASTER);
    }

    @Override
    public void stopHBaseMaster() throws CleverException {
        this.forceCheckpoint(); //aggiorno gli host
        //Controllo se ci sono master di HBase
        ArrayList<HostManagerNodes> masterHosts = this.getHostsWithHBaseMasters(this.availableHMList);
        if (masterHosts.isEmpty())
            throw new CleverException ("Non esiste nessun master di HBase da stoppare");
        //Se è l'unico master posso stopparlo solo se non ci sono regionserver attivi
        if (masterHosts.size()==1) {
            ArrayList<HostManagerNodes> hbaseHosts = this.getHostsWithHBase(this.availableHMList);
            ArrayList<HostManagerNodes> regionservers = this.getHostsWithoutHBaseMasters(hbaseHosts);
            if (!regionservers.isEmpty())
                throw new CleverException ("Impossibile stoppare il master. Tutti i regionserver devono essere spenti per stoppare l'ultimo master");
        }
        //Stoppo il master
        HostManagerNodes masterHost = masterHosts.get(0);
        String target = masterHost.getHM().getNick();
        String agent = HBaseManager.HBASE_LAUNCHER_AGENT_CLASS_NAME;
        String method = "stopHBaseMaster";
        this.owner.remoteInvocation(target, agent, method, false, new ArrayList());
        //indico di aver stoppato il master
        for (String node : masterHost.getRunningNodes())
            if (node.compareTo(HostManagerNodes.HBASE_MASTER)==0) {
                masterHost.getRunningNodes().remove(node);
                break;
            }
        //se ho stoppato l'ultimo master lo comunico tramite un'eccezione lanciata alla fine del metodo, che, quindi, lo lascia completare
        if (masterHosts.size()==1) { //==1 perchè masterHosts comprende quello appena stoppato
            throw new CleverException("Master di HBase stoppato correttamente. Tuttavia era l'ultimo quindi non ci sono più master.");
        }
    }

    @Override
    public void startRegionserver() throws CleverException {
        this.forceCheckpoint(); //aggiorno gli host
        //vedo se sono attivi il cluster zookeeper, almeno un master di HBase, il namenode e almeno un regionserver
        if (!this.isHadoopRunning())
            throw new CleverException("Impossibile lanciare il regionserver se non è attivo un cluster Hadoop.");
        if (this.getNamenodeHost()==null)
            throw new CleverException("Impossibile lanciare il regionserver se non è attivo il namenode e almeno un datanode di Hadoop.");
        if (!this.isDatanodeRunning())
            throw new CleverException("Impossibile lanciare il regionserver se non è attivo il namenode e almeno un datanode di Hadoop.");
        if (!this.isZookeeperRunning())
            throw new CleverException("Impossibile lanciare il regionserver se non è attivo il quorum Zookeeper.");
        if (!this.isHBaseMasterRunning())
            throw new CleverException("Impossibile lanciare il regionserver se non è attivo il master di HBase.");
        //cerco un host in cui non siano stati lanciati nodi hbase con il minor numero possibile di nodi
        ArrayList<HostManagerNodes> hostsWithoutHBase = this.getHostsWithoutHBase(this.availableHMList);
        int minNodesNumber = this.getMinimumRunningNodesNumber(hostsWithoutHBase);
        ArrayList<HostManagerNodes> availableHosts = this.getHostsWithNodes(minNodesNumber, hostsWithoutHBase);
        HostManagerNodes regionserverHost = availableHosts.get(0); //esiste sicuramente
        //lancio il nodo
        String target = regionserverHost.getHM().getNick();
        String agent = HBaseManager.HBASE_LAUNCHER_AGENT_CLASS_NAME;
        String method = "startRegionserver";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.writeConfigurationXmlString(this.hbaseConfiguration));
        this.owner.remoteInvocation(target, agent, method, false, params);
        //indico che sull'host ho lanciato un regionserver
        regionserverHost.getRunningNodes().add(HostManagerNodes.REGIONSERVER);
    }

    @Override
    public void stopRegionserver() throws CleverException {
        this.forceCheckpoint(); //aggiorno gli host
        //cerco un nodo da stoppare
        ArrayList<HostManagerNodes> hbaseHosts = this.getHostsWithHBase(this.availableHMList);
        if (hbaseHosts==null || hbaseHosts.isEmpty())
            throw new CleverException ("Non esistono regionserver da stoppare");
        ArrayList<HostManagerNodes> regionserverHosts = this.getHostsWithoutHBaseMasters(hbaseHosts);
        if (regionserverHosts==null || regionserverHosts.isEmpty())
            throw new CleverException ("Non esistono regionserver da stoppare");
        HostManagerNodes regionserverHost = regionserverHosts.get(0); //esiste sicuramente
        //stoppo il nodo
        String target = regionserverHost.getHM().getNick();
        String agent = HBaseManager.HBASE_LAUNCHER_AGENT_CLASS_NAME;
        String method = "stopRegionserver";
        this.owner.remoteInvocation(target, agent, method, false, new ArrayList());
        //indico che sul nodo non gira più un regionserver
        for (String node : regionserverHost.getRunningNodes())
            if (node.compareTo(HostManagerNodes.REGIONSERVER)==0) {
                regionserverHost.getRunningNodes().remove(node);
                break;
            }
        //se ho stoppato l'ultimo regionserver lo comunico tramite un'eccezione lanciata alla fine del metodo, che, quindi, lo lascia completare
        if (regionserverHosts.size()==1) { //==1 perchè masterHosts comprende quello appena stoppato
            throw new CleverException("Regionserver stoppato correttamente. Tuttavia era l'ultimo quindi non ci sono più regionserver.");
        }
    }

    @Override
    public void formatFS() throws CleverException {
        this.logger.debug("HBASEMANAGER DEBUG: entrato in formatFS(): ");
        this.forceCheckpoint(); //aggiorno gli host.
        //Per il namenode scelgo il primo Host Manager con meno nodi lanciati
        //Vedo il numero minimo di nodi lanciati sugli host manager e scelgo un host
        //manager con quel numero di nodi per lanciare il namenode
        this.logger.debug("HBASEMANAGER DEBUG: formatFS(): fatto checkpoint");
        if (this.isHadoopRunning()) {
            throw new CleverException ("Impossibile formattare l'HadoopFS con il cluster Hadoop già in esecuzione");
        }
        //cerco un host manager con il minimo numero di nodi in esecuzione e
        //in cui non sia in esecuzione un altro nodo di Hadoop.
        ArrayList<HostManagerNodes> hostWithoutHadoop = this.getHostsWithoutHadoop(this.availableHMList);
        if (hostWithoutHadoop.isEmpty())
            throw new CleverException("Impossibile formattare l'HadoopFS. Non ci sono host disponibili per il lancio del namenode.");
        int minNodesNumber = this.getMinimumRunningNodesNumber(hostWithoutHadoop);
        ArrayList<HostManagerNodes> foundHosts = this.getHostsWithNodes(minNodesNumber, hostWithoutHadoop);
        HostManagerNodes newNamenodeHost = foundHosts.get(0); //esiste sicuramente
        this.logger.debug("HBASEMANAGER DEBUG: formatFS(): scelto l'host per il namenode: "+newNamenodeHost.getHM().getNick());
        //vi assegno il namenode (anche per il futuro) e formatto il nuovo HadoopFS
        this.namenodeHost = newNamenodeHost; //prendo l'IP del namenode per costruire le proprietà di configurazione di Hadoop
        this.logger.debug("HBASEMANAGER DEBUG: formatFS(): letto l'ip del namenode"+this.namenodeHost.getIP());
        this.completeHadoopConfiguration(); //completo la configurazione delle proprietà di Hadoop con l'IP del namenode.
        this.logger.debug("HBASEMANAGER DEBUG: formatFS(): completate le configurazioni di Hadoop");
        String target = newNamenodeHost.getHM().getNick();
        String agent = HBaseManager.HBASE_LAUNCHER_AGENT_CLASS_NAME;
        String method = "formatHadoopFS";
        
        //Creo la stringa con le proprietà da passare
        String configurationString = this.writeConfigurationXmlString(this.hadoopConfiguration);
        this.logger.debug("HBASEMANAGER DEBUG: startNamenode(): configurazione:\n"+configurationString);

        ArrayList<Object> launchingParams = new ArrayList<Object>();
        launchingParams.add(configurationString);
        this.logger.debug("HBASEMANAGER DEBUG: startNamenode(): preparata la formattazione e il lancio del namenode");
 
        this.owner.remoteInvocation(target, agent, method, false, launchingParams); //lancio il namenode
        this.logger.debug("HBASEMANAGER DEBUG: startNamenode(): lancio del namenode effettuto");
    }
    
    
}




