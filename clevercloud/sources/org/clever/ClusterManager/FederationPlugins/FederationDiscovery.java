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
package org.clever.ClusterManager.FederationPlugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.clever.ClusterManager.HadoopNamenode.HadoopNamenodeAgent;
import org.clever.ClusterManager.HadoopNamenode.HadoopNamenodePlugin;
import org.clever.ClusterManager.FederationPlugins.FederationListener;
import org.clever.ClusterManager.HadoopPlugin.JobRuler;
import org.clever.Common.Exceptions.CleverException;
//import org.clever.Common.Exceptions.HDFSConnectionException;
//import org.clever.Common.Exceptions.HDFSInternalException;
//import org.clever.Common.StorageRuler.FileInfo;
import org.clever.Common.Timestamp.Timestamper;

/**
 *
 * @author apanarello
 */
public class FederationDiscovery {

    public FederationDiscovery(Logger l, JobRuler j, HadoopNamenodeAgent h) {
        this.logger = l;
        this.jobRuler = j;
        this.owner = h;
    }

    private HadoopNamenodeAgent owner;
    private JobRuler jobRuler;
    String domain = "";
    private Logger logger;
    //
    //ArrayList<String> filePathSmil = new ArrayList<String>();
    ArrayList<Object> federationParams = new ArrayList<Object>();
    ArrayList<Object> fParams = new ArrayList<Object>();
    ArrayList<Object> commandParams = new ArrayList<Object>();

    public String[][] startDiscovery() throws CleverException, IOException {

        ArrayList<String> federatedCmsChat = (ArrayList<String>) this.owner.invoke("FederationListenerAgent", "getFederatedCM", true, new ArrayList<Object>());
        HashMap<String, String> federatedCmInDb = (HashMap<String, String>) this.owner.invoke("FederationListenerAgent", "getFederatedCMinDB", true, new ArrayList<Object>());
        ArrayList<String> domini = new ArrayList<String>(federatedCmInDb.keySet());

        logger.debug("federatedCmInDb size è " + federatedCmInDb.size());
        logger.debug(
                "federatedCms size è " + federatedCmsChat.size());
        logger.debug(
                "PRIMA DEL FOR -----");
        boolean found = false;
        int sizeDB = federatedCmInDb.size();
        for (int i = 0;
                i < domini.size();
                i++) {
            logger.debug("--domini presenti in Fed prima del controllo: " + domini.get(i));
        }
        for (int i = 0; i < sizeDB; i++) {
            found = false;
            logger.debug("found vale : " + found);
            logger.debug("Nel DB Sedna  è presente il cm con nick: " + federatedCmInDb.get(domini.get(i)) + " Appartenente al dominio: " + domini.get(i) + "\n");
            for (int j = 0; j < federatedCmsChat.size(); j++) {
                logger.debug("dentro il for innestato");
                if (federatedCmInDb.get(domini.get(i)).equals(federatedCmsChat.get(j))) {
                    found = true;
                    logger.debug("dentro if del for innestato");
                    logger.debug("found vale : " + found);
                }
            }
            if (found) {
                logger.debug("dominio : " + federatedCmInDb.get(domini.get(i)) + " presente si in Db che in Chat");
            }
            if (!found) {
                logger.debug("dominio : " + domini.get(i) + " sarà rimosso perchè non presente alcun cm appartenete ad esso in chat");
                ArrayList<Object> domainsToDelete = new ArrayList<Object>();
                domainsToDelete.add(domini.get(i));
                try {
                    this.owner.invoke("FederationListenerAgent", "deleteDomain", true, domainsToDelete);
                } catch (CleverException ex) {
                    logger.debug("Error invoke", ex);
                }
                domini.remove(i);
                i = i - 1;
                sizeDB = sizeDB - 1;

            }
        }

        //domini.add(jobRuler.localDomains());
        String domResources[][] = new String[domini.size()][];
        for (int k = 0; k < (domini.size()); k++) {
            domResources[k] = new String[2];
        }
        for (int i = 0; i < domini.size(); i++) {
            logger.debug("--domini presenti in Fed dopo del controllo: " + domini.get(i));
        }

        logger.debug("Nella chat sono presenti  : " + federatedCmsChat.size() + " cms \n");
        logger.debug("Numero domini dopo il controllo: " + domini.size());
        logger.debug("waiting for retrive num of vms per domain");
        for (int i = 0; i < federatedCmsChat.size(); i++) {

            fParams.clear();

            commandParams.clear();
            try {
                if (domini.get(i).equals(jobRuler.localDomains())) {
                    commandParams.add(domini.get(i));
                    logger.debug("Dominio scelto per il getNumHmPerDomain è: "+domini.get(i));

                    domResources[i][0] = domini.get(i); //AGGIUNGO IL DOMINIO LOCALE ALLA LISTA DEI DOMINI
                    logger.debug("  - - - Aggiunto all'array il dominio: -------  "+domini.get(i));
                    String a =String.valueOf(this.owner.invoke("DispatcherAgent", "getHmsInRoom", true, commandParams));
                    domResources[i][1] = a;
                    logger.debug("Lanciato Invoke sul comando getNuHmPerDomain locale ritorna = "+a);

                } else {
                    commandParams.add(domini.get(i));
                    //commandParams.add(datTemp.getWeight());

                    fParams.add(domini.get(i));
                    fParams.add("DispatcherAgent");
                    fParams.add("getHmsInRoom");
                    fParams.add(true); //even if it's false. The reply isn't used in any case
                    fParams.add(commandParams);
                    domResources[i][0] = domini.get(i);
                    logger.debug("Lancio Invoke sul comando getNuHmPerDomain in federazione");
                    String a = String.valueOf(this.owner.invoke("FederationListenerAgent", "forwardCommandToDomainWithoutTimeout", true, fParams));
                    logger.debug(" Invoke Remoto ritorna la stringa : "+a);
                    domResources[i][1] = a;
                    logger.debug(" - " + federatedCmsChat.get(i) + "\n");
                }
            } catch (CleverException ex) {
                logger.debug("Error during writing array of resources - 1 ", ex);
            } catch (IOException ex) {
                logger.debug("Error during writing array of resources - 2 ", ex);
            }

        }
        return domResources;
    }
}
