/*
 * The MIT License
 *
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

import org.clever.Common.StorageRuler.Client;
import org.clever.Common.StorageRuler.Group;
import java.util.ArrayList;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Exceptions.CleverException;

/**
 *
 * @author Giovanni Volpintesta
 */
public class ClientRuler {
    
    private CmAgent owner;
    
    public final String agentName = "HadoopNamenodeAgent";
    public final String groupsNodeName = "StorageGroups";
    public final String clientsNodeName = "StorageClients";
    public final static String REGISTRATION_GROUP_DESCRIPTION = "Created with the client's registration";
    public final String registrationGroupDescription = ClientRuler.REGISTRATION_GROUP_DESCRIPTION;
    
    private Logger logger; //per debug
    
    
    public ClientRuler () {}
    
    public void init(CmAgent owner, Logger logger) {
        this.owner = owner;
        this.logger = logger;
        try {
            logger.info("Initializing StorageRuler");
            ArrayList<Object> params = new ArrayList<Object>();
            //Check if the clients node exists
            params.add(this.agentName);
            params.add("/"+this.clientsNodeName);
            boolean exist;
            exist = ((Boolean)this.owner.invoke("DatabaseManagerAgent", "existNode", true, params)).booleanValue();
            if (exist) {
                logger.info("Clients node already exists.");
            } else {
                logger.info("Clients node doesn't exist in sednaDB. Creating clients' node");
                params.clear();
                params.add(this.agentName);
                params.add("<"+this.clientsNodeName+" />");
                params.add("into");
                params.add("");
                try {
                    this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
                } catch (CleverException ex) {
                    logger.error("Error inserting Clients node in DB");
                }
                logger.info("Clients node created.");
            }
            //Check if the groups node exists
            params.clear();
            params.add(this.agentName);
            params.add("/"+this.groupsNodeName);
            exist = ((Boolean)this.owner.invoke("DatabaseManagerAgent", "existNode", true, params)).booleanValue();
            if (exist) {
                logger.info("Groups node already exists.");
            } else {
                logger.info("Groups node doesn't exist in sednaDB. Creating groups' node");
                params.clear();
                params.add(this.agentName);
                params.add("<"+this.groupsNodeName+" />");
                params.add("into");
                params.add("");
                try {
                    this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
                } catch (CleverException ex) {
                    logger.error("Error inserting Groups node in DB");
                }
                logger.info("Groups node created.");
            }
        } catch (CleverException ex) {
            logger.error("Error checking if StorageGroups and StorageClients node exists in DB or creating them");
        }
    }
    
    synchronized public String registerClient (String clientName, String TYPE, String password) throws CleverException {
        if (this.existsClientInDB(clientName))
            throw new CleverException ("Impossible to register a client with name "+clientName+"because a client with this name already exists.");
        Client client = new Client (clientName, Client.CLIENT_TYPE.valueOf(TYPE), password);
        String groupID = this.generateGroupID();
        String note = this.registrationGroupDescription;
        Group group = new Group(groupID, note, clientName);
        this.addClientInDB(client);
        this.addGroupInDB(group);
        this.addClientToGroup(groupID, clientName);
        return group.getID();
    }
    
    synchronized public boolean authenticate (String clientName, String password) throws CleverException {
        Client client = this.retrieveClientFromDB(clientName);
        if (client!=null && client.getPassword().compareTo(password)==0) {
            this.logger.debug("authenticated.");
            return true;
        }
        else {
            this.logger.debug("not authenticated.");
            return false;
        }
    }
    
    synchronized private boolean existsClientInDB (String client) throws CleverException {
        String location = "/"+this.clientsNodeName+"/"+"client[@name='"+client+"']";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.agentName);
        params.add(location);
        return (Boolean)this.owner.invoke("DatabaseManagerAgent", "existNode", true, params);
    }
    
    synchronized private boolean existsClientInDB (Client client) throws CleverException {
        return this.existsClientInDB(client.getName());
    }
    
    synchronized private boolean existsGroupInDB (Group group) throws CleverException {
        return this.existsGroupInDB(group.getID());
    }
    
    synchronized private boolean existsGroupInDB (String group) throws CleverException {
        String location = "/"+this.groupsNodeName+"/"+"group[@id='"+group+"']";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.agentName);
        params.add(location);
        return (Boolean)this.owner.invoke("DatabaseManagerAgent", "existNode", true, params);
    }
    
    synchronized private void addClientInDB (Client client) throws CleverException {
        if (this.existsClientInDB(client))
            throw new CleverException ("Impossible to add the client in the DB. A client with the same name already exists.");
        else {
            String node = client.toXML();
            String location = "/"+this.clientsNodeName;
            ArrayList<Object> params = new ArrayList<Object>();
            params.add(this.agentName);
            params.add(node);
            params.add("into");
            params.add(location);
            this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
        }
    }
    
    synchronized private void updateClientInDB (Client client) throws CleverException {
        if (!this.existsClientInDB(client))
            this.addClientInDB(client);
        else {
            //remove it first:
            ArrayList<Object> params = new ArrayList<Object>();
            String location = "/"+this.clientsNodeName+"/client[@name='"+client.getName()+"']";
            params.add(this.agentName);
            params.add(location);
            this.owner.invoke("DatabaseManagerAgent", "deleteNode", true, params);
            this.addClientInDB(client);
        }
    }
    
    synchronized private String generateGroupID () throws CleverException {
        String groupID;
        do {
            int id = UUID.randomUUID().hashCode();
            if (id < 0)
                id = (-1) * id; //the ID for the group can't be negative because in administration shell an argument cannot begin with "-"
            groupID = Integer.toString(id);
        } while (this.existsGroupInDB(groupID));
        //in this way a different ID is created until it doesn't exist in DB.
        return groupID;
    }
    
    synchronized private void addGroupInDB (Group group) throws CleverException {
        if (this.existsClientInDB(group.getID()))
            throw new CleverException ("Impossible to add the group in the DB. A group with the same ID already exists.");
        else {
            String node = group.toXML();
            String location = "/"+this.groupsNodeName;
            ArrayList<Object> params = new ArrayList<Object>();
            params.add(this.agentName);
            params.add(node);
            params.add("into");
            params.add(location);
            this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
        }
    }
    
    synchronized private void updateGroupInDB (Group group) throws CleverException {
        if (!this.existsGroupInDB(group))
            this.addGroupInDB(group);
        else {
            this.deleteGroupFromDB(group);
            this.addGroupInDB(group);
        }
    }
    
    synchronized private String retrieveClientXMLFromDB (String clientName) throws CleverException {
        String location = "/"+this.clientsNodeName+"/client[@name='"+clientName+"']";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.agentName);
        params.add(location);
        String result = (String) this.owner.invoke("DatabaseManagerAgent", "getContentNodeObject", true, params);
        if (result.isEmpty())
            return null;
        else
            return result;
    }
    
    synchronized private Client retrieveClientFromDB (String clientName) throws CleverException {
        Client result;
        String xml = this.retrieveClientXMLFromDB(clientName);
        if (xml==null || xml.isEmpty())
            return null;
        xml = SednaUtil.decodeSednaXml(xml);
        xml = "<client name=\""+clientName+"\">"
                + xml
                + "</client>";
        this.logger.debug(xml);
        result = new Client(xml, this.logger);
        return result;
    }
    
    synchronized private String retrieveGroupXMLFromDB (String groupID) throws CleverException {
        String location = "/"+this.groupsNodeName+"/group[@id='"+groupID+"']";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.agentName);
        params.add(location);
        String result = (String) this.owner.invoke("DatabaseManagerAgent", "getContentNodeObject", true, params);
        if (result.isEmpty())
            return null;
        else
            return result;
    }
    
    synchronized private Group retrieveGroupFromDB (String groupID) throws CleverException {
        Group result;
        String xml = this.retrieveGroupXMLFromDB(groupID);
        if (xml==null || xml.isEmpty())
            return null;
        xml = SednaUtil.decodeSednaXml(xml);
        xml = "<group id=\""+groupID+"\">"
                + xml
                + "</group>";
        result = new Group(xml);
        return result;
    }
    
    synchronized private void addClientToGroup (String groupID, String clientName) throws CleverException {
        this.logger.debug("Entrato in addClientToGroup");
        if (!this.existsClientInDB(clientName))
            throw new CleverException("Client doesn't exist in DB.");
        if (!this.existsGroupInDB(groupID))
            throw new CleverException("Group doesn't exist in DB.");
        Client client = this.retrieveClientFromDB(clientName);
        this.logger.debug("Client = "+client);
        Group group = this.retrieveGroupFromDB(groupID);
        this.logger.debug("Group = "+group);
        if (!client.isPartOfGroup(group.getID()))
            client.addGroup(group.getID(), group.getNote());
        this.logger.debug("Group added to client object");
        if (!group.containsClient(client.getName()) && group.getCreator().compareTo(client.getName())!=0)
            group.addClient(clientName);
        this.logger.debug("Client added to group object");
        this.updateClientInDB(client);
        this.logger.debug("Client updated in DB");
        this.updateGroupInDB(group);
        this.logger.debug("Group updated in DB");
    }
    
    synchronized public void addClientToGroup (String clientName, String groupID, String user, String password) throws CleverException {
        if (this.authenticate(user, password)) {
            if (this.isCreatorOfGroup(user, groupID)) {
                this.addClientToGroup(groupID, clientName);
            } else
                throw new CleverException ("The user isn't the creator of the group. The group can be modified only by its creator.");
        } else
            throw new CleverException ("Authentication Failed with user: "+user);
    }
    
    synchronized public String createGroup (String description, String user, String password) throws CleverException {
        this.logger.debug("Entrato in createGroup");
        boolean authenticated = this.authenticate(user, password);
        this.logger.debug("authenticated = "+authenticated);
        if (authenticated) {
            this.logger.debug("authenticated.");
            String groupID = this.generateGroupID();
            this.logger.debug("Generated group id: "+groupID);
            Group group = new Group (groupID, description, user);
            this.logger.debug("Group created: "+group);
            this.addGroupInDB(group);
            this.logger.debug("Group added in DB");
            this.addClientToGroup(groupID, user);
            this.logger.debug("Client added to group");
            return group.getID();
        } else
            throw new CleverException ("Authentication Failed with user: "+user);
    }
    
    synchronized private boolean isCreatorOfGroup (String clientName, String groupID) throws CleverException {
        Group group = this.retrieveGroupFromDB(groupID);
        if (group==null)
            throw new CleverException ("Group doesn't exist.");
        return (group.getCreator().compareTo(clientName)==0);
    }
    
    synchronized private void removeClientFromGroup (String groupID, String clientName) throws CleverException {
        if (!this.existsClientInDB(clientName))
            throw new CleverException("Client doesn't exist in DB");
        if (!this.existsGroupInDB(groupID))
            throw new CleverException("Group doesn't exist in DB.");
        Client client = this.retrieveClientFromDB(clientName);
        Group group = this.retrieveGroupFromDB(groupID);
        client.removeGroup(group.getID());
        group.removeClient(clientName);
        this.updateClientInDB(client);
        this.updateGroupInDB(group);
    }
    
    synchronized public void removeClientFromGroup (String clientName, String groupID, String user, String password) throws CleverException {
        if (this.authenticate(user, password)) {
            if (this.isCreatorOfGroup(user, groupID)) {
                this.removeClientFromGroup(groupID, clientName);
            } else
                throw new CleverException ("The user isn't the creator of the group. The group can be modified only by its creator.");
        } else
            throw new CleverException ("Authentication Failed with user: "+user);
    }
    
    synchronized public void deleteGroup (String groupID, String user, String password) throws CleverException {
        if (this.authenticate(user, password)) {
            Group group = this.retrieveGroupFromDB(groupID);
            if (group.getCreator().compareTo(user)==0) {
                if (group.getNote().compareTo(this.registrationGroupDescription)!=0) {
                    ArrayList<String> clients = group.getClients();
                    for (String client : clients) {
                        if (user.compareTo(client)==0)
                            continue; //the group's creator must be removed at the end
                        this.removeClientFromGroup(groupID, client);
                    }
                    this.removeClientFromGroup(groupID, user); //remove group's creator
                    this.deleteGroupFromDB(group);
                } else
                    throw new CleverException ("Impossible to delete the group created during registration");
            } else
                throw new CleverException ("The user isn't the creator of the group. The group can be modified only by its creator.");
        } else
            throw new CleverException ("Authentication Failed with user: "+user);
    }
        
    synchronized private boolean isPartOfGroup (String clientName, String groupID) throws CleverException {
        Client client = this.retrieveClientFromDB(clientName);
        if (client==null)
            return false;
        return client.isPartOfGroup(groupID);
    }
    
    synchronized public boolean isPartOfGroup (String clientName, String groupID, String user, String password) throws CleverException {
        if (this.authenticate(user, password)) {
            if (this.isPartOfGroup(user, groupID)) {
                return this.isPartOfGroup(clientName, groupID);
            } else {
                throw new CleverException ("Permission denied. The user "+user+" isn't part of the group.");
            }
        } else
            throw new CleverException ("Authentication Failed with user: "+user);
    }
    
    synchronized private ArrayList<String> getGroupMembers (String groupID) throws CleverException {
        Group group = this.retrieveGroupFromDB(groupID);
        if (group==null)
            throw new CleverException ("Group doesn't exist.");
        ArrayList<String> result = new ArrayList<String> ();
        result.add(this.getGroupCreator(groupID));
        result.addAll(group.getClients());
        return result;
    }
    
    synchronized public ArrayList<String> getGroupMembers (String groupID, String user, String password) throws CleverException {
        if (this.authenticate(user, password)) {
            if (this.isPartOfGroup(user, groupID)) {
                return this.getGroupMembers(groupID);
            } else {
                throw new CleverException ("Permission denied. The user "+user+" isn't part of the group.");
            }
        } else
            throw new CleverException ("Authentication Failed with user: "+user);
    }
    
    synchronized private String getGroupCreator (String groupID) throws CleverException {
        Group group = this.retrieveGroupFromDB(groupID);
        if (group==null)
            throw new CleverException ("Group doesn't exist.");
        return group.getCreator();
    }
    
    synchronized public String getGroupCreator (String groupID, String user, String password) throws CleverException {
        if (this.authenticate(user, password)) {
            if (this.isPartOfGroup(user, groupID)) {
                return this.getGroupCreator(groupID);
            } else {
                throw new CleverException ("Permission denied. The user "+user+" isn't part of the group.");
            }
        } else
            throw new CleverException ("Authentication Failed with user: "+user);
    }
    
    synchronized public ArrayList<String[]> getClientGroups (String clientName) throws CleverException {
        Client client = this.retrieveClientFromDB(clientName);
        if (client==null)
            throw new CleverException ("Client doesn't exist.");
        return new ArrayList<String[]> (client.getGroups());
    }
    
    synchronized public ArrayList<String[]> getMyGroups (String user, String password) throws CleverException {
        if (this.authenticate(user, password)) {
            return this.getClientGroups(user);
        } else
            throw new CleverException ("Authentication Failed with user: "+user);
    }
    
    synchronized public String getFirstGroupID (String user, String password) throws CleverException {
        ArrayList<String[]> groupIDs = this.getMyGroups(user, password);
        String primaryGroupID = null;
        for (String[] group : groupIDs)
            if (group[1].compareTo(this.registrationGroupDescription)==0) {
                primaryGroupID = group[0];
                break;
            }
        return primaryGroupID;
    }
        
    synchronized private void deleteGroupFromDB (Group group) throws CleverException {
        if (!this.existsGroupInDB(group))
            return;
        ArrayList<Object> params = new ArrayList<Object>();
        String location = "/"+this.groupsNodeName+"/group[@id='"+group.getID()+"']";
        params.add(this.agentName);
        params.add(location);
        this.owner.invoke("DatabaseManagerAgent", "deleteNode", true, params);
    }
    
    synchronized public String getStorageNodes() throws CleverException {
        String clientXML;
        String location = "/"+this.clientsNodeName;
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(this.agentName);
        params.add(location);
        clientXML = (String) this.owner.invoke("DatabaseManagerAgent", "getContentNodeObject", true, params);
        clientXML = SednaUtil.decodeSednaXml(clientXML);
        clientXML = "<"+this.clientsNodeName+">\n"+clientXML+"\n</"+this.clientsNodeName+">";
        
        String groupXML;
        location = "/"+this.groupsNodeName;
        params.clear();
        params.add(this.agentName);
        params.add(location);
        groupXML = (String) this.owner.invoke("DatabaseManagerAgent", "getContentNodeObject", true, params);
        groupXML = SednaUtil.decodeSednaXml(groupXML);
        groupXML = "<"+this.groupsNodeName+">\n"+groupXML+"\n</"+this.groupsNodeName+">";
        
        return clientXML+"\n\n"+groupXML;
    }
    
}
