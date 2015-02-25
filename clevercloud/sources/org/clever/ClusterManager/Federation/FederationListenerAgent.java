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
package org.clever.ClusterManager.Federation;

import org.clever.ClusterManager.FederationPlugins.FederationRequestExecutor;
import org.clever.ClusterManager.FederationPlugins.FederationMessagePoolThread;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.clever.ClusterManager.Dispatcher.DispatcherAgent;
import org.clever.ClusterManager.DispatcherPlugins.DispatcherClever.Request;
import static org.clever.Common.Communicator.Agent.logger;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Communicator.Notification;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.Shared.Support;
import org.clever.Common.Timestamp.Timestamper;
import org.clever.Common.XMLTools.FileStreamer;
import org.clever.Common.XMLTools.ParserXML;
import org.clever.Common.XMPPCommunicator.CleverMessage;
import org.clever.Common.XMPPCommunicator.CleverMessageHandler;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.Common.XMPPCommunicator.RoomListener;
import org.clever.Common.smack.packet.Presence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author Giovanni Volpintesta
 */
public class FederationListenerAgent extends CmAgent implements CleverMessageHandler {

    private Class cl;
    private FederationListenerPlugin plugin;

    private DispatcherAgent dispatcherAgent;
    private ConnectionXMPP conn = null; //Connection to the FEDERATION room
    private RoomListener messageListener;

    private FederationMessagePoolThread federationMessagePoolThread;
    private final HashMap<Integer, Request> requestPool;
    private final HashMap<Integer, Request> requestPool2;
    private String domain;
    private final String cfgPath = "./cfg/configuration_federation.xml";
    private File cfgFile;
    private String server = "";
    private int port = 0;
    private String room = "";
    private String username = "";
    private String password = "";
    private String nickname = "";
    private long defaultTimeout = 60000;
    private int attempts = 1;

    private ParserXML pXML;

    public FederationListenerAgent() {
        super();
        logger = Logger.getLogger("FederationListenerAgent");
        this.requestPool = new HashMap<Integer, Request>();
        this.requestPool2 = new HashMap<Integer, Request>();
    }

    @Override
    public void initialization() throws Exception {
        if (super.getAgentName().equals("NoName")) {
            super.setAgentName("FederationListenerAgent");
        }
        super.start();

        this.cfgFile = new File(this.cfgPath);
        InputStream inxml;

        //If cfgFile doesn't exist (for example: first time starting), create it.
        if (!this.cfgFile.exists()) {
            // Copy the content of file
            logger.info("configuration file doesn't exist. Creating configuration file");
            inxml = getClass().getResourceAsStream("/org/clever/ClusterManager/Federation/configuration_federationListener.xml");
            Support.copy(inxml, cfgFile);
            inxml.close();
            logger.info("configuration file created");
        }

        logger.info("Reading configuration FederationListenerAgent");
        inxml = new FileInputStream(cfgPath);
        FileStreamer fs = new FileStreamer();
        String xmlString = fs.xmlToString(inxml);
        this.pXML = new ParserXML(xmlString);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new InputSource(new ByteArrayInputStream(xmlString.getBytes("utf-8"))));
        doc.getDocumentElement().normalize();
        Element rootelement = doc.getDocumentElement();

        NodeList list;

        logger.info("configuring FederationListenerAgent...");

        list = rootelement.getElementsByTagName("server");
        if (list != null && list.getLength() > 0) {
            this.server = list.item(0).getTextContent();
        }
        list = rootelement.getElementsByTagName("port");
        if (list != null && list.getLength() > 0) {
            this.port = Integer.parseInt(list.item(0).getTextContent());
        }
        list = rootelement.getElementsByTagName("room");
        if (list != null && list.getLength() > 0) {
            this.room = list.item(0).getTextContent();
        }
        list = rootelement.getElementsByTagName("username");
        if (list != null && list.getLength() > 0) {
            this.username = list.item(0).getTextContent();
        }
        list = rootelement.getElementsByTagName("password");
        if (list != null && list.getLength() > 0) {
            this.password = list.item(0).getTextContent();
        }
        list = rootelement.getElementsByTagName("nickname");
        if (list != null && list.getLength() > 0) {
            this.nickname = list.item(0).getTextContent();
        }
        list = rootelement.getElementsByTagName("defaultTimeout");
        if (list != null && list.getLength() > 0) {
            this.defaultTimeout = Long.parseLong(list.item(0).getTextContent());
        }
        list = rootelement.getElementsByTagName("attempts");
        if (list != null && list.getLength() > 0) {
            this.attempts = Integer.parseInt(list.item(0).getTextContent());
        }

        logger.info("FederationListenerAgent configured!");

        this.messageListener = new RoomListener(this);
        logger.info("FEDERATION RoomListener created");

        this.connectionManagement();
        logger.info("Created connection to FEDERATION XMPP server");

        list = rootelement.getElementsByTagName("domain");
        if (list != null && list.getLength() > 0) {
            this.domain = list.item(0).getTextContent();
        }
        list = rootelement.getElementsByTagName("FederationListenerPlugin");
        if (list != null && list.getLength() > 0) {
            this.cl = Class.forName(list.item(0).getTextContent());
        }
        this.plugin = (FederationListenerPlugin) this.cl.newInstance();
        this.plugin.setOwner(this);
        this.plugin.setLogger(logger);
        this.plugin.setConnection(this.conn);
        this.plugin.setDomain(this.domain);
        this.plugin.setAttempts(this.attempts);
        this.plugin.setDefaultTimeout(this.defaultTimeout);

        this.federationMessagePoolThread = new FederationMessagePoolThread(this);
        this.federationMessagePoolThread.setLogger(logger);
        this.federationMessagePoolThread.start();
        logger.info("FederatioMessagePoolThread started");

        logger.info("FederationListenerPlugin istantiated!");

        inxml.close();
    }

    @Override
    public Class getPluginClass() {
        return this.cl;
    }

    @Override
    public Object getPlugin() {
        return this.plugin;
    }

    @Override
    public void shutDown() {
    }

    @Override
    public void handleNotification(Notification notification) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Creates a new connection to the FEDERATION room and add the agent himself
     * to che chat listeners.
     *
     * @throws java.lang.Exception
     */
    public void connectionManagement() throws Exception {
        this.conn = new ConnectionXMPP();
        logger.info("Connection to federation's server XMPP: " + server + "at port: " + port);
        this.conn.connect(server, port);
        // Check if the username or password is blank
        // and try to use In-Band Registration
        if (username.isEmpty() || password.isEmpty()) {
            username = nickname = "cm" + this.conn.getHostName();
            password = Support.generatePassword(7);
            this.conn.inBandRegistration(username, password);
            pXML.modifyXML("username", username);
            pXML.modifyXML("password", password);
            pXML.modifyXML("nickname", nickname);
            pXML.saveXML(cfgPath); //Update configuration file
        }
        logger.info("authentication with federation's XMPP server....");
        this.conn.authenticate(username, password);
        logger.info("authenticated with federation's XMPP server!");
        //Not joining to any room because only the active CM can join    
    }

    public void setAsActiveCM(Boolean active) {
        if (active) {
            logger.debug("ConnectionXMPP.ROOM.FEDERATION = " + ConnectionXMPP.ROOM.FEDERATION);
            logger.debug("conn = " + this.conn);
            logger.debug("room = " + room);
            logger.debug("username = " + this.conn.getUsername());
            this.conn.joinInRoom(room, ConnectionXMPP.ROOM.FEDERATION, this.conn.getUsername(), "CM_ACTIVE"); //Tutti gli occupanti della chat sono CM_ACTIVE. Qui si potrebbe aggiungere il dominio come stato in modo da poter identificare i domini
            logger.info("CM joined in FEDERATION room");
            logger.debug("MultiUserChat = " + this.conn.getMultiUserChat(ConnectionXMPP.ROOM.FEDERATION));
            //Il getMultiUserChat va fatto dopo essersi uniti alla room
            this.conn.getMultiUserChat(ConnectionXMPP.ROOM.FEDERATION).changeAvailabilityStatus("CM_ACTIVE", Presence.Mode.chat); //set the status of this FederationListenerAgent active
            this.conn.getMultiUserChat(ConnectionXMPP.ROOM.FEDERATION).addMessageListener(this.messageListener);
            logger.info("Room listener added in FEDERATION MultiUserChat");
            this.conn.addChatManagerListener(this);
            logger.info("Chat manager added for the connection with federation server");
            this.plugin.initAsActive();
            logger.info("Plugin initialized as ActiveCM");
        } else {
            //TODO Verify this functionality
            this.conn.getMultiUserChat(ConnectionXMPP.ROOM.FEDERATION).changeAvailabilityStatus("CM_MONITOR", Presence.Mode.away);
            this.conn.getMultiUserChat(ConnectionXMPP.ROOM.FEDERATION).removeMessageListener(this.messageListener);
        }
    }

    @Override
    public synchronized void handleCleverMessage(final CleverMessage msg) {

        if (msg.getDst().compareTo(this.username) == 0) {
            logger.debug("Message: " + msg.toXML());
            switch (msg.getType()) {
                case REQUEST:
                    FederationRequestExecutor th = new FederationRequestExecutor(this, msg, logger);
                    th.start();
                    break;
                case REPLY:
                    try {
                        //Timestamper.write("TIME-"+msg.getSrc());
                        //this.logger.debug("((((((" + msg.getBodyModule() + " " + msg.getBodyOperation());
                        if (this.requestPool2.containsKey(msg.getReplyToMsg())) {
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

    public FederationMessagePoolThread getFederationMessagePoolThread() {
        return this.federationMessagePoolThread;
    }

    public HashMap<Integer, Request> getRequestPool() {
        return this.requestPool;
    }
    
    public HashMap<Integer, Request> getRequestPool2() {
        return this.requestPool2;
    }
    
    public void sendMessage(final CleverMessage msg) {
        String target = msg.getDst();
        this.conn.sendMessage(target, msg);
    }
    public void sendMessage(final CleverMessage msg,final String cm) {
        String target = msg.getDst();
        this.conn.sendMessage(target, msg, cm);
    }
    public void sendMessage(final CleverMessage msg,final String cm, ConnectionXMPP connect) {
        String target = msg.getDst();
        connect.sendMessage(target, msg, cm);
    }
}
