/*
 * The MIT License
 *
 * Copyright (c) 2014 Giovanni Volpintesta
 * Copyright (c) 2014 Alfonso Panarello
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.clever.ClusterManager.DispatcherPlugins.DispatcherClever.Request;
import org.clever.ClusterManager.Federation.FederationListenerAgent;
import org.clever.ClusterManager.Federation.FederationListenerPlugin;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.Exceptions.RequestAborted;
import org.clever.Common.Exceptions.RequestExpired;
import org.clever.Common.Federation.FederationReply;
import org.clever.Common.Timestamp.Timestamper;
import org.clever.Common.XMPPCommunicator.CleverMessage;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.Common.XMPPCommunicator.ExecOperation;

/**
 *
 * @author Giovanni Volpintesta
 */
public class FederationListener implements FederationListenerPlugin {

    private CmAgent owner;
    private Logger logger;
    private String domain;
    private long defaultTimeout;
    private int attempts;
    private ConnectionXMPP conn;
    final public static String agentName = "FederationListenerAgent";
    final public static String federationNode = "Federation";
    final public static String CMtag = "CM";
    final public static String domainAttribute = "domain";

    @Override
    public void setOwner(Agent owner) {
        this.owner = (CmAgent) owner;
    }

    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void setConnection(ConnectionXMPP conn) {
        this.conn = conn;
    }

    @Override
    public void setDomain(String domain) {
        this.domain = domain;
    }

    //LOOP PROBLEMS ARE NOT SOLVED
    private FederationReply forwardCommand(final String agent, final String command, final boolean hasReply, final List params, final List<String> history, long timeout) throws CleverException {
        logger.info("Requested remote launching of " + command + " method of " + agent + " agent");
        //launch the command on each of the CMs until it's executed
        HashMap<String, String> federation = this.scanFederation();
        for (String usedDomain : history) {
            federation.remove(usedDomain); //elimino tutti i domini su cui è già stato lanciato il metodo
        }
        if (federation.isEmpty()) {
            throw new CleverException("Launch of " + command + " method of " + agent + " agent failed on all of the federated CMs");
        }
        String choosenDomain = this.chooseDomain(federation);
        try {
            //non aggiungo il dominio alla history perchè lo faccio in forwardCommandToDomain()
            logger.debug("Launching " + command + " method of " + agent + " agent on " + choosenDomain + " domain");
            Object r = this.forwardCommandToDomain(choosenDomain, agent, command, hasReply, params, history, timeout);
            logger.debug(command + " method of " + agent + " agent successfully launched on " + choosenDomain + " domain");
            return new FederationReply(choosenDomain, history, r);
        } catch (CleverException ex) {
            this.logger.error("Error launching " + command + " method of " + agent + " agent to the active CM of " + choosenDomain + " domain: " + ex.getMessage());
            return this.forwardCommand(agent, command, hasReply, params, history, timeout);
        }
        //If the method have not returned at this point it means that he tried to
        //launch the method to all federated CM withouto success. So throw Exception.
    }

    @Override
    public ArrayList<String> getFederatedCM() {
        logger.debug("Sono nel mio get FederatedCM");
        return this.getAllFederatedCM();
    }

    private ArrayList<String> getAllFederatedCM() {
        ArrayList<String> a = new ArrayList<String>();
        a = this.conn.getAllFederatedCMs();
        //logger.debug("a.size= "+a.size());
        //logger.debug("a.value= "+a.get(0));
        return a;
    }

    private ArrayList<String> getFederatedCMs() {
        ArrayList<String> a = new ArrayList<String>();
        a = this.conn.getFederatedCMs();
        //logger.debug("a.size= "+a.size());
        //logger.debug("a.value= "+a.get(0));
        return a;

    }

    @Override
    public int getNumHmPerDomain(String domain) throws CleverException {
        logger.debug(" - - - GetNumHmPerDomain");
        int a = this.getNumVms();
        logger.debug("Numero HM per il dominio: " + domain + " è= " + a);

        return a;

    }

    private int getNumVms() {
        ArrayList<String> a = new ArrayList<String>();
        a = this.conn.getHCsInRoom();
        return a.size();
    }

    /**
     * This method actually returns a random domain from the HashMap, but it's
     * written as a method because in future there could be more complex
     * politics to decide the domain.
     *
     * @param federation
     * @return
     */
    private String chooseDomain(HashMap<String, String> federation) {
        int N = federation.size();
        Random rnd = new Random();
        Set<String> domains = federation.keySet();
        return (String) domains.toArray()[rnd.nextInt(N)];
    }

    /**
     * This method must be launched by a CM that becomes active. Parameters nick
     * and domain are the ones of the CM that has just become active, so that
     * this CM can add it in SednaDB, and then this method returns an array
     * containing the domain [0] and the nickname [1] of this CM, so that the CM
     * that has just become active can add them to its SednaDB.
     *
     * @param nick
     * @param domain
     * @return
     * @throws org.clever.Common.Exceptions.CleverException
     */
    @Override
    public String[] addAsActiveCMandReply(String domain, String nick) throws CleverException {
        this.updateDomain(domain, nick); //updateDomain() launches addDomain() if the domain does not exist in DB
        logger.info(nick + " cm added to federation for " + domain + " domain.");
        logger.info("Actual Federation:\n" + this.scanFederation().toString());
        String[] reply = new String[2];
        reply[0] = this.domain;
        reply[1] = this.conn.getUsername();
        logger.info("Nick and domain of this CM was communicated to CM " + nick);
        return reply;
    }

    private void addDomain(String domain, String CMnick) throws CleverException {
        if (this.existsDomain(domain)) {
            this.updateDomain(domain, CMnick);
        } else {
            String node = "<" + FederationListener.CMtag + " " + FederationListener.domainAttribute + "=\"" + domain + "\">"
                    + CMnick
                    + "</" + FederationListener.CMtag + ">";
            String location = "/" + FederationListener.federationNode;
            ArrayList<Object> params = new ArrayList<Object>();
            params.add(FederationListener.agentName);
            params.add(node);
            params.add("into");
            params.add(location);
            this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
        }
    }

    private boolean existsDomain(String domain) throws CleverException {
        String location = "/" + FederationListener.federationNode + "/" + FederationListener.CMtag + "[@" + FederationListener.domainAttribute + "='" + domain + "']";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(FederationListener.agentName);
        params.add(location);
        return (Boolean) this.owner.invoke("DatabaseManagerAgent", "existNode", true, params);
    }

    private void updateDomain(String domain, String CMnick) throws CleverException {
        if (this.existsDomain(domain)) {
            ArrayList<Object> params = new ArrayList<Object>();
            String location = "/" + FederationListener.federationNode + "/" + FederationListener.CMtag + "[@" + FederationListener.domainAttribute + "='" + domain + "']";
            params.add(FederationListener.agentName);
            params.add(location);
            this.owner.invoke("DatabaseManagerAgent", "deleteNode", true, params);
            this.addDomain(domain, CMnick);
        } else {
            this.addDomain(domain, CMnick);
        }
    }

    public void deleteDomain(String domain) throws CleverException {
        logger.debug("deleteDomain - ");
        this.deleteDomainOnDB(domain);

    }

    private void deleteDomainOnDB(String domain) throws CleverException {
        if (this.existsDomain(domain)) {
            ArrayList<Object> params = new ArrayList<Object>();
            String location = "/" + FederationListener.federationNode + "/" + FederationListener.CMtag + "[@" + FederationListener.domainAttribute + "='" + domain + "']";
            params.add(FederationListener.agentName);
            params.add(location);
            this.owner.invoke("DatabaseManagerAgent", "deleteNode", true, params);
            //this.addDomain(domain, CMnick);
        }
    }

    private String getCM(String domain) throws CleverException {
        String location = "/" + FederationListener.federationNode + "/" + FederationListener.CMtag + "[@" + FederationListener.domainAttribute + "='" + domain + "']";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(FederationListener.agentName);
        params.add(location);
        params.add("/text()");
        String result = (String) this.owner.invoke("DatabaseManagerAgent", "getContentNodeXML", true, params);
        if (result.isEmpty()) {
            return null;
        } else {
            return result;
        }
    }

    /**
     * Returns an HashMap containing all the domains present in the federation
     * and the CM of each of them.
     *
     * @return
     */
    private HashMap<String, String> scanFederation() throws CleverException {
        HashMap<String, String> result = new HashMap<String, String>();
        ArrayList<Object> params = new ArrayList<Object>();
        String location = "/" + FederationListener.federationNode;
        params.add(FederationListener.agentName);
        params.add(location);
        String xml = (String) this.owner.invoke("DatabaseManagerAgent", "getContentNodeObject", true, params);
        logger.debug("IL GETContentNodeObject ritorna la stringa: " + xml);

        xml = this.decodeSednaXml(xml);
        while (true) {
            String[] p = xml.split("<" + FederationListener.CMtag, 2);
            if (p.length < 2) //Condizione di fine ciclo
            {
                break;
            }
            p = p[1].split(FederationListener.domainAttribute, 2);
            if (p.length < 2) //Questo non dovrebbe mai succedere. Lo metto per evitare puntatori a null.
            {
                break;
            }
            p = p[1].split("\"", 2);
            if (p.length < 2) //Questo non dovrebbe mai succedere. Lo metto per evitare puntatori a null.
            {
                break;
            }
            p = p[1].split("\"", 2);
            if (p.length < 2) //Questo non dovrebbe mai succedere. Lo metto per evitare puntatori a null.
            {
                break;
            }
            String dominio = p[0];
            p = p[1].split(">", 2);
            if (p.length < 2) //Questo non dovrebbe mai succedere. Lo metto per evitare puntatori a null.
            {
                break;
            }
            p = p[1].split("</" + FederationListener.CMtag + ">", 2);
            String cmNick = p[0];
            xml = p[1];
            result.put(dominio, cmNick);
        }
        return result;
    }

    private String decodeSednaXml(String xml) {
        String result = "";
        String[] p;
        while (true) {
            p = xml.split("&", 2);
            if (p.length < 2) {
                result += p[0];
                break;
            }
            result += p[0];
            p = p[1].split(";", 2);
            if (p.length < 2) { //per evitare puntatori a null, ma questa condizione non dovrebbe mai verificarsi
                result += p[0];
                break;
            }
            if (p[0].compareTo("nbsp") == 0) {
                result += " ";
                xml = p[1];
            } else if (p[0].compareTo("quot") == 0) {
                result += "\"";
                xml = p[1];
            } else if (p[0].compareTo("amp") == 0) {
                result += "&";
                xml = p[1];
            } else if (p[0].compareTo("lt") == 0) {
                result += "<";
                xml = p[1];
            } else if (p[0].compareTo("gt") == 0) {
                result += ">";
                xml = p[1];
            } else if (p[0].compareTo("acute") == 0) {
                result += "´";
                xml = p[1];
            } else {
                result += "&" + p[0] + ";";
                xml = p[1];
            }
        }
        return result;
    }

    @Override
    public void initAsActive() {
        try {
            ArrayList<String> federatedCMs = this.getAllFederatedCM();
            logger.debug("\n\n\n\n\n\nRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR" + federatedCMs.get(0) + "RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR");
            ArrayList<Object> params = new ArrayList<Object>();
            //Check if the agent node exists
            params.add(FederationListener.agentName);
            params.add("/" + FederationListener.federationNode);
            boolean exist;
            exist = ((Boolean) this.owner.invoke("DatabaseManagerAgent", "existNode", true, params)).booleanValue();
            if (exist) {
                this.logger.info("Federation's node already exists.");
            } else {
                this.logger.info("Federation's node doesn't exist in sednaDB. Creating federation's node");
                params.clear();
                params.add(FederationListener.agentName);
                params.add("<" + FederationListener.federationNode + " />");
                params.add("into");
                params.add("");
                try {
                    this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
                } catch (CleverException ex) {
                    this.logger.error("Error inserting Federation node in DB");
                }
                this.logger.info("FederationNode created.");
            }

            //Notifies connection and create nodes for each of the occupants.
            params.clear();
            params.add(this.domain);
            params.add(this.conn.getUsername());
            logger.debug("Creation of nodes on sednaDB for each of the federated CMs...");
            for (String target : federatedCMs) {
                logger.debug("target: " + target);
                try {
                    logger.info("Retrieving nick and domain for CM: " + target);
                    String[] reply = (String[]) this.forwardCommandToCM(target, FederationListener.agentName, "addAsActiveCMandReply", true, params, this.defaultTimeout);
                    logger.info("Retrieved nick: " + reply[1] + " domain: " + reply[0]);
                    this.updateDomain(reply[0], reply[1]); //updateDomain() launches addDomain() if domain node does not exixt in DB
                    this.logger.info("Domain=" + reply[0] + " CM=" + reply[1] + " added in DB");
                } catch (CleverException ex) {
                    this.logger.error("Error notifing connection to CM " + target);
                }
            }
            //stampo nel log lo stato dei domini della federazione:
            logger.info("Domains in federation node:\n" + this.scanFederation().toString());
        } catch (CleverException ex) {
            this.logger.error("Error checking if Federaton node exists in DB");
        }
    }

    private Object forwardCommandToDomain(String domain, String agent, String command, boolean hasReply, List params, List<String> history, long timeout) throws CleverException {
        history.add(domain); //questa riga deve essere la prima perchè deve essere eseguita prima di incorrere in qualsiasi eccezione
        logger.info(domain + " domain added to the history of the method.");
        if (!this.existsDomain(domain)) {
            throw new CleverException(domain + " domain doesn't exist in federation");
        }
        String cm = this.getCM(domain);
        logger.info("Preparing to launch " + command + " method of " + agent + " agent to " + domain + " domain (CM=" + cm + ").");
        return this.forwardCommandToCM(cm, agent, command, hasReply, params, timeout);
    }

    private Object forwardCommandToDomain(String domain, String agent, String command, boolean hasReply, List params, List<String> history) throws CleverException {
        history.add(domain); //questa riga deve essere la prima perchè deve essere eseguita prima di incorrere in qualsiasi eccezione
        logger.info(domain + " domain added to the history of the method.");
        if (!this.existsDomain(domain)) {
            throw new CleverException(domain + " domain doesn't exist in federation");
        }
        String cm = this.getCM(domain);
        logger.info("Preparing to launch " + command + " method of " + agent + " agent to " + domain + " domain (CM=" + cm + ").");
        return this.forwardCommandToCM(cm, agent, command, hasReply, params);
    }

    private Object forwardCommandToCM(String cm, String agent, String command, boolean hasReply, List params, long timeout) throws CleverException {
        logger.debug("Timeout = " + timeout);
        if (timeout < 0) {
            timeout = this.defaultTimeout;
        }
        logger.debug("Timeout = " + timeout);
        CleverMessage msg = new CleverMessage();
        String src = this.conn.getUsername();
        int id = UUID.randomUUID().hashCode();
        logger.debug("ID for this msg: " + id);
        msg.fillMessageFields(CleverMessage.MessageType.REQUEST, src, cm, hasReply, params, new ExecOperation(command, params, agent), id);
        msg.setId(id);
        Object result;
        int tries;
        for (tries = 0; tries < this.attempts; tries++) {
            Request request = new Request(msg.getId(), timeout);
            //aggiungo la request alla requestPool
            ((FederationListenerAgent) this.owner).getRequestPool().put(new Integer(id), request);
            logger.debug("Launching " + command + " method of " + agent + " agent to CM=" + cm + ").");
            logger.debug("Message type: " + msg.getType().toString() + " type value: " + msg.getType().ordinal() + " id: " + msg.getId());
            ((FederationListenerAgent) this.owner).sendMessage(msg);
            logger.info("Launched " + command + " method of " + agent + " agent to CM=" + cm + ").");
            logger.debug("Waiting and retrieving result for " + command + " method of " + agent + " agent, lauched to CM=" + cm + ").");
            try {
                result = request.getReturnValue();
                logger.info(command + " method of " + agent + " agent successfully launched to CM=" + cm + " after " + tries + " failed tries.");
                return result;
            } catch (CleverException ex) {
                if (ex instanceof RequestExpired) {
                    logger.info("The launch of " + command + " method of " + agent + " to CM=" + cm + " failed due to reached timeout (attempt " + (tries + 1) + "). Trying to relaunch it.");
                    //continue the cycle.
                } else {
                    throw ex;
                }
            }
        }
        //If arrived here, the attempts of failed tries have been reached
        throw new RequestAborted("The launch of " + command + " method of " + agent + " to CM=" + cm + " failed due to reached timeout for " + tries + " attempts. Request aborted");
    }

    private Object forwardCommandToCM(String cm, String agent, String command, boolean hasReply, List params) throws CleverException {
        /* logger.debug("Timeout = "+timeout);
         if (timeout < 0)
         timeout = this.defaultTimeout;
         logger.debug("Timeout = "+timeout);*/
        CleverMessage msg = new CleverMessage();
        String src = this.conn.getUsername();
        int id = UUID.randomUUID().hashCode();
        logger.debug("ID for this msg: " + id);
        msg.fillMessageFields(CleverMessage.MessageType.REQUEST, src, cm, hasReply, params, new ExecOperation(command, params, agent), id);
        msg.setId(id);
        Object result;
        //int tries;
        //for (tries=0; tries<this.attempts; tries++) {
        //for (tries=0; tries<2; tries++) {
        Request request = new Request(msg.getId(), 0);
        //aggiungo la request alla requestPool
        ((FederationListenerAgent) this.owner).getRequestPool().put(new Integer(id), request);
        if (command.equals("sendJob")) {
            ((FederationListenerAgent) this.owner).getRequestPool2().put(new Integer(id), request);
        }
        logger.debug("Launching " + command + " method of " + agent + " agent to CM=" + cm + ").");
        logger.debug("Message type: " + msg.getType().toString() + " type value: " + msg.getType().ordinal() + " id: " + msg.getId());
        if (command.equals("sendJob")) {

            ((FederationListenerAgent) this.owner).sendMessage(msg, cm);
        } else {
            ((FederationListenerAgent) this.owner).sendMessage(msg);
        }
        logger.info("Launched " + command + " method of " + agent + " agent to CM=" + cm);
        logger.debug("Waiting and retrieving result for " + command + " method of " + agent + " agent, lauched to CM=" + cm + ").");
        try {
            result = request.getReturnValue();
            logger.info(command + " method of " + agent + " agent successfully launched to CM= " + cm);
            logger.info("Result = " + result);
            return result;
        } catch (CleverException ex) {
            if (ex instanceof RequestExpired) {
                logger.info("E' Scaduto il time out--");
                logger.info("The launch of " + command + " method of " + agent + " to CM=" + cm + " failed due to reached timeout ");
                //continue the cycle.
            } else {
                throw ex;
            }
        }
        //}
        //If arrived here, the attempts of failed tries have been reached
        throw new RequestAborted("The launch of " + command + " method of " + agent + " to CM=" + cm + " failed due to reached timeout . Request aborted");
    }

    /**
     * This method choose one of the federated CMs and launch the method using
     * it as target. List containing 2 Objects: 1) String representing the
     * nickname of the choosen CM 2) Object returned by the called method
     *
     * @param hasReply
     * @param command
     * @param params
     * @param agent
     * @param timeout
     * @return
     * @throws org.clever.Common.Exceptions.CleverException
     */
    @Override //LOOP PROBLEMS ARE NOT SOLVED
    public FederationReply forwardCommandWithTimeout(String agent, String command, Boolean hasReply, ArrayList params, Long timeout) throws CleverException {
        ArrayList<String> history = new ArrayList<String>();
        history.add(this.domain); //questo serve per evitare loop, in modo che non si possa rilanciare il metodo sul primo dominio che ne ha richiesto il lancio
        return this.forwardCommand(agent, command, hasReply.booleanValue(), params, history, timeout.longValue());
    }

    @Override //LOOP PROBLEMS ARE NOT SOLVED
    public FederationReply forwardCommand(String agent, String command, Boolean hasReply, ArrayList params) throws CleverException {
        return this.forwardCommandWithTimeout(agent, command, hasReply, params, new Long(this.defaultTimeout));
    }

    @Override
    public Object forwardCommandToDomainWithTimeout(String domain, String agent, String command, Boolean hasReply, ArrayList params, Long timeout) throws CleverException {
        ArrayList<String> history = new ArrayList<String>();
        history.add(this.domain); //questo serve per evitare loop, in modo che non si possa rilanciare il metodo sul primo dominio che ne ha richiesto il lancio
        return this.forwardCommandToDomain(domain, agent, command, hasReply.booleanValue(), params, history, timeout.longValue());
    }

    @Override
    public Object forwardCommandToDomainWithoutTimeout(String domain, String agent, String command, Boolean hasReply, ArrayList params) throws CleverException {
        logger.debug("STO CHIAMANDO IL MIO METODO");
        ArrayList<String> history = new ArrayList<String>();
        history.add(this.domain);
        return this.forwardCommandToDomain(domain, agent, command, hasReply.booleanValue(), params, history);
    }

    @Override
    public Object forwardCommandToDomain(String domain, String agent, String command, Boolean hasReply, ArrayList params) throws CleverException {
        return this.forwardCommandToDomainWithTimeout(domain, agent, command, hasReply, params, new Long(this.defaultTimeout));
    }

    @Override
    public ArrayList<String> getFederatedDomains() throws CleverException {
        HashMap<String, String> federation = this.scanFederation();
        ArrayList<String> result = new ArrayList<String>(federation.keySet());

        result.remove(this.domain);
        return result;
    }

    @Override
    public String getLocalDomainName() {
        return this.domain;
    }

    @Override
    public void setDefaultTimeout(long t) {
        this.defaultTimeout = t;
    }

    @Override
    public void setAttempts(int n) {
        this.attempts = n;
    }

    @Override
    public HashMap<String, String> getFederatedCMinDB() throws CleverException {
        HashMap<String, String> federationAll = this.scanFederation();
        // ArrayList<String> result = new ArrayList<String> (federationAll.values());
        return federationAll;

    }

}
