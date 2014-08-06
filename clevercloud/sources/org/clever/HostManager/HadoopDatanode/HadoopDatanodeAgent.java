/*
 * The MIT License
 *
 * Copyright 2013 mariacristina sinagra.
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
package org.clever.HostManager.HadoopDatanode;

/**
 *
 * @author cristina
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Communicator.Notification;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMLTools.FileStreamer;
import org.clever.Common.XMLTools.ParserXML;
import java.io.OutputStream;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import org.clever.Common.Communicator.Agent;
import static org.clever.Common.Communicator.Agent.logger;
import org.clever.Common.Communicator.MethodInvoker;
//import org.jdom2.*;

public class HadoopDatanodeAgent extends Agent {

    private Logger logger;
    private Class cl;
    private HadoopDatanodePlugin plugin;
    private String hostname;
    private String agentId = "HadoopDatanodeAgent";
    private String coreHadoop;
    private String mapredHadoop;
    private String hadoopTmpDir;

    public HadoopDatanodeAgent()  {
        super();
        logger = Logger.getLogger("HadoopDatanodeAgent");
    }

    @Override
    public void initialization() {
        
        if (super.getAgentName().equals("NoName")) {
            super.setAgentName("HadoopDatanodeAgent");
        }

        try {
            super.start();

            logger.info("Read Configuration HadoopDatanodeAgent!");
            InputStream inxml = getClass().getResourceAsStream("/org/clever/HostManager/HadoopDatanode/configuration_datanode.xml");
            FileStreamer fs = new FileStreamer();
            ParserXML pars = new ParserXML(fs.xmlToString(inxml));
			this.cl = Class.forName(pars.getElementContent("HadoopDatanodePlugin"));
            this.plugin = (HadoopDatanodePlugin) this.cl.newInstance();
            this.plugin.setOwner(this);
            this.plugin.setStartCommand(pars.getElementContent("startCommand"));
            this.plugin.setStopCommand("stopCommand");
            this.coreHadoop = pars.getElementContent("coreSite");
            this.mapredHadoop = pars.getElementContent("mapredSite");
            this.hadoopTmpDir = pars.getElementContent("hadoopTmpDir");

            this.hostname = InetAddress.getLocalHost().getHostName();
            // send notification to dispatcher agent
/*                Notification not=new Notification();
                not.setHostId(hostname);
                not.setId("HADOOP");
                this.sendNotification(not);
                logger.info("notification HADOOP sent");              
*/            
            logger.info("HadoopDatanodePlugin created ");
            
        } catch (CleverException ex) {
            java.util.logging.Logger.getLogger(HadoopDatanodeAgent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            logger.error("Error: " + ex);
        } catch (IOException ex) {
            logger.error("Error: " + ex);
        } catch (InstantiationException ex) {
            logger.error("Error: " + ex);
        } catch (IllegalAccessException ex) {
            logger.error("Error: " + ex);
        } catch (Exception ex) {
            logger.error("HadoopDatanodePlugin creation failed: " + ex);
        }



    }
    
    @Override
    public void sendNotification(Notification notification) {
        try {
            List params = new ArrayList();
            params.add(notification);
            notification.setAgentId(this.agentId);
            //System.out.println("sendNotification invoke");
            MethodInvoker mi = new MethodInvoker("DispatcherAgent",
                    "sendNotification",
                    true,
                    params);

            mc.invoke(mi);
        } catch (CleverException ex) {
            logger.error("Invoke error: " + ex);
        } catch (SecurityException ex) {
            logger.error("Invoke error: " + ex);
        }

    }

    @Override
    public Class getPluginClass() {
        return cl;
    }

    @Override
    public Object getPlugin() {
        return plugin;
    }

    @Override
    public void shutDown() {
        
         //send notification 
         Notification notify=new Notification();
         notify.setHostId(hostname);
         notify.setId("HADOOP/DOWN");
         this.sendNotification(notify);
         logger.info("notification HADOOP/DOWN sent");
        
    }
}
