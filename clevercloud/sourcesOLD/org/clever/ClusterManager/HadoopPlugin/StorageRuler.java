/*
 * The MIT License
 *
 * Copyright 2014 Giovanni Volpintesta
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.clever.ClusterManager.HadoopNamenode.HadoopNamenodeAgent;
import org.clever.ClusterManager.HadoopNamenode.HadoopNamenodePlugin;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.Exceptions.HDFSConnectionException;
import org.clever.Common.Exceptions.HDFSInternalException;
import org.clever.Common.StorageRuler.FileInfo;
import org.clever.Common.Timestamp.Timestamper;

/**
 *
 * @author Giovanni Volpintesta
 */
public class StorageRuler {
    
    private HadoopNamenodeAgent owner;
    private HadoopNamenodePlugin ownerPlugin;
    private ClientRuler clientRuler;
    private boolean flag ;
    
    private Logger logger;
    
    public final String agentName = "HadoopNamenodeAgent";
    public final String storageNodeName = "Storage";
    
    public StorageRuler () {
    
        flag=true;
    }
    
    public void init(HadoopNamenodeAgent owner, ClientRuler clientRuler, Logger logger) {
        this.owner = owner;
        this.ownerPlugin = (HadoopNamenodePlugin) this.owner.getPlugin();
        this.clientRuler = clientRuler;
        this.logger = logger;
        try {
            logger.info("Initializing StorageManager");
            ArrayList<Object> params = new ArrayList<Object>();
            //Check if the clients node exists
            params.add(this.agentName);
            params.add("/"+this.storageNodeName);
            boolean exist;
            exist = ((Boolean)this.owner.invoke("DatabaseManagerAgent", "existNode", true, params)).booleanValue();
            if (exist) {
                logger.info("Storage node already exists.");
            } else {
                logger.info("Storage node doesn't exist in sednaDB. Creating storage's node");
                params.clear();
                params.add(this.agentName);
                params.add("<"+this.storageNodeName+" />");
                params.add("into");
                params.add("");
                try {
                    this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
                } catch (CleverException ex) {
                    logger.error("Error inserting Storage node in DB");
                }
                logger.info("Storage node created.");
            }
            
            //TEST
            //logger.debug(this.test());
            
        } catch (CleverException ex) {
            logger.error("Error checking if Storage node exists in DB or creating it");
        }
    }
    
    synchronized public void putFile (String fileBuffer, String filePath, String user, String password, boolean forwardable, long timeout) throws CleverException {
        try {
            Timestamper.write("P03-inizioEsecuzionePutFile");
        } catch (IOException ex) {
            this.logger.warn("can't write timestamp log: "+ex.getMessage());
        }
        boolean canPut;
        boolean canCreate;
        if (!this.clientRuler.authenticate(user, password))
            throw new CleverException ("Authentication failed with user: "+user);
        ArrayList<String> owners = this.getOwnerGroups(filePath, user, password);
        if (owners.isEmpty()) {
            canPut = true;
            canCreate = true;
        } else{
            if (this.hasAnyoneWriteRights(filePath, owners)) {
                canPut = true;
                canCreate = false;
            } else {
                canPut = false;
                canCreate = false;
            }
        }
        ArrayList<String> federatedDomains = (ArrayList<String>) this.owner.invoke("FederationListenerAgent", "getFederatedDomains", true, new ArrayList<Object>());
        if (canCreate) { //CREATE
            logger.debug("Numero Federated Domains"+federatedDomains.size());
            String creatorID;
            try {
                creatorID = this.clientRuler.getFirstGroupID(user, password);
            } catch (CleverException ex) {
                throw new CleverException ("Failed while retrieving the primary group of the user: "+user+". Exception caught: "+ex.getMessage());
            }
            String fileID;
            try {
                fileID = this.generateFileID();
            } catch (CleverException ex) {
                throw new CleverException ("Failed while generating the ID for the file. The file was not been inserted but the database is still in a consistent state. Exception caught: "+ex.getMessage());
            }
            String domain = null;
            Process pro1;
            try {
                int num=federatedDomains.size();
                this.ownerPlugin.putFileInHDFS(fileBuffer, fileID);    
                String s = Integer.toString(num);
                
                pro1=Runtime.getRuntime().exec("sh /home/apanarello/misure.sh "+s);
                 
            } catch (HDFSInternalException ex) {
                logger.info("Failed while inserting the file in HDFS. The file was not been inserted but the database is still in a consistent state. Trying to put it in a faderated domain. Exception caught: "+ex.getMessage());
                if (!forwardable)
                    throw new CleverException ("Failed while inserting the file in HDFS. The method isn't forwardable. The file couldn't be inserted anywhere, but the database is still in a consistent state.");
                else {
                        ArrayList<Object> federationParams = new ArrayList<Object>();
                        ArrayList<Object> commandParams = new ArrayList<Object>();
                    
                   //while (true) {
                        if (!federatedDomains.isEmpty()){ //note that the local domain isn't present in list 
                            //break;
                        try {
                            Timestamper.write("P14-inizioSceltaDominioPerPut");
                        } catch (IOException ex1) {
                            this.logger.warn("can't write timestamp log: "+ex1.getMessage());
                        }
                        //Random rnd = new Random(System.currentTimeMillis());
                        //domain = federatedDomains.get(rnd.nextInt(federatedDomains.size()));
                        
                        String[] login;
                        for(int i=0;i<federatedDomains.size();i++){
                            domain=federatedDomains.get(i);
                            logger.info("Domain "+domain+" was choosen");
                            login=this.ownerPlugin.retrieveLoginInfo(domain);
                            logger.info("Domain authentication info retrieved: user="+login[0]+" pass="+login[1]);
                            logger.debug("Timeout = "+timeout);
                            try {
                                Timestamper.write("P15-fineSceltaDominioPerPut");
                            } catch (IOException ex1) {
                                this.logger.warn("can't write timestamp log: "+ex1.getMessage());
                            }
                            federationParams.clear();
                            commandParams.clear();
                            commandParams.add(fileBuffer);
                            commandParams.add(fileID);
                            commandParams.add(login[0]); //user
                            commandParams.add(login[1]); //password
                            commandParams.add(String.valueOf(timeout)); //because it's the HadoopNamenodePlugin method, not the StorageRuler's one.
                            commandParams.add(new Boolean(false)); //not forwardable
                            federationParams.add(domain);
                            federationParams.add(this.agentName);
                            federationParams.add("putFile");
                            federationParams.add(true); //even if it's false. The reply isn't used in any case
                            federationParams.add(commandParams);
                            federationParams.add(new Long(timeout));
                            try {
                                this.logger.debug("Launching the command to choosen domain...");
                                try {   
                                    Timestamper.write("P16-inizioLancioPutFileSuDominioFederato");
                                } catch (IOException ex1) {
                                this.logger.warn("can't write timestamp log: "+ex1.getMessage());
                                }
                                Object reply = this.owner.invoke("FederationListenerAgent", "forwardCommandToDomainWithTimeout", true, federationParams);
                                this.logger.debug("INVOCATO FORWARD");
                                try {
                                    Timestamper.write("P17-riuscitoLancioPutFileSuDominioFederato");
                                } catch (IOException ex1) {
                                    this.logger.warn("can't write timestamp log: "+ex1.getMessage());
                                }
                                this.logger.info("Command launched. Reply: "+reply);
                                if (reply!=null && reply instanceof Exception) {
                                    throw new CleverException ((Exception) reply);
                                }
                                this.logger.debug("Command successfully launched on domain "+domain);
                            //break;
                            } catch (CleverException ex1) {
                                    try {
                                            Timestamper.write("P17B-fallitoLancioPutFileSuDominioFederato");
                                    } catch (IOException ex2) {
                                            this.logger.warn("can't write timestamp log: "+ex2.getMessage());
                                    }
                                this.logger.warn("Exception caught while forwarding putFile method on "+domain+" domain: "+ex1);
                                federatedDomains.remove(domain);
                                i--;
                                }
                            }
                        if (federatedDomains.isEmpty()) {
                        throw new CleverException ("Failed while inserting the file in HDFS and then while trying to insert it in all of the federated domain. The file couldn't be inserted anywhere, but the database is still in a consistent state.");
                        }
                    }
                }
            } catch (HDFSConnectionException ex) {
                throw new CleverException ("Error while connecting or disconnecting to HDFS: "+ex);
            } catch (IOException ex) {
                throw new CleverException ("Error while inserting the file in the HDFS. This error isn't related to HFDS neither to the connection: "+ex);
            }
            if (domain == null) //It has been inserted in local domain
                domain = FileInfo.LOCAL_DOMAIN;
            try {
                try {
                    Timestamper.write("P18-inizioCreazioneFileInfo(PutFile)");
                } catch (IOException ex1) {
                    this.logger.warn("can't write timestamp log: "+ex1.getMessage());
                }
                this.createFileInfo(filePath, fileID, domain, creatorID);
                try {
                    Timestamper.write("P19-riuscitaCreazioneFileInfo(PutFile)");
                } catch (IOException ex1) {
                    this.logger.warn("can't write timestamp log: "+ex1.getMessage());
                }
            } catch (CleverException ex) {
                try {
                    Timestamper.write("P19B-fallitaCreazioneFileInfo(PutFile)(inizioRecovery)");
                } catch (IOException ex1) {
                    this.logger.warn("can't write timestamp log: "+ex1.getMessage());
                }
                try {
                    if (domain.compareTo(FileInfo.LOCAL_DOMAIN)==0)
                        this.ownerPlugin.deleteFileFromHDFS(fileID);
                    else {
                        String[] login = this.ownerPlugin.retrieveLoginInfo(domain);
                        ArrayList<Object> federationParams = new ArrayList<Object>();
                        ArrayList<Object> commandParams = new ArrayList<Object>();
                        commandParams.add(fileID);
                        commandParams.add(login[0]); //user
                        commandParams.add(login[1]); //password
                        commandParams.add(String.valueOf(timeout)); //because it's the HadoopNamenodePlugin method, not the StorageRuler's one.
                        federationParams.add(domain);
                        federationParams.add(this.agentName);
                        federationParams.add("deleteFile");
                        federationParams.add(true); //even if it's false. The reply isn't used in any case
                        federationParams.add(commandParams);
                        federationParams.add(new Long(timeout));
                        Object reply = this.owner.invoke("FederationListenerAgent", "forwardCommandToDomainWithTimeout", true, federationParams);
                        if (reply instanceof CleverException) {
                            throw new CleverException ((CleverException)reply);
                        }
                    }
                } catch (Exception ex1) {
                    try {
                    Timestamper.write("P19D-fallitaCreazioneFileInfo(PutFile)(fallitoRecovery)");
                } catch (IOException ex2) {
                    this.logger.warn("can't write timestamp log: "+ex2.getMessage());
                }
                    throw new CleverException ("Error while creating file node in DB after put it in HDFS. An Error occurred also while deleting it from HDFS to recover from error. HDFS AND DB ARE NOT IN A CONSISTENT STATE ANYMORE. Exception caught: "+ex1.getMessage());
                }
                try {
                    Timestamper.write("P19C-fallitaCreazioneFileInfo(PutFile)(riuscitoRecovery)");
                } catch (IOException ex1) {
                    this.logger.warn("can't write timestamp log: "+ex1.getMessage());
                }
                throw new CleverException ("Error while creating file node in DB after put it in HDFS. The file in HDFS was deleted to recover from error. HDFS and DB are in a consistent state. Exception caught: "+ex.getMessage());
            }
        } else if (canPut) { //MODIFY
            FileInfo fileInfo = this.getFileInfoFromOwners(filePath, owners);
            String fileID = fileInfo.getID();
            String domain = fileInfo.getDomain();
            if (domain.compareTo(FileInfo.LOCAL_DOMAIN)==0) {
                try {
                    this.ownerPlugin.putFileInHDFS(fileBuffer, fileID);
                } catch (IOException ex) {
                    throw new CleverException ("Error while modifing file in local HDFS. THE ERROR IS UNRECUPERABLE. THE DATABASE COULD BE IN UNCONSISTEND STATE. Exception caught: "+ex.getMessage());
                }
            } else {
                logger.info("Failed while inserting the file in HDFS. The file was not been inserted but the database is still in a consistent state. Trying to put it in a faderated domain.");
                ArrayList<Object> federationParams = new ArrayList<Object>();
                ArrayList<Object> commandParams = new ArrayList<Object>();
                String[] login = this.ownerPlugin.retrieveLoginInfo(domain);
                if (login == null)
                    throw new CleverException ("Impossible authenticate with the domain in which the file should be stored. Put operation aborted. DB is still in a consistend state.");
                commandParams.add(fileBuffer);
                commandParams.add(fileID);
                commandParams.add(login[0]); //user
                commandParams.add(login[1]); //password
                commandParams.add(String.valueOf(timeout)); //because it's the HadoopNamenodePlugin method, not the StorageRuler's one.
                commandParams.add(new Boolean(false)); //not forwardable
                federationParams.add(domain);
                federationParams.add(this.agentName);
                federationParams.add("putFile");
                federationParams.add(true); //even if it's false. The reply isn't used in any case
                federationParams.add(commandParams);
                federationParams.add(new Long(timeout));
                Object reply = null;
                try {
                    reply = this.owner.invoke("FederationListenerAgent", "forwardCommandToDomainWithTimeout", true, federationParams);
                } catch (CleverException ex) {
                    throw new CleverException ("Error while modifing file in local HDFS. THE ERROR IS UNRECUPERABLE. THE DATABASE COULD BE IN UNCONSISTEND STATE Exception caught: "+ex.getMessage());
                }
                if (reply!=null && reply instanceof Exception) {
                    throw new CleverException ("Error while modifing file in local HDFS. THE ERROR IS UNRECUPERABLE. THE DATABASE COULD BE IN UNCONSISTEND STATE. Exception caught: "+((Exception)reply).getMessage());
                }
            }  
        } else { //DO NOTHING
            throw new CleverException ("A file with the specified path already exists and you don't have rights to overwrite it.");
        }
        try {
            Timestamper.write("P20-fineEsecuzionePutFile");
        } catch (IOException ex) {
            this.logger.warn("can't write timestamp log: "+ex.getMessage());
        }
    }
    
    synchronized public String getFile (String filePath, String user, String password, long timeout) throws CleverException {
        try {
            Timestamper.write("G03-inizioEsecuzioneGetFile");
        } catch (IOException ex) {
            this.logger.warn("can't write timestamp log: "+ex.getMessage());
        }
        try {
            if (!this.clientRuler.authenticate(user, password))
                throw new CleverException ("Authentication failed with user: "+user);
        } catch (CleverException ex) {
            throw new CleverException ("Error while trying to authenticate to the Storage system: "+ex.getMessage());
        }
        ArrayList<String> owners;
        try {
            owners = this.getOwnerGroups(filePath, user, password);
        } catch (CleverException ex) {
            throw new CleverException ("Error while retrieving groups of the user that are also owner of the file: "+ex.getMessage());
        }
        if (owners.isEmpty())
            throw new CleverException ("File doesn't exist");
        try {
            if (!this.hasAnyoneReadRights(filePath, owners))
                throw new CleverException ("The file exists but you don't have read permission on it");
        } catch (CleverException ex) {
            throw new CleverException ("Error while checking for owners with read rights: "+ex.getMessage());
        }
        FileInfo fileInfo;
        try {
            try {
                Timestamper.write("G04-inizioEstrazioneFileInfo(GetFile)");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: "+ex.getMessage());
            }
            fileInfo = this.getFileInfoFromOwners(filePath, owners);
            try {
                Timestamper.write("G05-riuscitaEstrazioneFileInfto(GetFile)");
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: "+ex.getMessage());
            }
        } catch (CleverException ex) {
            try {
                Timestamper.write("G05B-fallitaEstrazioneFileInfto(GetFile)");
            } catch (IOException ex1) {
                this.logger.warn("can't write timestamp log: "+ex1.getMessage());
            }
            throw new CleverException ("Error while retrieving file's info from DB: "+ex.getMessage());
        }
        String domain = fileInfo.getDomain();
        String fileID = fileInfo.getID();
        String fileBuffer = null;
        if (domain.compareTo(FileInfo.LOCAL_DOMAIN)==0) {
            try {
                fileBuffer = this.ownerPlugin.getFileFromHDFS(fileID);
            } catch (IOException ex) {
                throw new CleverException ("Error while retrieving file from HDFS: "+ex.getMessage());
            }
            if (fileBuffer == null) {
                throw new CleverException ("No file retrieved from HDFS");
            }
        } else {
            ArrayList<Object> federationParams = new ArrayList<Object>();
            ArrayList<Object> commandParams = new ArrayList<Object>();
            String[] login;
            try {
                try {
                    Timestamper.write("G18-inizioRecuperoCredenzialiDominio(GetFile)");
                } catch (IOException ex) {
                    this.logger.warn("can't write timestamp log: "+ex.getMessage());
                }
                login = this.ownerPlugin.retrieveLoginInfo(domain);
                try {
                    Timestamper.write("G19-riuscitoRecuperoCredenzialiDominio(GetFile)");
                } catch (IOException ex) {
                    this.logger.warn("can't write timestamp log: "+ex.getMessage());
                }
            } catch (CleverException ex) {
                try {
                    Timestamper.write("G20-fallitoRecuperoCredenzialiDominio(GetFile)");
                } catch (IOException ex1) {
                    this.logger.warn("can't write timestamp log: "+ex1.getMessage());
                }
                throw new CleverException ("Impossible to retrieve info to authenticate with the domain in which the file is stored. Get operation aborted. DB should not be in a consistent state. Exception caught: "+ex.getMessage());
            }
            if (login == null)
                throw new CleverException ("Impossible authenticate with the domain in which the file is stored. Get operation aborted. DB should not be in a consistent state.");
            commandParams.add(fileID);
            commandParams.add(login[0]); //user
            commandParams.add(login[1]); //password
            commandParams.add(String.valueOf(timeout)); //because it's the HadoopNamenodePlugin method, not the StorageRuler's one.
            federationParams.add(domain);
            federationParams.add(this.agentName);
            federationParams.add("getFile");
            federationParams.add(true); //even if it's false. The reply isn't used in any case
            federationParams.add(commandParams);
            federationParams.add(new Long(timeout));
            Object reply = null;
            try {
                try {
                    Timestamper.write("G20-inizioLancioComandoFederato(GetFile)");
                } catch (IOException ex) {
                    this.logger.warn("can't write timestamp log: "+ex.getMessage());
                }
                reply = this.owner.invoke("FederationListenerAgent", "forwardCommandToDomainWithTimeout", true, federationParams);
                try {
                    Timestamper.write("G21-riuscitoLancioComandoFederato(GetFile)");
                } catch (IOException ex) {
                    this.logger.warn("can't write timestamp log: "+ex.getMessage());
                }
            } catch (CleverException ex) {
                try {
                    Timestamper.write("G21B-fallitoLancioComandoFederato(GetFile)");
                } catch (IOException ex1) {
                    this.logger.warn("can't write timestamp log: "+ex1.getMessage());
                }
                throw new CleverException ("Error while retrieving file from domain "+domain+". DB COULD BE IN AN UNCONSISTEND STATE. Exception caught: "+ex.getMessage());
            }
            if (reply!=null && reply instanceof Exception) {
                throw new CleverException ("Error while retrieving file from domain "+domain+". DB COULD BE IN AN UNCONSISTEND STATE. Exception caught: "+((Exception)reply).getMessage());
            }
            fileBuffer = (String) reply;
        }
        return fileBuffer;
    }
    
    synchronized public void deleteFile (String filePath, String user, String password, long timeout) throws CleverException {
        try {
            if (!this.clientRuler.authenticate(user, password))
                throw new CleverException ("Authentication failed with user: "+user);
        } catch (CleverException ex) {
            throw new CleverException ("Error while trying to authenticate with Storage system: "+ex.getMessage());
        }
        ArrayList<String> owners;
        try {
            owners = this.getOwnerGroups(filePath, user, password);
        } catch (CleverException ex) {
            throw new CleverException ("Error while retrieving group of the user: "+ex.getMessage());
        }
        FileInfo fileInfo;
        try {
            fileInfo= this.getFileInfoFromOwners(filePath, owners);
        } catch (CleverException ex) {
            throw new CleverException ("Error while retrieving from DB the info of the file the user wants to delete: "+ex.getMessage());
        }
        if (fileInfo==null || owners.isEmpty())
            throw new CleverException ("File doesn't exist.");
        try {
            if (!this.hasAnyoneWriteRights(filePath, owners)) {
                throw new CleverException ("User is not allowed to delete this file. He have not write permissions on it.");
            }
        } catch (CleverException ex) {
            throw new CleverException ("Error while searching for a group of the user that has write permissions on the file: "+ex.getMessage());
        }
        String domain = fileInfo.getDomain();
        String fileID = fileInfo.getID();
        if (domain.compareTo(FileInfo.LOCAL_DOMAIN)==0) {
            try {
                this.ownerPlugin.deleteFileFromHDFS(fileID);
            } catch (IOException ex) {
                throw new CleverException ("Error while deleting file from HDFS. DB AND HDFS SHOULD BE IN UNCONSISTENT STATE. Exception caught: "+ex.getMessage());
            }
            try {
                this.deleteFileInfoFromDB(fileInfo);
            } catch (CleverException ex) {
                throw new CleverException ("Error while deleting file info from SednaDB. DB AND HDFS SHOULD BE IN UNCONSISTENT STATE Exception caught: "+ex.getMessage());
            }
        } else {
            ArrayList<Object> federationParams = new ArrayList<Object>();
            ArrayList<Object> commandParams = new ArrayList<Object>();
            String[] login;
            try {
                login = this.ownerPlugin.retrieveLoginInfo(domain);
            } catch (CleverException ex) {
                throw new CleverException ("Impossible to retrieve info to authenticate with the domain in which the file is stored. Get operation aborted. DB should not be in a consistent state. Exception caught: "+ex.getMessage());
            }
            if (login == null)
                throw new CleverException ("Impossible authenticate with the domain in which the file is stored. Get operation aborted. DB should not be in a consistent state.");
            commandParams.add(fileID);
            commandParams.add(login[0]); //user
            commandParams.add(login[1]); //password
            commandParams.add(String.valueOf(timeout)); //because it's the HadoopNamenodePlugin method, not the StorageRuler's one.
            federationParams.add(domain);
            federationParams.add(this.agentName);
            federationParams.add("deleteFile");
            federationParams.add(true); //even if it's false. The reply isn't used in any case
            federationParams.add(commandParams);
            federationParams.add(new Long(timeout));
            Object reply = null;
            try {
                reply = this.owner.invoke("FederationListenerAgent", "forwardCommandToDomainWithTimeout", true, federationParams);
            } catch (CleverException ex) {
                throw new CleverException ("Error while retrieving file from domain "+domain+". DB COULD BE IN AN UNCONSISTEND STATE. Exception caught: "+ex.getMessage());
            }
            try {
                this.deleteFileInfoFromDB(fileInfo);
            } catch (CleverException ex) {
                throw new CleverException ("Error while deleting file info from SednaDB. DB AND HDFS SHOULD BE IN UNCONSISTENT STATE Exception caught: "+ex.getMessage());
            }
            if (reply!=null && reply instanceof Exception) {
                throw new CleverException ("Error while retrieving file from domain "+domain+". DB COULD BE IN AN UNCONSISTEND STATE. Exception caught: "+((Exception)reply).getMessage());
            }
        }
    }
    
    synchronized public boolean existsFile (String filePath, String user, String password) throws CleverException {
        try {
            if (!this.clientRuler.authenticate(user, password))
                throw new CleverException ("Authentication failed with user: "+user);
        } catch (CleverException ex) {
            throw new CleverException ("Error while trying to authenticate to the Storage system");
        }
        ArrayList<String> owners;
        try {
            owners = this.getOwnerGroups(filePath, user, password);
        } catch (CleverException ex) {
            throw new CleverException ("Error while retrieving groups of the user that are also owner of the file: "+ex.getMessage());
        }
        if (owners.isEmpty())
            return false;
        else
            return true;
    }
    
    synchronized public void moveFile (String srcPath, String dstPath, String user, String password) throws CleverException {
        try {
            if (!this.clientRuler.authenticate(user, password))
                throw new CleverException ("Authentication failed with user: "+user);
        } catch (CleverException ex) {
            throw new CleverException ("Error while trying to authenticate to the Storage system");
        }
        ArrayList<String> srcOwners = null; //owners of src file
        try {
            srcOwners = this.getOwnerGroups(srcPath, user, password);
        } catch (CleverException ex) {
            throw new CleverException ("Error while retrieving groups of the user that are also owner of the source file: "+ex.getMessage());
        }
        if (srcOwners==null || srcOwners.isEmpty())
            throw new CleverException ("Source file doesn't exist");
        ArrayList<String> dstOwners = null; //owners of dst file
        try {
            dstOwners = this.getOwnerGroups(dstPath, user, password);
        } catch (CleverException ex) {
            throw new CleverException ("Error while retrieving groups of the user that are also owner of the destination file: "+ex.getMessage());
        }
        if (dstOwners!=null && !dstOwners.isEmpty())
            throw new CleverException ("The destination path is already occupated by another file. Delete it first.");
        FileInfo fileInfo = null;
        try {
            fileInfo = this.getFileInfoFromOwners(srcPath, srcOwners);
        } catch (CleverException ex) {
            throw new CleverException ("Error while retrieving info for source file from SednaDB: "+ex.getMessage());
        }
        ArrayList<String> writeSrcOwner = new ArrayList<String>(); //owners of src file with write permissions on it
        for (String o : srcOwners) {
            if (fileInfo.hasOwner(o) && (fileInfo.getRights(o)==FileInfo.PERMISSION_CODE.WRITE || fileInfo.getRights(o)==FileInfo.PERMISSION_CODE.READ_AND_WRITE)) {
                writeSrcOwner.add(o);
            }
        }
        if (writeSrcOwner.isEmpty())
            throw new CleverException ("The user has not write permission on the file, so he cannot change it's path.");
        //dst file doesn't exist, so are required write permission only on first file to change it's path.
        fileInfo.setPath(dstPath);
        try {
            this.updateFileInfo(srcPath, writeSrcOwner.get(0), fileInfo);
        } catch (CleverException ex) {
            throw new CleverException ("Error while changing file's path. DB AND HDFS COULD BE IN INCONSISTENT STATE.");
        }
    }
    
    synchronized public HashMap<String, FileInfo.PERMISSION_CODE> getAllFilePermissions (String filePath, String user, String password) throws CleverException {
        if (!this.clientRuler.authenticate(user, password))
            throw new CleverException ("Authentication failed with user: "+user);
        ArrayList<String> owners = this.getOwnerGroups(filePath, user, password);
        FileInfo file = this.getFileInfoFromOwners(filePath, owners);
        if (file==null)
            throw new CleverException ("File not found");
        if (!file.isCreator(this.clientRuler.getFirstGroupID(user, password)))
            throw new CleverException ("The user is not allowed to know permission on this file. Only the file's creator can to it.");
        return file.getRights();
    }
    
    synchronized public HashMap<String, FileInfo.PERMISSION_CODE> getMyPermissionOnFile (String filePath, String user, String password) throws CleverException {
        if (!this.clientRuler.authenticate(user, password))
            throw new CleverException ("Authentication failed with user: "+user);
        ArrayList<String> owners = this.getOwnerGroups(filePath, user, password);
        FileInfo file = this.getFileInfoFromOwners(filePath, owners);
        if (file==null)
            throw new CleverException ("File not found");
        HashMap<String, FileInfo.PERMISSION_CODE> result = new HashMap<String, FileInfo.PERMISSION_CODE>();
        for (String ownerID : owners) {
            if (file.hasOwner(ownerID))
                result.put(ownerID, file.getRights(ownerID));
        }
        return result;
    }
    
    synchronized public void changePermissionOnFile (String filePath, String owner, FileInfo.PERMISSION_CODE rights, String user, String password) throws CleverException {
        if (!this.clientRuler.authenticate(user, password))
            throw new CleverException ("Authentication failed with user: "+user);
        ArrayList<String> owners = this.getOwnerGroups(filePath, user, password);
        FileInfo file = this.getFileInfoFromOwners(filePath, owners);
        if (file==null)
            throw new CleverException ("File not found");
        if (!file.isCreator(this.clientRuler.getFirstGroupID(user, password)))
            throw new CleverException ("The user is not allowed to change permission on this file. Only the file's creator can to it.");
        if (!file.hasOwner(owner))
            throw new CleverException ("The specified group is not owner of this file");
        if (file.getCreatorID().compareTo(owner)==0)
            throw new CleverException ("The creator has the highest rights on the file and cannot change them.");
        this.modifyOwnerRights(file, owner, rights);
    }

    synchronized public void addOwnerToFile (String filePath, String ownerID, FileInfo.PERMISSION_CODE rights, String user, String password) throws CleverException {
        if (!this.clientRuler.authenticate(user, password))
            throw new CleverException ("Authentication failed with user: "+user);
        ArrayList<String> owners = this.getOwnerGroups(filePath, user, password);
        FileInfo file = this.getFileInfoFromOwners(filePath, owners);
        this.logger.debug("in public method: fileInfo = "+file);
        if (file==null)
            throw new CleverException ("File not found");
        this.logger.debug("in public method: fileXML = "+file.toXML());
        this.logger.debug("in public method: firstGroupID = "+this.clientRuler.getFirstGroupID(user, password));
        if (!file.isCreator(this.clientRuler.getFirstGroupID(user, password)))
            throw new CleverException ("The user is not allowed to change sharing properties of this file. Only the file's creator can to it.");
        this.addOwnerToFile(file, ownerID, rights);        
    }
    
    synchronized public void removeOwnerFromFile (String filePath, String ownerID, String user, String password) throws CleverException {
        if (!this.clientRuler.authenticate(user, password))
            throw new CleverException ("Authentication failed with user: "+user);
        ArrayList<String> owners = this.getOwnerGroups(filePath, user, password);
        FileInfo file = this.getFileInfoFromOwners(filePath, owners);
        if (file==null)
            throw new CleverException ("File not found");
        if (!file.isCreator(this.clientRuler.getFirstGroupID(user, password)))
            throw new CleverException ("The user is not allowed to change sharing properties of this file. Only the file's creator can to it.");
        if (file.getCreatorID().compareTo(ownerID)==0)
            throw new CleverException ("The creator group cannot be removed from file's owners.");        
        this.removeOwnerFromFile(file, ownerID);        
    }
    
    /*
    synchronized public void removeClientFromFileOwners (String filePath, String client, String user, String password) throws CleverException {
        if (!this.clientRuler.authenticate(user, password))
            throw new CleverException ("Authentication failed with user: "+user);
        ArrayList<String> owners = this.getOwnerGroups(filePath, user, password);
        FileInfo file = this.getFileInfoFromOwners(filePath, owners);
        if (file==null)
            throw new CleverException ("File not found");
        if (!file.isCreator(this.clientRuler.getFirstGroupID(user, password)))
            throw new CleverException ("The user is not allowed to change sharing properties of this file. Only the file's creator can to it.");
        if (client.compareTo(user)==0) //a questo punto user è il creatore del file
            throw new CleverException ("The user is the creator of the file but he's not allowed to remove all of its groups from the owners of this file.");
        ArrayList<String[]> clientGroups = this.clientRuler.getClientGroups(client);
        for (String[] group : clientGroups)
            file.removeOwner(group[0]);
        this.updateFileInfoInDB(file);
    }*/
    
    /* synchronized private String getCreatorID (String path, String ownerID) throws CleverException {
        FileInfo fileInfo = this.retrieveFileInfoFromDB(path, ownerID);
        return fileInfo.getCreatorID();
    } */
    
    synchronized private FileInfo getFileInfoFromOwners (String path, ArrayList<String> owners) throws CleverException {
        for (String o : owners) {
            FileInfo file = this.retrieveFileInfoFromDB(path, o);
            if (file!=null) {
                logger.debug("file found"+file.toXML());
                return file;
            }
        }
        this.logger.debug("Returned NULL");
        return null;
    }
    
    synchronized private ArrayList<String> getOwnerGroups (String path, String user, String password) throws CleverException {
        ArrayList<String[]> groups = this.clientRuler.getMyGroups(user, password);
        ArrayList<String> ownersFound = new ArrayList<String>();
        for (String[] group : groups)
            if (this.existsFileInfoInDB(path, group[0])) {
                ownersFound.add(group[0]);
            }
        return ownersFound;
    }
    
    synchronized private boolean hasAnyoneReadRights (String path, ArrayList<String> owners) throws CleverException {
        for (String o : owners) {
            FileInfo.PERMISSION_CODE rights = this.getOwnerRights(path, o);
            if (rights==FileInfo.PERMISSION_CODE.READ || rights==FileInfo.PERMISSION_CODE.READ_AND_WRITE)
                return true;
        }
        return false;
    }
    
    synchronized private boolean hasAnyoneWriteRights (String path, ArrayList<String> owners) throws CleverException {
        for (String o : owners) {
            FileInfo.PERMISSION_CODE rights = this.getOwnerRights(path, o);
            if (rights==FileInfo.PERMISSION_CODE.WRITE || rights==FileInfo.PERMISSION_CODE.READ_AND_WRITE)
                return true;
        }
        return false;
    }
    
    synchronized private String retrieveFileInfoSednaXmlByPath (String filePath) throws CleverException {
        String location = "/"+this.storageNodeName+"/file[@path='"+filePath+"']";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.agentName);
        params.add(location);
        String result = (String) this.owner.invoke("DatabaseManagerAgent", "getContentNodeObject", true, params);
        if (result.isEmpty())
            return null;
        else
            return result;
    }
    
    /* synchronized private String retrieveFileInfoSednaXmlByID (String fileID) throws CleverException {
        String location = "/"+this.storageNodeName+"/file[@id='"+fileID+"']";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.agentName);
        params.add(location);
        String result = (String) this.owner.invoke("DatabaseManagerAgent", "getContentNodeObject", true, params);
        if (result.isEmpty())
            return null;
        else
            return result;
    } */
    
    synchronized private String retrieveFileInfoXMLFromDB (String filePath, String ownerID) throws CleverException {
        this.logger.debug("filePath: "+filePath+"   -   ownerID: "+ownerID);
        String xml = this.retrieveFileInfoSednaXmlByPath (filePath);
        this.logger.debug("sednaXML: "+xml);
        if (xml==null || xml.isEmpty())
            return "";
        xml = SednaUtil.decodeSednaXml(xml);
        this.logger.debug("xml: "+xml);
        String[] p = xml.split("<path>");
        String loggerString = "Dopo split con <path>:";
        for (int i=0; i<p.length; i++) {
            loggerString += "\nAll'indice "+i+": \n"+p[i];
        }
        logger.debug(loggerString);
        String fileXML = "";
        for (String s : p) {
            if (s==null || s.isEmpty())
                continue;
            this.logger.debug("s = "+s);
            String token = "<group id=\""+ownerID+"\"";
            this.logger.debug("token: "+token);
            if (s.contains(token.subSequence(0, token.length()-1))) {
                this.logger.debug("Trovato");
                String arr[] = s.split("<id>",2); //it could be absent if the first split operation was made with <id>
                String s1 = arr[arr.length-1];
                arr = s1.split("</id>", 2);
                String id = arr[0];
                fileXML = "<file path=\""+filePath+"\" id=\""+id+"\"><path>"+s+"</file>";
                this.logger.debug("fileXML: "+fileXML);
                break;
            }
        }
        this.logger.debug("fileXML ritornato: "+fileXML);
        return fileXML;
    }
    
    /* synchronized private String retrieveFileInfoXMLFromDB (String fileID) throws CleverException {
        String xml = this.retrieveFileInfoSednaXmlByPath (fileID);
        if (xml==null || xml.isEmpty())
            return "";
        xml = SednaUtil.decodeSednaXml(xml);
        String p[] = xml.split("<path>", 2);
        String s1 = p[p.length-1];
        p = s1.split("</path>",2);
        String path = p[0];
        String fileXML = "<file path=\""+path+"\" id=\""+fileID+"\"><path>"+xml+"</file>";
        return fileXML;
    } */
    
    synchronized private FileInfo retrieveFileInfoFromDB (String filePath, String ownerID) throws CleverException {
        String fileXML = this.retrieveFileInfoXMLFromDB(filePath, ownerID);
        if (fileXML==null || fileXML.isEmpty())
            return null;
        else
            return new FileInfo(fileXML);
    }
    
    /* synchronized private FileInfo retrieveFileInfoFromDB (String fileID) throws CleverException {
        String fileXML = this.retrieveFileInfoXMLFromDB(fileID);
        if (fileXML==null || fileXML.isEmpty())
            return null;
        else
            return new FileInfo(fileXML);
    } */
    
    synchronized private boolean existsFileInfoInDB (String filePath, String ownerID) throws CleverException {
        String location = "/"+this.storageNodeName+"/"+"file[@path='"+filePath+"']/owners/group[@id='"+ownerID+"']";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.agentName);
        params.add(location);
        return (Boolean)this.owner.invoke("DatabaseManagerAgent", "existNode", true, params);
    }
    
    synchronized private boolean existsFileInfoInDB (String fileID) throws CleverException {
        String location = "/"+this.storageNodeName+"/file[@id='"+fileID+"']";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.agentName);
        params.add(location);
        return (Boolean)this.owner.invoke("DatabaseManagerAgent", "existNode", true, params);
    }
    
    synchronized private boolean existsFileInfo (FileInfo fileInfo) throws CleverException {
        return this.existsFileInfoInDB(fileInfo.getID());
    }
    
    synchronized private boolean existsFileInfoWithSameName (FileInfo fileInfo) throws CleverException {
        //Use a random owner because no group can have other files having the same path,
        //so a random owner identifies the same file the other owners identify.
        return this.existsFileInfoInDB(fileInfo.getPath(), fileInfo.getFirstOwner());
    }
    
    synchronized private void addFileInfoInDB (FileInfo fileInfo) throws CleverException {
        if (this.existsFileInfoWithSameName(fileInfo))
            throw new CleverException ("Impossible to add the file info in the DB. A file info with the same name already exists.");
        else {
            String node = fileInfo.toXML();
            String location = "/"+this.storageNodeName;
            ArrayList<Object> params = new ArrayList<Object>();
            params.add(this.agentName);
            params.add(node);
            params.add("into");
            params.add(location);
            this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
        }
    }
    
    synchronized private void createFileInfo (String path, String fileID, String locationDomain, String creatorID) throws CleverException {
        FileInfo fileInfo = new FileInfo (path, fileID, locationDomain, creatorID);
        this.addFileInfoInDB(fileInfo);
    }
    
    synchronized private String generateFileID() throws CleverException {
        String fileID;
        do {
            UUID uid = UUID.randomUUID();
            fileID = Long.toString(uid.getMostSignificantBits()) + Long.toString(uid.getLeastSignificantBits());
        } while (this.existsFileInfoInDB(fileID));
        return fileID;
    }
    
    synchronized private void deleteFileInfo (FileInfo fileInfo) throws CleverException {
        fileInfo = this.retrieveFileInfoFromDB(fileInfo.getPath(), fileInfo.getFirstOwner());
        this.deleteFileInfoFromDB(fileInfo);
    }
    
    synchronized private void updateFileInfo (String path, String owner, FileInfo newFile) throws CleverException {
        FileInfo oldFile = this.retrieveFileInfoFromDB(path, owner);
        if (oldFile!=null)
            this.deleteFileInfo(oldFile);
        this.addFileInfoInDB(newFile);
    }
    
    synchronized private void addOwnerToFile (FileInfo fileInfo, String ownerID, FileInfo.PERMISSION_CODE rights) throws CleverException {
        if (!this.existsFileInfoWithSameName(fileInfo))
            throw new CleverException ("File doesn't exist");
        else {
            //To be sure: the file could have a different ID (and so a different version).
            //So first the FileInfo is updated from DB.
            fileInfo = this.retrieveFileInfoFromDB(fileInfo.getPath(), fileInfo.getFirstOwner());
            if (fileInfo.hasOwner(ownerID))
                throw new CleverException ("Cannot add an already existing owner");
            fileInfo.addOwner(ownerID, rights);
            this.updateFileInfoInDB(fileInfo);
        }
    }
    
    synchronized private void modifyOwnerRights (FileInfo fileInfo, String ownerID, FileInfo.PERMISSION_CODE rights) throws CleverException {
        if (!this.existsFileInfoWithSameName(fileInfo))
            throw new CleverException ("File doesn't exist");
        else {
            //To be sure: the file could have a different ID (and so a different version).
            //So first the FileInfo is updated from DB.
            fileInfo = this.retrieveFileInfoFromDB(fileInfo.getPath(), fileInfo.getFirstOwner());
            if (!fileInfo.hasOwner(ownerID))
                throw new CleverException ("The file does not belong to the specified owner.");
            if (fileInfo.isCreator(ownerID))
                throw new CleverException ("Creator rights are the highest one and cannot be changed.");
            fileInfo.removeOwner(ownerID);
            fileInfo.addOwner(ownerID, rights);
            this.updateFileInfoInDB(fileInfo);
        }
    }
    
    synchronized private FileInfo.PERMISSION_CODE getOwnerRights (String path, String ownerID) throws CleverException {
        FileInfo fileInfo = this.retrieveFileInfoFromDB(path, ownerID);
        if (fileInfo==null)
            throw new CleverException ("File doesn't exist.");
        return fileInfo.getRights(ownerID);
    }
    
    synchronized private void removeOwnerFromFile (FileInfo fileInfo, String ownerID) throws CleverException {
        if (!this.existsFileInfoWithSameName(fileInfo))
            throw new CleverException ("File doesn't exist");
        else {
            if (fileInfo.isCreator(ownerID))
                throw new CleverException ("Cannot remove the creator of the file from the owner list");
            //To be sure: the file could have a different ID (and so a different version).
            //So first the FileInfo is updated from DB.
            fileInfo = this.retrieveFileInfoFromDB(fileInfo.getPath(), fileInfo.getFirstOwner());
            if (!fileInfo.hasOwner(ownerID))
                throw new CleverException ("The file does not belong to the specified owner.");
            if (fileInfo.isCreator(ownerID))
                throw new CleverException ("Creator rights are the highest one and cannot be changed.");
            fileInfo.removeOwner(ownerID);
            this.updateFileInfoInDB(fileInfo);
        }
    }
    
    synchronized private void deleteFileInfoFromDB (FileInfo fileInfo) throws CleverException {
        if (!this.existsFileInfo(fileInfo))
            return;
        ArrayList<Object> params = new ArrayList<Object>();
        String location = "/"+this.storageNodeName+"/file[@path='"+fileInfo.getPath()+"']";
        params.add(this.agentName);
        params.add(location);
        this.owner.invoke("DatabaseManagerAgent", "deleteNode", true, params);
    }
    
    synchronized public ArrayList<String> listMyFiles (String user, String password) throws CleverException {
        if (!this.clientRuler.authenticate(user, password))
            throw new CleverException ("Authentication failed with user: "+user);
        String storageXML = this.getStorageNode();
        ArrayList<String>files = new ArrayList<String>();
        ArrayList<String[]>groups = this.clientRuler.getMyGroups(user, password);
        String filesXML[] = storageXML.split("<file");
        for (String fileXML : filesXML) {
            if (fileXML==null || fileXML.isEmpty())
                continue;
            for (String[] group : groups) {
                String token = "<group id=\""+group[0]+"\"";
                if (fileXML.contains(token)) {
                    String[] p = fileXML.split("<path>", 2);
                    String s = p[1];
                    p = s.split("</path>", 2);
                    String path = p[0];
                    files.add(path);
                    break;
                }
            }
        }
        
        
        
        /* String filesXML[] = storageXML.split("<file");
        for (String fileXML : filesXML) {
            if (fileXML==null || fileXML.isEmpty())
                continue;
            String s = fileXML;
            String p[] = fileXML.split("<owners>", 2);
            if (p.length < 2)
                continue;
            s = p[1];
            p = s.split("</owners>", 2);
            if (p.length < 2)
                continue;
            s = p[0];
            String owners[] = s.split("<group");
            boolean toAdd = false;
            for (String ownerXML : owners) {
                if (ownerXML==null || ownerXML.isEmpty())
                    continue;
                String p1[] = ownerXML.split("\"",3);
                if (p1.length<3)
                    continue;
                String owner = p1[1];
                for (String[] group : groups) {
                    if (group[0].compareTo(owner)==0) {
                        toAdd = true;
                        break;
                    }
                }
                if (toAdd)
                    break;
            }
            if (toAdd) {
                s = fileXML;
                String p2[] = s.split("<path>",2);
                s = p2[1];
                p2 = s.split("</path>", 2);
                String path = p[0];
                files.add(path);
            }
        } */
        return files;
    }
    
    synchronized private void updateFileInfoInDB (FileInfo fileInfo) throws CleverException {
        if (this.existsFileInfoWithSameName(fileInfo)) {
            this.deleteFileInfoFromDB(fileInfo);
            this.addFileInfoInDB(fileInfo);
        } else {
            this.addFileInfoInDB(fileInfo);
        }
    }
    
    //JUST FOR TEST
    synchronized public String getStorageNode() throws CleverException {
        String xml;
        String location = "/"+this.storageNodeName;
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.agentName);
        params.add(location);
        xml = (String) this.owner.invoke("DatabaseManagerAgent", "getContentNodeObject", true, params);
        xml = SednaUtil.decodeSednaXml(xml);
        xml = "<"+this.storageNodeName+">\n"+xml+"\n</"+this.storageNodeName+">";
        return xml;
    }
    
    //JUST FOR TEST
    /* synchronized public String test() {
        String result = "Lanciato test di StorageRuler()";
            FileInfo fileInfo1 = new FileInfo("file1", "ID1", "dominioA", "gruppo100");
            fileInfo1.addOwner("gruppo1", FileInfo.PERMISSION_CODE.READ);
            FileInfo fileInfo2 = new FileInfo("file1", "ID2", "dominioA", "gruppo100");
            fileInfo2.addOwner("gruppo2", FileInfo.PERMISSION_CODE.WRITE);
            this.addFileInfoInDB(fileInfo1);
            result += "\nAggiunto file 1:\n"+fileInfo1.toXML()+"\n";
            this.addFileInfoInDB(fileInfo2);  
            result += "\nAggiunto file 2:\n"+fileInfo2.toXML()+"\n";
            String retrieved = this.retrieveFileInfoSednaXmlByPath("file1");
            result += "\n risultato di retrieveFileInfoSednaXMLFromDB(\"file1\") :\n"+retrieved+"\n";
            retrieved = this.retrieveFileInfoXMLFromDB("file1", "gruppo1");
            result += "\n risultato di retrieveFileInfoXMLFromDB(\"file1\", \"gruppo1\") :\n"+retrieved+"\n";
            retrieved = this.retrieveFileInfoXMLFromDB("file1", "gruppo2");
            result += "\n risultato di retrieveFileInfoXMLFromDB(\"file1\", \"gruppo2\") :\n"+retrieved+"\n";
            retrieved = this.retrieveFileInfoXMLFromDB("file2", "gruppo1");
            result += "\n risultato di retrieveFileInfoXMLFromDB(\"file2\", \"gruppo1\") :\n"+retrieved+"\n";
            retrieved = this.retrieveFileInfoXMLFromDB("file1", "gruppo3");
            result += "\n risultato di retrieveFileInfoXMLFromDB(\"file1\", \"gruppo3\") :\n"+retrieved+"\n";
            boolean found = this.existsFileInfoInDB("file1", "gruppo1");
            result += "\n risultato di existsFileInfoInDB(\"file1\", \"gruppo1\") :\n"+found+"\n";
            found = this.existsFileInfoInDB("file1", "gruppo2");
            result += "\n risultato di existsFileInfoInDB(\"file1\", \"gruppo2\") :\n"+found+"\n";
            found = this.existsFileInfoInDB("file1", "gruppo3");
            result += "\n risultato di existsFileInfoInDB(\"file1\", \"gruppo3\") :\n"+found+"\n";
            found = this.existsFileInfoInDB("file2", "gruppo1");
            result += "\n risultato di existsFileInfoInDB(\"file2\", \"gruppo1\") :\n"+found+"\n";
            result += "\n contenuto dello storageNode:\n"+this.getStorageNode();
            found = this.existsFileInfoInDB("ID1");
            result += "\n risultato di existsFileInfoInDB(\"ID1\") :\n"+found+"\n";
            found = this.existsFileInfoInDB("ID2");
            result += "\n risultato di existsFileInfoInDB(\"ID2\") :\n"+found+"\n";
            found = this.existsFileInfoInDB("ID3");
            result += "\n risultato di existsFileInfoInDB(\"ID3\") :\n"+found+"\n";
    } */
    
}
