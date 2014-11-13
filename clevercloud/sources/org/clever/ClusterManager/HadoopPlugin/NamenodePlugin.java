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
package org.clever.ClusterManager.HadoopPlugin;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.log4j.Logger;
import org.clever.ClusterManager.HadoopNamenode.HadoopNamenodePlugin;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Communicator.ModuleCommunicator;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMLTools.ParserXML;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.logging.Level;
import javax.crypto.NoSuchPaddingException;
import org.apache.commons.io.IOUtils;
//import org.clever.ClusterManager.HadoopJobtracker.HadoopJobtrackerAgent;
import org.clever.ClusterManager.HadoopNamenode.HadoopNamenodeAgent;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Exceptions.HDFSConnectionException;
import org.clever.Common.Exceptions.HDFSInternalException;
import org.clever.Common.JobRouler.Dataweight;
import org.clever.Common.S3tools.S3Tools;
import org.clever.Common.Shared.Support;
import org.clever.Common.StorageRuler.Client;
import org.clever.Common.StorageRuler.FileInfo;
import org.clever.Common.Timestamp.Timestamper;
import org.clever.Common.XMLTools.FileStreamer;

/**
 * @author Giovanni Volpintesta
 * @author Mariacristina Sinagra
 */
public class NamenodePlugin implements HadoopNamenodePlugin {

    private final Logger logger;
    private Class cl;
    private ModuleCommunicator mc;
    private String hostName;
    private ParserXML pXML;
    private Agent owner;
    private final String nodoHadoop = "Hadoop";
    private String coreHadoop;
    private String hdfsHadoop;
    private String mapredHadoop;
    private String slave;
    private ArrayList urlList;

    public final static String storageNode = "HadoopStorage";
    public final static String domainNode = "domain";
    public final static String agentName = "HadoopNamenodeAgent";
    public final static String nameAttribute = "name";
    public final static String fileNode = "file";
    public final static String containerDomainNode = "containerDomain";
    public final static String UIDNode = "UID";
    public final static String localDomain = "LOCAL";
    public final String StorageDirectoryInHDFS = "CleverStorage";
    public final String authenticationNode = "Authentication";

    ClientRuler clientRuler;
    StorageRuler storageRuler;
    JobRuler jobRuler;

    /**
     * Instantiates a new NamenodePlugin object
     */
    public NamenodePlugin() {
        this.logger = Logger.getLogger("HadoopNamenodePlugin");
        this.storageRuler = new StorageRuler(); //va inizializzato dopo 
        this.clientRuler = new ClientRuler(); //va inizializzato dopo 
        this.jobRuler = new JobRuler();
        try {
            logger.info("Read Configuration HadoopNamenodeAgent!");
            InputStream inxml = getClass().getResourceAsStream("/org/clever/ClusterManager/HadoopNamenode/configuration_HadoopNamenode.xml");
            FileStreamer fs = new FileStreamer();
            ParserXML pars = new ParserXML(fs.xmlToString(inxml));
            this.coreHadoop = pars.getElementContent("coreSite");
            this.hdfsHadoop = pars.getElementContent("hdfsSite");
            this.mapredHadoop = pars.getElementContent("mapredSite");
            this.slave = pars.getElementContent("slaves");
            //retrieve hostname		
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                this.logger.error("Error getting local host name :" + e.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            this.logger.error("Error reading configuration");
        }
    }

    @Override
    public void setModuleCommunicator(ModuleCommunicator m) {
        this.mc = m;
    }

    @Override
    public ModuleCommunicator getModuleCommunicator() {
        return this.mc;
    }

    @Override
    public void init() {
        try {
            this.initAuthenticatoinNode();
        } catch (CleverException ex) {
            this.logger.error("Error initializing authentication node: " + ex.getMessage());
        }
        this.clientRuler.init((CmAgent) this.owner, this.logger);
        this.storageRuler.init((HadoopNamenodeAgent) this.owner, this.clientRuler, this.logger);
        this.jobRuler.init((HadoopNamenodeAgent) this.owner, this.clientRuler, this.logger);
    }

    //retrieve ip address
    @Override
    public String networkIp() {
        Enumeration<NetworkInterface> ifaces = null;
        try {
            ifaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        for (; ifaces.hasMoreElements();) {
            NetworkInterface iface = ifaces.nextElement();
            //System.out.println(iface.getName() + ":");

            for (Enumeration<InetAddress> addresses = iface.getInetAddresses(); addresses.hasMoreElements();) {
                InetAddress address = addresses.nextElement();
                String str = address.toString().substring(1);
                if (address.isSiteLocalAddress() && !address.isLoopbackAddress() && !(address.getHostAddress().indexOf(":") > -1)) {
                    return str;
                }
            }
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            return "ip error";
        }
    }

    //edit /etc/hosts file
    /**
     *
     * @param address ip_address of the host
     * @param name hostname of the host
     * @throws Exception
     */
    @Override
    public void setHosts(String address, String name) throws Exception {

        BufferedReader in = new BufferedReader(new FileReader("/etc/hosts"));
        String line;
        StringBuffer buffer = new StringBuffer();
        while ((line = in.readLine()) != null) {
            buffer.append(line + "\n");
        }
        in.close();
        BufferedWriter b;
        b = new BufferedWriter(new FileWriter("/etc/hosts"));

        b.write(buffer.toString());
        //insert (ip_address hostname) at the end of file
        b.write(address + "\t" + name);

        b.close();
    }

    //edit /conf/slaves file
    /**
     *
     * @param username username of the host added
     * @param name hostname of the host added
     * @throws Exception
     */
    @Override
    public void setSlaves(String username, String name) throws Exception {

        BufferedReader in = new BufferedReader(new FileReader(slave));
        String line;
        StringBuffer buffer = new StringBuffer();
        while ((line = in.readLine()) != null) {
            buffer.append(line + "\n");
        }
        in.close();
        BufferedWriter b;
        b = new BufferedWriter(new FileWriter(slave));

        b.write(buffer.toString());

        b.write(username + "@" + name);

        b.close();
    }

    //check if exists hostname in the /etc/hosts file
    /**
     *
     * @param address ip_address of the host
     * @param name hostname of the host
     * @return
     * @throws Exception
     */
    @Override
    public boolean existsHost(String address, String name) throws Exception {
        boolean exists = false;

        BufferedReader in = new BufferedReader(new FileReader("/etc/hosts"));
        String line;
        StringBuffer buffer = new StringBuffer();

        while (((line = in.readLine()) != null) && !exists) {
            System.out.println(line);
            if (line.equals(address + " " + name)) {
                exists = true;
            }
        }

        in.close();

        return exists;
    }

    /**
     *
     * @param address ip_address of the host
     * @param name hostname of the host
     * @throws Exception
     */
    @Override
    public void updateHosts(String address, String name) throws Exception {
        BufferedReader in = new BufferedReader(new FileReader("/etc/hosts"));
        String line;
        StringBuffer buffer = new StringBuffer();
        while ((line = in.readLine()) != null) {
            if (!(line.equals(address + " " + name))) {
                buffer.append(line + "\n");
            }
        }
        in.close();
        BufferedWriter b;
        b = new BufferedWriter(new FileWriter("/etc/hosts"));

        b.write(buffer.toString());

        b.close();
    }

    @Override
    public boolean checkHadoopAgent() throws CleverException {
        List params = new ArrayList();
        params.add("HadoopNamenodeAgent");
        params.add("/" + this.nodoHadoop);
        return (Boolean) this.owner.invoke("DatabaseManagerAgent", "checkAgentNode", true, params);
    }

    @Override
    public void initHadoopAgent() throws CleverException {
        String node = "<" + this.nodoHadoop + "/>";
        List params = new ArrayList();
        params.add("HadoopNamenodeAgent");
        params.add(node);
        params.add("into");
        params.add("");
        this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
    }

    /**
     *
     * @param hostname hostname of the hadoop host
     * @param address ip_address of the hadoop host
     * @param username username of the hadoop host
     * @throws CleverException
     */
    @Override
    public void InsertItemIntoHadoopNode(String hostname, String address, String username) throws CleverException {
        String node = "<Node name=\"" + hostname + "\" request=\"" + new Date().toString() + "\"" + ">"
                + "<ip>" + address + "</ip>"
                + "<host>" + hostname + "</host>"
                + "<user>" + username + "</user>"
                + "</Node>";
        List params = new ArrayList();
        params.add("HadoopNamenodeAgent");
        params.add(node);
        params.add("into");
        params.add("/" + this.nodoHadoop);
        this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
    }

    /*
     @Override    
     public void InsertItemIntoHadoopNamespace(String hostname, String namespace) throws CleverException {
     String node="<Namespace name=\""+hostname+"\" request=\""+new Date().toString()+"\""+">"
     +"<id>"+namespace+"</id>"
     +"</Namespace>";
     List params = new ArrayList();
     params.add("HadoopNamenodeAgent");
     params.add(node);
     params.add("into");
     params.add("/"+this.nodoHadoop);
     this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
     }
     */
    @Override
    public void setOwner(Agent owner) {
        this.owner = owner;
    }

    private void initAuthenticatoinNode() throws CleverException {
        ArrayList<Object> params = new ArrayList<Object>();
        //Check if the clients node exists
        params.add(this.agentName);
        params.add("/" + this.authenticationNode);
        boolean exist;
        exist = ((Boolean) this.owner.invoke("DatabaseManagerAgent", "existNode", true, params)).booleanValue();
        if (exist) {
            logger.info("Authentication node already exists.");
        } else {
            logger.info("Authentication node doesn't exist in sednaDB. Creating authentication's node");
            params.clear();
            params.add(this.agentName);
            params.add("<" + this.authenticationNode + " />");
            params.add("into");
            params.add("");
            try {
                this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
            } catch (CleverException ex) {
                logger.error("Error inserting Authentication node in DB");
            }
            logger.info("Authentication node created.");
        }
    }

    private HashMap<String, String[]> getDomainLogins() throws CleverException {
        String xml;
        String location = "/" + this.authenticationNode;
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.agentName);
        params.add(location);
        xml = (String) this.owner.invoke("DatabaseManagerAgent", "getContentNodeObject", true, params);
        xml = SednaUtil.decodeSednaXml(xml);
        xml = "<" + this.authenticationNode + ">\n" + xml + "\n</" + this.authenticationNode + ">";
        //Spit the xml to retrieve all of the domains
        HashMap<String, String[]> result = new HashMap<String, String[]>();
        String s = xml;
        while (true) {
            String[] p = s.split("<domain", 2);
            if (p.length < 2) {
                break;
            }
            s = p[1];
            p = s.split("</domain>", 2);
            s = p[1]; //ready forn next cicle
            String domainXML = p[0];
            p = domainXML.split("name", 2);
            domainXML = p[1];
            p = domainXML.split("\"", 3);
            String domain = p[1];
            domainXML = p[2];
            p = domainXML.split("<user>", 2);
            domainXML = p[1];
            p = domainXML.split("</user>", 2);
            String user = p[0];
            domainXML = p[1];
            p = domainXML.split("<password>", 2);
            domainXML = p[1];
            p = domainXML.split("</password>", 2);
            String password = p[0];
            String[] auth = {user, password};
            result.put(domain, auth);
        }
        return result;
    }

    private void insertLoginInDB(String domain, String user, String password) throws CleverException {
        String node = "<domain name=\"" + domain + "\">"
                + "<user>" + user + "</user>"
                + "<password>" + password + "</password>"
                + "</domain>";
        String location = "/" + this.authenticationNode;
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.agentName);
        params.add(node);
        params.add("into");
        params.add(location);
        this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
    }

    @Override
    public String[] retrieveLoginInfo(String domain) throws CleverException {
        HashMap<String, String[]> logins = this.getDomainLogins();
        if (logins.containsKey(domain)) {
            return logins.get(domain);
        } else {
            String localDomain = (String) this.owner.invoke("FederationListenerAgent", "getLocalDomainName", true, new ArrayList<Object>());
            String user = "HADOOP-" + localDomain;
            String password = Support.generatePassword(7);
            for (int count = 0; count < 10; count++) {//prova massimo 10 volte perchè oltre deve essere per forza un problema di connessione.
                this.logger.info("Generated authentication info: user=" + user + " pass=" + password);
                ArrayList<Object> commandParams = new ArrayList<Object>();
                commandParams.add(user);
                commandParams.add(Client.CLIENT_TYPE.DOMAIN.toString());
                commandParams.add(password);
                ArrayList<Object> federationParams = new ArrayList<Object>();
                federationParams.add(domain);
                federationParams.add("HadoopNamenodeAgent");
                federationParams.add("registerClient");
                federationParams.add(new Boolean(true));
                federationParams.add(commandParams);
                Object reply;
                try {
                    reply = this.owner.invoke("FederationListenerAgent", "forwardCommandToDomain", true, federationParams);
                } catch (CleverException ex) {
                    user = user.substring(0, user.lastIndexOf("-") - 1) + "-" + Support.generatePassword(10);
                    continue;
                }
                if (reply != null && reply instanceof Exception) {
                    user = user.substring(0, user.lastIndexOf("-") - 1) + "-" + Support.generatePassword(10);
                    continue;
                } else {
                    String groupID = (String) reply;
                    this.logger.info("Group created while registrating to domain " + domain + ": " + groupID);
                    this.insertLoginInDB(domain, user, password);
                    String[] login = {user, password};
                    return login;
                }
            }
            throw new CleverException("Not registered to domain " + domain + ". Impossible to register to domain " + domain + " after 10 tries");
        }
    }

    @Override
    public boolean authenticate(String user, String password) throws CleverException {
        return this.clientRuler.authenticate(user, password);
    }

    @Override
    public String registerClient(String client, String type, String password) throws CleverException {
        return this.clientRuler.registerClient(client, type, password);
    }

    @Override
    public void addClientToGroup(String client, String group, String user, String password) throws CleverException {
        this.clientRuler.addClientToGroup(client, group, user, password);
    }

    @Override
    public void removeClientFromGroup(String client, String group, String user, String password) throws CleverException {
        this.clientRuler.removeClientFromGroup(client, group, user, password);
    }

    @Override
    public boolean isPartOfGroup(String client, String group, String user, String password) throws CleverException {
        return this.clientRuler.isPartOfGroup(client, group, user, password);
    }

    @Override
    public String createGroup(String description, String user, String password) throws CleverException {
        this.logger.debug("Lanciato createGroup");
        return this.clientRuler.createGroup(description, user, password);
    }

    @Override
    public void deleteGroup(String group, String user, String password) throws CleverException {
        this.clientRuler.deleteGroup(group, user, password);
    }

    @Override
    public ArrayList<String[]> getMyGroups(String user, String password) throws CleverException {
        return this.clientRuler.getMyGroups(user, password);
    }

    @Override
    public ArrayList<String> getGroupMembers(String group, String user, String password) throws CleverException {
        return this.clientRuler.getGroupMembers(group, user, password);
    }

    @Override
    public String getGroupCreator(String group, String user, String password) throws CleverException {
        return this.clientRuler.getGroupCreator(group, user, password);
    }

    @Override //JUST TO TEST
    public String readClientRulerNodes() throws CleverException {
        return this.clientRuler.getStorageNodes();
    }

    @Override
    public void changePermissionOnFile(String path, String ownerGroupID, String rights, String user, String password) throws CleverException {
        this.storageRuler.changePermissionOnFile(path, ownerGroupID, FileInfo.PERMISSION_CODE.valueOf(rights), user, password);
    }

    @Override
    public HashMap<String, FileInfo.PERMISSION_CODE> getAllRightsOnFile(String path, String user, String password) throws CleverException {
        return this.storageRuler.getAllFilePermissions(path, user, password);
    }

    @Override
    public HashMap<String, FileInfo.PERMISSION_CODE> getMyRightsOnFile(String path, String user, String password) throws CleverException {
        return this.storageRuler.getMyPermissionOnFile(path, user, password);
    }

    @Override
    public void addOwnerGroupOnFile(String path, String ownerGroupID, String rights, String user, String password) throws CleverException {
        this.storageRuler.addOwnerToFile(path, ownerGroupID, FileInfo.PERMISSION_CODE.valueOf(rights), user, password);
    }

    @Override
    public void removeOwnerGroupFromFile(String path, String ownerGroupID, String user, String password) throws CleverException {
        this.storageRuler.removeOwnerFromFile(path, ownerGroupID, user, password);
    }

    @Override
    public String readStorageRulerNodes() throws CleverException {
        return this.storageRuler.getStorageNode();
    }

    @Override
    synchronized public void putFileInHDFS(String file, String dstDbPath) throws HDFSConnectionException, HDFSInternalException {
        this.logger.debug("Inserting object in HDFS. Path = " + dstDbPath + ". Content = " + file);
//        String srcPathString = "tmp"+dstDbPath;
        dstDbPath = this.StorageDirectoryInHDFS + "/" + dstDbPath;
        /*        Path srcPath = new Path(srcPathString);
         File srcFile = new File(srcPathString);
         while (srcFile.exists()) {
         srcPathString += "0";
         srcPath = new Path(srcPathString);
         srcFile = new File(srcPathString);
         }
         //sono così sicuro che il file temporaneo non esista, e lo creo.
         srcFile.createNewFile();
         //copio il contenuto del file passato come stringa nel file temporaneo creato
         FileWriter writer = new FileWriter(srcFile);
         StringReader reader = new StringReader(file);
         while (true) {
         int c = reader.read();
         if (c == -1)
         break;
         writer.write(c);
         }
         writer.close();
         reader.close(); //fino a qui arriva
         */        //Creo il file nel filesystem:
        this.logger.debug("Reading configuration files");
        Configuration conf = new Configuration();
        conf.addResource(new Path(coreHadoop));
        conf.addResource(new Path(hdfsHadoop));
        conf.addResource(new Path(mapredHadoop));
        this.logger.debug("Read configuration files and created Configuration object");
        FileSystem fileSystem = null;
        try {
            try {
                Timestamper.write("P04-inizioConnessioneConNamenode(PUT)");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
            fileSystem = FileSystem.get(conf);

            try {
                Timestamper.write("P05-riuscitaConnessioneConNamenode(PUT)");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
        } catch (IOException ex) {
            try {
                Timestamper.write("P05B-fallitaConnessioneConNamenode(PUT)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            throw new HDFSConnectionException(ex);
        }
        this.logger.debug("Connection 1 to HadoopFS opened");
        Path dstPath = new Path(dstDbPath);

        FSDataOutputStream fsOut = null;
        try {
            try {
                Timestamper.write("P06-inizioCreazioneFileSuHadoop(PUT)");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
            fsOut = fileSystem.create(dstPath, true); //overwrite the file, if it exists
            try {
                Timestamper.write("P07-riuscitaCreazioneFileSuHadoop(PUT)");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
            this.logger.debug("Created destination Path (the file is still empty)");
        } catch (IOException ex) {
            try {
                Timestamper.write("P07B-fallitaCreazioneFileSuHadoop(inizioRecovery)(PUT)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            logger.error("Error creating file " + dstPath.getName() + " in HDFS: " + ex.getMessage());
            try {
                fileSystem.delete(dstPath, true);
            } catch (IOException ex1) {
                logger.warn("Error trying to recover from failed creation of file " + dstPath.getName() + " in HDFS: error occurred while deleting file: " + ex1.getMessage());
            } finally {
                try {
                    fileSystem.close();
                } catch (IOException ex1) {
                    throw new HDFSConnectionException(ex1);
                }
//                srcFile.delete();
                try {
                    Timestamper.write("P07C-fallitaCreazioneFileSuHadoop(fineRecovery)(PUT)");
                } catch (IOException ex1) {
                    this.logger.warn("can't write timestamp log: " + ex1.getMessage());
                }
                throw new HDFSInternalException("Failed while creating new file in HDFS: " + ex.getMessage());
            }
        }

        /*
         fileSystem.close();
         this.logger.debug("Connection 1 with HDFS closed");
        
         fileSystem = FileSystem.get(conf);
         this.logger.debug("Connection 2 with HDFS opened");
         */
        StringReader reader = null;

        try {

            this.logger.debug("Coping file content in HDFS");

            /*            
             fileSystem.copyFromLocalFile(srcPath, dstPath);
             */
            reader = new StringReader(file);

            try {
                Timestamper.write("P08-inizioScritturaFileSuHadoop(PUT)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }

            while (true) {
                int c = reader.read();
                if (c == -1) {
                    break;
                }
                fsOut.write(c); //fsOut non può essere null
            }

            try {
                Timestamper.write("P09-riuscitaScritturaFileSuHadoop(PUT)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }

            this.logger.debug("File content copied in HDFS");

        } catch (IOException ex) {

            try {
                Timestamper.write("P09B-fallitaScritturaFileSuHadoop(inizioRecovery)(PUT)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }

            logger.error("Error creating file " + dstPath.getName() + " in HDFS: " + ex.getMessage());
            try {
                fileSystem.delete(dstPath, true);
                if (reader != null) {
                    reader.close();
                }
                if (fsOut != null) {
                    fsOut.close();
                }
            } catch (IOException ex1) {
                logger.warn("Error trying to recover from failed copy of temp in file " + dstPath.getName() + " in HDFS: error occurred while deleting file: " + ex1.getMessage());
            } finally {
                try {
                    fileSystem.close();
                } catch (IOException ex2) {
                    throw new HDFSConnectionException(ex2);
                }
                //            srcFile.delete();
                try {
                    Timestamper.write("P09C-fallitaScritturaFileSuHadoop(fineRecovery)(PUT)");
                } catch (IOException ex1) {
                    this.logger.warn("can't write timestamp log: " + ex1.getMessage());
                }
                throw new HDFSInternalException("Error while coping file contento from local temp file to HDFS: " + ex.getMessage());
            }
        }
        if (reader != null) {
            reader.close();
        }
        if (fsOut != null) {
            try {
                this.logger.debug("FSOUT DIVERSO DA NULL");

                try {
                    Timestamper.write("P10-inizioChiusuraFileSuHadoop(PUT)");
                } catch (IOException ex1) {
                    this.logger.warn("can't write timestamp log: " + ex1.getMessage());
                }
                fsOut.close();
                this.logger.debug("FSOUT CLOSED");
                try {
                    Timestamper.write("P11-riuscitaChiusuraFileSuHadoop(PUT)");
                } catch (IOException ex1) {
                    this.logger.warn("can't write timestamp log: " + ex1.getMessage());
                }
            } catch (IOException ex) {
                try {
                    this.logger.debug("FALLITA CHIUSURA");

                    Timestamper.write("P11B-fallitaChiusuraFileSuHadoop(PUT)");
                } catch (IOException ex1) {
                    this.logger.warn("can't write timestamp log: " + ex1.getMessage());
                }
                try {
                    fileSystem.close();
                } catch (IOException ex2) {
                }
                throw new HDFSInternalException(ex);
            }
        }
        try {
            try {
                Timestamper.write("P12-inizioChiusuraConnessioneConNamenode(PUT)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            this.logger.debug("FS CLOSING");

            fileSystem.close();
            this.logger.debug("FS CLOSED");

            try {
                Timestamper.write("P13-riuscitaChiusuraConnessioneConNamenode(PUT)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
        } catch (IOException ex2) {
            this.logger.debug("FS HIUSURA FALLITA");

            try {
                Timestamper.write("P13B-fallitaChiusuraConnessioneConNamenode(PUT)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            throw new HDFSConnectionException(ex2);
        }
        this.logger.debug("Connection 2 with HDFS closed");
        //       srcFile.delete();
    }

    @Override
    synchronized public String getFileFromHDFS(String srcDbPath) throws HDFSConnectionException, HDFSInternalException, IOException {
        /*        String localTempFileName = "tmp"+srcDbPath.substring(srcDbPath.lastIndexOf("/")+1, srcDbPath.length()-1);
         File localTempFile = new File (localTempFileName);
         Path localTempFilePath = new Path (localTempFileName);
         while (localTempFile.exists()) {
         localTempFileName += "0";
         localTempFile = new File (localTempFileName);
         localTempFilePath = new Path (localTempFileName);
         }
         //Ora sicuramente il file temporaneo non esiste
         localTempFile.createNewFile();
         */
        Path srcPath = new Path(this.StorageDirectoryInHDFS + "/" + srcDbPath);

        Configuration conf = new Configuration();
        conf.addResource(new Path(coreHadoop));
        conf.addResource(new Path(hdfsHadoop));
        conf.addResource(new Path(mapredHadoop));
        FileSystem fileSystem = null;
        try {
            try {
                Timestamper.write("G06-inizioConnessioneConNamenode(GET)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            fileSystem = FileSystem.get(conf);
            try {
                Timestamper.write("G07-riuscitaConnessioneConNamenode(GET)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
        } catch (IOException ex) {
            try {
                Timestamper.write("G07B-fallitaConnessioneConNamenode(GET)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            throw new HDFSConnectionException(ex);
        }
        try {
            try {
                Timestamper.write("G08-inizioControlloEsistenzaFileSuHadoop(GET)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            if (!fileSystem.exists(srcPath)) {
                try {
                    Timestamper.write("G08C-fileNonTrovatoSuHadoop(GET)");
                } catch (IOException ex1) {
                    this.logger.warn("can't write timestamp log: " + ex1.getMessage());
                }
                try {
                    fileSystem.close();
                } catch (IOException ex) {
                }
                throw new HDFSInternalException("File doesn't exist in HDFS");
            }
        } catch (IOException ex) {
            try {
                Timestamper.write("G08B-fallitoControlloEsistenzaFile(GET)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            if (!(ex instanceof HDFSInternalException)) {
                try {
                    fileSystem.close();
                } catch (IOException ex1) {
                }
                throw new HDFSInternalException(ex);
            }
        }
        try {
            Timestamper.write("G09-fileTrovatoSuHadoop(GET)");
        } catch (IOException ex1) {
            this.logger.warn("can't write timestamp log: " + ex1.getMessage());
        }
        /*        try {
         fileSystem.copyToLocalFile(srcPath, localTempFilePath);
         } catch (IOException ex) {
         localTempFile.delete();
         throw new IOException("Error coping file "+srcDbPath+" from HDFS to local temp file: "+ex.getMessage());
         }
         */ StringWriter writer = new StringWriter();
//        FileReader reader = null;
        FSDataInputStream fsIn = null;
        String result;

        /*        try {
         reader = new FileReader(localTempFile);
         } catch (FileNotFoundException ex) {
         //Non verrà mai lanciata perchè il file, per come è scritto il codice, esiste certamente
         throw new IOException ("Temp File not found");
         }
         */
        //la condizione seguente sarà sempre soddisfatta
        try {
            try {
                Timestamper.write("G10-inizioAperturaFileSuHadoop(GET)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            fsIn = fileSystem.open(srcPath);
            try {
                Timestamper.write("G11-fineApertura-inizioLetturaFileSuHadoop(GET)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            while (true) {
                int c = fsIn.read(); //non può essere null
                if (c == -1) {
                    break;
                }
                writer.write(c);
            }
            try {
                Timestamper.write("G12-fineLetturaFileSuHadoop(GET)-inizioChiusura");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            fsIn.close();
            try {
                Timestamper.write("G13-fineChiusuraFileSuHadoop(GET)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
        } catch (IOException ex) {
            try {
                Timestamper.write("G13B-erroreSuHadoopTraAperturaEChiusuraFile(GET)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            try {
                fileSystem.close();
            } catch (IOException ex1) {
            }
            throw new HDFSInternalException(ex);
        }

        try {
            Timestamper.write("G14-inizioCreazioneStringaContenutoFile(GET)");
        } catch (IOException ex1) {
            this.logger.warn("can't write timestamp log: " + ex1.getMessage());
        }
        result = writer.toString();
        try {
            Timestamper.write("G15-fineCreazioneStringaContenutoFile(GET)");
        } catch (IOException ex1) {
            this.logger.warn("can't write timestamp log: " + ex1.getMessage());
        }
        writer.close();
        try {
            try {
                Timestamper.write("G16-inizioChiusuraConnessioneConNamenode(GET)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            fileSystem.close();
            try {
                Timestamper.write("G17-riuscitaChiusuraConnessioneConNamenode(GET)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
        } catch (IOException ex) {
            try {
                Timestamper.write("G17B-fallitaChiusuraConnessioneConNamenode(GET)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: " + ex1.getMessage());
            }
            throw new HDFSConnectionException(ex);
        }
//        localTempFile.delete();
        return result;
    }

    @Override
    synchronized public void deleteFileFromHDFS(String dbPath) throws HDFSInternalException, HDFSConnectionException {
        Path path = new Path(this.StorageDirectoryInHDFS + "/" + dbPath);
        Configuration conf = new Configuration();
        conf.addResource(new Path(coreHadoop));
        conf.addResource(new Path(hdfsHadoop));
        conf.addResource(new Path(mapredHadoop));
        FileSystem fileSystem = null;
        try {
            fileSystem = FileSystem.get(conf);
        } catch (IOException ex) {
            throw new HDFSConnectionException(ex);
        }
        try {
            if (!fileSystem.exists(path)) {
                return; //se il file non esiste si può considerare come se è stato cancellato
            }
            fileSystem.delete(path, true);
        } catch (IOException ex) {
            try {
                fileSystem.close();
            } catch (IOException ex1) {
            }
            throw new HDFSInternalException(ex);
        }
        try {
            fileSystem.close();
        } catch (IOException ex) {
            throw new HDFSConnectionException(ex);
        }
    }

    /* @Override
     public void putFile(String fileBuffer, String dstDbPath, String user, String password, Boolean forwardable) throws CleverException {
     this.putFile(fileBuffer, dstDbPath, user, password, "-1", forwardable);
     } */
    @Override
    public void putFile(String fileBuffer, String dstDbPath, String user, String password, String timeout, Boolean forwardable) throws CleverException {
        this.logger.debug("Entrato in putFile con timeout = " + timeout);
        this.storageRuler.putFile(fileBuffer, dstDbPath, user, password, forwardable.booleanValue(), Long.parseLong(timeout));
    }

    /* @Override
     public String getFile(String srcDbPath, String user, String password) throws CleverException {
     return this.getFile(srcDbPath, user, password, "-1");
     } */
    @Override
    public String getFile(String srcDbPath, String user, String password, String timeout) throws CleverException {
        return this.storageRuler.getFile(srcDbPath, user, password, Long.parseLong(timeout));
    }

    @Override
    public boolean existsFile(String srcDbPath, String user, String password) throws CleverException {
        return this.storageRuler.existsFile(srcDbPath, user, password);
    }

    @Override
    public void moveFile(String srcDbPath, String dstDbPath, String user, String password) throws CleverException {
        this.storageRuler.moveFile(srcDbPath, dstDbPath, user, password);
    }

    /* @Override
     public void deleteFile(String srcDbPath, String user, String password) throws CleverException {
     this.deleteFile(srcDbPath, user, password, "-1");
     } */
    @Override
    public void deleteFile(String srcDbPath, String user, String password, String timeout) throws CleverException {
        this.storageRuler.deleteFile(srcDbPath, user, password, Long.parseLong(timeout));
    }

    // @Override
    /*public String submitJob(String input, String output) throws Exception {
     throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
     }*/
    @Override
    public ArrayList submitJob(String fileBuffer, String jobName, String bucket, String fileS3Name, Long startByte, Long endByte, Byte p, Integer vm) throws CleverException {
        ArrayList<String> filePaths = new ArrayList<String>();
        ArrayList<String> paths = new ArrayList<String>();
        ArrayList urList = new ArrayList<String>();
        //ArrayList<String> transcodepaths = new ArrayList<String>();

        S3Tools s3 = new S3Tools(this.logger);
        Path dest;

        String file = fileS3Name.substring(0, fileS3Name.indexOf("."));
        String format = fileS3Name.substring(fileS3Name.indexOf("."));
        //String newFileName = "transcodedPart-" + p + "-" + file;
        String home = "/home/apanarello/";
        //Path home_ = new Path("/home/apanarello/");
        //Path hdsf_Path = new Path("/input/");
        //String merge = "";
        String fileAfterTranscode = null;
        //logger.debug("PROVO A LANCIARE IL GETFILE CON IL SEGUENTE PATH DI DESTINAZIONE: "+dest);
        //Process pro=null;
        logger.debug("File name senza estensione è: " + file);
        try {
            logger.debug("Provo l'autenticazione su s3");
            s3.getAuth(fileBuffer);
            logger.debug("Autenticazione fatto prima del get file");
            byte part = 0;

            //-----------------------------------------------
            long div = (endByte - startByte) / vm;
            try {
                Timestamper.write("Time10-InizioDownloadS3File");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
            for (long i = startByte; i < (endByte); i = i + div) {
                dest = new Path(home + file + "-part-" + p + "-" + part + format);
                s3.getFileFromS3(fileBuffer, dest.toString(), bucket, fileS3Name, i, (i + div));
                paths.add(file + "-part-" + p + "-" + part + format);
                part++;
            }
            //-------------------------------------------------
            try {
                Timestamper.write("Time11-FineDownloadS3File");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
            //s3.getFileFromS3(fileBuffer, dest, bucket, fileS3Name, startByte, endByte);
            logger.debug("GET S3 eseguito con successo");
        } catch (IOException ex) {
            logger.error("Error in IO ", ex);
        }
        for (byte y = 0; y < paths.size(); y++) {
            logger.debug("lista di paths: " + " path " + y + "= " + home + paths.get(y));

        }
        Process ps = null;
        File files = null;
        try {
            //-------------------------------
            try {
                Timestamper.write("Time12-InizioUpLoadSuHadoop");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }

            for (byte y = 0; y < paths.size(); y++) {
                ps = Runtime.getRuntime().exec("hadoop fs -put " + home + paths.get(y) + " /input/ ");
                ps.waitFor();
                /*
                 try {
                 this.putFileInHDFS(home + paths.get(y), new Path("/input/"), paths.get(y));
                 } catch (HDFSConnectionException ex) {
                 logger.error("Error to put file to HDSF path: /input/" + paths.get(y), ex);
                 throw new CleverException("Error to write in HDSH path: " + "/input/" + paths.get(y));

                 }
                 */

                //---------------------------
            }
            try {
                Timestamper.write("Time12-FineUpLoadSuHadoop");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
            
            //cancello file scaricati da s3 dal sistema
            for (byte y = 0; y < paths.size(); y++) {
                files = new File(home + paths.get(y));
                if (files.delete()) {
                    logger.debug("deleted file: " + home + paths.get(y));
                } else {
                    throw new CleverException("ERROR DURING DELETING FILE");
                }
            }
            logger.debug("PUTFILE ESEGUITO CON SUCCESSO: ");

            /*
            * Solo per misure- Non eseguo il job---
            * Ma faccio un get del file e lo taglio di un certa percentuale
            *
            *
            *
            */
            
            
            
            
            
            //DOVREBBE ESSERE UN JOB DI HADOOP A FARE LA TRANSCODIFICA
            logger.debug("DOVREI LANCIARE IL JOB" + " hadoop jar " + home + jobName + " com.manuh.vidproc.DirectVideoProcessor /input/ /output/");
            //this.wait(10000);

            //___________---------------
            ps = Runtime.getRuntime().exec("hadoop jar " + home + "Transcodifica/dist/Transcodifica.jar" + " com.manuh.vidproc.DirectVideoProcessor /input /output");

            //ps = Runtime.getRuntime().exec("hadoop jar " + home + "HadoopVideoTranscode/dist/VideoTranscode.jar" + " com.manuh.vidproc.DirectVideoProcessor /input/ /output");
            ps.waitFor();

            //___________---------------
            logger.debug("JOB DONE - try to cancel local files. exit value: " + ps.exitValue());
            if (ps.exitValue() == 0) {

                logger.debug("JOB DONE - try to cancel local files.");

                ps = Runtime.getRuntime().exec("hadoop fs -rm /input/*");
                ps.waitFor();
                if (ps.exitValue() == 0) {
                    try {
                        Timestamper.write("Time13-InizioUpLoadSuS3");
                    } catch (IOException ex) {
                        this.logger.warn("can't write timestamp log: " + ex.getMessage());
                    }
                    for (byte y = 0; y < paths.size(); y++) {
                        fileAfterTranscode = paths.get(y).substring(0, paths.get(y).indexOf(".")) + ".mpeg";
                        ps = Runtime.getRuntime().exec("hadoop fs -get /output/" + paths.get(y) + " " + home + fileAfterTranscode);
                        ps.waitFor();
                        filePaths.add(fileAfterTranscode);
                        urList.add("https://s3.amazonaws.com/" + bucket + "/" + fileAfterTranscode);

                        logger.debug("url added to arryalist= " + "https://s3.amazonaws.com/" + bucket + "/" + fileAfterTranscode);
                        if (ps.exitValue() == 0) {
                        } else {
                            logger.error("error to getfile " + home + filePaths.get(y));
                        }

                    }
                } else {
                    logger.error("error to deletefile from hdfs ");

                }

                /*
                 for (byte y = 0; y < paths.size(); y++) {
                 try {
                 logger.debug("!!!try to delete file from hdfs: " + home + paths.get(y));
                 this.deleteFileFromHDFS(new Path("/input/" + paths.get(y)));
                 logger.debug("!!!deleted file from hdfs: " + home + paths.get(y));

                 } catch (IOException ex) {
                 logger.error("Error to deletefile from hdfs: /input/" + paths.get(y), ex);
                 } catch (Exception ex) {
                 logger.error("Error to deletefile from hdfs: /input/" + paths.get(y), ex);
                 }
                 //___-----___-__-__--____--
                 files = new File(home + paths.get(y));
                 if (files.delete()) {
                 logger.debug("deleted file from fisical fs: " + home + paths.get(y));
                 } else {
                 throw new CleverException("ERROR DURING DELETING FILE");
                 }
                 fileAfterTranscode = paths.get(y).substring(0, paths.get(y).indexOf(".")) + ".mpeg";

                 // ps = Runtime.getRuntime().exec("hadoop fs -get /output/" + paths.get(y) + " " + home+fileAfterTranscode);
                 // ps.waitFor();
                 try {
                 logger.debug("!!!launching getFIleFromHDFS - src: " + "/output/" + paths.get(y) + " dstPath : " + home + fileAfterTranscode);
                 this.getFileFromHDFS(new Path("/output/" + paths.get(y)), home + fileAfterTranscode);
                 filePaths.add(fileAfterTranscode);
                 logger.debug("get file  DONE - new file= " + fileAfterTranscode);

                 } catch (HDFSConnectionException ex) {
                 logger.error("Error to get file from HDSF path: /output/" + paths.get(y), ex);
                 throw new CleverException("Error to get file from HDSF path: /output/" + paths.get(y));
                 } catch (HDFSInternalException ex) {
                 logger.error("INTERNAL EXCEPTION Error to get file from HDSF path: /output/" + paths.get(y), ex);
                 throw new CleverException("Error to get file from HDSF path: /output/" + paths.get(y));
                 }
                 // if (ps.exitValue() == 0) {
                 urList.add("https://s3.amazonaws.com/" + bucket + "/" + fileAfterTranscode);

                 logger.debug("url added to arryalist= " + "https://s3.amazonaws.com/" + bucket + "/" + fileAfterTranscode);

                 }
                
                 */
                //faccio upload su S3 dei file transcodificati
                try {
                    for (int y = 0; y < filePaths.size(); y++) {

                        s3.uploadFile(home + filePaths.get(y), "outputfederation", filePaths.get(y));

                    }
                } catch (Exception ex) {
                    logger.error("Error during uploading s3file", ex);

                }
                //fileAfterMerge = "transcoded_" + file + "-" + p + ".mpeg";
                
                //cancello file trascodificato dal sistema
                for (int y = 0; y < filePaths.size(); y++) {
                    files = new File(home + filePaths.get(y));
                    if (files.delete()) {
                        logger.debug("deleted file: " + home + filePaths.get(y));
                    } else {
                        throw new CleverException("ERROR DURING DELETING FILE");
                    }
                }
            } else {
                throw new CleverException("Error to exec hadoop jar; exec has returned " + ps.exitValue());
            }

            //Eseguo File SH che carica il file su hadoop, fa partire il job e poi alla fine riposizione il file nel fs locale
            //Runtime.getRuntime().exec("hadoop fs -put "+ dest +" /output-"+ p);
            //Runtime.getRuntime().exec("ffmpeg -i " + dest + " -acodec copy -vcodec mpeg4" + " /home/dissennato/output-" + p);
        } catch (RuntimeException ex) {
            logger.error("Error to exec ffmpeg transcode");
        } catch (IOException ex) {
            logger.error("IOerror in runtime.exec", ex);
        } catch (InterruptedException ex) {
            logger.error("Error to exec hadoop put pswaitfor");
        }
        //s3.uploadFile(fileBuffer, dest, bucket, destAfterTranscode);

        //     
        //....
        logger.debug("SONO NEL DOMINIO,METODO" + this.getClass().getMethods().toString());
        //url = "https://s3.amazonaws.com/" + bucket + "/" + fileAfterMerge;
        //logger.debug("Sono in submitJob, con URL: " + url + "// E -BucketName= " + bucket + " - fileName= " + fileAfterMerge + " -PartFile= " + p);
        return urList;
    }

    @Override
    public ArrayList<String> listMyFiles(String user, String password) throws CleverException {
        return this.storageRuler.listMyFiles(user, password);
    }

    /**
     *
     * @param jobName
     * @param fileS3Name
     * @throws CleverException
     */
    @Override
    public void sendJob(String fileBuffer, String jobName, String bucket, String fileS3Name, String user, String pass, Boolean forwardable) throws CleverException {

        this.logger.debug("Entrato in SendJob-NamenodePlugin ");
        try {
            this.jobRuler.sendJob(fileBuffer, jobName, bucket, fileS3Name, user, pass, forwardable);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(NamenodePlugin.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     *
     * @param fileBuffer rootkey
     * @param jobName job to send : the path of the file.jar tu execute
     * @param fileS3Name
     * @param bucket
     * @param startByte
     * @param end
     * @param p, part chuck to elaborate
     * @param vm nmbero of vms available
     * @throws CleverException
     */
    @Override
    public ArrayList sendJob(String fileBuffer, String jobName, String bucket, String fileS3Name, Long startByte, Long end, Byte p, Integer vm) throws CleverException {

        try {
            Timestamper.write("Time09-Time06-FineInoltro");
        } catch (IOException ex) {
            this.logger.warn("can't write timestamp log: " + ex.getMessage());
        }

        this.logger.debug("Entrato in SendJob-NamenodePlugin for federation :" + " JobName: " + jobName + "; BucketName: " + bucket + "; FileName S3: " + fileS3Name + "; Range Start: " + startByte + "; Range End " + end + "; PartFile " + p + ";NumVms " + vm);

        try {
            urlList = this.jobRuler.sendJob(fileBuffer, jobName, bucket, fileS3Name, startByte, end, p, vm);

        } catch (IOException ex) {
            logger.error("Error to send sendJob: ", ex);

        }
        return urlList;
    }

    /* @Override
     public void prova(String stringa) throws CleverException {
     this.logger.debug("PROVA DI INVOKE : "+this+" "+stringa);
     }*/
    @Override
    public void putFileInHDFS(String srcP, Path hdsfDstP, String file) throws HDFSConnectionException, HDFSInternalException {

        this.logger.debug("Inserting object in HDFS. Path = " + hdsfDstP.toString() + "/" + file);

        this.logger.debug("Reading configuration files");
        Configuration conf = new Configuration();
        conf.addResource(new Path(coreHadoop));
        conf.addResource(new Path(hdfsHadoop));
        conf.addResource(new Path(mapredHadoop));
        this.logger.debug("Read configuration files and created Configuration object");
        FileSystem fileSystem = null;
        java.nio.file.Path pathFile = Paths.get(srcP.toString());
        byte[] data = null;
        try {
            data = Files.readAllBytes(pathFile);
        } catch (IOException ex) {
            logger.error("Errore to read byte from file", ex);
        }
        InputStream in = new BufferedInputStream(new ByteArrayInputStream(data));

        try {

            fileSystem = FileSystem.get(conf);

        } catch (IOException ex) {
            this.logger.debug("ERROR TO FILESYSTEMconfiguration");
            throw new HDFSConnectionException(ex);
        }
        try {
            if (fileSystem.exists(hdsfDstP)) {
                this.logger.debug("directory esistente");
            } else {
                fileSystem.mkdirs(hdsfDstP);
            }
        } catch (IOException ex) {
            this.logger.error("Impossibile leggere la directory");
        }
        this.logger.debug("Connection 1 to HadoopFS opened");
        //Path dstPath = new Path(hdsfDstP.toString());
        Path filePath = new Path(hdsfDstP.toString() + "/" + file);
        FSDataOutputStream fsOut = null;

        try {

            fsOut = fileSystem.create(filePath, true); //overwrite the file, if it exists

            this.logger.debug("Created destination Path (the file is still empty)");
        } catch (IOException ex) {

            logger.error("Error creating file " + filePath.getName() + " in HDFS: " + ex.getMessage());
            try {
                fileSystem.delete(filePath, true);
            } catch (IOException ex1) {
                logger.warn("Error trying to recover from failed creation of file " + filePath.getName() + " in HDFS: error occurred while deleting file: " + ex1.getMessage());
            } finally {
                try {
                    fileSystem.close();
                } catch (IOException ex1) {
                    throw new HDFSConnectionException(ex1);
                }
//                srcFile.delete();

                throw new HDFSInternalException("Failed while creating new file in HDFS: " + ex.getMessage());
            }
        }

        // FileReader reader = null;
        try {

            org.apache.hadoop.io.IOUtils.copyBytes(in, fsOut, conf);

            this.logger.debug("Coping file content in HDFS");

            /*            
             fileSystem.copyFromLocalFile(srcPath, dstPath);
             */
            //reader = new FileReader(srcP.toString());
            //logger.debug("inizioScritturaFileSuHadoop");
/*
             while (true) {
             int c = reader.read();
             if (c == -1) {
             break;
             }
             fsOut.write(c); //fsOut non può essere null
             }
             */
            logger.debug("P09-riuscitaScritturaFileSuHadoop(PUT)");

            this.logger.debug("File content copied in HDFS");

        } catch (IOException ex) {
            logger.error("Error creating file " + hdsfDstP + " in HDFS: " + ex.getMessage());
            try {
                fileSystem.delete(hdsfDstP, true);
                /*
                 if (reader != null) {
                 reader.close();
                 }
                 if (fsOut != null) {
                 fsOut.close();
                 }
                 */
            } catch (IOException ex1) {
                logger.warn("Error trying to recover from failed copy of temp in file " + hdsfDstP.getName() + " in HDFS: error occurred while deleting file: " + ex1.getMessage());
            } finally {
                try {
                    fileSystem.close();
                } catch (IOException ex2) {
                    throw new HDFSConnectionException(ex2);
                }
                //            srcFile.delete();

                throw new HDFSInternalException("Error while coping file contento from local temp file to HDFS: " + ex.getMessage());
            }
        }
        /*
         if (reader != null) {
         try {
         reader.close();
         } catch (IOException ex) {
         logger.error("errore scrittura- errore chiusura reader", ex);
         }
         }
         */
        if (fsOut != null) {
            try {
                this.logger.debug("FSOUT DIVERSO DA NULL");

                logger.debug("inizioChiusuraFileSuHadoop ");

                fsOut.close();
                this.logger.debug("FSOUT CLOSED - riuscitaChiusura");

            } catch (IOException ex) {

                this.logger.debug("FALLITA CHIUSURA");

                try {
                    fileSystem.close();
                } catch (IOException ex2) {
                    logger.error("errore chiusure file system: ", ex2);
                }
                throw new HDFSInternalException(ex);
            }
        }
        try {

            this.logger.debug("FS CLOSING");

            fileSystem.close();
            this.logger.debug("FS CLOSED");

        } catch (IOException ex2) {
            this.logger.debug("FS HIUSURA FALLITA");

            throw new HDFSConnectionException(ex2);
        }
        this.logger.debug("Connection 2 with HDFS closed");
        //    

    }

    @Override
    public void getFileFromHDFS(Path hdsfSrcP, String dstP) throws HDFSConnectionException, HDFSInternalException, IOException {

        Configuration conf = new Configuration();
        conf.addResource(new Path(coreHadoop));
        conf.addResource(new Path(hdfsHadoop));
        conf.addResource(new Path(mapredHadoop));
        FileSystem fileSystem = null;
        logger.debug("getFile from HDSF. : " + hdfsHadoop.toString());
        try {

            fileSystem = FileSystem.get(conf);
            logger.debug("leggo confi filesystem : " + fileSystem.toString());

        } catch (IOException ex) {

            throw new HDFSConnectionException(ex);
        }
        try {

            if (!fileSystem.exists(hdsfSrcP)) {

                try {
                    fileSystem.close();
                } catch (IOException ex) {
                    logger.debug("Error closing filesystem : ", ex);
                }
                throw new HDFSInternalException("File doesn't exist in HDFS");
            }
        } catch (IOException ex) {

            if (!(ex instanceof HDFSInternalException)) {
                try {
                    fileSystem.close();
                } catch (IOException ex1) {
                    logger.debug("Error closing filesystem : ", ex1);
                }
                throw new HDFSInternalException(ex);
            }
        } catch (Exception ex) {
            logger.debug("Error closing filesystem : ", ex);
        }
        //logger.debug(fileSystem.getHomeDirectory().toString());
        //StringWriter writer = new StringWriter();
//        FileReader reader = null;
        logger.debug("CREO fsDATAiNPUTsTREAM");
        FSDataInputStream fsIn = null;
        //String result;
        Path dstLocalPath = new Path(dstP);
        File dstFile = new File(dstP);
        if (dstFile.exists()) {
            logger.debug("Il file" + dstP + " esiste");
        } else if (dstFile.createNewFile()) {
            logger.debug("Il file" + dstP + " è stato creato");
        } else {
            logger.debug("Il file" + dstP + " non può essere creato");
        }
        logger.debug("output path is: " + dstFile.toString());
        if (fileSystem.exists(hdsfSrcP)) {
            fileSystem.copyToLocalFile(hdsfSrcP, dstLocalPath);
        } else {
            logger.error("output path is: " + dstFile.toString());
        }

    }

    //Alfonso
    @Override
    public void deleteFileFromHDFS(Path dbPath) throws HDFSConnectionException, HDFSInternalException {

        logger.debug("Try to get configuration file hdsf: ");

        Configuration conf = new Configuration();
        conf.addResource(new Path(coreHadoop));
        conf.addResource(new Path(hdfsHadoop));
        conf.addResource(new Path(mapredHadoop));
        Path path = new Path(dbPath.toString());
        FileSystem fileSystem = null;
        logger.debug("configuration obtained file hdsf: ");
        try {
            fileSystem = FileSystem.get(conf);

        } catch (IOException ex) {
            logger.error("error to get configuration file hdsf: ");
            throw new HDFSConnectionException(ex);
        }
        try {
            if (!fileSystem.exists(path)) {
                logger.debug("the specified path does not exist: " + path);
                return; //se il file non esiste si può considerare come se è stato cancellato
            }
            fileSystem.delete(path, true);
        } catch (IOException ex) {
            try {
                fileSystem.close();
            } catch (IOException ex1) {
            }
            throw new HDFSInternalException(ex);
        }
        try {
            fileSystem.close();
        } catch (IOException ex) {
            throw new HDFSConnectionException(ex);
        }

    }
}
