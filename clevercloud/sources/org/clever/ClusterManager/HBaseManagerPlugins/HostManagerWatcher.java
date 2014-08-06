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
package org.clever.ClusterManager.HBaseManagerPlugins;

import java.util.ArrayList;
import java.util.TimerTask;
import org.apache.log4j.Logger;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.Shared.HostEntityInfo;

/**
 *
 * @author giovanni
 */
public class HostManagerWatcher extends TimerTask {
    //private int checkpointTimeout; //il periodo di tempo per cui il thread dorme tra un checkpoint e l'altro.
    private Logger logger;
    private CmAgent owner;
    private ArrayList<HostManagerNodes> availableHMList; //la lista degli HM su cui sono lanciati nodi di HBase, Hadoop o Zookeeper
    private HBaseManager plugin;
    private float cleverOccupationRatio;
    
    public HostManagerWatcher (/* int checkpointTimeout,*/ ArrayList<HostManagerNodes> availableHMList, float cleverOccupationRatio, HBaseManager plugin, CmAgent owner, Logger logger) {
        //this.checkpointTimeout = checkpointTimeout;
        this.availableHMList = availableHMList;
        this.cleverOccupationRatio = cleverOccupationRatio;
        this.plugin = plugin;
        this.owner = owner;
        this.logger = logger;
    }
    
    private synchronized void checkpoint() throws CleverException {
        //aggiorno i nodi in esecuzione sugli HostManager attaulmente disponibili
        ArrayList<HostManagerNodes> updatedList = new ArrayList<HostManagerNodes>();
        for (HostManagerNodes h : this.availableHMList) {
            ArrayList<String> activeNodes = (ArrayList<String>) this.owner.remoteInvocation(h.getHM().getNick(), "HBaseLauncherAgent", "getRunningNodes", true, new ArrayList());
            String IP = h.getIP();
            HostEntityInfo info = h.getHM();
            HostManagerNodes updatedHost = new HostManagerNodes();
            updatedHost.setHM(info);
            updatedHost.setIP(IP);
            updatedHost.setRunningNodes(activeNodes);
            updatedList.add(updatedHost);
        }
        this.availableHMList.clear();
        this.availableHMList.addAll(updatedList);
        //ora controllo se ci sono dei nuovi host manager disponibili
        ArrayList<HostEntityInfo> occupants = (ArrayList<HostEntityInfo>) this.owner.invoke("InfoAgent", "listHostManager", true, new ArrayList());
        int currentlyAvailableHMNumber = this.availableHMList.size(); //numero degli HM attualmente occupati da nodi di Hadoop, HBase e Zookeeper
        int totalHMNumber = occupants.size(); //il numero totale degli HMs
        int requiredAvailableHMNumber = (int) (this.cleverOccupationRatio * totalHMNumber); //numero richiesto di HM occupati da nodi del database ricalcolato
        int increment = requiredAvailableHMNumber - currentlyAvailableHMNumber; //numero di HM nuovi su cui lanciare i nodi del database (o da cui toglierli, se negativo)
        //aggiorno i nodi presenti su ogni host e i loro IP
        if (increment < 0) { //se increment è <0 chiedo di togliere nodi del database da una parte degli HMs
            this.plugin.removeAvailableHMs(increment);
        } else if (increment > 0 ) { //se increment è >0 scelgo degli HMs su cui non girano nodi del database e chiedo di utilizzarli per il lancio dei nodi
            ArrayList<HostEntityInfo> newAvailableHMs = new ArrayList<HostEntityInfo>();
            for (int i=0; i<increment; i++) {
                for (HostEntityInfo o : occupants) { //per ognuno degli HM totali della room...
                    boolean trovato = false;
                    for (HostManagerNodes inUse : this.availableHMList) {
                        //se lo trovo nella lista degli HM in uso imposto a true il flag
                        //per indicare che è stato trovato.
                        if (inUse.getHM().getNick().compareTo(o.getNick())==0) {
                            trovato = true;
                            break;
                        }
                    }
                    //se non è stato trovato tra gli HM già in uso lo aggiungo alla lista di
                    //quelli su cui lanciare i nodi del database.
                    if (!trovato)
                        newAvailableHMs.add(o);
                }
            }            
            //chiedo di aggiungere gli HMs alla lista degli HMs su cui lanciare i
            //nodi del database.
            this.plugin.addAvailableHMs(newAvailableHMs);
            
            String logResult = "";
            for (HostManagerNodes h : this.availableHMList) {
                logResult += "\n HM: Nick: "+h.getHM().getNick();
            }
            this.logger.debug("HBASEMANAGER DEBUG Lista degli HM trovati: "+logResult);
        }
        
        //In ogni caso aggiorno la lista dei nodi in esecuzione su ogni host
        for(HostManagerNodes h : this.availableHMList) {
            String target = h.getHM().getNick();
            String agent = HBaseManager.HBASE_LAUNCHER_AGENT_CLASS_NAME;
            String method = "getRunningNodes";
            ArrayList<String> runningNodes = (ArrayList<String>) this.owner.remoteInvocation(target, agent, method, true, new ArrayList());
            if (runningNodes != null)
                h.setRunningNodes(runningNodes);
        }
        this.logger.debug("HBASEMANAGER DEBUG: checkpoint(): Cercati tutti i nodi in esecuzione su ogni host");
    }
    
    public void forceCheckpoint() throws CleverException {
        this.checkpoint();
        //se il checkpoint è stato lanciato dall'utente, o per il lancio da parte dell'utente di
        //qualche nodo, non ridistribuisco i nodi altrimenti si fa confusione perchè è già l'utente
        //che può aver lanciato qualche nodo o lo vuole lanciare.
    }
    
    private void routineCheckpoint() throws CleverException {
        this.checkpoint();
        this.plugin.reDistribuiteNodes();
        //se il checkpoint non è stato lanciato dall'utente, ma è stato fatto quello periodico,
        //allora ridistribuisco i nodi.
    }
    
    @Override
    public void run() {
        try {
            this.routineCheckpoint();
        } catch (CleverException ex) {
            this.logger.warn("Problem during HMs checkpoint: "+ex.getMessage());
        }
    }
    
}
