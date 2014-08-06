/*
 * The MIT License
 *
 * Copyright 2013 giovanni.
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
package org.clever.ClusterManager.HBaseManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import static org.clever.Common.Communicator.Agent.logger;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Communicator.Notification;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMLTools.FileStreamer;
import org.clever.Common.XMLTools.ParserXML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author giovanni
 */
public class HBaseManagerAgent extends CmAgent {

    Class cl;
    HBaseManagerPlugin plugin;
    
    public HBaseManagerAgent () {
        super();
        logger = Logger.getLogger("HBaseManagerAgent");
    }
    
    @Override
    public void initialization() {
        if (super.getAgentName().equals("NoName")) {
            super.setAgentName("HBaseManagerAgent");
        }
        try {
            super.start();

            logger.info("Reading configuration HBaseManagerAgent");
            InputStream inxml = getClass().getResourceAsStream("/org/clever/ClusterManager/HBaseManager/configuration_hbaseManager.xml");
            FileStreamer fs = new FileStreamer();
            String xmlString = fs.xmlToString(inxml);
            ParserXML pars = new ParserXML(xmlString);
            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new ByteArrayInputStream(xmlString.getBytes("utf-8"))));
            doc.getDocumentElement().normalize(); //recommended
            Element rootelement = doc.getDocumentElement();

            NodeList nodeList = rootelement.getElementsByTagName("HBaseManagerPlugin"); //può essere uno solo e deve esserci
            this.cl = Class.forName(nodeList.item(0).getTextContent());
            logger.debug("HBASEMANAGER DEBUG: plugin class: "+cl.getName());
            
            this.plugin = (HBaseManagerPlugin) this.cl.newInstance(); //LE CONFIGURAZIONI DEL PLIGIN SONO AUTOMATICAMENTE POSTE AI VALORI DI DEFAULT
            logger.debug("HBASEMANAGER DEBUG: plugin: "+plugin);
            
            this.plugin.setLogger(logger);
            
            nodeList = rootelement.getElementsByTagName("checkpointTimeout"); //può essere uno solo e deve esserci
            this.plugin.setCheckpointTimeout(Integer.parseInt(nodeList.item(0).getTextContent()));
            logger.debug("HBASEMANAGER DEBUG: checkpointTimeout: "+nodeList.item(0).getTextContent());
            
            this.plugin.setOwner(this);
            
            logger.info("Initializing plugin configurations...");
            nodeList = rootelement.getElementsByTagName("options"); //può essere uno solo e deve esserci
            logger.debug("HBASEMANAGER DEBUG: nodeList1: "+nodeList);
            Element options = (Element) nodeList.item(0);
            logger.debug("HBASEMANAGER DEBUG: options element: "+options);
            this.plugin.initialize (options);
            //this.plugin.init(pars.getRootElement().getChild("options"), this);
/*            
            inxml.close();
            inxml = getClass().getResourceAsStream("/org/clever/ClusterManager/HBaseManager/configuration_hbaseManager.xml");
            pars = new ParserXML (fs.xmlToString(inxml));
            //INIZIALIZZA I VALORI DELLE PROPRIETÀ DI HADOOP E HBASE CON QUELLI SPECIFICATI
            //NEL FILE DI CONFIGURATIONE DELL'AGENT.
            
            ParserXML optionsParser = new ParserXML (pars.getElementContent("options"));
            String optionValue;
            
            optionValue = optionsParser.getElementContent("HadoopProperties");
            if (optionValue!=null && !optionValue.isEmpty())
                this.plugin.initializeHadoopConfiguration(optionValue);
            
            optionValue = optionsParser.getElementContent("HBaseProperties");
            if (optionValue!=null && !optionValue.isEmpty())
                this.plugin.initializeHBaseConfiguration(optionValue);
            
            //INIZIALIZZA I VALORI DELLE CONFIGURATIONI DEL PLUGIN HBASEMANAGER CON I VALORI
            //SPECIFICATI NEI RELATIVI TAG DEL FILE DI CONFIGURATIONE DELL'AGENT.
            //SE TALI VALORI VANNO INSERITI NELLE PRORIETÀ DI HADOOP E HBASE, ESSI SOVRASCRIVERANNO
            //QUELLI INSERITI PRECEDENTEMENTE, ANCHE SE SPECIFICATI ALL'INTERNO DEI TAG
            //<HadoopProperties> E <HBaseProperties>.
            optionValue = optionsParser.getElementContent("namenodePort");
            if (optionValue!=null && !optionValue.isEmpty())
                this.plugin.setRequiredNamenodePort(Integer.parseInt(optionValue));
            
            optionValue = optionsParser.getElementContent("backupMastersNumber");
            if (optionValue!=null && !optionValue.isEmpty())
                this.plugin.setRequiredBackupMasterNumber(Integer.parseInt(optionValue));
            
            optionValue = optionsParser.getElementContent("zookeeperPeersNumber");
            if (optionValue!=null && !optionValue.isEmpty())
                this.plugin.setRequiredZookeeperPeersNumber(Integer.parseInt(optionValue));
            
            optionValue = optionsParser.getElementContent("useSecondaryNamenode");
            if (optionValue!=null && !optionValue.isEmpty())
                this.plugin.setRequiredUseSecondaryNamenode(Boolean.parseBoolean(optionValue));
            
            optionValue = optionsParser.getElementContent("nodesPerHostMaxNumber");
            if (optionValue!=null && !optionValue.isEmpty())
                this.plugin.setRequiredNodesPerHostMaxNumber(Integer.parseInt(optionValue));

            optionValue = optionsParser.getElementContent("launchOnlyOneMasterPerHost");
            if (optionValue!=null && !optionValue.isEmpty())
                this.plugin.setRequiredLaunchOnlyOneMasterPerHost(Boolean.parseBoolean(optionValue));
            
            optionValue = optionsParser.getElementContent("keepClustersSepared");
            if (optionValue!=null && !optionValue.isEmpty())
                this.plugin.setRequiredKeepClustersSepared(Boolean.parseBoolean(optionValue));
            
            optionValue = optionsParser.getElementContent("launchSlavesOnMasterNodes");
            if (optionValue!=null && !optionValue.isEmpty())
                this.plugin.setRequiredLaunchSlavesOnMasterNodes(Boolean.parseBoolean(optionValue));
            
            optionValue = optionsParser.getElementContent("cleverOccupationRatio");
            if (optionValue!=null && !optionValue.isEmpty())
                this.plugin.setRequiredCleverOccupationRatio(Float.parseFloat(optionValue));
            
            optionValue = optionsParser.getElementContent("HBase-HadoopSlavesRatio");
            if (optionValue!=null && !optionValue.isEmpty())
                this.plugin.setRequiredHbaseHadoopSlavesRatio(Float.parseFloat(optionValue));
            
            optionValue = optionsParser.getElementContent("joinRegionserversAndDatanodes");
            if (optionValue!=null && !optionValue.isEmpty())
                this.plugin.setRequiredJoinRegionserversAndDatanodes(Boolean.parseBoolean(optionValue));
            
            */
            inxml.close(); 
            logger.info("HBaseManager plugin instantiated!");
        } catch (Exception e) {
            logger.error("Error initializing HBaseManager plugin: " + e.getMessage());
            
        }
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
        //non fa niente, attualmente.
    }

    @Override
    public void handleNotification(Notification notification) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
