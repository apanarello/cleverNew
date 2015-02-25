/*
 * The MIT License
 *
 * Copyright 2014 Alfonso Panarello.
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

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.FileReader;
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
    String localDom = null;

    public String[][] startDiscovery() throws CleverException, IOException {

        ArrayList<String> federatedCmsChat = (ArrayList<String>) this.owner.invoke("FederationListenerAgent", "getFederatedCM", true, new ArrayList<Object>());
        //ritorna tutti i domini nel DM compreso il presente
        HashMap<String, String> federatedCmInDb = (HashMap<String, String>) this.owner.invoke("FederationListenerAgent", "getFederatedCMinDB", true, new ArrayList<Object>());
        ArrayList<String> dominiInDB = new ArrayList<String>(federatedCmInDb.keySet());
        BufferedReader b;
        ArrayList<Object> domainsToDelete = new ArrayList<Object>();
        logger.debug("federatedCmInDb size è " + federatedCmInDb.size());
        logger.debug("federatedCms size è " + federatedCmsChat.size());
        logger.debug("PRIMA DEL FOR -----");
        boolean found = false;
        int sizeDB = federatedCmInDb.size();
        localDom = jobRuler.localDomains();
        for (int i = 0; i < dominiInDB.size(); i++) {
            logger.debug("--domini presenti in DB Fed prima del controllo: " + dominiInDB.get(i));
        }
        /*
         * Doppio ciclo innestato seguente verifica che i domini federati presenti nel DB siano al momento della richiesta
         * presenti nella chat. Se cosi non è elimina il dominio dal DB 
         * (se un dominio presente in chat è sicuramente presente nel DB ma non è vero il contrario
         */
        for (int i = 0; i < sizeDB; i++) {
            found = false;
            logger.debug("found vale : " + found);
            //logger.debug("Nel DB Sedna  è presente il cm con nick: " + federatedCmInDb.get(domini.get(i)) + " Appartenente al dominio: " + domini.get(i) + "\n");
            for (int j = 0; j < federatedCmsChat.size(); j++) {
                logger.debug("dentro il for innestato" + "dominio in chat: " + federatedCmsChat.get(j));
                logger.debug("dentro il for innestato" + "dominio: " + federatedCmInDb.get(dominiInDB.get(i)));
                if (federatedCmInDb.get(dominiInDB.get(i)).equals(federatedCmsChat.get(j))) {
                    found = true;
                    logger.debug("dentro if del for innestato");
                    logger.debug("found vale : " + found);
                }

            }
            if (found) {
                logger.debug("dominio : " + federatedCmInDb.get(dominiInDB.get(i)) + " presente si in Db che in Chat");
            }
            if (!found) {

                logger.debug("dominio : " + dominiInDB.get(i) + " sarà rimosso perchè non presente alcun cm appartenete ad esso in chat");
                domainsToDelete.clear();
                domainsToDelete.add(dominiInDB.get(i));
                try {
                    this.owner.invoke("FederationListenerAgent", "deleteDomain", true, domainsToDelete);
                } catch (CleverException ex) {
                    logger.debug("Error invoke", ex);
                }
                dominiInDB.remove(i);
                i = i - 1;
                sizeDB = sizeDB - 1;

            }

        }

        //domini.add(jobRuler.localDomains());
        if (dominiInDB.isEmpty() && (federatedCmsChat.size() != 0)) //si verifica solo se il CM locale è l'unico in chat e mai nessuno ha partecipato alla federazione
        {
            dominiInDB.add(localDom); /////FACCIO IN MODO CHE CI SIA SEMPRE UN DOMINO A CUI LANCIARE IL JOB
        }
        String domResources[][] = new String[dominiInDB.size()][];

        for (int k = 0; k < (dominiInDB.size()); k++) {
            domResources[k] = new String[2];
        }
        for (int i = 0; i < dominiInDB.size(); i++) {
            logger.debug("--domini presenti in Fed dopo del controllo: " + dominiInDB.get(i));
        }

        logger.debug("Nella chat sono presenti  : " + federatedCmsChat.size() + " cms \n");
        logger.debug("Numero domini dopo il controllo: " + dominiInDB.size());
        logger.debug("waiting for retrive num of vms per domain");
        for (int i = 0; i < dominiInDB.size(); i++) {

            fParams.clear();

            commandParams.clear();
            try {
                if (!dominiInDB.isEmpty()) {

                    if (dominiInDB.get(i).equals(localDom)) {
                        commandParams.add(dominiInDB.get(i));
                        logger.debug("Dominio scelto per il getNumHmPerDomain è: " + dominiInDB.get(i));

                        domResources[i][0] = dominiInDB.get(i); //AGGIUNGO IL DOMINIO LOCALE ALLA LISTA DEI DOMINI
                        logger.debug("  - - - Aggiunto all'array il dominio: -------  " + dominiInDB.get(i));
                        String a = String.valueOf(this.owner.invoke("DispatcherAgent", "getHmsInRoom", true, commandParams));
                        domResources[i][1] = a;
                        logger.debug("Lanciato Invoke sul comando getNuHmPerDomain locale ritorna = " + a);

                    } else {
                        commandParams.add(dominiInDB.get(i));
                        //commandParams.add(datTemp.getWeight());

                        fParams.add(dominiInDB.get(i));
                        fParams.add("DispatcherAgent");
                        fParams.add("getHmsInRoom");
                        fParams.add(true); //even if it's false. The reply isn't used in any case
                        fParams.add(commandParams);
                        domResources[i][0] = dominiInDB.get(i);
                        logger.debug("Lancio Invoke sul comando getNuHmPerDomain in federazione");
                        String a = String.valueOf(this.owner.invoke("FederationListenerAgent", "forwardCommandToDomainWithoutTimeout", true, fParams));
                         logger.debug(" Invoke Remoto ritorna la stringa : " + a);
                        /* */
                         
                        ///////////////////////////--------SOLO PER MISURE ACCCROCCHIO SIMULATO------------////////////////
                       /* */
                         if(!dominiInDB.get(i).equals("dominioA")) //il dominio A torna il numero effettivo di nodi
                         try {
                            /* */ b = new BufferedReader(new FileReader("/home/apanarello/"+dominiInDB.get(i)));
                            /* */ a = b.readLine();
                            /* */ logger.debug(" sostituisco con il valore letto da file : " + a);
                        } /* */ catch (IOException ex) {

                            logger.warn("WARNING file numHM NON ESISTE ---PROCEDO CON VALORE DI DEFAULT", ex);
                            domResources[i][1] = a ;
                            logger.debug(" - Il dominio ha " +a+" "+ dominiInDB.get(i) + "\n");
                            return domResources;
                        }

                        logger.debug(" Invoke Remoto sul dominio "+dominiInDB.get(i)+" ritorna la stringa : " + a);

                        domResources[i][1] = a;

                        /*
                         * ///////////////////////////--------SOLO PER MISURE ACCCROCCHIO SIMULATO------------////////////////
                         *
                         *
                         */
                    }
                }
            } catch (CleverException ex) {
                logger.debug("Error during writing array of resources - 1 ", ex);
            }

        }
        return domResources;
    }
}
