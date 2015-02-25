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
package org.clever.HostManager.HBaseLauncher;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Exceptions.CleverException;


/**
 *
 * @author giovanni
 */
public interface HBaseLauncherPlugin {
    public void setOwner (Agent owner);
    public void setLogger (Logger logger); //per scrivere i log
    public void setNetworkInterface (String name);
    public void setIsIPv4Preferred (Boolean b);
    public void setisIPv6Used (Boolean b);
    public void setFilesDirectory (String dir);
    
    /**
     * This method stops all Hadoop, Zookeeper and HBase nodes managed by this Host Manager and frees
     * memory. It must not be explicitely invoked by Cluster Manager or Host Managers. In must just
     * be invoked while the HBaseLauncherAgent is shutting down
     * @throws CleverException 
     */
    public void shutdown () throws IOException, CleverException; //termina tutti i nodi lanciati e libera le risorse
    
    //METODI CHIAMATI DAL CLUSTER MANAGER
    
    public ArrayList<String> getRunningNodes();
    
    public void startNamenode (String conf) throws Exception; //lancia il namenode sull'HM senza formattare il filesystem
    public void formatHadoopFS (String conf) throws Exception; //formatta l'HadoopFS.
    public void stopNamenode () throws IOException, CleverException;	
    public void startDatanode (String conf) throws Exception;	
    public void stopDatanode () throws IOException, CleverException;	
    public void startSecondaryNamenode (String conf) throws Exception;	
    public void stopSecondaryNamenode () throws IOException, CleverException;	
    public void startZookeeperPeer (String conf) throws Exception;	
    public void stopZookeeperPeer () throws IOException, CleverException;	
    public void startHBaseMaster (String conf) throws Exception;	
    public void stopHBaseMaster () throws IOException, CleverException;	
    public void startRegionserver (String conf) throws Exception;	
    public void stopRegionserver () throws IOException, CleverException;
    public String getIpAddress ();

}
