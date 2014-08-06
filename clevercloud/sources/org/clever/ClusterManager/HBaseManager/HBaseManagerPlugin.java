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
package org.clever.ClusterManager.HBaseManager;

import org.apache.log4j.Logger;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Exceptions.CleverException;
import org.w3c.dom.Element;

/**
 *
 * @author giovanni
 */
public interface HBaseManagerPlugin {
    public void setOwner (Agent owner);
    public void setLogger (Logger logger); //per scrivere i log
    public void setCheckpointTimeout (int millisecs); //da lanciare solo in inizializzazione. Non da shell
    public void initialize (Element element) throws Exception;
    
    public Integer getRequiredNamenodePort();
    public Boolean isRequiredHadoopAppendFiles();
    public Boolean isRequiredHbaseClusterDistributed();
    public String getRequiredHadoopNamenodeFilesDirectory();
    public String getRequiredHadoopDatanodeFilesDirectory();
    public Integer getRequiredHadoopMaxXcievers();
    public Integer getRequiredHadoopFilesReplication();
    public Integer getRequiredBackupMasterNumber();
    public Integer getRequiredZookeeperPeersNumber();
    public Boolean isRequiredUseSecondaryNamenode();
    public Integer getRequiredNodesPerHostMaxNumber();
    public Boolean isRequiredLaunchOnlyOneMasterPerHost();
    public Boolean isRequiredKeepClustersSepared();
    public Boolean isRequiredLaunchSlavesOnMasterNodes();
    public Float getRequiredCleverOccupationRatio();
    public Float getRequiredHbaseHadoopSlavesRatio();
    public Boolean isRequiredJoinRegionserversAndDatanodes();
    public String getHBasePropertyValue (String name);
    public String getHadoopPropertyValue (String name);
    
    public void setRequiredNamenodePort (Integer requiredNamenodePort) throws CleverException;
    public void setRequiredHadoopAppendFiles(Boolean requiredHadoopAppendFiles) throws CleverException;
    public void setRequiredHbaseClusterDistributed(Boolean requiredHbaseClusterDistributed) throws CleverException;
    public void setRequiredHadoopNamenodeFilesDirectory(String requiredHadoopNamenodeFilesDirectory) throws CleverException;
    public void setRequiredHadoopDatanodeFilesDirectory(String requiredHadoopDatanodeFilesDirectory) throws CleverException;
    public void setRequiredHadoopMaxXcievers(Integer requiredHadoopMaxXcievers) throws CleverException;
    public void setRequiredHadoopFilesReplication(Integer requiredHadoopFilesReplication) throws CleverException;
    public void setRequiredBackupMasterNumber(Integer requiredBackupMasterNumber);
    public void setRequiredZookeeperPeersNumber(Integer requiredZookeeperPeersNumber) throws CleverException;
    public void setRequiredUseSecondaryNamenode(Boolean requiredUseSecondaryNamenode);
    public void setRequiredNodesPerHostMaxNumber(Integer requiredNodesPerHostMaxNumber);
    public void setRequiredLaunchOnlyOneMasterPerHost(Boolean requiredLaunchOnlyOneMasterPerHost);
    public void setRequiredKeepClustersSepared(Boolean requiredKeepClustersSepared);
    public void setRequiredLaunchSlavesOnMasterNodes(Boolean requiredLaunchSlavesOnMasterNodes);
    public void setRequiredCleverOccupationRatio(Float requiredCleverOccupationRatio);
    public void setRequiredHbaseHadoopSlavesRatio(Float requiredHbaseHadoopSlavesRatio);
    public void setRequiredJoinRegionserversAndDatanodes(Boolean requiredJoinRegionserversAndDatanodes);
    public void setHBasePropertyValue (String name, String value) throws CleverException;
    public void setHadoopPropertyValue (String name, String value) throws CleverException;
 
    public String forceCheckpoint() throws CleverException;
    
    public void startNamenode() throws CleverException; //formatta un nuovo HDFS e lancia un nuovo namenode
    public void stopNamenode() throws CleverException; //stoppa il namenode, ma non permette di farlo se sono attivi altri nodi di Hadoop o di HBase.
    public void startDatanode() throws CleverException; //Lancia un nuovo datanode che si connette al namenode attivo
    public void stopDatanode() throws CleverException; //stoppa un datanode
    public void startZookeeperQuorum() throws CleverException; //lancia tutto il quorum zookeeper, nel caso in cui non ci siano nè nodi Zookeeper nè HBase attivi
    public void stopZookeeperQuorum() throws CleverException; //stoppa tutto il quorum zookeeper, ma non permette di farlo se ci son nodi HBase attivi
    public void startHBaseMaster() throws CleverException; //lancia un nuovo master, solo se è attivo il quorum zookeeper, un namenode e almeno un datanode
    public void stopHBaseMaster() throws CleverException; //stoppa il master di HBase se non c'è almeno un altro master presente oppure, se non ce n'è altri, solo se non si sono regionserver attivi
    public void startRegionserver() throws CleverException; //Se è attivo il master, il cluster zookeeper, il namenode e almeno un datanode, lancia un nuovo regionserver
    public void stopRegionserver() throws CleverException; //Se esiste stoppa un regionserver
    public void formatFS() throws CleverException;
}
