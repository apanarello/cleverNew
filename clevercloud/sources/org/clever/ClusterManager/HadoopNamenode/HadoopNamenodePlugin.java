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
package org.clever.ClusterManager.HadoopNamenode;



import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Communicator.ModuleCommunicator;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.Exceptions.HDFSConnectionException;
import org.clever.Common.Exceptions.HDFSInternalException;
import org.clever.Common.StorageRuler.FileInfo;

/**
 * @author Giovanni Volpintesta
 * @author Mariacristina Sinagra
 */
public interface HadoopNamenodePlugin {
    
    //Mariacristina Sinagra's commands
    
    public void setModuleCommunicator(ModuleCommunicator mc);
     
    public ModuleCommunicator getModuleCommunicator();
     
    public String networkIp();
     
    public boolean existsHost(String address, String name) throws Exception;
     
    public void setHosts(String address, String name) throws Exception;
    
    public void updateHosts(String address, String name) throws Exception;
    
    public void setSlaves(String username, String name) throws Exception;
     
    public boolean checkHadoopAgent() throws CleverException;
     
    public void initHadoopAgent() throws CleverException;
    
    public void InsertItemIntoHadoopNode(String hostname, String address, String username) throws CleverException;
    //public void prova(String stringa) throws CleverException;
   // public String submitJob(String input, String output) throws Exception;
    public String submitJob (String fileBuffer, String jobName,String bucket,String fileS3Name, Long startByte,Long endByte,Byte p) throws CleverException;;
    public void setOwner(Agent owner);
        
    //Giovanni Volpintesta's commands
    
    //Usage oh Hadoop client (DON'T CREATE ANY COMMAND FOR SHELL AMMINISTRATION)
    
    public void init();
    
    public String[] retrieveLoginInfo (String domain) throws CleverException;
    
    public void putFileInHDFS (String file, String dstDbPath) throws HDFSConnectionException, HDFSInternalException;
    
    public String getFileFromHDFS (String srcDbPath) throws HDFSConnectionException, HDFSInternalException, IOException;

    public void deleteFileFromHDFS (String dbPath) throws HDFSConnectionException, HDFSInternalException;


    //Gestione gruppi
    
    public boolean authenticate(String user, String password) throws CleverException; //command done
    
    public String registerClient (String client, String type, String password) throws CleverException; //command done
    
    public void addClientToGroup (String client, String group, String user, String password) throws CleverException; //command done
    
    public void removeClientFromGroup (String client, String group, String user, String password) throws CleverException; //command done
    
    public boolean isPartOfGroup (String client, String group, String user, String password) throws CleverException; //command done
    
    public String createGroup (String description, String user, String password) throws CleverException; //command done
    
    public void deleteGroup (String group, String user, String password) throws CleverException; //command done
    
    public ArrayList<String[]> getMyGroups (String user, String password) throws CleverException; //command done
    
    public ArrayList<String> getGroupMembers (String group, String user, String password) throws CleverException; //command done
    
    public String getGroupCreator (String group, String user, String password) throws CleverException; //command done

    public String readClientRulerNodes () throws CleverException; //TEST //command done

    //Gestione file
    
    public void changePermissionOnFile (String path, String ownerGroupID, String rights, String user, String password) throws CleverException; //command done
    
    public HashMap<String, FileInfo.PERMISSION_CODE> getAllRightsOnFile (String path, String user, String password) throws CleverException; //command done
    
    public HashMap<String, FileInfo.PERMISSION_CODE> getMyRightsOnFile (String path, String user, String password) throws CleverException; //command done
    
    public void addOwnerGroupOnFile (String path, String ownerGroupID, String rights, String user, String password) throws CleverException; //command done
    
    public void removeOwnerGroupFromFile (String path, String ownerGroupID, String user, String password) throws CleverException; //command done

    public String readStorageRulerNodes () throws CleverException; //TEST //command done

   // public void putFile (String fileBuffer, String dstDbPath, String user, String password, Boolean forwardable) throws CleverException;
    
    public void putFile (String fileBuffer, String dstDbPath, String user, String password, String timeout, Boolean forwardable) throws CleverException;
    
   // public String getFile (String srcDbPath, String user, String password) throws CleverException;
    
    public String getFile (String srcDbPath, String user, String password, String timeout) throws CleverException;
    
    public boolean existsFile (String srcDbPath, String user, String password) throws CleverException;
    
    public void moveFile (String srcDbPath, String dstDbPath, String user, String password) throws CleverException;
    
   // public void deleteFile (String srcDbPath, String user, String password) throws CleverException;

    public void deleteFile (String srcDbPath, String user, String password, String timeout) throws CleverException;

    public ArrayList<String> listMyFiles (String user, String password) throws CleverException;
    
    //apanarello
    
    /**
     *
     * @param fileBuffer
     * @param jobName
     * @param files3
     * @param bucket
     * @param user
     * @param pass
     * @param forwardable
     * @param domRes
     * @throws CleverException
     */
    public void sendJob (String fileBuffer, String jobName, String bucket , String files3 ,String user, String pass, Boolean forwardable, String domRes[][]) throws CleverException;

    /**
     *
     * @param fileBuffer
     * @param jobName
     * @param files3Name
     * @param bucket
     * @param startByte
     * @param endByte
     * @param p
     * @throws CleverException
     */
    public String sendJob (String fileBuffer, String jobName,String bucket,String files3Name,  Long startByte, Long endByte, Byte p) throws CleverException;

}
