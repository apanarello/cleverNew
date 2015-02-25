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
package org.clever.ClusterManager.HadoopPlugin;

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
import org.clever.ClusterManager.FederationPlugins.FederationDiscovery;

import org.clever.Common.Exceptions.CleverException;
//import org.clever.Common.Exceptions.HDFSConnectionException;
//import org.clever.Common.Exceptions.HDFSInternalException;
//import org.clever.Common.StorageRuler.FileInfo;
import org.clever.Common.Timestamp.Timestamper;
import org.clever.Common.XMLTools.ParserXML;
import org.clever.Common.S3tools.S3Tools;

import org.clever.Common.JobRouler.Dataweight;
import org.clever.Common.XMLTools.ControlDim;

/**
 *
 * @author apanarello
 */
public class JobRuler implements Runnable {

    public HashMap<Byte, ArrayList> urlMap = null;
    private HadoopNamenodeAgent owner;
    private HadoopNamenodePlugin ownerPlugin;

    private ClientRuler clientRuler;
    //private String localDomain;
    private boolean flag;
    private boolean local;
    private Logger logger;
    public final String agentName = "HadoopNamenodeAgent";
    private final String JobNode = "Job";
    public Map<String, Integer> mapWeight;
    public final long localResources = 2;
    ArrayList<Object> fedParms;
    long[][] range;
    String fileBuffer, jobName, fileNameS3, bucketName, user, pass;
    long first, last;
    byte position;
    String[][] domRes;
    private S3Tools s3t = null;
    byte part;
    int w;
    static int count = 0;
    ArrayList<Dataweight> domWeights;

    public JobRuler() {

        flag = true;
        mapWeight = null;

    }

    public JobRuler(boolean loc, String fileBuffer, String jobName, String bucketName, String fileNameS3, long start, long end, byte part, HashMap m, int w) {

        this.fileBuffer = fileBuffer;
        this.jobName = jobName;
        this.fileNameS3 = fileNameS3;
        this.bucketName = bucketName;
        //this.user = user;
        //this.pass = pass;
        this.first = start;
        this.last = end;
        this.local = loc;
        this.part = part;
        this.urlMap = m;
        this.w = w;

    }

    /**
     *
     * @param b true = thread locale
     * @param a
     * @param p
     * @param m hashmap di url
     */
    public JobRuler(boolean b, ArrayList a, byte p, HashMap m) {

        this.fedParms = a;
        this.local = b;
        this.part = p;
        this.urlMap = m;
        //this.part=part;
        //this.position = b;
    }

    public void init(HadoopNamenodeAgent owner, ClientRuler clientRuler, Logger logger) {

        this.owner = owner;
        this.ownerPlugin = (HadoopNamenodePlugin) this.owner.getPlugin();
        this.clientRuler = clientRuler;
        this.logger = logger;
        try {
            logger.info("Initializing JobManager");
            ArrayList<Object> params = new ArrayList<Object>();
            //Check if the clients node exists
            params.add(this.agentName);
            params.add("/" + this.JobNode);
            boolean exist;
            exist = ((Boolean) this.owner.invoke("DatabaseManagerAgent", "existNode", true, params)).booleanValue();
            if (exist) {
                logger.info("Job node already exists.");
            } else {
                logger.info("Job node doesn't exist in sednaDB. Creating storage's node");
                params.clear();
                params.add(this.agentName);
                params.add("<" + this.JobNode + " />");
                params.add("into");
                params.add("");
                try {
                    this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
                } catch (CleverException ex) {
                    logger.error("Error inserting Job node in DB");
                }
                logger.info("Job node created.");
            }

            //TEST
            //logger.debug(this.test());
        } catch (CleverException ex) {
            logger.error("Error checking if Storage node exists in DB or creating it");
        }
    }

    public void init(ClientRuler clientRuler, HadoopNamenodeAgent owner, Logger logger) {
        this.owner = owner;
        this.ownerPlugin = (HadoopNamenodePlugin) this.owner.getPlugin();
        this.clientRuler = clientRuler;
        this.logger = logger;
    }

    /**
     *
     * @param fileBuffer Contains the rootkey file for S3 authentication,
     * @param jobName job name to launch
     * @param fileNameS3
     * @param bucketName
     * @param user
     * @param pass
     * @param forwardable Boolean value that does forwardable the send job
     * commands
     * @param domRes
     * @throws org.clever.Common.Exceptions.CleverException
     * @throws java.io.IOException
     */
    public void sendJob(String fileBuffer, String jobName, String bucketName, String fileNameS3, String user, String pass, Boolean forwardable) throws CleverException, IOException {

        //this.start = start;
        for (byte h = 0; h < 25; h++) {  //this.end = end;
            this.logger.debug("SONO NEL SEND JOB ESTERNO");
            try {
                Timestamper.write("T01-inizioEsecuzioneSendJob");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
            if (!this.clientRuler.authenticate(user, pass)) {
                throw new CleverException("Authentication failed with user: " + user);
            }
            this.logger.debug("Autenticazione fatta");
            String domain = "";
            String fedRes[][];
            logger.debug("Initialization of Discovery");
            FederationDiscovery fedDiscovery = new FederationDiscovery(this.logger, this, this.owner);
            logger.debug("waiting for retrive num of vms per domain");
            try {
                Timestamper.write("Time02-inizioDiscovery");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }

            fedRes = fedDiscovery.startDiscovery();

            try {
                Timestamper.write("Time03-FineDiscovery");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
            ArrayList<Object> federationParams = new ArrayList<Object>();
            ArrayList<Object> commandParams = new ArrayList<Object>();

            logger.debug("Vms information retrived");

            forwardable = false;
            String[] login = null;
            domWeights = new ArrayList<Dataweight>();
            s3t = new S3Tools(this.logger);
            s3t.getAuth(fileBuffer);
            this.logger.debug("Autenticazione S3 fatta");
            long size;
            size = s3t.getInfo(fileNameS3, bucketName);
            this.logger.debug("size ricavata file - Chiamo il metodo calcWeights per il calcolo dei frammenti");
            //logger.debug("VERIFICO VALORE array domRES: " + fedRes[0][0] + "con numero VM: " + fedRes[0][1]);
            try {
                Timestamper.write("Time04-InizioCalcoloBlock");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
            try {
                domWeights = calcWeights(fedRes, size);
            } catch (CleverException ex) {
                throw new CleverException(" Nessuno Dominio Federato Presente in chat");
            }
            try {
                Timestamper.write("Time05-FineCalcoloBlock");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
            Dataweight datTemp = null;
            urlMap = new HashMap<Byte, ArrayList>();

            this.logger.debug("Before For to launch thread, thread to launch are: " + domWeights.size());

            for (byte i = 0; i < domWeights.size(); i++) {
                this.logger.debug("CICLO NUMERO: " + i);

                try {
                    datTemp = domWeights.get(i);
                    domain = datTemp.getDomain();// metto in domain il dominio contenuto in domWeights
                    this.logger.debug("Domain on which to launch jop is: " + domain);
                    this.logger.debug("CICLO NUMERO: " + i);
                    boolean t = domain.equals(localDomains());
                    if (t) {
                        this.logger.debug(" domain.equals(localoDomains() is : " + t);

                        try {
                            long firstByte = datTemp.getStart();
                            long lastByte = datTemp.getEnd();

                            this.logger.debug("first =: " + datTemp.getStart() + " last: " + datTemp.getEnd());
                            JobRuler nJobRuler = new JobRuler(t, fileBuffer, jobName, bucketName, fileNameS3, firstByte, lastByte, i, urlMap, datTemp.getWeight());
                            nJobRuler.init(this.clientRuler, this.owner, this.logger);

                            new Thread(nJobRuler).start();
                            this.logger.debug("Thread LANCIATOOOOO");

                            this.local = false;
                        } catch (RuntimeException ex) {
                            this.logger.debug("Error to launch Thread", ex);
                        }
                    } else {
                        try {
                            Timestamper.write("Time06-InizioPreparazioneInoltroaA-" + domain);
                        } catch (IOException ex) {
                            this.logger.warn("can't write timestamp log: " + ex.getMessage());
                        }
                        logger.info("Domain  choosen to send job is: " + domain);
                        login = this.ownerPlugin.retrieveLoginInfo(domain);
                        logger.info("Domain authentication info retrieved: user=" + login[0] + " pass=" + login[1]);
                        logger.debug("Domain : " + domain + "has to transcode the following byte range : from " + datTemp.getStart() + "to" + datTemp.getEnd());

                        federationParams.clear();
                        commandParams.clear();
                        commandParams.add(fileBuffer);
                        commandParams.add(jobName);
                        commandParams.add(bucketName);
                        commandParams.add(fileNameS3);
                        //commandParams.add(login[0]); //user
                        //commandParams.add(login[1]); //userPass
                        commandParams.add(datTemp.getStart());
                        commandParams.add(datTemp.getEnd());
                        commandParams.add(i);
                        commandParams.add(datTemp.getWeight());

                        federationParams.add(domain);
                        federationParams.add(this.agentName);
                        federationParams.add("sendJob");
                        federationParams.add(true); //even if it's false. The reply isn't used in any case
                        federationParams.add(commandParams);
                        //part++;
                        //JobRuler job = new JobRuler(federationParams);
                        this.logger.debug("Try to launch thread to send command to federated domain: " + domain);
                      // JobRuler.LittleJR lJobRuler = this.new LittleJR(t, federationParams, i, urlMap,this.clientRuler, this.owner, this.logger);
                        //new Thread(lJobRuler).start();
                        JobRuler nJobRuler = new JobRuler(t, federationParams, i, urlMap);
                        nJobRuler.init(this.clientRuler, this.owner, this.logger);
                        new Thread(nJobRuler).start();
                        
                        
                       
                        
                        
                    }

                } catch (CleverException ex) {
                    logger.error("Error to start thread job", ex);
                } catch (Exception ex) {
                    logger.error("Error to start thread job", ex);
                }

            }

            ControlDim cDim = new ControlDim(this.logger, domWeights.size(), urlMap, bucketName, fileNameS3, s3t);
            cDim.start();
            synchronized (cDim) {
                try {
                    logger.error("Error to start thread job");
                    cDim.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }
    }
/*
    class LittleJR implements Runnable {

        private final ArrayList fedParms;
        private final boolean local;
        private final byte part;
        private final HashMap urlMap;
        private final HadoopNamenodeAgent owner;
        private final HadoopNamenodePlugin ownerPlugin;
        private final ClientRuler clientRuler;
        private final Logger logger;

        public LittleJR(boolean b, ArrayList a, byte p, HashMap m, ClientRuler clientRuler, HadoopNamenodeAgent owner, Logger logger) {

            this.owner = owner;
            this.ownerPlugin = (HadoopNamenodePlugin) this.owner.getPlugin();
            this.clientRuler = clientRuler;
            this.logger = logger;
            this.fedParms = a;
            this.local = b;
            this.part = p;
            this.urlMap = m;
        }

        @Override
        public void run() {
            logger.debug("ESEGUO IL RUN: " + this.toString());

            if (!local) {
                logger.debug("ESEGUO IL RUN remoto: " + this.toString());

                this.logger.debug("Launching the command to choosen domain...");
                Object reply = null;
                ArrayList a = (ArrayList) fedParms.get(4);
                Byte n = (Byte) a.get(6);
                w = (Integer) a.get(7);

                String dom = fedParms.get(0).toString();
                this.logger.debug("Sono nel thread per il dominio: " + dom);
                try {
                    Timestamper.write("Time07-FinePreparazioneInoltroaA-" + dom);
                } catch (IOException ex) {
                    this.logger.warn("can't write timestamp log: " + ex.getMessage());
                }
                this.logger.debug("Sto per lanciare L'invoke con i seguenti parametri: " + fedParms.get(0).toString() + ";" + fedParms.get(1).toString() + ";" + fedParms.get(2).toString() + ";" + fedParms.get(3).toString());
                try {
                 Timestamper.write("Time08-InizioInoltroaA-" + dom);
                 } catch (IOException ex) {
                 this.logger.warn("can't write timestamp log: " + ex.getMessage());
                 }
                try {
                    reply = this.owner.invoke("FederationListenerAgent", "forwardCommandToDomainWithoutTimeout", true, fedParms);
                    //                reply = this.owner.invoke("FederationListenerAgent", "forwardCommandToDomainWithoutTimeout", true, fedParms);
                    this.logger.debug("Response of INVOKE IS: first element in arraylist is " + ((ArrayList) reply).get(0) + "---VALUE----: ");
                    this.logger.info("Command launched. Reply: " + reply);

                    try {
                        this.logger.debug("Sto per scrivere il log del dominio: " + dom);
                        Timestamper.write("Time18-riuscitoLancioSendJobSuDominioFederato - " + dom);
                    } catch (IOException ex) {
                        this.logger.warn("can't write timestamp log: " + ex.getMessage());
                    }
                    if (reply != null && reply instanceof Exception) {
                        throw new CleverException((Exception) reply);
                    }
                } catch (CleverException ex) {

                    this.logger.error("Exception caught while forwarding sendJob method on " + fedParms.get(0) + " domain: " + ex);

                }
                //count++;
                try {
                    urlMap.put(n.byteValue(), (ArrayList) reply);

                    // urlMap.put(n.byteValue(),"https://s3.amazonaws.com/"+a.get(2)+"/"+a.get(3)+"-part-"+n.toString());
                } catch (Exception e) {
                    logger.error("error in urlMap put", e);
                }
                logger.debug("Aggiunto Url all'hash table: " + "chiave: " + n.byteValue() + " - Valore:  " + urlMap.get(n.byteValue()));
                this.logger.debug("INVOCATO FORWARD");
                try {
                    Timestamper.write("T18-riuscitoLancioSendJobSuDominioFederato - " + fedParms.get(0));
                } catch (IOException ex) {
                    this.logger.warn("can't write timestamp log: " + ex.getMessage());
                }

                this.logger.debug("Command successfully launched on domain " + fedParms.get(0));
             //break;

                //listDomains.remove(domain);
            } //this.sendJob(fileBuffer, jobName, fileNameS3, bucketName, user, pass, start, end);
            else {
                logger.debug("ESEGUO IL RUN locale: " + this.toString());

                logger.debug("job ruler lanciato jon in locale...:");
            //String url="";
                //ArrayList<Object> para = new ArrayList<Object>();

                this.logger.debug("Lancio del metodo : " + this.getClass().getName());
            //url=this.ownerPlugin.submitJob(fileBuffer, jobName, bucketName, fileNameS3, first, last, part);
                //urlMap=new HashMap<Byte, String>();
                try {
                    urlMap.put(this.part, this.ownerPlugin.submitJob(fileBuffer, jobName, bucketName, fileNameS3, first, last, part, w));

                    logger.debug("Aggiunto Url all'hash table: " + "chiave: " + this.part + " - Valore: " + urlMap.get(this.part));
                    //count++;
                } catch (CleverException ex) {
                    logger.error("Error to lauch job in local domain", ex);
                    //this.logger.warn("Exception caught while forwarding sendJob method on " + fedParms.get(0) + " domain: " + ex);

                }

            }

        }

    }
*/
    /**
     *
     * @param fileBuffer
     * @param jobName
     * @param fileS3
     * @param bucket
     * @param starByte
     * @param endByte
     * @param p
     * @return url
     * @throws CleverException
     * @throws IOException
     */
    public ArrayList sendJob(String fileBuffer, String jobName, String bucket, String fileS3, Long starByte, Long endByte, Byte p, Integer w) throws CleverException, IOException {
        logger.debug("Local federated sendJob method: " + this.toString());
        return this.ownerPlugin.submitJob(fileBuffer, jobName, bucket, fileS3, starByte, endByte, p, w);
        //this.execJob(fileBuffer, jobName, fileS3, bucket, starByte, endByte, p);

    }

    @Override
    public void run() {
        logger.debug("ESEGUO IL RUN: " + this.toString());

        if (!local) {
            logger.debug("ESEGUO IL RUN remoto: " + this.toString());

            this.logger.debug("Launching the command to choosen domain...");
            Object reply = null;
            ArrayList a = (ArrayList) fedParms.get(4);
            Byte n = (Byte) a.get(6);
            w = (Integer) a.get(7);

            String dom = fedParms.get(0).toString();
            this.logger.debug("Sono nel thread per il dominio: " + dom);
            try {
                Timestamper.write("Time07-FinePreparazioneInoltroaA-" + dom);
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
            this.logger.debug("Sto per lanciare L'invoke con i seguenti parametri: " + fedParms.get(0).toString() + ";" + fedParms.get(1).toString() + ";" + fedParms.get(2).toString() + ";" + fedParms.get(3).toString());
            /* try {
             Timestamper.write("Time08-InizioInoltroaA-" + dom);
             } catch (IOException ex) {
             this.logger.warn("can't write timestamp log: " + ex.getMessage());
             }*/
            try {
                reply = this.owner.invoke("FederationListenerAgent", "forwardCommandToDomainWithoutTimeout", true, fedParms);
                //                reply = this.owner.invoke("FederationListenerAgent", "forwardCommandToDomainWithoutTimeout", true, fedParms);
                this.logger.debug("Response of INVOKE IS: first element in arraylist is " + ((ArrayList) reply).get(0) + "---VALUE----: ");
                this.logger.info("Command launched. Reply: " + reply);

                try {
                    this.logger.debug("Sto per scrivere il log del dominio: " + dom);
                    Timestamper.write("Time18-riuscitoLancioSendJobSuDominioFederato - " + dom);
                } catch (IOException ex) {
                    this.logger.warn("can't write timestamp log: " + ex.getMessage());
                }
                if (reply != null && reply instanceof Exception) {
                    throw new CleverException((Exception) reply);
                }
            } catch (CleverException ex) {

                this.logger.error("Exception caught while forwarding sendJob method on " + fedParms.get(0) + " domain: " + ex);

            }
            //count++;
            try {
                urlMap.put(n.byteValue(), (ArrayList) reply);

                // urlMap.put(n.byteValue(),"https://s3.amazonaws.com/"+a.get(2)+"/"+a.get(3)+"-part-"+n.toString());
            } catch (Exception e) {
                logger.error("error in urlMap put", e);
            }
            logger.debug("Aggiunto Url all'hash table: " + "chiave: " + n.byteValue() + " - Valore:  " + urlMap.get(n.byteValue()));
            this.logger.debug("INVOCATO FORWARD");
            try {
                Timestamper.write("T18-riuscitoLancioSendJobSuDominioFederato - " + fedParms.get(0));
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }

            this.logger.debug("Command successfully launched on domain " + fedParms.get(0));
             //break;

            //listDomains.remove(domain);
        } //this.sendJob(fileBuffer, jobName, fileNameS3, bucketName, user, pass, start, end);
        else {
            logger.debug("ESEGUO IL RUN locale: " + this.toString());

            logger.debug("job ruler lanciato jon in locale...:");
            //String url="";
            //ArrayList<Object> para = new ArrayList<Object>();

            this.logger.debug("Lancio del metodo : " + this.getClass().getName());
            //url=this.ownerPlugin.submitJob(fileBuffer, jobName, bucketName, fileNameS3, first, last, part);
            //urlMap=new HashMap<Byte, String>();
            try {
                urlMap.put(this.part, this.ownerPlugin.submitJob(fileBuffer, jobName, bucketName, fileNameS3, first, last, part, w));

                logger.debug("Aggiunto Url all'hash table: " + "chiave: " + this.part + " - Valore: " + urlMap.get(this.part));
                //count++;
            } catch (CleverException ex) {
                logger.error("Error to lauch job in local domain", ex);
                //this.logger.warn("Exception caught while forwarding sendJob method on " + fedParms.get(0) + " domain: " + ex);

            }

        }

    }

    public String localDomains() throws IOException {
        //private String localDomain;
        String localDom;
        logger.info("Read Configuration to retrieve Local Domain name");
        InputStream inxml = getClass().getResourceAsStream("./cfg/configuration_federation.xml");
        File cfgFile = new File("./cfg/configuration_federation.xml");
        logger.debug("Creato oggettoFILE per il file di configurazione Federation.xml");
        //FileStreamer fs = new FileStreamer();
        ParserXML pars = new ParserXML(cfgFile);
        localDom = pars.getElementContent("domain");
        logger.debug("il dominio letto è: " + localDom);
        return localDom;

    }

    /**
     *
     * @param domR array [dom][resources]
     * @param local we are ipotizing that the local domain has tw0 available vm
     * @param size size file on S3amazon
     * @return
     * @throws java.io.IOException
     */
    public ArrayList<Dataweight> calcWeights(String domR[][], long size) throws IOException, CleverException {

        ArrayList<Dataweight> tempArray = new ArrayList<Dataweight>();
        ArrayList<Dataweight> tempArray2 = new ArrayList<Dataweight>();
        Dataweight dataWeight = null;
        String localdomain;
        //long locWeight;
        logger.debug("Calculate num chunks for each domain");
        //locWeight = local;
        int c;
        Long sum = 0L;
        Dataweight obTemp, obTempPrev;
        logger.debug("Creati Oggetti obTemp ,obTempPrev");
        obTempPrev = null;
        try {

            logger.debug("Creato Iterator per TempARRAy");
            logger.debug("domR[][] size= " + domR.length);
            //int numDom = domRes.size();
            localdomain = localDomains();
            // logger.debug("VALORI da aggiungere all'array sono: " + localdomain + " con peso " + locWeight);
            // tempArray.add(new Dataweight(localdomain, (int) (locWeight)));
            //logger.debug("Aggiunto alla lista : " + tempArray.get(0).getDomain() + " con valore: " + tempArray.get(0).getWeight());
            try {
                for (int j = 0; j < domR.length; j++) {
                    tempArray.add(new Dataweight(domR[j][0], Integer.parseInt(domR[j][1])));

                    logger.debug("Ciclo " + j + " --Aggiunto alla lista :  " + tempArray.get(j).getDomain() + " con valore: " + tempArray.get(j).getWeight());
                }
                logger.debug("PRIMA DEL FOR");
                //it = tempArray.iterator();
            } catch (Exception e) {
                logger.error("Error in calcWeights", e);
            }

            ///////////////////////////////////////////////////////
            ////SOLO PER MISURE SI ESCLUDE IL DOMINIO LOCALE///////
            ///////////////////////////////////////////////////////
            int d = tempArray.size(); ////elimino il dominio locale 
            for (c = 0; c < d; c++) {

                if ((tempArray.get(c).getDomain()).equals(localdomain)) {
                    logger.debug("elimino il dominio locale " + tempArray.get(c).getDomain() + " dal calcolo: ");
                    tempArray.remove(c);
                    d = d - 1;
                    c = c - 1;

                }

            }

            ///////////////////////////////////////////////////////
            ////SOLO PER MISURE SI ESCLUDE IL DOMINIO LOCALE///////
            ///////////////////////////////////////////////////////
            for (c = 0; c < tempArray.size(); c++) {

                //logger.debug("DENTRO WHILE");
                dataWeight = tempArray.get(c);

                sum += (long) dataWeight.getWeight(); //ottengo il numero di chunck totali
                logger.debug("it is summing the numeber of resources of each domains: " + sum + "the currently domain is: " + dataWeight.getDomain());
                //j++;

            }

            ////versione con blocchi di guali di dim per dominio
           /*
             long block;
             block = size / tempArray.size();

             for (c = 0; c < tempArray.size(); c++) {
             obTemp = tempArray.get(c);

             logger.debug("calcolo il range per il dominio: " + obTemp.getDomain());
             obTemp.setStart(block * c);
             obTemp.setEnd(block * (c + 1));
             tempArray2.add(obTemp);
             //range[0][1] = (Long) (local * chunck);
             //weight.put("localdomain", range);
             logger.debug("added domain: " + obTemp.getDomain() + " and range :" + obTemp.getStart() + " - " + obTemp.getEnd() + " to ArrayList");

             }
             */
            long chunck;
            if (sum != 0) {

                chunck = size / (sum);
                logger.debug("Il chunck ha dimensione: " + chunck);

                for (c = 0; c < tempArray.size(); c++) {

                    //logger.debug("DENTRO WHILE");
                    obTemp = tempArray.get(c);
                    //obTemp = it.next();
                    //obTempPrev = (Dataweight) obTemp.clone();
                    //logger.debug("Effettuata copia oggetti");
                    if (c == 0) {
                        logger.debug("calcolo il range per il dominio: " + obTemp.getDomain());
                        obTemp.setStart(0);
                        obTemp.setEnd((chunck * (obTemp.getWeight())));
                        tempArray2.add(obTemp);
                        //range[0][1] = (Long) (local * chunck);
                        //weight.put("localdomain", range);
                        logger.debug("added domain: " + obTemp.getDomain() + " and range :" + obTemp.getStart() + " - " + obTemp.getEnd() + " to ArrayList");

                    } else {
                        obTemp.setStart(tempArray.get(c - 1).getEnd());
                        obTemp.setEnd(tempArray.get(c - 1).getEnd() + (chunck * obTemp.getWeight()));
                        //range[0][0] = range[0][1];
                        //range[0][1] = (chunck * ((Long) domRes.get(dom)));
                        tempArray2.add(obTemp);
                        this.logger.debug("added domain: " + obTemp.getDomain() + " and range :" + obTemp.getStart() + " - " + obTemp.getEnd() + " to ArrayList");

                    }
                    this.logger.debug("It is filling range Array");

                }
                this.logger.debug("Finish Calculate chunck per domains");
            } else {
                throw new CleverException("Nessun Dominio a cui lanciare il job");

            }

//        
        } catch (NullPointerException ex) {
            this.logger.error("Error to calculate weight in :/* */" + this.getClass().getName(), ex);
        }
        return tempArray2;
    }

    /**
     * Buffer
     */
    //public void execJob(String fileBuffer, String jobName, String fileS3, String bucket, long starByte, long endByte, byte p) throws IOException {
        /*
     S3Tools s3 = new S3Tools(this.logger);
     String dest = "/home/apanarello/" + fileS3 + "part-" + p;
     //Process pro=null;
     try {
     //s3.getAuth(fileBuffer);
     s3.getFileFromS3(fileBuffer, dest, bucket, fileS3, (int) starByte, (int) endByte);
     } catch (NoSuchAlgorithmException ex) {
     this.logger.error("Error to retrive S3 file from Amazon", ex);
     } catch (NoSuchProviderException ex) {
     this.logger.error("Error to retrive S3 file from Amazon", ex);
     } catch (NoSuchPaddingException ex) {
     this.logger.error("Error to retrive S3 file from Amazon", ex);
     } catch (InvalidKeySpecException ex) {
     this.logger.error("Error to retrive S3 file from Amazon", ex);
     } catch (InterruptedException ex) {
     this.logger.error("Error to retrive S3 file from Amazon", ex);
     }
     logger.debug("SONO NEL DOMINIO: " + this.localDomains() + "METODO" + this.getClass().getMethods().toString());
     try {
            
     Runtime.getRuntime().exec("hadoop fs -put " + dest + " -acodec copy -vcodec mpeg4" + " /home/dissennato/output-" + p);

     //DOVREBBE ESSERE UN JOB DI HADOOP A FARE LA TRANSCODIFICA
     Runtime.getRuntime().exec("ffmpeg -i " + dest + " -acodec copy -vcodec mpeg4" + " /home/dissennato/output-" + p);
     } catch (RuntimeException ex) {
     logger.error("Error to exec ffmpeg transcode");
     }
     s3.uploadFile(fileBuffer, dest, bucket, fileS3);
     */
    //     logger.debug("SONO NEL DOMINIO: " + this.localDomains() + "METODO" + this.getClass().getMethods().toString());
    // }
}
