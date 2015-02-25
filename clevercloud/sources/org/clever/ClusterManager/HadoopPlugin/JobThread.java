/*
 * The MIT License
 *
 * Copyright 2015 apanarello.
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.clever.ClusterManager.DispatcherPlugins.DispatcherClever.Request;
import org.clever.ClusterManager.FederationPlugins.FederationListener;
import org.clever.ClusterManager.HadoopNamenode.HadoopNamenodeAgent;
import org.clever.ClusterManager.HadoopNamenode.HadoopNamenodePlugin;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.Exceptions.RequestAborted;
import org.clever.Common.Exceptions.RequestExpired;
import org.clever.Common.Federation.DataFederationContainer;
import org.clever.Common.JobRouler.ThreadUser;
import org.clever.Common.Timestamp.Timestamper;
import org.clever.Common.XMPPCommunicator.CleverMessage;
import org.clever.Common.XMPPCommunicator.CleverMessageHandler;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.Common.XMPPCommunicator.ExecOperation;
import org.clever.Common.XMPPCommunicator.RoomListener;

/**
 *
 * @author apanarello
 */
public final class JobThread implements Runnable, CleverMessageHandler {

    public HashMap<Byte, ArrayList> urlMap = null;
    public Map<String, Integer> mapWeight;
    Logger logger;
    public ArrayList<ThreadUser> userList;
    private ArrayList<Object> federationParams = new ArrayList<Object>();
    private ArrayList<Object> commandParams = new ArrayList<Object>();
    private final HashMap<Integer, Request> requestPool;
    private DataFederationContainer dataC;
    private String server, room, userName, nickName, password;
    private int port;
    private Integer w;
    private HadoopNamenodeAgent owner;
    private HadoopNamenodePlugin ownerPlugin;

    public JobThread(DataFederationContainer dataCont, HashMap<Byte, ArrayList> uMap, Logger log, ArrayList<ThreadUser> uList, HadoopNamenodeAgent owner) {

        this.requestPool = new HashMap<Integer, Request>();
        setDataC(dataCont);
        setUserList(uList);
        setOwner(owner);
        setOwnerPlugin(ownerPlugin);
        setUrlMap(uMap);
        setLogger(log);        
        setCommandParams();
        setFederationParams();
   

    }

    public void setCommandParams() {
        this.commandParams.add(dataC.getFileBuffer());
        this.commandParams.add(dataC.getJobName());
        this.commandParams.add(dataC.getBucketName());
        this.commandParams.add(dataC.getFileNameS3());
        this.commandParams.add(dataC.getInizioChunck());
        this.commandParams.add(dataC.getFineChunck());
        this.commandParams.add(dataC.getPart());
        this.commandParams.add(dataC.getWeight());
    }

    public void setFederationParams() {
        this.federationParams.add(dataC.getDomain());
        this.federationParams.add(dataC.getAgentname());
        this.federationParams.add(dataC.getJobName());
        this.federationParams.add(dataC.getHasReplay());
       // this.federationParams.add(getCommandParams());

    }

    
    
    public void setOwner(HadoopNamenodeAgent owner) {
        this.owner = owner;
    }

    public void setOwnerPlugin(HadoopNamenodePlugin ownerPlugin) {
        this.ownerPlugin = ownerPlugin;
    }

    public String getServer() {
        return server;
    }

    public String getRoom() {
        return room;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public ArrayList<Object> getCommandParams() {
        return commandParams;
    }

    public ArrayList<Object> getFederationParams() {
        return federationParams;
    }

    public ArrayList<ThreadUser> getUserList() {
        return userList;
    }

    public DataFederationContainer getDataC() {
        return dataC;
    }

    public void setDataC(DataFederationContainer dataCont) {
        this.dataC = dataCont;
    }

    public void setUserList(ArrayList<ThreadUser> userList) {
        this.userList = userList;
    }

    public HashMap<Byte, ArrayList> getUrlMap() {
        return urlMap;
    }

    public void setUrlMap(HashMap<Byte, ArrayList> urlMap) {
        this.urlMap = urlMap;
    }

    public Map<String, Integer> getMapWeight() {
        return mapWeight;
    }

    public void setMapWeight(Map<String, Integer> mapWeight) {
        this.mapWeight = mapWeight;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    private void connectToChat() {

    }

    @Override
    public void run() {
        logger.debug("ESEGUO IL RUN: " + this.toString());
        setUserName(userList.get(getDataC().getPart()).getUser());
        setPassword(userList.get(getDataC().getPart()).getPass());
        setNickName(userList.get(getDataC().getPart()).getUser());
        this.server = "apanarello";
        this.room = "federation@conference.apanarello";
        this.port = 5222;
        ConnectionXMPP connection = null;
        Byte n = (Byte) commandParams.get(6);
        w = (Integer) commandParams.get(7);
        /* CONNESSIONA AL SERVER E CHAT*/
        try //                
        {
            connection = new ConnectionXMPP();
            connection.connect(this.server, this.port);
            this.logger.debug("---AAA---connessione al server : "+this.server+" sulla porta "+this.port+" NICKNAME: "+this.getUserName());
            connection.authenticate(this.getUserName(), this.getPassword());
            connection.joinInRoom(this.room, ConnectionXMPP.ROOM.FEDERATION, this.nickName, "CM_MONITOR");
        } catch (CleverException Cexec) {
            Cexec.printStackTrace();
        }
        try {
            connection.getMultiUserChat(ConnectionXMPP.ROOM.FEDERATION).addMessageListener(new RoomListener(this));
        } catch (CleverException ex) {
            logger.error("error to obtain ROOMLISTENER", ex);
        }
        logger.info("---AAA---Room listener added in FEDERATION MultiUserChat");
        connection.addChatManagerListener(this);

        

        this.logger.debug("Launching the command to choosen domain... "+federationParams.get(0));
        Object reply = null;

        //* CREO NUOVA CONNESSIONE E LOGIN DEL THREAD IN CHAT
      
        try {
            Timestamper.write("Time07-FinePreparazioneInoltroa-" + this.getDataC().getDomain());
        } catch (IOException ex) {
            this.logger.warn("can't write timestamp log: " + ex.getMessage());
        }
        this.logger.debug("Sto per lanciare L'invoke con i seguenti parametri: " + this.getDataC().getDomain() + " " + this.getDataC().getAgentname());

        try {
            
             logger.debug("--AAA-- INVOCO IL FORWAR COMMAND CON I SEGUENTI PARAMRTI" +"DOMINIO: "+ federationParams.get(0).toString()+"\n"+"AGENTE: "+federationParams.get(1).toString()+"\n"+"METODO: "+federationParams.get(2).toString()+"\n"+"HASREPLAY: "+federationParams.get(3)+"\n"+"FILEROOT: "+commandParams.get(0)+"\n"+"JOB: "+commandParams.get(1)+"\n"+"BUCKET: "+commandParams.get(2)+"\n"+"FILES3: "+commandParams.get(3)+"\n"+"INIZIO: "+commandParams.get(4)+"\n"+"FINE: "+commandParams.get(5)+"\n"+"PART: "+commandParams.get(6)+"\n"+"WEIGHT: "+commandParams.get(7)+"\n"+this.getUserName()+"\n");

            reply = this.forwardCommandToDomain(federationParams.get(0).toString()/*dominoi*/, federationParams.get(1).toString()/*agent*/, federationParams.get(2).toString()/*method*/, (Boolean) federationParams.get(3) /*hasReplay*/, commandParams/*parametersMethod*/, connection, this.getUserName());
            //reply = this.owner.invoke("FederationListenerAgent", "forwardCommandToDomainWithoutTimeout", true, fedParms);
            //                reply = this.owner.invoke("FederationListenerAgent", "forwardCommandToDomainWithoutTimeout", true, fedParms);
            this.logger.debug("---AAA---Response of INVOKE IS: first element in arraylist is " + ((ArrayList) reply).get(0) + "---VALUE----: ");
            this.logger.info("---AAA---Command launched. Reply: " + reply);

            try {
                this.logger.debug("Sto per scrivere il log del dominio: " + federationParams.get(0));
                Timestamper.write("Time18-riuscitoLancioSendJobSuDominioFederato - " + federationParams.get(0));
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
            if (reply != null && reply instanceof Exception) {
                throw new CleverException((Exception) reply);
            }
        } catch (CleverException ex) {

            this.logger.error("Exception caught while forwarding sendJob method on " + federationParams.get(0) + " domain: " + ex);

        }
         try {
                urlMap.put(n.byteValue(), (ArrayList) reply);

            } catch (Exception e) {
                logger.error("error in urlMap put", e);
            }
            logger.debug("Aggiunto Url all'hash table: " + "chiave: " + n.byteValue() + " - Valore:  " + urlMap.get(n.byteValue()));
            this.logger.debug("INVOCATO FORWARD");
            try {
                Timestamper.write("T18-riuscitoLancioSendJobSuDominioFederato - " + federationParams.get(0));
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }

            this.logger.debug("Command successfully launched on domain " + federationParams.get(0));
            connection.closeConnection();

    }

    private Object forwardCommandToDomain(String dominio, String agent, String command, boolean hasReply, List params, ConnectionXMPP conn, String src) throws CleverException {

        logger.info("---AAA---Sono in forwardCommandTo Domani- NUOVA IMPLEMENTAZIONE: " + dominio + " " + agent + " " + command + " ");

        logger.info("---AAA---Preparing to launch " + command + " method of " + agent + " agent to " + dominio + " domain (CM=" + dominio + ").");

        return this.forwardCommandToCM(dominio, agent, command, hasReply, params, conn, src);
    }

    private Object forwardCommandToCM(String dom, String agent, String command, boolean hasReply, List params, ConnectionXMPP connect, String src) throws CleverException {
        /* logger.debug("Timeout = "+timeout);
         if (timeout < 0)
         timeout = this.defaultTimeout;
         logger.debug("Timeout = "+timeout);*/
        CleverMessage msg = new CleverMessage();
        String destCM = this.getCMName(dom);
        logger.debug("---AAA---Il CM del dominio scelto è "+destCM);
        int id = UUID.randomUUID().hashCode();
        logger.debug("---AAA---ID for this msg: --FORWARD COMMAND " + id);
        msg.fillMessageFields(CleverMessage.MessageType.REQUEST, src, destCM, hasReply, params, new ExecOperation(command, params, agent), id);
        msg.setId(id);
      
        Object result;

        Request request = new Request(msg.getId(), 0);
        //aggiungo la request alla requestPool
        requestPool.put(new Integer(id), request);

        logger.debug("---AAA---Launching " + command + " method of " + agent + " agent to CM=" + destCM + ").");
        logger.debug("---AAA---Message type: " + msg.getType().toString() + " type value: " + msg.getType().ordinal() + " id: " + msg.getId());

        this.sendMessage(msg, destCM, connect);

        logger.info("---AAA---Launched " + command + " method of " + agent + " agent to CM=" + destCM);
        logger.debug("---AAA---Waiting and retrieving result for " + command + " method of " + agent + " agent, lauched to CM=" + destCM + ").");
        try {
            result = request.getReturnValue();
            logger.info(command + " method of " + agent + " agent successfully launched to CM= " + destCM);
            logger.info("Result = " + result);
            return result;
        } catch (CleverException ex) {
            if (ex instanceof RequestExpired) {
                logger.info("E' Scaduto il time out--");
                logger.info("The launch of " + command + " method of " + agent + " to CM=" + destCM + " failed due to reached timeout ");
                //continue the cycle.
            } else {
                throw ex;
            }
        }

        throw new RequestAborted("The launch of " + command + " method of " + agent + " to CM=" + destCM + " failed due to reached timeout . Request aborted");

    }

    private String getCMName(String domain) throws CleverException {

        logger.debug("---AAA---Sono nel mio getCMName");

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

    public void sendMessage(final CleverMessage msg, final String cm, ConnectionXMPP connect) {
         logger.debug("---AAA---Sono nel mio sendMe");
          try {
                Timestamper.write("T08-InizioSendMessage - " + federationParams.get(0));
            } catch (IOException ex) {
                this.logger.warn("can't write timestamp log: " + ex.getMessage());
            }
        String target = msg.getDst();
        connect.sendMessage(target, msg);
    }

    @Override
    public void handleCleverMessage(CleverMessage msg) {
        if (msg.getDst().compareTo(this.userName) == 0) {
            logger.debug("Message: " + msg.toXML());
            switch (msg.getType()) {
                /*case REQUEST:
                 FederationRequestExecutor th = new FederationRequestExecutor(this, msg, logger);
                 th.start();
                 break;
                 */
                case REPLY:
                    try {
                        //Timestamper.write("TIME-"+msg.getSrc());
                        //this.logger.debug("((((((" + msg.getBodyModule() + " " + msg.getBodyOperation());
                        if (this.requestPool.containsKey(msg.getReplyToMsg())) {
                            Timestamper.write("TIME-" + msg.getSrc());
                        }
                    } catch (IOException ex) {
                        this.logger.warn("can't write timestamp log: " + ex.getMessage());
                    }
                    logger.debug("id risposta: " + msg.getId());
                    logger.debug("Prendo la request alla key " + msg.getId() + " dalla hashmap che fa da pool");
                    Integer ID = new Integer(msg.getReplyToMsg());
                    if (!this.requestPool.containsKey(ID)) //a timeout occurred
                    {
                        break;
                    }
                    Request request = this.requestPool.get(ID);
                    logger.debug("Request = " + request);
                    requestPool.remove(ID);
                    try {
                        Object replyObject = msg.getObjectFromMessage();
                        logger.debug("reply object: " + replyObject);
                        request.setReturnValue(replyObject);
                        logger.debug("setted return value to request");
                    } catch (CleverException ex) {
                        logger.info("Exception retrieving object from message: " + ex.getMessage());
                        request.setReturnValue(ex);
                    }
                    break;
                case ERROR:
                    try {
                        Timestamper.write("TIME-ERROR" + msg.getSrc());
                    } catch (IOException ex) {
                        this.logger.warn("can't write timestamp log: " + ex.getMessage());
                    }
                    break;
                default:
                    logger.error("Message type is " + msg.getType() + ". FederationListenerAgent isn't allowed to manage a type of message different to REQUEST, REPLY or ERROR. You need to implement managing of other type of messages.");
            }
        }
    }

}
